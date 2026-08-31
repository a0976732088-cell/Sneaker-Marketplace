package com.project.sneakermarket.auction.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.sneakermarket.auction.dto.AuctionSearchCriteria;
import com.project.sneakermarket.auction.dto.AuctionSummaryResponse;
import com.project.sneakermarket.auction.dto.PageData;
import com.project.sneakermarket.auction.service.AuctionQueryService;
import com.project.sneakermarket.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * 競標系統公開 API Controller。
 *
 * 負責接收競標相關的 HTTP 請求，
 * 並將實際商業邏輯交由 Service 處理。
 */
@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionQueryService auctionQueryService;

    /**
     * 查詢競標列表。
     *
     * API：
     * GET /api/auctions
     *
     * 支援搜尋、篩選、排序與分頁。
     *
     * @param criteria 搜尋與篩選條件
     * @param pageable 分頁與排序條件
     * @return 競標列表與分頁資料
     */
    @GetMapping
    public ApiResponse<PageData<AuctionSummaryResponse>> getAuctions(
            @ModelAttribute AuctionSearchCriteria criteria,
            @PageableDefault(
                    page = 0,
                    size = 12,
                    sort = "endAt",
                    direction = Sort.Direction.ASC
            ) Pageable pageable) {

        // 呼叫 Service 依條件查詢競標列表
        PageData<AuctionSummaryResponse> auctions =
                auctionQueryService.searchAuctions(
                        criteria,
                        pageable
                );

        // 使用 BASE 專案統一的 ApiResponse 格式回傳成功結果
        return ApiResponse.ok(
                "查詢競標列表成功",
                auctions
        );
    }
}