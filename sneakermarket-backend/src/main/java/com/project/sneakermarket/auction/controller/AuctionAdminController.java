package com.project.sneakermarket.auction.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.sneakermarket.auction.dto.AuctionAdminActionRequest;
import com.project.sneakermarket.auction.dto.AuctionAdminBatchRequest;
import com.project.sneakermarket.auction.dto.AuctionAdminBatchResponse;
import com.project.sneakermarket.auction.dto.AuctionAdminBatchRestoreRequest;
import com.project.sneakermarket.auction.dto.AuctionAdminDetailResponse;
import com.project.sneakermarket.auction.dto.AuctionAdminSearchCriteria;
import com.project.sneakermarket.auction.dto.AuctionAdminSummaryResponse;
import com.project.sneakermarket.auction.dto.PageData;
import com.project.sneakermarket.auction.service.AuctionAdminBatchService;
import com.project.sneakermarket.auction.service.AuctionAdminMutationService;
import com.project.sneakermarket.auction.service.AuctionAdminQueryService;
import com.project.sneakermarket.auction.service.AuctionSessionMemberResolver;
import com.project.sneakermarket.common.response.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/auctions")
@RequiredArgsConstructor
@Validated
public class AuctionAdminController {
    private final AuctionAdminQueryService queries;
    private final AuctionAdminMutationService mutations;
    private final AuctionAdminBatchService batches;
    private final AuctionSessionMemberResolver members;

    @GetMapping
    public ApiResponse<PageData<AuctionAdminSummaryResponse>> getAuctions(
            @ModelAttribute AuctionAdminSearchCriteria criteria,
            @PageableDefault(page = 0, size = 15, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            HttpServletRequest request) {
        members.resolveAdminMemberId(request.getSession(false));
        return ApiResponse.ok("查詢管理員競標列表成功", queries.search(criteria, pageable));
    }

    @GetMapping("/{auctionId}")
    public ApiResponse<AuctionAdminDetailResponse> getAuction(
            @PathVariable Long auctionId,
            HttpServletRequest request) {
        members.resolveAdminMemberId(request.getSession(false));
        return ApiResponse.ok("查詢管理員競標詳情成功", queries.detail(auctionId));
    }

    @PutMapping("/{auctionId}/remove")
    public ApiResponse<Void> removeAuction(
            @PathVariable Long auctionId,
            @Valid @RequestBody AuctionAdminActionRequest body,
            HttpServletRequest request) {
        Long adminId = members.resolveAdminMemberId(request.getSession(false));
        mutations.removeAuction(auctionId, adminId, body.reason());
        return ApiResponse.ok("下架競標成功", null);
    }

    @PutMapping("/{auctionId}/restore")
    public ApiResponse<Void> restoreAuction(
            @PathVariable Long auctionId,
            HttpServletRequest request) {
        Long adminId = members.resolveAdminMemberId(request.getSession(false));
        mutations.restoreAuction(auctionId, adminId);
        return ApiResponse.ok("恢復競標顯示成功", null);
    }

    @PutMapping("/{auctionId}/delete")
    public ApiResponse<Void> deleteAuction(
            @PathVariable Long auctionId,
            @Valid @RequestBody AuctionAdminActionRequest body,
            HttpServletRequest request) {
        Long adminId = members.resolveAdminMemberId(request.getSession(false));
        mutations.deleteAuction(auctionId, adminId, body.reason());
        return ApiResponse.ok("刪除競標成功", null);
    }

    @PutMapping("/batch/remove")
    public ApiResponse<AuctionAdminBatchResponse> removeBatch(
            @Valid @RequestBody AuctionAdminBatchRequest body,
            HttpServletRequest request) {
        Long adminId = members.resolveAdminMemberId(request.getSession(false));
        return ApiResponse.ok("批次下架處理完成",
                batches.removeAuctions(body.auctionIds(), adminId, body.reason()));
    }

    @PutMapping("/batch/restore")
    public ApiResponse<AuctionAdminBatchResponse> restoreBatch(
            @Valid @RequestBody AuctionAdminBatchRestoreRequest body,
            HttpServletRequest request) {
        Long adminId = members.resolveAdminMemberId(request.getSession(false));
        return ApiResponse.ok("批次恢復顯示處理完成",
                batches.restoreAuctions(body.auctionIds(), adminId));
    }

    @PutMapping("/batch/delete")
    public ApiResponse<AuctionAdminBatchResponse> deleteBatch(
            @Valid @RequestBody AuctionAdminBatchRequest body,
            HttpServletRequest request) {
        Long adminId = members.resolveAdminMemberId(request.getSession(false));
        return ApiResponse.ok("批次刪除處理完成",
                batches.deleteAuctions(body.auctionIds(), adminId, body.reason()));
    }
}
