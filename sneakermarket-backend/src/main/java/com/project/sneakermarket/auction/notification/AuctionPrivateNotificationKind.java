package com.project.sneakermarket.auction.notification;

/** 交易內發布、由 AFTER_COMMIT listener 展開的私人通知工作類型。 */
public enum AuctionPrivateNotificationKind {
    OUTBID,
    STARTED,
    SOLD,
    UNSOLD,
    CANCELLED,
    REMOVED,
    RESTORED,
    ORDER_CREATED,
    ORDER_PAID,
    ORDER_ABANDONED
}
