package com.project.sneakermarket.commerce.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
//商品[明細頁]要用的回應
public class ProductDetailResponse {

    private Long productId;
    private String productName;
    private String brandName;
    private String status;
    private String description;
    private List<String> images;
    private List<ProductVariantResponse> variants;
}