package com.project.sneakermarket.auction.email;

/** 僅傳 ID；Listener 在主交易提交後重新檢查 readiness。 */
public record AuctionEmailReadyEvent(Long auctionId) {}
