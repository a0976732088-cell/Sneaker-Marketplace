package com.project.sneakermarket.commerce.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

//訂單[列表頁]
@Getter
@AllArgsConstructor
public class OrderListItemResponse {
    private Long orderId;
    private String orderNumber;
    private String status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private String firstProductName;   // 第一件商品名稱
    private String firstImageUrl;      // 第一件商品主圖（可能是 null）
    private Integer itemCount;         // 總件數
}