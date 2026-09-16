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

    private static final String TARGET_TYPE = "AUCTION";
    private static final String OUTBID = "AUCTION_OUTBID";
    private static final String WON = "AUCTION_WON";
    private static final String LOST = "AUCTION_LOST";

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
                auctionId,
                occurredAt
        );
        return delivery(
                notificationRepository.saveAndFlush(notification),
                AuctionPrivateRealtimeEventType.AUCTION_OUTBID,
                occurredAt
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AuctionPrivateNotificationDelivery> createWonNotification(
            Long auctionId,
            Long winnerId,
            BigDecimal finalPrice,
            LocalDateTime occurredAt) {
        if (notificationExists(winnerId, WON, auctionId)) {
            return Optional.empty();
        }

        Notification notification = baseNotification(
                winnerId,
                WON,
                "恭喜您競標得標",
                "競標 #" + auctionId + " 已結標，您的得標金額為 NT$ "
                        + formatAmount(finalPrice),
                auctionId,
                occurredAt
        );
        return Optional.of(delivery(
                notificationRepository.saveAndFlush(notification),
                AuctionPrivateRealtimeEventType.AUCTION_WON,
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
                || notificationExists(loserId, LOST, auctionId)) {
            return Optional.empty();
        }

        Notification notification = baseNotification(
                loserId,
                LOST,
                "本次競標未得標",
                "競標 #" + auctionId + " 已結標，本次由其他會員得標。",
                auctionId,
                occurredAt
        );
        return Optional.of(delivery(
                notificationRepository.saveAndFlush(notification),
                AuctionPrivateRealtimeEventType.AUCTION_LOST,
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

    private boolean notificationExists(Long memberId, String type, Long auctionId) {
        return notificationRepository
                .existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
                        memberId,
                        type,
                        TARGET_TYPE,
                        auctionId
                );
    }

    private Notification baseNotification(
            Long memberId,
            String notificationType,
            String title,
            String content,
            Long auctionId,
            LocalDateTime occurredAt) {
        Notification notification = new Notification();
        notification.setMemberId(memberId);
        notification.setNotificationType(notificationType);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setTargetType(TARGET_TYPE);
        notification.setTargetId(auctionId);
        notification.setTargetUrl("/auctions/" + auctionId);
        notification.setIsRead(false);
        notification.setIsSent(true);
        notification.setSentAt(occurredAt);
        return notification;
    }

    private AuctionPrivateNotificationDelivery delivery(
            Notification notification,
            AuctionPrivateRealtimeEventType eventType,
            LocalDateTime occurredAt) {
        return new AuctionPrivateNotificationDelivery(
                notification.getMemberId(),
                new AuctionPrivateRealtimeMessage(
                        eventType,
                        notification.getTargetId(),
                        notification.getNotificationId(),
                        occurredAt
                )
        );
    }

    private String formatAmount(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getIntegerInstance(Locale.TAIWAN);
        formatter.setMaximumFractionDigits(0);
        return formatter.format(amount);
    }
}
