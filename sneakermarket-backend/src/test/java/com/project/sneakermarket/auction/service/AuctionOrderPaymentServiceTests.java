package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.project.sneakermarket.auction.entity.AuctionOrder;
import com.project.sneakermarket.auction.entity.AuctionOrderStatus;
import com.project.sneakermarket.auction.entity.AuctionPaymentStatus;
import com.project.sneakermarket.auction.entity.AuctionShippingStatus;
import com.project.sneakermarket.auction.exception.AuctionConflictException;
import com.project.sneakermarket.auction.exception.AuctionUnauthorizedException;
import com.project.sneakermarket.auction.repository.AuctionOrderRepository;
import com.project.sneakermarket.common.exception.ForbiddenException;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;

class AuctionOrderPaymentServiceTests {
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 9, 14, 9, 0);
    private static final LocalDateTime PAYMENT_AT = LocalDateTime.of(2026, 9, 14, 12, 30);

    private AuctionOrderRepository orders;
    private AuctionTime time;
    private AuctionOrderPaymentService service;
    private AuctionOrder order;

    @BeforeEach
    void setup() {
        orders = mock(AuctionOrderRepository.class);
        time = mock(AuctionTime.class);
        service = new AuctionOrderPaymentService(orders, time);
        order = pendingOrder();
        when(orders.findByOrderNumberForUpdate("AO-PAY-001")).thenReturn(Optional.of(order));
    }

    @Test
    void buyerPaysPendingOrderAndOnlyPaymentStateChanges() {
        when(time.now()).thenReturn(PAYMENT_AT);
        var response = service.pay("AO-PAY-001", 10L);

        assertEquals(AuctionPaymentStatus.PAID, order.getPaymentStatus());
        assertEquals(AuctionOrderStatus.PAID, order.getOrderStatus());
        assertEquals(PAYMENT_AT, order.getPaidAt());
        assertEquals(PAYMENT_AT, order.getUpdatedAt());
        assertEquals(AuctionShippingStatus.NOT_SHIPPED, order.getShippingStatus());
        assertNull(order.getShippedAt());
        assertNull(order.getCompletedAt());
        assertEquals("轉帳", order.getPaymentMethod());
        assertEquals(0, new BigDecimal("6800.00").compareTo(order.getFinalPrice()));

        assertEquals("AO-PAY-001", response.orderNumber());
        assertEquals(7L, response.auctionId());
        assertEquals("轉帳", response.paymentMethod());
        assertEquals(AuctionPaymentStatus.PAID, response.paymentStatus());
        assertEquals(AuctionOrderStatus.PAID, response.orderStatus());
        assertEquals(PAYMENT_AT, response.paidAt());
        assertEquals(PAYMENT_AT, response.updatedAt());
        verify(orders).save(order);
        verify(time).now();
    }

    @Test
    void buyerPaysImmediatelyBeforeDeadline() {
        order.setPaymentDueAt(PAYMENT_AT.plusNanos(1));
        when(time.now()).thenReturn(PAYMENT_AT);

        assertEquals(AuctionPaymentStatus.PAID,
                service.pay("AO-PAY-001", 10L).paymentStatus());
        assertEquals(AuctionOrderStatus.PAID, order.getOrderStatus());
    }

    @Test
    void exactDeadlineIsConflictAndCannotBecomePaid() {
        order.setPaymentDueAt(PAYMENT_AT);
        when(time.now()).thenReturn(PAYMENT_AT);
        var before = snapshot(order);

        AuctionConflictException error = assertThrows(AuctionConflictException.class,
                () -> service.pay("AO-PAY-001", 10L));

        assertEquals("競標訂單付款期限已過", error.getMessage());
        assertEquals(before, snapshot(order));
        verify(orders, never()).save(any());
        verify(time).now();
    }

    @Test
    void afterDeadlineIsConflictAndCannotBecomePaid() {
        order.setPaymentDueAt(PAYMENT_AT.minusSeconds(1));
        when(time.now()).thenReturn(PAYMENT_AT);
        var before = snapshot(order);

        AuctionConflictException error = assertThrows(AuctionConflictException.class,
                () -> service.pay("AO-PAY-001", 10L));

        assertEquals("競標訂單付款期限已過", error.getMessage());
        assertEquals(before, snapshot(order));
        verify(orders, never()).save(any());
    }

    @Test
    void legacyNullDeadlineKeepsExistingPaymentBehavior() {
        order.setPaymentDueAt(null);
        when(time.now()).thenReturn(PAYMENT_AT);

        var response = service.pay("AO-PAY-001", 10L);

        assertEquals(AuctionPaymentStatus.PAID, response.paymentStatus());
        assertEquals(PAYMENT_AT, response.paidAt());
    }

    @Test
    void secondCallAfterSuccessfulPaymentKeepsOriginalTimestamps() {
        when(time.now()).thenReturn(PAYMENT_AT, PAYMENT_AT.plusMinutes(5));

        service.pay("AO-PAY-001", 10L);
        LocalDateTime firstPaidAt = order.getPaidAt();
        LocalDateTime firstUpdatedAt = order.getUpdatedAt();
        var repeated = service.pay("AO-PAY-001", 10L);

        assertEquals(PAYMENT_AT, firstPaidAt);
        assertEquals(firstPaidAt, order.getPaidAt());
        assertEquals(firstUpdatedAt, order.getUpdatedAt());
        assertEquals(firstPaidAt, repeated.paidAt());
        assertEquals(firstUpdatedAt, repeated.updatedAt());
        verify(time, times(1)).now();
        verify(orders, times(1)).save(order);
    }

    @Test
    void missingOrInvalidMemberIsUnauthorizedBeforeLocking() {
        assertThrows(AuctionUnauthorizedException.class, () -> service.pay("AO-PAY-001", null));
        assertThrows(AuctionUnauthorizedException.class, () -> service.pay("AO-PAY-001", 0L));
        verifyNoInteractions(orders, time);
    }

    @Test
    void differentBuyerIsForbiddenAndLeavesOrderUnchanged() {
        var before = snapshot(order);
        assertThrows(ForbiddenException.class, () -> service.pay("AO-PAY-001", 20L));
        assertEquals(before, snapshot(order));
        verify(orders, never()).save(any());
        verifyNoInteractions(time);
    }

    @Test
    void missingOrderIsNotFound() {
        when(orders.findByOrderNumberForUpdate("AO-MISSING")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.pay("AO-MISSING", 10L));
        verify(orders, never()).save(any());
        verifyNoInteractions(time);
    }

    @Test
    void failedPaymentIsConflictAndCannotBecomePaid() {
        order.setPaymentStatus(AuctionPaymentStatus.FAILED);
        var before = snapshot(order);
        assertThrows(AuctionConflictException.class, () -> service.pay("AO-PAY-001", 10L));
        assertEquals(before, snapshot(order));
        verify(orders, never()).save(any());
        verifyNoInteractions(time);
    }

    @Test
    void cancelledOrderIsConflict() {
        order.setOrderStatus(AuctionOrderStatus.CANCELLED);
        var before = snapshot(order);
        assertThrows(AuctionConflictException.class, () -> service.pay("AO-PAY-001", 10L));
        assertEquals(before, snapshot(order));
        verify(orders, never()).save(any());
        verifyNoInteractions(time);
    }

    @Test
    void abandonedOrderIsConflict() {
        order.setOrderStatus(AuctionOrderStatus.ABANDONED);
        order.setAbandonedAt(PAYMENT_AT);
        var before = snapshot(order);
        assertThrows(AuctionConflictException.class, () -> service.pay("AO-PAY-001", 10L));
        assertEquals(before, snapshot(order));
        verify(orders, never()).save(any());
        verifyNoInteractions(time);
    }

    @Test
    void inconsistentPendingPaymentAndShippedOrderIsConflict() {
        order.setOrderStatus(AuctionOrderStatus.SHIPPED);
        var before = snapshot(order);
        assertThrows(AuctionConflictException.class, () -> service.pay("AO-PAY-001", 10L));
        assertEquals(before, snapshot(order));
        verify(orders, never()).save(any());
        verifyNoInteractions(time);
    }

    @Test
    void inconsistentPaidPaymentAndPendingOrderIsConflict() {
        order.setPaymentStatus(AuctionPaymentStatus.PAID);
        order.setPaidAt(PAYMENT_AT.minusHours(1));
        var before = snapshot(order);
        assertThrows(AuctionConflictException.class, () -> service.pay("AO-PAY-001", 10L));
        assertEquals(before, snapshot(order));
        verify(orders, never()).save(any());
        verifyNoInteractions(time);
    }

    @ParameterizedTest
    @EnumSource(value = AuctionOrderStatus.class, names = {"PAID", "SHIPPED", "COMPLETED"})
    void alreadyPaidLegalOrderStateIsIdempotent(AuctionOrderStatus state) {
        LocalDateTime originalPaidAt = PAYMENT_AT.minusHours(1);
        LocalDateTime originalUpdatedAt = PAYMENT_AT.minusMinutes(30);
        order.setPaymentStatus(AuctionPaymentStatus.PAID);
        order.setOrderStatus(state);
        order.setPaidAt(originalPaidAt);
        order.setUpdatedAt(originalUpdatedAt);
        order.setPaymentDueAt(PAYMENT_AT.minusDays(1));

        var response = service.pay("AO-PAY-001", 10L);

        assertEquals(AuctionPaymentStatus.PAID, response.paymentStatus());
        assertEquals(state, response.orderStatus());
        assertEquals(originalPaidAt, response.paidAt());
        assertEquals(originalUpdatedAt, response.updatedAt());
        assertEquals(originalPaidAt, order.getPaidAt());
        assertEquals(originalUpdatedAt, order.getUpdatedAt());
        verify(orders, never()).save(any());
        verifyNoInteractions(time);
    }

    private AuctionOrder pendingOrder() {
        AuctionOrder value = new AuctionOrder();
        value.setOrderNumber("AO-PAY-001");
        value.setAuctionId(7L);
        value.setBuyerId(10L);
        value.setSellerId(11L);
        value.setFinalPrice(new BigDecimal("6800.00"));
        value.setPaymentMethod("轉帳");
        value.setPaymentStatus(AuctionPaymentStatus.PENDING);
        value.setShippingStatus(AuctionShippingStatus.NOT_SHIPPED);
        value.setOrderStatus(AuctionOrderStatus.PENDING_PAYMENT);
        value.setReceiverName("買家");
        value.setReceiverPhone("0912345678");
        value.setShippingAddress("台北市測試路 1 號");
        value.setCreatedAt(CREATED_AT);
        value.setUpdatedAt(CREATED_AT);
        value.setPaymentDueAt(PAYMENT_AT.plusMinutes(1));
        return value;
    }

    private PaymentSnapshot snapshot(AuctionOrder value) {
        return new PaymentSnapshot(
                value.getPaymentMethod(), value.getPaymentStatus(), value.getOrderStatus(),
                value.getShippingStatus(), value.getPaidAt(), value.getUpdatedAt(),
                value.getShippedAt(), value.getCompletedAt(), value.getPaymentDueAt(),
                value.getAbandonedAt(), value.getFinalPrice());
    }

    private record PaymentSnapshot(
            String paymentMethod,
            AuctionPaymentStatus paymentStatus,
            AuctionOrderStatus orderStatus,
            AuctionShippingStatus shippingStatus,
            LocalDateTime paidAt,
            LocalDateTime updatedAt,
            LocalDateTime shippedAt,
            LocalDateTime completedAt,
            LocalDateTime paymentDueAt,
            LocalDateTime abandonedAt,
            BigDecimal finalPrice) {
    }
}
