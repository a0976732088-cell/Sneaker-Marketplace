package com.project.sneakermarket.auction.notification;

/** 傳給目前登入會員的最小私人即時事件種類。 */
public enum AuctionPrivateRealtimeEventType {
    AUCTION_OUTBID,
    AUCTION_STARTED,
    AUCTION_SOLD,
    AUCTION_WON,
    AUCTION_LOST,
    AUCTION_UNSOLD,
    AUCTION_CANCELLED,
    AUCTION_REMOVED,
    AUCTION_RESTORED,
    AUCTION_ORDER_CREATED,
    AUCTION_ORDER_PAID,
    AUCTION_ORDER_ABANDONED
}
