package com.project.sneakermarket.auction.email;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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

    @ParameterizedTest
    @MethodSource("failures")
    void errorsAreSafeBoundedSingleLineSummaries(RuntimeException failure) {
        var logs = mock(AuctionEmailLogRepository.class);
        var results = mock(AuctionResultRepository.class);
        var auctions = mock(AuctionRepository.class);
        var products = mock(AuctionProductRepository.class);
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
        new AuctionEmailDispatchService(logs, results, auctions, products, sender, new AuctionTime(),
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
                mock(AuctionProductRepository.class), sender, new AuctionTime(),
                new AuctionEmailSettings(true, 3, "from@example.invalid", "https://example.invalid")).sendEmailLog(1L);
        verifyNoInteractions(sender);
    }
}
