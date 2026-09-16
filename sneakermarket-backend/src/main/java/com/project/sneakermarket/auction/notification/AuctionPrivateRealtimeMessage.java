package com.project.sneakermarket.auction.notification;

import java.time.LocalDateTime;

/** WebSocket 只通知 Navbar 重新同步 REST，不複製完整 Notification。 */
public record AuctionPrivateRealtimeMessage(
        AuctionPrivateRealtimeEventType eventType,
        Long auctionId,
        Long notificationId,
        LocalDateTime occurredAt
) {
}
