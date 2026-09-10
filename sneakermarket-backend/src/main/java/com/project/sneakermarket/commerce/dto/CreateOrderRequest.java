package com.project.sneakermarket.commerce.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderRequest {
    private Long memberId;
    private String receiverName;
    private String receiverPhone;
    private String shippingAddress;
}
