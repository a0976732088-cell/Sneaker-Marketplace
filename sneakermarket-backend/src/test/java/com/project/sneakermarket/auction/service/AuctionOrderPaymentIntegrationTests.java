package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.project.sneakermarket.auction.dto.AuctionOrderPaymentResponse;
import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionCondition;
import com.project.sneakermarket.auction.entity.AuctionOrder;
import com.project.sneakermarket.auction.entity.AuctionOrderStatus;
import com.project.sneakermarket.auction.entity.AuctionPaymentStatus;
import com.project.sneakermarket.auction.entity.AuctionProduct;
import com.project.sneakermarket.auction.entity.AuctionShippingStatus;
import com.project.sneakermarket.auction.entity.AuctionStatus;
import com.project.sneakermarket.auction.exception.AuctionConflictException;
import com.project.sneakermarket.auction.repository.AuctionOrderRepository;
import com.project.sneakermarket.auction.repository.AuctionProductRepository;
import com.project.sneakermarket.auction.repository.AuctionRepository;
import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.member.repository.MemberRepository;

/** 真實 MSSQL；兩個 Service 呼叫各自使用獨立 Transaction 驗證付款 row lock 與冪等性。 */
@SpringBootTest(properties = {
        "auction.lifecycle.enabled=false", "auction.order.enabled=false",
        "auction.email.enabled=false", "spring.jpa.hibernate.ddl-auto=validate"
})
class AuctionOrderPaymentIntegrationTests {
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 9, 14, 9, 0);
    private static final LocalDateTime PAYMENT_AT = LocalDateTime.of(2026, 9, 14, 12, 30);
    private static final LocalDateTime PAYMENT_DUE_AT = PAYMENT_AT.plusHours(1);

    @Autowired private AuctionOrderPaymentService payments;
    @Autowired private AuctionOrderPaymentDeadlineService deadlines;
    @Autowired private AuctionOrderRepository orders;
    @Autowired private AuctionRepository auctions;
    @Autowired private AuctionProductRepository products;
    @Autowired private MemberRepository members;
    @MockitoBean private AuctionTime time;

    private Long buyerId;
    private Long sellerId;
    private Long productId;
    private Long auctionId;
    private Long orderId;
    private String orderNumber;
    private ExecutorService executor;
    private CountDownLatch release;

    @BeforeEach
    void setup() {
        buyerId = createMember("付款買家");
        sellerId = createMember("競標賣家");
        createCommittedPendingOrder();
    }

    @AfterEach
    void cleanup() throws InterruptedException {
        if (release != null) release.countDown();
        if (executor != null) {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
        if (orderId != null && orders.existsById(orderId)) orders.deleteById(orderId);
        if (auctionId != null && auctions.existsById(auctionId)) auctions.deleteById(auctionId);
        if (productId != null && products.existsById(productId)) products.deleteById(productId);
        if (buyerId != null && members.existsById(buyerId)) members.deleteById(buyerId);
        if (sellerId != null && members.existsById(sellerId)) members.deleteById(sellerId);
    }

    @Test
    void concurrentPaymentsSerializeAndCreateOnlyOnePaymentTimestamp() throws Exception {
        CountDownLatch firstPaymentReached = new CountDownLatch(1);
        release = new CountDownLatch(1);
        AtomicBoolean first = new AtomicBoolean(true);
        when(time.now()).thenAnswer(call -> {
            if (first.compareAndSet(true, false)) {
                firstPaymentReached.countDown();
                assertTrue(release.await(10, TimeUnit.SECONDS));
            }
            return PAYMENT_AT;
        });
        executor = Executors.newFixedThreadPool(2);

        Future<AuctionOrderPaymentResponse> firstRequest = executor.submit(
                () -> payments.pay(orderNumber, buyerId));
        assertTrue(firstPaymentReached.await(10, TimeUnit.SECONDS),
                "第一個付款交易未在持有 order row lock 時進入付款狀態更新");

        CountDownLatch secondStarted = new CountDownLatch(1);
        Future<AuctionOrderPaymentResponse> secondRequest = executor.submit(() -> {
            secondStarted.countDown();
            return payments.pay(orderNumber, buyerId);
        });
        assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> secondRequest.get(200, TimeUnit.MILLISECONDS),
                "第二個付款交易應等待第一個交易持有的 order row lock");

        release.countDown();
        AuctionOrderPaymentResponse firstResponse = firstRequest.get(20, TimeUnit.SECONDS);
        AuctionOrderPaymentResponse secondResponse = secondRequest.get(20, TimeUnit.SECONDS);

        AuctionOrder persisted = orders.findByOrderNumber(orderNumber).orElseThrow();
        assertEquals(AuctionPaymentStatus.PAID, persisted.getPaymentStatus());
        assertEquals(AuctionOrderStatus.PAID, persisted.getOrderStatus());
        assertEquals(PAYMENT_AT, persisted.getPaidAt());
        assertEquals(PAYMENT_AT, persisted.getUpdatedAt());
        assertEquals(PAYMENT_DUE_AT, persisted.getPaymentDueAt());
        assertNull(persisted.getAbandonedAt());
        assertEquals(AuctionShippingStatus.NOT_SHIPPED, persisted.getShippingStatus());
        assertNull(persisted.getShippedAt());
        assertNull(persisted.getCompletedAt());
        assertEquals("轉帳", persisted.getPaymentMethod());
        assertEquals(PAYMENT_AT, firstResponse.paidAt());
        assertEquals(PAYMENT_AT, secondResponse.paidAt());
        assertEquals(PAYMENT_AT, secondResponse.updatedAt());
        verify(time, times(1)).now();
    }

    @Test
    void deadlineCandidateQueryRequiresEveryPredicateAndIncludesEquality() {
        LocalDateTime candidateAt = PAYMENT_DUE_AT;
        AuctionOrder order = orders.findById(orderId).orElseThrow();

        order.setPaymentDueAt(null);
        orders.saveAndFlush(order);
        assertFalse(orders.findPaymentDeadlineCandidateIds(candidateAt).contains(orderId));

        order.setPaymentDueAt(candidateAt.plusSeconds(1));
        orders.saveAndFlush(order);
        assertFalse(orders.findPaymentDeadlineCandidateIds(candidateAt).contains(orderId));

        order.setPaymentDueAt(candidateAt);
        orders.saveAndFlush(order);
        assertTrue(orders.findPaymentDeadlineCandidateIds(candidateAt).contains(orderId));

        order.setPaymentDueAt(candidateAt.minusSeconds(1));
        orders.saveAndFlush(order);
        assertTrue(orders.findPaymentDeadlineCandidateIds(candidateAt).contains(orderId));

        order.setPaymentStatus(AuctionPaymentStatus.PAID);
        order.setOrderStatus(AuctionOrderStatus.PAID);
        orders.saveAndFlush(order);
        assertFalse(orders.findPaymentDeadlineCandidateIds(candidateAt).contains(orderId));

        order.setPaymentStatus(AuctionPaymentStatus.PENDING);
        order.setOrderStatus(AuctionOrderStatus.ABANDONED);
        order.setAbandonedAt(candidateAt);
        orders.saveAndFlush(order);
        assertFalse(orders.findPaymentDeadlineCandidateIds(candidateAt).contains(orderId));
    }

    @Test
    void paymentHoldingRowLockWinsAndDeadlineBecomesNoOp() throws Exception {
        CountDownLatch paymentReached = new CountDownLatch(1);
        release = new CountDownLatch(1);
        AtomicBoolean first = new AtomicBoolean(true);
        when(time.now()).thenAnswer(call -> {
            if (first.compareAndSet(true, false)) {
                paymentReached.countDown();
                assertTrue(release.await(10, TimeUnit.SECONDS));
            }
            return PAYMENT_AT;
        });
        executor = Executors.newFixedThreadPool(2);

        Future<AuctionOrderPaymentResponse> payment = executor.submit(
                () -> payments.pay(orderNumber, buyerId));
        assertTrue(paymentReached.await(10, TimeUnit.SECONDS));

        CountDownLatch deadlineStarted = new CountDownLatch(1);
        Future<?> deadline = executor.submit(() -> {
            deadlineStarted.countDown();
            deadlines.expireOverdueOrder(orderId);
        });
        assertTrue(deadlineStarted.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> deadline.get(200, TimeUnit.MILLISECONDS));

        release.countDown();
        assertEquals(AuctionPaymentStatus.PAID, payment.get(20, TimeUnit.SECONDS).paymentStatus());
        deadline.get(20, TimeUnit.SECONDS);

        AuctionOrder persisted = orders.findById(orderId).orElseThrow();
        assertEquals(AuctionPaymentStatus.PAID, persisted.getPaymentStatus());
        assertEquals(AuctionOrderStatus.PAID, persisted.getOrderStatus());
        assertNull(persisted.getAbandonedAt());
        verify(time).now();
    }

    @Test
    void deadlineHoldingRowLockWinsAndLaterPaymentCannotReviveOrder() throws Exception {
        AuctionOrder order = orders.findById(orderId).orElseThrow();
        order.setPaymentDueAt(PAYMENT_AT);
        orders.saveAndFlush(order);

        CountDownLatch deadlineReached = new CountDownLatch(1);
        release = new CountDownLatch(1);
        AtomicBoolean first = new AtomicBoolean(true);
        when(time.now()).thenAnswer(call -> {
            if (first.compareAndSet(true, false)) {
                deadlineReached.countDown();
                assertTrue(release.await(10, TimeUnit.SECONDS));
            }
            return PAYMENT_AT;
        });
        executor = Executors.newFixedThreadPool(2);

        Future<?> deadline = executor.submit(() -> deadlines.expireOverdueOrder(orderId));
        assertTrue(deadlineReached.await(10, TimeUnit.SECONDS));

        CountDownLatch paymentStarted = new CountDownLatch(1);
        Future<AuctionOrderPaymentResponse> payment = executor.submit(() -> {
            paymentStarted.countDown();
            return payments.pay(orderNumber, buyerId);
        });
        assertTrue(paymentStarted.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> payment.get(200, TimeUnit.MILLISECONDS));

        release.countDown();
        deadline.get(20, TimeUnit.SECONDS);
        ExecutionException paymentFailure = assertThrows(ExecutionException.class,
                () -> payment.get(20, TimeUnit.SECONDS));
        assertInstanceOf(AuctionConflictException.class, paymentFailure.getCause());

        AuctionOrder persisted = orders.findById(orderId).orElseThrow();
        assertEquals(AuctionPaymentStatus.PENDING, persisted.getPaymentStatus());
        assertEquals(AuctionOrderStatus.ABANDONED, persisted.getOrderStatus());
        assertEquals(PAYMENT_AT, persisted.getAbandonedAt());
        assertNull(persisted.getPaidAt());
        verify(time).now();
    }

    private Long createMember(String name) {
        String token = UUID.randomUUID().toString();
        Member member = new Member();
        member.setAccount("pay-" + token);
        member.setEmail(token + "@example.invalid");
        member.setPasswordHash("integration-fixture-not-a-login");
        member.setName(name);
        member.setPhone("0912345678");
        member.setAddress("台北市測試路 1 號");
        return members.saveAndFlush(member).getMemberId();
    }

    private void createCommittedPendingOrder() {
        AuctionProduct product = new AuctionProduct();
        product.setSellerId(sellerId);
        product.setBrandId(1L);
        product.setCategoryId(1L);
        product.setProductName("Payment lock test shoe");
        product.setModel("PAY-LOCK");
        product.setSize("US 9");
        product.setCondition(AuctionCondition.NEW);
        product.setDescription("PESSIMISTIC_WRITE payment integration fixture");
        product.setLocation("台北市");
        product.setShippingMethod("宅配");
        product.setShippingFee(new BigDecimal("100.00"));
        product.setPaymentMethod("轉帳");
        product.setAllowMeetup(false);
        product.setAllowReturn(false);
        product.setCreatedAt(CREATED_AT);
        product.setUpdatedAt(CREATED_AT);
        product = products.saveAndFlush(product);
        productId = product.getAuctionProductId();

        Auction auction = new Auction();
        auction.setAuctionProductId(productId);
        auction.setStartPrice(new BigDecimal("6000.00"));
        auction.setCurrentPrice(new BigDecimal("6800.00"));
        auction.setMinIncrement(new BigDecimal("100.00"));
        auction.setStartAt(CREATED_AT.minusHours(2));
        auction.setEndAt(CREATED_AT.minusHours(1));
        auction.setStatus(AuctionStatus.SOLD);
        auction.setHighestBidderId(buyerId);
        auction.setBidCount(2);
        auction.setCreatedAt(CREATED_AT.minusHours(3));
        auction.setUpdatedAt(CREATED_AT.minusHours(1));
        auction = auctions.saveAndFlush(auction);
        auctionId = auction.getAuctionId();

        orderNumber = "AO-PAY-" + UUID.randomUUID().toString().substring(0, 12);
        AuctionOrder order = new AuctionOrder();
        order.setOrderNumber(orderNumber);
        order.setAuctionId(auctionId);
        order.setBuyerId(buyerId);
        order.setSellerId(sellerId);
        order.setFinalPrice(new BigDecimal("6800.00"));
        order.setPaymentMethod("轉帳");
        order.setPaymentStatus(AuctionPaymentStatus.PENDING);
        order.setShippingStatus(AuctionShippingStatus.NOT_SHIPPED);
        order.setOrderStatus(AuctionOrderStatus.PENDING_PAYMENT);
        order.setReceiverName("付款買家");
        order.setReceiverPhone("0912345678");
        order.setShippingAddress("台北市測試路 1 號");
        order.setCreatedAt(CREATED_AT);
        order.setUpdatedAt(CREATED_AT);
        order.setPaymentDueAt(PAYMENT_DUE_AT);
        orderId = orders.saveAndFlush(order).getAuctionOrderId();
    }
}
