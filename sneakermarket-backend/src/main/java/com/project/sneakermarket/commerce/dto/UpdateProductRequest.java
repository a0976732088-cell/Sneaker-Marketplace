package com.project.sneakermarket.commerce.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
//後台「接收」修改某筆整筆商品資料
public class UpdateProductRequest {
    private Long categoryId;
    private Long brandId;
    private String productName;
    private String description;
    private List<UpdateVariantRequest> variants;
    private List<ProductImageRequest> images;
    private String status;
}