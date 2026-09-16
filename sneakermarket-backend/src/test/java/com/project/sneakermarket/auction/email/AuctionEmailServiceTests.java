package com.project.sneakermarket.auction.email;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.project.sneakermarket.auction.entity.*;
import com.project.sneakermarket.auction.repository.*;
import com.project.sneakermarket.auction.service.AuctionTime;
import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.member.repository.MemberRepository;

class AuctionEmailServiceTests {
    @ParameterizedTest
    @EnumSource(value = AuctionResultStatus.class, names = {"CANCELLED", "REMOVED"})
    void otherResultStatusesDoNotCreateOrSend(AuctionResultStatus status) {
        var results = mock(AuctionResultRepository.class);
        var result = new AuctionResult();
        result.setResultStatus(status);
        when(results.findByAuctionIdForUpdate(1L)).thenReturn(Optional.of(result));
        var logs = mock(AuctionEmailLogRepository.class);
        var orders = mock(AuctionOrderRepository.class);
        assertTrue(service(results, orders, logs, true).ensureEmailLogsForAuction(1L).isEmpty());
        verifyNoInteractions(logs, orders);
    }

    @Test
    void missingResultHasNoTasks() {
        var results = mock(AuctionResultRepository.class);
        var logs = mock(AuctionEmailLogRepository.class);
        var orders = mock(AuctionOrderRepository.class);
        assertTrue(service(results, orders, logs, true).ensureEmailLogsForAuction(1L).isEmpty());
        verifyNoInteractions(logs, orders);
    }

    @Test
    void disabledFeatureDoesNotEvenQueryDatabase() {
        var results = mock(AuctionResultRepository.class);
        var logs = mock(AuctionEmailLogRepository.class);
        var orders = mock(AuctionOrderRepository.class);
        var service = service(results, orders, logs, false);
        assertTrue(service.ensureEmailLogsForAuction(1L).isEmpty());
        var order = mock(AuctionOrder.class);
        service.ensureAbandonedEmailLogs(order);
        verifyNoInteractions(results, logs, orders);
        verifyNoInteractions(order);
    }

    @Test
    void abandonedOrderQueuesBuyerAndSellerFromPersistedMembers() {
        LocalDateTime abandonedAt = LocalDateTime.of(2026, 9, 15, 20, 0);
        var results = mock(AuctionResultRepository.class);
        var orders = mock(AuctionOrderRepository.class);
        var auctions = mock(AuctionRepository.class);
        var products = mock(AuctionProductRepository.class);
        var bids = mock(AuctionBidRepository.class);
        var members = mock(MemberRepository.class);
        var logs = mock(AuctionEmailLogRepository.class);
        var auction = new Auction();
        auction.setAuctionProductId(20L);
        var product = new AuctionProduct();
        product.setProductName("限量球鞋");
        var buyer = new Member();
        buyer.setEmail("buyer@example.invalid");
        var seller = new Member();
        seller.setEmail("seller@example.invalid");
        when(auctions.findById(10L)).thenReturn(Optional.of(auction));
        when(products.findById(20L)).thenReturn(Optional.of(product));
        when(members.findById(30L)).thenReturn(Optional.of(buyer));
        when(members.findById(40L)).thenReturn(Optional.of(seller));

        var order = new AuctionOrder();
        order.setAuctionId(10L);
        order.setBuyerId(30L);
        order.setSellerId(40L);
        order.setOrderStatus(AuctionOrderStatus.ABANDONED);
        order.setAbandonedAt(abandonedAt);
        new AuctionEmailService(results, orders, auctions, products, bids, members, logs, new AuctionTime(),
                new AuctionEmailSettings(true, 3, "sender@example.invalid", "https://example.invalid"))
                .ensureAbandonedEmailLogs(order);

        var captured = org.mockito.ArgumentCaptor.forClass(AuctionEmailLog.class);
        verify(logs, times(2)).save(captured.capture());
        verify(logs).flush();
        var rows = captured.getAllValues();
        var buyerLog = rows.stream().filter(e -> e.getEmailType() == AuctionEmailType.ORDER_ABANDONED_BUYER)
                .findFirst().orElseThrow();
        var sellerLog = rows.stream().filter(e -> e.getEmailType() == AuctionEmailType.ORDER_ABANDONED_SELLER)
                .findFirst().orElseThrow();
        assertEquals(30L, buyerLog.getRecipientMemberId());
        assertEquals("buyer@example.invalid", buyerLog.getRecipientEmail());
        assertEquals(AuctionEmailRecipientRole.WINNER, buyerLog.getRecipientRole());
        assertEquals(40L, sellerLog.getRecipientMemberId());
        assertEquals("seller@example.invalid", sellerLog.getRecipientEmail());
        assertEquals(AuctionEmailRecipientRole.SELLER, sellerLog.getRecipientRole());
        for (AuctionEmailLog row : rows) {
            assertEquals(AuctionEmailSendStatus.PENDING, row.getSendStatus());
            assertEquals(0, row.getRetryCount());
            assertEquals(abandonedAt, row.getCreatedAt());
            assertEquals(abandonedAt, row.getUpdatedAt());
            assertTrue(row.getSubject().contains("限量球鞋"));
        }
    }

    @Test
    void nonAbandonedOrMissingAbandonedAtQueuesNothing() {
        var results = mock(AuctionResultRepository.class);
        var orders = mock(AuctionOrderRepository.class);
        var auctions = mock(AuctionRepository.class);
        var products = mock(AuctionProductRepository.class);
        var bids = mock(AuctionBidRepository.class);
        var members = mock(MemberRepository.class);
        var logs = mock(AuctionEmailLogRepository.class);
        var service = new AuctionEmailService(results, orders, auctions, products, bids, members, logs,
                new AuctionTime(), new AuctionEmailSettings(true, 3, "sender@example.invalid", "https://example.invalid"));
        var order = new AuctionOrder();
        order.setOrderStatus(AuctionOrderStatus.PENDING_PAYMENT);
        order.setAbandonedAt(LocalDateTime.of(2026, 9, 15, 20, 0));
        service.ensureAbandonedEmailLogs(order);
        order.setOrderStatus(AuctionOrderStatus.ABANDONED);
        order.setAbandonedAt(null);
        service.ensureAbandonedEmailLogs(order);

        verifyNoInteractions(results, orders, auctions, products, bids, members, logs);
    }

    @Test
    void abandonedQueueMustJoinTheDeadlineTransaction() throws Exception {
        Transactional transactional = AuctionEmailService.class
                .getMethod("ensureAbandonedEmailLogs", AuctionOrder.class)
                .getAnnotation(Transactional.class);
        assertNotNull(transactional);
        assertEquals(Propagation.MANDATORY, transactional.propagation());
    }

    @Test
    void listenerSwallowsCreationFailureAfterMainCommit() {
        var emails = mock(AuctionEmailService.class);
        var dispatch = mock(AuctionEmailDispatchService.class);
        when(emails.ensureEmailLogsForAuction(1L)).thenThrow(new IllegalStateException("fixture"));
        assertDoesNotThrow(() -> new AuctionEmailEventListener(emails, dispatch).onAuctionEmailReady(new AuctionEmailReadyEvent(1L)));
        verifyNoInteractions(dispatch);
    }

    @Test
    void listenerContinuesAfterOneDispatchTransactionFailure() {
        var emails = mock(AuctionEmailService.class);
        var dispatch = mock(AuctionEmailDispatchService.class);
        when(emails.ensureEmailLogsForAuction(1L)).thenReturn(List.of(10L, 11L));
        doThrow(new IllegalStateException("fixture")).when(dispatch).sendEmailLog(10L);
        assertDoesNotThrow(() -> new AuctionEmailEventListener(emails, dispatch).onAuctionEmailReady(new AuctionEmailReadyEvent(1L)));
        verify(dispatch).sendEmailLog(11L);
    }

    @Test
    void settingsNormalizeBaseUrlAndRejectZeroAttempts() {
        var settings = new AuctionEmailSettings(true, 3, "sender@example.invalid", "https://example.invalid/");
        assertEquals("https://example.invalid", settings.frontendBaseUrl());
        assertThrows(IllegalArgumentException.class,
                () -> new AuctionEmailSettings(true, 0, "sender@example.invalid", "https://example.invalid"));
    }

    private AuctionEmailService service(AuctionResultRepository results, AuctionOrderRepository orders,
            AuctionEmailLogRepository logs, boolean enabled) {
        return new AuctionEmailService(results, orders, mock(AuctionRepository.class),
                mock(AuctionProductRepository.class), mock(AuctionBidRepository.class), mock(MemberRepository.class),
                logs, new AuctionTime(), new AuctionEmailSettings(enabled, 3, "sender@example.invalid", "https://example.invalid"));
    }
}
