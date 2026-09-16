package com.project.sneakermarket.auction.notification;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.project.sneakermarket.auction.repository.AuctionBidRepository;
import com.project.sneakermarket.notification.entity.Notification;
import com.project.sneakermarket.notification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

/** 將競標事件轉成既有 notifications 資料；不修改 Notification 模組。 */
@Service
@RequiredArgsConstructor
public class AuctionNotificationIntegrationService {

    private static final String AUCTION_TARGET_TYPE = "AUCTION";
    private static final String ORDER_TARGET_TYPE = "AUCTION_ORDER";
    private static final String SELLER_TARGET_URL = "/auctions/me/listings";
    private static final String PARTICIPANT_TARGET_URL = "/auctions/me/bids";
    private static final String OUTBID = "AUCTION_OUTBID";
    private static final String STARTED = "AUCTION_STARTED";
    private static final String SOLD = "AUCTION_SOLD";
    private static final String WON = "AUCTION_WON";
    private static final String LOST = "AUCTION_LOST";
    private static final String UNSOLD = "AUCTION_UNSOLD";
    private static final String CANCELLED = "AUCTION_CANCELLED";
    private static final String REMOVED = "AUCTION_REMOVED";
    private static final String RESTORED = "AUCTION_RESTORED";
    private static final String ORDER_CREATED = "AUCTION_ORDER_CREATED";
    private static final String ORDER_PAID = "AUCTION_ORDER_PAID";
    private static final String ORDER_ABANDONED = "AUCTION_ORDER_ABANDONED";

    private final NotificationRepository notificationRepository;
    private final AuctionBidRepository auctionBidRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuctionPrivateNotificationDelivery createOutbidNotification(
            Long auctionId,
            Long previousHighestBidderId,
            BigDecimal currentPrice,
            LocalDateTime occurredAt) {
        Notification notification = baseNotification(
                previousHighestBidderId,
                OUTBID,
                "您的競標出價已被超越",
                "您在競標 #" + auctionId + " 的最高出價已被超越，目前價格為 NT$ "
                        + formatAmount(currentPrice),
                AUCTION_TARGET_TYPE,
                auctionId,
                "/auctions/" + auctionId,
                occurredAt
        );
        return delivery(
                notificationRepository.saveAndFlush(notification),
                AuctionPrivateRealtimeEventType.AUCTION_OUTBID,
                auctionId,
                occurredAt
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AuctionPrivateNotificationDelivery> createWonNotification(
            Long auctionId,
            Long winnerId,
            BigDecimal finalPrice,
            LocalDateTime occurredAt) {
        if (notificationExists(winnerId, WON, AUCTION_TARGET_TYPE, auctionId)) {
            return Optional.empty();
        }

        Notification notification = baseNotification(
                winnerId,
                WON,
                "恭喜您競標得標",
                "競標 #" + auctionId + " 已結標，您的得標金額為 NT$ "
                        + formatAmount(finalPrice),
                AUCTION_TARGET_TYPE,
                auctionId,
                "/auctions/" + auctionId,
                occurredAt
        );
        return Optional.of(delivery(
                notificationRepository.saveAndFlush(notification),
                AuctionPrivateRealtimeEventType.AUCTION_WON,
                auctionId,
                occurredAt
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AuctionPrivateNotificationDelivery> createLostNotification(
            Long auctionId,
            Long loserId,
            Long winnerId,
            LocalDateTime occurredAt) {
        if (loserId == null || loserId.equals(winnerId)
                || notificationExists(loserId, LOST, AUCTION_TARGET_TYPE, auctionId)) {
            return Optional.empty();
        }

        Notification notification = baseNotification(
                loserId,
                LOST,
                "本次競標未得標",
                "競標 #" + auctionId + " 已結標，本次由其他會員得標。",
                AUCTION_TARGET_TYPE,
                auctionId,
                "/auctions/" + auctionId,
                occurredAt
        );
        return Optional.of(delivery(
                notificationRepository.saveAndFlush(notification),
                AuctionPrivateRealtimeEventType.AUCTION_LOST,
                auctionId,
                occurredAt
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AuctionPrivateNotificationDelivery> createStartedSellerNotification(
            Long auctionId,
            Long sellerId,
            LocalDateTime occurredAt) {
        return createIdempotentAuctionNotification(
                auctionId,
                sellerId,
                STARTED,
                "您的競標已開始",
                "您的競標已正式開始，目前已開放會員出價。",
                SELLER_TARGET_URL,
                AuctionPrivateRealtimeEventType.AUCTION_STARTED,
                occurredAt
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AuctionPrivateNotificationDelivery> createSoldSellerNotification(
            Long auctionId,
            Long sellerId,
            BigDecimal finalPrice,
            LocalDateTime occurredAt) {
        return createIdempotentAuctionNotification(
                auctionId,
                sellerId,
                SOLD,
                "您的競標已結標",
                "您的競標已成功結標，商品已有會員得標，最終成交金額為 NT$ "
                        + formatAmount(finalPrice) + "。",
                SELLER_TARGET_URL,
                AuctionPrivateRealtimeEventType.AUCTION_SOLD,
                occurredAt
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AuctionPrivateNotificationDelivery> createUnsoldSellerNotification(
            Long auctionId,
            Long sellerId,
            LocalDateTime occurredAt) {
        return createIdempotentAuctionNotification(
                auctionId,
                sellerId,
                UNSOLD,
                "您的競標已流標",
                "您的競標已結束，本次沒有會員得標。",
                SELLER_TARGET_URL,
                AuctionPrivateRealtimeEventType.AUCTION_UNSOLD,
                occurredAt
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AuctionPrivateNotificationDelivery> createCancelledSellerNotification(
            Long auctionId,
            Long sellerId,
            LocalDateTime occurredAt) {
        return createIdempotentAuctionNotification(
                auctionId,
                sellerId,
                CANCELLED,
                "您的競標已取消",
                "此競標已成功取消。",
                SELLER_TARGET_URL,
                AuctionPrivateRealtimeEventType.AUCTION_CANCELLED,
                occurredAt
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AuctionPrivateNotificationDelivery> createCancelledParticipantNotification(
            Long auctionId,
            Long participantId,
            LocalDateTime occurredAt) {
        return createIdempotentAuctionNotification(
                auctionId,
                participantId,
                CANCELLED,
                "您參與的競標已被取消",
                "您參與的競標已被取消，後續將無法繼續出價。",
                PARTICIPANT_TARGET_URL,
                AuctionPrivateRealtimeEventType.AUCTION_CANCELLED,
                occurredAt
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuctionPrivateNotificationDelivery createRemovedSellerNotification(
            Long auctionId,
            Long sellerId,
            String reason,
            LocalDateTime occurredAt) {
        return createAuctionNotification(
                auctionId,
                sellerId,
                REMOVED,
                "您的競標已被平台下架",
                removedSellerContent(reason),
                SELLER_TARGET_URL,
                AuctionPrivateRealtimeEventType.AUCTION_REMOVED,
                occurredAt
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuctionPrivateNotificationDelivery createRemovedParticipantNotification(
            Long auctionId,
            Long participantId,
            LocalDateTime occurredAt) {
        return createAuctionNotification(
                auctionId,
                participantId,
                REMOVED,
                "您參與的競標已被平台下架",
                "您參與的競標已被平台下架，目前已無法繼續出價。",
                PARTICIPANT_TARGET_URL,
                AuctionPrivateRealtimeEventType.AUCTION_REMOVED,
                occurredAt
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuctionPrivateNotificationDelivery createRestoredSellerNotification(
            Long auctionId,
            Long sellerId,
            LocalDateTime occurredAt) {
        return createAuctionNotification(
                auctionId,
                sellerId,
                RESTORED,
                "您的競標已恢復",
                "您刊登的競標已由平台恢復顯示，請查看目前競標狀態。",
                SELLER_TARGET_URL,
                AuctionPrivateRealtimeEventType.AUCTION_RESTORED,
                occurredAt
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuctionPrivateNotificationDelivery createRestoredParticipantNotification(
            Long auctionId,
            Long participantId,
            LocalDateTime occurredAt) {
        return createAuctionNotification(
                auctionId,
                participantId,
                RESTORED,
                "您參與的競標已恢復",
                "您先前參與的競標已由平台恢復，請查看目前最新競標狀態。",
                PARTICIPANT_TARGET_URL,
                AuctionPrivateRealtimeEventType.AUCTION_RESTORED,
                occurredAt
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AuctionPrivateNotificationDelivery> createOrderCreatedNotification(
            Long auctionId,
            Long orderId,
            String orderNumber,
            Long buyerId,
            LocalDateTime occurredAt) {
        if (notificationExists(buyerId, ORDER_CREATED, ORDER_TARGET_TYPE, orderId)) {
            return Optional.empty();
        }

        Notification notification = baseNotification(
                buyerId,
                ORDER_CREATED,
                "您的競標訂單已建立",
                "您得標的競標已建立訂單，請於付款期限內完成付款。",
                ORDER_TARGET_TYPE,
                orderId,
                buyerOrderTargetUrl(orderNumber),
                occurredAt
        );
        return Optional.of(delivery(
                notificationRepository.saveAndFlush(notification),
                AuctionPrivateRealtimeEventType.AUCTION_ORDER_CREATED,
                auctionId,
                occurredAt
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AuctionPrivateNotificationDelivery> createOrderPaidNotification(
            Long auctionId,
            Long orderId,
            Long sellerId,
            LocalDateTime occurredAt) {
        if (notificationExists(sellerId, ORDER_PAID, ORDER_TARGET_TYPE, orderId)) {
            return Optional.empty();
        }

        Notification notification = baseNotification(
                sellerId,
                ORDER_PAID,
                "競標訂單已完成付款",
                "買家已完成競標訂單付款，請查看訂單最新狀態。",
                ORDER_TARGET_TYPE,
                orderId,
                "/auction-orders",
                occurredAt
        );
        return Optional.of(delivery(
                notificationRepository.saveAndFlush(notification),
                AuctionPrivateRealtimeEventType.AUCTION_ORDER_PAID,
                auctionId,
                occurredAt
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AuctionPrivateNotificationDelivery> createOrderAbandonedBuyerNotification(
            Long auctionId,
            Long orderId,
            String orderNumber,
            Long buyerId,
            LocalDateTime occurredAt) {
        if (notificationExists(buyerId, ORDER_ABANDONED, ORDER_TARGET_TYPE, orderId)) {
            return Optional.empty();
        }

        Notification notification = baseNotification(
                buyerId,
                ORDER_ABANDONED,
                "競標訂單已逾期棄標",
                "您的競標訂單因超過付款期限未完成付款，已標記為棄標。",
                ORDER_TARGET_TYPE,
                orderId,
                buyerOrderTargetUrl(orderNumber),
                occurredAt
        );
        return Optional.of(delivery(
                notificationRepository.saveAndFlush(notification),
                AuctionPrivateRealtimeEventType.AUCTION_ORDER_ABANDONED,
                auctionId,
                occurredAt
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AuctionPrivateNotificationDelivery> createOrderAbandonedSellerNotification(
            Long auctionId,
            Long orderId,
            Long sellerId,
            LocalDateTime occurredAt) {
        if (notificationExists(sellerId, ORDER_ABANDONED, ORDER_TARGET_TYPE, orderId)) {
            return Optional.empty();
        }

        Notification notification = baseNotification(
                sellerId,
                ORDER_ABANDONED,
                "競標訂單已棄標",
                "買家未於付款期限內完成付款，此競標訂單已標記為棄標。",
                ORDER_TARGET_TYPE,
                orderId,
                "/auction-orders",
                occurredAt
        );
        return Optional.of(delivery(
                notificationRepository.saveAndFlush(notification),
                AuctionPrivateRealtimeEventType.AUCTION_ORDER_ABANDONED,
                auctionId,
                occurredAt
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<Long> findDistinctLosingBidderIds(Long auctionId, Long winnerId) {
        return auctionBidRepository.findDistinctLosingBidderIds(auctionId, winnerId)
                .stream()
                .filter(memberId -> memberId != null && !memberId.equals(winnerId))
                .distinct()
                .toList();
    }

    private boolean notificationExists(
            Long memberId,
            String type,
            String targetType,
            Long targetId) {
        return notificationRepository
                .existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
                        memberId,
                        type,
                        targetType,
                        targetId
                );
    }

    private Optional<AuctionPrivateNotificationDelivery> createIdempotentAuctionNotification(
            Long auctionId,
            Long recipientId,
            String notificationType,
            String title,
            String content,
            String targetUrl,
            AuctionPrivateRealtimeEventType eventType,
            LocalDateTime occurredAt) {
        if (notificationExists(
                recipientId,
                notificationType,
                AUCTION_TARGET_TYPE,
                auctionId)) {
            return Optional.empty();
        }
        return Optional.of(createAuctionNotification(
                auctionId,
                recipientId,
                notificationType,
                title,
                content,
                targetUrl,
                eventType,
                occurredAt
        ));
    }

    private AuctionPrivateNotificationDelivery createAuctionNotification(
            Long auctionId,
            Long recipientId,
            String notificationType,
            String title,
            String content,
            String targetUrl,
            AuctionPrivateRealtimeEventType eventType,
            LocalDateTime occurredAt) {
        Notification notification = baseNotification(
                recipientId,
                notificationType,
                title,
                content,
                AUCTION_TARGET_TYPE,
                auctionId,
                targetUrl,
                occurredAt
        );
        return delivery(
                notificationRepository.saveAndFlush(notification),
                eventType,
                auctionId,
                occurredAt
        );
    }

    private Notification baseNotification(
            Long memberId,
            String notificationType,
            String title,
            String content,
            String targetType,
            Long targetId,
            String targetUrl,
            LocalDateTime occurredAt) {
        Notification notification = new Notification();
        notification.setMemberId(memberId);
        notification.setNotificationType(notificationType);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setTargetUrl(targetUrl);
        notification.setIsRead(false);
        notification.setIsSent(true);
        notification.setSentAt(occurredAt);
        return notification;
    }

    private AuctionPrivateNotificationDelivery delivery(
            Notification notification,
            AuctionPrivateRealtimeEventType eventType,
            Long auctionId,
            LocalDateTime occurredAt) {
        return new AuctionPrivateNotificationDelivery(
                notification.getMemberId(),
                new AuctionPrivateRealtimeMessage(
                        eventType,
                        auctionId,
                        notification.getNotificationId(),
                        occurredAt
                )
        );
    }

    private String buyerOrderTargetUrl(String orderNumber) {
        if (orderNumber == null || orderNumber.isBlank()) {
            return "/auction-orders";
        }
        return "/auction-orders/" + orderNumber + "/payment";
    }

    private String removedSellerContent(String reason) {
        String content = "您刊登的競標已被平台下架，目前已停止公開顯示與後續競標行為。";
        if (reason == null || reason.isBlank()) {
            return content;
        }
        return content + " 下架原因：" + reason.trim();
    }

    private String formatAmount(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getIntegerInstance(Locale.TAIWAN);
        formatter.setMaximumFractionDigits(0);
        return formatter.format(amount);
    }
}
