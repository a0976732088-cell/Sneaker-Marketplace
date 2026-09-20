package com.project.sneakermarket.auction.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 付款前僅允許更新競標訂單的收件快照。 */
public record AuctionOrderCheckoutUpdateRequest(
        @NotBlank(message = "收件人姓名不可空白")
        @Size(max = 50, message = "收件人姓名不可超過 50 個字")
        String receiverName,

        @NotBlank(message = "收件人電話不可空白")
        @Size(max = 20, message = "收件人電話不可超過 20 個字")
        String receiverPhone,

        @NotBlank(message = "配送地址不可空白")
        @Size(max = 300, message = "配送地址不可超過 300 個字")
        String shippingAddress) {

    /** 防止 client 夾帶價格、身分、付款方式或期限等交易欄位。 */
    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object value) {
        throw new IllegalArgumentException("競標訂單 Checkout Request 不允許欄位：" + fieldName);
    }
}
