package com.project.sneakermarket.auction.notification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** 讓競標交易 service 只發布私人通知工作，不直接接觸通知表或 broker。 */
@Component
@RequiredArgsConstructor
public class AuctionPrivateNotificationEventPublisher {

    private final ApplicationEventPublisher events;

    public void publishOutbid(
            Long auctionId,
            Long previousHighestBidderId,
            BigDecimal currentPrice,
            LocalDateTime occurredAt) {
        events.publishEvent(new AuctionPrivateNotificationReadyEvent(
                AuctionPrivateNotificationKind.OUTBID,
                auctionId,
                previousHighestBidderId,
                currentPrice,
                occurredAt
        ));
    }

    public void publishSold(
            Long auctionId,
            Long winnerId,
            BigDecimal finalPrice,
            LocalDateTime occurredAt) {
        events.publishEvent(new AuctionPrivateNotificationReadyEvent(
                AuctionPrivateNotificationKind.SOLD,
                auctionId,
                winnerId,
                finalPrice,
                occurredAt
        ));
    }
}
