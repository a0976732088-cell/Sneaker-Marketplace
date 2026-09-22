package com.project.sneakermarket.auction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.project.sneakermarket.auction.entity.AuctionOrderStatus;
import com.project.sneakermarket.auction.entity.AuctionPaymentStatus;
import com.project.sneakermarket.auction.entity.AuctionShippingStatus;

public record AuctionOrderDetailResponse(
        String orderNumber, Long auctionId, String productName,
        Long brandId, String brandName, String model, String size, String imageUrl,
        BigDecimal finalPrice, BigDecimal shippingFee, BigDecimal totalAmount,
        String shippingMethod, String paymentMethod,
        AuctionPaymentStatus paymentStatus, AuctionShippingStatus shippingStatus,
        AuctionOrderStatus orderStatus,
        AuctionOrderViewerRole viewerRole, boolean receiverInfoVisible,
        String receiverName, String receiverPhone, String shippingAddress,
        LocalDateTime createdAt, LocalDateTime paymentDueAt, LocalDateTime abandonedAt,
        LocalDateTime paidAt, LocalDateTime updatedAt) {
}
