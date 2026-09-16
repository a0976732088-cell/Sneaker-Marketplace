package com.project.sneakermarket.auction.dto;

import java.time.LocalDateTime;

import com.project.sneakermarket.auction.entity.AuctionOrderStatus;
import com.project.sneakermarket.auction.entity.AuctionPaymentStatus;

public record AuctionOrderPaymentResponse(
        String orderNumber,
        Long auctionId,
        String paymentMethod,
        AuctionPaymentStatus paymentStatus,
        AuctionOrderStatus orderStatus,
        LocalDateTime paidAt,
        LocalDateTime updatedAt) {
}
