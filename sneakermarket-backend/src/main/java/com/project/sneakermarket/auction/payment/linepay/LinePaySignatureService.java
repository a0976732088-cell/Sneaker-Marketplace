package com.project.sneakermarket.auction.payment.linepay;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.GeneralSecurityException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

@Component
public class LinePaySignatureService {
    public String sign(String apiPath, String requestBody, String nonce, String channelSecret) {
        return hmac(channelSecret + apiPath + requestBody + nonce, channelSecret);
    }

    public String signGet(String apiPath, String queryString, String nonce, String channelSecret) {
        return hmac(channelSecret + apiPath + queryString + nonce, channelSecret);
    }

    private String hmac(String message, String channelSecret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(channelSecret.getBytes(UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes(UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("無法建立 LINE Pay 簽章", ex);
        }
    }
}
