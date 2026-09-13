package com.project.sneakermarket.auction.entity;

/**
 * 競標狀態列舉。
 * 用來表示一場競標從建立到結束期間的各種狀態。
 */
public enum AuctionStatus {

    // 草稿，尚未公開
    DRAFT,

    // 已設定競標時間，但尚未開始
    SCHEDULED,

    // 競標進行中，可接受會員出價
    ACTIVE,

    // 競標結束且已有得標者
    SOLD,

    // 競標結束，但沒有任何有效出價
    UNSOLD,

    // 競標由賣家或系統取消
    CANCELLED,

    // 競標在平台下架期間到達截止時間，由 Lifecycle 判定為廢標
    REMOVED

}