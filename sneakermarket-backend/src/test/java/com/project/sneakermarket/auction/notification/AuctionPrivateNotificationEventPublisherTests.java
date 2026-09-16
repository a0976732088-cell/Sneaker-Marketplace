package com.project.sneakermarket.auction.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class AuctionPrivateNotificationEventPublisherTests {

    private static final LocalDateTime OCCURRED_AT =
            LocalDateTime.of(2026, 9, 16, 12, 30);

    @Test
    void orderPublishersReuseOneScalarOnlyReadyEventContract() {
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        AuctionPrivateNotificationEventPublisher publisher =
                new AuctionPrivateNotificationEventPublisher(events);

        publisher.publishOrderCreated(
                123L, 456L, "AUC-20260916-000456", 41L, 31L, OCCURRED_AT
        );
        publisher.publishOrderPaid(
                123L, 456L, "AUC-20260916-000456", 41L, 31L, OCCURRED_AT
        );
        publisher.publishOrderAbandoned(
                123L, 456L, "AUC-20260916-000456", 41L, 31L, OCCURRED_AT
        );

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(events, times(3)).publishEvent(captor.capture());
        List<Object> published = captor.getAllValues();

        assertOrderEvent(published.get(0), AuctionPrivateNotificationKind.ORDER_CREATED);
        assertOrderEvent(published.get(1), AuctionPrivateNotificationKind.ORDER_PAID);
        assertOrderEvent(published.get(2), AuctionPrivateNotificationKind.ORDER_ABANDONED);
    }

    @Test
    void lifecyclePublishersCaptureImmutableRoleAndParticipantSnapshots() {
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        AuctionPrivateNotificationEventPublisher publisher =
                new AuctionPrivateNotificationEventPublisher(events);
        List<Long> participants = new ArrayList<>(
                Arrays.asList(77L, 61L, 61L, null, 62L)
        );

        publisher.publishStarted(123L, 31L, OCCURRED_AT);
        publisher.publishSold(
                123L, 31L, 77L, participants, new BigDecimal("7000.00"), OCCURRED_AT
        );
        participants.add(63L);
        publisher.publishUnsold(123L, 31L, OCCURRED_AT);
        publisher.publishCancelled(123L, 31L, List.of(61L, 62L), "賣家取消", OCCURRED_AT);
        publisher.publishRemoved(123L, 31L, List.of(61L), "  違反規範  ", OCCURRED_AT);
        publisher.publishRestored(123L, 31L, List.of(61L), "申訴成立", OCCURRED_AT);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(events, times(6)).publishEvent(captor.capture());
        List<Object> published = captor.getAllValues();

        assertLifecycleEvent(
                published.get(0), AuctionPrivateNotificationKind.STARTED,
                null, List.of(), null, null
        );
        assertLifecycleEvent(
                published.get(1), AuctionPrivateNotificationKind.SOLD,
                77L, List.of(77L, 61L, 62L), null, new BigDecimal("7000.00")
        );
        assertLifecycleEvent(
                published.get(2), AuctionPrivateNotificationKind.UNSOLD,
                null, List.of(), null, null
        );
        assertLifecycleEvent(
                published.get(3), AuctionPrivateNotificationKind.CANCELLED,
                null, List.of(61L, 62L), "賣家取消", null
        );
        assertLifecycleEvent(
                published.get(4), AuctionPrivateNotificationKind.REMOVED,
                null, List.of(61L), "  違反規範  ", null
        );
        assertLifecycleEvent(
                published.get(5), AuctionPrivateNotificationKind.RESTORED,
                null, List.of(61L), "申訴成立", null
        );
    }

    private void assertOrderEvent(Object published, AuctionPrivateNotificationKind kind) {
        assertTrue(published instanceof AuctionPrivateNotificationReadyEvent);
        AuctionPrivateNotificationReadyEvent event =
                (AuctionPrivateNotificationReadyEvent) published;
        assertEquals(kind, event.kind());
        assertEquals(123L, event.auctionId());
        assertEquals(456L, event.orderId());
        assertEquals("AUC-20260916-000456", event.orderNumber());
        assertEquals(41L, event.memberId());
        assertEquals(31L, event.sellerId());
        assertEquals(List.of(), event.participantIds());
        assertNull(event.reason());
        assertNull(event.amount());
        assertEquals(OCCURRED_AT, event.occurredAt());
    }

    private void assertLifecycleEvent(
            Object published,
            AuctionPrivateNotificationKind kind,
            Long memberId,
            List<Long> participantIds,
            String reason,
            BigDecimal amount) {
        assertTrue(published instanceof AuctionPrivateNotificationReadyEvent);
        AuctionPrivateNotificationReadyEvent event =
                (AuctionPrivateNotificationReadyEvent) published;
        assertEquals(kind, event.kind());
        assertEquals(123L, event.auctionId());
        assertNull(event.orderId());
        assertNull(event.orderNumber());
        assertEquals(memberId, event.memberId());
        assertEquals(31L, event.sellerId());
        assertEquals(participantIds, event.participantIds());
        assertEquals(reason, event.reason());
        assertEquals(amount, event.amount());
        assertEquals(OCCURRED_AT, event.occurredAt());
    }
}
