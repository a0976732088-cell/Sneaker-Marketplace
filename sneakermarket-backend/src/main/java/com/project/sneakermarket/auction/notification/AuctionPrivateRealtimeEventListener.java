package com.project.sneakermarket.auction.notification;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 競標主交易提交後建立站內通知，再傳送最小私人 WebSocket 訊息。 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionPrivateRealtimeEventListener {

    private static final String PRIVATE_DESTINATION = "/queue/auction-events";

    private final AuctionNotificationIntegrationService notifications;
    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPrivateNotificationReady(AuctionPrivateNotificationReadyEvent event) {
        if (event.kind() == AuctionPrivateNotificationKind.OUTBID) {
            createAndSend(
                    () -> Optional.of(notifications.createOutbidNotification(
                            event.auctionId(),
                            event.memberId(),
                            event.amount(),
                            event.occurredAt()
                    )),
                    event,
                    event.memberId()
            );
            return;
        }

        createAndSend(
                () -> notifications.createWonNotification(
                        event.auctionId(),
                        event.memberId(),
                        event.amount(),
                        event.occurredAt()
                ),
                event,
                event.memberId()
        );

        List<Long> losingBidderIds;
        try {
            losingBidderIds = notifications.findDistinctLosingBidderIds(
                    event.auctionId(),
                    event.memberId()
            );
        } catch (RuntimeException ex) {
            log.error("Auction losing bidders lookup failed: auctionId={}", event.auctionId(), ex);
            return;
        }

        for (Long loserId : losingBidderIds) {
            createAndSend(
                    () -> notifications.createLostNotification(
                            event.auctionId(),
                            loserId,
                            event.memberId(),
                            event.occurredAt()
                    ),
                    event,
                    loserId
            );
        }
    }

    private void createAndSend(
            Supplier<Optional<AuctionPrivateNotificationDelivery>> notificationFactory,
            AuctionPrivateNotificationReadyEvent event,
            Long recipientMemberId) {
        Optional<AuctionPrivateNotificationDelivery> delivery;
        try {
            // REQUIRES_NEW proxy 已回傳，代表 Notification transaction 已完成 commit。
            delivery = notificationFactory.get();
        } catch (RuntimeException ex) {
            log.error(
                    "Auction private notification creation failed: auctionId={}, kind={}, memberId={}",
                    event.auctionId(),
                    event.kind(),
                    recipientMemberId,
                    ex
            );
            return;
        }

        delivery.ifPresent(this::sendSafely);
    }

    private void sendSafely(AuctionPrivateNotificationDelivery delivery) {
        try {
            messagingTemplate.convertAndSendToUser(
                    delivery.recipientMemberId().toString(),
                    PRIVATE_DESTINATION,
                    delivery.message()
            );
        } catch (RuntimeException ex) {
            // Notification 已提交；broker 失敗交由 Navbar polling/focus REST reconciliation 補足。
            log.error(
                    "Auction private realtime send failed: auctionId={}, eventType={}, memberId={}",
                    delivery.message().auctionId(),
                    delivery.message().eventType(),
                    delivery.recipientMemberId(),
                    ex
            );
        }
    }
}
