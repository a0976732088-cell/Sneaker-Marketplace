package com.project.sneakermarket.commerce.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
//後台「接收」修改某筆商品其一規格
public class UpdateVariantRequest {
    private String size;
    private String color;
    private BigDecimal price;
    private Integer stock;
    private Long variantId;

}
