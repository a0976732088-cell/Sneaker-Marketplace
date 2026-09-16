package com.project.sneakermarket.auction.notification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 不可變的競標私人通知工作快照；只在 server 內傳遞。 */
public record AuctionPrivateNotificationReadyEvent(
        AuctionPrivateNotificationKind kind,
        Long auctionId,
        Long memberId,
        BigDecimal amount,
        LocalDateTime occurredAt
) {
}
