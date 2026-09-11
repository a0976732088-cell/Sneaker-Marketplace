package com.project.sneakermarket.auction.service;

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
import com.project.sneakermarket.common.exception.BusinessException;

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

    public PageData<AuctionMyBidResponse> getMyBids(Long memberId, int page, int size) {
        var summaries = bids.findMemberBidSummaries(memberId, pageRequest(page, size, Sort.unsorted()));
        var auctionMap = auctionMap(summaries.getContent().stream()
                .map(AuctionMemberBidSummary::getAuctionId).toList());
        var productMap = productMap(List.copyOf(auctionMap.values()));
        var resultMap = resultMap(List.copyOf(auctionMap.values()));
        return pageData(summaries.map(bid -> {
            Auction auction = auctionMap.get(bid.getAuctionId());
            AuctionProduct product = productMap.get(auction.getAuctionProductId());
            AuctionResult result = resultMap.get(auction.getAuctionId());
            return new AuctionMyBidResponse(
                    auction.getAuctionId(), product.getProductName(), product.getModel(), product.getSize(),
                    bid.getMyHighestBid(), auction.getCurrentPrice(), auction.getBidCount(), auction.getStatus(),
                    auction.getStatus() == AuctionStatus.ACTIVE && memberId.equals(auction.getHighestBidderId()),
                    result != null && result.getResultStatus() == AuctionResultStatus.SOLD
                            && memberId.equals(result.getWinnerId()),
                    result == null ? null : result.getResultStatus(),
                    result == null ? null : result.getFinalPrice(), bid.getLastBidAt(), auction.getEndAt());
        }));
    }

    public PageData<AuctionMyListingResponse> getMyListings(Long memberId, int page, int size) {
        var listings = auctions.findMemberListings(memberId,
                pageRequest(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("auctionId"))));
        var productMap = productMap(listings.getContent());
        var resultMap = resultMap(listings.getContent());
        return pageData(listings.map(auction -> {
            AuctionProduct product = productMap.get(auction.getAuctionProductId());
            AuctionResult result = resultMap.get(auction.getAuctionId());
            return new AuctionMyListingResponse(
                    auction.getAuctionId(), product.getProductName(), product.getModel(), product.getSize(),
                    auction.getCurrentPrice(), auction.getBidCount(), auction.getStatus(),
                    result == null ? null : result.getResultStatus(),
                    result == null ? null : result.getFinalPrice(),
                    auction.getCreatedAt(), auction.getStartAt(), auction.getEndAt());
        }));
    }

    public PageData<AuctionOrderSummaryResponse> getMyAuctionOrders(Long memberId, int page, int size) {
        var orderPage = orders.findByBuyerId(memberId,
                pageRequest(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("auctionOrderId"))));
        var auctionMap = auctionMap(orderPage.getContent().stream().map(AuctionOrder::getAuctionId).toList());
        var productMap = productMap(List.copyOf(auctionMap.values()));
        return pageData(orderPage.map(order -> new AuctionOrderSummaryResponse(
                order.getOrderNumber(), order.getAuctionId(),
                productMap.get(auctionMap.get(order.getAuctionId()).getAuctionProductId()).getProductName(),
                order.getFinalPrice(), order.getPaymentMethod(), order.getPaymentStatus(),
                order.getShippingStatus(), order.getOrderStatus(), order.getCreatedAt(), order.getUpdatedAt())));
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

    private <T> PageData<T> pageData(Page<T> page) {
        return new PageData<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
