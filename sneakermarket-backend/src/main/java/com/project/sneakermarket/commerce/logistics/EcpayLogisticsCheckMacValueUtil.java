package com.project.sneakermarket.commerce.logistics;

import com.project.sneakermarket.commerce.payment.ECPayCheckMacValueUtil;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

/**
 * ECPay 物流 API (GetStoreList 等) 使用的 CheckMacValue。
/* 跟金流付款用的 {@link ECPayCheckMacValueUtil} 不同 —— 物流這套官方規格是 MD5,不是 SHA-256,
 * 兩支不能共用,參數排序/URL encode 規則相同但雜湊演算法不同。*/
public class EcpayLogisticsCheckMacValueUtil {

    public static String generate(Map<String, String> params, String hashKey, String hashIv) {
        try {
            TreeMap<String, String> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            sorted.putAll(params);
            sorted.remove("CheckMacValue");

            StringBuilder raw = new StringBuilder();
            raw.append("HashKey=").append(hashKey);
            for (Map.Entry<String, String> entry : sorted.entrySet()) {
                raw.append("&").append(entry.getKey()).append("=").append(entry.getValue());
            }
            raw.append("&HashIV=").append(hashIv);

            String encoded = URLEncoder.encode(raw.toString(), StandardCharsets.UTF_8.toString());
            encoded = fixEncoding(encoded).toLowerCase();

            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashBytes = digest.digest(encoded.getBytes(StandardCharsets.UTF_8));

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