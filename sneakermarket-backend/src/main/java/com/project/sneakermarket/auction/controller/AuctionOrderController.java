package com.project.sneakermarket.auction.controller;

import org.springframework.web.bind.annotation.*;
import com.project.sneakermarket.auction.dto.AuctionOrderSummaryResponse;
import com.project.sneakermarket.auction.dto.AuctionOrderDetailResponse;
import com.project.sneakermarket.auction.dto.AuctionOrderPaymentResponse;
import com.project.sneakermarket.auction.dto.PageData;
import com.project.sneakermarket.auction.service.AuctionMemberQueryService;
import com.project.sneakermarket.auction.service.AuctionOrderPaymentService;
import com.project.sneakermarket.auction.service.AuctionSessionMemberResolver;
import com.project.sneakermarket.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auction-orders")
@RequiredArgsConstructor
public class AuctionOrderController {
    private final AuctionMemberQueryService queries;
    private final AuctionOrderPaymentService payments;
    private final AuctionSessionMemberResolver sessionMemberResolver;

    @PostMapping("/{orderNumber}/pay")
    public ApiResponse<AuctionOrderPaymentResponse> payAuctionOrder(
            @PathVariable String orderNumber, HttpServletRequest request) {
        Long memberId = sessionMemberResolver.resolveMemberId(request.getSession(false));
        return ApiResponse.ok("競標訂單付款成功", payments.pay(orderNumber, memberId));
    }

    @GetMapping("/{orderNumber}")
    public ApiResponse<AuctionOrderDetailResponse> getAuctionOrderDetail(
            @PathVariable String orderNumber, HttpServletRequest request) {
        Long memberId = sessionMemberResolver.resolveMemberId(request.getSession(false));
        return ApiResponse.ok("查詢競標訂單詳細資料成功",
                queries.getAuctionOrderDetail(memberId, orderNumber));
    }

    @GetMapping("/me")
    public ApiResponse<PageData<AuctionOrderSummaryResponse>> getMyAuctionOrders(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Long memberId = sessionMemberResolver.resolveMemberId(request.getSession(false));
        return ApiResponse.ok("查詢我的競標訂單成功", queries.getMyAuctionOrders(memberId, page, size));
    }

    @GetMapping("/me/sales")
    public ApiResponse<PageData<AuctionOrderSummaryResponse>> getMyAuctionSales(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Long memberId = sessionMemberResolver.resolveMemberId(request.getSession(false));
        return ApiResponse.ok("查詢我的競標賣出訂單成功", queries.getMyAuctionSales(memberId, page, size));
    }
}
