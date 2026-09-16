package com.project.sneakermarket.auction.realtime;

/** 交易內發布、提交後才由 listener 廣播的 immutable event snapshot。 */
public record AuctionRealtimeEvent(AuctionRealtimeMessage message) {
}
