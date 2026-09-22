package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.project.sneakermarket.auction.controller.AuctionOrderController;
import com.project.sneakermarket.auction.dto.AuctionOrderPaymentResponse;
import com.project.sneakermarket.auction.entity.AuctionOrderStatus;
import com.project.sneakermarket.auction.entity.AuctionPaymentStatus;
import com.project.sneakermarket.auction.exception.AuctionConflictException;
import com.project.sneakermarket.auction.exception.AuctionExceptionHandler;
import com.project.sneakermarket.common.exception.ForbiddenException;
import com.project.sneakermarket.common.exception.GlobalExceptionHandler;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;

class AuctionOrderPaymentAccessTests {
    private static final LocalDateTime PAID_AT = LocalDateTime.of(2026, 9, 14, 12, 30);

    private AuctionReferenceService references;
    private AuctionOrderPaymentService payments;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        references = mock(AuctionReferenceService.class);
        payments = mock(AuctionOrderPaymentService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AuctionOrderController(
                        mock(AuctionMemberQueryService.class),
                        mock(AuctionOrderCheckoutService.class), payments,
                        new AuctionSessionMemberResolver(references)))
                .setControllerAdvice(new AuctionExceptionHandler(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    void activeSessionBuyerPaysAndForgedIdentityOrAmountsCannotAffectCall() throws Exception {
        when(references.isActiveMember(77L)).thenReturn(true);
        when(payments.pay("AO-PAY-001", 77L)).thenReturn(new AuctionOrderPaymentResponse(
                "AO-PAY-001", 9L, "轉帳", AuctionPaymentStatus.PAID,
                AuctionOrderStatus.PAID, PAID_AT, PAID_AT));

        mvc.perform(post("/api/auction-orders/AO-PAY-001/pay")
                        .session(session(77L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"buyerId":999,"memberId":999,"finalPrice":1,"shippingFee":0,"totalAmount":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").value("AO-PAY-001"))
                .andExpect(jsonPath("$.data.auctionId").value(9))
                .andExpect(jsonPath("$.data.paymentMethod").value("轉帳"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.data.orderStatus").value("PAID"))
                .andExpect(jsonPath("$.data.paidAt").value("2026-09-14T12:30:00"));

        verify(payments).pay("AO-PAY-001", 77L);
        verifyNoMoreInteractions(payments);
    }

    @Test
    void anonymousGets401WithoutCreatingSession() throws Exception {
        var result = mvc.perform(post("/api/auction-orders/AO-PAY-001/pay"))
                .andExpect(status().isUnauthorized())
                .andReturn();
        assertNull(result.getRequest().getSession(false));
        verifyNoInteractions(payments);
    }

    @Test
    void inactiveMemberGets401() throws Exception {
        mvc.perform(post("/api/auction-orders/AO-PAY-001/pay").session(session(77L)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(payments);
    }

    @Test
    void differentBuyerGets403() throws Exception {
        activeMember();
        when(payments.pay("AO-PAY-001", 77L))
                .thenThrow(new ForbiddenException("你沒有權限付款此競標訂單"));
        mvc.perform(post("/api/auction-orders/AO-PAY-001/pay").session(session(77L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingOrderGets404() throws Exception {
        activeMember();
        when(payments.pay("AO-MISSING", 77L))
                .thenThrow(new ResourceNotFoundException("找不到競標訂單"));
        mvc.perform(post("/api/auction-orders/AO-MISSING/pay").session(session(77L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unpayableStateGets409() throws Exception {
        activeMember();
        when(payments.pay("AO-PAY-001", 77L))
                .thenThrow(new AuctionConflictException("此競標訂單目前無法付款"));
        mvc.perform(post("/api/auction-orders/AO-PAY-001/pay").session(session(77L)))
                .andExpect(status().isConflict());
    }

    private void activeMember() {
        when(references.isActiveMember(77L)).thenReturn(true);
    }

    private MockHttpSession session(Long memberId) {
        var session = new MockHttpSession();
        session.setAttribute("memberId", memberId);
        return session;
    }
}
