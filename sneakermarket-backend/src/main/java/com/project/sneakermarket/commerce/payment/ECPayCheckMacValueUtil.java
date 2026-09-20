package com.project.sneakermarket.commerce.payment;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

public class ECPayCheckMacValueUtil {

    /**
     * ECPay 金流使用的 SHA-256 CheckMacValue。
     */
    public static String generate(Map<String, String> params, String hashKey, String hashIV) {
        try {
            TreeMap<String, String> sorted = new TreeMap<>(params);
            sorted.remove("CheckMacValue");

            StringBuilder raw = new StringBuilder();
            raw.append("HashKey=").append(hashKey);

            for (Map.Entry<String, String> entry : sorted.entrySet()) {
                raw.append("&")
                        .append(entry.getKey())
                        .append("=")
                        .append(entry.getValue());
            }

            raw.append("&HashIV=").append(hashIV);

            String encoded = URLEncoder.encode(
                    raw.toString(),
                    StandardCharsets.UTF_8.toString()
            );

            encoded = fixEncoding(encoded);

            String lower = encoded.toLowerCase();

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(
                    lower.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder hex = new StringBuilder();

            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString().toUpperCase();

        } catch (Exception e) {
            throw new RuntimeException("CheckMacValue 計算失敗", e);
        }
    }

    // Java 內建的網址編碼跟綠界要求的規則有幾個字元不一樣，要換回來
    private static String fixEncoding(String encoded) {
        return encoded
                .replace("%2D", "-")
                .replace("%5F", "_")
                .replace("%2E", ".")
                .replace("%21", "!")
                .replace("%2A", "*")
                .replace("%28", "(")
                .replace("%29", ")");
    }
}