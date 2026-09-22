package com.project.sneakermarket.auction.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.sneakermarket.auction.payment.linepay.AuctionLinePayRequestResponse;
import com.project.sneakermarket.auction.payment.linepay.AuctionLinePayService;
import com.project.sneakermarket.auction.payment.linepay.AuctionLinePayStatusResponse;
import com.project.sneakermarket.auction.service.AuctionSessionMemberResolver;
import com.project.sneakermarket.common.response.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auction-orders")
@RequiredArgsConstructor
public class AuctionLinePayController {
    private final AuctionLinePayService linePay;
    private final AuctionSessionMemberResolver sessionMemberResolver;

    @PostMapping("/{orderNumber}/line-pay/request")
    public ApiResponse<AuctionLinePayRequestResponse> requestPayment(
            @PathVariable String orderNumber,
            HttpServletRequest request) {
        Long memberId = sessionMemberResolver.resolveMemberId(request.getSession(false));
        return ApiResponse.ok("LINE Pay 付款已建立", linePay.request(orderNumber, memberId));
    }

    @GetMapping("/{orderNumber}/line-pay/status")
    public ApiResponse<AuctionLinePayStatusResponse> checkStatus(
            @PathVariable String orderNumber,
            @RequestParam String merchantOrderId,
            HttpServletRequest request) {
        Long memberId = sessionMemberResolver.resolveMemberId(request.getSession(false));
        return ApiResponse.ok(
                "LINE Pay 付款狀態已查詢",
                linePay.checkStatus(orderNumber, merchantOrderId, memberId));
    }
}
