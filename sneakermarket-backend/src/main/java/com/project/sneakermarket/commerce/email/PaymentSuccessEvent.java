package com.project.sneakermarket.commerce.email;

//付款成功後告訴 Email Listener
public class PaymentSuccessEvent {

    private final Long orderId;
    private final Long memberId;

    public PaymentSuccessEvent(Long orderId, Long memberId) {
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