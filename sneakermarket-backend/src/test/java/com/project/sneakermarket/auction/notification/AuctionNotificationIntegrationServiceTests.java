package com.project.sneakermarket.auction.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    private static final Long ORDER_ID = 456L;
    private static final String ORDER_NUMBER = "AUC-20260916-000456";
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
    void sellerLifecycleNotificationsUseExactContentAndAreIdempotent() {
        when(notifications.existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
                31L, "AUCTION_STARTED", "AUCTION", AUCTION_ID
        )).thenReturn(false, true);
        when(notifications.existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
                31L, "AUCTION_SOLD", "AUCTION", AUCTION_ID
        )).thenReturn(false, true);
        when(notifications.existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
                31L, "AUCTION_UNSOLD", "AUCTION", AUCTION_ID
        )).thenReturn(false, true);

        var started = service.createStartedSellerNotification(AUCTION_ID, 31L, OCCURRED_AT);
        var duplicateStarted = service.createStartedSellerNotification(
                AUCTION_ID, 31L, OCCURRED_AT
        );
        var sold = service.createSoldSellerNotification(
                AUCTION_ID, 31L, new BigDecimal("7000.00"), OCCURRED_AT
        );
        var duplicateSold = service.createSoldSellerNotification(
                AUCTION_ID, 31L, new BigDecimal("7000.00"), OCCURRED_AT
        );
        var unsold = service.createUnsoldSellerNotification(AUCTION_ID, 31L, OCCURRED_AT);
        var duplicateUnsold = service.createUnsoldSellerNotification(
                AUCTION_ID, 31L, OCCURRED_AT
        );

        assertTrue(started.isPresent());
        assertTrue(sold.isPresent());
        assertTrue(unsold.isPresent());
        assertTrue(duplicateStarted.isEmpty());
        assertTrue(duplicateSold.isEmpty());
        assertTrue(duplicateUnsold.isEmpty());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(3)).saveAndFlush(captor.capture());
        List<Notification> saved = captor.getAllValues();
        assertAuctionLifecycleNotification(
                saved.get(0), 31L, "AUCTION_STARTED", "您的競標已開始",
                "您的競標已正式開始，目前已開放會員出價。", "/auctions/me/listings"
        );
        assertAuctionLifecycleNotification(
                saved.get(1), 31L, "AUCTION_SOLD", "您的競標已結標",
                "您的競標已成功結標，商品已有會員得標，最終成交金額為 NT$ 7,000。",
                "/auctions/me/listings"
        );
        assertAuctionLifecycleNotification(
                saved.get(2), 31L, "AUCTION_UNSOLD", "您的競標已流標",
                "您的競標已結束，本次沒有會員得標。", "/auctions/me/listings"
        );
        assertDelivery(
                started.orElseThrow(), 31L,
                AuctionPrivateRealtimeEventType.AUCTION_STARTED, 900L
        );
        assertDelivery(
                sold.orElseThrow(), 31L,
                AuctionPrivateRealtimeEventType.AUCTION_SOLD, 901L
        );
        assertDelivery(
                unsold.orElseThrow(), 31L,
                AuctionPrivateRealtimeEventType.AUCTION_UNSOLD, 902L
        );
    }

    @Test
    void cancelledUsesRoleSpecificContentAndOneNotificationPerRecipient() {
        when(notifications.existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
                31L, "AUCTION_CANCELLED", "AUCTION", AUCTION_ID
        )).thenReturn(false, true);
        when(notifications.existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
                61L, "AUCTION_CANCELLED", "AUCTION", AUCTION_ID
        )).thenReturn(false, true);

        var seller = service.createCancelledSellerNotification(AUCTION_ID, 31L, OCCURRED_AT);
        var participant = service.createCancelledParticipantNotification(
                AUCTION_ID, 61L, OCCURRED_AT
        );
        var duplicateSeller = service.createCancelledSellerNotification(
                AUCTION_ID, 31L, OCCURRED_AT
        );
        var duplicateParticipant = service.createCancelledParticipantNotification(
                AUCTION_ID, 61L, OCCURRED_AT
        );

        assertTrue(seller.isPresent());
        assertTrue(participant.isPresent());
        assertTrue(duplicateSeller.isEmpty());
        assertTrue(duplicateParticipant.isEmpty());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(2)).saveAndFlush(captor.capture());
        List<Notification> saved = captor.getAllValues();
        assertAuctionLifecycleNotification(
                saved.get(0), 31L, "AUCTION_CANCELLED", "您的競標已取消",
                "此競標已成功取消。", "/auctions/me/listings"
        );
        assertAuctionLifecycleNotification(
                saved.get(1), 61L, "AUCTION_CANCELLED", "您參與的競標已被取消",
                "您參與的競標已被取消，後續將無法繼續出價。", "/auctions/me/bids"
        );
        assertDelivery(
                seller.orElseThrow(), 31L,
                AuctionPrivateRealtimeEventType.AUCTION_CANCELLED, 900L
        );
        assertDelivery(
                participant.orElseThrow(), 61L,
                AuctionPrivateRealtimeEventType.AUCTION_CANCELLED, 901L
        );
    }

    @Test
    void removedAndRestoredAllowLaterCyclesWithoutPermanentExistsDeduplication() {
        var removedSeller = service.createRemovedSellerNotification(
                AUCTION_ID, 31L, "  違反刊登規範  ", OCCURRED_AT
        );
        service.createRemovedSellerNotification(
                AUCTION_ID, 31L, "  違反刊登規範  ", OCCURRED_AT
        );
        var removedParticipant = service.createRemovedParticipantNotification(
                AUCTION_ID, 61L, OCCURRED_AT
        );
        var restoredSeller = service.createRestoredSellerNotification(
                AUCTION_ID, 31L, OCCURRED_AT
        );
        var restoredParticipant = service.createRestoredParticipantNotification(
                AUCTION_ID, 61L, OCCURRED_AT
        );
        service.createRestoredParticipantNotification(AUCTION_ID, 61L, OCCURRED_AT);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(6)).saveAndFlush(captor.capture());
        List<Notification> saved = captor.getAllValues();
        assertAuctionLifecycleNotification(
                saved.get(0), 31L, "AUCTION_REMOVED", "您的競標已被平台下架",
                "您刊登的競標已被平台下架，目前已停止公開顯示與後續競標行為。 下架原因：違反刊登規範",
                "/auctions/me/listings"
        );
        assertAuctionLifecycleNotification(
                saved.get(2), 61L, "AUCTION_REMOVED", "您參與的競標已被平台下架",
                "您參與的競標已被平台下架，目前已無法繼續出價。", "/auctions/me/bids"
        );
        assertAuctionLifecycleNotification(
                saved.get(3), 31L, "AUCTION_RESTORED", "您的競標已恢復",
                "您刊登的競標已由平台恢復顯示，請查看目前競標狀態。", "/auctions/me/listings"
        );
        assertAuctionLifecycleNotification(
                saved.get(4), 61L, "AUCTION_RESTORED", "您參與的競標已恢復",
                "您先前參與的競標已由平台恢復，請查看目前最新競標狀態。", "/auctions/me/bids"
        );
        assertDelivery(
                removedSeller, 31L, AuctionPrivateRealtimeEventType.AUCTION_REMOVED, 900L
        );
        assertDelivery(
                removedParticipant, 61L, AuctionPrivateRealtimeEventType.AUCTION_REMOVED, 902L
        );
        assertDelivery(
                restoredSeller, 31L, AuctionPrivateRealtimeEventType.AUCTION_RESTORED, 903L
        );
        assertDelivery(
                restoredParticipant, 61L, AuctionPrivateRealtimeEventType.AUCTION_RESTORED, 904L
        );
        verify(notifications, never())
                .existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
                        any(), any(), any(), any()
                );
    }

    @Test
    void orderCreatedCreatesOnlyBuyerNotificationAndReplayReturnsEmpty() {
        when(notifications.existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
                41L, "AUCTION_ORDER_CREATED", "AUCTION_ORDER", ORDER_ID
        )).thenReturn(false, true);

        var first = service.createOrderCreatedNotification(
                AUCTION_ID, ORDER_ID, ORDER_NUMBER, 41L, OCCURRED_AT
        );
        var duplicate = service.createOrderCreatedNotification(
                AUCTION_ID, ORDER_ID, ORDER_NUMBER, 41L, OCCURRED_AT
        );

        assertTrue(first.isPresent());
        assertTrue(duplicate.isEmpty());
        assertOrderNotification(
                savedNotification(),
                41L,
                "AUCTION_ORDER_CREATED",
                "您的競標訂單已建立",
                "/auction-orders/" + ORDER_NUMBER + "/payment",
                "付款期限內完成付款"
        );
        assertDelivery(
                first.orElseThrow(),
                41L,
                AuctionPrivateRealtimeEventType.AUCTION_ORDER_CREATED,
                900L
        );
    }

    @Test
    void orderPaidCreatesOnlySellerNotificationAndReplayReturnsEmpty() {
        when(notifications.existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
                31L, "AUCTION_ORDER_PAID", "AUCTION_ORDER", ORDER_ID
        )).thenReturn(false, true);

        var first = service.createOrderPaidNotification(
                AUCTION_ID, ORDER_ID, 31L, OCCURRED_AT
        );
        var duplicate = service.createOrderPaidNotification(
                AUCTION_ID, ORDER_ID, 31L, OCCURRED_AT
        );

        assertTrue(first.isPresent());
        assertTrue(duplicate.isEmpty());
        assertOrderNotification(
                savedNotification(),
                31L,
                "AUCTION_ORDER_PAID",
                "競標訂單已完成付款",
                "/auction-orders",
                "買家已完成競標訂單付款"
        );
        assertDelivery(
                first.orElseThrow(),
                31L,
                AuctionPrivateRealtimeEventType.AUCTION_ORDER_PAID,
                900L
        );
    }

    @Test
    void orderAbandonedCreatesRoleSpecificBuyerAndSellerNotificationsOnceEach() {
        when(notifications.existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
                41L, "AUCTION_ORDER_ABANDONED", "AUCTION_ORDER", ORDER_ID
        )).thenReturn(false, true);
        when(notifications.existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
                31L, "AUCTION_ORDER_ABANDONED", "AUCTION_ORDER", ORDER_ID
        )).thenReturn(false, true);

        var buyer = service.createOrderAbandonedBuyerNotification(
                AUCTION_ID, ORDER_ID, ORDER_NUMBER, 41L, OCCURRED_AT
        );
        var seller = service.createOrderAbandonedSellerNotification(
                AUCTION_ID, ORDER_ID, 31L, OCCURRED_AT
        );
        var duplicateBuyer = service.createOrderAbandonedBuyerNotification(
                AUCTION_ID, ORDER_ID, ORDER_NUMBER, 41L, OCCURRED_AT
        );
        var duplicateSeller = service.createOrderAbandonedSellerNotification(
                AUCTION_ID, ORDER_ID, 31L, OCCURRED_AT
        );

        assertTrue(buyer.isPresent());
        assertTrue(seller.isPresent());
        assertTrue(duplicateBuyer.isEmpty());
        assertTrue(duplicateSeller.isEmpty());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(2)).saveAndFlush(captor.capture());
        List<Notification> saved = captor.getAllValues();
        assertOrderNotification(
                saved.get(0),
                41L,
                "AUCTION_ORDER_ABANDONED",
                "競標訂單已逾期棄標",
                "/auction-orders/" + ORDER_NUMBER + "/payment",
                "超過付款期限"
        );
        assertOrderNotification(
                saved.get(1),
                31L,
                "AUCTION_ORDER_ABANDONED",
                "競標訂單已棄標",
                "/auction-orders",
                "買家未於付款期限內完成付款"
        );
        assertDelivery(
                buyer.orElseThrow(),
                41L,
                AuctionPrivateRealtimeEventType.AUCTION_ORDER_ABANDONED,
                900L
        );
        assertDelivery(
                seller.orElseThrow(),
                31L,
                AuctionPrivateRealtimeEventType.AUCTION_ORDER_ABANDONED,
                901L
        );
    }

    @Test
    void notificationWritesUseRequiresNewAndLosingBidLookupIsReadOnly() throws Exception {
        assertTransaction("createOutbidNotification", false,
                Long.class, Long.class, BigDecimal.class, LocalDateTime.class);
        assertTransaction("createWonNotification", false,
                Long.class, Long.class, BigDecimal.class, LocalDateTime.class);
        assertTransaction("createLostNotification", false,
                Long.class, Long.class, Long.class, LocalDateTime.class);
        assertTransaction("createStartedSellerNotification", false,
                Long.class, Long.class, LocalDateTime.class);
        assertTransaction("createSoldSellerNotification", false,
                Long.class, Long.class, BigDecimal.class, LocalDateTime.class);
        assertTransaction("createUnsoldSellerNotification", false,
                Long.class, Long.class, LocalDateTime.class);
        assertTransaction("createCancelledSellerNotification", false,
                Long.class, Long.class, LocalDateTime.class);
        assertTransaction("createCancelledParticipantNotification", false,
                Long.class, Long.class, LocalDateTime.class);
        assertTransaction("createRemovedSellerNotification", false,
                Long.class, Long.class, String.class, LocalDateTime.class);
        assertTransaction("createRemovedParticipantNotification", false,
                Long.class, Long.class, LocalDateTime.class);
        assertTransaction("createRestoredSellerNotification", false,
                Long.class, Long.class, LocalDateTime.class);
        assertTransaction("createRestoredParticipantNotification", false,
                Long.class, Long.class, LocalDateTime.class);
        assertTransaction("createOrderCreatedNotification", false,
                Long.class, Long.class, String.class, Long.class, LocalDateTime.class);
        assertTransaction("createOrderPaidNotification", false,
                Long.class, Long.class, Long.class, LocalDateTime.class);
        assertTransaction("createOrderAbandonedBuyerNotification", false,
                Long.class, Long.class, String.class, Long.class, LocalDateTime.class);
        assertTransaction("createOrderAbandonedSellerNotification", false,
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

    private void assertAuctionLifecycleNotification(
            Notification notification,
            Long memberId,
            String type,
            String title,
            String content,
            String targetUrl) {
        assertEquals(memberId, notification.getMemberId());
        assertEquals(type, notification.getNotificationType());
        assertEquals(title, notification.getTitle());
        assertEquals(content, notification.getContent());
        assertEquals("AUCTION", notification.getTargetType());
        assertEquals(AUCTION_ID, notification.getTargetId());
        assertEquals(targetUrl, notification.getTargetUrl());
        assertFalse(notification.getIsRead());
        assertTrue(notification.getIsSent());
        assertEquals(OCCURRED_AT, notification.getSentAt());
    }

    private void assertOrderNotification(
            Notification notification,
            Long memberId,
            String type,
            String title,
            String targetUrl,
            String contentFragment) {
        assertEquals(memberId, notification.getMemberId());
        assertEquals(type, notification.getNotificationType());
        assertEquals(title, notification.getTitle());
        assertTrue(notification.getContent().contains(contentFragment));
        assertEquals("AUCTION_ORDER", notification.getTargetType());
        assertEquals(ORDER_ID, notification.getTargetId());
        assertEquals(targetUrl, notification.getTargetUrl());
        assertFalse(notification.getIsRead());
        assertTrue(notification.getIsSent());
        assertEquals(OCCURRED_AT, notification.getSentAt());
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
