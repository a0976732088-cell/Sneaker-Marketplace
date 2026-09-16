package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.project.sneakermarket.auction.dto.BidCreateRequest;
import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionCondition;
import com.project.sneakermarket.auction.entity.AuctionProduct;
import com.project.sneakermarket.auction.entity.AuctionResult;
import com.project.sneakermarket.auction.entity.AuctionResultStatus;
import com.project.sneakermarket.auction.entity.AuctionStatus;
import com.project.sneakermarket.auction.exception.AuctionConflictException;
import com.project.sneakermarket.auction.repository.AuctionBidRepository;
import com.project.sneakermarket.auction.repository.AuctionProductRepository;
import com.project.sneakermarket.auction.repository.AuctionRepository;
import com.project.sneakermarket.auction.repository.AuctionResultRepository;
import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.member.repository.MemberRepository;

/** 真實 MSSQL：驗證 Admin、Bid、Lifecycle 競爭同一筆 PESSIMISTIC_WRITE row lock。 */
@SpringBootTest(properties = {
        "auction.lifecycle.enabled=false", "auction.order.enabled=false",
        "auction.email.enabled=false", "spring.jpa.hibernate.ddl-auto=update"
})
class AuctionAdminConcurrencyTests {
    private static final LocalDateTime END = LocalDateTime.of(2026, 9, 12, 12, 0);
    private static final Long ADMIN_ID = 9_300_099L;

    @Autowired private AuctionAdminMutationService adminMutations;
    @Autowired private AuctionBidService bidding;
    @Autowired private AuctionLifecycleService lifecycle;
    @Autowired private AuctionRepository auctions;
    @Autowired private AuctionProductRepository products;
    @Autowired private AuctionBidRepository bids;
    @Autowired private AuctionResultRepository results;
    @Autowired private MemberRepository members;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private AuctionTime time;

    private Long auctionId;
    private Long productId;
    private Long buyerId;
    private ExecutorService executor;
    private CountDownLatch release;

    @BeforeEach
    void setup() {
        when(time.now()).thenReturn(END);
        Member buyer = new Member();
        String token = UUID.randomUUID().toString();
        buyer.setAccount("admcon-" + token);
        buyer.setEmail(token + "@example.invalid");
        buyer.setPasswordHash("integration-fixture-not-a-login");
        buyer.setName("Admin concurrency buyer");
        buyer.setPhone("0900000011");
        buyerId = members.saveAndFlush(buyer).getMemberId();
    }

    @AfterEach
    void cleanup() throws InterruptedException {
        if (release != null) release.countDown();
        if (executor != null) {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
        if (auctionId != null) {
            jdbc.update("DELETE FROM dbo.notifications WHERE target_type='AUCTION' AND target_id=?", auctionId);
            results.findByAuctionId(auctionId).ifPresent(results::delete);
            bids.deleteAllInBatch(bids.findByAuctionIdOrderByCreatedAtDesc(auctionId));
            if (auctions.existsById(auctionId)) auctions.deleteById(auctionId);
        }
        if (productId != null && products.existsById(productId)) products.deleteById(productId);
        if (buyerId != null && members.existsById(buyerId)) members.deleteById(buyerId);
    }

    @Test
    void adminRemoveHoldingLockRejectsBidWaitingOnTheSameAuctionRow() throws Exception {
        fixture(END.plusHours(1));
        CountDownLatch locked = blockFirstTimeCallAfterLock();
        executor = Executors.newFixedThreadPool(2);

        Future<?> remover = executor.submit(() ->
                adminMutations.removeAuction(auctionId, ADMIN_ID, "併發下架"));
        assertTrue(locked.await(10, TimeUnit.SECONDS));
        CountDownLatch biddingStarted = new CountDownLatch(1);
        Future<?> bidder = executor.submit(() -> {
            biddingStarted.countDown();
            assertThrows(AuctionConflictException.class, () -> bidding.placeBid(
                    auctionId, buyerId, new BidCreateRequest(new BigDecimal("5500.00"))));
        });
        assertTrue(biddingStarted.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> bidder.get(200, TimeUnit.MILLISECONDS),
                "Bid 應等待 Admin remove 持有的 DB row lock");

        release.countDown();
        remover.get(20, TimeUnit.SECONDS);
        bidder.get(20, TimeUnit.SECONDS);

        Auction persisted = persisted();
        assertEquals(AuctionStatus.ACTIVE, persisted.getStatus());
        assertNotNull(persisted.getRemovedAt());
        assertEquals(0, persisted.getBidCount());
        assertTrue(bids.findByAuctionIdOrderByCreatedAtDesc(auctionId).isEmpty());
        assertEquals(0, resultRowCount());
    }

    @Test
    void adminRemoveHoldingLockMakesWaitingLifecycleVoidAuctionAtEnd() throws Exception {
        fixture(END);
        when(time.now()).thenReturn(END.minusSeconds(1));
        bidding.placeBid(auctionId, buyerId, new BidCreateRequest(new BigDecimal("5500.00")));
        CountDownLatch locked = blockFirstTimeCallAfterLock();
        executor = Executors.newFixedThreadPool(2);

        Future<?> remover = executor.submit(() ->
                adminMutations.removeAuction(auctionId, ADMIN_ID, "結標前下架"));
        assertTrue(locked.await(10, TimeUnit.SECONDS));
        CountDownLatch lifecycleStarted = new CountDownLatch(1);
        Future<?> closer = executor.submit(() -> {
            lifecycleStarted.countDown();
            lifecycle.processAuction(auctionId);
        });
        assertTrue(lifecycleStarted.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> closer.get(200, TimeUnit.MILLISECONDS),
                "Lifecycle 應等待 Admin remove 持有的 DB row lock");

        release.countDown();
        remover.get(20, TimeUnit.SECONDS);
        closer.get(20, TimeUnit.SECONDS);

        Auction persisted = persisted();
        AuctionResult result = results.findByAuctionId(auctionId).orElseThrow();
        assertEquals(AuctionStatus.REMOVED, persisted.getStatus());
        assertNotNull(persisted.getRemovedAt());
        assertEquals(1, persisted.getBidCount());
        assertEquals(buyerId, persisted.getHighestBidderId());
        assertEquals(0, new BigDecimal("5500.00").compareTo(persisted.getCurrentPrice()));
        assertEquals(AuctionResultStatus.REMOVED, result.getResultStatus());
        assertNull(result.getWinnerId());
        assertNull(result.getFinalPrice());
        assertFalse(result.getReason().isBlank());
        assertEquals(1, resultRowCount());
    }

    @Test
    void lifecycleHoldingLockCommitsSoldBeforeWaitingAdminAddsOnlyVisibilityMetadata() throws Exception {
        fixture(END);
        when(time.now()).thenReturn(END.minusSeconds(1));
        bidding.placeBid(auctionId, buyerId, new BidCreateRequest(new BigDecimal("5500.00")));
        CountDownLatch locked = blockFirstTimeCallAfterLock();
        executor = Executors.newFixedThreadPool(2);

        Future<?> closer = executor.submit(() -> lifecycle.processAuction(auctionId));
        assertTrue(locked.await(10, TimeUnit.SECONDS));
        CountDownLatch removeStarted = new CountDownLatch(1);
        Future<?> remover = executor.submit(() -> {
            removeStarted.countDown();
            adminMutations.removeAuction(auctionId, ADMIN_ID, "成交後隱藏");
        });
        assertTrue(removeStarted.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> remover.get(200, TimeUnit.MILLISECONDS),
                "Admin remove 應等待 Lifecycle 持有的 DB row lock");

        release.countDown();
        closer.get(20, TimeUnit.SECONDS);
        remover.get(20, TimeUnit.SECONDS);

        Auction persisted = persisted();
        AuctionResult result = results.findByAuctionId(auctionId).orElseThrow();
        assertEquals(AuctionStatus.SOLD, persisted.getStatus());
        assertEquals(AuctionStatus.SOLD, persisted.getRemovedFromStatus());
        assertNotNull(persisted.getRemovedAt());
        assertEquals(1, persisted.getBidCount());
        assertEquals(buyerId, persisted.getHighestBidderId());
        assertEquals(AuctionResultStatus.SOLD, result.getResultStatus());
        assertEquals(buyerId, result.getWinnerId());
        assertEquals(0, persisted.getCurrentPrice().compareTo(result.getFinalPrice()));
        assertEquals(1, resultRowCount());
    }

    @Test
    void restoreHoldingLockMakesWaitingDeleteFailAgainstFreshVisibleState() throws Exception {
        fixture(END.plusHours(1));
        markRemoved();
        CountDownLatch locked = blockFirstTimeCallAfterLock();
        executor = Executors.newFixedThreadPool(2);

        Future<?> restorer = executor.submit(() -> adminMutations.restoreAuction(auctionId, ADMIN_ID));
        assertTrue(locked.await(10, TimeUnit.SECONDS));
        CountDownLatch deleteStarted = new CountDownLatch(1);
        Future<?> deleter = executor.submit(() -> {
            deleteStarted.countDown();
            AuctionConflictException error = assertThrows(AuctionConflictException.class,
                    () -> adminMutations.deleteAuction(auctionId, ADMIN_ID, "併發刪除"));
            assertEquals("競標必須先下架才能刪除", error.getMessage());
        });
        assertTrue(deleteStarted.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> deleter.get(200, TimeUnit.MILLISECONDS),
                "Delete 應等待 Restore 持有的 DB row lock");

        release.countDown();
        restorer.get(20, TimeUnit.SECONDS);
        deleter.get(20, TimeUnit.SECONDS);

        Auction persisted = persisted();
        assertNull(persisted.getRemovedAt());
        assertNull(persisted.getDeletedAt());
        assertEquals(AuctionStatus.ACTIVE, persisted.getStatus());
    }

    @Test
    void restoreHoldingLockLetsWaitingLifecycleUseRestoredState() throws Exception {
        fixture(END.plusHours(1));
        markRemoved();
        CountDownLatch locked = blockFirstTimeCallAfterLock();
        executor = Executors.newFixedThreadPool(2);

        Future<?> restorer = executor.submit(() -> adminMutations.restoreAuction(auctionId, ADMIN_ID));
        assertTrue(locked.await(10, TimeUnit.SECONDS));
        CountDownLatch lifecycleStarted = new CountDownLatch(1);
        Future<?> processor = executor.submit(() -> {
            lifecycleStarted.countDown();
            lifecycle.processAuction(auctionId);
        });
        assertTrue(lifecycleStarted.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> processor.get(200, TimeUnit.MILLISECONDS),
                "Lifecycle 應等待 Restore 持有的 DB row lock");

        release.countDown();
        restorer.get(20, TimeUnit.SECONDS);
        processor.get(20, TimeUnit.SECONDS);

        Auction persisted = persisted();
        assertNull(persisted.getRemovedAt());
        assertEquals(AuctionStatus.ACTIVE, persisted.getStatus());
        assertEquals(0, resultRowCount());

        when(time.now()).thenReturn(END.plusHours(1));
        lifecycle.processAuction(auctionId);
        assertEquals(AuctionStatus.UNSOLD, persisted().getStatus());
        assertEquals(AuctionResultStatus.UNSOLD,
                results.findByAuctionId(auctionId).orElseThrow().getResultStatus());
    }

    @Test
    void lifecycleVoidingHoldingLockMakesWaitingRestoreFail() throws Exception {
        fixture(END);
        markRemoved();
        CountDownLatch locked = blockFirstTimeCallAfterLock();
        executor = Executors.newFixedThreadPool(2);

        Future<?> closer = executor.submit(() -> lifecycle.processAuction(auctionId));
        assertTrue(locked.await(10, TimeUnit.SECONDS));
        CountDownLatch restoreStarted = new CountDownLatch(1);
        Future<?> restorer = executor.submit(() -> {
            restoreStarted.countDown();
            AuctionConflictException error = assertThrows(AuctionConflictException.class,
                    () -> adminMutations.restoreAuction(auctionId, ADMIN_ID));
            assertEquals("廢標競標無法重新上架", error.getMessage());
        });
        assertTrue(restoreStarted.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> restorer.get(200, TimeUnit.MILLISECONDS),
                "Restore 應等待 Lifecycle 持有的 DB row lock");

        release.countDown();
        closer.get(20, TimeUnit.SECONDS);
        restorer.get(20, TimeUnit.SECONDS);

        assertEquals(AuctionStatus.REMOVED, persisted().getStatus());
        assertNotNull(persisted().getRemovedAt());
        assertEquals(AuctionResultStatus.REMOVED,
                results.findByAuctionId(auctionId).orElseThrow().getResultStatus());
    }

    @Test
    void restoreHoldingLockLetsWaitingBidSucceedAfterVisibilityReturns() throws Exception {
        fixture(END.plusHours(1));
        markRemoved();
        CountDownLatch locked = blockFirstTimeCallAfterLock();
        executor = Executors.newFixedThreadPool(2);

        Future<?> restorer = executor.submit(() -> adminMutations.restoreAuction(auctionId, ADMIN_ID));
        assertTrue(locked.await(10, TimeUnit.SECONDS));
        CountDownLatch biddingStarted = new CountDownLatch(1);
        Future<?> bidder = executor.submit(() -> {
            biddingStarted.countDown();
            bidding.placeBid(auctionId, buyerId, new BidCreateRequest(new BigDecimal("5500.00")));
        });
        assertTrue(biddingStarted.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> bidder.get(200, TimeUnit.MILLISECONDS),
                "Bid 應等待 Restore 持有的 DB row lock");

        release.countDown();
        restorer.get(20, TimeUnit.SECONDS);
        bidder.get(20, TimeUnit.SECONDS);

        Auction persisted = persisted();
        assertNull(persisted.getRemovedAt());
        assertEquals(AuctionStatus.ACTIVE, persisted.getStatus());
        assertEquals(1, persisted.getBidCount());
        assertEquals(buyerId, persisted.getHighestBidderId());
        assertEquals(1, bids.findByAuctionIdOrderByCreatedAtDesc(auctionId).size());
    }

    private CountDownLatch blockFirstTimeCallAfterLock() {
        CountDownLatch locked = new CountDownLatch(1);
        release = new CountDownLatch(1);
        AtomicBoolean first = new AtomicBoolean(true);
        when(time.now()).thenAnswer(call -> {
            if (first.compareAndSet(true, false)) {
                locked.countDown();
                assertTrue(release.await(10, TimeUnit.SECONDS));
            }
            return END;
        });
        return locked;
    }

    private void markRemoved() {
        Auction auction = persisted();
        auction.setRemovedAt(END.minusMinutes(5));
        auction.setRemovedByMemberId(ADMIN_ID);
        auction.setRemoveReason("併發恢復 fixture");
        auction.setRemovedFromStatus(auction.getStatus());
        auctions.saveAndFlush(auction);
    }

    private void fixture(LocalDateTime endAt) {
        AuctionProduct product = new AuctionProduct();
        product.setSellerId(9_300_001L);
        product.setBrandId(1L);
        product.setCategoryId(1L);
        product.setProductName("Admin concurrency fixture");
        product.setSize("US 9");
        product.setCondition(AuctionCondition.NEW);
        product.setDescription("Feature 11 PESSIMISTIC_WRITE fixture");
        product.setShippingMethod("Delivery");
        product.setShippingFee(BigDecimal.ZERO);
        product.setPaymentMethod("Transfer");
        product.setAllowMeetup(false);
        product.setAllowReturn(false);
        product.setCreatedAt(END.minusHours(2));
        product.setUpdatedAt(END.minusHours(2));
        product = products.saveAndFlush(product);
        productId = product.getAuctionProductId();

        Auction auction = new Auction();
        auction.setAuctionProductId(productId);
        auction.setStartPrice(new BigDecimal("5000.00"));
        auction.setCurrentPrice(new BigDecimal("5000.00"));
        auction.setMinIncrement(new BigDecimal("500.00"));
        auction.setStartAt(END.minusHours(1));
        auction.setEndAt(endAt);
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setBidCount(0);
        auction.setCreatedAt(END.minusHours(2));
        auction.setUpdatedAt(END.minusHours(2));
        auction = auctions.saveAndFlush(auction);
        auctionId = auction.getAuctionId();
    }

    private Auction persisted() {
        return auctions.findById(auctionId).orElseThrow();
    }

    private int resultRowCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM dbo.auction_results WHERE auction_id = ?",
                Integer.class, auctionId);
    }
}
