package com.project.sneakermarket.commerce.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
//商品[規格資料]
public class ProductVariantResponse {

    private String size;
    private String color;
    private BigDecimal price;
    private Integer stock;
    private Long variantId;
}
