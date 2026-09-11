package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import com.project.sneakermarket.auction.entity.*;
import com.project.sneakermarket.auction.repository.*;
import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.member.repository.MemberRepository;

class AuctionOrderServiceTests {
    private AuctionResultRepository results;
    private AuctionOrderRepository orders;
    private AuctionRepository auctions;
    private AuctionProductRepository products;
    private MemberRepository members;
    private AuctionOrderService service;
    private AuctionResult result;
    private Auction auction;
    private AuctionProduct product;
    private Member buyer;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 10, 12, 0);

    @BeforeEach
    void setup() {
        results = mock(AuctionResultRepository.class);
        orders = mock(AuctionOrderRepository.class);
        auctions = mock(AuctionRepository.class);
        products = mock(AuctionProductRepository.class);
        members = mock(MemberRepository.class);
        service = new AuctionOrderService(results, orders, auctions, products, members,
                new AuctionTime(Clock.fixed(Instant.parse("2026-09-10T04:00:00Z"), ZoneOffset.UTC)),
                mock(org.springframework.context.ApplicationEventPublisher.class));
        result = new AuctionResult();
        result.setAuctionId(1L);
        result.setWinnerId(2L);
        result.setFinalPrice(new BigDecimal("6000"));
        result.setResultStatus(AuctionResultStatus.SOLD);
        auction = new Auction();
        auction.setAuctionId(1L);
        auction.setAuctionProductId(4L);
        auction.setStatus(AuctionStatus.SOLD);
        auction.setHighestBidderId(2L);
        auction.setCurrentPrice(new BigDecimal("6000.00"));
        product = new AuctionProduct();
        product.setSellerId(3L);
        product.setPaymentMethod("銀行轉帳");
        buyer = new Member();
        buyer.setName("得標會員");
        buyer.setPhone("0912345678");
        buyer.setAddress("台北市測試路 1 號");
        when(results.findByAuctionIdForUpdate(1L)).thenReturn(Optional.of(result));
        when(auctions.findById(1L)).thenReturn(Optional.of(auction));
        when(products.findById(4L)).thenReturn(Optional.of(product));
        when(members.existsById(3L)).thenReturn(true);
        when(members.findById(2L)).thenReturn(Optional.of(buyer));
    }

    @Test
    void soldUsesResultAndMemberSnapshotAndComparesPriceWithoutScale() {
        service.createOrderForAuction(1L);
        var captured = ArgumentCaptor.forClass(AuctionOrder.class);
        verify(orders).saveAndFlush(captured.capture());
        AuctionOrder order = captured.getValue();
        assertEquals(1L, order.getAuctionId());
        assertEquals(2L, order.getBuyerId());
        assertEquals(3L, order.getSellerId());
        assertSame(result.getFinalPrice(), order.getFinalPrice());
        assertEquals(product.getPaymentMethod(), order.getPaymentMethod());
        assertEquals(buyer.getName(), order.getReceiverName());
        assertEquals(buyer.getPhone(), order.getReceiverPhone());
        assertEquals(buyer.getAddress(), order.getShippingAddress());
        assertEquals(AuctionPaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals(AuctionShippingStatus.NOT_SHIPPED, order.getShippingStatus());
        assertEquals(AuctionOrderStatus.PENDING_PAYMENT, order.getOrderStatus());
        assertEquals(NOW, order.getCreatedAt());
        assertEquals(NOW, order.getUpdatedAt());
        assertNull(order.getPaidAt());
        assertNull(order.getShippedAt());
        assertNull(order.getCompletedAt());
        var sequence = inOrder(results, orders);
        sequence.verify(results).findByAuctionIdForUpdate(1L);
        sequence.verify(orders).existsByAuctionId(1L);
        sequence.verify(orders).saveAndFlush(any());
        verify(auctions, never()).save(any());
        verify(results, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = AuctionResultStatus.class, names = {"UNSOLD", "CANCELLED", "REMOVED"})
    void nonSoldResultDoesNotCreate(AuctionResultStatus status) {
        result.setResultStatus(status);
        service.createOrderForAuction(1L);
        verify(orders, never()).saveAndFlush(any());
        verifyNoInteractions(auctions, products, members);
    }

    @Test
    void missingResultDoesNotCreate() {
        when(results.findByAuctionIdForUpdate(1L)).thenReturn(Optional.empty());
        service.createOrderForAuction(1L);
        verifyNoInteractions(orders, auctions, products, members);
    }

    @Test
    void existingOrderReturnsAfterLockWithoutRevalidatingChangedMember() {
        when(orders.existsByAuctionId(1L)).thenReturn(true);
        service.createOrderForAuction(1L);
        verify(orders, never()).saveAndFlush(any());
        verifyNoInteractions(auctions, products, members);
    }

    @Test
    void missingWinnerIdIsRejected() {
        result.setWinnerId(null);
        rejected();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"0", "-1"})
    void invalidPriceIsRejected(String value) {
        result.setFinalPrice(value == null || value.isEmpty() ? null : new BigDecimal(value));
        rejected();
    }

    @Test
    void inconsistentWinnerIsRejected() {
        auction.setHighestBidderId(99L);
        rejected();
    }

    @Test
    void inconsistentPriceIsRejected() {
        auction.setCurrentPrice(new BigDecimal("6500"));
        rejected();
    }

    @ParameterizedTest
    @EnumSource(value = AuctionStatus.class, names = {"SOLD"}, mode = EnumSource.Mode.EXCLUDE)
    void auctionMustBeSold(AuctionStatus status) {
        auction.setStatus(status);
        rejected();
    }

    @Test
    void missingWinnerMemberIsRejected() {
        when(members.findById(2L)).thenReturn(Optional.empty());
        rejected();
    }

    @Test
    void missingSellerMemberIsRejected() {
        when(members.existsById(3L)).thenReturn(false);
        rejected();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void incompleteReceiverIsRejected(String value) {
        buyer.setName(value);
        rejected();
        buyer.setName("得標會員");
        buyer.setPhone(value);
        rejected();
        buyer.setPhone("0912345678");
        buyer.setAddress(value);
        rejected();
    }

    @Test
    void blankPaymentMethodIsRejected() {
        product.setPaymentMethod(" ");
        rejected();
    }

    @Test
    void orderNumbersAreNonemptyWithinLimitAndUnique() {
        var numbers = new HashSet<String>();
        doAnswer(call -> {
            AuctionOrder order = call.getArgument(0);
            assertFalse(order.getOrderNumber().isBlank());
            assertTrue(order.getOrderNumber().length() <= 40);
            assertTrue(numbers.add(order.getOrderNumber()));
            return order;
        }).when(orders).saveAndFlush(any());
        for (int i = 0; i < 1000; i++) service.createOrderForAuction(1L);
        assertEquals(1000, numbers.size());
    }

    private void rejected() {
        // 記下主要業務欄位，證明任何拒絕都沒有偷偷改寫正式來源。
        var winner = result.getWinnerId();
        var price = result.getFinalPrice();
        var status = auction.getStatus();
        assertThrows(IllegalStateException.class, () -> service.createOrderForAuction(1L));
        assertEquals(winner, result.getWinnerId());
        assertEquals(price, result.getFinalPrice());
        assertEquals(status, auction.getStatus());
        verify(orders, never()).saveAndFlush(any());
        verify(results, never()).save(any());
        verify(auctions, never()).save(any());
    }
}
