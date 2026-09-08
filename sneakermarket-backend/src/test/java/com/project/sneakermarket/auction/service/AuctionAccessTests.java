package com.project.sneakermarket.auction.service;

import java.util.List;

import com.project.sneakermarket.auction.controller.AuctionController;
import com.project.sneakermarket.auction.dto.AuctionLookupOption;
import com.project.sneakermarket.auction.dto.AuctionOptionsResponse;
import com.project.sneakermarket.auction.exception.AuctionExceptionHandler;
import com.project.sneakermarket.auction.exception.AuctionUnauthorizedException;
import com.project.sneakermarket.common.exception.ForbiddenException;
import com.project.sneakermarket.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
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
    private AuctionQueryService queries;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        references = mock(AuctionReferenceService.class);
        resolver = new AuctionSessionMemberResolver(references);
        commands = mock(AuctionCommandService.class);
        queries = mock(AuctionQueryService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                new AuctionController(queries, commands, resolver, references))
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
