package com.project.sneakermarket.auction.notification;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    private AuctionPrivateNotificationReadyEvent event() {
        return new AuctionPrivateNotificationReadyEvent(
                AuctionPrivateNotificationKind.OUTBID,
                123L,
                41L,
                new BigDecimal("6500.00"),
                LocalDateTime.of(2026, 9, 16, 12, 30)
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
