package com.project.sneakermarket.commerce.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
//品牌下拉選單用的簡易回應
public class BrandResponse {
    private Long brandId;
    private String brandName;
}