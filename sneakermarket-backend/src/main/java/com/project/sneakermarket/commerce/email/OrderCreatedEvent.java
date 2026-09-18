package com.project.sneakermarket.commerce.email;

public class OrderCreatedEvent {

    private final Long orderId;
    private final Long memberId;

    public OrderCreatedEvent(Long orderId, Long memberId) {
        this.orderId = orderId;
        this.memberId = memberId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getMemberId() {
        return memberId;
    }
}