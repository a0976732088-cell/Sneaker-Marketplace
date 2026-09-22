package com.project.sneakermarket.auction.service;

import com.project.sneakermarket.auction.dto.*;
import com.project.sneakermarket.auction.entity.*;
import com.project.sneakermarket.auction.exception.AuctionUnauthorizedException;
import com.project.sneakermarket.auction.notification.AuctionPrivateNotificationEventPublisher;
import com.project.sneakermarket.auction.repository.*;
import com.project.sneakermarket.auction.realtime.AuctionRealtimeEventPublisher;
import com.project.sneakermarket.auction.realtime.AuctionRealtimeEventType;
import com.project.sneakermarket.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
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
    private AuctionProductImageRepository productImages;
    private AuctionBidRepository bids;
    private ImageStorageService images;
    private AuctionRealtimeEventPublisher realtimeEvents;
    private AuctionPrivateNotificationEventPublisher privateNotificationEvents;
    private AuctionCommandService service;
    private Auction auction;
    private AuctionProduct product;

    @BeforeEach
    void setup() {
        references = mock(AuctionReferenceRepository.class);
        auctions = mock(AuctionRepository.class);
        products = mock(AuctionProductRepository.class);
        productImages = mock(AuctionProductImageRepository.class);
        bids = mock(AuctionBidRepository.class);
        images = mock(ImageStorageService.class);
        realtimeEvents = mock(AuctionRealtimeEventPublisher.class);
        privateNotificationEvents = mock(AuctionPrivateNotificationEventPublisher.class);
        var time = new AuctionTime(Clock.fixed(Instant.parse("2026-09-07T04:00:00Z"), ZoneOffset.UTC));
        service = new AuctionCommandService(products, productImages,
                auctions, bids, images, new AuctionReferenceService(references), time,
                realtimeEvents, privateNotificationEvents);
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
        when(bids.findDistinctBidderIds(12L)).thenReturn(List.of());
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

    private List<MultipartFile> uploadedImages(int count) {
        List<MultipartFile> uploadedImages = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            uploadedImages.add(new MockMultipartFile(
                    "images",
                    "shoe-" + index + ".jpg",
                    "image/jpeg",
                    ("image-" + index).getBytes()));
        }
        return uploadedImages;
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
    void tenImagesPassServiceValidationAndKeepImageOrdering() {
        when(products.saveAndFlush(any(AuctionProduct.class))).thenAnswer(invocation -> {
            AuctionProduct savedProduct = invocation.getArgument(0);
            savedProduct.setAuctionProductId(3L);
            return savedProduct;
        });
        when(images.store(any(MultipartFile.class), eq(3L))).thenAnswer(invocation -> {
            MultipartFile image = invocation.getArgument(0);
            return "/uploads/auctions/3/" + image.getOriginalFilename();
        });

        List<AuctionProductImage> savedImageRows = new ArrayList<>();
        when(productImages.saveAllAndFlush(anyList())).thenAnswer(invocation -> {
            List<AuctionProductImage> imageRows = invocation.getArgument(0);
            savedImageRows.addAll(imageRows);
            return imageRows;
        });
        when(auctions.saveAndFlush(any(Auction.class))).thenAnswer(invocation -> {
            Auction savedAuction = invocation.getArgument(0);
            savedAuction.setAuctionId(12L);
            return savedAuction;
        });

        Long auctionId = service.createAuction(
                42L,
                createRequest(7L, 9L),
                uploadedImages(10));

        assertEquals(12L, auctionId);
        verify(images, times(10)).store(any(MultipartFile.class), eq(3L));
        assertEquals(10, savedImageRows.size());
        for (int index = 0; index < savedImageRows.size(); index++) {
            AuctionProductImage imageRow = savedImageRows.get(index);
            assertEquals(index, imageRow.getSortOrder());
            assertEquals(index == 0, imageRow.getIsPrimary());
        }
    }

    @Test
    void elevenImagesFailServiceValidationBeforeAnyWrite() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createAuction(
                        42L,
                        createRequest(7L, 9L),
                        uploadedImages(11)));

        assertEquals("競標商品最多只能上傳 10 張圖片", exception.getMessage());
        verifyNoInteractions(products, productImages, images, auctions);
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
        verifyNoInteractions(bids, realtimeEvents, privateNotificationEvents);
    }

    @Test
    void successfulCancellationPublishesRealtimeEventAfterSave() {
        when(bids.findDistinctBidderIds(12L)).thenReturn(List.of(61L, 77L));

        service.cancelAuction(12L, 42L);

        assertEquals(AuctionStatus.CANCELLED, auction.getStatus());
        assertEquals(now, auction.getUpdatedAt());
        var order = inOrder(auctions, realtimeEvents, privateNotificationEvents);
        order.verify(auctions).save(auction);
        order.verify(realtimeEvents).publish(AuctionRealtimeEventType.AUCTION_CANCELLED, auction);
        order.verify(privateNotificationEvents).publishCancelled(
                12L, 42L, List.of(61L, 77L), null, now);
        verify(bids).findDistinctBidderIds(12L);
    }

    @Test
    void alreadyCancelledAuctionDoesNotQueryParticipantsOrPublishAgain() {
        auction.setStatus(AuctionStatus.CANCELLED);

        assertThrows(BusinessException.class, () -> service.cancelAuction(12L, 42L));

        verify(auctions, never()).save(any());
        verifyNoInteractions(bids, realtimeEvents, privateNotificationEvents);
    }

    @Test
    void auctionWithBidSummaryDoesNotQueryParticipantsOrPublishCancellation() {
        auction.setBidCount(1);

        assertThrows(BusinessException.class, () -> service.cancelAuction(12L, 42L));

        verify(auctions, never()).save(any());
        verifyNoInteractions(bids, realtimeEvents, privateNotificationEvents);
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
        verifyNoInteractions(bids, realtimeEvents, privateNotificationEvents);
    }
}
