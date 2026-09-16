package com.project.sneakermarket.auction.notification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    public void publishStarted(
            Long auctionId,
            Long sellerId,
            LocalDateTime occurredAt) {
        publishAuctionLifecycleEvent(
                AuctionPrivateNotificationKind.STARTED,
                auctionId,
                null,
                sellerId,
                List.of(),
                null,
                null,
                occurredAt
        );
    }

    public void publishSold(
            Long auctionId,
            Long sellerId,
            Long winnerId,
            List<Long> participantIds,
            BigDecimal finalPrice,
            LocalDateTime occurredAt) {
        publishAuctionLifecycleEvent(
                AuctionPrivateNotificationKind.SOLD,
                auctionId,
                winnerId,
                sellerId,
                participantIds,
                null,
                finalPrice,
                occurredAt
        );
    }

    public void publishUnsold(
            Long auctionId,
            Long sellerId,
            LocalDateTime occurredAt) {
        publishAuctionLifecycleEvent(
                AuctionPrivateNotificationKind.UNSOLD,
                auctionId,
                null,
                sellerId,
                List.of(),
                null,
                null,
                occurredAt
        );
    }

    public void publishCancelled(
            Long auctionId,
            Long sellerId,
            List<Long> participantIds,
            String reason,
            LocalDateTime occurredAt) {
        publishAuctionLifecycleEvent(
                AuctionPrivateNotificationKind.CANCELLED,
                auctionId,
                null,
                sellerId,
                participantIds,
                reason,
                null,
                occurredAt
        );
    }

    public void publishRemoved(
            Long auctionId,
            Long sellerId,
            List<Long> participantIds,
            String reason,
            LocalDateTime occurredAt) {
        publishAuctionLifecycleEvent(
                AuctionPrivateNotificationKind.REMOVED,
                auctionId,
                null,
                sellerId,
                participantIds,
                reason,
                null,
                occurredAt
        );
    }

    public void publishRestored(
            Long auctionId,
            Long sellerId,
            List<Long> participantIds,
            String reason,
            LocalDateTime occurredAt) {
        publishAuctionLifecycleEvent(
                AuctionPrivateNotificationKind.RESTORED,
                auctionId,
                null,
                sellerId,
                participantIds,
                reason,
                null,
                occurredAt
        );
    }

    private void publishAuctionLifecycleEvent(
            AuctionPrivateNotificationKind kind,
            Long auctionId,
            Long memberId,
            Long sellerId,
            List<Long> participantIds,
            String reason,
            BigDecimal amount,
            LocalDateTime occurredAt) {
        events.publishEvent(new AuctionPrivateNotificationReadyEvent(
                kind,
                auctionId,
                null,
                null,
                memberId,
                sellerId,
                participantIds,
                reason,
                amount,
                occurredAt
        ));
    }

    public void publishOrderCreated(
            Long auctionId,
            Long orderId,
            String orderNumber,
            Long buyerId,
            Long sellerId,
            LocalDateTime occurredAt) {
        publishOrderEvent(
                AuctionPrivateNotificationKind.ORDER_CREATED,
                auctionId,
                orderId,
                orderNumber,
                buyerId,
                sellerId,
                occurredAt
        );
    }

    public void publishOrderPaid(
            Long auctionId,
            Long orderId,
            String orderNumber,
            Long buyerId,
            Long sellerId,
            LocalDateTime occurredAt) {
        publishOrderEvent(
                AuctionPrivateNotificationKind.ORDER_PAID,
                auctionId,
                orderId,
                orderNumber,
                buyerId,
                sellerId,
                occurredAt
        );
    }

    public void publishOrderAbandoned(
            Long auctionId,
            Long orderId,
            String orderNumber,
            Long buyerId,
            Long sellerId,
            LocalDateTime occurredAt) {
        publishOrderEvent(
                AuctionPrivateNotificationKind.ORDER_ABANDONED,
                auctionId,
                orderId,
                orderNumber,
                buyerId,
                sellerId,
                occurredAt
        );
    }

    private void publishOrderEvent(
            AuctionPrivateNotificationKind kind,
            Long auctionId,
            Long orderId,
            String orderNumber,
            Long buyerId,
            Long sellerId,
            LocalDateTime occurredAt) {
        events.publishEvent(new AuctionPrivateNotificationReadyEvent(
                kind,
                auctionId,
                orderId,
                orderNumber,
                buyerId,
                sellerId,
                null,
                occurredAt
        ));
    }
}
