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
        switch (event.kind()) {
            case OUTBID -> createAndSend(
                        () -> Optional.of(notifications.createOutbidNotification(
                                event.auctionId(),
                                event.memberId(),
                                event.amount(),
                                event.occurredAt()
                        )),
                        event,
                        event.memberId()
                );
            case STARTED -> createAndSend(
                    () -> notifications.createStartedSellerNotification(
                            event.auctionId(),
                            event.sellerId(),
                            event.occurredAt()
                    ),
                    event,
                    event.sellerId()
            );
            case SOLD -> handleSold(event);
            case UNSOLD -> createAndSend(
                    () -> notifications.createUnsoldSellerNotification(
                            event.auctionId(),
                            event.sellerId(),
                            event.occurredAt()
                    ),
                    event,
                    event.sellerId()
            );
            case CANCELLED -> handleCancelled(event);
            case REMOVED -> handleRemoved(event);
            case RESTORED -> handleRestored(event);
            case ORDER_CREATED -> createAndSend(
                        () -> notifications.createOrderCreatedNotification(
                                event.auctionId(),
                                event.orderId(),
                                event.orderNumber(),
                                event.memberId(),
                                event.occurredAt()
                        ),
                        event,
                        event.memberId()
                );
            case ORDER_PAID -> createAndSend(
                        () -> notifications.createOrderPaidNotification(
                                event.auctionId(),
                                event.orderId(),
                                event.sellerId(),
                                event.occurredAt()
                        ),
                        event,
                        event.sellerId()
                );
            case ORDER_ABANDONED -> handleOrderAbandoned(event);
        }
    }

    private void handleSold(AuctionPrivateNotificationReadyEvent event) {
        if (event.sellerId() != null) {
            createAndSend(
                    () -> notifications.createSoldSellerNotification(
                            event.auctionId(),
                            event.sellerId(),
                            event.amount(),
                            event.occurredAt()
                    ),
                    event,
                    event.sellerId()
            );
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

        List<Long> losingBidderIds = event.participantIds();
        // 舊版 SOLD event 沒有 participant snapshot，保留既有查詢路徑。
        if (event.sellerId() == null && losingBidderIds.isEmpty()) {
            try {
                losingBidderIds = notifications.findDistinctLosingBidderIds(
                        event.auctionId(),
                        event.memberId()
                );
            } catch (RuntimeException ex) {
                log.error("Auction losing bidders lookup failed: auctionId={}", event.auctionId(), ex);
                return;
            }
        }

        for (Long loserId : losingBidderIds) {
            if (loserId.equals(event.memberId()) || loserId.equals(event.sellerId())) {
                continue;
            }
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

    private void handleCancelled(AuctionPrivateNotificationReadyEvent event) {
        createAndSend(
                () -> notifications.createCancelledSellerNotification(
                        event.auctionId(),
                        event.sellerId(),
                        event.occurredAt()
                ),
                event,
                event.sellerId()
        );
        for (Long participantId : participantsExcludingSeller(event)) {
            createAndSend(
                    () -> notifications.createCancelledParticipantNotification(
                            event.auctionId(),
                            participantId,
                            event.occurredAt()
                    ),
                    event,
                    participantId
            );
        }
    }

    private void handleRemoved(AuctionPrivateNotificationReadyEvent event) {
        createAndSend(
                () -> Optional.of(notifications.createRemovedSellerNotification(
                        event.auctionId(),
                        event.sellerId(),
                        event.reason(),
                        event.occurredAt()
                )),
                event,
                event.sellerId()
        );
        for (Long participantId : participantsExcludingSeller(event)) {
            createAndSend(
                    () -> Optional.of(notifications.createRemovedParticipantNotification(
                            event.auctionId(),
                            participantId,
                            event.occurredAt()
                    )),
                    event,
                    participantId
            );
        }
    }

    private void handleRestored(AuctionPrivateNotificationReadyEvent event) {
        createAndSend(
                () -> Optional.of(notifications.createRestoredSellerNotification(
                        event.auctionId(),
                        event.sellerId(),
                        event.occurredAt()
                )),
                event,
                event.sellerId()
        );
        for (Long participantId : participantsExcludingSeller(event)) {
            createAndSend(
                    () -> Optional.of(notifications.createRestoredParticipantNotification(
                            event.auctionId(),
                            participantId,
                            event.occurredAt()
                    )),
                    event,
                    participantId
            );
        }
    }

    private List<Long> participantsExcludingSeller(AuctionPrivateNotificationReadyEvent event) {
        return event.participantIds().stream()
                .filter(participantId -> !participantId.equals(event.sellerId()))
                .toList();
    }

    private void handleOrderAbandoned(AuctionPrivateNotificationReadyEvent event) {
        // 每位收件人各自建立 REQUIRES_NEW 通知；一方失敗不會阻止另一方。
        createAndSend(
                () -> notifications.createOrderAbandonedBuyerNotification(
                        event.auctionId(),
                        event.orderId(),
                        event.orderNumber(),
                        event.memberId(),
                        event.occurredAt()
                ),
                event,
                event.memberId()
        );
        createAndSend(
                () -> notifications.createOrderAbandonedSellerNotification(
                        event.auctionId(),
                        event.orderId(),
                        event.sellerId(),
                        event.occurredAt()
                ),
                event,
                event.sellerId()
        );
    }

    private void createAndSend(
            Supplier<Optional<AuctionPrivateNotificationDelivery>> notificationFactory,
            AuctionPrivateNotificationReadyEvent event,
            Long recipientMemberId) {
        if (recipientMemberId == null) {
            log.warn(
                    "Auction private notification skipped without recipient: auctionId={}, kind={}",
                    event.auctionId(),
                    event.kind()
            );
            return;
        }

        Optional<AuctionPrivateNotificationDelivery> delivery;
        try {
            // REQUIRES_NEW proxy 已回傳，代表 Notification transaction 已完成 commit。
            delivery = notificationFactory.get();
        } catch (RuntimeException ex) {
            log.error(
                    "Auction private notification creation failed: auctionId={}, orderId={}, kind={}, memberId={}",
                    event.auctionId(),
                    event.orderId(),
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
