package com.project.sneakermarket.auction.notification;

/** 交易內發布、由 AFTER_COMMIT listener 展開的私人通知工作類型。 */
public enum AuctionPrivateNotificationKind {
    OUTBID,
    SOLD
}
