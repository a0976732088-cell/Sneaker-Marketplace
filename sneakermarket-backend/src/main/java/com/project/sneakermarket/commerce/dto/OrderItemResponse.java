package com.project.sneakermarket.commerce.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

// 訂單[明細]裡一筆商品的資料(一張訂單可能有好幾筆)
@Getter
@AllArgsConstructor
public class OrderItemResponse {
    private Long orderItemId;
    private String productName;
    private String specification;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineTotal; // unitPrice × quantity
}