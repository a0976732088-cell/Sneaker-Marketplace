package com.project.sneakermarket.auction.logistics.ecpay;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Component;

/** ECPay 物流 API 使用的 .NET 相容 URL encode + MD5 CheckMacValue。 */
@Component
public class AuctionEcpayCheckMacValueService {

    public String calculate(Map<String, String> parameters, String hashKey, String hashIv) {
        TreeMap<String, String> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (parameters != null) {
            parameters.forEach((key, value) -> {
                if (key != null && !"CheckMacValue".equalsIgnoreCase(key)) {
                    sorted.put(key, value == null ? "" : value);
                }
            });
        }

        StringBuilder raw = new StringBuilder("HashKey=").append(requireCredential(hashKey));
        sorted.forEach((key, value) -> raw.append('&').append(key).append('=').append(value));
        raw.append("&HashIV=").append(requireCredential(hashIv));

        String encoded = URLEncoder.encode(raw.toString(), UTF_8)
                .toLowerCase(Locale.ROOT)
                .replace("%2d", "-")
                .replace("%5f", "_")
                .replace("%2e", ".")
                .replace("%21", "!")
                .replace("%2a", "*")
                .replace("%28", "(")
                .replace("%29", ")");
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(encoded.getBytes(UTF_8));
            return HexFormat.of().formatHex(digest).toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JVM 不支援 ECPay 所需的 MD5", ex);
        }
    }

    private String requireCredential(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ECPay CheckMacValue credential 不可為空");
        }
        return value;
    }
}
