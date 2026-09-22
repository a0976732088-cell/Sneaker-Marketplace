package com.project.sneakermarket.auction.payment.linepay;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.project.sneakermarket.auction.controller.AuctionLinePayController;
import com.project.sneakermarket.auction.exception.AuctionExceptionHandler;
import com.project.sneakermarket.auction.exception.AuctionUnauthorizedException;
import com.project.sneakermarket.auction.service.AuctionSessionMemberResolver;
import com.project.sneakermarket.common.exception.ForbiddenException;
import com.project.sneakermarket.common.exception.GlobalExceptionHandler;

class AuctionLinePayControllerTests {
    private AuctionLinePayService linePay;
    private AuctionSessionMemberResolver members;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        linePay = mock(AuctionLinePayService.class);
        members = mock(AuctionSessionMemberResolver.class);
        mvc = MockMvcBuilders.standaloneSetup(new AuctionLinePayController(linePay, members))
                .setControllerAdvice(new AuctionExceptionHandler(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    void requestUsesSessionBuyerAndReturnsOnlyPaymentUrl() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("memberId", 10L);
        when(members.resolveMemberId(session)).thenReturn(10L);
        when(linePay.request("AO-LINE-001", 10L)).thenReturn(
                new AuctionLinePayRequestResponse(
                        "https://sandbox-web-pay.line.me/pay/abc",
                        "ALP0123456789ABCDEF0123456789ABCDEF"));

        mvc.perform(post("/api/auction-orders/AO-LINE-001/line-pay/request").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentUrl")
                        .value("https://sandbox-web-pay.line.me/pay/abc"))
                .andExpect(jsonPath("$.data.merchantOrderId")
                        .value("ALP0123456789ABCDEF0123456789ABCDEF"))
                .andExpect(jsonPath("$.data.channelSecret").doesNotExist());
    }

    @Test
    void anonymousAndNonBuyerRequestsKeep401And403Contracts() throws Exception {
        when(members.resolveMemberId(null)).thenReturn(null);
        when(linePay.request("AO-LINE-001", null)).thenThrow(new AuctionUnauthorizedException());

        mvc.perform(post("/api/auction-orders/AO-LINE-001/line-pay/request"))
                .andExpect(status().isUnauthorized());

        MockHttpSession otherBuyer = new MockHttpSession();
        when(members.resolveMemberId(otherBuyer)).thenReturn(99L);
        when(linePay.request("AO-LINE-001", 99L))
                .thenThrow(new ForbiddenException("你沒有權限付款此競標訂單"));

        mvc.perform(post("/api/auction-orders/AO-LINE-001/line-pay/request")
                        .session(otherBuyer))
                .andExpect(status().isForbidden());
    }

    @Test
    void statusUsesSessionBuyerAndReturnsOnlyPollingState() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("memberId", 10L);
        when(members.resolveMemberId(session)).thenReturn(10L);
        when(linePay.checkStatus("AO-LINE-001", "ALP123", 10L))
                .thenReturn(new AuctionLinePayStatusResponse("WAITING", "等待 LINE Pay 完成付款"));

        mvc.perform(get("/api/auction-orders/AO-LINE-001/line-pay/status")
                        .session(session)
                        .param("merchantOrderId", "ALP123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING"))
                .andExpect(jsonPath("$.data.message").value("等待 LINE Pay 完成付款"))
                .andExpect(jsonPath("$.data.transactionId").doesNotExist());
    }

    @Test
    void anonymousAndNonBuyerStatusKeep401And403Contracts() throws Exception {
        when(members.resolveMemberId(null)).thenReturn(null);
        when(linePay.checkStatus("AO-LINE-001", "ALP123", null))
                .thenThrow(new AuctionUnauthorizedException());

        mvc.perform(get("/api/auction-orders/AO-LINE-001/line-pay/status")
                        .param("merchantOrderId", "ALP123"))
                .andExpect(status().isUnauthorized());

        MockHttpSession otherBuyer = new MockHttpSession();
        when(members.resolveMemberId(otherBuyer)).thenReturn(99L);
        when(linePay.checkStatus("AO-LINE-001", "ALP123", 99L))
                .thenThrow(new ForbiddenException("你沒有權限付款此競標訂單"));

        mvc.perform(get("/api/auction-orders/AO-LINE-001/line-pay/status")
                        .session(otherBuyer)
                        .param("merchantOrderId", "ALP123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void obsoletePublicConfirmAndCancelEndpointsAreNotMapped() throws Exception {
        mvc.perform(get("/api/auction-orders/line-pay/confirm")
                        .param("merchantOrderId", "ALP123")
                        .param("transactionId", "998877"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/auction-orders/line-pay/cancel")
                        .param("merchantOrderId", "ALP123"))
                .andExpect(status().isNotFound());
    }
}
