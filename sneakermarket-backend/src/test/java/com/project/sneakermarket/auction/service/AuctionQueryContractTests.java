package com.project.sneakermarket.auction.service;

import com.project.sneakermarket.auction.dto.*;
import com.project.sneakermarket.auction.entity.*;
import com.project.sneakermarket.auction.repository.*;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuctionQueryContractTests {
    private AuctionQueryService query;
    private Auction auction;
    private AuctionReferenceService references;
    private AuctionRepository auctions;

    @BeforeEach
    void setup() {
        references = mock(AuctionReferenceService.class);
        auctions = mock(AuctionRepository.class);
        var products = mock(AuctionProductRepository.class);
        var images = mock(AuctionProductImageRepository.class);
        var now = LocalDateTime.of(2026, 9, 7, 12, 0);
        var time = new AuctionTime(Clock.fixed(Instant.parse("2026-09-07T04:00:00Z"), ZoneOffset.UTC));
        query = new AuctionQueryService(references, time, auctions, products,
                mock(AuctionBidRepository.class), images);
        auction = new Auction();
        auction.setAuctionId(12L);
        auction.setAuctionProductId(3L);
        auction.setStartAt(now.minusHours(1));
        auction.setEndAt(now.plusDays(1));
        auction.setStatus(AuctionStatus.SCHEDULED);
        auction.setBidCount(0);
        auction.setStartPrice(BigDecimal.TEN);
        auction.setCurrentPrice(BigDecimal.TEN);
        auction.setMinIncrement(new BigDecimal("5"));
        auction.setUpdatedAt(now.minusMinutes(10));
        var product = new AuctionProduct();
        product.setAuctionProductId(3L);
        product.setSellerId(42L);
        product.setBrandId(71L);
        product.setCategoryId(92L);
        when(auctions.findById(12L)).thenReturn(Optional.of(auction));
        when(products.findById(3L)).thenReturn(Optional.of(product));
        when(images.findByAuctionProductIdOrderBySortOrderAsc(3L)).thenReturn(List.of());
        when(references.getOptions()).thenReturn(new AuctionOptionsResponse(
                List.of(new AuctionLookupOption(71L, "Actual brand")),
                List.of(new AuctionLookupOption(92L, "Actual category"))));
    }

    @Test
    void detailUsesActualMasterNamesAndTimeBeforeSchedulerRuns() {
        var result = query.getAuctionDetail(12L, 42L);
        assertEquals("Actual brand", result.brandName());
        assertEquals("Actual category", result.categoryName());
        assertTrue(result.hasStarted());
        assertTrue(result.canManage());
        // 查詢不能擅自寫入競標狀態。
        assertEquals(AuctionStatus.SCHEDULED, auction.getStatus());
    }

    @Test
    void detailWithoutBidsUsesCurrentPricePlusIncrementAsNextMinimum() {
        var result = query.getAuctionDetail(12L, null);

        assertEquals(0, new BigDecimal("15").compareTo(result.nextMinimumBid()));
    }

    @Test
    void detailWithBidUsesCurrentPricePlusIncrementAsNextMinimum() {
        auction.setCurrentPrice(new BigDecimal("15"));
        auction.setBidCount(1);

        var result = query.getAuctionDetail(12L, null);

        assertEquals(0, new BigDecimal("20").compareTo(result.nextMinimumBid()));
    }

    @Test
    void statusReturnsLatestPublicFieldsWithCurrentPricePlusIncrement() {
        LocalDateTime endAt = LocalDateTime.of(2026, 9, 8, 12, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 9, 7, 11, 55);
        auction.setCurrentPrice(new BigDecimal("5500"));
        auction.setMinIncrement(new BigDecimal("500"));
        auction.setBidCount(4);
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setEndAt(endAt);
        auction.setUpdatedAt(updatedAt);

        var result = query.getAuctionStatus(12L);

        assertEquals(12L, result.auctionId());
        assertEquals(0, new BigDecimal("5500").compareTo(result.currentPrice()));
        assertEquals(4, result.bidCount());
        assertEquals(0, new BigDecimal("6000").compareTo(result.nextMinimumBid()));
        assertEquals(AuctionStatus.ACTIVE, result.status());
        assertEquals(endAt, result.endAt());
        assertEquals(updatedAt, result.updatedAt());
        verify(auctions).findById(12L);
        verify(auctions, never()).findByIdForUpdate(anyLong());
        verify(auctions, never()).save(any());
    }

    @Test
    void statusWithoutBidsStillRequiresOneIncrementAboveCurrentPrice() {
        auction.setCurrentPrice(new BigDecimal("3000"));
        auction.setMinIncrement(new BigDecimal("500"));
        auction.setBidCount(0);

        var result = query.getAuctionStatus(12L);

        assertEquals(0, new BigDecimal("3500").compareTo(result.nextMinimumBid()));
    }

    @Test
    void missingAuctionStatusReturnsNotFound() {
        when(auctions.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> query.getAuctionStatus(99L)
        );
    }

    @Test
    void statusQueryDoesNotAdvanceScheduledAuctionAfterStartTime() {
        assertEquals(AuctionStatus.SCHEDULED, query.getAuctionStatus(12L).status());
        assertEquals(AuctionStatus.SCHEDULED, auction.getStatus());
        verify(auctions, never()).save(any());
    }

    @Test
    void guestAndDifferentMemberCannotManage() {
        assertFalse(query.getAuctionDetail(12L, null).isOwner());
        assertFalse(query.getAuctionDetail(12L, null).canManage());
        assertFalse(query.getAuctionDetail(12L, 43L).canManage());
    }

    @Test
    void expiredOwnerCannotManageAndMissingNamesDoNotBreakHistory() {
        auction.setEndAt(LocalDateTime.of(2026, 9, 7, 12, 0));
        when(references.getOptions()).thenReturn(new AuctionOptionsResponse(List.of(), List.of()));
        var result = query.getAuctionDetail(12L, 42L);
        assertTrue(result.isOwner());
        assertFalse(result.canManage());
        assertNull(result.brandName());
        assertNull(result.categoryName());
    }
}
