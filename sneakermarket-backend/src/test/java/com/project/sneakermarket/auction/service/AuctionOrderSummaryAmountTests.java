package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionOrder;
import com.project.sneakermarket.auction.entity.AuctionProduct;
import com.project.sneakermarket.auction.repository.AuctionBidRepository;
import com.project.sneakermarket.auction.repository.AuctionOrderRepository;
import com.project.sneakermarket.auction.repository.AuctionProductImageRepository;
import com.project.sneakermarket.auction.repository.AuctionProductRepository;
import com.project.sneakermarket.auction.repository.AuctionRepository;
import com.project.sneakermarket.auction.repository.AuctionResultRepository;
import com.project.sneakermarket.commerce.repository.BrandRepository;

class AuctionOrderSummaryAmountTests {
    @Test
    void buyerAndSellerSummariesIncludeShippingInOrderTotal() {
        var bids = mock(AuctionBidRepository.class);
        var auctions = mock(AuctionRepository.class);
        var products = mock(AuctionProductRepository.class);
        var results = mock(AuctionResultRepository.class);
        var orders = mock(AuctionOrderRepository.class);
        var images = mock(AuctionProductImageRepository.class);
        var brands = mock(BrandRepository.class);
        var service = new AuctionMemberQueryService(bids, auctions, products, results, orders, images, brands);

        var order = new AuctionOrder();
        order.setOrderNumber("AO-test");
        order.setAuctionId(3L);
        order.setFinalPrice(new BigDecimal("6500"));
        var auction = new Auction();
        auction.setAuctionId(3L);
        auction.setAuctionProductId(4L);
        var product = new AuctionProduct();
        product.setAuctionProductId(4L);
        product.setProductName("Test shoe");
        product.setShippingFee(new BigDecimal("120"));

        var page = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1);
        when(orders.findByBuyerId(eq(21L), any(Pageable.class))).thenReturn(page);
        when(orders.findBySellerId(eq(22L), any(Pageable.class))).thenReturn(page);
        when(auctions.findAllById(List.of(3L))).thenReturn(List.of(auction));
        when(products.findAllById(List.of(4L))).thenReturn(List.of(product));
        when(images.findByAuctionProductIdInOrderByAuctionProductIdAscSortOrderAsc(List.of(4L)))
                .thenReturn(List.of());

        for (var summary : List.of(service.getMyAuctionOrders(21L, 0, 10).items().get(0),
                service.getMyAuctionSales(22L, 0, 10).items().get(0))) {
            assertEquals(0, new BigDecimal("6500").compareTo(summary.finalPrice()));
            assertEquals(0, new BigDecimal("120").compareTo(summary.shippingFee()));
            assertEquals(0, new BigDecimal("6620").compareTo(summary.totalAmount()));
        }
    }
}