package com.project.sneakermarket.auction.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.project.sneakermarket.auction.dto.*;
import com.project.sneakermarket.auction.entity.AuctionProductImage;
import com.project.sneakermarket.auction.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionProduct;
import com.project.sneakermarket.auction.entity.AuctionBid;
import com.project.sneakermarket.common.exception.BusinessException;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * 競標查詢 Service。
 *
 * 負責處理競標列表等唯讀查詢邏輯，
 * 並將資料庫 Entity 轉換成前端需要的 Response DTO。
 */
@Service
@RequiredArgsConstructor
public class AuctionQueryService {

    // 競標列表允許使用的排序欄位
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "endAt",
            "createdAt",
            "currentPrice",
            "bidCount"
    );

    private final AuctionRepository auctionRepository;
    private final AuctionProductRepository auctionProductRepository;
    private final AuctionBidRepository auctionBidRepository;
    private final AuctionProductImageRepository auctionProductImageRepository;
    /**
     * 查詢全部競標列表。
     *
     * 目前保留給原本的 Controller 使用，
     * 後續 Controller 改成搜尋與分頁後會移除。
     *
     * @return 競標列表摘要資料
     */
    public List<AuctionSummaryResponse> getAllAuctions() {

        // 查詢所有競標資料
        List<Auction> auctions = auctionRepository.findAll();

        // 一次查出列表需要的競標商品資料
        Map<Long, AuctionProduct> productMap =
                getProductMap(auctions);

        // 將 Auction 與 AuctionProduct 組合成前端需要的 DTO
        return auctions.stream()
                .map(auction -> toSummaryResponse(
                        auction,
                        productMap.get(auction.getAuctionProductId())
                ))
                .toList();
    }

    /**
     * 依搜尋條件查詢競標列表並進行分頁。
     *
     * @param criteria 搜尋與篩選條件
     * @param pageable 分頁與排序條件
     * @return 分頁後的競標列表摘要資料
     */
    public PageData<AuctionSummaryResponse> searchAuctions(
            AuctionSearchCriteria criteria,
            Pageable pageable) {

        // 驗證價格範圍
        validatePriceRange(criteria);

        // 過濾前端傳入的排序欄位
        Pageable safePageable = createSafePageable(pageable);

        // 依搜尋條件查詢目前頁面的競標資料
        Page<Auction> auctionPage =
                auctionRepository.findAll(
                        AuctionSpecification.withCriteria(criteria),
                        safePageable
                );

        List<Auction> auctions = auctionPage.getContent();

        // 一次查出目前這一頁需要的競標商品資料
        Map<Long, AuctionProduct> productMap =
                getProductMap(auctions);

        List<AuctionSummaryResponse> items =
                auctions.stream()
                        .map(auction -> toSummaryResponse(
                                auction,
                                productMap.get(
                                        auction.getAuctionProductId()
                                )
                        ))
                        .toList();

        return new PageData<>(
                items,
                auctionPage.getNumber(),
                auctionPage.getSize(),
                auctionPage.getTotalElements(),
                auctionPage.getTotalPages()
        );
    }


    /**
     * 查詢指定競標的公開出價紀錄。
     *
     * 只回傳成功出價的金額、時間與遮蔽後的出價者資訊，
     * 不直接將 AuctionBid Entity 或 bidderId 暴露給前端。
     *
     * @param auctionId 競標 ID
     * @return 該場競標的公開出價紀錄
     */
    public List<BidHistoryResponse> getBidHistory(Long auctionId) {

        // 先確認競標本身存在。
        // 若 auctionId 不存在，不應把「不存在」誤認成「目前沒有出價」。
        if (!auctionRepository.existsById(auctionId)) {
            throw new ResourceNotFoundException(
                    "找不到指定的競標資料"
            );
        }

        // 查詢此競標所有成功出價，Repository 已依時間由新到舊排序
        List<AuctionBid> bids =
                auctionBidRepository
                        .findByAuctionIdOrderByCreatedAtDesc(
                                auctionId
                        );

        // 將 Entity 轉為公開 Response DTO
        return bids.stream()
                .map(this::toBidHistoryResponse)
                .toList();
    }

    /**
     * 查詢指定競標的完整詳細資料。
     *
     * 組合競標主資料、競標商品資料與商品圖片，
     * 並由後端計算下一次最低可出價金額。
     *
     * @param auctionId 競標 ID
     * @return 競標詳細資料
     */
    public AuctionDetailResponse getAuctionDetail(
            Long auctionId,
            Long currentMemberId) {

        // 查詢競標主資料；不存在時回 404
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "找不到指定的競標資料"
                        )
                );

        // 查詢此競標所對應的商品資料
        AuctionProduct product =
                auctionProductRepository
                        .findById(auction.getAuctionProductId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "找不到此競標的商品資料"
                                )
                        );


        /*
         * 判斷目前瀏覽者是否為此競標的賣家。
         *
         * 訪客 currentMemberId = null 時，
         * isOwner 一律為 false。
         */
        boolean isOwner =
                currentMemberId != null
                        && currentMemberId.equals(
                        product.getSellerId()
                );

        // 查詢商品圖片，Repository 已依 sortOrder 由小到大排序
        List<AuctionProductImage> images =
                auctionProductImageRepository
                        .findByAuctionProductIdOrderBySortOrderAsc(
                                product.getAuctionProductId()
                        );

        // 詳細頁目前只需要圖片 URL
        List<String> imageUrls = images.stream()
                .map(AuctionProductImage::getImageUrl)
                .toList();

        // 由後端統一計算下一次最低可出價金額
        var nextMinimumBid =
                auction.getBidCount() == 0
                        ? auction.getStartPrice()
                        : auction.getCurrentPrice()
                        .add(auction.getMinIncrement());

        // 組成詳細頁 Response DTO
        return new AuctionDetailResponse(
                auction.getAuctionId(),
                auction.getAuctionProductId(),
                product.getBrandId(),
                product.getCategoryId(),
                product.getProductName(),
                product.getModel(),
                product.getSize(),
                product.getCondition(),
                product.getDescription(),
                product.getDefectNote(),
                product.getAuthenticityNote(),
                product.getLocation(),
                imageUrls,
                product.getShippingMethod(),
                product.getShippingFee(),
                product.getPaymentMethod(),
                product.getAllowMeetup(),
                product.getAllowReturn(),
                product.getWarrantyNote(),
                auction.getStartPrice(),
                auction.getCurrentPrice(),
                auction.getMinIncrement(),
                nextMinimumBid,
                auction.getStartAt(),
                auction.getEndAt(),
                auction.getStatus(),
                auction.getBidCount(),
                auction.getCreatedAt(),
                isOwner
        );
    }



    /**
     * 驗證競標列表價格搜尋範圍。
     *
     * @param criteria 搜尋與篩選條件
     */
    private void validatePriceRange(
            AuctionSearchCriteria criteria) {

        // 最低價格與最高價格都有輸入時才需要比較
        if (criteria.getMinPrice() != null
                && criteria.getMaxPrice() != null
                && criteria.getMinPrice()
                .compareTo(criteria.getMaxPrice()) > 0) {

            throw new BusinessException(
                    "最低價格不可大於最高價格"
            );
        }
    }

    /**
     * 建立經過排序欄位白名單檢查的 Pageable。
     *
     * @param pageable 原始分頁與排序條件
     * @return 安全的分頁與排序條件
     */
    private Pageable createSafePageable(Pageable pageable) {

        // 只保留允許的排序欄位
        List<Sort.Order> safeOrders = pageable.getSort()
                .stream()
                .filter(order ->
                        ALLOWED_SORT_FIELDS.contains(
                                order.getProperty()
                        )
                )
                .toList();

        // 沒有合法排序條件時，預設依結束時間由近到遠排列
        Sort safeSort = safeOrders.isEmpty()
                ? Sort.by(
                Sort.Order.asc("endAt")
        )
                : Sort.by(safeOrders);

        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                safeSort
        );
    }

    /**
     * 批次查詢競標商品並轉成 Map。
     *
     * @param auctions 競標資料
     * @return 以競標商品 ID 為 Key 的商品 Map
     */
    private Map<Long, AuctionProduct> getProductMap(
            List<Auction> auctions) {

        // 取得目前列表所使用的商品 ID
        List<Long> productIds = auctions.stream()
                .map(Auction::getAuctionProductId)
                .toList();

        // 一次查詢商品資料，避免逐筆查詢產生 N+1
        return auctionProductRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(
                        AuctionProduct::getAuctionProductId,
                        Function.identity()
                ));
    }

    /**
     * 將競標資料與商品資料轉換成列表用 Response DTO。
     *
     * @param auction 競標主資料
     * @param product 競標商品資料
     * @return 競標列表摘要 DTO
     */
    private AuctionSummaryResponse toSummaryResponse(
            Auction auction,
            AuctionProduct product) {

        return new AuctionSummaryResponse(
                auction.getAuctionId(),
                auction.getAuctionProductId(),
                product.getProductName(),
                product.getModel(),
                product.getSize(),
                product.getCondition(),
                auction.getCurrentPrice(),
                auction.getBidCount(),
                auction.getCreatedAt(),
                auction.getEndAt(),
                auction.getStatus()
        );
    }

    /**
     * 將出價 Entity 轉換成公開出價紀錄 DTO。
     *
     * @param bid 出價紀錄 Entity
     * @return 公開出價紀錄 DTO
     */
    private BidHistoryResponse toBidHistoryResponse(
            AuctionBid bid) {

        return new BidHistoryResponse(
                bid.getBidAmount(),
                bid.getCreatedAt(),
                "匿名會員"
        );
    }
}