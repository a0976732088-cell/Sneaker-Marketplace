package com.project.sneakermarket.auction.controller;

import org.springframework.web.bind.annotation.*;
import com.project.sneakermarket.auction.dto.*;
import com.project.sneakermarket.auction.service.AuctionMemberQueryService;
import com.project.sneakermarket.auction.service.AuctionSessionMemberResolver;
import com.project.sneakermarket.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/** 將會員唯讀入口與既有公開／寫入 Controller 分開。 */
@RestController
@RequestMapping("/api/auctions/me")
@RequiredArgsConstructor
public class AuctionMemberController {
    private final AuctionMemberQueryService queries;
    private final AuctionSessionMemberResolver sessionMemberResolver;

    @GetMapping("/bids")
    public ApiResponse<PageData<AuctionMyBidResponse>> getMyBids(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Long memberId = sessionMemberResolver.resolveMemberId(request.getSession(false));
        return ApiResponse.ok("查詢我的出價成功", queries.getMyBids(memberId, page, size));
    }

    @GetMapping("/listings")
    public ApiResponse<PageData<AuctionMyListingResponse>> getMyListings(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Long memberId = sessionMemberResolver.resolveMemberId(request.getSession(false));
        return ApiResponse.ok("查詢我的刊登成功", queries.getMyListings(memberId, page, size));
    }
}
