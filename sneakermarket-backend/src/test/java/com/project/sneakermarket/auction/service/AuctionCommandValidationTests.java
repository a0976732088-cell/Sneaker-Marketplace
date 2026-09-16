package com.project.sneakermarket.auction.service;

import com.project.sneakermarket.auction.dto.*;
import com.project.sneakermarket.auction.entity.*;
import com.project.sneakermarket.auction.exception.AuctionUnauthorizedException;
import com.project.sneakermarket.auction.repository.*;
import com.project.sneakermarket.auction.realtime.AuctionRealtimeEventPublisher;
import com.project.sneakermarket.auction.realtime.AuctionRealtimeEventType;
import com.project.sneakermarket.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuctionCommandValidationTests {
    private final LocalDateTime now = LocalDateTime.of(2026, 9, 7, 12, 0);
    private AuctionReferenceRepository references;
    private AuctionRepository auctions;
    private AuctionProductRepository products;
    private ImageStorageService images;
    private AuctionRealtimeEventPublisher realtimeEvents;
    private AuctionCommandService service;
    private Auction auction;
    private AuctionProduct product;

    @BeforeEach
    void setup() {
        references = mock(AuctionReferenceRepository.class);
        auctions = mock(AuctionRepository.class);
        products = mock(AuctionProductRepository.class);
        images = mock(ImageStorageService.class);
        realtimeEvents = mock(AuctionRealtimeEventPublisher.class);
        var time = new AuctionTime(Clock.fixed(Instant.parse("2026-09-07T04:00:00Z"), ZoneOffset.UTC));
        service = new AuctionCommandService(products, mock(AuctionProductImageRepository.class),
                auctions, images, new AuctionReferenceService(references), time, realtimeEvents);
        when(references.isActiveMember(42L)).thenReturn(true);
        when(references.brandExists(7L)).thenReturn(true);
        when(references.categoryExists(9L)).thenReturn(true);
        auction = new Auction();
        auction.setAuctionId(12L);
        auction.setAuctionProductId(3L);
        auction.setStartAt(now.plusDays(1));
        auction.setEndAt(now.plusDays(2));
        auction.setStatus(AuctionStatus.SCHEDULED);
        auction.setBidCount(0);
        product = new AuctionProduct();
        product.setAuctionProductId(3L);
        product.setSellerId(42L);
        product.setBrandId(7L);
        product.setCategoryId(9L);
        when(auctions.findByIdForUpdate(12L)).thenReturn(Optional.of(auction));
        when(products.findById(3L)).thenReturn(Optional.of(product));
    }

    private AuctionCreateRequest createRequest(Long brand, Long category) {
        return new AuctionCreateRequest(brand, category, "Test shoe", "T", "US 9",
                AuctionCondition.NEW, "description", null, null, "台北", "宅配",
                BigDecimal.ZERO, "轉帳", false, false, null, BigDecimal.TEN,
                BigDecimal.ONE, now.plusDays(1), now.plusDays(2));
    }

    private AuctionUpdateRequest updateRequest(Long brand, Long category, LocalDateTime start) {
        return new AuctionUpdateRequest(brand, category, "Updated shoe", "T", "US 9",
                AuctionCondition.NEW, "description", null, null, "台北", "宅配",
                BigDecimal.ZERO, "轉帳", false, false, null, BigDecimal.TEN,
                BigDecimal.ONE, start, now.plusDays(3));
    }

    @Test
    void nonexistentBrandFailsBeforeAnyProductOrImageWrite() {
        assertThrows(BusinessException.class, () -> service.createAuction(42L, createRequest(99L, 9L), List.of()));
        verifyNoInteractions(products, images);
    }

    @Test
    void nonexistentCategoryFailsBeforeAnyProductOrImageWrite() {
        assertThrows(BusinessException.class, () -> service.createAuction(42L, createRequest(7L, 99L), List.of()));
        verifyNoInteractions(products, images);
    }

    @Test
    void nonexistentSellerCannotWriteEvenWhenServiceIsCalledDirectly() {
        assertThrows(AuctionUnauthorizedException.class,
                () -> service.createAuction(88L, createRequest(7L, 9L), List.of()));
        verifyNoInteractions(products, images);
    }

    @Test
    void scheduledEditRejectsUnknownBrandWithoutChangingProduct() {
        assertThrows(BusinessException.class,
                () -> service.updateAuction(12L, 42L, updateRequest(99L, 9L, now.plusDays(1))));
        assertEquals(7L, product.getBrandId());
        verify(products, never()).save(any());
    }

    @Test
    void validScheduledEditUsesActualReferenceIds() {
        service.updateAuction(12L, 42L, updateRequest(7L, 9L, now.plusDays(1)));
        assertEquals("Updated shoe", product.getProductName());
        assertEquals(BigDecimal.TEN, auction.getCurrentPrice());
        assertEquals(AuctionStatus.SCHEDULED, auction.getStatus());
        verify(products).save(product);
        verify(auctions).save(auction);
        verifyNoInteractions(realtimeEvents);
    }

    @Test
    void activeEditKeepsOriginalBrandAndCategory() {
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setStartAt(now.minusHours(1));
        service.updateAuction(12L, 42L, updateRequest(99L, 99L, auction.getStartAt()));
        assertEquals(7L, product.getBrandId());
        assertEquals(9L, product.getCategoryId());
        assertEquals("Updated shoe", product.getProductName());
        assertEquals(BigDecimal.TEN, auction.getCurrentPrice());
        assertEquals(AuctionStatus.ACTIVE, auction.getStatus());
        verify(references).brandExists(7L);
        verify(references, never()).brandExists(99L);
        verify(products).save(product);
        verify(auctions).save(auction);
    }

    @Test
    void scheduledRowWhoseStartHasPassedUsesActiveRulesWithoutPersistingStatus() {
        LocalDateTime originalStartAt = now.minusHours(1);
        auction.setStartAt(originalStartAt);

        service.updateAuction(12L, 42L, updateRequest(99L, 99L, originalStartAt));

        assertEquals(AuctionStatus.SCHEDULED, auction.getStatus());
        assertEquals(originalStartAt, auction.getStartAt());
        assertEquals(7L, product.getBrandId());
        assertEquals(9L, product.getCategoryId());
        assertEquals("Updated shoe", product.getProductName());
        assertEquals(BigDecimal.TEN, auction.getCurrentPrice());
        verify(references).brandExists(7L);
        verify(references, never()).brandExists(99L);
        verify(products).save(product);
        verify(auctions).save(auction);
    }

    @Test
    void elapsedEndTimePreventsRevivingAuctionThroughEdit() {
        auction.setEndAt(now);
        assertThrows(BusinessException.class,
                () -> service.updateAuction(12L, 42L, updateRequest(7L, 9L, now.plusDays(1))));
        verify(auctions, never()).save(any());
    }

    @Test
    void elapsedEndTimePreventsCancellationBeforeSchedulerRuns() {
        auction.setEndAt(now);
        assertThrows(BusinessException.class, () -> service.cancelAuction(12L, 42L));
        verify(auctions, never()).save(any());
        verifyNoInteractions(realtimeEvents);
    }

    @Test
    void successfulCancellationPublishesRealtimeEventAfterSave() {
        service.cancelAuction(12L, 42L);

        assertEquals(AuctionStatus.CANCELLED, auction.getStatus());
        var order = inOrder(auctions, realtimeEvents);
        order.verify(auctions).save(auction);
        order.verify(realtimeEvents).publish(AuctionRealtimeEventType.AUCTION_CANCELLED, auction);
    }

    @Test
    void scheduledRowWhoseStartHasPassedCannotMoveStartTime() {
        auction.setStartAt(now.minusHours(1));
        assertThrows(BusinessException.class,
                () -> service.updateAuction(12L, 42L, updateRequest(7L, 9L, now.plusDays(1))));
        assertEquals(AuctionStatus.SCHEDULED, auction.getStatus());
        verify(auctions, never()).save(any());
    }

    @Test
    void timeIsCheckedAfterWaitingForAuctionLock() {
        when(auctions.findByIdForUpdate(12L)).thenAnswer(invocation -> {
            auction.setEndAt(now);
            return Optional.of(auction);
        });
        assertThrows(BusinessException.class, () -> service.cancelAuction(12L, 42L));
    }
}
