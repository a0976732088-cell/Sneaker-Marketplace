package com.project.sneakermarket.auction.notification;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/** 驗證私人通知工作只在競標主交易 commit 後啟動。 */
class AuctionPrivateRealtimeTransactionTests {

    @Test
    void commitCreatesNotificationThenSendsButRollbackDoesNothing() {
        AuctionNotificationIntegrationService notifications =
                mock(AuctionNotificationIntegrationService.class);
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        TestTransactionManager transactions = new TestTransactionManager();
        AuctionPrivateNotificationReadyEvent event = event();
        AuctionPrivateNotificationDelivery delivery = new AuctionPrivateNotificationDelivery(
                41L,
                new AuctionPrivateRealtimeMessage(
                        AuctionPrivateRealtimeEventType.AUCTION_OUTBID,
                        123L,
                        901L,
                        event.occurredAt()
                )
        );
        when(notifications.createOutbidNotification(
                event.auctionId(), event.memberId(), event.amount(), event.occurredAt()
        )).thenReturn(delivery);

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(TransactionalEventListenerFactory.class);
            context.registerBean(AuctionNotificationIntegrationService.class, () -> notifications);
            context.registerBean(SimpMessagingTemplate.class, () -> messaging);
            context.registerBean(AuctionPrivateRealtimeEventListener.class);
            context.refresh();

            TransactionTemplate template = new TransactionTemplate(transactions);
            template.executeWithoutResult(status -> {
                context.publishEvent(event);
                verifyNoInteractions(notifications, messaging);
            });
            verify(notifications).createOutbidNotification(
                    event.auctionId(), event.memberId(), event.amount(), event.occurredAt()
            );
            verify(messaging).convertAndSendToUser(
                    "41", "/queue/auction-events", delivery.message()
            );

            clearInvocations(notifications, messaging);
            template.executeWithoutResult(status -> {
                context.publishEvent(event);
                verifyNoInteractions(notifications, messaging);
                status.setRollbackOnly();
            });
            verifyNoInteractions(notifications, messaging);
        }
    }

    @Test
    void everyOrderEventWaitsForCommitAndRollbackDropsIt() {
        for (AuctionPrivateNotificationKind kind : List.of(
                AuctionPrivateNotificationKind.ORDER_CREATED,
                AuctionPrivateNotificationKind.ORDER_PAID,
                AuctionPrivateNotificationKind.ORDER_ABANDONED)) {
            assertOrderEventWaitsForCommit(kind);
        }
    }

    @Test
    void everyAuctionLifecycleEventWaitsForCommitAndRollbackDropsIt() {
        for (AuctionPrivateNotificationKind kind : List.of(
                AuctionPrivateNotificationKind.STARTED,
                AuctionPrivateNotificationKind.SOLD,
                AuctionPrivateNotificationKind.UNSOLD,
                AuctionPrivateNotificationKind.CANCELLED,
                AuctionPrivateNotificationKind.REMOVED,
                AuctionPrivateNotificationKind.RESTORED)) {
            assertLifecycleEventWaitsForCommit(kind);
        }
    }

    private void assertLifecycleEventWaitsForCommit(AuctionPrivateNotificationKind kind) {
        AuctionNotificationIntegrationService notifications =
                mock(AuctionNotificationIntegrationService.class);
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        TestTransactionManager transactions = new TestTransactionManager();
        AuctionPrivateNotificationReadyEvent event = lifecycleEvent(kind);
        AuctionPrivateNotificationDelivery seller = delivery(
                31L,
                lifecycleEventType(kind),
                920L
        );
        AuctionPrivateNotificationDelivery participant = delivery(
                61L,
                kind == AuctionPrivateNotificationKind.SOLD
                        ? AuctionPrivateRealtimeEventType.AUCTION_LOST
                        : lifecycleEventType(kind),
                921L
        );
        AuctionPrivateNotificationDelivery winner = delivery(
                77L,
                AuctionPrivateRealtimeEventType.AUCTION_WON,
                922L
        );

        switch (kind) {
            case STARTED -> when(notifications.createStartedSellerNotification(
                    123L, 31L, event.occurredAt()
            )).thenReturn(Optional.of(seller));
            case SOLD -> {
                when(notifications.createSoldSellerNotification(
                        123L, 31L, new BigDecimal("7000.00"), event.occurredAt()
                )).thenReturn(Optional.of(seller));
                when(notifications.createWonNotification(
                        123L, 77L, new BigDecimal("7000.00"), event.occurredAt()
                )).thenReturn(Optional.of(winner));
                when(notifications.createLostNotification(
                        123L, 61L, 77L, event.occurredAt()
                )).thenReturn(Optional.of(participant));
            }
            case UNSOLD -> when(notifications.createUnsoldSellerNotification(
                    123L, 31L, event.occurredAt()
            )).thenReturn(Optional.of(seller));
            case CANCELLED -> {
                when(notifications.createCancelledSellerNotification(
                        123L, 31L, event.occurredAt()
                )).thenReturn(Optional.of(seller));
                when(notifications.createCancelledParticipantNotification(
                        123L, 61L, event.occurredAt()
                )).thenReturn(Optional.of(participant));
            }
            case REMOVED -> {
                when(notifications.createRemovedSellerNotification(
                        123L, 31L, "違反規範", event.occurredAt()
                )).thenReturn(seller);
                when(notifications.createRemovedParticipantNotification(
                        123L, 61L, event.occurredAt()
                )).thenReturn(participant);
            }
            case RESTORED -> {
                when(notifications.createRestoredSellerNotification(
                        123L, 31L, event.occurredAt()
                )).thenReturn(seller);
                when(notifications.createRestoredParticipantNotification(
                        123L, 61L, event.occurredAt()
                )).thenReturn(participant);
            }
            default -> throw new IllegalArgumentException("Not a lifecycle event: " + kind);
        }

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(TransactionalEventListenerFactory.class);
            context.registerBean(AuctionNotificationIntegrationService.class, () -> notifications);
            context.registerBean(SimpMessagingTemplate.class, () -> messaging);
            context.registerBean(AuctionPrivateRealtimeEventListener.class);
            context.refresh();

            TransactionTemplate template = new TransactionTemplate(transactions);
            template.executeWithoutResult(status -> {
                context.publishEvent(event);
                verifyNoInteractions(notifications, messaging);
            });
            verifyCommittedLifecycleEvent(kind, notifications, messaging, event,
                    seller, participant, winner);

            clearInvocations(notifications, messaging);
            template.executeWithoutResult(status -> {
                context.publishEvent(event);
                verifyNoInteractions(notifications, messaging);
                status.setRollbackOnly();
            });
            verifyNoInteractions(notifications, messaging);
        }
    }

    private void verifyCommittedLifecycleEvent(
            AuctionPrivateNotificationKind kind,
            AuctionNotificationIntegrationService notifications,
            SimpMessagingTemplate messaging,
            AuctionPrivateNotificationReadyEvent event,
            AuctionPrivateNotificationDelivery seller,
            AuctionPrivateNotificationDelivery participant,
            AuctionPrivateNotificationDelivery winner) {
        switch (kind) {
            case STARTED -> verify(notifications).createStartedSellerNotification(
                    123L, 31L, event.occurredAt()
            );
            case SOLD -> {
                verify(notifications).createSoldSellerNotification(
                        123L, 31L, new BigDecimal("7000.00"), event.occurredAt()
                );
                verify(notifications).createWonNotification(
                        123L, 77L, new BigDecimal("7000.00"), event.occurredAt()
                );
                verify(notifications).createLostNotification(
                        123L, 61L, 77L, event.occurredAt()
                );
                verify(messaging).convertAndSendToUser(
                        "77", "/queue/auction-events", winner.message()
                );
            }
            case UNSOLD -> verify(notifications).createUnsoldSellerNotification(
                    123L, 31L, event.occurredAt()
            );
            case CANCELLED -> {
                verify(notifications).createCancelledSellerNotification(
                        123L, 31L, event.occurredAt()
                );
                verify(notifications).createCancelledParticipantNotification(
                        123L, 61L, event.occurredAt()
                );
            }
            case REMOVED -> {
                verify(notifications).createRemovedSellerNotification(
                        123L, 31L, "違反規範", event.occurredAt()
                );
                verify(notifications).createRemovedParticipantNotification(
                        123L, 61L, event.occurredAt()
                );
            }
            case RESTORED -> {
                verify(notifications).createRestoredSellerNotification(
                        123L, 31L, event.occurredAt()
                );
                verify(notifications).createRestoredParticipantNotification(
                        123L, 61L, event.occurredAt()
                );
            }
            default -> throw new IllegalArgumentException("Not a lifecycle event: " + kind);
        }
        verify(messaging).convertAndSendToUser(
                "31", "/queue/auction-events", seller.message()
        );
        if (kind == AuctionPrivateNotificationKind.SOLD
                || kind == AuctionPrivateNotificationKind.CANCELLED
                || kind == AuctionPrivateNotificationKind.REMOVED
                || kind == AuctionPrivateNotificationKind.RESTORED) {
            verify(messaging).convertAndSendToUser(
                    "61", "/queue/auction-events", participant.message()
            );
        }
    }

    private void assertOrderEventWaitsForCommit(AuctionPrivateNotificationKind kind) {
        AuctionNotificationIntegrationService notifications =
                mock(AuctionNotificationIntegrationService.class);
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        TestTransactionManager transactions = new TestTransactionManager();
        AuctionPrivateNotificationReadyEvent event = orderEvent(kind);
        AuctionPrivateNotificationDelivery buyer = delivery(
                41L,
                kind == AuctionPrivateNotificationKind.ORDER_CREATED
                        ? AuctionPrivateRealtimeEventType.AUCTION_ORDER_CREATED
                        : AuctionPrivateRealtimeEventType.AUCTION_ORDER_ABANDONED,
                905L
        );
        AuctionPrivateNotificationDelivery seller = delivery(
                31L,
                kind == AuctionPrivateNotificationKind.ORDER_PAID
                        ? AuctionPrivateRealtimeEventType.AUCTION_ORDER_PAID
                        : AuctionPrivateRealtimeEventType.AUCTION_ORDER_ABANDONED,
                906L
        );

        switch (kind) {
            case ORDER_CREATED -> when(notifications.createOrderCreatedNotification(
                    123L, 456L, "AUC-20260916-000456", 41L, event.occurredAt()
            )).thenReturn(Optional.of(buyer));
            case ORDER_PAID -> when(notifications.createOrderPaidNotification(
                    123L, 456L, 31L, event.occurredAt()
            )).thenReturn(Optional.of(seller));
            case ORDER_ABANDONED -> {
                when(notifications.createOrderAbandonedBuyerNotification(
                        123L, 456L, "AUC-20260916-000456", 41L, event.occurredAt()
                )).thenReturn(Optional.of(buyer));
                when(notifications.createOrderAbandonedSellerNotification(
                        123L, 456L, 31L, event.occurredAt()
                )).thenReturn(Optional.of(seller));
            }
            default -> throw new IllegalArgumentException("Not an order event: " + kind);
        }

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(TransactionalEventListenerFactory.class);
            context.registerBean(AuctionNotificationIntegrationService.class, () -> notifications);
            context.registerBean(SimpMessagingTemplate.class, () -> messaging);
            context.registerBean(AuctionPrivateRealtimeEventListener.class);
            context.refresh();

            TransactionTemplate template = new TransactionTemplate(transactions);
            template.executeWithoutResult(status -> {
                context.publishEvent(event);
                verifyNoInteractions(notifications, messaging);
            });
            verifyCommittedOrderEvent(kind, notifications, messaging, event, buyer, seller);

            clearInvocations(notifications, messaging);
            template.executeWithoutResult(status -> {
                context.publishEvent(event);
                verifyNoInteractions(notifications, messaging);
                status.setRollbackOnly();
            });
            verifyNoInteractions(notifications, messaging);
        }
    }

    private void verifyCommittedOrderEvent(
            AuctionPrivateNotificationKind kind,
            AuctionNotificationIntegrationService notifications,
            SimpMessagingTemplate messaging,
            AuctionPrivateNotificationReadyEvent event,
            AuctionPrivateNotificationDelivery buyer,
            AuctionPrivateNotificationDelivery seller) {
        switch (kind) {
            case ORDER_CREATED -> {
                verify(notifications).createOrderCreatedNotification(
                        123L, 456L, "AUC-20260916-000456", 41L, event.occurredAt()
                );
                verify(messaging).convertAndSendToUser(
                        "41", "/queue/auction-events", buyer.message()
                );
            }
            case ORDER_PAID -> {
                verify(notifications).createOrderPaidNotification(
                        123L, 456L, 31L, event.occurredAt()
                );
                verify(messaging).convertAndSendToUser(
                        "31", "/queue/auction-events", seller.message()
                );
            }
            case ORDER_ABANDONED -> {
                verify(notifications).createOrderAbandonedBuyerNotification(
                        123L, 456L, "AUC-20260916-000456", 41L, event.occurredAt()
                );
                verify(notifications).createOrderAbandonedSellerNotification(
                        123L, 456L, 31L, event.occurredAt()
                );
                verify(messaging).convertAndSendToUser(
                        "41", "/queue/auction-events", buyer.message()
                );
                verify(messaging).convertAndSendToUser(
                        "31", "/queue/auction-events", seller.message()
                );
            }
            default -> throw new IllegalArgumentException("Not an order event: " + kind);
        }
    }

    private AuctionPrivateNotificationReadyEvent event() {
        return new AuctionPrivateNotificationReadyEvent(
                AuctionPrivateNotificationKind.OUTBID,
                123L,
                41L,
                new BigDecimal("6500.00"),
                LocalDateTime.of(2026, 9, 16, 12, 30)
        );
    }

    private AuctionPrivateNotificationReadyEvent orderEvent(
            AuctionPrivateNotificationKind kind) {
        return new AuctionPrivateNotificationReadyEvent(
                kind,
                123L,
                456L,
                "AUC-20260916-000456",
                41L,
                31L,
                null,
                LocalDateTime.of(2026, 9, 16, 12, 30)
        );
    }

    private AuctionPrivateNotificationReadyEvent lifecycleEvent(
            AuctionPrivateNotificationKind kind) {
        return new AuctionPrivateNotificationReadyEvent(
                kind,
                123L,
                null,
                null,
                kind == AuctionPrivateNotificationKind.SOLD ? 77L : null,
                31L,
                kind == AuctionPrivateNotificationKind.SOLD
                        ? List.of(77L, 61L)
                        : List.of(61L),
                kind == AuctionPrivateNotificationKind.REMOVED ? "違反規範" : null,
                kind == AuctionPrivateNotificationKind.SOLD
                        ? new BigDecimal("7000.00")
                        : null,
                LocalDateTime.of(2026, 9, 16, 12, 30)
        );
    }

    private AuctionPrivateRealtimeEventType lifecycleEventType(
            AuctionPrivateNotificationKind kind) {
        return switch (kind) {
            case STARTED -> AuctionPrivateRealtimeEventType.AUCTION_STARTED;
            case SOLD -> AuctionPrivateRealtimeEventType.AUCTION_SOLD;
            case UNSOLD -> AuctionPrivateRealtimeEventType.AUCTION_UNSOLD;
            case CANCELLED -> AuctionPrivateRealtimeEventType.AUCTION_CANCELLED;
            case REMOVED -> AuctionPrivateRealtimeEventType.AUCTION_REMOVED;
            case RESTORED -> AuctionPrivateRealtimeEventType.AUCTION_RESTORED;
            default -> throw new IllegalArgumentException("Not a lifecycle event: " + kind);
        };
    }

    private AuctionPrivateNotificationDelivery delivery(
            Long memberId,
            AuctionPrivateRealtimeEventType eventType,
            Long notificationId) {
        return new AuctionPrivateNotificationDelivery(
                memberId,
                new AuctionPrivateRealtimeMessage(
                        eventType,
                        123L,
                        notificationId,
                        LocalDateTime.of(2026, 9, 16, 12, 30)
                )
        );
    }

    private static final class TestTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
