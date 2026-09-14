package com.project.sneakermarket.auction.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.sneakermarket.auction.dto.AuctionAdminDetailResponse;
import com.project.sneakermarket.auction.dto.AuctionAdminManagementState;
import com.project.sneakermarket.auction.dto.AuctionAdminSearchCriteria;
import com.project.sneakermarket.auction.dto.AuctionAdminSummaryResponse;
import com.project.sneakermarket.auction.dto.PageData;
import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionProduct;
import com.project.sneakermarket.auction.entity.AuctionProductImage;
import com.project.sneakermarket.auction.entity.AuctionStatus;
import com.project.sneakermarket.auction.repository.AuctionAdminSpecification;
import com.project.sneakermarket.auction.repository.AuctionProductImageRepository;
import com.project.sneakermarket.auction.repository.AuctionProductRepository;
import com.project.sneakermarket.auction.repository.AuctionRepository;

import com.project.sneakermarket.common.exception.ResourceNotFoundException;
import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionAdminQueryService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "auctionId", "createdAt", "startAt", "endAt", "currentPrice", "bidCount",
            "status", "removedAt", "deletedAt");

    private final AuctionRepository auctions;
    private final AuctionProductRepository products;
    private final AuctionProductImageRepository images;
    private final MemberRepository members;

    public PageData<AuctionAdminSummaryResponse> search(
            AuctionAdminSearchCriteria criteria, Pageable pageable) {
        Pageable safePageable = safePageable(pageable);
        var page = auctions.findAll(AuctionAdminSpecification.withCriteria(criteria), safePageable);
        Map<Long, AuctionProduct> productMap = productMap(page.getContent());
        Map<Long, String> firstImageMap = firstImageMap(page.getContent());
        Map<Long, String> sellerNames = sellerNameMap(productMap);
        var items = page.getContent().stream()
                .map(auction -> {
                    AuctionProduct product = productMap.get(auction.getAuctionProductId());
                    String sellerName = product == null
                            ? null : sellerNames.get(product.getSellerId());
                    return toResponse(
                            auction,
                            product,
                            firstImageMap.get(auction.getAuctionProductId()),
                            sellerName);
                })
                .toList();
        return new PageData<>(items, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    public AuctionAdminDetailResponse detail(Long auctionId) {
        Auction auction = auctions.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到指定的競標資料"));
        AuctionProduct product = products.findById(auction.getAuctionProductId())
                .orElseThrow(() -> new ResourceNotFoundException("找不到此競標的商品資料"));
        String sellerName = members.findById(product.getSellerId())
                .map(Member::getName)
                .orElse(null);
        List<String> imageUrls = images.findByAuctionProductIdOrderBySortOrderAsc(
                        product.getAuctionProductId()).stream()
                .map(AuctionProductImage::getImageUrl)
                .toList();
        return new AuctionAdminDetailResponse(
                toResponse(
                        auction,
                        product,
                        imageUrls.isEmpty() ? null : imageUrls.get(0),
                        sellerName),
                product.getSize(), product.getCondition(), product.getDescription(),
                product.getDefectNote(), product.getAuthenticityNote(), product.getLocation(),
                product.getShippingMethod(), product.getShippingFee(), product.getPaymentMethod(),
                product.getAllowMeetup(), product.getAllowReturn(), product.getWarrantyNote(),
                imageUrls);
    }
    private Pageable safePageable(Pageable pageable) {
        List<Sort.Order> orders = pageable.getSort().stream()
                .filter(order -> ALLOWED_SORT_FIELDS.contains(order.getProperty()))
                .toList();
        Sort sort = orders.isEmpty()
                ? Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("auctionId"))
                : Sort.by(orders);
        return PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100), sort);
    }

    private Map<Long, AuctionProduct> productMap(List<Auction> page) {
        if (page.isEmpty()) return Map.of();
        return products.findAllById(productIds(page)).stream()
                .collect(Collectors.toMap(AuctionProduct::getAuctionProductId, Function.identity()));
    }

    private Map<Long, String> sellerNameMap(
            Map<Long, AuctionProduct> productMap) {
        List<Long> sellerIds = productMap.values().stream()
                .map(AuctionProduct::getSellerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (sellerIds.isEmpty()) return Map.of();
        return members.findAllById(sellerIds).stream()
                .collect(Collectors.toMap(Member::getMemberId, Member::getName));
    }

    private Map<Long, String> firstImageMap(List<Auction> page) {
        if (page.isEmpty()) return Map.of();
        return images.findByAuctionProductIdInOrderByAuctionProductIdAscSortOrderAsc(productIds(page))
                .stream()
                .collect(Collectors.toMap(
                        AuctionProductImage::getAuctionProductId,
                        AuctionProductImage::getImageUrl,
                        (first, ignored) -> first,
                        LinkedHashMap::new));
    }

    private List<Long> productIds(List<Auction> page) {
        return page.stream().map(Auction::getAuctionProductId).distinct().toList();
    }

    private AuctionAdminSummaryResponse toResponse(
            Auction auction, AuctionProduct product, String imageUrl,
            String sellerName) {
        return new AuctionAdminSummaryResponse(
                auction.getAuctionId(),
                product == null ? null : product.getProductName(),
                product == null ? null : product.getModel(),
                imageUrl,
                product == null ? null : product.getSellerId(),
                sellerName,
                auction.getCurrentPrice(), auction.getBidCount(), auction.getStatus(),
                auction.getStartAt(), auction.getEndAt(), auction.getCreatedAt(),
                auction.getRemovedAt(), auction.getRemovedByMemberId(), auction.getRemoveReason(),
                auction.getRemovedFromStatus(), auction.getDeletedAt(), auction.getDeletedByMemberId(),
                auction.getDeleteReason(), managementState(auction));
    }

    private AuctionAdminManagementState managementState(Auction auction) {
        if (auction.getDeletedAt() != null) return AuctionAdminManagementState.DELETED;
        if (auction.getRemovedAt() != null || auction.getStatus() == AuctionStatus.REMOVED) {
            return AuctionAdminManagementState.REMOVED;
        }
        return AuctionAdminManagementState.VISIBLE;
    }
}
