package com.project.sneakermarket.auction.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.sneakermarket.auction.logistics.ecpay.AuctionCvsSelectionRequest;
import com.project.sneakermarket.auction.logistics.ecpay.AuctionCvsSelectionResponse;
import com.project.sneakermarket.auction.logistics.ecpay.AuctionCvsStoreResponse;
import com.project.sneakermarket.auction.logistics.ecpay.AuctionCvsStoreSelectionService;
import com.project.sneakermarket.auction.logistics.ecpay.AuctionCvsStoreType;
import com.project.sneakermarket.auction.service.AuctionSessionMemberResolver;
import com.project.sneakermarket.common.response.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auction-orders")
@RequiredArgsConstructor
public class AuctionCvsStoreController {
    private final AuctionCvsStoreSelectionService stores;
    private final AuctionSessionMemberResolver sessionMemberResolver;

    @GetMapping("/{orderNumber}/cvs/stores")
    public ApiResponse<List<AuctionCvsStoreResponse>> getStores(
            @PathVariable String orderNumber,
            @RequestParam AuctionCvsStoreType storeType,
            HttpServletRequest request) {
        Long memberId = sessionMemberResolver.resolveMemberId(request.getSession(false));
        return ApiResponse.ok("查詢 ECPay 超商門市清單成功",
                stores.getStores(orderNumber, memberId, storeType));
    }

    @PutMapping("/{orderNumber}/cvs-selection")
    public ApiResponse<AuctionCvsSelectionResponse> selectStore(
            @PathVariable String orderNumber,
            @RequestBody AuctionCvsSelectionRequest selection,
            HttpServletRequest request) {
        Long memberId = sessionMemberResolver.resolveMemberId(request.getSession(false));
        return ApiResponse.ok("競標訂單取貨門市已更新",
                stores.selectStore(orderNumber, memberId, selection));
    }

    @GetMapping("/{orderNumber}/cvs-selection")
    public ApiResponse<AuctionCvsSelectionResponse> getSelection(
            @PathVariable String orderNumber,
            HttpServletRequest request) {
        Long memberId = sessionMemberResolver.resolveMemberId(request.getSession(false));
        return ApiResponse.ok("查詢競標訂單超商門市成功",
                stores.getSelection(orderNumber, memberId));
    }
}
