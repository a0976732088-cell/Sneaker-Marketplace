package com.project.sneakermarket.auction.payment.linepay;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionLinePayTransaction;
import com.project.sneakermarket.auction.entity.AuctionLinePayTransactionStatus;
import com.project.sneakermarket.auction.entity.AuctionOrder;
import com.project.sneakermarket.auction.entity.AuctionOrderStatus;
import com.project.sneakermarket.auction.entity.AuctionPaymentStatus;
import com.project.sneakermarket.auction.entity.AuctionProduct;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionLinePayService {
    public static final String LINE_PAY_METHOD = "LINE Pay";
    private static final String CURRENCY = "TWD";
    private static final EnumSet<AuctionOrderStatus> PAID_ORDER_STATES = EnumSet.of(
            AuctionOrderStatus.PAID,
            AuctionOrderStatus.SHIPPED,
            AuctionOrderStatus.COMPLETED);

    private final AuctionOrderRepository orders;
    private final AuctionLinePayTransactionRepository transactions;
    private final AuctionRepository auctions;
    private final AuctionProductRepository products;
    private final AuctionCvsStoreSelectionService stores;
    private final AuctionTime time;
    private final LinePayClient client;
    private final AuctionLinePaySettings settings;
    private final AuctionPrivateNotificationEventPublisher privateNotificationEvents;

    @Transactional(noRollbackFor = AuctionIntegrationException.class)
    public AuctionLinePayRequestResponse request(String orderNumber, Long memberId) {
        requireMember(memberId);
        AuctionOrder order = orders.findByOrderNumberForUpdate(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("找不到競標訂單"));
        requireBuyer(order, memberId);
        requirePending(order, "此競標訂單目前無法發起 LINE Pay");
        if (!LINE_PAY_METHOD.equals(order.getPaymentMethod())) {
            throw new AuctionConflictException("此競標訂單不是 LINE Pay 付款方式");
        }
        LocalDateTime now = time.now();
        requireBeforeDeadline(order, now);
        stores.requireSelectedStoreIfNeeded(order);
        settings.requireProviderConfigured();

        AuctionProduct product = productFor(order);
        BigDecimal amount = totalAmount(order, product);
        long integerAmount = integerTwdAmount(amount);
        String merchantOrderId = nextMerchantOrderId();

        AuctionLinePayTransaction transaction = new AuctionLinePayTransaction();
        transaction.setAuctionOrderId(order.getAuctionOrderId());
        transaction.setMerchantOrderId(merchantOrderId);
        // SQL Server 的單欄 UNIQUE 只允許一個 NULL；使用不會外洩的 attempt key，
        // provider 成功回應後立刻換成真正 transactionId，欄位 schema 仍維持 nullable。
        transaction.setLinePayTransactionId("PENDING-" + merchantOrderId);
        transaction.setAmount(amount);
        transaction.setCurrency(CURRENCY);
        transaction.setStatus(AuctionLinePayTransactionStatus.INITIATED);
        transaction.setCreatedAt(now);
        transaction.setUpdatedAt(now);
        transactions.saveAndFlush(transaction);

        LinePayClient.RequestResult result;
        try {
            result = client.requestPayment(new LinePayClient.RequestCommand(
                    integerAmount,
                    CURRENCY,
                    merchantOrderId,
                    new LinePayClient.Product(
                            String.valueOf(product.getAuctionProductId()),
                            product.getProductName(),
                            1,
                            integerAmount)));
        } catch (AuctionIntegrationException ex) {
            fail(transaction, "CLIENT_ERROR", ex.getMessage(), now);
            throw ex;
        }

        transaction.setReturnCode(shorten(result.returnCode(), 20));
        transaction.setReturnMessage(shorten(result.returnMessage(), 500));
        if (!"0000".equals(result.returnCode())) {
            fail(transaction, result.returnCode(), result.returnMessage(), now);
            throw AuctionIntegrationException.provider(providerFailureMessage(
                    "LINE Pay 建立付款失敗", result.returnCode()));
        }

        String providerTransactionId;
        String paymentUrl;
        try {
            providerTransactionId = normalizeProviderTransactionId(result.transactionId());
            paymentUrl = requireHttpPaymentUrl(result.paymentUrl());
        } catch (AuctionIntegrationException ex) {
            fail(transaction, "INVALID_RESPONSE", ex.getMessage(), now);
            throw ex;
        }
        if (transactions.existsByLinePayTransactionId(providerTransactionId)) {
            fail(transaction, "DUPLICATE_TRANSACTION", "Duplicate transactionId", now);
            throw AuctionIntegrationException.provider("LINE Pay 回傳重複的交易識別碼");
        }
        transaction.setLinePayTransactionId(providerTransactionId);
        transaction.setStatus(AuctionLinePayTransactionStatus.REQUESTED);
        transaction.setUpdatedAt(now);
        transactions.save(transaction);
        return new AuctionLinePayRequestResponse(paymentUrl, merchantOrderId);
    }

    @Transactional(noRollbackFor = AuctionIntegrationException.class)
    public AuctionLinePayStatusResponse checkStatus(
            String orderNumber,
            String merchantOrderId,
            Long memberId) {
        requireMember(memberId);
        AuctionOrder order = orders.findByOrderNumberForUpdate(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("找不到競標訂單"));
        requireBuyer(order, memberId);
        if (!LINE_PAY_METHOD.equals(order.getPaymentMethod())) {
            throw new AuctionConflictException("此競標訂單不是 LINE Pay 付款方式");
        }

        String merchantId = normalize(merchantOrderId, 80, "merchant order id");
        AuctionLinePayTransaction transaction = transactions.findByMerchantOrderIdForUpdate(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到 LINE Pay 交易"));
        if (!Objects.equals(transaction.getAuctionOrderId(), order.getAuctionOrderId())) {
            throw new AuctionConflictException("LINE Pay 交易不屬於此競標訂單");
        }

        return switch (transaction.getStatus()) {
            case CONFIRMED -> confirmedStatus(order);
            case CANCELLED -> cancelledStatus();
            case FAILED, INITIATED -> failedStatus();
            case REQUESTED -> checkRequestedStatus(order, transaction);
        };
    }

    private AuctionLinePayStatusResponse checkRequestedStatus(
            AuctionOrder order,
            AuctionLinePayTransaction transaction) {
        settings.requireProviderConfigured();
        String transactionId = normalizeProviderTransactionId(
                transaction.getLinePayTransactionId());
        LinePayClient.CheckStatusResult result =
                client.checkPaymentRequestStatus(transactionId);

        return switch (result.returnCode()) {
            case "0000" -> waitingStatus();
            case "0110" -> confirmAuthenticatedPayment(order, transaction, transactionId);
            case "0121" -> cancelAttempt(transaction, result);
            case "0122" -> failAttempt(transaction, result);
            case "0123" -> reconcileCompletedPayment(order, transaction, result);
            default -> throw AuctionIntegrationException.provider(providerFailureMessage(
                    "LINE Pay 查詢付款狀態失敗", result.returnCode()));
        };
    }

    private AuctionLinePayStatusResponse confirmAuthenticatedPayment(
            AuctionOrder order,
            AuctionLinePayTransaction transaction,
            String transactionId) {
        LocalDateTime paidAt = time.now();
        long integerAmount = requireEligibleCompletion(order, transaction, paidAt);

        LinePayClient.ConfirmResult result;
        try {
            result = client.confirmPayment(transactionId, integerAmount, CURRENCY);
        } catch (AuctionIntegrationException ex) {
            transaction.setReturnCode("CLIENT_ERROR");
            transaction.setReturnMessage(shorten(ex.getMessage(), 500));
            transaction.setUpdatedAt(paidAt);
            transactions.save(transaction);
            throw ex;
        }
        if (!"0000".equals(result.returnCode())) {
            transaction.setStatus(AuctionLinePayTransactionStatus.FAILED);
            transaction.setReturnCode(shorten(result.returnCode(), 20));
            transaction.setReturnMessage(shorten(result.returnMessage(), 500));
            transaction.setUpdatedAt(paidAt);
            transactions.save(transaction);
            return failedStatus();
        }

        markPaid(transaction, order, paidAt, result.returnCode(), result.returnMessage());
        return paidStatus();
    }

    private AuctionLinePayStatusResponse reconcileCompletedPayment(
            AuctionOrder order,
            AuctionLinePayTransaction transaction,
            LinePayClient.CheckStatusResult result) {
        LocalDateTime paidAt = time.now();
        requireEligibleCompletion(order, transaction, paidAt);
        markPaid(transaction, order, paidAt, result.returnCode(), result.returnMessage());
        return paidStatus();
    }

    private long requireEligibleCompletion(
            AuctionOrder order,
            AuctionLinePayTransaction transaction,
            LocalDateTime paidAt) {
        requirePending(order, "此競標訂單目前無法完成 LINE Pay");
        requireBeforeDeadline(order, paidAt);
        // Authentication 與 polling 之間仍可能 PATCH checkout；完成付款前必須重新驗證。
        stores.requireSelectedStoreIfNeeded(order);

        AuctionProduct product = productFor(order);
        BigDecimal currentAmount = totalAmount(order, product);
        if (!CURRENCY.equals(transaction.getCurrency())
                || currentAmount.compareTo(transaction.getAmount()) != 0) {
            throw new AuctionConflictException("LINE Pay 交易金額與競標訂單不符");
        }
        return integerTwdAmount(transaction.getAmount());
    }

    private AuctionLinePayStatusResponse cancelAttempt(
            AuctionLinePayTransaction transaction,
            LinePayClient.CheckStatusResult result) {
        LocalDateTime cancelledAt = time.now();
        transaction.setStatus(AuctionLinePayTransactionStatus.CANCELLED);
        transaction.setReturnCode(shorten(result.returnCode(), 20));
        transaction.setReturnMessage(shorten(result.returnMessage(), 500));
        transaction.setCancelledAt(cancelledAt);
        transaction.setUpdatedAt(cancelledAt);
        transactions.save(transaction);
        return cancelledStatus();
    }

    private AuctionLinePayStatusResponse failAttempt(
            AuctionLinePayTransaction transaction,
            LinePayClient.CheckStatusResult result) {
        LocalDateTime failedAt = time.now();
        transaction.setStatus(AuctionLinePayTransactionStatus.FAILED);
        transaction.setReturnCode(shorten(result.returnCode(), 20));
        transaction.setReturnMessage(shorten(result.returnMessage(), 500));
        transaction.setUpdatedAt(failedAt);
        transactions.save(transaction);
        return failedStatus();
    }

    private void markPaid(
            AuctionLinePayTransaction transaction,
            AuctionOrder order,
            LocalDateTime paidAt,
            String returnCode,
            String returnMessage) {
        transaction.setStatus(AuctionLinePayTransactionStatus.CONFIRMED);
        transaction.setReturnCode(shorten(returnCode, 20));
        transaction.setReturnMessage(shorten(returnMessage, 500));
        transaction.setConfirmedAt(paidAt);
        transaction.setUpdatedAt(paidAt);
        transactions.save(transaction);

        order.setPaymentStatus(AuctionPaymentStatus.PAID);
        order.setOrderStatus(AuctionOrderStatus.PAID);
        order.setPaidAt(paidAt);
        order.setUpdatedAt(paidAt);
        orders.save(order);
        privateNotificationEvents.publishOrderPaid(
                order.getAuctionId(),
                order.getAuctionOrderId(),
                order.getOrderNumber(),
                order.getBuyerId(),
                order.getSellerId(),
                paidAt);
    }

    private AuctionLinePayStatusResponse confirmedStatus(AuctionOrder order) {
        if (order.getPaymentStatus() == AuctionPaymentStatus.PAID
                && PAID_ORDER_STATES.contains(order.getOrderStatus())) {
            return paidStatus();
        }
        throw new AuctionConflictException("LINE Pay 交易與競標訂單付款狀態不一致");
    }

    private AuctionLinePayStatusResponse waitingStatus() {
        return new AuctionLinePayStatusResponse("WAITING", "等待 LINE Pay 完成付款");
    }

    private AuctionLinePayStatusResponse paidStatus() {
        return new AuctionLinePayStatusResponse("PAID", "LINE Pay 付款完成");
    }

    private AuctionLinePayStatusResponse cancelledStatus() {
        return new AuctionLinePayStatusResponse("CANCELLED", "LINE Pay 已取消");
    }

    private AuctionLinePayStatusResponse failedStatus() {
        return new AuctionLinePayStatusResponse("FAILED", "LINE Pay 付款失敗");
    }

    private AuctionProduct productFor(AuctionOrder order) {
        Auction auction = auctions.findById(order.getAuctionId())
                .orElseThrow(() -> new ResourceNotFoundException("找不到此訂單的競標資料"));
        return products.findById(auction.getAuctionProductId())
                .orElseThrow(() -> new ResourceNotFoundException("找不到此訂單的商品資料"));
    }

    private BigDecimal totalAmount(AuctionOrder order, AuctionProduct product) {
        if (order.getFinalPrice() == null || product.getShippingFee() == null) {
            throw new AuctionConflictException("競標訂單金額不完整");
        }
        return order.getFinalPrice().add(product.getShippingFee());
    }

    private long integerTwdAmount(BigDecimal amount) {
        try {
            long value = amount.longValueExact();
            if (value <= 0) throw new ArithmeticException();
            return value;
        } catch (ArithmeticException ex) {
            throw new AuctionConflictException("LINE Pay 僅接受合法的 TWD 整數金額");
        }
    }

    private String nextMerchantOrderId() {
        String value;
        do {
            value = "ALP" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        } while (transactions.existsByMerchantOrderId(value));
        return value;
    }

    private void requireMember(Long memberId) {
        if (memberId == null || memberId <= 0) throw new AuctionUnauthorizedException();
    }

    private void requireBuyer(AuctionOrder order, Long memberId) {
        if (!memberId.equals(order.getBuyerId())) {
            throw new ForbiddenException("你沒有權限付款此競標訂單");
        }
    }

    private void requirePending(AuctionOrder order, String message) {
        if (order.getPaymentStatus() != AuctionPaymentStatus.PENDING
                || order.getOrderStatus() != AuctionOrderStatus.PENDING_PAYMENT) {
            throw new AuctionConflictException(message);
        }
    }

    private void requireBeforeDeadline(AuctionOrder order, LocalDateTime now) {
        if (order.getPaymentDueAt() == null || !now.isBefore(order.getPaymentDueAt())) {
            throw new AuctionConflictException("競標訂單付款期限已過");
        }
    }

    private String normalize(String value, int maxLength, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new AuctionConflictException(field + " 格式不正確");
        }
        return normalized;
    }

    private String requireHttpPaymentUrl(String value) {
        String normalized = normalizeProviderValue(value, 2000, "LINE Pay 未回傳有效的付款網址");
        try {
            URI uri = URI.create(normalized);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
            return normalized;
        } catch (IllegalArgumentException ex) {
            throw AuctionIntegrationException.provider("LINE Pay 未回傳有效的付款網址");
        }
    }

    private String normalizeProviderValue(String value, int maxLength, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw AuctionIntegrationException.provider(message);
        }
        return normalized;
    }

    private String normalizeProviderTransactionId(String value) {
        String normalized = normalizeProviderValue(
                value, 100, "LINE Pay 未回傳有效的交易識別碼");
        if (!normalized.matches("[A-Za-z0-9_-]{1,100}")) {
            throw AuctionIntegrationException.provider("LINE Pay 未回傳有效的交易識別碼");
        }
        return normalized;
    }

    private void fail(
            AuctionLinePayTransaction transaction,
            String returnCode,
            String returnMessage,
            LocalDateTime now) {
        transaction.setStatus(AuctionLinePayTransactionStatus.FAILED);
        transaction.setReturnCode(shorten(returnCode, 20));
        transaction.setReturnMessage(shorten(returnMessage, 500));
        transaction.setUpdatedAt(now);
        transactions.save(transaction);
    }

    private String shorten(String value, int maxLength) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String providerFailureMessage(String prefix, String returnCode) {
        String code = shorten(returnCode, 20);
        return code == null || code.isBlank() ? prefix : prefix + "（代碼 " + code + "）";
    }
}
