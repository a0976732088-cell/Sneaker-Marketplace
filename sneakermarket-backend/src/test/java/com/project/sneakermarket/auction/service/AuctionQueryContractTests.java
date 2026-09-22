package com.project.sneakermarket.auction.service;

import com.project.sneakermarket.auction.dto.*;
import com.project.sneakermarket.auction.entity.*;
import com.project.sneakermarket.auction.repository.*;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;
import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
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
    private AuctionProductRepository products;
    private AuctionBidRepository bids;
    private AuctionProductImageRepository images;
    private MemberRepository members;
    private AuctionProduct product;

    @BeforeEach
    void setup() {
        references = mock(AuctionReferenceService.class);
        auctions = mock(AuctionRepository.class);
        products = mock(AuctionProductRepository.class);
        bids = mock(AuctionBidRepository.class);
        images = mock(AuctionProductImageRepository.class);
        members = mock(MemberRepository.class);
        var now = LocalDateTime.of(2026, 9, 7, 12, 0);
        var time = new AuctionTime(Clock.fixed(Instant.parse("2026-09-07T04:00:00Z"), ZoneOffset.UTC));
        query = new AuctionQueryService(references, time, auctions, products,
                bids, images, members);
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
        product = new AuctionProduct();
        product.setAuctionProductId(3L);
        product.setSellerId(42L);
        product.setBrandId(71L);
        product.setCategoryId(92L);
        when(auctions.findPublicById(12L)).thenReturn(Optional.of(auction));
        when(products.findById(3L)).thenReturn(Optional.of(product));
        when(images.findByAuctionProductIdOrderBySortOrderAsc(3L)).thenReturn(List.of());
        when(images.findByAuctionProductIdInOrderByAuctionProductIdAscSortOrderAsc(anyList()))
                .thenReturn(List.of());
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
        verify(auctions).findPublicById(12L);
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
        when(auctions.findPublicById(99L)).thenReturn(Optional.empty());

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

    @Test
    void removedDeletedAndLegacyRemovedAuctionsAreNotPubliclyReadable() {
        for (Auction hidden : List.of(hiddenWithRemovedAt(), hiddenWithDeletedAt(), hiddenWithRemovedStatus())) {
            when(auctions.findPublicById(12L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> query.getAuctionStatus(12L));
            assertThrows(ResourceNotFoundException.class, () -> query.getAuctionDetail(12L, null));
        }
    }


    @Test
    void listReturnsSellerNamesWithOneBatchMemberQuery() {
        Auction secondAuction = auction(13L, 4L);
        AuctionProduct secondProduct = product(4L, 43L, "第二雙鞋");
        when(auctions.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(auction, secondAuction), PageRequest.of(0, 20), 2));
        when(products.findAllById(any())).thenReturn(List.of(product, secondProduct));
        when(members.findAllById(any())).thenReturn(List.of(
                member(42L, "王小明"), member(43L, "陳小華")));

        var result = query.searchAuctions(new AuctionSearchCriteria(), PageRequest.of(0, 20));

        assertEquals(List.of("王小明", "陳小華"),
                result.items().stream().map(AuctionSummaryResponse::sellerName).toList());
        verify(members, times(1)).findAllById(any());
        verify(members, never()).findById(anyLong());
    }

    @Test
    void pagedListLoadsImagesOnceAndPrefersPrimaryWithFirstImageFallback() {
        Auction secondAuction = auction(13L, 4L);
        Auction thirdAuction = auction(14L, 5L);
        AuctionProduct secondProduct = product(4L, 43L, "第二雙鞋");
        AuctionProduct thirdProduct = product(5L, 44L, "第三雙鞋");
        when(auctions.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(
                        List.of(auction, secondAuction, thirdAuction),
                        PageRequest.of(0, 20),
                        3));
        when(products.findAllById(any())).thenReturn(List.of(product, secondProduct, thirdProduct));
        when(members.findAllById(any())).thenReturn(List.of(
                member(42L, "王小明"), member(43L, "陳小華"), member(44L, "林小美")));
        when(images.findByAuctionProductIdInOrderByAuctionProductIdAscSortOrderAsc(anyList()))
                .thenReturn(List.of(
                        image(3L, "/images/first.jpg", 0, false),
                        image(3L, "/images/primary.jpg", 1, true),
                        image(4L, "/images/fallback.jpg", 0, false),
                        image(4L, "/images/second.jpg", 1, false)));

        var result = query.searchAuctions(
                new AuctionSearchCriteria(),
                PageRequest.of(0, 20));

        assertEquals("/images/primary.jpg", result.items().get(0).imageUrl());
        assertEquals("/images/fallback.jpg", result.items().get(1).imageUrl());
        assertNull(result.items().get(2).imageUrl());
        verify(images, times(1))
                .findByAuctionProductIdInOrderByAuctionProductIdAscSortOrderAsc(
                        argThat(ids -> ids.equals(List.of(3L, 4L, 5L))));
        verify(images, never()).findByAuctionProductIdOrderBySortOrderAsc(anyLong());
    }

    @Test
    void allListUsesOneBatchImageQuery() {
        when(auctions.findAll(any(Specification.class))).thenReturn(List.of(auction));
        when(products.findAllById(any())).thenReturn(List.of(product));
        when(members.findAllById(any())).thenReturn(List.of(member(42L, "王小明")));
        when(images.findByAuctionProductIdInOrderByAuctionProductIdAscSortOrderAsc(anyList()))
                .thenReturn(List.of(image(3L, "/images/all-primary.jpg", 0, true)));

        var result = query.getAllAuctions();

        assertEquals("/images/all-primary.jpg", result.get(0).imageUrl());
        verify(images, times(1))
                .findByAuctionProductIdInOrderByAuctionProductIdAscSortOrderAsc(List.of(3L));
        verify(images, never()).findByAuctionProductIdOrderBySortOrderAsc(anyLong());
    }

    @Test
    void detailReturnsSellerNameWithoutChangingOwnerCheck() {
        when(members.findById(42L)).thenReturn(Optional.of(member(42L, "王小明")));

        var ownerResult = query.getAuctionDetail(12L, 42L);
        var guestResult = query.getAuctionDetail(12L, null);

        assertEquals("王小明", ownerResult.sellerName());
        assertTrue(ownerResult.isOwner());
        assertFalse(guestResult.isOwner());
    }

    @Test
    void bidHistoryReturnsNamesWithOneBatchMemberQuery() {
        when(bids.findByAuctionIdOrderByCreatedAtDesc(12L)).thenReturn(List.of(
                bid(42L, "6000", 3), bid(43L, "5500", 2), bid(42L, "5000", 1)));
        when(members.findAllById(any())).thenReturn(List.of(
                member(42L, "王小明"), member(43L, "陳小華")));

        var result = query.getBidHistory(12L);

        assertEquals(List.of("王小明", "陳小華", "王小明"),
                result.stream().map(BidHistoryResponse::bidderName).toList());
        verify(members, times(1)).findAllById(any());
        verify(members, never()).findById(anyLong());
    }

    @Test
    void missingMembersProduceNullNamesWithoutFailingQueries() {
        when(auctions.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(auction), PageRequest.of(0, 20), 1));
        when(products.findAllById(any())).thenReturn(List.of(product));
        when(members.findAllById(any())).thenReturn(List.of());
        when(members.findById(42L)).thenReturn(Optional.empty());
        when(bids.findByAuctionIdOrderByCreatedAtDesc(12L))
                .thenReturn(List.of(bid(99L, "5000", 1)));

        assertNull(query.searchAuctions(new AuctionSearchCriteria(), PageRequest.of(0, 20))
                .items().get(0).sellerName());
        assertNull(query.getAuctionDetail(12L, null).sellerName());
        assertNull(query.getBidHistory(12L).get(0).bidderName());
    }

    @Test
    void publicNameResponsesDoNotExposeMemberIdentifiersOrPrivateFields() {
        assertTrue(recordFields(AuctionSummaryResponse.class).contains("sellerName"));
        assertTrue(recordFields(AuctionDetailResponse.class).contains("sellerName"));
        assertEquals(Set.of("bidAmount", "createdAt", "bidderName"),
                recordFields(BidHistoryResponse.class));
        for (Class<?> responseType : List.of(
                AuctionSummaryResponse.class, AuctionDetailResponse.class, BidHistoryResponse.class)) {
            assertTrue(Collections.disjoint(recordFields(responseType), Set.of(
                    "memberId", "sellerId", "bidderId", "account", "email", "phone",
                    "address", "passwordHash", "role")));
        }
    }

    private Auction auction(Long id, Long productId) {
        Auction value = new Auction();
        value.setAuctionId(id);
        value.setAuctionProductId(productId);
        value.setStartAt(auction.getStartAt());
        value.setEndAt(auction.getEndAt());
        value.setStatus(AuctionStatus.ACTIVE);
        value.setBidCount(0);
        value.setStartPrice(BigDecimal.TEN);
        value.setCurrentPrice(BigDecimal.TEN);
        value.setMinIncrement(new BigDecimal("5"));
        return value;
    }

    private AuctionProduct product(Long id, Long sellerId, String name) {
        AuctionProduct value = new AuctionProduct();
        value.setAuctionProductId(id);
        value.setSellerId(sellerId);
        value.setBrandId(71L);
        value.setCategoryId(92L);
        value.setProductName(name);
        return value;
    }

    private Member member(Long id, String name) {
        Member value = new Member();
        value.setMemberId(id);
        value.setName(name);
        return value;
    }

    private AuctionProductImage image(
            Long productId,
            String imageUrl,
            int sortOrder,
            boolean primary) {
        AuctionProductImage value = new AuctionProductImage();
        value.setAuctionProductId(productId);
        value.setImageUrl(imageUrl);
        value.setSortOrder(sortOrder);
        value.setIsPrimary(primary);
        return value;
    }

    private AuctionBid bid(Long bidderId, String amount, int minute) {
        AuctionBid value = new AuctionBid();
        value.setAuctionId(12L);
        value.setBidderId(bidderId);
        value.setBidAmount(new BigDecimal(amount));
        value.setCreatedAt(LocalDateTime.of(2026, 9, 7, 12, minute));
        return value;
    }

    private Set<String> recordFields(Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
                .map(component -> component.getName())
                .collect(java.util.stream.Collectors.toSet());
    }

    private Auction hiddenWithRemovedAt() {
        auction.setRemovedAt(LocalDateTime.of(2026, 9, 7, 11, 0));
        return auction;
    }

    private Auction hiddenWithDeletedAt() {
        auction.setRemovedAt(null);
        auction.setDeletedAt(LocalDateTime.of(2026, 9, 7, 11, 0));
        return auction;
    }

    private Auction hiddenWithRemovedStatus() {
        auction.setDeletedAt(null);
        auction.setStatus(AuctionStatus.REMOVED);
        return auction;
    }}
