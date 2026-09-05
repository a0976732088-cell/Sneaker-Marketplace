package com.project.sneakermarket.auction.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.project.sneakermarket.auction.dto.AuctionCreateRequest;
import com.project.sneakermarket.auction.dto.AuctionDetailResponse;
import com.project.sneakermarket.auction.dto.AuctionSearchCriteria;
import com.project.sneakermarket.auction.dto.AuctionSummaryResponse;
import com.project.sneakermarket.auction.dto.BidHistoryResponse;
import com.project.sneakermarket.auction.dto.PageData;
import com.project.sneakermarket.auction.service.AuctionCommandService;
import com.project.sneakermarket.auction.service.AuctionQueryService;
import com.project.sneakermarket.auction.service.AuctionSessionMemberResolver;
import com.project.sneakermarket.common.response.ApiResponse;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 競標系統 API Controller。
 *
 * 負責接收競標相關 HTTP 請求，
 * 實際查詢與寫入邏輯交由 Service 處理。
 */
@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
@Validated
public class AuctionController {

    private final AuctionQueryService auctionQueryService;

    // 處理建立、修改、取消等競標寫入操作
    private final AuctionCommandService auctionCommandService;

    /*
     * 【臨時開發整合層】
     *
     * 目前會員模組 Session 契約尚未完成，
     * 因此先透過 AuctionSessionMemberResolver 取得 memberId。
     *
     * 未來會員模組合板後，
     * Session Key / Session Value 型別只需修改 Resolver，
     * Controller 不需要跟著修改。
     */
    private final AuctionSessionMemberResolver sessionMemberResolver;

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

    /**
     * 建立新的競標。
     *
     * API：
     * POST /api/auctions
     *
     * Content-Type：
     * multipart/form-data
     *
     * multipart 內容：
     * request = AuctionCreateRequest JSON
     * images  = 一張以上的圖片
     *
     * @param request 建立競標資料
     * @param images 商品圖片
     * @param session 目前瀏覽器的 HttpSession
     * @return 新建立的 auctionId
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createAuction(
            @Valid
            @RequestPart("request")
            AuctionCreateRequest request,

            @RequestPart(
                    value = "images",
                    required = false
            )
            List<MultipartFile> images,

            HttpSession session) {

        /*
         * 正式設計：
         * sellerId 不接受前端傳入，
         * 必須從目前登入會員的 Session 取得。
         *
         * 目前會員模組尚未合板時，
         * Resolver 可依 local 設定暫時使用 fallback memberId。
         */
        Long sellerId =
                sessionMemberResolver.resolveMemberId(session);

        // 呼叫競標寫入 Service 建立商品、圖片與競標
        Long auctionId =
                auctionCommandService.createAuction(
                        sellerId,
                        request,
                        images
                );

        ApiResponse<Long> response =
                ApiResponse.ok(
                        "建立競標成功",
                        auctionId
                );

        // 建立新資源使用 HTTP 201 Created
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * 查詢指定競標的公開出價紀錄。
     *
     * API：
     * GET /api/auctions/{auctionId}/bids
     *
     * @param auctionId 競標 ID
     * @return 該場競標的公開出價紀錄
     */
    @GetMapping("/{auctionId}/bids")
    public ApiResponse<List<BidHistoryResponse>> getBidHistory(
            @PathVariable Long auctionId) {

        // 呼叫 Service 查詢並轉換公開出價紀錄
        List<BidHistoryResponse> bids =
                auctionQueryService.getBidHistory(auctionId);

        // 使用 BASE 專案統一的 ApiResponse 格式回傳成功結果
        return ApiResponse.ok(
                "查詢競標出價紀錄成功",
                bids
        );
    }

    /**
     * 查詢指定競標的完整詳細資料。
     *
     * API：
     * GET /api/auctions/{auctionId}
     *
     * @param auctionId 競標 ID
     * @return 競標詳細資料
     */
    @GetMapping("/{auctionId}")
    public ApiResponse<AuctionDetailResponse> getAuctionDetail(
            @PathVariable Long auctionId) {

        // 呼叫 Service 組合競標、商品與圖片資料
        AuctionDetailResponse auction =
                auctionQueryService.getAuctionDetail(auctionId);

        // 使用 BASE 專案統一的 ApiResponse 格式回傳
        return ApiResponse.ok(
                "查詢競標詳細資料成功",
                auction
        );
    }
}