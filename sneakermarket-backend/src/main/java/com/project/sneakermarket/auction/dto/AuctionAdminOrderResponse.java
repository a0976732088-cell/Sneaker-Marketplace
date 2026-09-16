package com.project.sneakermarket.auction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.project.sneakermarket.auction.entity.AuctionOrderStatus;
import com.project.sneakermarket.auction.entity.AuctionPaymentStatus;
import com.project.sneakermarket.auction.entity.AuctionShippingStatus;

/** 管理員競標詳情中的唯讀訂單生命週期；刻意排除收件地址、電話與會員 ID。 */
public record AuctionAdminOrderResponse(
        String orderNumber,
        AuctionOrderStatus orderStatus,
        AuctionPaymentStatus paymentStatus,
        AuctionShippingStatus shippingStatus,
        BigDecimal finalPrice,
        String buyerName,
        String sellerName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime paymentDueAt,
        LocalDateTime paidAt,
        LocalDateTime abandonedAt,
        LocalDateTime shippedAt,
        LocalDateTime completedAt
) {
}
