package com.project.sneakermarket.auction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.project.sneakermarket.auction.entity.AuctionPaymentStatus;
import com.project.sneakermarket.auction.entity.AuctionShippingStatus;
import com.project.sneakermarket.auction.entity.AuctionOrderStatus;

public record AuctionOrderSummaryResponse(
        String orderNumber, Long auctionId, String productName,
        BigDecimal finalPrice, String paymentMethod,
        AuctionPaymentStatus paymentStatus, AuctionShippingStatus shippingStatus,
        AuctionOrderStatus orderStatus, LocalDateTime createdAt, LocalDateTime updatedAt,
        String model, String size, String imageUrl, LocalDateTime paymentDueAt,
        AuctionAdminManagementState managementState) {
}
