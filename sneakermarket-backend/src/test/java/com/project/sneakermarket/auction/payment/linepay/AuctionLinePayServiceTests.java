package com.project.sneakermarket.auction.payment.linepay;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionLinePayTransaction;
import com.project.sneakermarket.auction.entity.AuctionLinePayTransactionStatus;
import com.project.sneakermarket.auction.entity.AuctionOrder;
import com.project.sneakermarket.auction.entity.AuctionOrderStatus;
import com.project.sneakermarket.auction.entity.AuctionPaymentStatus;
import com.project.sneakermarket.auction.entity.AuctionProduct;
import com.project.sneakermarket.auction.entity.AuctionShippingStatus;
import com.project.sneakermarket.auction.exception.AuctionConflictException;
import com.project.sneakermarket.auction.exception.AuctionIntegrationException;
import com.project.sneakermarket.auction.exception.AuctionUnauthorizedException;
import com.project.sneakermarket.auction.logistics.ecpay.AuctionCvsStoreSelectionService;
import com.project.sneakermarket.auction.notification.AuctionPrivateNotificationEventPublisher;
import com.project.sneakermarket.auction.repository.AuctionLinePayTransactionRepository;
import com.project.sneakermarket.auction.repository.AuctionOrderRepository;
import com.project.sneakermarket.auction.repository.AuctionProductRepository;
import com.project.sneakermarket.auction.repository.AuctionRepository;
import com.project.sneakermarket.auction.service.AuctionTime;
import com.project.sneakermarket.common.exception.ForbiddenException;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;

class AuctionLinePayServiceTests {
    private static final String ORDER_NUMBER = "AO-LINE-001";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 21, 13, 0);

    private AuctionOrderRepository orders;
    private AuctionLinePayTransactionRepository transactions;
    private AuctionRepository auctions;
    private AuctionProductRepository products;
    private AuctionCvsStoreSelectionService stores;
    private AuctionTime time;
    private LinePayClient client;
    private AuctionPrivateNotificationEventPublisher privateNotificationEvents;
    private AuctionLinePayService service;
    private AuctionOrder order;
    private AuctionProduct product;

    @BeforeEach
    void setup() {
        orders = mock(AuctionOrderRepository.class);
        transactions = mock(AuctionLinePayTransactionRepository.class);
        auctions = mock(AuctionRepository.class);
        products = mock(AuctionProductRepository.class);
        stores = mock(AuctionCvsStoreSelectionService.class);
        time = mock(AuctionTime.class);
        client = mock(LinePayClient.class);
        privateNotificationEvents = mock(AuctionPrivateNotificationEventPublisher.class);

        AuctionLinePaySettings settings = new AuctionLinePaySettings(
                "channel-123",
                "channel-secret",
                "https://sandbox-api-pay.line.me");
        service = new AuctionLinePayService(
                orders,
                transactions,
                auctions,
                products,
                stores,
                time,
                client,
                settings,
                privateNotificationEvents);

        order = pendingLinePayOrder();
        Auction auction = new Auction();
        auction.setAuctionId(7L);
        auction.setAuctionProductId(17L);
        product = new AuctionProduct();
        product.setAuctionProductId(17L);
        product.setProductName("測試球鞋");
        product.setShippingMethod("宅配");
        product.setShippingFee(new BigDecimal("100.00"));

        when(orders.findByOrderNumberForUpdate(ORDER_NUMBER)).thenReturn(Optional.of(order));
        when(auctions.findById(7L)).thenReturn(Optional.of(auction));
        when(products.findById(17L)).thenReturn(Optional.of(product));
        when(time.now()).thenReturn(NOW);
    }

    @Test
    void anonymousRequestIsRejectedBeforeOrderLookup() {
        assertThrows(AuctionUnauthorizedException.class,
                () -> service.request(ORDER_NUMBER, null));

        verifyNoInteractions(orders, transactions, stores, client, privateNotificationEvents);
    }

    @Test
    void nonBuyerCannotRequestLinePay() {
        assertThrows(ForbiddenException.class,
                () -> service.request(ORDER_NUMBER, 99L));

        verifyNoInteractions(transactions, stores, client, privateNotificationEvents);
    }

    @Test
    void nonLinePayOrderCannotRequestLinePay() {
        order.setPaymentMethod("信用卡");

        AuctionConflictException error = assertThrows(AuctionConflictException.class,
                () -> service.request(ORDER_NUMBER, 10L));

        assertEquals("此競標訂單不是 LINE Pay 付款方式", error.getMessage());
        verifyNoInteractions(transactions, stores, client, privateNotificationEvents);
    }

    @Test
    void requestAtDeadlineIsRejectedWithoutCreatingAttempt() {
        order.setPaymentDueAt(NOW);

        AuctionConflictException error = assertThrows(AuctionConflictException.class,
                () -> service.request(ORDER_NUMBER, 10L));

        assertEquals("競標訂單付款期限已過", error.getMessage());
        verify(transactions, never()).saveAndFlush(any());
        verifyNoInteractions(stores, client, privateNotificationEvents);
    }

    @Test
    void cvsGuardRunsBeforeTransactionAndProviderRequest() {
        doThrow(new AuctionConflictException("請先選擇超商取貨門市"))
                .when(stores).requireSelectedStoreIfNeeded(order);

        AuctionConflictException error = assertThrows(AuctionConflictException.class,
                () -> service.request(ORDER_NUMBER, 10L));

        assertEquals("請先選擇超商取貨門市", error.getMessage());
        verify(transactions, never()).saveAndFlush(any());
        verifyNoInteractions(client, privateNotificationEvents);
    }

    @Test
    void validRequestUsesOnlyServerOrderAndProductAmountAndCreatesRequestedAttempt() {
        when(client.requestPayment(any())).thenReturn(new LinePayClient.RequestResult(
                "0000",
                "Success",
                "202609211234567890",
                "https://sandbox-web-pay.line.me/pay/abc"));

        AuctionLinePayRequestResponse response = service.request(ORDER_NUMBER, 10L);

        assertEquals("https://sandbox-web-pay.line.me/pay/abc", response.paymentUrl());
        assertTrue(response.merchantOrderId().matches("ALP[A-F0-9]{32}"));
        ArgumentCaptor<LinePayClient.RequestCommand> command =
                ArgumentCaptor.forClass(LinePayClient.RequestCommand.class);
        verify(client).requestPayment(command.capture());
        assertEquals(6900L, command.getValue().amount());
        assertEquals("TWD", command.getValue().currency());
        assertTrue(command.getValue().merchantOrderId().matches("ALP[A-F0-9]{32}"));
        assertEquals("17", command.getValue().product().id());
        assertEquals("測試球鞋", command.getValue().product().name());
        assertEquals(1, command.getValue().product().quantity());
        assertEquals(6900L, command.getValue().product().price());
        assertEquals(command.getValue().merchantOrderId(), response.merchantOrderId());

        ArgumentCaptor<AuctionLinePayTransaction> created =
                ArgumentCaptor.forClass(AuctionLinePayTransaction.class);
        verify(transactions).saveAndFlush(created.capture());
        AuctionLinePayTransaction attempt = created.getValue();
        assertEquals(8L, attempt.getAuctionOrderId());
        assertEquals(new BigDecimal("6900.00"), attempt.getAmount());
        assertEquals("TWD", attempt.getCurrency());
        assertEquals("202609211234567890", attempt.getLinePayTransactionId());
        assertEquals(AuctionLinePayTransactionStatus.REQUESTED, attempt.getStatus());
        assertEquals("0000", attempt.getReturnCode());
        verify(transactions).save(attempt);
        assertEquals(AuctionPaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals(AuctionOrderStatus.PENDING_PAYMENT, order.getOrderStatus());
        verifyNoInteractions(privateNotificationEvents);
    }

    @Test
    void providerRequestFailureMarksAttemptFailedAndNeverPaysOrder() {
        when(client.requestPayment(any())).thenReturn(new LinePayClient.RequestResult(
                "1172", "Invalid amount", null, null));

        assertThrows(AuctionIntegrationException.class,
                () -> service.request(ORDER_NUMBER, 10L));

        ArgumentCaptor<AuctionLinePayTransaction> created =
                ArgumentCaptor.forClass(AuctionLinePayTransaction.class);
        verify(transactions).saveAndFlush(created.capture());
        AuctionLinePayTransaction attempt = created.getValue();
        assertEquals(AuctionLinePayTransactionStatus.FAILED, attempt.getStatus());
        assertEquals("1172", attempt.getReturnCode());
        assertEquals("Invalid amount", attempt.getReturnMessage());
        verify(transactions).save(attempt);
        assertEquals(AuctionPaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals(AuctionOrderStatus.PENDING_PAYMENT, order.getOrderStatus());
        verify(orders, never()).save(any());
        verifyNoInteractions(privateNotificationEvents);
    }

    @Test
    void providerSuccessWithoutTransactionIdMarksAttemptFailedAndNeverPaysOrder() {
        when(client.requestPayment(any())).thenReturn(new LinePayClient.RequestResult(
                "0000", "Success", null, "https://sandbox-web-pay.line.me/pay/abc"));

        assertThrows(AuctionIntegrationException.class,
                () -> service.request(ORDER_NUMBER, 10L));

        ArgumentCaptor<AuctionLinePayTransaction> created =
                ArgumentCaptor.forClass(AuctionLinePayTransaction.class);
        verify(transactions).saveAndFlush(created.capture());
        AuctionLinePayTransaction attempt = created.getValue();
        assertEquals(AuctionLinePayTransactionStatus.FAILED, attempt.getStatus());
        assertEquals("INVALID_RESPONSE", attempt.getReturnCode());
        assertEquals(AuctionPaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals(AuctionOrderStatus.PENDING_PAYMENT, order.getOrderStatus());
        verify(orders, never()).save(any());
        verifyNoInteractions(privateNotificationEvents);
    }

    @Test
    void anonymousStatusIsRejectedBeforeOrderLookup() {
        assertThrows(AuctionUnauthorizedException.class,
                () -> service.checkStatus(ORDER_NUMBER, "ALP123", null));

        verifyNoInteractions(orders, transactions, client, privateNotificationEvents);
    }

    @Test
    void nonBuyerCannotCheckStatus() {
        assertThrows(ForbiddenException.class,
                () -> service.checkStatus(ORDER_NUMBER, "ALP123", 99L));

        verifyNoInteractions(transactions, client, privateNotificationEvents);
    }

    @Test
    void fakeMerchantOrderIdReturnsNotFound() {
        when(transactions.findByMerchantOrderIdForUpdate("ALPFAKE"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.checkStatus(ORDER_NUMBER, "ALPFAKE", 10L));

        verifyNoInteractions(client, privateNotificationEvents);
    }

    @Test
    void transactionFromAnotherOrderIsRejected() {
        AuctionLinePayTransaction attempt = requestedAttempt();
        attempt.setAuctionOrderId(999L);
        when(transactions.findByMerchantOrderIdForUpdate(attempt.getMerchantOrderId()))
                .thenReturn(Optional.of(attempt));

        assertThrows(AuctionConflictException.class,
                () -> service.checkStatus(ORDER_NUMBER, attempt.getMerchantOrderId(), 10L));

        verifyNoInteractions(client, privateNotificationEvents);
    }

    @Test
    void waitingStatusDoesNotModifyAttempt() {
        AuctionLinePayTransaction attempt = requestedAttempt();
        stubStatusLookup(attempt);
        when(client.checkPaymentRequestStatus(attempt.getLinePayTransactionId()))
                .thenReturn(new LinePayClient.CheckStatusResult("0000", "Waiting"));

        AuctionLinePayStatusResponse response = service.checkStatus(
                ORDER_NUMBER, attempt.getMerchantOrderId(), 10L);

        assertEquals("WAITING", response.status());
        assertEquals(AuctionLinePayTransactionStatus.REQUESTED, attempt.getStatus());
        assertNull(attempt.getReturnCode());
        verify(transactions, never()).save(any());
        verify(client, never()).confirmPayment(anyString(), anyLong(), anyString());
        verifyNoInteractions(stores, privateNotificationEvents);
    }

    @Test
    void authenticatedStatusConfirmsPaysAndPublishesExactlyOnce() {
        AuctionLinePayTransaction attempt = requestedAttempt();
        stubStatusLookup(attempt);
        when(client.checkPaymentRequestStatus("202609211234567890"))
                .thenReturn(new LinePayClient.CheckStatusResult("0110", "Authenticated"));
        when(client.confirmPayment("202609211234567890", 6900L, "TWD"))
                .thenReturn(new LinePayClient.ConfirmResult("0000", "Success"));

        assertEquals("PAID", service.checkStatus(
                ORDER_NUMBER, attempt.getMerchantOrderId(), 10L).status());

        assertEquals(AuctionLinePayTransactionStatus.CONFIRMED, attempt.getStatus());
        assertEquals(NOW, attempt.getConfirmedAt());
        assertEquals("0000", attempt.getReturnCode());
        assertEquals(AuctionPaymentStatus.PAID, order.getPaymentStatus());
        assertEquals(AuctionOrderStatus.PAID, order.getOrderStatus());
        assertEquals(NOW, order.getPaidAt());
        assertEquals(AuctionShippingStatus.NOT_SHIPPED, order.getShippingStatus());
        verify(stores).requireSelectedStoreIfNeeded(order);
        verify(privateNotificationEvents).publishOrderPaid(
                7L, 8L, ORDER_NUMBER, 10L, 11L, NOW);

        assertEquals("PAID", service.checkStatus(
                ORDER_NUMBER, attempt.getMerchantOrderId(), 10L).status());

        verify(client, times(1)).checkPaymentRequestStatus("202609211234567890");
        verify(client, times(1)).confirmPayment("202609211234567890", 6900L, "TWD");
        verify(orders, times(1)).save(order);
        verify(privateNotificationEvents, times(1)).publishOrderPaid(
                7L, 8L, ORDER_NUMBER, 10L, 11L, NOW);
    }

    @Test
    void cancelledStatusCancelsAttemptAndKeepsOrderPending() {
        AuctionLinePayTransaction attempt = requestedAttempt();
        stubStatusLookup(attempt);
        when(client.checkPaymentRequestStatus(attempt.getLinePayTransactionId()))
                .thenReturn(new LinePayClient.CheckStatusResult("0121", "Cancelled"));

        AuctionLinePayStatusResponse response = service.checkStatus(
                ORDER_NUMBER, attempt.getMerchantOrderId(), 10L);

        assertEquals("CANCELLED", response.status());
        assertEquals(AuctionLinePayTransactionStatus.CANCELLED, attempt.getStatus());
        assertEquals(NOW, attempt.getCancelledAt());
        assertEquals(AuctionPaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals(AuctionOrderStatus.PENDING_PAYMENT, order.getOrderStatus());
        verify(transactions).save(attempt);
        verify(orders, never()).save(any());
        verifyNoInteractions(privateNotificationEvents);
    }

    @Test
    void failedStatusMarksOnlyAttemptFailed() {
        AuctionLinePayTransaction attempt = requestedAttempt();
        stubStatusLookup(attempt);
        when(client.checkPaymentRequestStatus(attempt.getLinePayTransactionId()))
                .thenReturn(new LinePayClient.CheckStatusResult("0122", "Failed"));

        AuctionLinePayStatusResponse response = service.checkStatus(
                ORDER_NUMBER, attempt.getMerchantOrderId(), 10L);

        assertEquals("FAILED", response.status());
        assertEquals(AuctionLinePayTransactionStatus.FAILED, attempt.getStatus());
        assertEquals(AuctionPaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals(AuctionOrderStatus.PENDING_PAYMENT, order.getOrderStatus());
        verify(orders, never()).save(any());
        verifyNoInteractions(privateNotificationEvents);
    }

    @Test
    void completedProviderStatusSafelyReconcilesWithoutConfirmAndIsIdempotent() {
        AuctionLinePayTransaction attempt = requestedAttempt();
        stubStatusLookup(attempt);
        when(client.checkPaymentRequestStatus(attempt.getLinePayTransactionId()))
                .thenReturn(new LinePayClient.CheckStatusResult("0123", "Completed"));

        assertEquals("PAID", service.checkStatus(
                ORDER_NUMBER, attempt.getMerchantOrderId(), 10L).status());

        assertEquals(AuctionLinePayTransactionStatus.CONFIRMED, attempt.getStatus());
        assertEquals("0123", attempt.getReturnCode());
        assertEquals(AuctionPaymentStatus.PAID, order.getPaymentStatus());
        assertEquals(AuctionOrderStatus.PAID, order.getOrderStatus());
        verify(client, never()).confirmPayment(anyString(), anyLong(), anyString());
        verify(stores).requireSelectedStoreIfNeeded(order);
        verify(privateNotificationEvents).publishOrderPaid(
                7L, 8L, ORDER_NUMBER, 10L, 11L, NOW);

        assertEquals("PAID", service.checkStatus(
                ORDER_NUMBER, attempt.getMerchantOrderId(), 10L).status());
        verify(client, times(1)).checkPaymentRequestStatus("202609211234567890");
        verify(privateNotificationEvents, times(1)).publishOrderPaid(
                7L, 8L, ORDER_NUMBER, 10L, 11L, NOW);
    }

    @Test
    void authenticatedAfterDeadlineNeverCallsConfirm() {
        AuctionLinePayTransaction attempt = requestedAttempt();
        stubStatusLookup(attempt);
        order.setPaymentDueAt(NOW);
        when(client.checkPaymentRequestStatus(attempt.getLinePayTransactionId()))
                .thenReturn(new LinePayClient.CheckStatusResult("0110", "Authenticated"));

        assertThrows(AuctionConflictException.class,
                () -> service.checkStatus(ORDER_NUMBER, attempt.getMerchantOrderId(), 10L));

        verify(client, never()).confirmPayment(anyString(), anyLong(), anyString());
        verifyNoInteractions(stores, privateNotificationEvents);
        assertEquals(AuctionPaymentStatus.PENDING, order.getPaymentStatus());
    }

    @Test
    void authenticatedWithInvalidCvsSelectionNeverCallsConfirm() {
        AuctionLinePayTransaction attempt = requestedAttempt();
        stubStatusLookup(attempt);
        when(client.checkPaymentRequestStatus(attempt.getLinePayTransactionId()))
                .thenReturn(new LinePayClient.CheckStatusResult("0110", "Authenticated"));
        doThrow(new AuctionConflictException("請先選擇超商取貨門市"))
                .when(stores).requireSelectedStoreIfNeeded(order);

        assertThrows(AuctionConflictException.class,
                () -> service.checkStatus(ORDER_NUMBER, attempt.getMerchantOrderId(), 10L));

        verify(client, never()).confirmPayment(anyString(), anyLong(), anyString());
        verifyNoInteractions(privateNotificationEvents);
        assertEquals(AuctionLinePayTransactionStatus.REQUESTED, attempt.getStatus());
    }

    @Test
    void completedStatusWithChangedAmountCannotReconcile() {
        AuctionLinePayTransaction attempt = requestedAttempt();
        attempt.setAmount(new BigDecimal("7000.00"));
        stubStatusLookup(attempt);
        when(client.checkPaymentRequestStatus(attempt.getLinePayTransactionId()))
                .thenReturn(new LinePayClient.CheckStatusResult("0123", "Completed"));

        assertThrows(AuctionConflictException.class,
                () -> service.checkStatus(ORDER_NUMBER, attempt.getMerchantOrderId(), 10L));

        assertEquals(AuctionLinePayTransactionStatus.REQUESTED, attempt.getStatus());
        assertEquals(AuctionPaymentStatus.PENDING, order.getPaymentStatus());
        verify(orders, never()).save(any());
        verifyNoInteractions(privateNotificationEvents);
    }

    @Test
    void confirmProviderFailureReturnsFailedWithoutPayingOrder() {
        AuctionLinePayTransaction attempt = requestedAttempt();
        stubStatusLookup(attempt);
        when(client.checkPaymentRequestStatus(attempt.getLinePayTransactionId()))
                .thenReturn(new LinePayClient.CheckStatusResult("0110", "Authenticated"));
        when(client.confirmPayment(attempt.getLinePayTransactionId(), 6900L, "TWD"))
                .thenReturn(new LinePayClient.ConfirmResult("1199", "Declined"));

        AuctionLinePayStatusResponse response = service.checkStatus(
                ORDER_NUMBER, attempt.getMerchantOrderId(), 10L);

        assertEquals("FAILED", response.status());
        assertEquals(AuctionLinePayTransactionStatus.FAILED, attempt.getStatus());
        assertEquals(AuctionPaymentStatus.PENDING, order.getPaymentStatus());
        verify(orders, never()).save(any());
        verifyNoInteractions(privateNotificationEvents);
    }

    @Test
    void alreadyConfirmedPaidAttemptDoesNotCallCheckConfirmOrPublishAgain() {
        AuctionLinePayTransaction attempt = requestedAttempt();
        attempt.setStatus(AuctionLinePayTransactionStatus.CONFIRMED);
        attempt.setConfirmedAt(NOW.minusMinutes(1));
        order.setPaymentStatus(AuctionPaymentStatus.PAID);
        order.setOrderStatus(AuctionOrderStatus.SHIPPED);
        order.setPaidAt(NOW.minusMinutes(1));
        stubStatusLookup(attempt);

        assertEquals("PAID", service.checkStatus(
                ORDER_NUMBER, attempt.getMerchantOrderId(), 10L).status());

        verifyNoInteractions(client, privateNotificationEvents);
        verify(orders, never()).save(any());
        verify(transactions, never()).save(any());
    }

    @Test
    void requestedSecondAttemptCannotConfirmAfterAnotherAttemptPaidOrder() {
        AuctionLinePayTransaction attempt = requestedAttempt();
        order.setPaymentStatus(AuctionPaymentStatus.PAID);
        order.setOrderStatus(AuctionOrderStatus.PAID);
        order.setPaidAt(NOW.minusMinutes(1));
        stubStatusLookup(attempt);
        when(client.checkPaymentRequestStatus(attempt.getLinePayTransactionId()))
                .thenReturn(new LinePayClient.CheckStatusResult("0110", "Authenticated"));

        assertThrows(AuctionConflictException.class,
                () -> service.checkStatus(ORDER_NUMBER, attempt.getMerchantOrderId(), 10L));

        verify(client, never()).confirmPayment(anyString(), anyLong(), anyString());
        verify(orders, never()).save(any());
        verifyNoInteractions(stores, privateNotificationEvents);
    }

    private AuctionOrder pendingLinePayOrder() {
        AuctionOrder value = new AuctionOrder();
        value.setAuctionOrderId(8L);
        value.setOrderNumber(ORDER_NUMBER);
        value.setAuctionId(7L);
        value.setBuyerId(10L);
        value.setSellerId(11L);
        value.setFinalPrice(new BigDecimal("6800.00"));
        value.setPaymentMethod("LINE Pay");
        value.setPaymentStatus(AuctionPaymentStatus.PENDING);
        value.setOrderStatus(AuctionOrderStatus.PENDING_PAYMENT);
        value.setShippingStatus(AuctionShippingStatus.NOT_SHIPPED);
        value.setShippingAddress("台北市測試路 1 號");
        value.setPaymentDueAt(NOW.plusHours(1));
        value.setUpdatedAt(NOW.minusHours(1));
        return value;
    }

    private AuctionLinePayTransaction requestedAttempt() {
        AuctionLinePayTransaction value = new AuctionLinePayTransaction();
        value.setId(21L);
        value.setAuctionOrderId(8L);
        value.setMerchantOrderId("ALP0123456789ABCDEF0123456789ABCDEF");
        value.setLinePayTransactionId("202609211234567890");
        value.setAmount(new BigDecimal("6900.00"));
        value.setCurrency("TWD");
        value.setStatus(AuctionLinePayTransactionStatus.REQUESTED);
        value.setCreatedAt(NOW.minusMinutes(2));
        value.setUpdatedAt(NOW.minusMinutes(2));
        return value;
    }

    private void stubStatusLookup(AuctionLinePayTransaction attempt) {
        when(transactions.findByMerchantOrderIdForUpdate(attempt.getMerchantOrderId()))
                .thenReturn(Optional.of(attempt));
    }
}
