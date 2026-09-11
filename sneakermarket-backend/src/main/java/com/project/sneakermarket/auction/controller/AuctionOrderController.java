package com.project.sneakermarket.auction.controller;

import org.springframework.web.bind.annotation.*;
import com.project.sneakermarket.auction.dto.AuctionOrderSummaryResponse;
import com.project.sneakermarket.auction.dto.PageData;
import com.project.sneakermarket.auction.service.AuctionMemberQueryService;
import com.project.sneakermarket.auction.service.AuctionSessionMemberResolver;
import com.project.sneakermarket.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auction-orders")
@RequiredArgsConstructor
public class AuctionOrderController {
    private final AuctionMemberQueryService queries;
    private final AuctionSessionMemberResolver sessionMemberResolver;

    @GetMapping("/me")
    public ApiResponse<PageData<AuctionOrderSummaryResponse>> getMyAuctionOrders(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Long memberId = sessionMemberResolver.resolveMemberId(request.getSession(false));
        return ApiResponse.ok("查詢我的競標訂單成功", queries.getMyAuctionOrders(memberId, page, size));
    }
}
