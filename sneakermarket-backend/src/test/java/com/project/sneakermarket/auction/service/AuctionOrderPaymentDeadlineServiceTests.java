package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.ApplicationEventPublisher;

import com.project.sneakermarket.auction.email.AuctionEmailReadyEvent;
import com.project.sneakermarket.auction.email.AuctionEmailService;
import com.project.sneakermarket.auction.entity.AuctionOrder;
import com.project.sneakermarket.auction.entity.AuctionOrderStatus;
import com.project.sneakermarket.auction.entity.AuctionPaymentStatus;
import com.project.sneakermarket.auction.entity.AuctionShippingStatus;
import com.project.sneakermarket.auction.notification.AuctionPrivateNotificationEventPublisher;
import com.project.sneakermarket.auction.repository.AuctionOrderRepository;

class AuctionOrderPaymentDeadlineServiceTests {
    private static final LocalDateTime DUE_AT = LocalDateTime.of(2026, 9, 16, 20, 0);

    private AuctionOrderRepository orders;
    private AuctionTime time;
    private AuctionEmailService emails;
    private AuctionPrivateNotificationEventPublisher privateNotificationEvents;
    private ApplicationEventPublisher events;
    private AuctionOrderPaymentDeadlineService service;
    private AuctionOrder order;

    @BeforeEach
    void setup() {
        orders = mock(AuctionOrderRepository.class);
        time = mock(AuctionTime.class);
        emails = mock(AuctionEmailService.class);
        privateNotificationEvents = mock(AuctionPrivateNotificationEventPublisher.class);
        events = mock(ApplicationEventPublisher.class);
        service = new AuctionOrderPaymentDeadlineService(
                orders, time, emails, privateNotificationEvents, events);
        order = pendingOrder();
        when(orders.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
    }

    @Test
    void exactDeadlineTransitionsToAbandonedAndQueuesEmailsOnce() {
        when(time.now()).thenReturn(DUE_AT);

        service.expireOverdueOrder(1L);

        assertEquals(AuctionPaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals(AuctionOrderStatus.ABANDONED, order.getOrderStatus());
        assertEquals(AuctionShippingStatus.NOT_SHIPPED, order.getShippingStatus());
        assertEquals(DUE_AT, order.getAbandonedAt());
        assertEquals(DUE_AT, order.getUpdatedAt());
        assertNull(order.getPaidAt());
        assertNull(order.getShippedAt());
        assertNull(order.getCompletedAt());
        var sequence = inOrder(orders, emails, privateNotificationEvents, events);
        sequence.verify(orders).save(order);
        sequence.verify(emails).ensureAbandonedEmailLogs(order);
        sequence.verify(privateNotificationEvents).publishOrderAbandoned(
                7L, 1L, "AO-DEADLINE-001", 10L, 11L, DUE_AT);
        sequence.verify(events).publishEvent(new AuctionEmailReadyEvent(7L));
    }

    @Test
    void overdueTransitionsToAbandoned() {
        LocalDateTime expiredAt = DUE_AT.plusSeconds(1);
        when(time.now()).thenReturn(expiredAt);

        service.expireOverdueOrder(1L);

        assertEquals(AuctionOrderStatus.ABANDONED, order.getOrderStatus());
        assertEquals(expiredAt, order.getAbandonedAt());
        assertEquals(expiredAt, order.getUpdatedAt());
        verify(privateNotificationEvents).publishOrderAbandoned(
                7L, 1L, "AO-DEADLINE-001", 10L, 11L, expiredAt);
    }

    @Test
    void beforeDeadlineIsNoOp() {
        when(time.now()).thenReturn(DUE_AT.minusNanos(1));

        service.expireOverdueOrder(1L);

        assertEquals(AuctionOrderStatus.PENDING_PAYMENT, order.getOrderStatus());
        assertNull(order.getAbandonedAt());
        verify(orders, never()).save(any());
        verifyNoInteractions(emails, privateNotificationEvents, events);
    }

    @ParameterizedTest
    @EnumSource(value = AuctionOrderStatus.class,
            names = {"PAID", "SHIPPED", "COMPLETED", "CANCELLED", "ABANDONED"})
    void nonPendingOrderStateIsNoOp(AuctionOrderStatus status) {
        order.setOrderStatus(status);
        if (status == AuctionOrderStatus.PAID
                || status == AuctionOrderStatus.SHIPPED
                || status == AuctionOrderStatus.COMPLETED) {
            order.setPaymentStatus(AuctionPaymentStatus.PAID);
        }

        service.expireOverdueOrder(1L);

        assertEquals(status, order.getOrderStatus());
        verify(orders, never()).save(any());
        verifyNoInteractions(time, emails, privateNotificationEvents, events);
    }

    @ParameterizedTest
    @EnumSource(value = AuctionPaymentStatus.class, names = {"PAID", "FAILED"})
    void nonPendingPaymentStateIsNoOp(AuctionPaymentStatus status) {
        order.setPaymentStatus(status);

        service.expireOverdueOrder(1L);

        assertEquals(AuctionOrderStatus.PENDING_PAYMENT, order.getOrderStatus());
        verify(orders, never()).save(any());
        verifyNoInteractions(time, emails, privateNotificationEvents, events);
    }

    @Test
    void legacyNullDeadlineIsNoOp() {
        order.setPaymentDueAt(null);

        service.expireOverdueOrder(1L);

        assertEquals(AuctionOrderStatus.PENDING_PAYMENT, order.getOrderStatus());
        verify(orders, never()).save(any());
        verifyNoInteractions(time, emails, privateNotificationEvents, events);
    }

    @Test
    void missingOrderIsNoOp() {
        when(orders.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        service.expireOverdueOrder(99L);

        verify(orders, never()).save(any());
        verifyNoInteractions(time, emails, privateNotificationEvents, events);
    }

    @Test
    void orderSaveFailureDoesNotPublishAbandonedEvent() {
        when(time.now()).thenReturn(DUE_AT);
        doThrow(new IllegalStateException("save failed")).when(orders).save(order);

        assertThrows(IllegalStateException.class, () -> service.expireOverdueOrder(1L));

        verifyNoInteractions(emails, privateNotificationEvents, events);
    }

    @Test
    void abandonedEmailLogFailureDoesNotPublishAbandonedEvent() {
        when(time.now()).thenReturn(DUE_AT);
        doThrow(new IllegalStateException("email log failed"))
                .when(emails).ensureAbandonedEmailLogs(order);

        assertThrows(IllegalStateException.class, () -> service.expireOverdueOrder(1L));

        verify(orders).save(order);
        verifyNoInteractions(privateNotificationEvents, events);
    }

    @Test
    void repeatedExecutionOnlyTransitionsQueuesAndPublishesOnce() {
        when(time.now()).thenReturn(DUE_AT);

        service.expireOverdueOrder(1L);
        service.expireOverdueOrder(1L);

        assertEquals(AuctionOrderStatus.ABANDONED, order.getOrderStatus());
        verify(orders, times(2)).findByIdForUpdate(1L);
        verify(orders).save(order);
        verify(time).now();
        verify(emails).ensureAbandonedEmailLogs(order);
        verify(privateNotificationEvents).publishOrderAbandoned(
                7L, 1L, "AO-DEADLINE-001", 10L, 11L, DUE_AT);
        verify(events).publishEvent(new AuctionEmailReadyEvent(7L));
    }

    private AuctionOrder pendingOrder() {
        AuctionOrder value = new AuctionOrder();
        value.setAuctionOrderId(1L);
        value.setOrderNumber("AO-DEADLINE-001");
        value.setAuctionId(7L);
        value.setBuyerId(10L);
        value.setSellerId(11L);
        value.setPaymentStatus(AuctionPaymentStatus.PENDING);
        value.setShippingStatus(AuctionShippingStatus.NOT_SHIPPED);
        value.setOrderStatus(AuctionOrderStatus.PENDING_PAYMENT);
        value.setPaymentDueAt(DUE_AT);
        value.setUpdatedAt(DUE_AT.minusDays(1));
        return value;
    }
}
