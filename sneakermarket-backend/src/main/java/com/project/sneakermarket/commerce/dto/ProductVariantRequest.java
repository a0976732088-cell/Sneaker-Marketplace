package com.project.sneakermarket.commerce.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
//後台「接收」商品[規格資料]
public class ProductVariantRequest {

    private String size;
    private String color;
    private BigDecimal price;
    private Integer stock;
}
