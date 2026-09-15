package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.project.sneakermarket.auction.controller.AuctionOrderController;
import com.project.sneakermarket.auction.entity.*;
import com.project.sneakermarket.auction.exception.AuctionExceptionHandler;
import com.project.sneakermarket.auction.repository.*;
import com.project.sneakermarket.commerce.entity.Brand;
import com.project.sneakermarket.commerce.repository.BrandRepository;
import com.project.sneakermarket.common.exception.GlobalExceptionHandler;

class AuctionOrderDetailTests {
    private AuctionOrderRepository orders;
    private AuctionRepository auctions;
    private AuctionProductRepository products;
    private AuctionProductImageRepository images;
    private BrandRepository brands;
    private AuctionReferenceService references;
    private MockMvc mvc;
    private AuctionOrder order;
    private Auction auction;
    private AuctionProduct product;

    @BeforeEach
    void setup() {
        orders = mock(AuctionOrderRepository.class);
        auctions = mock(AuctionRepository.class);
        products = mock(AuctionProductRepository.class);
        images = mock(AuctionProductImageRepository.class);
        brands = mock(BrandRepository.class);
        references = mock(AuctionReferenceService.class);
        AuctionMemberQueryService queries = new AuctionMemberQueryService(
                mock(AuctionBidRepository.class), auctions, products,
                mock(AuctionResultRepository.class), orders, images, brands);
        mvc = MockMvcBuilders.standaloneSetup(
                new AuctionOrderController(queries, mock(AuctionOrderPaymentService.class),
                        new AuctionSessionMemberResolver(references)))
                .setControllerAdvice(new AuctionExceptionHandler(), new GlobalExceptionHandler())
                .build();

        order = new AuctionOrder();
        order.setOrderNumber("AO-123456");
        order.setAuctionId(7L);
        order.setBuyerId(10L);
        order.setFinalPrice(new BigDecimal("6500.00"));
        order.setPaymentMethod("Transfer");
        order.setPaymentStatus(AuctionPaymentStatus.PENDING);
        order.setShippingStatus(AuctionShippingStatus.NOT_SHIPPED);
        order.setOrderStatus(AuctionOrderStatus.PENDING_PAYMENT);
        order.setReceiverName("收件人");
        order.setReceiverPhone("0912345678");
        order.setShippingAddress("台北市測試路 1 號");
        order.setCreatedAt(LocalDateTime.of(2026, 9, 14, 10, 0));
        order.setUpdatedAt(LocalDateTime.of(2026, 9, 14, 10, 0));
        order.setPaymentDueAt(LocalDateTime.of(2026, 9, 15, 10, 0));

        auction = new Auction();
        auction.setAuctionId(7L);
        auction.setAuctionProductId(8L);

        product = new AuctionProduct();
        product.setAuctionProductId(8L);
        product.setBrandId(9L);
        product.setProductName("Jordan 1");
        product.setModel("Retro High");
        product.setSize("US 9");
        product.setShippingMethod("Delivery");
        product.setShippingFee(new BigDecimal("120.00"));
    }

    @Test
    void buyerGetsDetailWithOrderPriceProductShippingAndTotal() throws Exception {
        stubDetail();
        var brand = new Brand();
        brand.setBrandName("Nike");
        when(brands.findById(9L)).thenReturn(Optional.of(brand));
        var image = new AuctionProductImage();
        image.setImageUrl("/uploads/jordan.jpg");
        when(images.findByAuctionProductIdOrderBySortOrderAsc(8L)).thenReturn(List.of(image));

        mvc.perform(get("/api/auction-orders/AO-123456").session(activeSession(10L))
                        .param("buyerId", "99").param("memberId", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").value("AO-123456"))
                .andExpect(jsonPath("$.data.auctionId").value(7))
                .andExpect(jsonPath("$.data.productName").value("Jordan 1"))
                .andExpect(jsonPath("$.data.brandId").value(9))
                .andExpect(jsonPath("$.data.brandName").value("Nike"))
                .andExpect(jsonPath("$.data.model").value("Retro High"))
                .andExpect(jsonPath("$.data.size").value("US 9"))
                .andExpect(jsonPath("$.data.imageUrl").value("/uploads/jordan.jpg"))
                .andExpect(jsonPath("$.data.finalPrice").value(6500.00))
                .andExpect(jsonPath("$.data.shippingFee").value(120.00))
                .andExpect(jsonPath("$.data.totalAmount").value(6620.00))
                .andExpect(jsonPath("$.data.shippingMethod").value("Delivery"))
                .andExpect(jsonPath("$.data.paymentMethod").value("Transfer"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.shippingStatus").value("NOT_SHIPPED"))
                .andExpect(jsonPath("$.data.orderStatus").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.receiverName").value("收件人"))
                .andExpect(jsonPath("$.data.receiverPhone").value("0912345678"))
                .andExpect(jsonPath("$.data.shippingAddress").value("台北市測試路 1 號"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-09-14T10:00:00"))
                .andExpect(jsonPath("$.data.paymentDueAt").value("2026-09-15T10:00:00"))
                .andExpect(jsonPath("$.data.abandonedAt").doesNotExist())
                .andExpect(jsonPath("$.data.updatedAt").value("2026-09-14T10:00:00"))
                .andExpect(jsonPath("$.data.buyerId").doesNotExist())
                .andExpect(jsonPath("$.data.sellerId").doesNotExist());
        verify(orders).findByOrderNumber("AO-123456");
        verify(orders, never()).save(any());
    }

    @Test
    void anonymousGets401WithoutCreatingSession() throws Exception {
        var result = mvc.perform(get("/api/auction-orders/AO-123456"))
                .andExpect(status().isUnauthorized()).andReturn();
        assertNull(result.getRequest().getSession(false));
        verifyNoInteractions(orders, auctions, products, images, brands);
    }

    @Test
    void inactiveMemberGetsExisting401Rule() throws Exception {
        mvc.perform(get("/api/auction-orders/AO-123456").session(session(10L)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(orders, auctions, products, images, brands);
    }

    @Test
    void differentBuyerGets403BeforeProductLookup() throws Exception {
        when(orders.findByOrderNumber("AO-123456")).thenReturn(Optional.of(order));
        mvc.perform(get("/api/auction-orders/AO-123456").session(activeSession(11L))
                        .param("buyerId", "10").param("memberId", "10"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(auctions, products, images, brands);
    }

    @Test
    void missingOrderGets404() throws Exception {
        mvc.perform(get("/api/auction-orders/AO-missing").session(activeSession(10L)))
                .andExpect(status().isNotFound());
        verify(orders).findByOrderNumber("AO-missing");
        verifyNoInteractions(auctions, products, images, brands);
    }

    @Test
    void optionalProductFieldsMayBeAbsent() throws Exception {
        product.setModel(null);
        stubDetail();
        when(brands.findById(9L)).thenReturn(Optional.empty());
        when(images.findByAuctionProductIdOrderBySortOrderAsc(8L)).thenReturn(List.of());
        mvc.perform(get("/api/auction-orders/AO-123456").session(activeSession(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productName").value("Jordan 1"))
                .andExpect(jsonPath("$.data.finalPrice").value(6500.00))
                .andExpect(jsonPath("$.data.shippingFee").value(120.00))
                .andExpect(jsonPath("$.data.totalAmount").value(6620.00));
    }

    @Test
    void abandonedDetailIncludesDeadlineAndAbandonedTimestampWithoutIdentityLeak() throws Exception {
        order.setOrderStatus(AuctionOrderStatus.ABANDONED);
        order.setAbandonedAt(LocalDateTime.of(2026, 9, 15, 10, 0));
        order.setUpdatedAt(order.getAbandonedAt());
        stubDetail();

        mvc.perform(get("/api/auction-orders/AO-123456").session(activeSession(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("ABANDONED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.paymentDueAt").value("2026-09-15T10:00:00"))
                .andExpect(jsonPath("$.data.abandonedAt").value("2026-09-15T10:00:00"))
                .andExpect(jsonPath("$.data.buyerId").doesNotExist())
                .andExpect(jsonPath("$.data.sellerId").doesNotExist())
                .andExpect(jsonPath("$.data.buyerEmail").doesNotExist())
                .andExpect(jsonPath("$.data.sellerEmail").doesNotExist());
    }

    @Test
    void existingMeRouteStillReturnsSummaryWithoutReceiverData() throws Exception {
        when(orders.findByBuyerId(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(auctions.findAllById(List.of(7L))).thenReturn(List.of(auction));
        when(products.findAllById(List.of(8L))).thenReturn(List.of(product));

        mvc.perform(get("/api/auction-orders/me").session(activeSession(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].orderNumber").value("AO-123456"))
                .andExpect(jsonPath("$.data.items[0].productName").value("Jordan 1"))
                .andExpect(jsonPath("$.data.items[0].receiverName").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].shippingFee").doesNotExist());
        verify(orders, never()).findByOrderNumber("me");
    }

    private void stubDetail() {
        when(orders.findByOrderNumber("AO-123456")).thenReturn(Optional.of(order));
        when(auctions.findById(7L)).thenReturn(Optional.of(auction));
        when(products.findById(8L)).thenReturn(Optional.of(product));
    }

    private MockHttpSession activeSession(Long memberId) {
        when(references.isActiveMember(memberId)).thenReturn(true);
        return session(memberId);
    }

    private MockHttpSession session(Long memberId) {
        var session = new MockHttpSession();
        session.setAttribute("memberId", memberId);
        return session;
    }
}
