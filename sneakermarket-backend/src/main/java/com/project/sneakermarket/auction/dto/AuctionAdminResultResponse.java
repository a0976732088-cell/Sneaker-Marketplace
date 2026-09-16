package com.project.sneakermarket.auction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.project.sneakermarket.auction.entity.AuctionResultStatus;

/** 管理員競標詳情中的唯讀結標結果；不暴露 winnerId 或會員內部資料。 */
public record AuctionAdminResultResponse(
        AuctionResultStatus resultStatus,
        BigDecimal finalPrice,
        String winnerName,
        LocalDateTime endedAt,
        LocalDateTime createdAt,
        String reason
) {
}
