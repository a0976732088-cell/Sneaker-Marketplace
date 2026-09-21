package com.project.sneakermarket.auction.logistics.ecpay;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.sneakermarket.auction.exception.AuctionIntegrationException;

class AuctionEcpayStoreListClientTests {
    private HttpClient http;
    private List<HttpRequest> requests;
    private AtomicInteger responseStatus;
    private AtomicReference<String> responseTemplate;

    @BeforeEach
    void setupHttp() throws Exception {
        http = mock(HttpClient.class);
        requests = new ArrayList<>();
        responseStatus = new AtomicInteger(200);
        responseTemplate = new AtomicReference<>(successResponse("${TYPE}"));
        stubHttpResponse();
    }

    @Test
    void postsOfficialFormAndParsesOnlyCanonicalRequestedStoreType() throws Exception {
        responseTemplate.set("""
                {
                  "RtnCode": 1,
                  "RtnMsg": "成功",
                  "StoreList": [
                    {
                      "CvsType": "FAMI",
                      "StoreInfo": [{
                        "StoreId": "009999",
                        "StoreName": "不應回傳門市",
                        "StoreAddr": "其他地址",
                        "StorePhone": "02-99999999"
                      }]
                    },
                    {
                      "CvsType": "UNIMART",
                      "StoreInfo": [{
                        "StoreId": " 001234 ",
                        "StoreName": " 測試門市 ",
                        "StoreAddr": " 臺北市中正區測試路 1 號 ",
                        "StorePhone": " 02-12345678 ",
                        "Ignored": "provider-only"
                      }]
                    }
                  ]
                }
                """);
        AuctionEcpayStoreListClient client = client(Clock.systemUTC());

        List<AuctionCvsStoreResponse> stores = client.getStores(AuctionCvsStoreType.UNIMART);

        assertEquals(List.of(new AuctionCvsStoreResponse(
                AuctionCvsStoreType.UNIMART,
                "001234",
                "測試門市",
                "臺北市中正區測試路 1 號",
                "02-12345678")), stores);
        assertEquals(1, requests.size());
        HttpRequest request = requests.get(0);
        assertEquals("POST", request.method());
        assertEquals("https://local-ecpay.test/Helper/GetStoreList", request.uri().toString());
        assertEquals("text/html", request.headers().firstValue("Accept").orElseThrow());
        assertEquals("application/x-www-form-urlencoded",
                request.headers().firstValue("Content-Type").orElseThrow());
        assertEquals(20, request.timeout().orElseThrow().toSeconds());

        Map<String, String> form = decodeForm(body(request));
        assertEquals("", form.get("PlatformID"));
        assertEquals("2000933", form.get("MerchantID"));
        assertEquals("UNIMART", form.get("CvsType"));
        assertEquals("0BDC6B22EB3A9A13FFB9AE4A73D0A9F7", form.get("CheckMacValue"));
        assertEquals(4, form.size());
    }

    @Test
    void cachesForSixHoursPerStoreTypeThenRefreshes() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-21T08:00:00Z"));
        AuctionEcpayStoreListClient client = client(clock);

        List<AuctionCvsStoreResponse> first = client.getStores(AuctionCvsStoreType.UNIMART);
        List<AuctionCvsStoreResponse> cached = client.getStores(AuctionCvsStoreType.UNIMART);
        assertEquals(first, cached);
        assertEquals(1, requests.size());

        client.getStores(AuctionCvsStoreType.FAMI);
        assertEquals(2, requests.size());

        clock.advance(Duration.ofHours(6));
        client.getStores(AuctionCvsStoreType.UNIMART);
        assertEquals(3, requests.size());
    }

    @Test
    void sendsExactOfficialCvsTypeForEverySupportedStoreBrand() throws Exception {
        AuctionEcpayStoreListClient client = client(Clock.systemUTC());

        client.getStores(AuctionCvsStoreType.UNIMART);
        client.getStores(AuctionCvsStoreType.FAMI);
        client.getStores(AuctionCvsStoreType.HILIFE);

        assertEquals(3, requests.size());
        assertEquals("UNIMART", decodeForm(body(requests.get(0))).get("CvsType"));
        assertEquals("FAMI", decodeForm(body(requests.get(1))).get("CvsType"));
        assertEquals("HILIFE", decodeForm(body(requests.get(2))).get("CvsType"));
    }

    @Test
    void providerFailureIsNotCached() {
        responseTemplate.set("""
                {"RtnCode": 0, "RtnMsg": "失敗", "StoreList": []}
                """);
        AuctionEcpayStoreListClient client = client(Clock.systemUTC());

        assertThrows(AuctionIntegrationException.class,
                () -> client.getStores(AuctionCvsStoreType.HILIFE));
        assertThrows(AuctionIntegrationException.class,
                () -> client.getStores(AuctionCvsStoreType.HILIFE));

        assertEquals(2, requests.size());
    }

    @Test
    void missingRequestedStoreGroupIsRejected() {
        responseTemplate.set(successResponse("FAMI"));
        AuctionEcpayStoreListClient client = client(Clock.systemUTC());

        assertThrows(AuctionIntegrationException.class,
                () -> client.getStores(AuctionCvsStoreType.UNIMART));
    }

    @Test
    void nonSuccessfulHttpStatusMapsToBadGateway() {
        responseStatus.set(503);
        AuctionEcpayStoreListClient client = client(Clock.systemUTC());

        AuctionIntegrationException error = assertThrows(AuctionIntegrationException.class,
                () -> client.getStores(AuctionCvsStoreType.UNIMART));

        assertEquals(HttpStatus.BAD_GATEWAY, error.status());
        assertEquals(1, requests.size());
    }

    @Test
    void malformedProviderJsonMapsToBadGateway() {
        responseTemplate.set("{not-valid-json");
        AuctionEcpayStoreListClient client = client(Clock.systemUTC());

        AuctionIntegrationException error = assertThrows(AuctionIntegrationException.class,
                () -> client.getStores(AuctionCvsStoreType.FAMI));

        assertEquals(HttpStatus.BAD_GATEWAY, error.status());
        assertEquals(1, requests.size());
    }

    private AuctionEcpayStoreListClient client(Clock clock) {
        AuctionEcpayStoreListSettings settings = new AuctionEcpayStoreListSettings(
                "https://local-ecpay.test/Helper/GetStoreList",
                "2000933",
                "XBERn1YOvpM9nfZc",
                "h1ONHk4P4yqbl5LK");
        return new AuctionEcpayStoreListClient(
                settings,
                new AuctionEcpayCheckMacValueService(),
                http,
                new ObjectMapper(),
                clock);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubHttpResponse() throws Exception {
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer(invocation -> {
                    HttpRequest request = invocation.getArgument(0);
                    requests.add(request);
                    String type = decodeForm(body(request)).getOrDefault("CvsType", "UNIMART");
                    HttpResponse<String> response = mock(HttpResponse.class);
                    when(response.statusCode()).thenReturn(responseStatus.get());
                    when(response.body()).thenReturn(
                            responseTemplate.get().replace("${TYPE}", type));
                    return response;
                });
    }

    private String body(HttpRequest request) throws Exception {
        HttpRequest.BodyPublisher publisher = request.bodyPublisher().orElseThrow();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CountDownLatch complete = new CountDownLatch(1);
        publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer item) {
                byte[] chunk = new byte[item.remaining()];
                item.get(chunk);
                bytes.writeBytes(chunk);
            }

            @Override
            public void onError(Throwable throwable) {
                complete.countDown();
            }

            @Override
            public void onComplete() {
                complete.countDown();
            }
        });
        assertTrue(complete.await(1, TimeUnit.SECONDS));
        return bytes.toString(UTF_8);
    }

    private Map<String, String> decodeForm(String body) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            values.put(
                    URLDecoder.decode(parts[0], UTF_8),
                    URLDecoder.decode(parts.length == 2 ? parts[1] : "", UTF_8));
        }
        return values;
    }

    private String successResponse(String type) {
        return """
                {
                  "RtnCode": 1,
                  "RtnMsg": "成功",
                  "StoreList": [{
                    "CvsType": "%s",
                    "StoreInfo": [{
                      "StoreId": "001234",
                      "StoreName": "測試門市",
                      "StoreAddr": "臺北市中正區測試路 1 號",
                      "StorePhone": "02-12345678"
                    }]
                  }]
                }
                """.formatted(type);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            if (ZoneOffset.UTC.equals(zone)) return this;
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
