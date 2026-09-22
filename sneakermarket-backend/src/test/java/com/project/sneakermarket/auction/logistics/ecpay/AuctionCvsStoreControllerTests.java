package com.project.sneakermarket.auction.logistics.ecpay;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.project.sneakermarket.auction.controller.AuctionCvsStoreController;
import com.project.sneakermarket.auction.exception.AuctionConflictException;
import com.project.sneakermarket.auction.exception.AuctionExceptionHandler;
import com.project.sneakermarket.auction.exception.AuctionUnauthorizedException;
import com.project.sneakermarket.auction.service.AuctionSessionMemberResolver;
import com.project.sneakermarket.common.exception.ForbiddenException;
import com.project.sneakermarket.common.exception.GlobalExceptionHandler;

class AuctionCvsStoreControllerTests {
    private AuctionCvsStoreSelectionService stores;
    private AuctionSessionMemberResolver members;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        stores = mock(AuctionCvsStoreSelectionService.class);
        members = mock(AuctionSessionMemberResolver.class);
        mvc = MockMvcBuilders.standaloneSetup(new AuctionCvsStoreController(stores, members))
                .setControllerAdvice(new AuctionExceptionHandler(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    void buyerCanGetCanonicalOfficialStoreList() throws Exception {
        MockHttpSession session = session(10L);
        when(members.resolveMemberId(session)).thenReturn(10L);
        when(stores.getStores("AO-CVS-001", 10L, AuctionCvsStoreType.UNIMART))
                .thenReturn(List.of(new AuctionCvsStoreResponse(
                        AuctionCvsStoreType.UNIMART,
                        "001234",
                        "官方門市",
                        "臺北市中正區官方路 1 號",
                        "02-12345678")));

        mvc.perform(get("/api/auction-orders/AO-CVS-001/cvs/stores")
                        .session(session)
                        .param("storeType", "UNIMART"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].storeType").value("UNIMART"))
                .andExpect(jsonPath("$.data[0].storeId").value("001234"))
                .andExpect(jsonPath("$.data[0].storeName").value("官方門市"))
                .andExpect(jsonPath("$.data[0].storeAddress")
                        .value("臺北市中正區官方路 1 號"))
                .andExpect(jsonPath("$.data[0].storePhone").value("02-12345678"));
    }

    @Test
    void storeListKeepsAnonymousForbiddenAndConflictHttpContracts() throws Exception {
        when(members.resolveMemberId(null)).thenReturn(null);
        when(stores.getStores("AO-CVS-001", null, AuctionCvsStoreType.FAMI))
                .thenThrow(new AuctionUnauthorizedException());
        mvc.perform(get("/api/auction-orders/AO-CVS-001/cvs/stores")
                        .param("storeType", "FAMI"))
                .andExpect(status().isUnauthorized());

        MockHttpSession otherBuyer = session(99L);
        when(members.resolveMemberId(otherBuyer)).thenReturn(99L);
        when(stores.getStores("AO-CVS-001", 99L, AuctionCvsStoreType.FAMI))
                .thenThrow(new ForbiddenException("你沒有權限操作此競標訂單的超商門市"));
        mvc.perform(get("/api/auction-orders/AO-CVS-001/cvs/stores")
                        .session(otherBuyer)
                        .param("storeType", "FAMI"))
                .andExpect(status().isForbidden());

        MockHttpSession buyer = session(10L);
        when(members.resolveMemberId(buyer)).thenReturn(10L);
        when(stores.getStores("AO-CVS-001", 10L, AuctionCvsStoreType.FAMI))
                .thenThrow(new AuctionConflictException("競標訂單付款期限已過"));
        mvc.perform(get("/api/auction-orders/AO-CVS-001/cvs/stores")
                        .session(buyer)
                        .param("storeType", "FAMI"))
                .andExpect(status().isConflict());
    }

    @Test
    void putSelectionPassesOnlyAllowlistedTypeAndStoreId() throws Exception {
        MockHttpSession session = session(10L);
        when(members.resolveMemberId(session)).thenReturn(10L);
        LocalDateTime selectedAt = LocalDateTime.of(2026, 9, 21, 18, 10);
        when(stores.selectStore(
                eq("AO-CVS-001"),
                eq(10L),
                any(AuctionCvsSelectionRequest.class)))
                .thenReturn(new AuctionCvsSelectionResponse(
                        true,
                        AuctionCvsStoreType.HILIFE,
                        "HILIFEC2C",
                        "002222",
                        "官方門市",
                        "官方地址",
                        "02-12345678",
                        null,
                        selectedAt));

        mvc.perform(put("/api/auction-orders/AO-CVS-001/cvs-selection")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storeType":"HILIFE","storeId":"002222"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selected").value(true))
                .andExpect(jsonPath("$.data.storeType").value("HILIFE"))
                .andExpect(jsonPath("$.data.cvsStoreId").value("002222"))
                .andExpect(jsonPath("$.data.cvsStoreName").value("官方門市"))
                .andExpect(jsonPath("$.data.cvsAddress").value("官方地址"));

        verify(stores).selectStore(
                eq("AO-CVS-001"),
                eq(10L),
                argThat(request -> request.storeType() == AuctionCvsStoreType.HILIFE
                        && "002222".equals(request.storeId())));
    }

    @Test
    void getSelectionStillReturnsSelectedFalseInsteadOf404() throws Exception {
        MockHttpSession session = session(10L);
        when(members.resolveMemberId(session)).thenReturn(10L);
        when(stores.getSelection("AO-CVS-001", 10L))
                .thenReturn(AuctionCvsSelectionResponse.notSelected());

        mvc.perform(get("/api/auction-orders/AO-CVS-001/cvs-selection").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selected").value(false));
    }

    private MockHttpSession session(Long memberId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("memberId", memberId);
        return session;
    }
}
