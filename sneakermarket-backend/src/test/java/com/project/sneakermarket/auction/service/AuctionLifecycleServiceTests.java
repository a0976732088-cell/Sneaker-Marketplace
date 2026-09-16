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
import com.project.sneakermarket.auction.notification.AuctionPrivateNotificationEventPublisher;
import com.project.sneakermarket.auction.repository.*;
import com.project.sneakermarket.auction.realtime.AuctionRealtimeEventPublisher;
import com.project.sneakermarket.auction.realtime.AuctionRealtimeEventType;

class AuctionLifecycleServiceTests {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 10, 12, 0);
    private AuctionRepository auctions;
    private AuctionResultRepository results;
    private org.springframework.context.ApplicationEventPublisher events;
    private AuctionRealtimeEventPublisher realtimeEvents;
    private AuctionPrivateNotificationEventPublisher privateNotificationEvents;
    private AuctionLifecycleService service;
    private Auction auction;

    @BeforeEach
    void setup() {
        auctions = mock(AuctionRepository.class);
        results = mock(AuctionResultRepository.class);
        events = mock(org.springframework.context.ApplicationEventPublisher.class);
        realtimeEvents = mock(AuctionRealtimeEventPublisher.class);
        privateNotificationEvents = mock(AuctionPrivateNotificationEventPublisher.class);
        var time = new AuctionTime(Clock.fixed(Instant.parse("2026-09-10T04:00:00Z"), ZoneOffset.UTC));
        service = new AuctionLifecycleService(
                auctions,
                results,
                time,
                events,
                realtimeEvents,
                privateNotificationEvents
        );
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
        verify(realtimeEvents).publish(AuctionRealtimeEventType.AUCTION_STARTED, auction);
    }

    @Test
    void activeBeforeEndDoesNotChange() {
        auction.setStatus(AuctionStatus.ACTIVE);
        service.processAuction(1L);
        assertEquals(AuctionStatus.ACTIVE, auction.getStatus());
        noWrites();
    }

    @ParameterizedTest
    @CsvSource({"-1", "1"})
    void removedScheduledBeforeEndNeverBecomesActive(int startOffsetMinutes) {
        auction.setRemovedAt(NOW.minusHours(1));
        auction.setStartAt(NOW.plusMinutes(startOffsetMinutes));

        service.processAuction(1L);

        assertEquals(AuctionStatus.SCHEDULED, auction.getStatus());
        noWrites();
    }

    @Test
    void removedActiveBeforeEndWaitsWithoutClosing() {
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setRemovedAt(NOW.minusHours(1));

        service.processAuction(1L);

        assertEquals(AuctionStatus.ACTIVE, auction.getStatus());
        noWrites();
    }

    @ParameterizedTest
    @CsvSource({"SCHEDULED,0", "SCHEDULED,3", "ACTIVE,0", "ACTIVE,3"})
    void removedAuctionAtEndIsVoidedAndKeepsBidLedger(AuctionStatus initial, int bidCount) {
        auction.setStatus(initial);
        auction.setStartAt(NOW.minusHours(1));
        auction.setEndAt(NOW);
        auction.setRemovedAt(NOW.minusMinutes(30));
        auction.setBidCount(bidCount);
        auction.setHighestBidderId(bidCount > 0 ? 77L : null);
        auction.setCurrentPrice(new BigDecimal(bidCount > 0 ? "6500.00" : "5000.00"));

        service.processAuction(1L);
        service.processAuction(1L);

        assertEquals(AuctionStatus.REMOVED, auction.getStatus());
        assertEquals(bidCount, auction.getBidCount());
        assertEquals(bidCount > 0 ? 77L : null, auction.getHighestBidderId());
        assertEquals(0, new BigDecimal(bidCount > 0 ? "6500.00" : "5000.00")
                .compareTo(auction.getCurrentPrice()));
        AuctionResult result = onlyResult();
        assertEquals(AuctionResultStatus.REMOVED, result.getResultStatus());
        assertNull(result.getWinnerId());
        assertNull(result.getFinalPrice());
        assertNotNull(result.getReason());
        assertFalse(result.getReason().isBlank());
        verifyNoInteractions(events);
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
    void existingResultPreventsDuplicateInsertWithoutUsingUniqueConstraintAsControlFlow() {
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setEndAt(NOW);
        AuctionResult existing = new AuctionResult();
        existing.setAuctionId(1L);
        existing.setResultStatus(AuctionResultStatus.SOLD);
        when(results.findByAuctionId(1L)).thenReturn(Optional.of(existing));

        assertDoesNotThrow(() -> service.processAuction(1L));

        assertEquals(AuctionStatus.ACTIVE, auction.getStatus());
        verify(results, never()).save(any());
        verify(auctions, never()).save(any());
    }

    @Test
    void deletedRunningAuctionIsIgnored() {
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setEndAt(NOW);
        auction.setDeletedAt(NOW.minusMinutes(1));

        service.processAuction(1L);

        assertEquals(AuctionStatus.ACTIVE, auction.getStatus());
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
        new AuctionLifecycleService(
                auctions,
                results,
                time,
                mock(org.springframework.context.ApplicationEventPublisher.class),
                mock(AuctionRealtimeEventPublisher.class),
                mock(AuctionPrivateNotificationEventPublisher.class)
        ).processAuction(1L);
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
        AuctionRealtimeEventType expectedType = switch (auction.getStatus()) {
            case SOLD -> AuctionRealtimeEventType.AUCTION_SOLD;
            case UNSOLD -> AuctionRealtimeEventType.AUCTION_UNSOLD;
            case REMOVED -> AuctionRealtimeEventType.AUCTION_REMOVED;
            default -> throw new AssertionError("Unexpected closed status: " + auction.getStatus());
        };
        verify(realtimeEvents).publish(expectedType, auction);
        if (auction.getStatus() == AuctionStatus.SOLD) {
            verify(privateNotificationEvents).publishSold(
                    1L,
                    auction.getHighestBidderId(),
                    auction.getCurrentPrice(),
                    NOW
            );
        } else {
            verifyNoInteractions(privateNotificationEvents);
        }
        return captor.getValue();
    }

    private void noWrites() {
        verify(auctions, never()).save(any());
        verify(results, never()).save(any());
        verifyNoInteractions(realtimeEvents, privateNotificationEvents);
        assertEquals(NOW.minusDays(1), auction.getUpdatedAt());
    }
}
