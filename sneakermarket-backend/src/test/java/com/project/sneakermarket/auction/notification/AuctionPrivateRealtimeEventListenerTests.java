package com.project.sneakermarket.auction.notification;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class AuctionPrivateRealtimeEventListenerTests {

    private static final LocalDateTime OCCURRED_AT =
            LocalDateTime.of(2026, 9, 16, 12, 30);

    private AuctionNotificationIntegrationService notifications;
    private SimpMessagingTemplate messaging;
    private AuctionPrivateRealtimeEventListener listener;

    @BeforeEach
    void setup() {
        notifications = mock(AuctionNotificationIntegrationService.class);
        messaging = mock(SimpMessagingTemplate.class);
        listener = new AuctionPrivateRealtimeEventListener(notifications, messaging);
    }

    @Test
    void outbidCreatesNotificationBeforeSendingOnlyToPreviousHighestBidder() {
        AuctionPrivateNotificationReadyEvent event = new AuctionPrivateNotificationReadyEvent(
                AuctionPrivateNotificationKind.OUTBID,
                123L,
                41L,
                new BigDecimal("6500.00"),
                OCCURRED_AT
        );
        AuctionPrivateNotificationDelivery delivery = delivery(
                41L,
                AuctionPrivateRealtimeEventType.AUCTION_OUTBID,
                901L
        );
        when(notifications.createOutbidNotification(
                123L, 41L, new BigDecimal("6500.00"), OCCURRED_AT
        )).thenReturn(delivery);

        listener.onPrivateNotificationReady(event);

        InOrder order = inOrder(notifications, messaging);
        order.verify(notifications).createOutbidNotification(
                123L, 41L, new BigDecimal("6500.00"), OCCURRED_AT
        );
        order.verify(messaging).convertAndSendToUser(
                "41", "/queue/auction-events", delivery.message()
        );
        verify(notifications, never()).createWonNotification(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void soldCreatesWinnerAndOneNotificationForEachDistinctLoser() {
        AuctionPrivateNotificationReadyEvent event = new AuctionPrivateNotificationReadyEvent(
                AuctionPrivateNotificationKind.SOLD,
                123L,
                77L,
                new BigDecimal("7000.00"),
                OCCURRED_AT
        );
        AuctionPrivateNotificationDelivery won = delivery(
                77L,
                AuctionPrivateRealtimeEventType.AUCTION_WON,
                902L
        );
        AuctionPrivateNotificationDelivery lostA = delivery(
                61L,
                AuctionPrivateRealtimeEventType.AUCTION_LOST,
                903L
        );
        AuctionPrivateNotificationDelivery lostB = delivery(
                62L,
                AuctionPrivateRealtimeEventType.AUCTION_LOST,
                904L
        );
        when(notifications.createWonNotification(
                123L, 77L, new BigDecimal("7000.00"), OCCURRED_AT
        )).thenReturn(Optional.of(won));
        when(notifications.findDistinctLosingBidderIds(123L, 77L))
                .thenReturn(List.of(61L, 62L));
        when(notifications.createLostNotification(123L, 61L, 77L, OCCURRED_AT))
                .thenReturn(Optional.of(lostA));
        when(notifications.createLostNotification(123L, 62L, 77L, OCCURRED_AT))
                .thenReturn(Optional.of(lostB));

        listener.onPrivateNotificationReady(event);

        verify(messaging).convertAndSendToUser(
                "77", "/queue/auction-events", won.message()
        );
        verify(messaging).convertAndSendToUser(
                "61", "/queue/auction-events", lostA.message()
        );
        verify(messaging).convertAndSendToUser(
                "62", "/queue/auction-events", lostB.message()
        );
        verify(notifications, never()).createLostNotification(123L, 77L, 77L, OCCURRED_AT);
    }

    @Test
    void idempotentEmptyResultsDoNotSendDuplicateWebSocketMessages() {
        AuctionPrivateNotificationReadyEvent event = new AuctionPrivateNotificationReadyEvent(
                AuctionPrivateNotificationKind.SOLD,
                123L,
                77L,
                new BigDecimal("7000.00"),
                OCCURRED_AT
        );
        when(notifications.createWonNotification(
                123L, 77L, new BigDecimal("7000.00"), OCCURRED_AT
        )).thenReturn(Optional.empty());
        when(notifications.findDistinctLosingBidderIds(123L, 77L))
                .thenReturn(List.of(61L));
        when(notifications.createLostNotification(123L, 61L, 77L, OCCURRED_AT))
                .thenReturn(Optional.empty());

        listener.onPrivateNotificationReady(event);

        verifyNoInteractions(messaging);
    }

    @Test
    void websocketFailureIsContainedAfterNotificationWasCreated() {
        AuctionPrivateNotificationReadyEvent event = new AuctionPrivateNotificationReadyEvent(
                AuctionPrivateNotificationKind.OUTBID,
                123L,
                41L,
                new BigDecimal("6500.00"),
                OCCURRED_AT
        );
        AuctionPrivateNotificationDelivery delivery = delivery(
                41L,
                AuctionPrivateRealtimeEventType.AUCTION_OUTBID,
                901L
        );
        when(notifications.createOutbidNotification(
                123L, 41L, new BigDecimal("6500.00"), OCCURRED_AT
        )).thenReturn(delivery);
        org.mockito.Mockito.doThrow(new IllegalStateException("broker unavailable"))
                .when(messaging)
                .convertAndSendToUser("41", "/queue/auction-events", delivery.message());

        assertDoesNotThrow(() -> listener.onPrivateNotificationReady(event));

        verify(notifications).createOutbidNotification(
                123L, 41L, new BigDecimal("6500.00"), OCCURRED_AT
        );
    }

    @Test
    void notificationWriteFailureIsContainedAndNeverAttemptsWebSocketSend() {
        AuctionPrivateNotificationReadyEvent event = new AuctionPrivateNotificationReadyEvent(
                AuctionPrivateNotificationKind.OUTBID,
                123L,
                41L,
                new BigDecimal("6500.00"),
                OCCURRED_AT
        );
        when(notifications.createOutbidNotification(
                123L, 41L, new BigDecimal("6500.00"), OCCURRED_AT
        )).thenThrow(new IllegalStateException("database unavailable"));

        assertDoesNotThrow(() -> listener.onPrivateNotificationReady(event));

        verifyNoInteractions(messaging);
    }

    @Test
    void losingBidderLookupFailureDoesNotUndoOrResendWinnerNotification() {
        AuctionPrivateNotificationReadyEvent event = new AuctionPrivateNotificationReadyEvent(
                AuctionPrivateNotificationKind.SOLD,
                123L,
                77L,
                new BigDecimal("7000.00"),
                OCCURRED_AT
        );
        AuctionPrivateNotificationDelivery won = delivery(
                77L,
                AuctionPrivateRealtimeEventType.AUCTION_WON,
                902L
        );
        when(notifications.createWonNotification(
                123L, 77L, new BigDecimal("7000.00"), OCCURRED_AT
        )).thenReturn(Optional.of(won));
        when(notifications.findDistinctLosingBidderIds(123L, 77L))
                .thenThrow(new IllegalStateException("query failed"));

        assertDoesNotThrow(() -> listener.onPrivateNotificationReady(event));

        verify(messaging).convertAndSendToUser(
                "77", "/queue/auction-events", won.message()
        );
    }

    private AuctionPrivateNotificationDelivery delivery(
            Long memberId,
            AuctionPrivateRealtimeEventType type,
            Long notificationId) {
        return new AuctionPrivateNotificationDelivery(
                memberId,
                new AuctionPrivateRealtimeMessage(
                        type,
                        123L,
                        notificationId,
                        OCCURRED_AT
                )
        );
    }
}
