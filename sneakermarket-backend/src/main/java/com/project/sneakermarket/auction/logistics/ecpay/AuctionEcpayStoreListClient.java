package com.project.sneakermarket.auction.logistics.ecpay;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.sneakermarket.auction.exception.AuctionIntegrationException;

/**
 * ECPay GetStoreList client。官方清單每日更新一次；本站依店別快取六小時，
 * 避免每次開啟選店介面都呼叫第三方。
 */
@Component
public class AuctionEcpayStoreListClient {
    static final Duration CACHE_TTL = Duration.ofHours(6);

    private final AuctionEcpayStoreListSettings settings;
    private final AuctionEcpayCheckMacValueService checkMacValues;
    private volatile HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Map<AuctionCvsStoreType, CachedStores> cache = new ConcurrentHashMap<>();
    private final Map<AuctionCvsStoreType, Object> cacheLocks = new EnumMap<>(AuctionCvsStoreType.class);

    @Autowired
    public AuctionEcpayStoreListClient(
            AuctionEcpayStoreListSettings settings,
            AuctionEcpayCheckMacValueService checkMacValues) {
        this(settings, checkMacValues, null, new ObjectMapper(), Clock.systemUTC());
    }

    public AuctionEcpayStoreListClient(
            AuctionEcpayStoreListSettings settings,
            AuctionEcpayCheckMacValueService checkMacValues,
            HttpClient httpClient,
            ObjectMapper objectMapper) {
        this(settings, checkMacValues, httpClient, objectMapper, Clock.systemUTC());
    }

    public AuctionEcpayStoreListClient(
            AuctionEcpayStoreListSettings settings,
            AuctionEcpayCheckMacValueService checkMacValues,
            HttpClient httpClient,
            ObjectMapper objectMapper,
            Clock clock) {
        this.settings = settings;
        this.checkMacValues = checkMacValues;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
        for (AuctionCvsStoreType storeType : AuctionCvsStoreType.values()) {
            cacheLocks.put(storeType, new Object());
        }
    }

    public List<AuctionCvsStoreResponse> getStores(AuctionCvsStoreType storeType) {
        if (storeType == null) {
            throw new IllegalArgumentException("storeType 不可為空");
        }
        Instant now = clock.instant();
        CachedStores current = cache.get(storeType);
        if (isFresh(current, now)) return current.stores();

        synchronized (cacheLocks.get(storeType)) {
            now = clock.instant();
            current = cache.get(storeType);
            if (isFresh(current, now)) return current.stores();
            List<AuctionCvsStoreResponse> stores = List.copyOf(fetch(storeType));
            cache.put(storeType, new CachedStores(stores, now.plus(CACHE_TTL)));
            return stores;
        }
    }

    private boolean isFresh(CachedStores cached, Instant now) {
        return cached != null && now.isBefore(cached.expiresAt());
    }

    private List<AuctionCvsStoreResponse> fetch(AuctionCvsStoreType storeType) {
        settings.requireConfigured();
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("PlatformID", "");
        parameters.put("MerchantID", settings.merchantId());
        parameters.put("CvsType", storeType.name());
        parameters.put("CheckMacValue",
                checkMacValues.calculate(parameters, settings.hashKey(), settings.hashIv()));

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(settings.storeListUrl()))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "text/html")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody(parameters), UTF_8))
                    .build();
        } catch (IllegalArgumentException ex) {
            throw AuctionIntegrationException.unavailable("競標超商門市清單網址設定不正確");
        }

        try {
            HttpResponse<String> response = httpClient().send(
                    request, HttpResponse.BodyHandlers.ofString(UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw AuctionIntegrationException.provider("ECPay 超商門市清單服務目前無法完成請求");
            }
            EcpayStoreListProviderResponse provider = objectMapper.readValue(
                    response.body(), EcpayStoreListProviderResponse.class);
            return canonicalStores(provider, storeType);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw AuctionIntegrationException.provider("ECPay 超商門市清單連線已中斷");
        } catch (IOException | RuntimeException ex) {
            if (ex instanceof AuctionIntegrationException integration) throw integration;
            throw AuctionIntegrationException.provider("ECPay 超商門市清單服務目前無法完成請求");
        }
    }

    private List<AuctionCvsStoreResponse> canonicalStores(
            EcpayStoreListProviderResponse provider,
            AuctionCvsStoreType requestedType) {
        if (provider == null || provider.rtnCode() != 1 || provider.storeList() == null) {
            throw AuctionIntegrationException.provider("ECPay 超商門市清單查詢失敗");
        }

        boolean foundRequestedType = false;
        Map<String, AuctionCvsStoreResponse> byStoreId = new LinkedHashMap<>();
        for (EcpayStoreListProviderResponse.StoreGroup group : provider.storeList()) {
            if (group == null || !requestedType.name().equals(group.cvsType())) continue;
            foundRequestedType = true;
            if (group.storeInfo() == null) {
                throw AuctionIntegrationException.provider("ECPay 超商門市清單格式不正確");
            }
            for (EcpayStoreListProviderResponse.StoreInfo store : group.storeInfo()) {
                if (store == null) {
                    throw AuctionIntegrationException.provider("ECPay 超商門市清單格式不正確");
                }
                AuctionCvsStoreResponse canonical = new AuctionCvsStoreResponse(
                        requestedType,
                        required(store.storeId(), 10),
                        required(store.storeName(), 40),
                        required(store.storeAddress(), 100),
                        optional(store.storePhone(), 20));
                AuctionCvsStoreResponse previous = byStoreId.putIfAbsent(canonical.storeId(), canonical);
                if (previous != null && !previous.equals(canonical)) {
                    throw AuctionIntegrationException.provider("ECPay 超商門市清單包含重複門市代碼");
                }
            }
        }
        if (!foundRequestedType) {
            throw AuctionIntegrationException.provider("ECPay 未回傳指定超商的門市清單");
        }
        return new ArrayList<>(byStoreId.values());
    }

    private String required(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw AuctionIntegrationException.provider("ECPay 超商門市清單格式不正確");
        }
        return normalized;
    }

    private String optional(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > maxLength) {
            throw AuctionIntegrationException.provider("ECPay 超商門市清單格式不正確");
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private String formBody(Map<String, String> parameters) {
        StringBuilder body = new StringBuilder();
        parameters.forEach((key, value) -> {
            if (!body.isEmpty()) body.append('&');
            body.append(URLEncoder.encode(key, UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(value == null ? "" : value, UTF_8));
        });
        return body.toString();
    }

    private HttpClient httpClient() {
        HttpClient current = httpClient;
        if (current != null) return current;
        synchronized (this) {
            current = httpClient;
            if (current == null) {
                try {
                    current = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(10))
                            .build();
                } catch (RuntimeException ex) {
                    throw AuctionIntegrationException.provider("ECPay 超商門市清單連線初始化失敗");
                }
                httpClient = current;
            }
            return current;
        }
    }

    private record CachedStores(List<AuctionCvsStoreResponse> stores, Instant expiresAt) {
    }
}
