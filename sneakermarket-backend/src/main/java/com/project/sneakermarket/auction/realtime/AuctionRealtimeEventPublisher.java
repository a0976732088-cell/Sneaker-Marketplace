package com.project.sneakermarket.auction.realtime;

import java.math.BigDecimal;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.project.sneakermarket.auction.dto.AuctionStatusResponse;
import com.project.sneakermarket.auction.entity.Auction;

import lombok.RequiredArgsConstructor;

/** 將交易內已更新完成的 Auction 轉成公開、不可變的事件快照。 */
@Component
@RequiredArgsConstructor
public class AuctionRealtimeEventPublisher {

    private final ApplicationEventPublisher events;

    public void publish(AuctionRealtimeEventType eventType, Auction auction) {
        AuctionStatusResponse data = new AuctionStatusResponse(
                auction.getAuctionId(),
                auction.getCurrentPrice(),
                auction.getBidCount(),
                calculateNextMinimumBid(auction),
                auction.getStatus(),
                auction.getEndAt(),
                auction.getUpdatedAt()
        );
        events.publishEvent(new AuctionRealtimeEvent(
                new AuctionRealtimeMessage(eventType, data)
        ));
    }

    private BigDecimal calculateNextMinimumBid(Auction auction) {
        return auction.getCurrentPrice().add(auction.getMinIncrement());
    }
}
