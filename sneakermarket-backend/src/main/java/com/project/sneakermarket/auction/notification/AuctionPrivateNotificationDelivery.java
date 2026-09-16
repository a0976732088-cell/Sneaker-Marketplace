package com.project.sneakermarket.auction.notification;

/** 一筆已提交的 Notification 與其私人 WebSocket 收件人。 */
public record AuctionPrivateNotificationDelivery(
        Long recipientMemberId,
        AuctionPrivateRealtimeMessage message
) {
}
