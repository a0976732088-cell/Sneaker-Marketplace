package com.project.sneakermarket.commerce.logistics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * OpenStreetMap Nominatim 地理編碼 (地址 -> 經緯度)。
 *
 * Nominatim 使用政策 (https://operations.osmfoundation.org/policies/nominatim/) 規定:
 * 1. 一定要帶能識別你這支服務的 User-Agent，沒帶很容易被擋掉 (403 或直接沒回應) —— 這是最常見會查失敗的原因。
 * 2. 呼叫頻率最多 1 次/秒。這支用 synchronized + 最小間隔控制，多執行緒同時查也不會超過限制。
 * 3. 只能從後端呼叫，不能讓前端瀏覽器直接打 Nominatim。
 */
@Slf4j
@Component
public class NominatimGeocodingClient {

    private static final String BASE_URL = "https://nominatim.openstreetmap.org/search";
    // TODO: 換成你自己的聯絡資訊，Nominatim 政策要求要能真的聯絡到服務提供者
    private static final String USER_AGENT = "SneakerMarket-School-Project/1.0 (contact: chrisxcodes@gmail.com)";
    private static final long MIN_INTERVAL_MILLIS = 1100;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private long lastCallAtMillis = 0L;

    /** 回傳 [latitude, longitude]，查不到或查詢失敗回傳 null。 */
    public synchronized double[] geocode(String address) {
        throttle();

        String query = "台灣 " + address;
        String url = BASE_URL
                + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&format=json&limit=1&countrycodes=tw";

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            lastCallAtMillis = System.currentTimeMillis();

            if (response.statusCode() != 200) {
                log.warn("Nominatim 回傳非 200: status={}, address={}, body={}",
                        response.statusCode(), address, response.body());
                return null;
            }

            JsonNode results = objectMapper.readTree(response.body());
            if (!results.isArray() || results.isEmpty()) {
                log.warn("Nominatim 查無結果: address={}", address);
                return null;
            }

            JsonNode first = results.get(0);
            double lat = Double.parseDouble(first.path("lat").asText());
            double lon = Double.parseDouble(first.path("lon").asText());
            return new double[]{lat, lon};

        } catch (Exception e) {
            lastCallAtMillis = System.currentTimeMillis();
            log.error("Nominatim 查詢發生例外: address={}", address, e);
            return null;
        }
    }

    private void throttle() {
        long elapsed = System.currentTimeMillis() - lastCallAtMillis;
        long waitMs = MIN_INTERVAL_MILLIS - elapsed;
        if (waitMs > 0) {
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}