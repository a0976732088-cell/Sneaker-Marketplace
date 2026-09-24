package com.project.sneakermarket.commerce.linepay;

import com.project.sneakermarket.commerce.linepay.CommerceLinePayPayloads.ApiResult;
import com.project.sneakermarket.commerce.linepay.CommerceLinePayPayloads.ConfirmBody;
import com.project.sneakermarket.commerce.linepay.CommerceLinePayPayloads.ConfirmInfo;
import com.project.sneakermarket.commerce.linepay.CommerceLinePayPayloads.PaymentRequestBody;
import com.project.sneakermarket.commerce.linepay.CommerceLinePayPayloads.RequestInfo;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * 專門負責「跟 LINE Pay 伺服器講話」，不碰訂單、不碰資料庫。
 *
 * 簽章做法：在 RestClient 上掛一個攔截器，送出前拿到「真正要送出的 body bytes」再算簽章，
 * 這樣簽章用的內容和實際送出的內容保證一模一樣。
 */
@Component
public class CommerceLinePayGateway {

    private static final String CURRENCY = "TWD";

    private final CommerceLinePayProperties props;
    private final RestClient restClient;

    public CommerceLinePayGateway(CommerceLinePayProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
                .baseUrl(props.getApiBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor(this::attachSignature)
                .build();
    }

    /** 第一步：向 LINE Pay 申請一筆付款，成功會拿到 transactionId 和付款頁網址 */
    public ApiResult<RequestInfo> requestPayment(PaymentRequestBody body) {
        checkKeys();
        return restClient.post()
                .uri("/v4/payments/request")
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResult<RequestInfo>>() {});
    }

    /** 第二步：使用者付完款被導回來後，由我們向 LINE Pay 確認扣款 */
    public ApiResult<ConfirmInfo> confirmPayment(Long transactionId, long amount) {
        checkKeys();
        return restClient.post()
                .uri("/v4/payments/{transactionId}/confirm", transactionId)
                .body(new ConfirmBody(amount, CURRENCY))
                .retrieve()
                .body(new ParameterizedTypeReference<ApiResult<ConfirmInfo>>() {});
    }

    public String currency() {
        return CURRENCY;
    }

    // ---------- 以下為內部工具 ----------

    private void checkKeys() {
        if (!props.isReady()) {
            throw new IllegalStateException("商城 LINE Pay 金鑰未設定，請檢查 COMMERCE_LINE_PAY_* 環境變數");
        }
    }

    /**
     * 每次送出前都會經過這裡，補上 LINE Pay 規定的三個 header。
     * 簽章內容 = Secret + API 路徑 + body + nonce（目前只用到 POST；若之後要打 GET，body 的位置要換成 query string）
     */
    private ClientHttpResponse attachSignature(HttpRequest request, byte[] body,
                                               ClientHttpRequestExecution execution) throws IOException {
        String secret = props.getChannelSecret();
        String nonce = UUID.randomUUID().toString();
        String apiPath = request.getURI().getRawPath();
        String bodyText = new String(body, StandardCharsets.UTF_8);

        HttpHeaders headers = request.getHeaders();
        headers.set("X-LINE-ChannelId", props.getChannelId());
        headers.set("X-LINE-Authorization-Nonce", nonce);
        headers.set("X-LINE-Authorization", hmacSha256Base64(secret, secret + apiPath + bodyText + nonce));

        return execution.execute(request, body);
    }

    private static String hmacSha256Base64(String key, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("產生 LINE Pay 簽章失敗", e);
        }
    }
}