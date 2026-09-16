package com.project.sneakermarket.auction.realtime;

/** 第一層公開競標即時事件。 */
public enum AuctionRealtimeEventType {
    BID_PLACED,
    AUCTION_STARTED,
    AUCTION_SOLD,
    AUCTION_UNSOLD,
    AUCTION_CANCELLED,
    AUCTION_REMOVED
}
