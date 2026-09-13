package com.project.sneakermarket.auction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.project.sneakermarket.auction.entity.AuctionStatus;
import com.project.sneakermarket.auction.entity.AuctionResultStatus;

public record AuctionMyBidResponse(
        Long auctionId, String productName, String model, String size,
        BigDecimal myHighestBid, BigDecimal currentPrice, Integer bidCount,
        AuctionStatus status, AuctionAdminManagementState managementState,
        boolean leading, boolean won,
        AuctionResultStatus resultStatus, BigDecimal finalPrice,
        LocalDateTime lastBidAt, LocalDateTime endAt) {
}
