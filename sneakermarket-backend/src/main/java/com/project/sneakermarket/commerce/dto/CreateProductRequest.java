package com.project.sneakermarket.commerce.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
//後台「接收」新增商品資料+規格清單
public class CreateProductRequest {

    private Long categoryId;
    private Long brandId;
    private String productName;
    private String description;
    private List<ProductVariantRequest> variants;
    private List<ProductImageRequest> images;
    private String status;
}
