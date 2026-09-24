package com.project.sneakermarket.commerce.logistics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ECPay GetStoreList — 拿指定超商品牌的全台門市清單 (店號/店名/地址/電話，不含經緯度)。
 */
@Component
public class EcpayStoreListClient {

    // ECPay 官方公開的物流 (C2C) 測試特店資訊，正式上線要換成你自己申請的物流特店代號
    @Value("${ecpay.logistics.merchant-id:2000132}")
    private String merchantId;

    @Value("${ecpay.logistics.hash-key:5294y06JbISpM5x9}")
    private String hashKey;

    @Value("${ecpay.logistics.hash-iv:v77hoKGq4kWxNNIS}")
    private String hashIv;

    @Value("${ecpay.logistics.store-list-url:https://logistics-stage.ecpay.com.tw/Helper/GetStoreList}")
    private String storeListUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** chain 對應 ECPay CvsType：FAMI(全家) / UNIMART(7-11) / HILIFE(萊爾富)。 */
    public List<CvsStore> getStores(String chain) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("PlatformID", "");
        params.put("MerchantID", merchantId);
        params.put("CvsType", chain);
        params.put("CheckMacValue", EcpayLogisticsCheckMacValueUtil.generate(params, hashKey, hashIv));

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(storeListUrl))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody(params), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = objectMapper.readTree(response.body());

            if (root.path("RtnCode").asInt() != 1) {
                throw new RuntimeException("ECPay 門市清單查詢失敗: " + root.path("RtnMsg").asText());
            }

            List<CvsStore> stores = new ArrayList<>();
            for (JsonNode group : root.path("StoreList")) {
                for (JsonNode store : group.path("StoreInfo")) {
                    stores.add(new CvsStore(
                            store.path("StoreId").asText(),
                            store.path("StoreName").asText(),
                            store.path("StoreAddr").asText(),
                            store.path("StorePhone").asText()
                    ));
                }
            }
            return stores;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("呼叫 ECPay GetStoreList 失敗", e);
        }
    }

    private String formBody(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> {
            if (!sb.isEmpty()) sb.append('&');
            sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8));
        });
        return sb.toString();
    }

    public record CvsStore(String storeId, String storeName, String address, String phone) {
    }
}