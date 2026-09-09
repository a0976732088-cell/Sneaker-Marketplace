package com.project.sneakermarket.auction.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.project.sneakermarket.auction.controller.AuctionController;
import com.project.sneakermarket.auction.dto.AuctionLookupOption;
import com.project.sneakermarket.auction.dto.AuctionOptionsResponse;
import com.project.sneakermarket.auction.dto.AuctionStatusResponse;
import com.project.sneakermarket.auction.dto.BidCreateRequest;
import com.project.sneakermarket.auction.entity.AuctionStatus;
import com.project.sneakermarket.auction.exception.AuctionExceptionHandler;
import com.project.sneakermarket.auction.exception.AuctionConflictException;
import com.project.sneakermarket.auction.exception.AuctionUnauthorizedException;
import com.project.sneakermarket.common.exception.BusinessException;
import com.project.sneakermarket.common.exception.ForbiddenException;
import com.project.sneakermarket.common.exception.GlobalExceptionHandler;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuctionAccessTests {
    private AuctionReferenceService references;
    private AuctionSessionMemberResolver resolver;
    private AuctionCommandService commands;
    private AuctionBidService bids;
    private AuctionQueryService queries;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        references = mock(AuctionReferenceService.class);
        resolver = new AuctionSessionMemberResolver(references);
        commands = mock(AuctionCommandService.class);
        bids = mock(AuctionBidService.class);
        queries = mock(AuctionQueryService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                new AuctionController(queries, commands, bids, resolver, references))
                .setControllerAdvice(new AuctionExceptionHandler(), new GlobalExceptionHandler()).build();
    }

    @Test
    void anonymousCannotCancelAndDoesNotCreateSession() throws Exception {
        var result = mvc.perform(delete("/api/auctions/12"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false)).andReturn();
        assertNull(result.getRequest().getSession(false));
        verifyNoInteractions(commands);
    }

    @Test
    void anonymousCannotBidAndDoesNotCreateSession() throws Exception {
        var result = mvc.perform(post("/api/auctions/12/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bidAmount\":5000}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andReturn();

        assertNull(result.getRequest().getSession(false));
        verifyNoInteractions(bids);
    }

    @Test
    void bidUsesSessionMemberAndReturnsLatestPublicStatus() throws Exception {
        var session = new MockHttpSession();
        session.setAttribute("memberId", 77L);
        when(references.isActiveMember(77L)).thenReturn(true);
        when(bids.placeBid(eq(12L), eq(77L), any(BidCreateRequest.class)))
                .thenReturn(new AuctionStatusResponse(
                        12L,
                        new BigDecimal("3500.00"),
                        1,
                        new BigDecimal("4000.00"),
                        AuctionStatus.ACTIVE,
                        LocalDateTime.of(2026, 9, 8, 13, 0),
                        LocalDateTime.of(2026, 9, 8, 12, 0)
                ));

        mvc.perform(post("/api/auctions/12/bids")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bidAmount\":3500.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("出價成功"))
                .andExpect(jsonPath("$.data.currentPrice").value(3500.00))
                .andExpect(jsonPath("$.data.bidCount").value(1))
                .andExpect(jsonPath("$.data.nextMinimumBid").value(4000.00))
                .andExpect(jsonPath("$.data.highestBidderId").doesNotExist())
                .andExpect(jsonPath("$.data.sellerId").doesNotExist())
                .andExpect(jsonPath("$.data.bidderId").doesNotExist())
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.jwt").doesNotExist());

        verify(bids).placeBid(eq(12L), eq(77L), argThat(request ->
                new BigDecimal("3500.00").compareTo(request.bidAmount()) == 0
        ));
    }

    @Test
    void integerBidPayloadIsAccepted() throws Exception {
        var session = new MockHttpSession();
        session.setAttribute("memberId", 77L);
        when(references.isActiveMember(77L)).thenReturn(true);
        when(bids.placeBid(eq(12L), eq(77L), any(BidCreateRequest.class)))
                .thenReturn(new AuctionStatusResponse(
                        12L,
                        new BigDecimal("3500.00"),
                        1,
                        new BigDecimal("4000.00"),
                        AuctionStatus.ACTIVE,
                        LocalDateTime.of(2026, 9, 8, 13, 0),
                        LocalDateTime.of(2026, 9, 8, 12, 0)
                ));

        mvc.perform(post("/api/auctions/12/bids")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bidAmount\":3500}"))
                .andExpect(status().isOk());

        verify(bids).placeBid(eq(12L), eq(77L), argThat(request ->
                new BigDecimal("3500").compareTo(request.bidAmount()) == 0
        ));
    }

    @Test
    void illegalIncrementAndFractionalBidReturnBadRequest() throws Exception {
        var session = new MockHttpSession();
        session.setAttribute("memberId", 77L);
        when(references.isActiveMember(77L)).thenReturn(true);

        when(bids.placeBid(eq(12L), eq(77L), any(BidCreateRequest.class)))
                .thenThrow(new BusinessException("出價金額不符合固定加價級距"));
        mvc.perform(post("/api/auctions/12/bids")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bidAmount\":3001}"))
                .andExpect(status().isBadRequest());

        when(bids.placeBid(eq(12L), eq(77L), any(BidCreateRequest.class)))
                .thenThrow(new BusinessException("競標金額僅接受整數新台幣"));
        mvc.perform(post("/api/auctions/12/bids")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bidAmount\":3500.5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bidRequestRejectsClientControlledIdentity() throws Exception {
        var session = new MockHttpSession();
        session.setAttribute("memberId", 77L);
        when(references.isActiveMember(77L)).thenReturn(true);

        mvc.perform(post("/api/auctions/12/bids")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bidAmount\":5500,\"bidderId\":999}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bids);
    }

    @Test
    void deletedMemberCannotBidWithStaleSession() throws Exception {
        var session = new MockHttpSession();
        session.setAttribute("memberId", 77L);
        when(references.isActiveMember(77L)).thenReturn(false);

        mvc.perform(post("/api/auctions/12/bids")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bidAmount\":5500}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(bids);
    }

    @Test
    void invalidBidAmountsReturnBadRequestBeforeServiceCall() throws Exception {
        for (String body : new String[]{
                "{}",
                "{\"bidAmount\":0}",
                "{\"bidAmount\":-1}",
                "{\"bidAmount\":1.001}"
        }) {
            mvc.perform(post("/api/auctions/12/bids")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
        verifyNoInteractions(bids);
    }

    @Test
    void bidDomainExceptionsUseExpectedHttpStatuses() throws Exception {
        var session = new MockHttpSession();
        session.setAttribute("memberId", 77L);
        when(references.isActiveMember(77L)).thenReturn(true);

        when(bids.placeBid(eq(12L), eq(77L), any(BidCreateRequest.class)))
                .thenThrow(new BusinessException("目前最低可出價金額為 6000.00"));
        mvc.perform(post("/api/auctions/12/bids").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bidAmount\":5999.99}"))
                .andExpect(status().isBadRequest());

        when(bids.placeBid(eq(12L), eq(77L), any(BidCreateRequest.class)))
                .thenThrow(new AuctionConflictException("競標已截止，無法出價"));
        mvc.perform(post("/api/auctions/12/bids").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bidAmount\":5500}"))
                .andExpect(status().isConflict());

        when(bids.placeBid(eq(12L), eq(77L), any(BidCreateRequest.class)))
                .thenThrow(new ForbiddenException("賣家不可對自己的競標出價"));
        mvc.perform(post("/api/auctions/12/bids").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bidAmount\":5500}"))
                .andExpect(status().isForbidden());

        when(bids.placeBid(eq(12L), eq(77L), any(BidCreateRequest.class)))
                .thenThrow(new ResourceNotFoundException("找不到指定的競標資料"));
        mvc.perform(post("/api/auctions/12/bids").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bidAmount\":5500}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicDetailRemainsAnonymousWithoutFallback() throws Exception {
        mvc.perform(get("/api/auctions/12")).andExpect(status().isOk());
        verify(queries).getAuctionDetail(12L, null);
    }

    @Test
    void actualMemberIdIsUsedForCancellation() throws Exception {
        var session = new MockHttpSession();
        session.setAttribute("memberId", 42L);
        when(references.isActiveMember(42L)).thenReturn(true);
        mvc.perform(delete("/api/auctions/12").session(session)).andExpect(status().isOk());
        verify(commands).cancelAuction(12L, 42L);
    }

    @Test
    void anotherMemberStillReceives403() throws Exception {
        var session = new MockHttpSession();
        session.setAttribute("memberId", 42L);
        when(references.isActiveMember(42L)).thenReturn(true);
        doThrow(new ForbiddenException("你無權取消此競標")).when(commands).cancelAuction(12L, 42L);
        mvc.perform(delete("/api/auctions/12").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deletedMemberWithOldSessionReceives401() throws Exception {
        var session = new MockHttpSession();
        session.setAttribute("memberId", 42L);
        mvc.perform(delete("/api/auctions/12").session(session)).andExpect(status().isUnauthorized());
        assertNull(resolver.resolveMemberIdOrNull(session));
        verifyNoInteractions(commands);
    }

    @Test
    void loggedOutSessionCannotResolveMember() {
        var session = new MockHttpSession();
        session.setAttribute("memberId", 42L);
        session.invalidate();
        assertNull(resolver.resolveMemberIdOrNull(session));
        assertThrows(AuctionUnauthorizedException.class, () -> resolver.resolveMemberId(session));
    }

    @Test
    void malformedOrMissingSessionCannotBecomeMemberOne() {
        for (Object value : new Object[]{"1", 1, 1.5, -1L, 0L}) {
            var session = new MockHttpSession();
            session.setAttribute("memberId", value);
            assertThrows(AuctionUnauthorizedException.class, () -> resolver.resolveMemberId(session));
        }
        assertThrows(AuctionUnauthorizedException.class, () -> resolver.resolveMemberId(null));
        verifyNoInteractions(references);
    }

    @Test
    void optionsEndpointReturnsSharedReferenceData() throws Exception {
        when(references.getOptions()).thenReturn(
                new AuctionOptionsResponse(
                        List.of(new AuctionLookupOption(7L, "Nike")),
                        List.of(new AuctionLookupOption(9L, "Sneakers"))));

        var result = mvc.perform(get("/api/auctions/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.brands[0].id").value(7))
                .andExpect(jsonPath("$.data.brands[0].name").value("Nike"))
                .andExpect(jsonPath("$.data.categories[0].id").value(9))
                .andExpect(jsonPath("$.data.categories[0].name").value("Sneakers"))
                .andReturn();

        assertNull(result.getRequest().getSession(false));
        verify(references).getOptions();
    }

    @Test
    void sessionEndpointRequiresActualLogin() throws Exception {
        mvc.perform(get("/api/auctions/session")).andExpect(status().isUnauthorized());
        var session = new MockHttpSession();
        session.setAttribute("memberId", 42L);
        when(references.isActiveMember(42L)).thenReturn(true);
        mvc.perform(get("/api/auctions/session").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").doesNotExist());
    }
}
