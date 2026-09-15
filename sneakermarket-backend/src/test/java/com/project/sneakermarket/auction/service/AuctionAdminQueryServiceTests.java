package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.project.sneakermarket.auction.dto.AuctionAdminManagementState;
import com.project.sneakermarket.auction.dto.AuctionAdminSearchCriteria;
import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionBid;
import com.project.sneakermarket.auction.entity.AuctionOrder;
import com.project.sneakermarket.auction.entity.AuctionOrderStatus;
import com.project.sneakermarket.auction.entity.AuctionPaymentStatus;
import com.project.sneakermarket.auction.entity.AuctionProduct;
import com.project.sneakermarket.auction.entity.AuctionProductImage;
import com.project.sneakermarket.auction.entity.AuctionResult;
import com.project.sneakermarket.auction.entity.AuctionResultStatus;
import com.project.sneakermarket.auction.entity.AuctionShippingStatus;
import com.project.sneakermarket.auction.entity.AuctionStatus;
import com.project.sneakermarket.auction.repository.AuctionBidRepository;
import com.project.sneakermarket.auction.repository.AuctionOrderRepository;
import com.project.sneakermarket.auction.repository.AuctionProductImageRepository;
import com.project.sneakermarket.auction.repository.AuctionProductRepository;
import com.project.sneakermarket.auction.repository.AuctionRepository;
import com.project.sneakermarket.auction.repository.AuctionResultRepository;
import com.project.sneakermarket.commerce.entity.Brand;
import com.project.sneakermarket.commerce.repository.BrandRepository;
import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.member.repository.MemberRepository;

class AuctionAdminQueryServiceTests {
    @Test
    void adminListIncludesMetadataAndFirstImagesWithBatchQueries() {
        AuctionRepository auctions = mock(AuctionRepository.class);
        AuctionProductRepository products = mock(AuctionProductRepository.class);
        AuctionProductImageRepository images = mock(AuctionProductImageRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        AuctionBidRepository bids = mock(AuctionBidRepository.class);
        AuctionResultRepository results = mock(AuctionResultRepository.class);
        AuctionOrderRepository orders = mock(AuctionOrderRepository.class);
        BrandRepository brands = mock(BrandRepository.class);
        AuctionAdminQueryService service = new AuctionAdminQueryService(
                auctions, products, images, members, bids, results, orders, brands);
        Auction removed = auction(11L, 21L, AuctionStatus.SOLD);
        removed.setRemovedAt(LocalDateTime.of(2026, 9, 12, 10, 0));
        removed.setRemovedByMemberId(7L);
        removed.setRemoveReason("違規");
        removed.setRemovedFromStatus(AuctionStatus.SOLD);
        Auction deleted = auction(12L, 22L, AuctionStatus.SOLD);
        deleted.setRemovedAt(LocalDateTime.of(2026, 9, 12, 9, 0));
        deleted.setDeletedAt(LocalDateTime.of(2026, 9, 12, 11, 0));
        deleted.setDeletedByMemberId(7L);
        deleted.setDeleteReason("刪除");
        when(auctions.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(removed, deleted), PageRequest.of(0, 20), 2));
        AuctionProduct p1 = product(21L, 31L, "Jordan");
        AuctionProduct p2 = product(22L, 32L, "Dunk");
        when(products.findAllById(any())).thenReturn(List.of(p1, p2));
        when(members.findAllById(any())).thenReturn(List.of(
                member(31L, "aa"),
                member(32L, "bb")));
        when(images.findByAuctionProductIdInOrderByAuctionProductIdAscSortOrderAsc(anyList()))
                .thenReturn(List.of(
                        image(21L, "/images/jordan-main.jpg", 0),
                        image(21L, "/images/jordan-second.jpg", 1),
                        image(22L, "/images/dunk-main.jpg", 0)));
        when(orders.findByAuctionIdIn(anyList())).thenReturn(List.of(
                order(11L, AuctionOrderStatus.ABANDONED)));

        var result = service.search(new AuctionAdminSearchCriteria(),
                PageRequest.of(0, 200, Sort.by("notAllowed")));

        assertEquals(2, result.totalElements());
        assertEquals(AuctionAdminManagementState.REMOVED, result.items().get(0).managementState());
        assertEquals(AuctionStatus.SOLD, result.items().get(0).status());
        assertEquals("違規", result.items().get(0).removeReason());
        assertEquals("/images/jordan-main.jpg", result.items().get(0).imageUrl());
        assertEquals(31L, result.items().get(0).sellerId());
        assertEquals("aa", result.items().get(0).sellerName());
        assertEquals(AuctionOrderStatus.ABANDONED, result.items().get(0).orderStatus());
        assertEquals(AuctionAdminManagementState.DELETED, result.items().get(1).managementState());
        assertEquals("刪除", result.items().get(1).deleteReason());
        assertEquals("/images/dunk-main.jpg", result.items().get(1).imageUrl());
        assertEquals(32L, result.items().get(1).sellerId());
        assertEquals("bb", result.items().get(1).sellerName());
        assertNull(result.items().get(1).orderStatus());
        verify(products, times(1)).findAllById(any());
        verify(members, times(1)).findAllById(any());
        verify(members, never()).findById(any());
        verify(images, times(1))
                .findByAuctionProductIdInOrderByAuctionProductIdAscSortOrderAsc(anyList());
        verify(orders, times(1)).findByAuctionIdIn(anyList());
        verify(orders, never()).findByAuctionId(any());
        verify(auctions).findAll(any(Specification.class), org.mockito.ArgumentMatchers.<org.springframework.data.domain.Pageable>argThat(pageable ->
                pageable.getPageSize() == 100 && pageable.getSort().getOrderFor("createdAt") != null));
    }

    @Test
    void adminDetailIncludesRemovedAuctionAndAllOrderedImages() {
        AuctionRepository auctions = mock(AuctionRepository.class);
        AuctionProductRepository products = mock(AuctionProductRepository.class);
        AuctionProductImageRepository images = mock(AuctionProductImageRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        AuctionBidRepository bids = mock(AuctionBidRepository.class);
        AuctionResultRepository results = mock(AuctionResultRepository.class);
        AuctionOrderRepository orders = mock(AuctionOrderRepository.class);
        BrandRepository brands = mock(BrandRepository.class);
        AuctionAdminQueryService service = new AuctionAdminQueryService(
                auctions, products, images, members, bids, results, orders, brands);
        Auction removed = auction(13L, 23L, AuctionStatus.SOLD);
        removed.setRemovedAt(LocalDateTime.of(2026, 9, 12, 10, 0));
        AuctionProduct product = product(23L, 33L, "管理詳情測試鞋");
        when(auctions.findById(13L)).thenReturn(java.util.Optional.of(removed));
        when(products.findById(23L)).thenReturn(Optional.of(product));
        when(members.findAllById(any())).thenReturn(List.of(member(33L, "aa")));
        when(bids.findByAuctionIdOrderByCreatedAtDescBidIdDesc(13L)).thenReturn(List.of());
        when(results.findByAuctionId(13L)).thenReturn(Optional.empty());
        when(orders.findByAuctionId(13L)).thenReturn(Optional.empty());
        when(brands.findById(1L)).thenReturn(Optional.of(brand(1L, "Nike")));
        when(images.findByAuctionProductIdOrderBySortOrderAsc(23L)).thenReturn(List.of(
                image(23L, "/uploads/auctions/23/first.jpg", 0),
                image(23L, "/uploads/auctions/23/second.jpg", 1)));

        var result = service.detail(13L);

        assertEquals(13L, result.auction().auctionId());
        assertEquals(33L, result.auction().sellerId());
        assertEquals("aa", result.auction().sellerName());
        assertEquals(AuctionAdminManagementState.REMOVED, result.auction().managementState());
        assertEquals("/uploads/auctions/23/first.jpg", result.auction().imageUrl());
        assertEquals(List.of("/uploads/auctions/23/first.jpg",
                "/uploads/auctions/23/second.jpg"), result.imageUrls());
        assertEquals("Nike", result.brandName());
        assertEquals(List.of(), result.bids());
        assertNull(result.result());
        assertNull(result.order());
        verify(members).findAllById(any());
        verify(members, never()).findById(any());
    }

    @Test
    void adminDetailKeepsSellerIdAndUsesNullNameWhenMemberIsMissing() {
        AuctionRepository auctions = mock(AuctionRepository.class);
        AuctionProductRepository products = mock(AuctionProductRepository.class);
        AuctionProductImageRepository images = mock(AuctionProductImageRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        AuctionBidRepository bids = mock(AuctionBidRepository.class);
        AuctionResultRepository results = mock(AuctionResultRepository.class);
        AuctionOrderRepository orders = mock(AuctionOrderRepository.class);
        BrandRepository brands = mock(BrandRepository.class);
        AuctionAdminQueryService service = new AuctionAdminQueryService(
                auctions, products, images, members, bids, results, orders, brands);
        Auction auction = auction(14L, 24L, AuctionStatus.SOLD);
        AuctionProduct product = product(24L, 999L, "歷史競標");
        when(auctions.findById(14L)).thenReturn(Optional.of(auction));
        when(products.findById(24L)).thenReturn(Optional.of(product));
        when(members.findAllById(any())).thenReturn(List.of());
        when(bids.findByAuctionIdOrderByCreatedAtDescBidIdDesc(14L)).thenReturn(List.of());
        when(results.findByAuctionId(14L)).thenReturn(Optional.empty());
        when(orders.findByAuctionId(14L)).thenReturn(Optional.empty());
        when(brands.findById(1L)).thenReturn(Optional.empty());
        when(images.findByAuctionProductIdOrderBySortOrderAsc(24L))
                .thenReturn(List.of());

        var result = service.detail(14L);

        assertEquals(999L, result.auction().sellerId());
        assertNull(result.auction().sellerName());
    }

    @Test
    void adminDetailAggregatesOrderedBidsResultAndOrderWithoutUsingHighestBidderAsBuyer() {
        AuctionRepository auctions = mock(AuctionRepository.class);
        AuctionProductRepository products = mock(AuctionProductRepository.class);
        AuctionProductImageRepository images = mock(AuctionProductImageRepository.class);
        MemberRepository members = mock(MemberRepository.class);
        AuctionBidRepository bids = mock(AuctionBidRepository.class);
        AuctionResultRepository results = mock(AuctionResultRepository.class);
        AuctionOrderRepository orders = mock(AuctionOrderRepository.class);
        BrandRepository brands = mock(BrandRepository.class);
        AuctionAdminQueryService service = new AuctionAdminQueryService(
                auctions, products, images, members, bids, results, orders, brands);
        Auction auction = auction(15L, 25L, AuctionStatus.SOLD);
        auction.setHighestBidderId(999L);
        auction.setStartPrice(new BigDecimal("5000.00"));
        auction.setMinIncrement(new BigDecimal("500.00"));
        auction.setUpdatedAt(LocalDateTime.of(2026, 9, 13, 8, 0));
        AuctionProduct product = product(25L, 35L, "完整生命週期");
        AuctionBid latest = bid(15L, 36L, "6500.00", LocalDateTime.of(2026, 9, 13, 7, 59));
        AuctionBid earlier = bid(15L, 37L, "6000.00", LocalDateTime.of(2026, 9, 13, 7, 50));
        AuctionResult sold = new AuctionResult();
        sold.setResultStatus(AuctionResultStatus.SOLD);
        sold.setWinnerId(36L);
        sold.setFinalPrice(new BigDecimal("6500.00"));
        sold.setEndedAt(LocalDateTime.of(2026, 9, 13, 8, 0));
        sold.setCreatedAt(LocalDateTime.of(2026, 9, 13, 8, 0));
        AuctionOrder abandoned = order(15L, AuctionOrderStatus.ABANDONED);
        abandoned.setOrderNumber("AO-15");
        abandoned.setBuyerId(36L);
        abandoned.setSellerId(35L);
        abandoned.setFinalPrice(new BigDecimal("6500.00"));
        abandoned.setPaymentStatus(AuctionPaymentStatus.PENDING);
        abandoned.setShippingStatus(AuctionShippingStatus.NOT_SHIPPED);
        abandoned.setPaymentDueAt(LocalDateTime.of(2026, 9, 14, 8, 0));
        abandoned.setAbandonedAt(LocalDateTime.of(2026, 9, 14, 8, 1));
        when(auctions.findById(15L)).thenReturn(Optional.of(auction));
        when(products.findById(25L)).thenReturn(Optional.of(product));
        when(bids.findByAuctionIdOrderByCreatedAtDescBidIdDesc(15L)).thenReturn(List.of(latest, earlier));
        when(results.findByAuctionId(15L)).thenReturn(Optional.of(sold));
        when(orders.findByAuctionId(15L)).thenReturn(Optional.of(abandoned));
        when(members.findAllById(any())).thenReturn(List.of(
                member(35L, "賣家"), member(36L, "得標買家"), member(37L, "較早出價者")));
        when(images.findByAuctionProductIdOrderBySortOrderAsc(25L)).thenReturn(List.of());
        when(brands.findById(1L)).thenReturn(Optional.of(brand(1L, "Nike")));

        var detail = service.detail(15L);

        assertEquals(List.of("得標買家", "較早出價者"), detail.bids().stream()
                .map(bid -> bid.bidderName()).toList());
        assertEquals("得標買家", detail.result().winnerName());
        assertEquals("得標買家", detail.order().buyerName());
        assertEquals("賣家", detail.order().sellerName());
        assertEquals(AuctionOrderStatus.ABANDONED, detail.auction().orderStatus());
        assertEquals(AuctionPaymentStatus.PENDING, detail.order().paymentStatus());
        assertEquals(LocalDateTime.of(2026, 9, 14, 8, 1), detail.order().abandonedAt());
    }

    private Auction auction(Long id, Long productId, AuctionStatus status) {
        Auction auction = new Auction();
        auction.setAuctionId(id);
        auction.setAuctionProductId(productId);
        auction.setStatus(status);
        auction.setCurrentPrice(new BigDecimal("5000"));
        auction.setBidCount(0);
        auction.setStartAt(LocalDateTime.of(2026, 9, 12, 8, 0));
        auction.setEndAt(LocalDateTime.of(2026, 9, 13, 8, 0));
        auction.setCreatedAt(LocalDateTime.of(2026, 9, 12, 7, 0));
        return auction;
    }

    private AuctionProduct product(Long id, Long sellerId, String name) {
        AuctionProduct product = new AuctionProduct();
        product.setAuctionProductId(id);
        product.setSellerId(sellerId);
        product.setBrandId(1L);
        product.setProductName(name);
        product.setModel("MODEL");
        return product;
    }

    private Member member(Long id, String name) {
        Member member = new Member();
        member.setMemberId(id);
        member.setName(name);
        return member;
    }

    private AuctionProductImage image(Long productId, String url, int sortOrder) {
        AuctionProductImage image = new AuctionProductImage();
        image.setAuctionProductId(productId);
        image.setImageUrl(url);
        image.setSortOrder(sortOrder);
        return image;
    }

    private AuctionBid bid(Long auctionId, Long bidderId, String amount, LocalDateTime createdAt) {
        AuctionBid bid = new AuctionBid();
        bid.setAuctionId(auctionId);
        bid.setBidderId(bidderId);
        bid.setBidAmount(new BigDecimal(amount));
        bid.setCreatedAt(createdAt);
        return bid;
    }

    private AuctionOrder order(Long auctionId, AuctionOrderStatus status) {
        AuctionOrder order = new AuctionOrder();
        order.setAuctionId(auctionId);
        order.setOrderStatus(status);
        return order;
    }

    private Brand brand(Long id, String name) {
        Brand brand = new Brand();
        brand.setBrandId(id);
        brand.setBrandName(name);
        return brand;
    }
}
