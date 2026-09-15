package com.project.sneakermarket.auction.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record AuctionOrderSettings(long paymentTimeoutHours) {
    public AuctionOrderSettings(
            @Value("${auction.order.payment-timeout-hours:24}") long paymentTimeoutHours) {
        if (paymentTimeoutHours <= 0) {
            throw new IllegalArgumentException("auction.order.payment-timeout-hours must be positive");
        }
        this.paymentTimeoutHours = paymentTimeoutHours;
    }
}
