package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.project.sneakermarket.auction.entity.*;
import com.project.sneakermarket.auction.notification.AuctionPrivateRealtimeEventType;
import com.project.sneakermarket.auction.notification.AuctionPrivateRealtimeMessage;
import com.project.sneakermarket.auction.repository.*;
import com.project.sneakermarket.auction.scheduler.AuctionOrderScheduler;
import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.member.repository.MemberRepository;
import com.project.sneakermarket.notification.entity.Notification;
import com.project.sneakermarket.notification.repository.NotificationRepository;

/** 真實 MSSQL；fixture 先 commit，Service 經 Spring proxy 呼叫，僅控制時間與候選清單。 */
@SpringBootTest(properties = {
        "auction.lifecycle.enabled=false", "auction.order.enabled=false",
        "spring.jpa.hibernate.ddl-auto=update"
})
class AuctionOrderIntegrationTests {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 10, 12, 0);
    @Autowired private AuctionOrderService service;
    @Autowired private AuctionOrderPaymentDeadlineService deadlines;
    @Autowired private AuctionLifecycleService lifecycle;
    @Autowired private AuctionAdminMutationService adminMutations;
    @Autowired private AuctionOrderRepository orders;
    @Autowired private AuctionResultRepository results;
    @Autowired private AuctionRepository auctions;
    @Autowired private AuctionProductRepository products;
    @Autowired private AuctionBidRepository bids;
    @Autowired private MemberRepository members;
    @Autowired private NotificationRepository notifications;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean private AuctionTime time;
    @MockitoBean private SimpMessagingTemplate messaging;
    private final List<Long> auctionIds = new ArrayList<>();
    private final List<Long> productIds = new ArrayList<>();
    private final List<Long> memberIds = new ArrayList<>();
    private Long buyerId;
    private Long sellerId;
    private ExecutorService executor;
    private CountDownLatch release;

    @BeforeEach
    void setup() {
        doReturn(NOW).when(time).now();
        buyerId = member("得標者");
        sellerId = member("賣家");
    }

    @AfterEach
    void cleanup() throws InterruptedException {
        if (release != null) release.countDown();
        if (executor != null) {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
        // Notification 持有 member FK；必須先於 order/member fixture 清除。
        for (Long id : memberIds) {
            jdbc.update("DELETE FROM dbo.notifications WHERE member_id = ?", id);
        }
        for (Long id : auctionIds) {
            orders.findByAuctionId(id).ifPresent(orders::delete);
            results.findByAuctionId(id).ifPresent(results::delete);
            bids.deleteAllInBatch(bids.findByAuctionIdOrderByCreatedAtDesc(id));
            auctions.deleteById(id);
        }
        for (Long id : productIds) products.deleteById(id);
        for (Long id : memberIds) members.deleteById(id);
    }

    @Test
    void committedLifecycleResultCreatesOneCompleteOrderAndRepeatsAreIdempotent() {
        Long id = fixture(true);
        clearInvocations(messaging);
        var auctionBefore = auctionRow(id);
        var resultBefore = resultRow(id);
        service.createOrderForAuction(id);
        service.createOrderForAuction(id);
        AuctionOrder order = orders.findByAuctionId(id).orElseThrow();
        assertEquals(id, order.getAuctionId());
        assertEquals(buyerId, order.getBuyerId());
        assertEquals(sellerId, order.getSellerId());
        assertEquals(0, new BigDecimal("6000").compareTo(order.getFinalPrice()));
        assertEquals("Transfer", order.getPaymentMethod());
        assertEquals("得標者", order.getReceiverName());
        assertEquals("0912345678", order.getReceiverPhone());
        assertEquals("台北市測試路 1 號", order.getShippingAddress());
        assertEquals(AuctionPaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals(AuctionShippingStatus.NOT_SHIPPED, order.getShippingStatus());
        assertEquals(AuctionOrderStatus.PENDING_PAYMENT, order.getOrderStatus());
        assertEquals(NOW, order.getCreatedAt());
        assertEquals(NOW, order.getUpdatedAt());
        assertEquals(NOW.plusHours(24), order.getPaymentDueAt());
        assertNull(order.getAbandonedAt());
        assertNull(order.getPaidAt());
        assertNull(order.getShippedAt());
        assertNull(order.getCompletedAt());
        assertEquals(1, orderCount(id));
        assertEquals(auctionBefore, auctionRow(id));
        assertEquals(resultBefore, resultRow(id));

        List<Notification> created = orderNotifications(
                order.getAuctionOrderId(), "AUCTION_ORDER_CREATED");
        assertEquals(1, created.size());
        Notification notification = created.get(0);
        assertEquals(buyerId, notification.getMemberId());
        assertEquals("您的競標訂單已建立", notification.getTitle());
        assertEquals("您得標的競標已建立訂單，請於付款期限內完成付款。",
                notification.getContent());
        assertOrderNotificationContract(
                notification,
                order.getAuctionOrderId(),
                "/auction-orders/" + order.getOrderNumber() + "/payment",
                NOW);

        AuctionPrivateRealtimeMessage message = capturePrivateMessage(buyerId);
        assertEquals(AuctionPrivateRealtimeEventType.AUCTION_ORDER_CREATED, message.eventType());
        assertEquals(id, message.auctionId());
        assertEquals(notification.getNotificationId(), message.notificationId());
        assertEquals(NOW, message.occurredAt());
        verify(messaging, never()).convertAndSendToUser(
                eq(sellerId.toString()), eq("/queue/auction-events"), any());
    }

    @Test
    void overdueOrderBecomesAbandonedWithoutRewritingSoldAuctionOrResult() {
        Long id = fixture(true);
        service.createOrderForAuction(id);
        AuctionOrder order = orders.findByAuctionId(id).orElseThrow();
        var auctionBefore = auctionRow(id);
        var resultBefore = resultRow(id);
        LocalDateTime expiredAt = NOW.plusHours(24);
        doReturn(expiredAt).when(time).now();

        deadlines.expireOverdueOrder(order.getAuctionOrderId());
        deadlines.expireOverdueOrder(order.getAuctionOrderId());

        AuctionOrder persisted = orders.findByAuctionId(id).orElseThrow();
        assertEquals(AuctionPaymentStatus.PENDING, persisted.getPaymentStatus());
        assertEquals(AuctionOrderStatus.ABANDONED, persisted.getOrderStatus());
        assertEquals(AuctionShippingStatus.NOT_SHIPPED, persisted.getShippingStatus());
        assertEquals(NOW.plusHours(24), persisted.getPaymentDueAt());
        assertEquals(expiredAt, persisted.getAbandonedAt());
        assertEquals(expiredAt, persisted.getUpdatedAt());
        assertNull(persisted.getPaidAt());
        assertEquals(auctionBefore, auctionRow(id));
        assertEquals(resultBefore, resultRow(id));
        assertEquals(AuctionStatus.SOLD, auctions.findById(id).orElseThrow().getStatus());
        assertEquals(AuctionResultStatus.SOLD, results.findByAuctionId(id).orElseThrow().getResultStatus());
    }

    @Test
    void unsoldNeverCreatesOrder() {
        Long id = fixture(false);
        service.createOrderForAuction(id);
        assertEquals(0, orderCount(id));
    }

    @Test
    void removedAuctionAtEndCreatesRemovedResultAndNeverCreatesOrder() {
        Long id = openFixture(true);

        adminMutations.removeAuction(id, sellerId, "到期前平台下架");
        lifecycle.processAuction(id);
        new AuctionOrderScheduler(results, service).createAuctionOrders();

        Auction auction = auctions.findById(id).orElseThrow();
        AuctionResult result = results.findByAuctionId(id).orElseThrow();
        assertEquals(AuctionStatus.REMOVED, auction.getStatus());
        assertNotNull(auction.getRemovedAt());
        assertEquals(1, auction.getBidCount());
        assertEquals(buyerId, auction.getHighestBidderId());
        assertEquals(0, new BigDecimal("6000").compareTo(auction.getCurrentPrice()));
        assertEquals(AuctionResultStatus.REMOVED, result.getResultStatus());
        assertNull(result.getWinnerId());
        assertNull(result.getFinalPrice());
        assertFalse(result.getReason().isBlank());
        assertFalse(results.findOrderCandidateIds().contains(id));
        assertEquals(0, orderCount(id));
    }

    @Test
    void soldBeforeAdminRemoveStillCreatesOrder() {
        Long id = fixture(true);

        adminMutations.removeAuction(id, sellerId, "成交後隱藏");
        new AuctionOrderScheduler(results, service).createAuctionOrders();

        Auction auction = auctions.findById(id).orElseThrow();
        AuctionResult result = results.findByAuctionId(id).orElseThrow();
        assertEquals(AuctionStatus.SOLD, auction.getStatus());
        assertNotNull(auction.getRemovedAt());
        assertEquals(AuctionResultStatus.SOLD, result.getResultStatus());
        assertEquals(buyerId, result.getWinnerId());
        assertEquals(0, new BigDecimal("6000").compareTo(result.getFinalPrice()));
        assertEquals(1, orderCount(id));
    }

    @Test
    void winnerMismatchLeavesAuctionAndResultUnchanged() {
        Long id = fixture(true);
        jdbc.update("UPDATE dbo.auctions SET highest_bidder_id = ? WHERE auction_id = ?", sellerId, id);
        rejectWithoutChanges(id);
    }

    @Test
    void priceMismatchLeavesAuctionAndResultUnchanged() {
        Long id = fixture(true);
        jdbc.update("UPDATE dbo.auctions SET current_price = 6500 WHERE auction_id = ?", id);
        rejectWithoutChanges(id);
    }

    @Test
    void missingSellerLeavesResultRetryable() {
        Long id = fixture(true);
        jdbc.update("UPDATE dbo.auction_products SET seller_id = -1 WHERE auction_product_id = ?",
                auctions.findById(id).orElseThrow().getAuctionProductId());
        rejectWithoutChanges(id);
        assertTrue(results.findOrderCandidateIds().contains(id));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void missingAddressRollsBackAndCanRetryAfterMemberCorrection(String address) {
        Long id = fixture(true);
        clearInvocations(messaging);
        changeBuyer(b -> b.setAddress(address));
        rejectWithoutChanges(id);
        assertTrue(memberNotifications(buyerId, "AUCTION_ORDER_CREATED").isEmpty());
        verify(messaging, never()).convertAndSendToUser(
                anyString(), eq("/queue/auction-events"), any());
        assertEquals(AuctionStatus.SOLD, auctions.findById(id).orElseThrow().getStatus());
        assertTrue(results.findOrderCandidateIds().contains(id));
        changeBuyer(b -> b.setAddress("補齊地址"));
        service.createOrderForAuction(id);
        AuctionOrder order = orders.findByAuctionId(id).orElseThrow();
        assertEquals("補齊地址", order.getShippingAddress());
        assertFalse(results.findOrderCandidateIds().contains(id));
        assertEquals(1, orderNotifications(
                order.getAuctionOrderId(), "AUCTION_ORDER_CREATED").size());
        assertEquals(AuctionPrivateRealtimeEventType.AUCTION_ORDER_CREATED,
                capturePrivateMessage(buyerId).eventType());
    }

    @Test
    void memberChangesNeverRewriteExistingSnapshot() {
        Long id = fixture(true);
        service.createOrderForAuction(id);
        changeBuyer(b -> {
            b.setName("更名");
            b.setPhone("0999999999");
            b.setAddress("新地址");
        });
        service.createOrderForAuction(id);
        var order = orders.findByAuctionId(id).orElseThrow();
        assertEquals("得標者", order.getReceiverName());
        assertEquals("0912345678", order.getReceiverPhone());
        assertEquals("台北市測試路 1 號", order.getShippingAddress());
    }

    @Test
    void candidateQueryProjectsOnlySoldWithoutOrder() {
        Long pending = fixture(true);
        Long existing = fixture(true);
        Long unsold = fixture(false);
        service.createOrderForAuction(existing);
        var candidates = results.findOrderCandidateIds();
        assertTrue(candidates.contains(pending));
        assertFalse(candidates.contains(existing));
        assertFalse(candidates.contains(unsold));
    }

    @Test
    void schedulerContinuesAfterRealTransactionFailure() {
        Long bad = fixture(true);
        Long good = fixture(true);
        Long alternateBuyer = member("第二位得標者");
        jdbc.update("UPDATE dbo.auctions SET highest_bidder_id = ? WHERE auction_id = ?", alternateBuyer, good);
        jdbc.update("UPDATE dbo.auction_results SET winner_id = ? WHERE auction_id = ?", alternateBuyer, good);
        changeBuyer(b -> b.setAddress(null));
        var before = resultRow(bad);
        var candidates = mock(AuctionResultRepository.class);
        when(candidates.findOrderCandidateIds()).thenReturn(List.of(bad, good));
        new AuctionOrderScheduler(candidates, service).createAuctionOrders();
        assertEquals(0, orderCount(bad));
        assertEquals(before, resultRow(bad));
        assertEquals(1, orderCount(good));
    }

    @Test
    void twoCreatorsWaitForResultLockAndCommitExactlyOneOrder() throws Exception {
        Long id = fixture(true);
        var locked = new CountDownLatch(1);
        release = new CountDownLatch(1);
        var first = new AtomicBoolean(true);
        when(time.now()).thenAnswer(call -> {
            if (first.compareAndSet(true, false)) {
                locked.countDown();
                assertTrue(release.await(10, TimeUnit.SECONDS));
            }
            return NOW;
        });
        executor = Executors.newFixedThreadPool(2);
        Future<?> a = executor.submit(() -> service.createOrderForAuction(id));
        assertTrue(locked.await(10, TimeUnit.SECONDS));
        var started = new CountDownLatch(1);
        Future<?> b = executor.submit(() -> {
            started.countDown();
            service.createOrderForAuction(id);
        });
        assertTrue(started.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> b.get(200, TimeUnit.MILLISECONDS));
        release.countDown();
        a.get(20, TimeUnit.SECONDS);
        b.get(20, TimeUnit.SECONDS);
        assertEquals(1, orderCount(id));
    }

    @Test
    void foreignKeyInsertFailureRollsBackOnlyOrderTransactionAndRemainsRetryable() {
        Long id = fixture(true);
        var auctionBefore = auctionRow(id);
        var resultBefore = resultRow(id);
        // Service 已驗證 seller 存在後，在同一訂單交易刪除自己的 fixture seller，
        // 讓真實 INSERT 違反 FK；連同此刪除都必須 rollback。
        when(time.now()).thenAnswer(call -> {
            assertEquals(1, jdbc.update("DELETE FROM dbo.members WHERE member_id = ?", sellerId));
            return NOW;
        });
        assertThrows(DataIntegrityViolationException.class, () -> service.createOrderForAuction(id));
        assertEquals(0, orderCount(id));
        assertTrue(members.existsById(sellerId));
        assertEquals(auctionBefore, auctionRow(id));
        assertEquals(resultBefore, resultRow(id));
        assertTrue(results.findOrderCandidateIds().contains(id));
        doReturn(NOW).when(time).now();
        service.createOrderForAuction(id);
        assertEquals(1, orderCount(id));
    }

    @Test
    void orderCommitIsIndependentOfCallingTransactionRollback() {
        Long id = fixture(true);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            service.createOrderForAuction(id);
            status.setRollbackOnly();
        });
        assertEquals(1, orderCount(id));
    }

    @Test
    void databaseRejectsDuplicateAuctionAndOrderNumber() {
        Long a = fixture(true);
        Long b = fixture(true);
        service.createOrderForAuction(a);
        service.createOrderForAuction(b);
        var first = orders.findByAuctionId(a).orElseThrow();
        var second = orders.findByAuctionId(b).orElseThrow();
        assertNotEquals(first.getOrderNumber(), second.getOrderNumber());
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE dbo.auction_orders SET auction_id = ? WHERE auction_id = ?", a, b));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE dbo.auction_orders SET order_number = ? WHERE auction_id = ?", first.getOrderNumber(), b));
        assertEquals(1, orderCount(a));
        assertEquals(1, orderCount(b));
    }

    private void rejectWithoutChanges(Long id) {
        var auctionBefore = auctionRow(id);
        var resultBefore = resultRow(id);
        assertThrows(IllegalStateException.class, () -> service.createOrderForAuction(id));
        assertEquals(0, orderCount(id));
        assertEquals(auctionBefore, auctionRow(id));
        assertEquals(resultBefore, resultRow(id));
    }

    private Map<String, Object> auctionRow(Long id) {
        return jdbc.queryForMap("SELECT * FROM dbo.auctions WHERE auction_id = ?", id);
    }

    private Map<String, Object> resultRow(Long id) {
        return jdbc.queryForMap("SELECT * FROM dbo.auction_results WHERE auction_id = ?", id);
    }

    private int orderCount(Long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM dbo.auction_orders WHERE auction_id = ?", Integer.class, id);
    }

    private List<Notification> orderNotifications(Long orderId, String type) {
        return notifications.findAll().stream()
                .filter(row -> "AUCTION_ORDER".equals(row.getTargetType()))
                .filter(row -> orderId.equals(row.getTargetId()))
                .filter(row -> type.equals(row.getNotificationType()))
                .toList();
    }

    private List<Notification> memberNotifications(Long memberId, String type) {
        return notifications.findAll().stream()
                .filter(row -> memberId.equals(row.getMemberId()))
                .filter(row -> type.equals(row.getNotificationType()))
                .toList();
    }

    private void assertOrderNotificationContract(
            Notification notification,
            Long orderId,
            String targetUrl,
            LocalDateTime sentAt) {
        assertEquals("AUCTION_ORDER", notification.getTargetType());
        assertEquals(orderId, notification.getTargetId());
        assertEquals(targetUrl, notification.getTargetUrl());
        assertFalse(notification.getIsRead());
        assertTrue(notification.getIsSent());
        assertEquals(sentAt, notification.getSentAt());
    }

    private AuctionPrivateRealtimeMessage capturePrivateMessage(Long memberId) {
        ArgumentCaptor<AuctionPrivateRealtimeMessage> message =
                ArgumentCaptor.forClass(AuctionPrivateRealtimeMessage.class);
        verify(messaging).convertAndSendToUser(
                eq(memberId.toString()), eq("/queue/auction-events"), message.capture());
        return message.getValue();
    }

    private void changeBuyer(java.util.function.Consumer<Member> change) {
        Member buyer = members.findById(buyerId).orElseThrow();
        change.accept(buyer);
        members.saveAndFlush(buyer);
    }

    private Long member(String name) {
        String token = UUID.randomUUID().toString();
        Member member = new Member();
        member.setAccount("ao-" + token);
        member.setEmail(token + "@example.invalid");
        member.setPasswordHash("integration-fixture-not-a-login");
        member.setName(name);
        member.setPhone("0912345678");
        member.setAddress("台北市測試路 1 號");
        Long id = members.saveAndFlush(member).getMemberId();
        memberIds.add(id);
        return id;
    }

    private Long openFixture(boolean sold) {
        AuctionProduct product = new AuctionProduct();
        product.setSellerId(sellerId);
        product.setBrandId(1L);
        product.setCategoryId(1L);
        product.setProductName("Order integration fixture");
        product.setSize("US 9");
        product.setCondition(AuctionCondition.NEW);
        product.setDescription("Owned by order integration test");
        product.setShippingMethod("Delivery");
        product.setShippingFee(BigDecimal.ZERO);
        product.setPaymentMethod("Transfer");
        product.setAllowMeetup(false);
        product.setAllowReturn(false);
        product.setCreatedAt(NOW.minusHours(2));
        product.setUpdatedAt(NOW.minusHours(2));
        product = products.saveAndFlush(product);
        productIds.add(product.getAuctionProductId());

        Auction auction = new Auction();
        auction.setAuctionProductId(product.getAuctionProductId());
        auction.setStartPrice(new BigDecimal("5000"));
        auction.setCurrentPrice(new BigDecimal(sold ? "6000" : "5000"));
        auction.setMinIncrement(new BigDecimal("500"));
        auction.setStartAt(NOW.minusHours(1));
        auction.setEndAt(NOW);
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setBidCount(sold ? 1 : 0);
        auction.setHighestBidderId(sold ? buyerId : null);
        auction.setCreatedAt(NOW.minusHours(2));
        auction.setUpdatedAt(NOW.minusHours(2));
        Long id = auctions.saveAndFlush(auction).getAuctionId();
        auctionIds.add(id);
        if (sold) {
            AuctionBid bid = new AuctionBid();
            bid.setAuctionId(id);
            bid.setBidderId(buyerId);
            bid.setBidAmount(new BigDecimal("6000"));
            bid.setCreatedAt(NOW.minusMinutes(1));
            bids.saveAndFlush(bid);
        }
        return id;
    }

    private Long fixture(boolean sold) {
        Long id = openFixture(sold);
        lifecycle.processAuction(id); // 正式功能 7 先獨立 COMMIT，功能 8 才能消費。
        assertEquals(0, orderCount(id));
        return id;
    }
}
