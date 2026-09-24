package com.project.sneakermarket.auction.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.sneakermarket.auction.dto.*;
import com.project.sneakermarket.auction.entity.*;
import com.project.sneakermarket.auction.repository.*;
import com.project.sneakermarket.commerce.repository.BrandRepository;
import com.project.sneakermarket.common.exception.BusinessException;
import com.project.sneakermarket.common.exception.ForbiddenException;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/** Session 會員的唯讀歷史；不依賴建單是否成功，也不觸發結標或建單。 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionMemberQueryService {
    private final AuctionBidRepository bids;
    private final AuctionRepository auctions;
    private final AuctionProductRepository products;
    private final AuctionResultRepository results;
    private final AuctionOrderRepository orders;
    private final AuctionProductImageRepository images;
    private final BrandRepository brands;

    public PageData<AuctionMyBidResponse> getMyBids(Long memberId, int page, int size) {
        var summaries = bids.findMemberBidSummaries(memberId, pageRequest(page, size, Sort.unsorted()));
        var auctionMap = auctionMap(summaries.getContent().stream()
                .map(AuctionMemberBidSummary::getAuctionId).toList());
        var productMap = productMap(List.copyOf(auctionMap.values()));
        var resultMap = resultMap(List.copyOf(auctionMap.values()));
        var imageUrlMap = imageUrlMap(List.copyOf(auctionMap.values()));
        return pageData(summaries.map(bid -> {
            Auction auction = auctionMap.get(bid.getAuctionId());
            AuctionProduct product = productMap.get(auction.getAuctionProductId());
            AuctionResult result = resultMap.get(auction.getAuctionId());
            AuctionAdminManagementState managementState = managementState(auction);
            return new AuctionMyBidResponse(
                    auction.getAuctionId(), product.getProductName(), product.getModel(), product.getSize(),
                    bid.getMyHighestBid(), auction.getCurrentPrice(), auction.getBidCount(), auction.getStatus(),
                    managementState,
                    managementState == AuctionAdminManagementState.VISIBLE
                            && auction.getStatus() == AuctionStatus.ACTIVE
                            && memberId.equals(auction.getHighestBidderId()),
                    result != null && result.getResultStatus() == AuctionResultStatus.SOLD
                            && memberId.equals(result.getWinnerId()),
                    result == null ? null : result.getResultStatus(),
                    result == null ? null : result.getFinalPrice(), bid.getLastBidAt(), auction.getEndAt(),
                    imageUrlMap.get(auction.getAuctionProductId()));
        }));
    }

    public PageData<AuctionMyListingResponse> getMyListings(Long memberId, int page, int size) {
        var listings = auctions.findMemberListings(memberId,
                pageRequest(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("auctionId"))));
        var productMap = productMap(listings.getContent());
        var resultMap = resultMap(listings.getContent());
        var imageUrlMap = imageUrlMap(listings.getContent());
        return pageData(listings.map(auction -> {
            AuctionProduct product = productMap.get(auction.getAuctionProductId());
            AuctionResult result = resultMap.get(auction.getAuctionId());
            AuctionAdminManagementState managementState = managementState(auction);
            return new AuctionMyListingResponse(
                    auction.getAuctionId(), product.getProductName(), product.getModel(), product.getSize(),
                    auction.getCurrentPrice(), auction.getBidCount(), auction.getStatus(),
                    managementState,
                    result == null ? null : result.getResultStatus(),
                    result == null ? null : result.getFinalPrice(),
                    auction.getCreatedAt(), auction.getStartAt(), auction.getEndAt(),
                    imageUrlMap.get(auction.getAuctionProductId()));
        }));
    }

    private AuctionAdminManagementState managementState(Auction auction) {
        if (auction.getDeletedAt() != null) return AuctionAdminManagementState.DELETED;
        if (auction.getRemovedAt() != null || auction.getStatus() == AuctionStatus.REMOVED) {
            return AuctionAdminManagementState.REMOVED;
        }
        return AuctionAdminManagementState.VISIBLE;
    }

    public PageData<AuctionOrderSummaryResponse> getMyAuctionOrders(Long memberId, int page, int size) {
        var orderPage = orders.findByBuyerId(memberId,
                pageRequest(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("auctionOrderId"))));
        return orderSummaries(orderPage);
    }

    public PageData<AuctionOrderSummaryResponse> getMyAuctionSales(Long memberId, int page, int size) {
        var orderPage = orders.findBySellerId(memberId,
                pageRequest(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("auctionOrderId"))));
        return orderSummaries(orderPage);
    }

    private PageData<AuctionOrderSummaryResponse> orderSummaries(Page<AuctionOrder> orderPage) {
        var auctionMap = auctionMap(orderPage.getContent().stream().map(AuctionOrder::getAuctionId).toList());
        var productMap = productMap(List.copyOf(auctionMap.values()));
        var imageUrlMap = imageUrlMap(List.copyOf(auctionMap.values()));
        return pageData(orderPage.map(order -> {
            Auction auction = auctionMap.get(order.getAuctionId());
            AuctionProduct product = productMap.get(auction.getAuctionProductId());
            return new AuctionOrderSummaryResponse(
                    order.getOrderNumber(), order.getAuctionId(), product.getProductName(),
                    order.getFinalPrice(), product.getShippingFee(),
                    order.getFinalPrice().add(product.getShippingFee()),
                    order.getPaymentMethod(), order.getPaymentStatus(),
                    order.getShippingStatus(), order.getOrderStatus(), order.getCreatedAt(), order.getUpdatedAt(),
                    product.getModel(), product.getSize(), imageUrlMap.get(product.getAuctionProductId()),
                    order.getPaymentDueAt(), managementState(auction));
        }));
    }

    public AuctionOrderDetailResponse getAuctionOrderDetail(Long memberId, String orderNumber) {
        AuctionOrder order = orders.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("找不到指定的競標訂單"));
        boolean isBuyer = order.getBuyerId().equals(memberId);
        boolean isSeller = order.getSellerId().equals(memberId);
        if (!isBuyer && !isSeller) {
            throw new ForbiddenException("無權查看此競標訂單");
        }
        AuctionOrderViewerRole viewerRole =
                isBuyer ? AuctionOrderViewerRole.BUYER : AuctionOrderViewerRole.SELLER;
        boolean receiverInfoVisible = isBuyer
                || (order.getPaymentStatus() == AuctionPaymentStatus.PAID
                        && order.getOrderStatus() == AuctionOrderStatus.PAID);

        Auction auction = auctions.findById(order.getAuctionId())
                .orElseThrow(() -> new ResourceNotFoundException("找不到此訂單的競標資料"));
        AuctionProduct product = products.findById(auction.getAuctionProductId())
                .orElseThrow(() -> new ResourceNotFoundException("找不到此訂單的商品資料"));
        String brandName = brands.findById(product.getBrandId())
                .map(brand -> brand.getBrandName()).orElse(null);
        String imageUrl = images.findByAuctionProductIdOrderBySortOrderAsc(product.getAuctionProductId())
                .stream().map(AuctionProductImage::getImageUrl).findFirst().orElse(null);

        return new AuctionOrderDetailResponse(
                order.getOrderNumber(), order.getAuctionId(), product.getProductName(),
                product.getBrandId(), brandName, product.getModel(), product.getSize(), imageUrl,
                order.getFinalPrice(), product.getShippingFee(),
                order.getFinalPrice().add(product.getShippingFee()),
                product.getShippingMethod(), order.getPaymentMethod(),
                order.getPaymentStatus(), order.getShippingStatus(), order.getOrderStatus(),
                viewerRole, receiverInfoVisible,
                receiverInfoVisible ? order.getReceiverName() : null,
                receiverInfoVisible ? order.getReceiverPhone() : null,
                receiverInfoVisible ? order.getShippingAddress() : null,
                order.getCreatedAt(), order.getPaymentDueAt(), order.getAbandonedAt(),
                order.getPaidAt(), order.getUpdatedAt());
    }

    private PageRequest pageRequest(int page, int size, Sort sort) {
        if (page < 0 || size < 1) throw new BusinessException("page 不可小於 0，size 必須大於 0");
        // 僅使用固定排序；即使客戶端傳入 sort，也不影響會員列表契約。
        return PageRequest.of(page, Math.min(size, 100), sort);
    }

    private Map<Long, Auction> auctionMap(List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return auctions.findAllById(ids).stream()
                .collect(Collectors.toMap(Auction::getAuctionId, Function.identity()));
    }

    private Map<Long, AuctionProduct> productMap(List<Auction> page) {
        if (page.isEmpty()) return Map.of();
        return products.findAllById(page.stream().map(Auction::getAuctionProductId).distinct().toList()).stream()
                .collect(Collectors.toMap(AuctionProduct::getAuctionProductId, Function.identity()));
    }

    private Map<Long, AuctionResult> resultMap(List<Auction> page) {
        if (page.isEmpty()) return Map.of();
        return results.findByAuctionIdIn(page.stream().map(Auction::getAuctionId).toList()).stream()
                .collect(Collectors.toMap(AuctionResult::getAuctionId, Function.identity()));
    }

    /** 與公開商城列表相同：主圖優先，否則使用 sortOrder 最前面的圖片。 */
    private Map<Long, String> imageUrlMap(List<Auction> page) {
        List<Long> productIds = page.stream()
                .map(Auction::getAuctionProductId)
                .distinct()
                .toList();
        if (productIds.isEmpty()) return Map.of();

        Map<Long, AuctionProductImage> selectedImages = new LinkedHashMap<>();
        images.findByAuctionProductIdInOrderByAuctionProductIdAscSortOrderAsc(productIds)
                .forEach(image -> selectedImages.merge(
                        image.getAuctionProductId(),
                        image,
                        (selected, candidate) -> Boolean.TRUE.equals(candidate.getIsPrimary())
                                && !Boolean.TRUE.equals(selected.getIsPrimary()) ? candidate : selected));

        return selectedImages.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getImageUrl()));
    }

    private <T> PageData<T> pageData(Page<T> page) {
        return new PageData<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
