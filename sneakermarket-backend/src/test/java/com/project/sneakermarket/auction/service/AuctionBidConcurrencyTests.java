package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.project.sneakermarket.auction.dto.BidCreateRequest;
import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionBid;
import com.project.sneakermarket.auction.entity.AuctionCondition;
import com.project.sneakermarket.auction.entity.AuctionProduct;
import com.project.sneakermarket.auction.entity.AuctionStatus;
import com.project.sneakermarket.auction.repository.AuctionBidRepository;
import com.project.sneakermarket.auction.repository.AuctionProductRepository;
import com.project.sneakermarket.auction.repository.AuctionRepository;
import com.project.sneakermarket.common.exception.BusinessException;

/**
 * 使用專案設定的真實 MSSQL 驗證 PESSIMISTIC_WRITE。
 *
 * 測試方法刻意不加 @Transactional，讓初始化先 Commit，且兩個執行緒中的
 * Service 呼叫各自建立並提交獨立 Transaction。
 */
@SpringBootTest
class AuctionBidConcurrencyTests {

    private static final Long SELLER_ID = 9_100_001L;
    private static final Long BUYER_A_ID = 9_100_002L;
    private static final Long BUYER_B_ID = 9_100_003L;

    @Autowired
    private AuctionBidService auctionBidService;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private AuctionProductRepository auctionProductRepository;

    @Autowired
    private AuctionBidRepository auctionBidRepository;

    @Autowired
    private AuctionTime auctionTime;

    private Long auctionId;
    private Long productId;
    private ExecutorService executor;

    @AfterEach
    void cleanupCommittedFixture() throws InterruptedException {
        if (executor != null) {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        if (auctionId != null) {
            List<AuctionBid> bids = auctionBidRepository
                    .findByAuctionIdOrderByCreatedAtDesc(auctionId);
            auctionBidRepository.deleteAllInBatch(bids);
            auctionRepository.deleteById(auctionId);
        }

        if (productId != null) {
            auctionProductRepository.deleteById(productId);
        }
    }

    @Test
    void sameAmountConcurrentBidsAllowOnlyOneCommittedWinner() throws Exception {
        createCommittedAuctionFixture();

        executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<BidAttempt> buyerA = executor.submit(
                () -> attemptBid(BUYER_A_ID, ready, start)
        );
        Future<BidAttempt> buyerB = executor.submit(
                () -> attemptBid(BUYER_B_ID, ready, start)
        );

        assertTrue(ready.await(5, TimeUnit.SECONDS), "兩個出價執行緒未及時就緒");
        start.countDown();

        List<BidAttempt> results = List.of(
                buyerA.get(20, TimeUnit.SECONDS),
                buyerB.get(20, TimeUnit.SECONDS)
        );

        List<BidAttempt> successes = results.stream()
                .filter(BidAttempt::success)
                .toList();
        List<BidAttempt> failures = results.stream()
                .filter(result -> !result.success())
                .toList();

        assertEquals(1, successes.size());
        assertEquals(1, failures.size());
        assertInstanceOf(BusinessException.class, failures.get(0).failure());
        assertTrue(failures.get(0).failure().getMessage().contains("最低可出價"));
        assertTrue(failures.get(0).failure().getMessage().contains("6000"));

        Auction persistedAuction = auctionRepository.findById(auctionId).orElseThrow();
        List<AuctionBid> persistedBids = auctionBidRepository
                .findByAuctionIdOrderByCreatedAtDesc(auctionId);

        assertAmount("5500.00", persistedAuction.getCurrentPrice());
        assertEquals(1, persistedAuction.getBidCount());
        assertEquals(successes.get(0).bidderId(), persistedAuction.getHighestBidderId());
        assertEquals(1, persistedBids.size());
        assertEquals(successes.get(0).bidderId(), persistedBids.get(0).getBidderId());
        assertAmount("5500.00", persistedBids.get(0).getBidAmount());
    }

    private BidAttempt attemptBid(
            Long bidderId,
            CountDownLatch ready,
            CountDownLatch start) throws InterruptedException {

        ready.countDown();
        start.await();

        try {
            auctionBidService.placeBid(
                    auctionId,
                    bidderId,
                    new BidCreateRequest(new BigDecimal("5500.00"))
            );
            return new BidAttempt(true, bidderId, null);
        } catch (RuntimeException ex) {
            return new BidAttempt(false, bidderId, ex);
        }
    }

    private void createCommittedAuctionFixture() {
        LocalDateTime now = auctionTime.now().withNano(0);

        AuctionProduct product = new AuctionProduct();
        product.setSellerId(SELLER_ID);
        product.setBrandId(1L);
        product.setCategoryId(1L);
        product.setProductName("Concurrency lock test shoe");
        product.setModel("LOCK-TEST");
        product.setSize("US 9");
        product.setCondition(AuctionCondition.NEW);
        product.setDescription("PESSIMISTIC_WRITE integration test fixture");
        product.setLocation("台北市");
        product.setShippingMethod("宅配");
        product.setShippingFee(BigDecimal.ZERO);
        product.setPaymentMethod("轉帳");
        product.setAllowMeetup(false);
        product.setAllowReturn(false);
        product.setCreatedAt(now);
        product.setUpdatedAt(now);
        product = auctionProductRepository.saveAndFlush(product);
        productId = product.getAuctionProductId();

        Auction auction = new Auction();
        auction.setAuctionProductId(productId);
        auction.setStartPrice(new BigDecimal("5000.00"));
        auction.setCurrentPrice(new BigDecimal("5000.00"));
        auction.setMinIncrement(new BigDecimal("500.00"));
        auction.setStartAt(now.minusHours(1));
        auction.setEndAt(now.plusHours(1));
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setHighestBidderId(null);
        auction.setBidCount(0);
        auction.setCreatedAt(now);
        auction.setUpdatedAt(now);
        auction = auctionRepository.saveAndFlush(auction);
        auctionId = auction.getAuctionId();
    }

    private void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private record BidAttempt(boolean success, Long bidderId, RuntimeException failure) {
    }
}
