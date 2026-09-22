package com.project.sneakermarket.auction.notification;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.project.sneakermarket.auction.dto.BidCreateRequest;
import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionBid;
import com.project.sneakermarket.auction.entity.AuctionCondition;
import com.project.sneakermarket.auction.entity.AuctionProduct;
import com.project.sneakermarket.auction.entity.AuctionStatus;
import com.project.sneakermarket.auction.repository.AuctionBidRepository;
import com.project.sneakermarket.auction.repository.AuctionProductRepository;
import com.project.sneakermarket.auction.repository.AuctionRepository;
import com.project.sneakermarket.auction.repository.AuctionResultRepository;
import com.project.sneakermarket.auction.service.AuctionBidService;
import com.project.sneakermarket.auction.service.AuctionCommandService;
import com.project.sneakermarket.auction.service.AuctionLifecycleService;
import com.project.sneakermarket.auction.service.AuctionTime;
import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.member.repository.MemberRepository;
import com.project.sneakermarket.notification.entity.Notification;
import com.project.sneakermarket.notification.repository.NotificationRepository;

/**
 * 真實 MSSQL + Spring transaction proxy，驗證私人通知只在競標交易提交後落地並傳送。
 * Repository、listener 與 notification integration service 都使用正式 Bean；只 mock 時間與 broker。
 */
@SpringBootTest(properties = {
        "auction.lifecycle.enabled=false", "auction.order.enabled=false",
        "auction.email.enabled=false", "spring.jpa.hibernate.ddl-auto=update"
})
class AuctionPrivateNotificationIntegrationTests {

    private static final String PRIVATE_DESTINATION = "/queue/auction-events";
    private static final LocalDateTime CLOSE_AT = LocalDateTime.of(2099, 9, 16, 12, 0);
    private static final LocalDateTime BID_AT = CLOSE_AT.minusMinutes(10);

    @Autowired private AuctionBidService bidding;
    @Autowired private AuctionCommandService commands;
    @Autowired private AuctionLifecycleService lifecycle;
    @Autowired private AuctionRepository auctions;
    @Autowired private AuctionProductRepository products;
    @Autowired private AuctionBidRepository bids;
    @Autowired private AuctionResultRepository results;
    @Autowired private NotificationRepository notifications;
    @Autowired private MemberRepository members;
    @Autowired private ApplicationEventPublisher events;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private AuctionTime time;
    @MockitoBean private SimpMessagingTemplate messaging;

    private final List<Long> auctionIds = new ArrayList<>();
    private final List<Long> productIds = new ArrayList<>();
    private final List<Long> memberIds = new ArrayList<>();
    private Long sellerId;
    private Long bidderAId;
    private Long bidderBId;
    private Long bidderCId;

    @BeforeEach
    void setup() {
        when(time.now()).thenReturn(BID_AT);
        sellerId = member("Private notification seller");
        bidderAId = member("Bidder A");
        bidderBId = member("Bidder B");
        bidderCId = member("Bidder C");
    }

    @AfterEach
    void cleanup() {
        for (Long auctionId : auctionIds) {
            deleteFixtureNotifications(auctionId);
            results.findByAuctionId(auctionId).ifPresent(results::delete);
            bids.deleteAllInBatch(bids.findByAuctionIdOrderByCreatedAtDesc(auctionId));
            if (auctions.existsById(auctionId)) auctions.deleteById(auctionId);
        }
        for (Long productId : productIds) {
            if (products.existsById(productId)) products.deleteById(productId);
        }
        for (Long memberId : memberIds) {
            if (members.existsById(memberId)) members.deleteById(memberId);
        }
    }

    @Test
    void firstBidCreatesNoOutbidNotificationOrPrivateMessage() {
        Long auctionId = fixture();

        bidding.placeBid(auctionId, bidderAId, bid("5100"));

        assertTrue(notificationRows(auctionId).isEmpty());
        verify(messaging, never()).convertAndSendToUser(
                anyString(), eq(PRIVATE_DESTINATION), any());
    }

    @Test
    void committedOutbidCreatesOnlyPreviousBidderNotificationAndPrivateMessage() {
        Long auctionId = fixture();
        bidding.placeBid(auctionId, bidderAId, bid("5100"));
        clearInvocations(messaging);

        bidding.placeBid(auctionId, bidderBId, bid("5200"));

        List<Notification> rows = notificationRows(auctionId, "AUCTION_OUTBID");
        assertEquals(1, rows.size());
        Notification row = rows.get(0);
        assertEquals(bidderAId, row.getMemberId());
        assertEquals("您的競標出價已被超越", row.getTitle());
        assertTrue(row.getContent().contains("競標 #" + auctionId));
        assertTrue(row.getContent().contains("NT$ 5,200"));
        assertNotificationContract(row, auctionId, BID_AT);

        AuctionPrivateRealtimeMessage message = capturePrivateMessage(bidderAId);
        assertEquals(AuctionPrivateRealtimeEventType.AUCTION_OUTBID, message.eventType());
        assertEquals(auctionId, message.auctionId());
        assertEquals(row.getNotificationId(), message.notificationId());
        assertEquals(BID_AT, message.occurredAt());
        verify(messaging, never()).convertAndSendToUser(
                eq(bidderBId.toString()), eq(PRIVATE_DESTINATION), any());
    }

    @Test
    void currentHighestBidderIncreasingOwnBidCreatesNoSelfOutbid() {
        Long auctionId = fixture();
        bidding.placeBid(auctionId, bidderAId, bid("5100"));
        clearInvocations(messaging);

        bidding.placeBid(auctionId, bidderAId, bid("5200"));

        assertTrue(notificationRows(auctionId).isEmpty());
        verify(messaging, never()).convertAndSendToUser(
                anyString(), eq(PRIVATE_DESTINATION), any());
    }

    @Test
    void rolledBackOutbidCreatesNeitherNotificationNorPrivateMessage() {
        Long auctionId = fixture();
        bidding.placeBid(auctionId, bidderAId, bid("5100"));
        clearInvocations(messaging);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            bidding.placeBid(auctionId, bidderBId, bid("5200"));
            status.setRollbackOnly();
        });

        assertTrue(notificationRows(auctionId).isEmpty());
        Auction persisted = auctions.findById(auctionId).orElseThrow();
        assertEquals(bidderAId, persisted.getHighestBidderId());
        assertEquals(0, new BigDecimal("5100").compareTo(persisted.getCurrentPrice()));
        assertEquals(1, persisted.getBidCount());
        assertEquals(1, bids.findByAuctionIdOrderByCreatedAtDesc(auctionId).size());
        verify(messaging, never()).convertAndSendToUser(
                anyString(), eq(PRIVATE_DESTINATION), any());
    }

    @Test
    void startedWaitsForCommitThenNotifiesOnlySeller() {
        Long auctionId = fixture(
                AuctionStatus.SCHEDULED,
                BID_AT,
                CLOSE_AT
        );
        clearInvocations(messaging);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            lifecycle.processAuction(auctionId);

            assertTrue(notificationRows(auctionId).isEmpty());
            verify(messaging, never()).convertAndSendToUser(
                    anyString(), eq(PRIVATE_DESTINATION), any());
        });

        assertEquals(AuctionStatus.ACTIVE, auctions.findById(auctionId).orElseThrow().getStatus());
        List<Notification> rows = notificationRows(auctionId, "AUCTION_STARTED");
        assertEquals(1, rows.size());
        Notification seller = rows.get(0);
        assertEquals(sellerId, seller.getMemberId());
        assertNotificationContract(seller, auctionId, "/auctions/me/listings", BID_AT);
        assertPrivateEvent(
                sellerId,
                AuctionPrivateRealtimeEventType.AUCTION_STARTED,
                auctionId,
                seller.getNotificationId(),
                BID_AT
        );
        verify(messaging, never()).convertAndSendToUser(
                eq(bidderAId.toString()), eq(PRIVATE_DESTINATION), any());
    }

    @Test
    void unsoldNotifiesOnlySeller() {
        Long auctionId = fixture();
        when(time.now()).thenReturn(CLOSE_AT);

        lifecycle.processAuction(auctionId);

        assertEquals(AuctionStatus.UNSOLD, auctions.findById(auctionId).orElseThrow().getStatus());
        List<Notification> rows = notificationRows(auctionId, "AUCTION_UNSOLD");
        assertEquals(1, rows.size());
        Notification seller = rows.get(0);
        assertEquals(sellerId, seller.getMemberId());
        assertNotificationContract(seller, auctionId, "/auctions/me/listings", CLOSE_AT);
        assertPrivateEvent(
                sellerId,
                AuctionPrivateRealtimeEventType.AUCTION_UNSOLD,
                auctionId,
                seller.getNotificationId()
        );
        verify(messaging, never()).convertAndSendToUser(
                eq(bidderAId.toString()), eq(PRIVATE_DESTINATION), any());
    }

    @Test
    void cancelledUsesDistinctSnapshotForSellerAndParticipants() {
        Long auctionId = fixture();
        directBid(auctionId, bidderAId, "5100", BID_AT.minusMinutes(4));
        directBid(auctionId, bidderAId, "5200", BID_AT.minusMinutes(3));
        directBid(auctionId, bidderBId, "5300", BID_AT.minusMinutes(2));
        directBid(auctionId, sellerId, "5400", BID_AT.minusMinutes(1));
        clearInvocations(messaging);

        commands.cancelAuction(auctionId, sellerId);

        assertEquals(AuctionStatus.CANCELLED, auctions.findById(auctionId).orElseThrow().getStatus());
        List<Notification> rows = notificationRows(auctionId, "AUCTION_CANCELLED");
        assertEquals(3, rows.size());
        Notification seller = notificationRows(
                auctionId, sellerId, "AUCTION_CANCELLED").get(0);
        Notification participantA = notificationRows(
                auctionId, bidderAId, "AUCTION_CANCELLED").get(0);
        Notification participantB = notificationRows(
                auctionId, bidderBId, "AUCTION_CANCELLED").get(0);
        assertNotificationContract(seller, auctionId, "/auctions/me/listings", BID_AT);
        assertNotificationContract(participantA, auctionId, "/auctions/me/bids", BID_AT);
        assertNotificationContract(participantB, auctionId, "/auctions/me/bids", BID_AT);
        assertTrue(notificationRows(auctionId, bidderCId, "AUCTION_CANCELLED").isEmpty());
        assertPrivateEvent(
                sellerId,
                AuctionPrivateRealtimeEventType.AUCTION_CANCELLED,
                auctionId,
                seller.getNotificationId(),
                BID_AT
        );
        assertPrivateEvent(
                bidderAId,
                AuctionPrivateRealtimeEventType.AUCTION_CANCELLED,
                auctionId,
                participantA.getNotificationId(),
                BID_AT
        );
        assertPrivateEvent(
                bidderBId,
                AuctionPrivateRealtimeEventType.AUCTION_CANCELLED,
                auctionId,
                participantB.getNotificationId(),
                BID_AT
        );
        verify(messaging, never()).convertAndSendToUser(
                eq(bidderCId.toString()), eq(PRIVATE_DESTINATION), any());
    }

    @Test
    void soldRollbackCreatesNeitherResultNotificationNorPrivateMessage() {
        Long auctionId = fixture();
        directBid(auctionId, bidderAId, "5100", CLOSE_AT.minusMinutes(1));
        Auction auction = auctions.findById(auctionId).orElseThrow();
        auction.setCurrentPrice(new BigDecimal("5100"));
        auction.setHighestBidderId(bidderAId);
        auction.setBidCount(1);
        auctions.saveAndFlush(auction);
        when(time.now()).thenReturn(CLOSE_AT);
        clearInvocations(messaging);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            lifecycle.processAuction(auctionId);

            assertTrue(notificationRows(auctionId).isEmpty());
            verify(messaging, never()).convertAndSendToUser(
                    anyString(), eq(PRIVATE_DESTINATION), any());
            status.setRollbackOnly();
        });

        assertEquals(AuctionStatus.ACTIVE, auctions.findById(auctionId).orElseThrow().getStatus());
        assertTrue(results.findByAuctionId(auctionId).isEmpty());
        assertTrue(notificationRows(auctionId).isEmpty());
        verify(messaging, never()).convertAndSendToUser(
                anyString(), eq(PRIVATE_DESTINATION), any());
    }

    @Test
    void cancelledRollbackCreatesNeitherNotificationNorPrivateMessage() {
        Long auctionId = fixture();
        clearInvocations(messaging);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            commands.cancelAuction(auctionId, sellerId);

            assertTrue(notificationRows(auctionId).isEmpty());
            verify(messaging, never()).convertAndSendToUser(
                    anyString(), eq(PRIVATE_DESTINATION), any());
            status.setRollbackOnly();
        });

        assertEquals(AuctionStatus.ACTIVE, auctions.findById(auctionId).orElseThrow().getStatus());
        assertTrue(notificationRows(auctionId).isEmpty());
        verify(messaging, never()).convertAndSendToUser(
                anyString(), eq(PRIVATE_DESTINATION), any());
    }

    @Test
    void removedAtDeadlineDoesNotPublishPrivateRemovedEvent() {
        Long auctionId = fixture();
        Auction auction = auctions.findById(auctionId).orElseThrow();
        auction.setRemovedAt(BID_AT);
        auction.setRemovedByMemberId(sellerId);
        auction.setRemoveReason("integration removed fixture");
        auction.setRemovedFromStatus(AuctionStatus.ACTIVE);
        auctions.saveAndFlush(auction);
        when(time.now()).thenReturn(CLOSE_AT);
        clearInvocations(messaging);

        lifecycle.processAuction(auctionId);

        assertEquals(AuctionStatus.REMOVED, auctions.findById(auctionId).orElseThrow().getStatus());
        assertTrue(results.findByAuctionId(auctionId).isPresent());
        assertTrue(notificationRows(auctionId).isEmpty());
        verify(messaging, never()).convertAndSendToUser(
                anyString(), eq(PRIVATE_DESTINATION), any());
    }

    @Test
    void soldCreatesWinnerAndDistinctLoserNotificationsAndReplayIsIdempotent() {
        Long auctionId = fixture();
        directBid(auctionId, bidderAId, "5100", CLOSE_AT.minusMinutes(4));
        directBid(auctionId, bidderAId, "5200", CLOSE_AT.minusMinutes(3));
        directBid(auctionId, bidderBId, "5300", CLOSE_AT.minusMinutes(2));
        directBid(auctionId, bidderCId, "6000", CLOSE_AT.minusMinutes(1));
        Auction auction = auctions.findById(auctionId).orElseThrow();
        auction.setCurrentPrice(new BigDecimal("6000"));
        auction.setHighestBidderId(bidderCId);
        auction.setBidCount(4);
        auction.setUpdatedAt(CLOSE_AT.minusMinutes(1));
        auctions.saveAndFlush(auction);
        when(time.now()).thenReturn(CLOSE_AT);

        lifecycle.processAuction(auctionId);

        assertEquals(AuctionStatus.SOLD, auctions.findById(auctionId).orElseThrow().getStatus());
        assertEquals(1, notificationRows(auctionId, sellerId, "AUCTION_SOLD").size());
        assertTrue(notificationRows(auctionId, sellerId, "AUCTION_WON").isEmpty());
        assertTrue(notificationRows(auctionId, sellerId, "AUCTION_LOST").isEmpty());
        assertEquals(1, notificationRows(auctionId, bidderCId, "AUCTION_WON").size());
        assertTrue(notificationRows(auctionId, bidderCId, "AUCTION_LOST").isEmpty());
        assertEquals(1, notificationRows(auctionId, bidderAId, "AUCTION_LOST").size());
        assertEquals(1, notificationRows(auctionId, bidderBId, "AUCTION_LOST").size());
        assertEquals(4, notificationRows(auctionId).size());

        Notification seller = notificationRows(auctionId, sellerId, "AUCTION_SOLD").get(0);
        assertEquals("您的競標已結標", seller.getTitle());
        assertTrue(seller.getContent().contains("NT$ 6,000"));
        assertNotificationContract(seller, auctionId, "/auctions/me/listings", CLOSE_AT);
        Notification winner = notificationRows(auctionId, bidderCId, "AUCTION_WON").get(0);
        assertEquals("恭喜您競標得標", winner.getTitle());
        assertTrue(winner.getContent().contains("NT$ 6,000"));
        assertNotificationContract(winner, auctionId, CLOSE_AT);
        Notification loserA = notificationRows(auctionId, bidderAId, "AUCTION_LOST").get(0);
        assertEquals("本次競標未得標", loserA.getTitle());
        assertTrue(loserA.getContent().contains("本次由其他會員得標"));
        assertNotificationContract(loserA, auctionId, CLOSE_AT);

        assertPrivateEvent(sellerId, AuctionPrivateRealtimeEventType.AUCTION_SOLD, auctionId,
                seller.getNotificationId());
        assertPrivateEvent(bidderCId, AuctionPrivateRealtimeEventType.AUCTION_WON, auctionId,
                winner.getNotificationId());
        assertPrivateEvent(bidderAId, AuctionPrivateRealtimeEventType.AUCTION_LOST, auctionId,
                loserA.getNotificationId());
        Notification loserB = notificationRows(auctionId, bidderBId, "AUCTION_LOST").get(0);
        assertPrivateEvent(bidderBId, AuctionPrivateRealtimeEventType.AUCTION_LOST, auctionId,
                loserB.getNotificationId());

        clearInvocations(messaging);
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                events.publishEvent(new AuctionPrivateNotificationReadyEvent(
                        AuctionPrivateNotificationKind.SOLD,
                        auctionId,
                        bidderCId,
                        new BigDecimal("6000"),
                        CLOSE_AT
                )));

        assertEquals(1, notificationRows(auctionId, sellerId, "AUCTION_SOLD").size());
        assertEquals(1, notificationRows(auctionId, bidderCId, "AUCTION_WON").size());
        assertEquals(1, notificationRows(auctionId, bidderAId, "AUCTION_LOST").size());
        assertEquals(1, notificationRows(auctionId, bidderBId, "AUCTION_LOST").size());
        assertEquals(4, notificationRows(auctionId).size());
        verify(messaging, never()).convertAndSendToUser(
                anyString(), eq(PRIVATE_DESTINATION), any());
    }

    @Test
    void webSocketFailureKeepsCommittedNotificationAndAuctionBid() {
        Long auctionId = fixture();
        bidding.placeBid(auctionId, bidderAId, bid("5100"));
        clearInvocations(messaging);
        doThrow(new IllegalStateException("broker unavailable"))
                .when(messaging).convertAndSendToUser(
                        anyString(), eq(PRIVATE_DESTINATION), any());

        assertDoesNotThrow(() -> bidding.placeBid(auctionId, bidderBId, bid("5200")));

        assertEquals(1, notificationRows(auctionId, bidderAId, "AUCTION_OUTBID").size());
        Auction persisted = auctions.findById(auctionId).orElseThrow();
        assertEquals(bidderBId, persisted.getHighestBidderId());
        assertEquals(0, new BigDecimal("5200").compareTo(persisted.getCurrentPrice()));
        assertEquals(2, persisted.getBidCount());
        assertEquals(2, bids.findByAuctionIdOrderByCreatedAtDesc(auctionId).size());
        verify(messaging).convertAndSendToUser(
                eq(bidderAId.toString()), eq(PRIVATE_DESTINATION), any());
    }

    private BidCreateRequest bid(String amount) {
        return new BidCreateRequest(new BigDecimal(amount));
    }

    private Long member(String name) {
        Member member = new Member();
        String token = UUID.randomUUID().toString();
        member.setAccount("pn-" + token);
        member.setEmail(token + "@example.invalid");
        member.setPasswordHash("integration-fixture-not-a-login");
        member.setName(name);
        member.setPhone("0900000016");
        member.setAddress("台北市測試地址");
        Long memberId = members.saveAndFlush(member).getMemberId();
        memberIds.add(memberId);
        return memberId;
    }

    private Long fixture() {
        return fixture(
                AuctionStatus.ACTIVE,
                CLOSE_AT.minusHours(1),
                CLOSE_AT
        );
    }

    private Long fixture(
            AuctionStatus status,
            LocalDateTime startAt,
            LocalDateTime endAt) {
        AuctionProduct product = new AuctionProduct();
        product.setSellerId(sellerId);
        product.setBrandId(1L);
        product.setCategoryId(1L);
        product.setProductName("Private notification integration fixture");
        product.setSize("US 9");
        product.setCondition(AuctionCondition.NEW);
        product.setDescription("Owned by auction private notification integration tests");
        product.setShippingMethod("Delivery");
        product.setShippingFee(BigDecimal.ZERO);
        product.setPaymentMethod("Transfer");
        product.setAllowMeetup(false);
        product.setAllowReturn(false);
        product.setCreatedAt(CLOSE_AT.minusHours(2));
        product.setUpdatedAt(CLOSE_AT.minusHours(2));
        product = products.saveAndFlush(product);
        productIds.add(product.getAuctionProductId());

        Auction auction = new Auction();
        auction.setAuctionProductId(product.getAuctionProductId());
        auction.setStartPrice(new BigDecimal("5000"));
        auction.setCurrentPrice(new BigDecimal("5000"));
        auction.setMinIncrement(new BigDecimal("100"));
        auction.setStartAt(startAt);
        auction.setEndAt(endAt);
        auction.setStatus(status);
        auction.setBidCount(0);
        auction.setCreatedAt(CLOSE_AT.minusHours(2));
        auction.setUpdatedAt(CLOSE_AT.minusHours(2));
        Long auctionId = auctions.saveAndFlush(auction).getAuctionId();
        auctionIds.add(auctionId);
        return auctionId;
    }

    private void directBid(Long auctionId, Long bidderId, String amount, LocalDateTime createdAt) {
        AuctionBid bid = new AuctionBid();
        bid.setAuctionId(auctionId);
        bid.setBidderId(bidderId);
        bid.setBidAmount(new BigDecimal(amount));
        bid.setCreatedAt(createdAt);
        bids.saveAndFlush(bid);
    }

    private List<Notification> notificationRows(Long auctionId) {
        return notifications.findAll().stream()
                .filter(row -> "AUCTION".equals(row.getTargetType()))
                .filter(row -> auctionId.equals(row.getTargetId()))
                .toList();
    }

    private List<Notification> notificationRows(Long auctionId, String type) {
        return notificationRows(auctionId).stream()
                .filter(row -> type.equals(row.getNotificationType()))
                .toList();
    }

    private List<Notification> notificationRows(Long auctionId, Long memberId, String type) {
        return notificationRows(auctionId, type).stream()
                .filter(row -> memberId.equals(row.getMemberId()))
                .toList();
    }

    private void assertNotificationContract(
            Notification notification,
            Long auctionId,
            LocalDateTime sentAt) {
        assertNotificationContract(
                notification,
                auctionId,
                "/auctions/" + auctionId,
                sentAt
        );
    }

    private void assertNotificationContract(
            Notification notification,
            Long auctionId,
            String targetUrl,
            LocalDateTime sentAt) {
        assertEquals("AUCTION", notification.getTargetType());
        assertEquals(auctionId, notification.getTargetId());
        assertEquals(targetUrl, notification.getTargetUrl());
        assertFalse(notification.getIsRead());
        assertTrue(notification.getIsSent());
        assertEquals(sentAt, notification.getSentAt());
    }

    private AuctionPrivateRealtimeMessage capturePrivateMessage(Long memberId) {
        ArgumentCaptor<AuctionPrivateRealtimeMessage> message =
                ArgumentCaptor.forClass(AuctionPrivateRealtimeMessage.class);
        verify(messaging).convertAndSendToUser(
                eq(memberId.toString()), eq(PRIVATE_DESTINATION), message.capture());
        return message.getValue();
    }

    private void assertPrivateEvent(
            Long memberId,
            AuctionPrivateRealtimeEventType eventType,
            Long auctionId,
            Long notificationId) {
        assertPrivateEvent(memberId, eventType, auctionId, notificationId, CLOSE_AT);
    }

    private void assertPrivateEvent(
            Long memberId,
            AuctionPrivateRealtimeEventType eventType,
            Long auctionId,
            Long notificationId,
            LocalDateTime occurredAt) {
        AuctionPrivateRealtimeMessage message = capturePrivateMessage(memberId);
        assertEquals(eventType, message.eventType());
        assertEquals(auctionId, message.auctionId());
        assertEquals(notificationId, message.notificationId());
        assertEquals(occurredAt, message.occurredAt());
    }

    private void deleteFixtureNotifications(Long auctionId) {
        jdbc.update("""
                DELETE FROM dbo.notifications
                WHERE target_type = 'AUCTION' AND target_id = ?
                """, auctionId);
    }
}
