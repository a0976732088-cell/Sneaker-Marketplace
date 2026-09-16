package com.project.sneakermarket.auction.realtime;

import com.project.sneakermarket.auction.dto.AuctionStatusResponse;

/**
 * 公開 STOMP payload。
 *
 * data 沿用不含會員識別資訊的 AuctionStatusResponse，禁止直接序列化 Entity。
 */
public record AuctionRealtimeMessage(
        AuctionRealtimeEventType eventType,
        AuctionStatusResponse data
) {
}
