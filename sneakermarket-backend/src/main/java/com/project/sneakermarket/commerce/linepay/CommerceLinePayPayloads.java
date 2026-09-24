package com.project.sneakermarket.commerce.linepay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 跟 LINE Pay API 往來的資料格式，全部集中在這裡。
 * record 的欄位名稱 = JSON 的 key，所以名稱不能亂改。
 */
public final class CommerceLinePayPayloads {

    private CommerceLinePayPayloads() {
    }

    // ===== 我們送出去的 =====

    /** POST /v4/payments/request 的 body */
    public record PaymentRequestBody(
            long amount,
            String currency,
            String orderId,
            List<PackageItem> packages,
            RedirectUrls redirectUrls) {
    }

    /** 一個包裹；amount 必須等於底下所有商品 quantity × price 的加總 */
    public record PackageItem(String id, long amount, List<ProductLine> products) {
    }

    public record ProductLine(String name, int quantity, long price) {
    }

    /** 付款完成 / 取消後，LINE Pay 要把使用者帶回哪個前端頁面 */
    public record RedirectUrls(String confirmUrl, String cancelUrl) {
    }

    /** POST /v4/payments/{transactionId}/confirm 的 body */
    public record ConfirmBody(long amount, String currency) {
    }

    // ===== LINE Pay 回給我們的 =====

    /** 所有 API 共用的外層格式；returnCode 是 "0000" 才代表成功 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApiResult<T>(String returnCode, String returnMessage, T info) {
        public boolean isSuccess() {
            return "0000".equals(returnCode);
        }
    }

    /** request 成功時 info 裡的內容；transactionId 是 19 位數，後端用 Long 裝得下 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RequestInfo(Long transactionId, PaymentUrl paymentUrl) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentUrl(String web) {
    }

    /** confirm 成功時 info 裡的內容 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ConfirmInfo(String orderId, Long transactionId) {
    }
}