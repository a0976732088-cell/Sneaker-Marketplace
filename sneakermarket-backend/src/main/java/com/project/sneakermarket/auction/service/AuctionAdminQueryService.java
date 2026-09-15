package com.project.sneakermarket.auction.service;

import java.util.ArrayList;
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
import com.project.sneakermarket.auction.dto.AuctionAdminOrderResponse;
import com.project.sneakermarket.auction.dto.AuctionAdminResultResponse;
import com.project.sneakermarket.auction.dto.AuctionAdminSearchCriteria;
import com.project.sneakermarket.auction.dto.AuctionAdminSummaryResponse;
import com.project.sneakermarket.auction.dto.BidHistoryResponse;
import com.project.sneakermarket.auction.dto.PageData;
import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionBid;
import com.project.sneakermarket.auction.entity.AuctionOrder;
import com.project.sneakermarket.auction.entity.AuctionOrderStatus;
import com.project.sneakermarket.auction.entity.AuctionProduct;
import com.project.sneakermarket.auction.entity.AuctionProductImage;
import com.project.sneakermarket.auction.entity.AuctionResult;
import com.project.sneakermarket.auction.entity.AuctionStatus;
import com.project.sneakermarket.auction.repository.AuctionAdminSpecification;
import com.project.sneakermarket.auction.repository.AuctionBidRepository;
import com.project.sneakermarket.auction.repository.AuctionOrderRepository;
import com.project.sneakermarket.auction.repository.AuctionProductImageRepository;
import com.project.sneakermarket.auction.repository.AuctionProductRepository;
import com.project.sneakermarket.auction.repository.AuctionRepository;
import com.project.sneakermarket.auction.repository.AuctionResultRepository;
import com.project.sneakermarket.commerce.entity.Brand;
import com.project.sneakermarket.commerce.repository.BrandRepository;

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
    private final AuctionBidRepository bids;
    private final AuctionResultRepository results;
    private final AuctionOrderRepository orders;
    private final BrandRepository brands;

    public PageData<AuctionAdminSummaryResponse> search(
            AuctionAdminSearchCriteria criteria, Pageable pageable) {
        Pageable safePageable = safePageable(pageable);
        var page = auctions.findAll(AuctionAdminSpecification.withCriteria(criteria), safePageable);
        Map<Long, AuctionProduct> productMap = productMap(page.getContent());
        Map<Long, String> firstImageMap = firstImageMap(page.getContent());
        Map<Long, String> sellerNames = sellerNameMap(productMap);
        Map<Long, AuctionOrderStatus> orderStatuses = orderStatusMap(page.getContent());
        var items = page.getContent().stream()
                .map(auction -> {
                    AuctionProduct product = productMap.get(auction.getAuctionProductId());
                    String sellerName = product == null
                            ? null : sellerNames.get(product.getSellerId());
                    return toResponse(
                            auction,
                            product,
                            firstImageMap.get(auction.getAuctionProductId()),
                            sellerName,
                            orderStatuses.get(auction.getAuctionId()));
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
        List<AuctionBid> bidHistory = bids.findByAuctionIdOrderByCreatedAtDescBidIdDesc(auctionId);
        AuctionResult result = results.findByAuctionId(auctionId).orElse(null);
        AuctionOrder order = orders.findByAuctionId(auctionId).orElse(null);
        List<Long> participantIds = new ArrayList<>();
        participantIds.add(product.getSellerId());
        participantIds.addAll(bidHistory.stream().map(AuctionBid::getBidderId).toList());
        if (result != null) participantIds.add(result.getWinnerId());
        if (order != null) {
            participantIds.add(order.getBuyerId());
            participantIds.add(order.getSellerId());
        }
        Map<Long, String> participantNames = memberNameMap(participantIds);
        String sellerName = participantNames.get(product.getSellerId());
        List<String> imageUrls = images.findByAuctionProductIdOrderBySortOrderAsc(
                        product.getAuctionProductId()).stream()
                .map(AuctionProductImage::getImageUrl)
                .toList();
        return new AuctionAdminDetailResponse(
                toResponse(
                        auction,
                        product,
                        imageUrls.isEmpty() ? null : imageUrls.get(0),
                        sellerName,
                        order == null ? null : order.getOrderStatus()),
                brands.findById(product.getBrandId()).map(Brand::getBrandName).orElse(null),
                product.getSize(), product.getCondition(), product.getDescription(),
                product.getDefectNote(), product.getAuthenticityNote(), product.getLocation(),
                product.getShippingMethod(), product.getShippingFee(), product.getPaymentMethod(),
                product.getAllowMeetup(), product.getAllowReturn(), product.getWarrantyNote(),
                imageUrls,
                auction.getStartPrice(), auction.getMinIncrement(), auction.getUpdatedAt(),
                bidHistory.stream()
                        .map(bid -> new BidHistoryResponse(
                                bid.getBidAmount(), bid.getCreatedAt(),
                                participantNames.get(bid.getBidderId())))
                        .toList(),
                toResultResponse(result, participantNames),
                toOrderResponse(order, participantNames));
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
        return memberNameMap(sellerIds);
    }

    private Map<Long, String> memberNameMap(List<Long> memberIds) {
        List<Long> distinctIds = memberIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinctIds.isEmpty()) return Map.of();
        return members.findAllById(distinctIds).stream()
                .collect(Collectors.toMap(Member::getMemberId, Member::getName));
    }

    private Map<Long, AuctionOrderStatus> orderStatusMap(List<Auction> page) {
        List<Long> auctionIds = page.stream().map(Auction::getAuctionId).toList();
        if (auctionIds.isEmpty()) return Map.of();
        return orders.findByAuctionIdIn(auctionIds).stream()
                .collect(Collectors.toMap(AuctionOrder::getAuctionId, AuctionOrder::getOrderStatus));
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
            String sellerName, AuctionOrderStatus orderStatus) {
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
                auction.getDeleteReason(), orderStatus, managementState(auction));
    }

    private AuctionAdminResultResponse toResultResponse(
            AuctionResult result, Map<Long, String> participantNames) {
        if (result == null) return null;
        return new AuctionAdminResultResponse(
                result.getResultStatus(), result.getFinalPrice(),
                participantNames.get(result.getWinnerId()), result.getEndedAt(),
                result.getCreatedAt(), result.getReason());
    }

    private AuctionAdminOrderResponse toOrderResponse(
            AuctionOrder order, Map<Long, String> participantNames) {
        if (order == null) return null;
        return new AuctionAdminOrderResponse(
                order.getOrderNumber(), order.getOrderStatus(), order.getPaymentStatus(),
                order.getShippingStatus(), order.getFinalPrice(),
                participantNames.get(order.getBuyerId()), participantNames.get(order.getSellerId()),
                order.getCreatedAt(), order.getUpdatedAt(), order.getPaymentDueAt(),
                order.getPaidAt(), order.getAbandonedAt(), order.getShippedAt(),
                order.getCompletedAt());
    }

    private AuctionAdminManagementState managementState(Auction auction) {
        if (auction.getDeletedAt() != null) return AuctionAdminManagementState.DELETED;
        if (auction.getRemovedAt() != null || auction.getStatus() == AuctionStatus.REMOVED) {
            return AuctionAdminManagementState.REMOVED;
        }
        return AuctionAdminManagementState.VISIBLE;
    }
}
