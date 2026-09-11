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
import com.project.sneakermarket.auction.dto.AuctionOptionsResponse;
import com.project.sneakermarket.auction.dto.AuctionSearchCriteria;
import com.project.sneakermarket.auction.dto.AuctionStatusResponse;
import com.project.sneakermarket.auction.dto.AuctionSummaryResponse;
import com.project.sneakermarket.auction.dto.BidCreateRequest;
import com.project.sneakermarket.auction.dto.BidHistoryResponse;
import com.project.sneakermarket.auction.dto.PageData;
import com.project.sneakermarket.auction.service.AuctionBidService;
import com.project.sneakermarket.auction.service.AuctionCommandService;
import com.project.sneakermarket.auction.service.AuctionQueryService;
import com.project.sneakermarket.auction.service.AuctionReferenceService;
import com.project.sneakermarket.auction.service.AuctionSessionMemberResolver;
import com.project.sneakermarket.common.response.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.project.sneakermarket.auction.dto.AuctionUpdateRequest;

import jakarta.servlet.http.HttpServletRequest;
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

    // 出價交易與悲觀鎖控制由專責 Service 處理
    private final AuctionBidService auctionBidService;

    /*
     * 所有競標寫入 API 共用正式 Session resolver。
     * Resolver 只接受 Long memberId，並確認會員存在且尚未刪除；不提供 fallback。
     */
    private final AuctionSessionMemberResolver sessionMemberResolver;

    // 只透過競標整合入口唯讀取得品牌、分類與會員有效性
    private final AuctionReferenceService auctionReferenceService;

    /**
     * 查詢建立與編輯競標所需的品牌、分類選項。
     *
     * API：
     * GET /api/auctions/options
     *
     * @return 由共用主資料唯讀組成的競標選項
     */
    @GetMapping("/options")
    public ApiResponse<AuctionOptionsResponse> getAuctionOptions() {
        return ApiResponse.ok(
                "查詢競標選項成功",
                auctionReferenceService.getOptions()
        );
    }

    /**
     * 驗證目前瀏覽器是否具有可用的會員 Session。
     *
     * API：
     * GET /api/auctions/session
     *
     * 此端點不公開會員資料，只回報 Session 是否有效。
     *
     * @param httpRequest 目前瀏覽器的 HTTP request
     * @return 驗證成功時 data 為 null
     */
    @GetMapping("/session")
    public ApiResponse<Void> validateAuctionSession(HttpServletRequest httpRequest) {
        sessionMemberResolver.resolveMemberId(httpRequest.getSession(false));

        return ApiResponse.ok(
                "競標會員 Session 有效",
                null
        );
    }

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
     * @param httpRequest 目前瀏覽器的 HTTP request
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

            HttpServletRequest httpRequest) {

        /*
         * sellerId 不接受前端傳入，
         * 必須從目前登入會員的有效 Session 取得。
         */
        Long sellerId =
                sessionMemberResolver.resolveMemberId(httpRequest.getSession(false));

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
     * 修改指定競標。
     *
     * API：
     * PUT /api/auctions/{auctionId}
     *
     * 規則：
     * - 只有該競標商品的賣家本人可以修改。
     * - 已有出價後不可修改。
     * - SCHEDULED 且無出價時，可修改所有一般可編輯欄位。
     * - ACTIVE 且無出價時，可修改商品、交易、價格與結束時間，
     *   但不可修改已經發生的開始時間。
     *
     * sellerId 不接受前端傳入，
     * 仍由目前登入 Session 取得。
     *
     * @param auctionId 要修改的競標 ID
     * @param request 修改資料
     * @param httpRequest 目前瀏覽器的 HTTP request
     * @return 修改結果
     */
    @PutMapping("/{auctionId}")
    public ApiResponse<Void> updateAuction(
            @PathVariable Long auctionId,

            @Valid
            @RequestBody
            AuctionUpdateRequest request,

            HttpServletRequest httpRequest) {

        // 從目前登入 Session 取得會員 ID
        Long sellerId =
                sessionMemberResolver.resolveMemberId(httpRequest.getSession(false));

        // 修改規則與 Transaction 統一由 Command Service 處理
        auctionCommandService.updateAuction(
                auctionId,
                sellerId,
                request
        );

        return ApiResponse.ok(
                "修改競標成功",
                null
        );
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
     * 對指定的進行中競標出價。
     *
     * bidderId 一律由目前登入 Session 取得，Request 不接受會員身分。
     *
     * @param auctionId 競標 ID
     * @param request 出價金額
     * @param httpRequest 目前瀏覽器的 HTTP request
     * @return 出價成功後的最新競標狀態
     */
    @PostMapping("/{auctionId}/bids")
    public ApiResponse<AuctionStatusResponse> placeBid(
            @PathVariable Long auctionId,
            @Valid @RequestBody BidCreateRequest request,
            HttpServletRequest httpRequest) {

        Long bidderId = sessionMemberResolver
                .resolveMemberId(httpRequest.getSession(false));

        AuctionStatusResponse response = auctionBidService.placeBid(
                auctionId,
                bidderId,
                request
        );

        return ApiResponse.ok("出價成功", response);
    }

    /**
     * 查詢指定競標的最新公開狀態。
     *
     * API：
     * GET /api/auctions/{auctionId}/status
     *
     * 此端點供詳細頁定期同步使用，訪客也可以查詢。
     *
     * @param auctionId 競標 ID
     * @return 競標最新公開狀態
     */
    @GetMapping("/{auctionId}/status")
    public ApiResponse<AuctionStatusResponse> getAuctionStatus(
            @PathVariable Long auctionId) {

        AuctionStatusResponse response =
                auctionQueryService.getAuctionStatus(auctionId);

        return ApiResponse.ok(
                "查詢競標最新狀態成功",
                response
        );
    }

    /**
     * 查詢指定競標的完整詳細資料。
     *
     * 訪客可以瀏覽。
     * 若目前有登入會員，額外判斷該會員是否為賣家本人，
     * 但不公開 sellerId。
     *
     * @param auctionId 競標 ID
     * @param httpRequest 目前瀏覽器的 HTTP request
     * @return 競標詳細資料
     */
    @GetMapping("/{auctionId}")
    public ApiResponse<AuctionDetailResponse> getAuctionDetail(
            @PathVariable Long auctionId,
            HttpServletRequest httpRequest) {

        /*
         * 詳細頁是公開 API，
         * 因此使用可回 null 的 Resolver。
         *
         * 未登入訪客不應因為沒有 memberId
         * 而無法查看競標詳細頁。
         */
        Long currentMemberId =
                sessionMemberResolver
                        .resolveMemberIdOrNull(httpRequest.getSession(false));

        // 呼叫 Service 組合競標、商品、圖片與擁有者資訊
        AuctionDetailResponse auction =
                auctionQueryService.getAuctionDetail(
                        auctionId,
                        currentMemberId
                );

        return ApiResponse.ok(
                "查詢競標詳細資料成功",
                auction
        );
    }

    /**
     * 取消指定競標。
     *
     * API：
     * DELETE /api/auctions/{auctionId}
     *
     * 目前登入會員必須是該競標商品的賣家，
     * 且該競標尚未存在任何成功出價。
     *
     * 此操作不會真正刪除資料，
     * 而是將競標狀態改為 CANCELLED。
     *
     * @param auctionId 要取消的競標 ID
     * @param httpRequest 目前瀏覽器的 HTTP request
     * @return 取消結果
     */
    @DeleteMapping("/{auctionId}")
    public ApiResponse<Void> cancelAuction(
            @PathVariable Long auctionId,
            HttpServletRequest httpRequest) {

        /*
         * sellerId 不接受前端指定，
         * 仍沿用建立競標時的 Session Resolver。
         */
        Long sellerId =
                sessionMemberResolver.resolveMemberId(httpRequest.getSession(false));

        // 商業規則與資料修改交由 Command Service
        auctionCommandService.cancelAuction(
                auctionId,
                sellerId
        );

        return ApiResponse.ok(
                "取消競標成功",
                null
        );
    }
}
