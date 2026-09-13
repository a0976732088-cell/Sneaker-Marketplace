package com.project.sneakermarket.auction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.project.sneakermarket.auction.entity.AuctionStatus;

public record AuctionAdminSummaryResponse(
        Long auctionId,
        String productName,
        String model,
        String imageUrl,
        Long sellerId,
        BigDecimal currentPrice,
        Integer bidCount,
        AuctionStatus status,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime createdAt,
        LocalDateTime removedAt,
        Long removedByMemberId,
        String removeReason,
        AuctionStatus removedFromStatus,
        LocalDateTime deletedAt,
        Long deletedByMemberId,
        String deleteReason,
        AuctionAdminManagementState managementState
) {
}
