package com.project.sneakermarket.auction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.project.sneakermarket.auction.entity.AuctionStatus;
import com.project.sneakermarket.auction.entity.AuctionResultStatus;

public record AuctionMyListingResponse(
        Long auctionId, String productName, String model, String size,
        BigDecimal currentPrice, Integer bidCount, AuctionStatus status,
        AuctionAdminManagementState managementState,
        AuctionResultStatus resultStatus, BigDecimal finalPrice,
        LocalDateTime createdAt, LocalDateTime startAt, LocalDateTime endAt) {
}
