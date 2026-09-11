package com.project.sneakermarket.auction.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 資料庫以 auction 聚合後的一列，僅供會員查詢 Service 使用。 */
public interface AuctionMemberBidSummary {
    Long getAuctionId();
    BigDecimal getMyHighestBid();
    LocalDateTime getLastBidAt();
}
