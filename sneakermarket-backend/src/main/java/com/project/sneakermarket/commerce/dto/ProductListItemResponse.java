package com.project.sneakermarket.commerce.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
//商品列表頁要用的回應
public class ProductListItemResponse {

    private Long productId;
    private String productName;
    private String brandName;
    private String status;
}
