package com.project.sneakermarket.auction.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AuctionAdminBatchRequest(
        @NotEmpty(message = "auctionIds 不可為空")
        @Size(max = 100, message = "一次最多處理 100 筆競標")
        List<@NotNull(message = "auctionId 不可為空") @Positive(message = "auctionId 必須為正數") Long> auctionIds,

        @NotBlank(message = "管理原因不可空白")
        @Size(max = 500, message = "管理原因不可超過 500 字")
        String reason
) {
    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object value) {
        throw new IllegalArgumentException("批次管理 Request 不允許欄位：" + fieldName);
    }
}
