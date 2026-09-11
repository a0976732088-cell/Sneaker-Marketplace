package com.project.sneakermarket.auction.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 建立競標出價 Request DTO。
 *
 * 出價者身分與競標狀態一律由後端取得，
 * Request 只接受本次出價金額。
 */
public record BidCreateRequest(

        @NotNull(message = "出價金額不可為空")
        @Positive(message = "出價金額必須大於 0")
        @Digits(integer = 10, fraction = 2, message = "出價金額格式不正確")
        BigDecimal bidAmount

) {

    /**
     * 此 Request 僅允許 bidAmount，避免前端夾帶 bidderId 等系統控制欄位。
     */
    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object value) {
        throw new IllegalArgumentException("出價 Request 不允許欄位：" + fieldName);
    }
}
