package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import com.project.sneakermarket.auction.entity.*;
import com.project.sneakermarket.auction.repository.*;

class AuctionLifecycleServiceTests {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 10, 12, 0);
    private AuctionRepository auctions;
    private AuctionResultRepository results;
    private AuctionLifecycleService service;
    private Auction auction;

    @BeforeEach
    void setup() {
        auctions = mock(AuctionRepository.class);
        results = mock(AuctionResultRepository.class);
        var time = new AuctionTime(Clock.fixed(Instant.parse("2026-09-10T04:00:00Z"), ZoneOffset.UTC));
        service = new AuctionLifecycleService(auctions, results, time);
        auction = new Auction();
        auction.setAuctionId(1L);
        auction.setStatus(AuctionStatus.SCHEDULED);
        auction.setStartAt(NOW);
        auction.setEndAt(NOW.plusHours(1));
        auction.setBidCount(0);
        auction.setCurrentPrice(new BigDecimal("5000.00"));
        auction.setUpdatedAt(NOW.minusDays(1));
        when(auctions.findByIdForUpdate(1L)).thenReturn(Optional.of(auction));
    }

    @Test
    void scheduledBeforeStartDoesNotChange() {
        auction.setStartAt(NOW.plusSeconds(1));
        service.processAuction(1L);
        assertEquals(AuctionStatus.SCHEDULED, auction.getStatus());
        noWrites();
    }

    @Test
    void scheduledAtStartBecomesActiveWithoutResult() {
        service.processAuction(1L);
        assertEquals(AuctionStatus.ACTIVE, auction.getStatus());
        assertEquals(NOW, auction.getUpdatedAt());
        var order = inOrder(auctions);
        order.verify(auctions).findByIdForUpdate(1L);
        order.verify(auctions).save(auction);
        verifyNoInteractions(results);
    }

    @Test
    void activeBeforeEndDoesNotChange() {
        auction.setStatus(AuctionStatus.ACTIVE);
        service.processAuction(1L);
        assertEquals(AuctionStatus.ACTIVE, auction.getStatus());
        noWrites();
    }

    @ParameterizedTest
    @EnumSource(value = AuctionStatus.class, names = {"ACTIVE", "SCHEDULED"})
    void atEndWithoutBidsClosesDirectlyAndIsIdempotent(AuctionStatus initial) {
        auction.setStatus(initial);
        auction.setStartAt(NOW.minusHours(1));
        auction.setEndAt(NOW);
        service.processAuction(1L);
        service.processAuction(1L);
        assertEquals(AuctionStatus.UNSOLD, auction.getStatus());
        AuctionResult result = onlyResult();
        assertEquals(AuctionResultStatus.UNSOLD, result.getResultStatus());
        assertNull(result.getWinnerId());
        assertNull(result.getFinalPrice());
        assertFalse(result.getReason().isBlank());
    }

    @ParameterizedTest
    @EnumSource(value = AuctionStatus.class, names = {"ACTIVE", "SCHEDULED"})
    void overdueWithBidsUsesLatestWinnerAndPriceAndIsIdempotent(AuctionStatus initial) {
        auction.setStatus(initial);
        auction.setStartAt(NOW.minusHours(2));
        auction.setEndAt(NOW.minusSeconds(1));
        auction.setBidCount(2);
        auction.setHighestBidderId(77L);
        auction.setCurrentPrice(new BigDecimal("6000.00"));
        service.processAuction(1L);
        service.processAuction(1L);
        assertEquals(AuctionStatus.SOLD, auction.getStatus());
        AuctionResult result = onlyResult();
        assertEquals(AuctionResultStatus.SOLD, result.getResultStatus());
        assertEquals(77L, result.getWinnerId());
        assertEquals(auction.getCurrentPrice(), result.getFinalPrice());
        assertNull(result.getReason());
    }

    @ParameterizedTest
    @EnumSource(value = AuctionStatus.class, names = {"DRAFT", "SOLD", "UNSOLD", "CANCELLED", "REMOVED"})
    void inactiveOrTerminalStatusIsNeverRewritten(AuctionStatus status) {
        auction.setStatus(status);
        auction.setEndAt(NOW.minusSeconds(1));
        service.processAuction(1L);
        assertEquals(status, auction.getStatus());
        noWrites();
    }

    @ParameterizedTest
    @CsvSource(value = {"1,NULL", "0,77", "-1,NULL", "NULL,NULL"}, nullValues = "NULL")
    void inconsistentBidSummaryDoesNotProduceResult(Integer count, Long winner) {
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setEndAt(NOW);
        auction.setBidCount(count);
        auction.setHighestBidderId(winner);
        assertThrows(IllegalStateException.class, () -> service.processAuction(1L));
        assertEquals(AuctionStatus.ACTIVE, auction.getStatus());
        noWrites();
    }

    @Test
    void soldWithoutPriceIsRejected() {
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setEndAt(NOW);
        auction.setBidCount(1);
        auction.setHighestBidderId(77L);
        auction.setCurrentPrice(null);
        assertThrows(IllegalStateException.class, () -> service.processAuction(1L));
        noWrites();
    }

    @Test
    void missingCandidateIsIgnored() {
        when(auctions.findByIdForUpdate(1L)).thenReturn(Optional.empty());
        service.processAuction(1L);
        noWrites();
    }

    @Test
    void timeIsReadOnlyAfterLockHasReturned() {
        AuctionTime time = mock(AuctionTime.class);
        when(time.now()).thenReturn(NOW);
        new AuctionLifecycleService(auctions, results, time).processAuction(1L);
        var order = inOrder(auctions, time);
        order.verify(auctions).findByIdForUpdate(1L);
        order.verify(time).now();
        verify(auctions, never()).findById(any());
    }

    private AuctionResult onlyResult() {
        var captor = ArgumentCaptor.forClass(AuctionResult.class);
        verify(results, times(1)).save(captor.capture());
        assertEquals(1L, captor.getValue().getAuctionId());
        assertEquals(NOW, captor.getValue().getEndedAt());
        assertEquals(NOW, captor.getValue().getCreatedAt());
        assertEquals(NOW, auction.getUpdatedAt());
        verify(auctions, times(1)).save(auction);
        return captor.getValue();
    }

    private void noWrites() {
        verify(auctions, never()).save(any());
        verifyNoInteractions(results);
        assertEquals(NOW.minusDays(1), auction.getUpdatedAt());
    }
}
