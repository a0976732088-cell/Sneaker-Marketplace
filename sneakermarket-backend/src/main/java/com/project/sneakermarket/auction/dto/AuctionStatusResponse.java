package com.project.sneakermarket.auction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.project.sneakermarket.auction.entity.AuctionStatus;

/**
 * 競標寫入完成後回傳的最新公開狀態。
 *
 * 不包含賣家、最高出價者等會員識別資訊。
 */
public record AuctionStatusResponse(
        Long auctionId,
        BigDecimal currentPrice,
        Integer bidCount,
        BigDecimal nextMinimumBid,
        AuctionStatus status,
        LocalDateTime endAt,
        LocalDateTime updatedAt
) {
}
