package com.project.sneakermarket.auction.payment.linepay;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.sneakermarket.auction.exception.AuctionIntegrationException;

@Component
public class LinePayClient {
    private static final String REQUEST_PATH = "/v4/payments/request";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration CHECK_TIMEOUT = Duration.ofSeconds(25);
    private static final Duration CONFIRM_TIMEOUT = Duration.ofSeconds(45);

    private final AuctionLinePaySettings settings;
    private final LinePaySignatureService signatures;
    private volatile HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public LinePayClient(AuctionLinePaySettings settings, LinePaySignatureService signatures) {
        // 不在 ApplicationContext 啟動時建立 selector/socket；只有真的呼叫 LINE Pay 才初始化。
        this(settings, signatures, null, new ObjectMapper());
    }

    public LinePayClient(
            AuctionLinePaySettings settings,
            LinePaySignatureService signatures,
            HttpClient httpClient,
            ObjectMapper objectMapper) {
        this.settings = settings;
        this.signatures = signatures;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public RequestResult requestPayment(RequestCommand command) {
        Map<String, Object> product = new LinkedHashMap<>();
        product.put("id", command.product().id());
        product.put("name", command.product().name());
        product.put("quantity", command.product().quantity());
        product.put("price", command.product().price());

        Map<String, Object> paymentPackage = new LinkedHashMap<>();
        paymentPackage.put("id", command.merchantOrderId());
        paymentPackage.put("amount", command.amount());
        paymentPackage.put("products", List.of(product));

        Map<String, Object> redirectUrls = new LinkedHashMap<>();
        redirectUrls.put("confirmUrlType", "NONE");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", command.amount());
        body.put("currency", command.currency());
        body.put("orderId", command.merchantOrderId());
        body.put("packages", List.of(paymentPackage));
        body.put("redirectUrls", redirectUrls);

        JsonNode json = post(REQUEST_PATH, serializeOnce(body), REQUEST_TIMEOUT);
        JsonNode info = json.path("info");
        return new RequestResult(
                json.path("returnCode").asText(""),
                json.path("returnMessage").asText(""),
                textOrNull(info.path("transactionId")),
                textOrNull(info.path("paymentUrl").path("web")));
    }

    public ConfirmResult confirmPayment(String transactionId, long amount, String currency) {
        requireTransactionId(transactionId);
        String apiPath = "/v4/payments/" + transactionId + "/confirm";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amount);
        body.put("currency", currency);
        JsonNode json = post(apiPath, serializeOnce(body), CONFIRM_TIMEOUT);
        return new ConfirmResult(
                json.path("returnCode").asText(""),
                json.path("returnMessage").asText(""));
    }

    public CheckStatusResult checkPaymentRequestStatus(String transactionId) {
        requireTransactionId(transactionId);
        String apiPath = "/v4/payments/requests/" + transactionId + "/check";
        JsonNode json = get(apiPath, "", CHECK_TIMEOUT);
        return new CheckStatusResult(
                json.path("returnCode").asText(""),
                json.path("returnMessage").asText(""));
    }

    private String serializeOnce(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            throw AuctionIntegrationException.provider("無法建立 LINE Pay 請求");
        }
    }

    /** 同一個 body String 同時用於 signature 與真正送出的 HttpRequest。 */
    private JsonNode post(String apiPath, String requestBody, Duration timeout) {
        settings.requireProviderConfigured();
        String nonce = UUID.randomUUID().toString();
        String signature = signatures.sign(apiPath, requestBody, nonce, settings.channelSecret());
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(settings.apiUrl(apiPath)))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("X-LINE-ChannelId", settings.channelId())
                    .header("X-LINE-Authorization", signature)
                    .header("X-LINE-Authorization-Nonce", nonce)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, UTF_8))
                    .build();
        } catch (IllegalArgumentException ex) {
            throw AuctionIntegrationException.unavailable("競標 LINE Pay API 網址設定不正確");
        }

        return send(request);
    }

    private JsonNode get(String apiPath, String queryString, Duration timeout) {
        settings.requireProviderConfigured();
        String nonce = UUID.randomUUID().toString();
        String signature = signatures.signGet(
                apiPath, queryString, nonce, settings.channelSecret());
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(settings.apiUrl(apiPath) + queryString))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("X-LINE-ChannelId", settings.channelId())
                    .header("X-LINE-Authorization", signature)
                    .header("X-LINE-Authorization-Nonce", nonce)
                    .GET()
                    .build();
        } catch (IllegalArgumentException ex) {
            throw AuctionIntegrationException.unavailable("競標 LINE Pay API 網址設定不正確");
        }

        return send(request);
    }

    private JsonNode send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient().send(
                    request, HttpResponse.BodyHandlers.ofString(UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw AuctionIntegrationException.provider("LINE Pay 服務目前無法完成請求");
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw AuctionIntegrationException.provider("LINE Pay 連線已中斷");
        } catch (IOException | RuntimeException ex) {
            if (ex instanceof AuctionIntegrationException integration) throw integration;
            throw AuctionIntegrationException.provider("LINE Pay 服務目前無法完成請求");
        }
    }

    private void requireTransactionId(String transactionId) {
        if (transactionId == null || !transactionId.matches("[A-Za-z0-9_-]{1,100}")) {
            throw AuctionIntegrationException.provider("LINE Pay transactionId 格式不正確");
        }
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
                    throw AuctionIntegrationException.provider("LINE Pay 連線初始化失敗");
                }
                httpClient = current;
            }
            return current;
        }
    }

    private String textOrNull(JsonNode value) {
        String text = value.isMissingNode() || value.isNull() ? null : value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    public record Product(String id, String name, int quantity, long price) {
    }

    public record RequestCommand(
            long amount,
            String currency,
            String merchantOrderId,
            Product product) {
    }

    public record RequestResult(
            String returnCode,
            String returnMessage,
            String transactionId,
            String paymentUrl) {
    }

    public record ConfirmResult(String returnCode, String returnMessage) {
    }

    public record CheckStatusResult(String returnCode, String returnMessage) {
    }
}
