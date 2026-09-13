package com.project.sneakermarket.auction.entity;

/**
 * 競標結果狀態列舉。
 * 用來記錄競標結束後的最終結果。
 */
public enum AuctionResultStatus {

    // 已成交，有有效出價並產生得標者
    SOLD,

    // 流標，競標結束時沒有任何有效出價
    UNSOLD,

    // 競標被取消
    CANCELLED,

    // 競標在平台下架期間到達截止時間，交易不成立的廢標結果
    REMOVED

}