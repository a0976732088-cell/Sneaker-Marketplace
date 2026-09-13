package com.project.sneakermarket.auction.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuctionAdminActionRequest(
        @NotBlank(message = "管理原因不可空白")
        @Size(max = 500, message = "管理原因不可超過 500 字")
        String reason
) {
    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object value) {
        throw new IllegalArgumentException("管理 Request 不允許欄位：" + fieldName);
    }
}
