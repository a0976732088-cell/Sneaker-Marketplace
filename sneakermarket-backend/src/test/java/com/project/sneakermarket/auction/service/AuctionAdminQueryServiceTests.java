package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.project.sneakermarket.auction.dto.AuctionAdminManagementState;
import com.project.sneakermarket.auction.dto.AuctionAdminSearchCriteria;
import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionProduct;
import com.project.sneakermarket.auction.entity.AuctionProductImage;
import com.project.sneakermarket.auction.entity.AuctionStatus;
import com.project.sneakermarket.auction.repository.AuctionProductImageRepository;
import com.project.sneakermarket.auction.repository.AuctionProductRepository;
import com.project.sneakermarket.auction.repository.AuctionRepository;

class AuctionAdminQueryServiceTests {
    @Test
    void adminListIncludesMetadataAndFirstImagesWithBatchQueries() {
        AuctionRepository auctions = mock(AuctionRepository.class);
        AuctionProductRepository products = mock(AuctionProductRepository.class);
        AuctionProductImageRepository images = mock(AuctionProductImageRepository.class);
        AuctionAdminQueryService service = new AuctionAdminQueryService(auctions, products, images);
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
        when(images.findByAuctionProductIdInOrderByAuctionProductIdAscSortOrderAsc(anyList()))
                .thenReturn(List.of(
                        image(21L, "/images/jordan-main.jpg", 0),
                        image(21L, "/images/jordan-second.jpg", 1),
                        image(22L, "/images/dunk-main.jpg", 0)));

        var result = service.search(new AuctionAdminSearchCriteria(),
                PageRequest.of(0, 200, Sort.by("notAllowed")));

        assertEquals(2, result.totalElements());
        assertEquals(AuctionAdminManagementState.REMOVED, result.items().get(0).managementState());
        assertEquals(AuctionStatus.SOLD, result.items().get(0).status());
        assertEquals("違規", result.items().get(0).removeReason());
        assertEquals("/images/jordan-main.jpg", result.items().get(0).imageUrl());
        assertEquals(AuctionAdminManagementState.DELETED, result.items().get(1).managementState());
        assertEquals("刪除", result.items().get(1).deleteReason());
        assertEquals("/images/dunk-main.jpg", result.items().get(1).imageUrl());
        verify(products, times(1)).findAllById(any());
        verify(images, times(1))
                .findByAuctionProductIdInOrderByAuctionProductIdAscSortOrderAsc(anyList());
        verify(auctions).findAll(any(Specification.class), org.mockito.ArgumentMatchers.<org.springframework.data.domain.Pageable>argThat(pageable ->
                pageable.getPageSize() == 100 && pageable.getSort().getOrderFor("createdAt") != null));
    }

    @Test
    void adminDetailIncludesRemovedAuctionAndAllOrderedImages() {
        AuctionRepository auctions = mock(AuctionRepository.class);
        AuctionProductRepository products = mock(AuctionProductRepository.class);
        AuctionProductImageRepository images = mock(AuctionProductImageRepository.class);
        AuctionAdminQueryService service = new AuctionAdminQueryService(auctions, products, images);
        Auction removed = auction(13L, 23L, AuctionStatus.SOLD);
        removed.setRemovedAt(LocalDateTime.of(2026, 9, 12, 10, 0));
        AuctionProduct product = product(23L, 33L, "管理詳情測試鞋");
        when(auctions.findById(13L)).thenReturn(java.util.Optional.of(removed));
        when(products.findById(23L)).thenReturn(java.util.Optional.of(product));
        when(images.findByAuctionProductIdOrderBySortOrderAsc(23L)).thenReturn(List.of(
                image(23L, "/uploads/auctions/23/first.jpg", 0),
                image(23L, "/uploads/auctions/23/second.jpg", 1)));

        var result = service.detail(13L);

        assertEquals(13L, result.auction().auctionId());
        assertEquals(AuctionAdminManagementState.REMOVED, result.auction().managementState());
        assertEquals("/uploads/auctions/23/first.jpg", result.auction().imageUrl());
        assertEquals(List.of("/uploads/auctions/23/first.jpg",
                "/uploads/auctions/23/second.jpg"), result.imageUrls());
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
        product.setProductName(name);
        product.setModel("MODEL");
        return product;
    }

    private AuctionProductImage image(Long productId, String url, int sortOrder) {
        AuctionProductImage image = new AuctionProductImage();
        image.setAuctionProductId(productId);
        image.setImageUrl(url);
        image.setSortOrder(sortOrder);
        return image;
    }
}
