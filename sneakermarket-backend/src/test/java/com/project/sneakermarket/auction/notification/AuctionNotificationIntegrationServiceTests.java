package com.project.sneakermarket.auction.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.project.sneakermarket.auction.repository.AuctionBidRepository;
import com.project.sneakermarket.notification.entity.Notification;
import com.project.sneakermarket.notification.repository.NotificationRepository;

class AuctionNotificationIntegrationServiceTests {

    private static final Long AUCTION_ID = 123L;
    private static final LocalDateTime OCCURRED_AT =
            LocalDateTime.of(2026, 9, 16, 12, 30);

    private NotificationRepository notifications;
    private AuctionBidRepository bids;
    private AuctionNotificationIntegrationService service;
    private long nextNotificationId;

    @BeforeEach
    void setup() {
        notifications = mock(NotificationRepository.class);
        bids = mock(AuctionBidRepository.class);
        service = new AuctionNotificationIntegrationService(notifications, bids);
        nextNotificationId = 900L;
        when(notifications.saveAndFlush(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(nextNotificationId++);
            return notification;
        });
    }

    @Test
    void outbidAlwaysCreatesExistingNotificationShapeAndMinimalRealtimeDelivery() {
        AuctionPrivateNotificationDelivery delivery = service.createOutbidNotification(
                AUCTION_ID,
                41L,
                new BigDecimal("6500.00"),
                OCCURRED_AT
        );

        Notification notification = savedNotification();
        assertNotification(
                notification,
                41L,
                "AUCTION_OUTBID",
                "您的競標出價已被超越",
                "競標 #123",
                "6,500"
        );
        assertDelivery(
                delivery,
                41L,
                AuctionPrivateRealtimeEventType.AUCTION_OUTBID,
                900L
        );
        verify(notifications, never())
                .existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
                        any(), any(), any(), any()
                );
    }

    @Test
    void wonCreatesOnceAndDuplicateLifecycleRunReturnsEmpty() {
        when(notifications.existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
                77L, "AUCTION_WON", "AUCTION", AUCTION_ID
        )).thenReturn(false, true);

        var first = service.createWonNotification(
                AUCTION_ID,
                77L,
                new BigDecimal("7000.00"),
                OCCURRED_AT
        );
        var duplicate = service.createWonNotification(
                AUCTION_ID,
                77L,
                new BigDecimal("7000.00"),
                OCCURRED_AT
        );

        assertTrue(first.isPresent());
        assertTrue(duplicate.isEmpty());
        Notification notification = savedNotification();
        assertNotification(
                notification,
                77L,
                "AUCTION_WON",
                "恭喜您競標得標",
                "競標 #123",
                "7,000"
        );
        assertDelivery(
                first.orElseThrow(),
                77L,
                AuctionPrivateRealtimeEventType.AUCTION_WON,
                900L
        );
        verify(notifications).saveAndFlush(any(Notification.class));
    }

    @Test
    void lostCreatesOnePerDistinctLoserAndNeverForWinner() {
        when(bids.findDistinctLosingBidderIds(AUCTION_ID, 77L)).thenReturn(
                Arrays.asList(61L, 61L, null, 62L, 77L)
        );
        when(notifications.existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
                61L, "AUCTION_LOST", "AUCTION", AUCTION_ID
        )).thenReturn(false, true);

        List<Long> losers = service.findDistinctLosingBidderIds(AUCTION_ID, 77L);
        var first = service.createLostNotification(AUCTION_ID, 61L, 77L, OCCURRED_AT);
        var duplicate = service.createLostNotification(AUCTION_ID, 61L, 77L, OCCURRED_AT);
        var winner = service.createLostNotification(AUCTION_ID, 77L, 77L, OCCURRED_AT);
        var nullLoser = service.createLostNotification(AUCTION_ID, null, 77L, OCCURRED_AT);

        assertEquals(List.of(61L, 62L), losers);
        assertTrue(first.isPresent());
        assertTrue(duplicate.isEmpty());
        assertTrue(winner.isEmpty());
        assertTrue(nullLoser.isEmpty());
        Notification notification = savedNotification();
        assertNotification(
                notification,
                61L,
                "AUCTION_LOST",
                "本次競標未得標",
                "競標 #123",
                "由其他會員得標"
        );
        assertDelivery(
                first.orElseThrow(),
                61L,
                AuctionPrivateRealtimeEventType.AUCTION_LOST,
                900L
        );
        verify(notifications).saveAndFlush(any(Notification.class));
    }

    @Test
    void notificationWritesUseRequiresNewAndLosingBidLookupIsReadOnly() throws Exception {
        assertTransaction("createOutbidNotification", false,
                Long.class, Long.class, BigDecimal.class, LocalDateTime.class);
        assertTransaction("createWonNotification", false,
                Long.class, Long.class, BigDecimal.class, LocalDateTime.class);
        assertTransaction("createLostNotification", false,
                Long.class, Long.class, Long.class, LocalDateTime.class);
        assertTransaction("findDistinctLosingBidderIds", true, Long.class, Long.class);
    }

    @Test
    void privateRealtimeContractContainsNoMemberOrPersonalData() {
        Set<String> fields = Arrays.stream(AuctionPrivateRealtimeMessage.class.getRecordComponents())
                .map(component -> component.getName())
                .collect(Collectors.toSet());

        assertEquals(Set.of("eventType", "auctionId", "notificationId", "occurredAt"), fields);
        assertFalse(fields.stream().anyMatch(name -> Set.of(
                "memberId", "recipientId", "email", "phone", "address", "sessionId"
        ).contains(name)));
    }

    private Notification savedNotification() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private void assertNotification(
            Notification notification,
            Long memberId,
            String type,
            String title,
            String... contentFragments) {
        assertEquals(memberId, notification.getMemberId());
        assertEquals(type, notification.getNotificationType());
        assertEquals(title, notification.getTitle());
        for (String fragment : contentFragments) {
            assertTrue(notification.getContent().contains(fragment));
        }
        assertEquals("AUCTION", notification.getTargetType());
        assertEquals(AUCTION_ID, notification.getTargetId());
        assertEquals("/auctions/123", notification.getTargetUrl());
        assertFalse(notification.getIsRead());
        assertTrue(notification.getIsSent());
        assertEquals(OCCURRED_AT, notification.getSentAt());
    }

    private void assertDelivery(
            AuctionPrivateNotificationDelivery delivery,
            Long recipient,
            AuctionPrivateRealtimeEventType type,
            Long notificationId) {
        assertEquals(recipient, delivery.recipientMemberId());
        assertEquals(type, delivery.message().eventType());
        assertEquals(AUCTION_ID, delivery.message().auctionId());
        assertEquals(notificationId, delivery.message().notificationId());
        assertEquals(OCCURRED_AT, delivery.message().occurredAt());
    }

    private void assertTransaction(
            String methodName,
            boolean readOnly,
            Class<?>... parameterTypes) throws Exception {
        Method method = AuctionNotificationIntegrationService.class
                .getMethod(methodName, parameterTypes);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
        assertEquals(readOnly, transactional.readOnly());
    }
}
