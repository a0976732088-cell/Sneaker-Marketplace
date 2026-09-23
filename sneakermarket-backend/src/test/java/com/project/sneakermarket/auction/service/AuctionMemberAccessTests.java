package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.project.sneakermarket.auction.controller.*;
import com.project.sneakermarket.auction.dto.*;
import com.project.sneakermarket.auction.entity.*;
import com.project.sneakermarket.auction.exception.AuctionExceptionHandler;
import com.project.sneakermarket.common.exception.GlobalExceptionHandler;

class AuctionMemberAccessTests {
    private AuctionReferenceService references;
    private AuctionMemberQueryService queries;
    private AuctionSessionMemberResolver resolver;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        references = mock(AuctionReferenceService.class);
        queries = mock(AuctionMemberQueryService.class);
        resolver = new AuctionSessionMemberResolver(references);
        // 同時註冊既有 Controller，確認 /me/bids 不會被 /{auctionId}/bids 攔截。
        mvc = MockMvcBuilders.standaloneSetup(
                new AuctionMemberController(queries, resolver),
                new AuctionOrderController(queries, mock(AuctionOrderCheckoutService.class),
                        mock(AuctionOrderPaymentService.class), resolver),
                new AuctionController(mock(AuctionQueryService.class), mock(AuctionCommandService.class),
                        mock(AuctionBidService.class), resolver, references))
                .setControllerAdvice(new AuctionExceptionHandler(), new GlobalExceptionHandler()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/auctions/me/bids", "/api/auctions/me/listings", "/api/auction-orders/me",
            "/api/auction-orders/me/sales"})
    void anonymousReturns401WithoutCreatingSession(String path) throws Exception {
        var response = mvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false)).andReturn();
        assertNull(response.getRequest().getSession(false));
        verifyNoInteractions(queries);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/auctions/me/bids", "/api/auctions/me/listings", "/api/auction-orders/me",
            "/api/auction-orders/me/sales"})
    void malformedMissingAndInactiveSessionMembersReturn401(String path) throws Exception {
        var session = new MockHttpSession();
        for (Object id : List.of("77", 77, 0L, -1L, 999L)) {
            session.setAttribute("memberId", id);
            mvc.perform(get(path).session(session)).andExpect(status().isUnauthorized());
        }
        session.removeAttribute("memberId");
        mvc.perform(get(path).session(session)).andExpect(status().isUnauthorized());
        verifyNoInteractions(queries);
    }

    @Test
    void invalidatedSessionUsesExistingUnauthorizedSemantics() {
        var session = new MockHttpSession();
        session.setAttribute("memberId", 77L);
        session.invalidate();
        assertThrows(com.project.sneakermarket.auction.exception.AuctionUnauthorizedException.class,
                () -> resolver.resolveMemberId(session));
        verifyNoInteractions(queries);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/auctions/me/bids", "/api/auctions/me/listings", "/api/auction-orders/me",
            "/api/auction-orders/me/sales"})
    void sessionIsSoleIdentityEvenWithForgedQueryParameters(String path) throws Exception {
        var session = activeSession();
        when(queries.getMyBids(77L, 2, 3)).thenReturn(new PageData<>(List.of(), 2, 3, 0, 0));
        when(queries.getMyListings(77L, 2, 3)).thenReturn(new PageData<>(List.of(), 2, 3, 0, 0));
        when(queries.getMyAuctionOrders(77L, 2, 3)).thenReturn(new PageData<>(List.of(), 2, 3, 0, 0));
        when(queries.getMyAuctionSales(77L, 2, 3)).thenReturn(new PageData<>(List.of(), 2, 3, 0, 0));
        mvc.perform(get(path).session(session).param("page", "2").param("size", "3")
                        .param("memberId", "999").param("bidderId", "999").param("sellerId", "999")
                        .param("buyerId", "999").param("winnerId", "999").param("highestBidderId", "999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.page").value(2)).andExpect(jsonPath("$.data.size").value(3));
        if (path.endsWith("/bids")) verify(queries).getMyBids(77L, 2, 3);
        else if (path.endsWith("/listings")) verify(queries).getMyListings(77L, 2, 3);
        else if (path.endsWith("/sales")) verify(queries).getMyAuctionSales(77L, 2, 3);
        else verify(queries).getMyAuctionOrders(77L, 2, 3);
        verifyNoMoreInteractions(queries);
    }

    @Test
    void defaultPaginationAndAllResponsesExposeOnlySummaryFields() throws Exception {
        var at = LocalDateTime.of(2026, 9, 11, 12, 0);
        var dueAt = at.plusDays(1);
        var money = new BigDecimal("6500");
        when(queries.getMyBids(77L, 0, 10)).thenReturn(new PageData<>(List.of(
                new AuctionMyBidResponse(1L, "Shoe", "Model", "US 9", money, money, 3,
                        AuctionStatus.SOLD, AuctionAdminManagementState.VISIBLE, false, true,
                        AuctionResultStatus.SOLD, money, at, at, "/images/bid.jpg")), 0, 10, 1, 1));
        when(queries.getMyListings(77L, 0, 10)).thenReturn(new PageData<>(List.of(
                new AuctionMyListingResponse(1L, "Shoe", "Model", "US 9", money, 3,
                        AuctionStatus.SOLD, AuctionAdminManagementState.VISIBLE, AuctionResultStatus.SOLD,
                        money, at, at, at, "/images/listing.jpg")), 0, 10, 1, 1));
        when(queries.getMyAuctionOrders(77L, 0, 10)).thenReturn(new PageData<>(List.of(
                new AuctionOrderSummaryResponse("AO-test", 1L, "Shoe", money,
                        new BigDecimal("120"), new BigDecimal("6620"), "Transfer",
                        AuctionPaymentStatus.PENDING, AuctionShippingStatus.NOT_SHIPPED,
                        AuctionOrderStatus.PENDING_PAYMENT, at, at,
                        "Order Model", "US 10", "/images/order.jpg", dueAt,
                        AuctionAdminManagementState.VISIBLE)), 0, 10, 1, 1));
        when(queries.getMyAuctionSales(77L, 0, 10)).thenReturn(new PageData<>(List.of(
                new AuctionOrderSummaryResponse("AO-sale", 1L, "Shoe", money,
                        new BigDecimal("120"), new BigDecimal("6620"), "Transfer",
                        AuctionPaymentStatus.PAID, AuctionShippingStatus.NOT_SHIPPED,
                        AuctionOrderStatus.PAID, at, at,
                        "Sale Model", "US 11", "/images/sale.jpg", dueAt,
                        AuctionAdminManagementState.REMOVED)), 0, 10, 1, 1));
        for (String path : List.of("/api/auctions/me/bids", "/api/auctions/me/listings", "/api/auction-orders/me",
                "/api/auction-orders/me/sales")) {
            var request = mvc.perform(get(path).session(activeSession()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.size").value(10))
                    .andExpect(jsonPath("$.data.items[0].auctionId").value(1));
            for (String field : List.of("bidderId", "highestBidderId", "winnerId", "sellerId", "buyerId",
                    "memberId", "email", "jwt", "JWT", "password", "passwordHash",
                    "receiverName", "receiverPhone", "shippingAddress", "removedByMemberId", "deletedByMemberId",
                    "removeReason", "deleteReason")) {
                request.andExpect(jsonPath("$.data.items[0]." + field).doesNotExist());
            }
        }
        mvc.perform(get("/api/auctions/me/bids").session(activeSession()))
                .andExpect(jsonPath("$.data.items[0].won").value(true))
                .andExpect(jsonPath("$.data.items[0].leading").value(false))
                .andExpect(jsonPath("$.data.items[0].managementState").value("VISIBLE"))
                .andExpect(jsonPath("$.data.items[0].imageUrl").value("/images/bid.jpg"));
        mvc.perform(get("/api/auctions/me/listings").session(activeSession()))
                .andExpect(jsonPath("$.data.items[0].imageUrl").value("/images/listing.jpg"));
        mvc.perform(get("/api/auction-orders/me").session(activeSession()))
                .andExpect(jsonPath("$.data.items[0].model").value("Order Model"))
                .andExpect(jsonPath("$.data.items[0].size").value("US 10"))
                .andExpect(jsonPath("$.data.items[0].imageUrl").value("/images/order.jpg"))
                .andExpect(jsonPath("$.data.items[0].shippingFee").value(120))
                .andExpect(jsonPath("$.data.items[0].totalAmount").value(6620))
                .andExpect(jsonPath("$.data.items[0].paymentDueAt").value("2026-09-12T12:00:00"))
                .andExpect(jsonPath("$.data.items[0].managementState").value("VISIBLE"));
        mvc.perform(get("/api/auction-orders/me/sales").session(activeSession()))
                .andExpect(jsonPath("$.data.items[0].model").value("Sale Model"))
                .andExpect(jsonPath("$.data.items[0].size").value("US 11"))
                .andExpect(jsonPath("$.data.items[0].imageUrl").value("/images/sale.jpg"))
                .andExpect(jsonPath("$.data.items[0].shippingFee").value(120))
                .andExpect(jsonPath("$.data.items[0].totalAmount").value(6620))
                .andExpect(jsonPath("$.data.items[0].paymentDueAt").value("2026-09-12T12:00:00"))
                .andExpect(jsonPath("$.data.items[0].managementState").value("REMOVED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/auctions/me/bids", "/api/auctions/me/listings", "/api/auction-orders/me",
            "/api/auction-orders/me/sales"})
    void nonNumericPaginationReturns400(String path) throws Exception {
        mvc.perform(get(path).session(activeSession()).param("page", "oops"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(queries);
    }

    private MockHttpSession activeSession() {
        var session = new MockHttpSession();
        session.setAttribute("memberId", 77L);
        when(references.isActiveMember(77L)).thenReturn(true);
        return session;
    }
}
