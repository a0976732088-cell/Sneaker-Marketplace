package com.project.sneakermarket.auction.notification;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
    private static final Long ORDER_ID = 456L;
    private static final String ORDER_NUMBER = "AUC-20260916-000456";

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
    void startedAndUnsoldSendOnlyToSellerOnTheExistingDestination() {
        AuctionPrivateNotificationReadyEvent started = lifecycleEvent(
                AuctionPrivateNotificationKind.STARTED,
                null,
                31L,
                List.of(),
                null,
                null
        );
        AuctionPrivateNotificationReadyEvent unsold = lifecycleEvent(
                AuctionPrivateNotificationKind.UNSOLD,
                null,
                31L,
                List.of(),
                null,
                null
        );
        AuctionPrivateNotificationDelivery startedDelivery = delivery(
                31L, AuctionPrivateRealtimeEventType.AUCTION_STARTED, 910L
        );
        AuctionPrivateNotificationDelivery unsoldDelivery = delivery(
                31L, AuctionPrivateRealtimeEventType.AUCTION_UNSOLD, 911L
        );
        when(notifications.createStartedSellerNotification(123L, 31L, OCCURRED_AT))
                .thenReturn(Optional.of(startedDelivery));
        when(notifications.createUnsoldSellerNotification(123L, 31L, OCCURRED_AT))
                .thenReturn(Optional.of(unsoldDelivery));

        listener.onPrivateNotificationReady(started);
        listener.onPrivateNotificationReady(unsold);

        verify(messaging).convertAndSendToUser(
                "31", "/queue/auction-events", startedDelivery.message()
        );
        verify(messaging).convertAndSendToUser(
                "31", "/queue/auction-events", unsoldDelivery.message()
        );
        verifyNoMoreInteractions(messaging);
    }

    @Test
    void soldSnapshotSendsSellerWinnerAndDistinctLosersWithoutPostCommitLookup() {
        AuctionPrivateNotificationReadyEvent event = lifecycleEvent(
                AuctionPrivateNotificationKind.SOLD,
                77L,
                31L,
                java.util.Arrays.asList(77L, 61L, 61L, 31L, null, 62L),
                null,
                new BigDecimal("7000.00")
        );
        AuctionPrivateNotificationDelivery seller = delivery(
                31L, AuctionPrivateRealtimeEventType.AUCTION_SOLD, 912L
        );
        AuctionPrivateNotificationDelivery winner = delivery(
                77L, AuctionPrivateRealtimeEventType.AUCTION_WON, 913L
        );
        AuctionPrivateNotificationDelivery loserA = delivery(
                61L, AuctionPrivateRealtimeEventType.AUCTION_LOST, 914L
        );
        AuctionPrivateNotificationDelivery loserB = delivery(
                62L, AuctionPrivateRealtimeEventType.AUCTION_LOST, 915L
        );
        when(notifications.createSoldSellerNotification(
                123L, 31L, new BigDecimal("7000.00"), OCCURRED_AT
        )).thenReturn(Optional.of(seller));
        when(notifications.createWonNotification(
                123L, 77L, new BigDecimal("7000.00"), OCCURRED_AT
        )).thenReturn(Optional.of(winner));
        when(notifications.createLostNotification(123L, 61L, 77L, OCCURRED_AT))
                .thenReturn(Optional.of(loserA));
        when(notifications.createLostNotification(123L, 62L, 77L, OCCURRED_AT))
                .thenReturn(Optional.of(loserB));

        listener.onPrivateNotificationReady(event);

        verify(messaging).convertAndSendToUser(
                "31", "/queue/auction-events", seller.message()
        );
        verify(messaging).convertAndSendToUser(
                "77", "/queue/auction-events", winner.message()
        );
        verify(messaging).convertAndSendToUser(
                "61", "/queue/auction-events", loserA.message()
        );
        verify(messaging).convertAndSendToUser(
                "62", "/queue/auction-events", loserB.message()
        );
        verify(notifications, never()).findDistinctLosingBidderIds(123L, 77L);
        verify(notifications, never()).createLostNotification(123L, 31L, 77L, OCCURRED_AT);
        verifyNoMoreInteractions(messaging);
    }

    @Test
    void cancelledSellerFailureDoesNotPreventParticipantDeliveries() {
        AuctionPrivateNotificationReadyEvent event = lifecycleEvent(
                AuctionPrivateNotificationKind.CANCELLED,
                null,
                31L,
                List.of(31L, 61L, 62L),
                "seller cancelled",
                null
        );
        AuctionPrivateNotificationDelivery participantA = delivery(
                61L, AuctionPrivateRealtimeEventType.AUCTION_CANCELLED, 916L
        );
        AuctionPrivateNotificationDelivery participantB = delivery(
                62L, AuctionPrivateRealtimeEventType.AUCTION_CANCELLED, 917L
        );
        when(notifications.createCancelledSellerNotification(123L, 31L, OCCURRED_AT))
                .thenThrow(new IllegalStateException("seller notification failed"));
        when(notifications.createCancelledParticipantNotification(123L, 61L, OCCURRED_AT))
                .thenReturn(Optional.of(participantA));
        when(notifications.createCancelledParticipantNotification(123L, 62L, OCCURRED_AT))
                .thenReturn(Optional.of(participantB));

        assertDoesNotThrow(() -> listener.onPrivateNotificationReady(event));

        verify(messaging).convertAndSendToUser(
                "61", "/queue/auction-events", participantA.message()
        );
        verify(messaging).convertAndSendToUser(
                "62", "/queue/auction-events", participantB.message()
        );
        verify(messaging, never()).convertAndSendToUser(
                org.mockito.ArgumentMatchers.eq("31"),
                org.mockito.ArgumentMatchers.eq("/queue/auction-events"),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void removedWebsocketFailureDoesNotPreventLaterParticipantDelivery() {
        AuctionPrivateNotificationReadyEvent event = lifecycleEvent(
                AuctionPrivateNotificationKind.REMOVED,
                null,
                31L,
                List.of(61L),
                "違反規範",
                null
        );
        AuctionPrivateNotificationDelivery seller = delivery(
                31L, AuctionPrivateRealtimeEventType.AUCTION_REMOVED, 918L
        );
        AuctionPrivateNotificationDelivery participant = delivery(
                61L, AuctionPrivateRealtimeEventType.AUCTION_REMOVED, 919L
        );
        when(notifications.createRemovedSellerNotification(
                123L, 31L, "違反規範", OCCURRED_AT
        )).thenReturn(seller);
        when(notifications.createRemovedParticipantNotification(123L, 61L, OCCURRED_AT))
                .thenReturn(participant);
        org.mockito.Mockito.doThrow(new IllegalStateException("broker unavailable"))
                .when(messaging)
                .convertAndSendToUser("31", "/queue/auction-events", seller.message());

        assertDoesNotThrow(() -> listener.onPrivateNotificationReady(event));

        verify(messaging).convertAndSendToUser(
                "61", "/queue/auction-events", participant.message()
        );
    }

    @Test
    void removedAndRestoredNotificationFailuresDoNotBlockOtherRecipients() {
        AuctionPrivateNotificationReadyEvent removed = lifecycleEvent(
                AuctionPrivateNotificationKind.REMOVED,
                null,
                31L,
                List.of(61L),
                "違反規範",
                null
        );
        AuctionPrivateNotificationReadyEvent restored = lifecycleEvent(
                AuctionPrivateNotificationKind.RESTORED,
                null,
                31L,
                List.of(61L),
                null,
                null
        );
        AuctionPrivateNotificationDelivery removedParticipant = delivery(
                61L, AuctionPrivateRealtimeEventType.AUCTION_REMOVED, 922L
        );
        AuctionPrivateNotificationDelivery restoredParticipant = delivery(
                61L, AuctionPrivateRealtimeEventType.AUCTION_RESTORED, 923L
        );
        when(notifications.createRemovedSellerNotification(
                123L, 31L, "違反規範", OCCURRED_AT
        )).thenThrow(new IllegalStateException("removed notification failed"));
        when(notifications.createRemovedParticipantNotification(123L, 61L, OCCURRED_AT))
                .thenReturn(removedParticipant);
        when(notifications.createRestoredSellerNotification(123L, 31L, OCCURRED_AT))
                .thenThrow(new IllegalStateException("restored notification failed"));
        when(notifications.createRestoredParticipantNotification(123L, 61L, OCCURRED_AT))
                .thenReturn(restoredParticipant);

        assertDoesNotThrow(() -> listener.onPrivateNotificationReady(removed));
        assertDoesNotThrow(() -> listener.onPrivateNotificationReady(restored));

        verify(messaging).convertAndSendToUser(
                "61", "/queue/auction-events", removedParticipant.message()
        );
        verify(messaging).convertAndSendToUser(
                "61", "/queue/auction-events", restoredParticipant.message()
        );
        verify(messaging, never()).convertAndSendToUser(
                org.mockito.ArgumentMatchers.eq("31"),
                org.mockito.ArgumentMatchers.eq("/queue/auction-events"),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void restoredSendsSellerAndParticipantsButNeverTreatsSellerAsParticipant() {
        AuctionPrivateNotificationReadyEvent event = lifecycleEvent(
                AuctionPrivateNotificationKind.RESTORED,
                null,
                31L,
                List.of(31L, 61L),
                "申訴成立",
                null
        );
        AuctionPrivateNotificationDelivery seller = delivery(
                31L, AuctionPrivateRealtimeEventType.AUCTION_RESTORED, 920L
        );
        AuctionPrivateNotificationDelivery participant = delivery(
                61L, AuctionPrivateRealtimeEventType.AUCTION_RESTORED, 921L
        );
        when(notifications.createRestoredSellerNotification(123L, 31L, OCCURRED_AT))
                .thenReturn(seller);
        when(notifications.createRestoredParticipantNotification(123L, 61L, OCCURRED_AT))
                .thenReturn(participant);

        listener.onPrivateNotificationReady(event);

        verify(messaging).convertAndSendToUser(
                "31", "/queue/auction-events", seller.message()
        );
        verify(messaging).convertAndSendToUser(
                "61", "/queue/auction-events", participant.message()
        );
        verify(notifications, never()).createRestoredParticipantNotification(
                123L, 31L, OCCURRED_AT
        );
        verifyNoMoreInteractions(messaging);
    }

    @Test
    void orderCreatedCreatesAndSendsOnlyToBuyer() {
        AuctionPrivateNotificationReadyEvent event = orderEvent(
                AuctionPrivateNotificationKind.ORDER_CREATED
        );
        AuctionPrivateNotificationDelivery created = delivery(
                41L,
                AuctionPrivateRealtimeEventType.AUCTION_ORDER_CREATED,
                905L
        );
        when(notifications.createOrderCreatedNotification(
                123L, ORDER_ID, ORDER_NUMBER, 41L, OCCURRED_AT
        )).thenReturn(Optional.of(created));

        listener.onPrivateNotificationReady(event);

        InOrder order = inOrder(notifications, messaging);
        order.verify(notifications).createOrderCreatedNotification(
                123L, ORDER_ID, ORDER_NUMBER, 41L, OCCURRED_AT
        );
        order.verify(messaging).convertAndSendToUser(
                "41", "/queue/auction-events", created.message()
        );
        verifyNoMoreInteractions(messaging);
    }

    @Test
    void orderPaidCreatesAndSendsOnlyToSeller() {
        AuctionPrivateNotificationReadyEvent event = orderEvent(
                AuctionPrivateNotificationKind.ORDER_PAID
        );
        AuctionPrivateNotificationDelivery paid = delivery(
                31L,
                AuctionPrivateRealtimeEventType.AUCTION_ORDER_PAID,
                906L
        );
        when(notifications.createOrderPaidNotification(
                123L, ORDER_ID, 31L, OCCURRED_AT
        )).thenReturn(Optional.of(paid));

        listener.onPrivateNotificationReady(event);

        InOrder order = inOrder(notifications, messaging);
        order.verify(notifications).createOrderPaidNotification(
                123L, ORDER_ID, 31L, OCCURRED_AT
        );
        order.verify(messaging).convertAndSendToUser(
                "31", "/queue/auction-events", paid.message()
        );
        verifyNoMoreInteractions(messaging);
    }

    @Test
    void orderAbandonedCreatesRoleSpecificNotificationsForBuyerAndSeller() {
        AuctionPrivateNotificationReadyEvent event = orderEvent(
                AuctionPrivateNotificationKind.ORDER_ABANDONED
        );
        AuctionPrivateNotificationDelivery buyer = delivery(
                41L,
                AuctionPrivateRealtimeEventType.AUCTION_ORDER_ABANDONED,
                907L
        );
        AuctionPrivateNotificationDelivery seller = delivery(
                31L,
                AuctionPrivateRealtimeEventType.AUCTION_ORDER_ABANDONED,
                908L
        );
        when(notifications.createOrderAbandonedBuyerNotification(
                123L, ORDER_ID, ORDER_NUMBER, 41L, OCCURRED_AT
        )).thenReturn(Optional.of(buyer));
        when(notifications.createOrderAbandonedSellerNotification(
                123L, ORDER_ID, 31L, OCCURRED_AT
        )).thenReturn(Optional.of(seller));

        listener.onPrivateNotificationReady(event);

        verify(messaging).convertAndSendToUser(
                "41", "/queue/auction-events", buyer.message()
        );
        verify(messaging).convertAndSendToUser(
                "31", "/queue/auction-events", seller.message()
        );
        verifyNoMoreInteractions(messaging);
    }

    @Test
    void duplicateOrderNotificationsDoNotSendWebSocketMessages() {
        AuctionPrivateNotificationReadyEvent event = orderEvent(
                AuctionPrivateNotificationKind.ORDER_ABANDONED
        );
        when(notifications.createOrderAbandonedBuyerNotification(
                123L, ORDER_ID, ORDER_NUMBER, 41L, OCCURRED_AT
        )).thenReturn(Optional.empty());
        when(notifications.createOrderAbandonedSellerNotification(
                123L, ORDER_ID, 31L, OCCURRED_AT
        )).thenReturn(Optional.empty());

        listener.onPrivateNotificationReady(event);

        verifyNoInteractions(messaging);
    }

    @Test
    void abandonedBuyerNotificationFailureDoesNotPreventSellerDelivery() {
        AuctionPrivateNotificationReadyEvent event = orderEvent(
                AuctionPrivateNotificationKind.ORDER_ABANDONED
        );
        AuctionPrivateNotificationDelivery seller = delivery(
                31L,
                AuctionPrivateRealtimeEventType.AUCTION_ORDER_ABANDONED,
                908L
        );
        when(notifications.createOrderAbandonedBuyerNotification(
                123L, ORDER_ID, ORDER_NUMBER, 41L, OCCURRED_AT
        )).thenThrow(new IllegalStateException("buyer notification failed"));
        when(notifications.createOrderAbandonedSellerNotification(
                123L, ORDER_ID, 31L, OCCURRED_AT
        )).thenReturn(Optional.of(seller));

        assertDoesNotThrow(() -> listener.onPrivateNotificationReady(event));

        verify(messaging, never()).convertAndSendToUser(
                org.mockito.ArgumentMatchers.eq("41"),
                org.mockito.ArgumentMatchers.eq("/queue/auction-events"),
                org.mockito.ArgumentMatchers.any()
        );
        verify(messaging).convertAndSendToUser(
                "31", "/queue/auction-events", seller.message()
        );
    }

    @Test
    void orderWebsocketFailureIsContainedAfterNotificationWasCreated() {
        AuctionPrivateNotificationReadyEvent event = orderEvent(
                AuctionPrivateNotificationKind.ORDER_PAID
        );
        AuctionPrivateNotificationDelivery paid = delivery(
                31L,
                AuctionPrivateRealtimeEventType.AUCTION_ORDER_PAID,
                906L
        );
        when(notifications.createOrderPaidNotification(
                123L, ORDER_ID, 31L, OCCURRED_AT
        )).thenReturn(Optional.of(paid));
        org.mockito.Mockito.doThrow(new IllegalStateException("broker unavailable"))
                .when(messaging)
                .convertAndSendToUser("31", "/queue/auction-events", paid.message());

        assertDoesNotThrow(() -> listener.onPrivateNotificationReady(event));

        verify(notifications).createOrderPaidNotification(
                123L, ORDER_ID, 31L, OCCURRED_AT
        );
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

    private AuctionPrivateNotificationReadyEvent orderEvent(
            AuctionPrivateNotificationKind kind) {
        return new AuctionPrivateNotificationReadyEvent(
                kind,
                123L,
                ORDER_ID,
                ORDER_NUMBER,
                41L,
                31L,
                null,
                OCCURRED_AT
        );
    }

    private AuctionPrivateNotificationReadyEvent lifecycleEvent(
            AuctionPrivateNotificationKind kind,
            Long memberId,
            Long sellerId,
            List<Long> participantIds,
            String reason,
            BigDecimal amount) {
        return new AuctionPrivateNotificationReadyEvent(
                kind,
                123L,
                null,
                null,
                memberId,
                sellerId,
                participantIds,
                reason,
                amount,
                OCCURRED_AT
        );
    }
}
