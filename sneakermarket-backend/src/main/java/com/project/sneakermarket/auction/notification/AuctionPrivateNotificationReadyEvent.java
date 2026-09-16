package com.project.sneakermarket.auction.notification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** 不可變的競標私人通知工作快照；只在 server 內傳遞。 */
public record AuctionPrivateNotificationReadyEvent(
        AuctionPrivateNotificationKind kind,
        Long auctionId,
        Long orderId,
        String orderNumber,
        Long memberId,
        Long sellerId,
        List<Long> participantIds,
        String reason,
        BigDecimal amount,
        LocalDateTime occurredAt
) {

    public AuctionPrivateNotificationReadyEvent {
        participantIds = participantIds == null
                ? List.of()
                : List.copyOf(participantIds.stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList());
    }

    /** 保留第三層競標訂單事件既有資料形狀。 */
    public AuctionPrivateNotificationReadyEvent(
            AuctionPrivateNotificationKind kind,
            Long auctionId,
            Long orderId,
            String orderNumber,
            Long memberId,
            Long sellerId,
            BigDecimal amount,
            LocalDateTime occurredAt) {
        this(kind, auctionId, orderId, orderNumber, memberId, sellerId,
                List.of(), null, amount, occurredAt);
    }

    /** 保留第一、二層競標事件既有資料形狀。 */
    public AuctionPrivateNotificationReadyEvent(
            AuctionPrivateNotificationKind kind,
            Long auctionId,
            Long memberId,
            BigDecimal amount,
            LocalDateTime occurredAt) {
        this(kind, auctionId, null, null, memberId, null,
                List.of(), null, amount, occurredAt);
    }
}
