package com.project.sneakermarket.auction.entity;

/**
 * 競標商品狀況列舉。
 * 用來表示賣家刊登競標商品時所選擇的商品新舊程度。
 */
public enum AuctionCondition {

    // 全新商品
    NEW,

    // 近全新商品，有使用痕跡但整體狀況良好
    LIKE_NEW,

    // 二手商品，有明顯使用痕跡
    USED

}