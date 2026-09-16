package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.project.sneakermarket.auction.dto.BidCreateRequest;
import com.project.sneakermarket.auction.entity.*;
import com.project.sneakermarket.auction.exception.AuctionConflictException;
import com.project.sneakermarket.auction.repository.*;
import com.project.sneakermarket.auction.scheduler.AuctionLifecycleScheduler;
import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.member.repository.MemberRepository;

/**
 * 真實 MSSQL；無測試外層交易，所有 fixture 先 commit。
 * 僅控制 AuctionTime，不 mock Repository、Service 或資料庫鎖。
 */
@SpringBootTest(properties = {"auction.lifecycle.enabled=false", "spring.jpa.hibernate.ddl-auto=none"})
class AuctionLifecycleIntegrationTests {
    private static final LocalDateTime END = LocalDateTime.of(2026, 9, 10, 12, 0);
    @Autowired private AuctionLifecycleService lifecycle;
    @Autowired private AuctionBidService bidding;
    @Autowired private AuctionQueryService queries;
    @Autowired private AuctionRepository auctions;
    @Autowired private AuctionProductRepository products;
    @Autowired private AuctionBidRepository bids;
    @Autowired private AuctionResultRepository results;
    @Autowired private MemberRepository members;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean private AuctionTime time;

    private final List<Long> auctionIds = new ArrayList<>();
    private final List<Long> productIds = new ArrayList<>();
    private Long buyerId;
    private ExecutorService executor;
    private CountDownLatch release;

    @BeforeEach
    void setup() {
        when(time.now()).thenReturn(END);
        Member buyer = new Member();
        String token = UUID.randomUUID().toString();
        buyer.setAccount("lc-" + token);
        buyer.setEmail(token + "@example.invalid");
        buyer.setPasswordHash("integration-fixture-not-a-login");
        buyer.setName("Lifecycle fixture");
        buyer.setPhone("0900000000");
        // 刻意不填地址：競標結果不依賴訂單收件資料。
        buyerId = members.saveAndFlush(buyer).getMemberId();
    }

    @AfterEach
    void cleanup() throws InterruptedException {
        if (release != null) release.countDown();
        if (executor != null) {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
        for (Long id : auctionIds) {
            jdbc.update("DELETE FROM dbo.notifications WHERE target_type='AUCTION' AND target_id=?", id);
            results.findByAuctionId(id).ifPresent(results::delete);
            bids.deleteAllInBatch(bids.findByAuctionIdOrderByCreatedAtDesc(id));
            auctions.deleteById(id);
        }
        for (Long id : productIds) products.deleteById(id);
        if (buyerId != null) members.deleteById(buyerId);
    }

    @Test
    void candidateQueryFiltersStatusAndTimeIncludingExactBoundaries() {
        Long future = fixture(AuctionStatus.SCHEDULED, END.plusSeconds(1), END.plusHours(1));
        Long start = fixture(AuctionStatus.SCHEDULED, END, END.plusHours(1));
        Long delayed = fixture(AuctionStatus.SCHEDULED, END.minusHours(1), END.minusSeconds(1));
        Long active = fixture(AuctionStatus.ACTIVE, END.minusHours(1), END.plusSeconds(1));
        Long due = fixture(AuctionStatus.ACTIVE, END.minusHours(1), END);
        Long removedScheduledWaiting = fixture(AuctionStatus.SCHEDULED, END.minusHours(1), END.plusSeconds(1));
        Long removedScheduledDue = fixture(AuctionStatus.SCHEDULED, END.minusHours(1), END);
        Long removedActiveWaiting = fixture(AuctionStatus.ACTIVE, END.minusHours(1), END.plusSeconds(1));
        Long removedActiveDue = fixture(AuctionStatus.ACTIVE, END.minusHours(1), END);
        Long deletedDue = fixture(AuctionStatus.ACTIVE, END.minusHours(1), END);
        change(removedScheduledWaiting, this::markRemoved);
        change(removedScheduledDue, this::markRemoved);
        change(removedActiveWaiting, this::markRemoved);
        change(removedActiveDue, this::markRemoved);
        change(deletedDue, a -> a.setDeletedAt(END.minusMinutes(1)));
        List<Long> excluded = new ArrayList<>(List.of(
                future, active, removedScheduledWaiting, removedActiveWaiting, deletedDue));
        for (AuctionStatus terminal : List.of(AuctionStatus.DRAFT, AuctionStatus.SOLD,
                AuctionStatus.UNSOLD, AuctionStatus.CANCELLED, AuctionStatus.REMOVED)) {
            excluded.add(fixture(terminal, END.minusHours(1), END));
        }
        List<Long> candidates = auctions.findLifecycleCandidateIds(END);
        assertTrue(candidates.containsAll(List.of(start, delayed, due, removedScheduledDue, removedActiveDue)));
        excluded.forEach(id -> assertFalse(candidates.contains(id)));
        // 取得候選後被取消，Service 必須以 lock 內的新狀態為準。
        change(start, a -> a.setStatus(AuctionStatus.CANCELLED));
        lifecycle.processAuction(start);
        assertEquals(AuctionStatus.CANCELLED, persisted(start).getStatus());
        assertTrue(results.findByAuctionId(start).isEmpty());
    }

    @Test
    void scheduledStartIsPersistedAndVisibleToExistingStatusQuery() {
        Long id = fixture(AuctionStatus.SCHEDULED, END, END.plusHours(1));
        when(time.now()).thenReturn(END.minusSeconds(1));
        lifecycle.processAuction(id);
        assertEquals(AuctionStatus.SCHEDULED, persisted(id).getStatus());
        when(time.now()).thenReturn(END);
        lifecycle.processAuction(id);
        assertEquals(AuctionStatus.ACTIVE, queries.getAuctionStatus(id).status());
        assertEquals(END, persisted(id).getUpdatedAt());
        assertTrue(results.findByAuctionId(id).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = AuctionStatus.class, names = {"ACTIVE", "SCHEDULED"})
    void expiredWithoutBidsCommitsOneUnsoldResult(AuctionStatus initial) {
        Long id = fixture(initial, END.minusHours(1), END);
        lifecycle.processAuction(id);
        lifecycle.processAuction(id);
        assertConsistent(id, false);
        assertEquals(END, results.findByAuctionId(id).orElseThrow().getEndedAt());
        assertEquals(END, persisted(id).getUpdatedAt());
    }

    @ParameterizedTest
    @CsvSource({"SCHEDULED,false", "SCHEDULED,true", "ACTIVE,false", "ACTIVE,true"})
    void removedAuctionAtEndCommitsVoidedResultAndPreservesLedger(
            AuctionStatus initial, boolean withBid) {
        Long id = fixture(withBid ? AuctionStatus.ACTIVE : initial, END.minusHours(1), END);
        if (withBid) {
            when(time.now()).thenReturn(END.minusSeconds(1));
            bid(id);
        }
        change(id, auction -> {
            auction.setStatus(initial);
            markRemoved(auction);
        });
        when(time.now()).thenReturn(END);

        lifecycle.processAuction(id);
        lifecycle.processAuction(id);

        assertVoided(id, withBid);
    }

    @Test
    void latestSuccessfulBidCommitsSoldResultWithoutReceiverAddress() {
        Long id = fixture(AuctionStatus.ACTIVE, END.minusHours(1), END);
        when(time.now()).thenReturn(END.minusSeconds(1));
        bid(id);
        when(time.now()).thenReturn(END);
        lifecycle.processAuction(id);
        lifecycle.processAuction(id);
        assertConsistent(id, true);
        assertThrows(AuctionConflictException.class, () -> bid(id));
        assertConsistent(id, true);
    }

    @Test
    void deadlineRejectsBidEvenBeforeSchedulerRuns() {
        Long id = fixture(AuctionStatus.ACTIVE, END.minusHours(1), END);
        assertThrows(AuctionConflictException.class, () -> bid(id));
        assertEquals(AuctionStatus.ACTIVE, persisted(id).getStatus());
        assertTrue(bids.findByAuctionIdOrderByCreatedAtDesc(id).isEmpty());
        assertTrue(results.findByAuctionId(id).isEmpty());
    }

    @Test
    void inconsistentAuctionRollsBackWhileNextSchedulerItemCommits() {
        Long bad = fixture(AuctionStatus.ACTIVE, END.minusHours(1), END);
        change(bad, a -> a.setBidCount(1));
        Long good = fixture(AuctionStatus.ACTIVE, END.minusHours(1), END);
        // 只限制掃描 ID，不 mock 真正的交易服務與 row lock。
        AuctionRepository candidateSource = mock(AuctionRepository.class);
        when(candidateSource.findLifecycleCandidateIds(END)).thenReturn(List.of(bad, good));
        new AuctionLifecycleScheduler(candidateSource, lifecycle, time).updateAuctionLifecycle();
        assertEquals(AuctionStatus.ACTIVE, persisted(bad).getStatus());
        assertEquals(END.minusHours(2), persisted(bad).getUpdatedAt());
        assertTrue(results.findByAuctionId(bad).isEmpty());
        assertConsistent(good, false);
    }

    @Test
    void resultForeignKeyFailureRollsBackFlushedAuctionUpdate() {
        Long id = fixture(AuctionStatus.ACTIVE, END.minusHours(1), END);
        change(id, a -> {
            a.setBidCount(1);
            a.setHighestBidderId(-1L);
        });
        Auction before = persisted(id);
        // 與正式 Service 使用同一 Spring 交易，強制 flush 以驗證 DB 已收到 UPDATE。
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        assertThrows(DataIntegrityViolationException.class, () -> transaction.executeWithoutResult(status -> {
            Auction locked = auctions.findByIdForUpdate(id).orElseThrow();
            locked.setUpdatedAt(END.minusMinutes(1));
            auctions.saveAndFlush(locked);
            lifecycle.processAuction(id); // INSERT 觸發正式 winner FK，不是 Mock 儲存失敗。
        }));
        assertEquals(before.getStatus(), persisted(id).getStatus());
        assertEquals(before.getUpdatedAt(), persisted(id).getUpdatedAt());
        assertEquals(before.getVersion(), persisted(id).getVersion());
        assertTrue(results.findByAuctionId(id).isEmpty());
    }

    @Test
    void uniqueAuctionConstraintRejectsSecondResult() {
        Long id = fixture(AuctionStatus.ACTIVE, END.minusHours(1), END);
        lifecycle.processAuction(id);
        AuctionResult duplicate = new AuctionResult();
        duplicate.setAuctionId(id);
        duplicate.setResultStatus(AuctionResultStatus.UNSOLD);
        duplicate.setEndedAt(END);
        duplicate.setCreatedAt(END);
        assertThrows(DataIntegrityViolationException.class, () -> results.saveAndFlush(duplicate));
        assertConsistent(id, false);
    }

    @Test
    void bidHoldingLockBeforeDeadlineWinsAndClosingReadsCommittedBid() throws Exception {
        Long id = fixture(AuctionStatus.ACTIVE, END.minusHours(1), END);
        CountDownLatch locked = new CountDownLatch(1);
        release = new CountDownLatch(1);
        AtomicBoolean first = new AtomicBoolean(true);
        when(time.now()).thenAnswer(call -> {
            if (first.compareAndSet(true, false)) {
                locked.countDown(); // placeBid 已取得真實 Auction row lock。
                assertTrue(release.await(10, TimeUnit.SECONDS));
                return END.minusSeconds(1);
            }
            return END;
        });
        executor = Executors.newFixedThreadPool(2);
        Future<?> bidder = executor.submit(() -> bid(id));
        assertTrue(locked.await(10, TimeUnit.SECONDS));
        CountDownLatch closingStarted = new CountDownLatch(1);
        Future<?> closer = executor.submit(() -> {
            closingStarted.countDown();
            lifecycle.processAuction(id);
        });
        assertTrue(closingStarted.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> closer.get(200, TimeUnit.MILLISECONDS),
                "Closing 應等待出價持有的 DB row lock");
        release.countDown();
        bidder.get(20, TimeUnit.SECONDS);
        closer.get(20, TimeUnit.SECONDS);
        assertConsistent(id, true);
    }

    @ParameterizedTest
    @EnumSource(value = AuctionStatus.class, names = {"SOLD", "UNSOLD"})
    void closingHoldingLockRejectsWaitingBid(AuctionStatus expected) throws Exception {
        Long id = fixture(AuctionStatus.ACTIVE, END.minusHours(1), END);
        if (expected == AuctionStatus.SOLD) {
            when(time.now()).thenReturn(END.minusSeconds(1));
            bid(id);
        }
        CountDownLatch locked = new CountDownLatch(1);
        release = new CountDownLatch(1);
        AtomicBoolean first = new AtomicBoolean(true);
        when(time.now()).thenAnswer(call -> {
            if (first.compareAndSet(true, false)) {
                locked.countDown(); // lifecycle 已取得真實 Auction row lock。
                assertTrue(release.await(10, TimeUnit.SECONDS));
            }
            return END;
        });
        executor = Executors.newFixedThreadPool(2);
        Future<?> closer = executor.submit(() -> lifecycle.processAuction(id));
        assertTrue(locked.await(10, TimeUnit.SECONDS));
        CountDownLatch biddingStarted = new CountDownLatch(1);
        Future<?> bidder = executor.submit(() -> {
            biddingStarted.countDown();
            assertThrows(AuctionConflictException.class, () ->
                    bidding.placeBid(id, buyerId, new BidCreateRequest(new BigDecimal("6000.00"))));
        });
        assertTrue(biddingStarted.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> bidder.get(200, TimeUnit.MILLISECONDS),
                "Bid 應等待結標持有的 DB row lock");
        release.countDown();
        closer.get(20, TimeUnit.SECONDS);
        bidder.get(20, TimeUnit.SECONDS);
        assertConsistent(id, expected == AuctionStatus.SOLD);
    }

    @Test
    void concurrentClosersCommitExactlyOneResult() throws Exception {
        Long id = fixture(AuctionStatus.ACTIVE, END.minusHours(1), END);
        executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        release = new CountDownLatch(1);
        Callable<Void> closing = () -> {
            ready.countDown();
            assertTrue(release.await(10, TimeUnit.SECONDS));
            lifecycle.processAuction(id);
            return null;
        };
        Future<?> a = executor.submit(closing);
        Future<?> b = executor.submit(closing);
        assertTrue(ready.await(5, TimeUnit.SECONDS));
        release.countDown();
        a.get(20, TimeUnit.SECONDS);
        b.get(20, TimeUnit.SECONDS);
        assertConsistent(id, false);
    }

    private void assertVoided(Long id, boolean withBid) {
        Auction auction = persisted(id);
        AuctionResult result = results.findByAuctionId(id).orElseThrow();
        List<AuctionBid> history = bids.findByAuctionIdOrderByCreatedAtDesc(id);
        assertEquals(AuctionStatus.REMOVED, auction.getStatus());
        assertNotNull(auction.getRemovedAt());
        assertEquals(withBid ? 1 : 0, auction.getBidCount());
        assertEquals(withBid ? buyerId : null, auction.getHighestBidderId());
        assertEquals(0, new BigDecimal(withBid ? "5500.00" : "5000.00")
                .compareTo(auction.getCurrentPrice()));
        assertEquals(withBid ? 1 : 0, history.size());
        assertEquals(AuctionResultStatus.REMOVED, result.getResultStatus());
        assertNull(result.getWinnerId());
        assertNull(result.getFinalPrice());
        assertNotNull(result.getReason());
        assertFalse(result.getReason().isBlank());
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM dbo.auction_results WHERE auction_id = ?", Integer.class, id));
    }

    private void assertConsistent(Long id, boolean sold) {
        Auction auction = persisted(id);
        AuctionResult result = results.findByAuctionId(id).orElseThrow();
        List<AuctionBid> history = bids.findByAuctionIdOrderByCreatedAtDesc(id);
        assertEquals(sold ? AuctionStatus.SOLD : AuctionStatus.UNSOLD, auction.getStatus());
        assertEquals(auction.getStatus(), queries.getAuctionStatus(id).status());
        assertEquals(history.size(), auction.getBidCount());
        assertEquals(sold ? 1 : 0, history.size());
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM dbo.auction_results WHERE auction_id = ?", Integer.class, id));
        assertEquals(sold ? AuctionResultStatus.SOLD : AuctionResultStatus.UNSOLD, result.getResultStatus());
        if (sold) {
            assertEquals(auction.getHighestBidderId(), result.getWinnerId());
            assertEquals(buyerId, result.getWinnerId());
            assertEquals(0, auction.getCurrentPrice().compareTo(result.getFinalPrice()));
            assertEquals(0, history.get(0).getBidAmount().compareTo(result.getFinalPrice()));
            assertEquals(history.get(0).getBidderId(), result.getWinnerId());
            assertNull(result.getReason());
        } else {
            assertNull(result.getWinnerId());
            assertNull(result.getFinalPrice());
            assertFalse(result.getReason().isBlank());
        }
    }

    private void bid(Long id) {
        bidding.placeBid(id, buyerId, new BidCreateRequest(new BigDecimal("5500.00")));
    }

    private Auction persisted(Long id) {
        return auctions.findById(id).orElseThrow();
    }

    private void change(Long id, java.util.function.Consumer<Auction> change) {
        Auction auction = persisted(id);
        change.accept(auction);
        auctions.saveAndFlush(auction);
    }

    private void markRemoved(Auction auction) {
        auction.setRemovedAt(END.minusMinutes(30));
        auction.setRemovedByMemberId(7L);
        auction.setRemoveReason("整合測試下架");
        auction.setRemovedFromStatus(auction.getStatus());
    }

    private Long fixture(AuctionStatus status, LocalDateTime start, LocalDateTime end) {
        AuctionProduct product = new AuctionProduct();
        product.setSellerId(9_200_001L);
        product.setBrandId(1L);
        product.setCategoryId(1L);
        product.setProductName("Lifecycle integration fixture");
        product.setSize("US 9");
        product.setCondition(AuctionCondition.NEW);
        product.setDescription("Owned by lifecycle integration test");
        product.setShippingMethod("Delivery");
        product.setShippingFee(BigDecimal.ZERO);
        product.setPaymentMethod("Transfer");
        product.setAllowMeetup(false);
        product.setAllowReturn(false);
        product.setCreatedAt(END.minusHours(2));
        product.setUpdatedAt(END.minusHours(2));
        product = products.saveAndFlush(product);
        productIds.add(product.getAuctionProductId());

        Auction auction = new Auction();
        auction.setAuctionProductId(product.getAuctionProductId());
        auction.setStartPrice(new BigDecimal("5000.00"));
        auction.setCurrentPrice(new BigDecimal("5000.00"));
        auction.setMinIncrement(new BigDecimal("500.00"));
        auction.setStartAt(start);
        auction.setEndAt(end);
        auction.setStatus(status);
        auction.setBidCount(0);
        auction.setCreatedAt(END.minusHours(2));
        auction.setUpdatedAt(END.minusHours(2));
        auction = auctions.saveAndFlush(auction);
        auctionIds.add(auction.getAuctionId());
        return auction.getAuctionId();
    }
}
