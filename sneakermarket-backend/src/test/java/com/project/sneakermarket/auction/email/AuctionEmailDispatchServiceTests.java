package com.project.sneakermarket.auction.email;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import com.project.sneakermarket.auction.entity.*;
import com.project.sneakermarket.auction.repository.*;
import com.project.sneakermarket.auction.service.AuctionTime;

class AuctionEmailDispatchServiceTests {
    static Stream<RuntimeException> failures() {
        String untrusted = "password=fixture-secret\r\nCookie=fixture-cookie " + "x".repeat(2000);
        return Stream.of(new MailAuthenticationException(untrusted), new MailSendException(untrusted),
                new MailParseException(untrusted), new IllegalStateException(untrusted));
    }

    static Stream<Arguments> abandonedMessages() {
        return Stream.of(
                Arguments.of(AuctionEmailType.ORDER_ABANDONED_BUYER, true),
                Arguments.of(AuctionEmailType.ORDER_ABANDONED_SELLER, false));
    }

    @ParameterizedTest
    @MethodSource("failures")
    void errorsAreSafeBoundedSingleLineSummaries(RuntimeException failure) {
        var logs = mock(AuctionEmailLogRepository.class);
        var results = mock(AuctionResultRepository.class);
        var auctions = mock(AuctionRepository.class);
        var products = mock(AuctionProductRepository.class);
        var orders = mock(AuctionOrderRepository.class);
        var sender = mock(JavaMailSender.class);
        var row = new AuctionEmailLog();
        row.setAuctionId(1L);
        row.setRecipientEmail("snapshot@example.invalid");
        row.setSubject("subject");
        row.setEmailType(AuctionEmailType.WON);
        row.setSendStatus(AuctionEmailSendStatus.PENDING);
        row.setRetryCount(0);
        when(logs.findByIdForUpdate(1L)).thenReturn(Optional.of(row));
        var result = new AuctionResult();
        result.setFinalPrice(new BigDecimal("6000"));
        when(results.findByAuctionId(1L)).thenReturn(Optional.of(result));
        var auction = new Auction();
        auction.setAuctionProductId(2L);
        when(auctions.findById(1L)).thenReturn(Optional.of(auction));
        var product = new AuctionProduct();
        product.setProductName("shoe");
        when(products.findById(2L)).thenReturn(Optional.of(product));
        doThrow(failure).when(sender).send(any(SimpleMailMessage.class));
        new AuctionEmailDispatchService(logs, results, auctions, products, orders, sender, new AuctionTime(),
                new AuctionEmailSettings(true, 3, "from@example.invalid", "https://example.invalid")).sendEmailLog(1L);
        assertEquals(AuctionEmailSendStatus.FAILED, row.getSendStatus());
        assertEquals(1, row.getRetryCount());
        assertFalse(row.getLastError().isBlank());
        assertTrue(row.getLastError().length() <= 1000);
        assertFalse(row.getLastError().matches("(?s).*(fixture|password|Cookie|\\r|\\n).*"));
        verify(logs).saveAndFlush(row);
        verify(auctions, never()).findByIdForUpdate(any());
        verify(results, never()).findByAuctionIdForUpdate(any());
    }

    @Test
    void missingLogDoesNotSend() {
        var logs = mock(AuctionEmailLogRepository.class);
        var sender = mock(JavaMailSender.class);
        new AuctionEmailDispatchService(logs, mock(AuctionResultRepository.class), mock(AuctionRepository.class),
                mock(AuctionProductRepository.class), mock(AuctionOrderRepository.class), sender, new AuctionTime(),
                new AuctionEmailSettings(true, 3, "from@example.invalid", "https://example.invalid")).sendEmailLog(1L);
        verifyNoInteractions(sender);
    }

    @ParameterizedTest
    @MethodSource("abandonedMessages")
    void abandonedMessagesContainOrderDeadlineAndRoleSpecificExplanation(AuctionEmailType type, boolean buyerMessage) {
        var logs = mock(AuctionEmailLogRepository.class);
        var results = mock(AuctionResultRepository.class);
        var auctions = mock(AuctionRepository.class);
        var products = mock(AuctionProductRepository.class);
        var orders = mock(AuctionOrderRepository.class);
        var sender = mock(JavaMailSender.class);
        var row = new AuctionEmailLog();
        row.setAuctionId(1L);
        row.setRecipientEmail("snapshot@example.invalid");
        row.setSubject("棄標通知");
        row.setEmailType(type);
        row.setSendStatus(AuctionEmailSendStatus.PENDING);
        row.setRetryCount(0);
        when(logs.findByIdForUpdate(1L)).thenReturn(Optional.of(row));
        var result = new AuctionResult();
        result.setFinalPrice(new BigDecimal("6000"));
        when(results.findByAuctionId(1L)).thenReturn(Optional.of(result));
        var auction = new Auction();
        auction.setAuctionProductId(2L);
        when(auctions.findById(1L)).thenReturn(Optional.of(auction));
        var product = new AuctionProduct();
        product.setProductName("限量球鞋");
        when(products.findById(2L)).thenReturn(Optional.of(product));
        var order = new AuctionOrder();
        order.setOrderNumber("AO-EMAIL-001");
        order.setFinalPrice(new BigDecimal("6000"));
        order.setPaymentDueAt(LocalDateTime.of(2026, 9, 16, 20, 0));
        order.setAbandonedAt(LocalDateTime.of(2026, 9, 16, 20, 0, 10));
        when(orders.findByAuctionId(1L)).thenReturn(Optional.of(order));

        new AuctionEmailDispatchService(logs, results, auctions, products, orders, sender, new AuctionTime(),
                new AuctionEmailSettings(true, 3, "from@example.invalid", "https://example.invalid"))
                .sendEmailLog(1L);

        var captured = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captured.capture());
        String text = captured.getValue().getText();
        assertNotNull(text);
        for (String expected : new String[]{"AO-EMAIL-001", "限量球鞋", "6000",
                "2026/09/16 20:00:00（Asia/Taipei）", "2026/09/16 20:00:10（Asia/Taipei）"}) {
            assertTrue(text.contains(expected));
        }
        if (buyerMessage) {
            assertTrue(text.contains("棄標原因：逾期未付款"));
            assertTrue(text.contains("目前無法再完成付款"));
        } else {
            assertTrue(text.contains("買家未在付款期限內完成付款"));
            assertTrue(text.contains("已標記為「已棄標」"));
        }
        assertFalse(text.contains("重新上架"));
        assertFalse(text.contains("第二順位"));
        assertEquals(AuctionEmailSendStatus.SENT, row.getSendStatus());
    }
}
