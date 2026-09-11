package com.project.sneakermarket.auction.email;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.project.sneakermarket.auction.entity.*;
import com.project.sneakermarket.auction.repository.*;
import com.project.sneakermarket.auction.service.AuctionTime;
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
        assertTrue(service(results, orders, logs, false).ensureEmailLogsForAuction(1L).isEmpty());
        verifyNoInteractions(results, logs, orders);
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
