package com.project.sneakermarket.commerce.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
//[購物車頁面]要用的回應
@Getter
@AllArgsConstructor
public class CartItemResponse {
    private Long cartItemId;
    private String productName;
    private String imageUrl;
    private String size;
    private String color;
    private Integer quantity;
    private Boolean selected;
    private BigDecimal price;
    private Integer stock;
}
