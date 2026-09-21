package com.project.sneakermarket.auction.payment.linepay;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class LinePayClientTests {
    private AuctionLinePaySettings settings;
    private LinePaySignatureService signatures;
    private HttpClient http;
    private ObjectMapper mapper;
    private LinePayClient client;

    @BeforeEach
    void setup() {
        settings = new AuctionLinePaySettings(
                "channel-123",
                "channel-secret",
                "https://sandbox-api-pay.line.me");
        signatures = mock(LinePaySignatureService.class);
        http = mock(HttpClient.class);
        mapper = new ObjectMapper();
        client = new LinePayClient(settings, signatures, http, mapper);
    }

    @Test
    void requestUsesPostV4HeadersAndTheExactSerializedBodyUsedForSignature() throws Exception {
        when(signatures.sign(eq("/v4/payments/request"), anyString(), anyString(), eq("channel-secret")))
                .thenReturn("signed-value");
        stubResponse("""
                {"returnCode":"0000","returnMessage":"Success","info":{
                  "transactionId":"202609211234567890",
                  "paymentUrl":{"web":"https://sandbox-web-pay.line.me/pay/abc"}
                }}
                """);

        LinePayClient.RequestResult result = client.requestPayment(new LinePayClient.RequestCommand(
                6900L,
                "TWD",
                "ALPABC123",
                new LinePayClient.Product("17", "測試球鞋", 1, 6900L)));

        assertEquals("0000", result.returnCode());
        assertEquals("202609211234567890", result.transactionId());
        assertEquals("https://sandbox-web-pay.line.me/pay/abc", result.paymentUrl());

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(requestCaptor.capture(), anyBodyHandler());
        HttpRequest request = requestCaptor.getValue();
        assertEquals("POST", request.method());
        assertEquals("https://sandbox-api-pay.line.me/v4/payments/request", request.uri().toString());
        assertEquals("application/json", request.headers().firstValue("Content-Type").orElseThrow());
        assertEquals("channel-123", request.headers().firstValue("X-LINE-ChannelId").orElseThrow());
        assertEquals("signed-value", request.headers().firstValue("X-LINE-Authorization").orElseThrow());
        String nonce = request.headers().firstValue("X-LINE-Authorization-Nonce").orElseThrow();
        assertFalse(nonce.isBlank());
        assertEquals(15, request.timeout().orElseThrow().toSeconds());

        String sentBody = body(request);
        ArgumentCaptor<String> signedBody = ArgumentCaptor.forClass(String.class);
        verify(signatures).sign(eq("/v4/payments/request"), signedBody.capture(), eq(nonce),
                eq("channel-secret"));
        assertEquals(signedBody.getValue(), sentBody);

        JsonNode json = mapper.readTree(sentBody);
        assertEquals(6900L, json.path("amount").asLong());
        assertEquals("TWD", json.path("currency").asText());
        assertEquals("ALPABC123", json.path("orderId").asText());
        assertEquals(6900L, json.path("packages").get(0).path("amount").asLong());
        assertEquals("17", json.path("packages").get(0).path("products").get(0).path("id").asText());
        assertEquals("測試球鞋",
                json.path("packages").get(0).path("products").get(0).path("name").asText());
        assertEquals(1, json.path("packages").get(0).path("products").get(0).path("quantity").asInt());
        assertEquals(6900L,
                json.path("packages").get(0).path("products").get(0).path("price").asLong());
        assertEquals("NONE", json.path("redirectUrls").path("confirmUrlType").asText());
        assertFalse(json.path("redirectUrls").has("confirmUrl"));
        assertFalse(json.path("redirectUrls").has("cancelUrl"));
    }

    @Test
    void confirmUsesTransactionIdOnlyInV4PathAndSendsServerAmount() throws Exception {
        when(signatures.sign(anyString(), anyString(), anyString(), eq("channel-secret")))
                .thenReturn("confirm-signature");
        stubResponse("{\"returnCode\":\"0000\",\"returnMessage\":\"Success\"}");

        LinePayClient.ConfirmResult result = client.confirmPayment("998877665544", 6900L, "TWD");

        assertEquals("0000", result.returnCode());
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(requestCaptor.capture(), anyBodyHandler());
        HttpRequest request = requestCaptor.getValue();
        assertEquals("POST", request.method());
        assertEquals("https://sandbox-api-pay.line.me/v4/payments/998877665544/confirm",
                request.uri().toString());
        assertEquals(45, request.timeout().orElseThrow().toSeconds());
        JsonNode json = mapper.readTree(body(request));
        assertEquals(6900L, json.path("amount").asLong());
        assertEquals("TWD", json.path("currency").asText());
    }

    @Test
    void checkStatusUsesGetSignatureNoBodyAndTwentyFiveSecondTimeout() throws Exception {
        when(signatures.signGet(
                eq("/v4/payments/requests/998877665544/check"),
                eq(""),
                anyString(),
                eq("channel-secret")))
                .thenReturn("check-signature");
        stubResponse("{\"returnCode\":\"0110\",\"returnMessage\":\"Authenticated\"}");

        LinePayClient.CheckStatusResult result =
                client.checkPaymentRequestStatus("998877665544");

        assertEquals("0110", result.returnCode());
        assertEquals("Authenticated", result.returnMessage());
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(requestCaptor.capture(), anyBodyHandler());
        HttpRequest request = requestCaptor.getValue();
        assertEquals("GET", request.method());
        assertEquals("https://sandbox-api-pay.line.me/v4/payments/requests/998877665544/check",
                request.uri().toString());
        assertEquals("application/json", request.headers().firstValue("Content-Type").orElseThrow());
        assertEquals("channel-123", request.headers().firstValue("X-LINE-ChannelId").orElseThrow());
        assertEquals("check-signature",
                request.headers().firstValue("X-LINE-Authorization").orElseThrow());
        String nonce = request.headers().firstValue("X-LINE-Authorization-Nonce").orElseThrow();
        assertFalse(nonce.isBlank());
        assertEquals(25, request.timeout().orElseThrow().toSeconds());
        assertTrue(request.bodyPublisher().isEmpty());
        verify(signatures).signGet(
                "/v4/payments/requests/998877665544/check",
                "",
                nonce,
                "channel-secret");
    }

    @SuppressWarnings("unchecked")
    private void stubResponse(String body) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private HttpResponse.BodyHandler<String> anyBodyHandler() {
        return (HttpResponse.BodyHandler) any(HttpResponse.BodyHandler.class);
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
}
