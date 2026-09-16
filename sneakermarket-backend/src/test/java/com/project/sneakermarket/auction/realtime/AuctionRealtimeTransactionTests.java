package com.project.sneakermarket.auction.realtime;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

import com.project.sneakermarket.auction.dto.AuctionStatusResponse;
import com.project.sneakermarket.auction.entity.AuctionStatus;

/** 驗證 Spring transaction synchronization，而非直接呼叫 listener。 */
class AuctionRealtimeTransactionTests {

    @Test
    void commitBroadcastsAfterCompletionButRollbackNeverBroadcasts() {
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        TestTransactionManager transactions = new TestTransactionManager();

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(TransactionalEventListenerFactory.class);
            context.registerBean(SimpMessagingTemplate.class, () -> messaging);
            context.registerBean(AuctionRealtimeEventListener.class);
            context.refresh();

            AuctionRealtimeEvent event = event();
            TransactionTemplate template = new TransactionTemplate(transactions);

            template.executeWithoutResult(status -> {
                context.publishEvent(event);
                verifyNoInteractions(messaging);
            });
            verify(messaging).convertAndSend("/topic/auctions/123", event.message());

            clearInvocations(messaging);
            template.executeWithoutResult(status -> {
                context.publishEvent(event);
                verifyNoInteractions(messaging);
                status.setRollbackOnly();
            });
            verifyNoInteractions(messaging);
        }
    }

    private AuctionRealtimeEvent event() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 16, 12, 30);
        return new AuctionRealtimeEvent(new AuctionRealtimeMessage(
                AuctionRealtimeEventType.BID_PLACED,
                new AuctionStatusResponse(
                        123L,
                        new BigDecimal("6500.00"),
                        4,
                        new BigDecimal("7000.00"),
                        AuctionStatus.ACTIVE,
                        now.plusHours(1),
                        now
                )
        ));
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
