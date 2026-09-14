package com.project.sneakermarket.commerce.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
//商品[列表頁]要用的回應
public class ProductListItemResponse {

    private Long productId;
    private String productName;
    private String brandName;
    private String status;
    private BigDecimal price;
    private String imageUrl;
    private Integer totalStock;
}
