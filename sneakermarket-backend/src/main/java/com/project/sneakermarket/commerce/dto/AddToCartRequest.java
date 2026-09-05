package com.project.sneakermarket.commerce.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddToCartRequest {
    private Long memberId;
    private Long variantId;
    private Integer quantity;
}