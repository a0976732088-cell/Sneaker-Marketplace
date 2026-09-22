package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.project.sneakermarket.auction.controller.AuctionOrderController;
import com.project.sneakermarket.auction.dto.AuctionOrderCheckoutUpdateRequest;
import com.project.sneakermarket.auction.dto.AuctionOrderCheckoutUpdateResponse;
import com.project.sneakermarket.auction.exception.AuctionConflictException;
import com.project.sneakermarket.auction.exception.AuctionExceptionHandler;
import com.project.sneakermarket.common.exception.ForbiddenException;
import com.project.sneakermarket.common.exception.GlobalExceptionHandler;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;

class AuctionOrderCheckoutAccessTests {
    private AuctionReferenceService references;
    private AuctionOrderCheckoutService checkouts;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        references = mock(AuctionReferenceService.class);
        checkouts = mock(AuctionOrderCheckoutService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AuctionOrderController(
                        mock(AuctionMemberQueryService.class), checkouts,
                        mock(AuctionOrderPaymentService.class),
                        new AuctionSessionMemberResolver(references)))
                .setControllerAdvice(new AuctionExceptionHandler(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    void buyerUpdatesOnlyTheThreeCheckoutFields() throws Exception {
        activeMember(77L);
        when(checkouts.update(eq("AO-CHECKOUT-001"), eq(77L), any()))
                .thenReturn(new AuctionOrderCheckoutUpdateResponse(
                        "AO-CHECKOUT-001", "新收件人", "0987654321", "台中市新地址",
                        LocalDateTime.of(2026, 9, 20, 12, 0)));

        mvc.perform(patch("/api/auction-orders/AO-CHECKOUT-001/checkout")
                        .session(session(77L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverName":"  新收件人  ",
                                  "receiverPhone":"  0987654321  ",
                                  "shippingAddress":"  台中市新地址  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").value("AO-CHECKOUT-001"))
                .andExpect(jsonPath("$.data.receiverName").value("新收件人"))
                .andExpect(jsonPath("$.data.receiverPhone").value("0987654321"))
                .andExpect(jsonPath("$.data.shippingAddress").value("台中市新地址"))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-09-20T12:00:00"))
                .andExpect(jsonPath("$.data.buyerId").doesNotExist())
                .andExpect(jsonPath("$.data.sellerId").doesNotExist());

        ArgumentCaptor<AuctionOrderCheckoutUpdateRequest> body =
                ArgumentCaptor.forClass(AuctionOrderCheckoutUpdateRequest.class);
        verify(checkouts).update(eq("AO-CHECKOUT-001"), eq(77L), body.capture());
        org.junit.jupiter.api.Assertions.assertEquals("  新收件人  ", body.getValue().receiverName());
    }

    @Test
    void anonymousGets401WithoutCreatingSession() throws Exception {
        var result = mvc.perform(patch("/api/auction-orders/AO-CHECKOUT-001/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized())
                .andReturn();
        assertNull(result.getRequest().getSession(false));
        verifyNoInteractions(checkouts);
    }

    @Test
    void inactiveMemberGets401() throws Exception {
        mvc.perform(patch("/api/auction-orders/AO-CHECKOUT-001/checkout")
                        .session(session(77L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(checkouts);
    }

    @Test
    void sellerAndOtherMemberErrorsRemain403() throws Exception {
        activeMember(77L);
        when(checkouts.update(eq("AO-CHECKOUT-001"), eq(77L), any()))
                .thenThrow(new ForbiddenException("你沒有權限修改此競標訂單"));
        mvc.perform(patch("/api/auction-orders/AO-CHECKOUT-001/checkout")
                        .session(session(77L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingOrderIs404() throws Exception {
        activeMember(77L);
        when(checkouts.update(eq("AO-MISSING"), eq(77L), any()))
                .thenThrow(new ResourceNotFoundException("找不到競標訂單"));
        mvc.perform(patch("/api/auction-orders/AO-MISSING/checkout")
                        .session(session(77L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unavailableStateOrDeadlineIs409() throws Exception {
        activeMember(77L);
        when(checkouts.update(eq("AO-CHECKOUT-001"), eq(77L), any()))
                .thenThrow(new AuctionConflictException("競標訂單付款期限已過"));
        mvc.perform(patch("/api/auction-orders/AO-CHECKOUT-001/checkout")
                        .session(session(77L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isConflict());
    }

    @Test
    void blankAndOverflowFieldsAre400BeforeService() throws Exception {
        activeMember(77L);
        for (String body : new String[] {
                """
                {"receiverName":"   ","receiverPhone":"0912345678","shippingAddress":"地址"}
                """,
                """
                {"receiverName":"收件人","receiverPhone":"123456789012345678901","shippingAddress":"地址"}
                """,
                "{\"receiverName\":\"收件人\",\"receiverPhone\":\"0912345678\",\"shippingAddress\":\""
                        + "地".repeat(301) + "\"}"
        }) {
            mvc.perform(patch("/api/auction-orders/AO-CHECKOUT-001/checkout")
                            .session(session(77L))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(checkouts);
    }

    @Test
    void clientControlledTransactionFieldsAreRejected() throws Exception {
        activeMember(77L);
        mvc.perform(patch("/api/auction-orders/AO-CHECKOUT-001/checkout")
                        .session(session(77L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverName":"收件人",
                                  "receiverPhone":"0912345678",
                                  "shippingAddress":"地址",
                                  "finalPrice":1,
                                  "shippingFee":0,
                                  "buyerId":999,
                                  "sellerId":999,
                                  "paymentMethod":"其他",
                                  "paymentDueAt":"2099-01-01T00:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(checkouts);
    }

    private String validBody() {
        return """
                {"receiverName":"收件人","receiverPhone":"0912345678","shippingAddress":"地址"}
                """;
    }

    private void activeMember(Long memberId) {
        when(references.isActiveMember(memberId)).thenReturn(true);
    }

    private MockHttpSession session(Long memberId) {
        var session = new MockHttpSession();
        session.setAttribute("memberId", memberId);
        return session;
    }
}
