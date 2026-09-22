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
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.project.sneakermarket.auction.dto.AuctionOrderCheckoutUpdateRequest;
import com.project.sneakermarket.auction.entity.AuctionOrder;
import com.project.sneakermarket.auction.entity.AuctionOrderStatus;
import com.project.sneakermarket.auction.entity.AuctionPaymentStatus;
import com.project.sneakermarket.auction.entity.AuctionShippingStatus;
import com.project.sneakermarket.auction.exception.AuctionConflictException;
import com.project.sneakermarket.auction.exception.AuctionUnauthorizedException;
import com.project.sneakermarket.auction.repository.AuctionOrderRepository;
import com.project.sneakermarket.common.exception.BusinessException;
import com.project.sneakermarket.common.exception.ForbiddenException;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;

class AuctionOrderCheckoutServiceTests {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 20, 12, 0);

    private AuctionOrderRepository orders;
    private AuctionTime time;
    private AuctionOrderCheckoutService service;
    private AuctionOrder order;

    @BeforeEach
    void setup() {
        orders = mock(AuctionOrderRepository.class);
        time = mock(AuctionTime.class);
        service = new AuctionOrderCheckoutService(orders, time);
        order = pendingOrder();
        when(time.now()).thenReturn(NOW);
        when(orders.findByOrderNumberForUpdate("AO-CHECKOUT-001"))
                .thenReturn(Optional.of(order));
    }

    @Test
    void buyerUpdatesTrimmedReceiverSnapshotUnderTheExistingWriteLock() {
        BigDecimal originalPrice = order.getFinalPrice();
        String originalPaymentMethod = order.getPaymentMethod();
        LocalDateTime originalDeadline = order.getPaymentDueAt();

        var response = service.update("AO-CHECKOUT-001", 10L,
                new AuctionOrderCheckoutUpdateRequest(
                        "  新收件人  ", "  0987654321  ", "  台中市新地址  "));

        assertEquals("新收件人", order.getReceiverName());
        assertEquals("0987654321", order.getReceiverPhone());
        assertEquals("台中市新地址", order.getShippingAddress());
        assertEquals(NOW, order.getUpdatedAt());
        assertEquals(originalPrice, order.getFinalPrice());
        assertEquals(originalPaymentMethod, order.getPaymentMethod());
        assertEquals(originalDeadline, order.getPaymentDueAt());
        assertEquals("AO-CHECKOUT-001", response.orderNumber());
        assertEquals("新收件人", response.receiverName());
        assertEquals(NOW, response.updatedAt());
        verify(orders).findByOrderNumberForUpdate("AO-CHECKOUT-001");
        verify(orders).save(order);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    void invalidMemberIsUnauthorizedBeforeOrderLookup(Long memberId) {
        assertThrows(AuctionUnauthorizedException.class,
                () -> service.update("AO-CHECKOUT-001", memberId, validRequest()));
        verifyNoInteractions(orders);
    }

    @Test
    void missingOrderIs404() {
        when(orders.findByOrderNumberForUpdate("AO-MISSING")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.update("AO-MISSING", 10L, validRequest()));
        verify(orders, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(longs = {20L, 30L})
    void sellerAndOtherMemberAreForbidden(Long memberId) {
        assertThrows(ForbiddenException.class,
                () -> service.update("AO-CHECKOUT-001", memberId, validRequest()));
        verify(orders, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = AuctionOrderStatus.class, names = {
            "PAID", "SHIPPED", "COMPLETED", "CANCELLED", "ABANDONED"
    })
    void nonPendingOrderStatesAre409(AuctionOrderStatus status) {
        order.setOrderStatus(status);
        assertThrows(AuctionConflictException.class,
                () -> service.update("AO-CHECKOUT-001", 10L, validRequest()));
        verify(orders, never()).save(any());
    }

    @Test
    void nonPendingPaymentStatusIs409() {
        order.setPaymentStatus(AuctionPaymentStatus.PAID);
        assertThrows(AuctionConflictException.class,
                () -> service.update("AO-CHECKOUT-001", 10L, validRequest()));
        verify(orders, never()).save(any());
    }

    @Test
    void deadlineEqualToNowIs409() {
        order.setPaymentDueAt(NOW);
        assertThrows(AuctionConflictException.class,
                () -> service.update("AO-CHECKOUT-001", 10L, validRequest()));
        verify(orders, never()).save(any());
    }

    @Test
    void deadlineBeforeNowIs409() {
        order.setPaymentDueAt(NOW.minusSeconds(1));
        assertThrows(AuctionConflictException.class,
                () -> service.update("AO-CHECKOUT-001", 10L, validRequest()));
        verify(orders, never()).save(any());
    }

    @Test
    void missingDeadlineIs409() {
        order.setPaymentDueAt(null);
        assertThrows(AuctionConflictException.class,
                () -> service.update("AO-CHECKOUT-001", 10L, validRequest()));
        verify(orders, never()).save(any());
    }

    @Test
    void serviceDefensivelyRejectsBlankTrimmedFields() {
        assertThrows(BusinessException.class,
                () -> service.update("AO-CHECKOUT-001", 10L,
                        new AuctionOrderCheckoutUpdateRequest("   ", "0912", "地址")));
        verify(orders, never()).save(any());
    }

    private AuctionOrderCheckoutUpdateRequest validRequest() {
        return new AuctionOrderCheckoutUpdateRequest("收件人", "0912345678", "台北市測試路 1 號");
    }

    private AuctionOrder pendingOrder() {
        AuctionOrder value = new AuctionOrder();
        value.setOrderNumber("AO-CHECKOUT-001");
        value.setAuctionId(7L);
        value.setBuyerId(10L);
        value.setSellerId(20L);
        value.setFinalPrice(new BigDecimal("6500"));
        value.setPaymentMethod("信用卡");
        value.setPaymentStatus(AuctionPaymentStatus.PENDING);
        value.setShippingStatus(AuctionShippingStatus.NOT_SHIPPED);
        value.setOrderStatus(AuctionOrderStatus.PENDING_PAYMENT);
        value.setReceiverName("原收件人");
        value.setReceiverPhone("0911111111");
        value.setShippingAddress("原地址");
        value.setCreatedAt(NOW.minusHours(2));
        value.setUpdatedAt(NOW.minusHours(2));
        value.setPaymentDueAt(NOW.plusHours(1));
        return value;
    }
}
