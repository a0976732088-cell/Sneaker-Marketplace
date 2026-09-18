package com.project.sneakermarket.auction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.project.sneakermarket.auction.entity.AuctionCondition;
import com.project.sneakermarket.auction.entity.AuctionStatus;

/**
 * 競標列表摘要 Response DTO。
 * <p>
 * 用來回傳競標商城列表頁所需要的資料。
 * 不直接把 Auction Entity 傳給前端，
 * 避免暴露不必要的資料庫欄位。
 */
public record AuctionSummaryResponse(

        // 競標 ID
        Long auctionId,

        // 競標商品 ID
        Long auctionProductId,

        // 商品名稱
        String productName,

        // 賣家會員名稱
        String sellerName,

        // 球鞋型號
        String model,

        // 球鞋尺寸
        String size,

        // 商品狀況
        AuctionCondition condition,

        // 目前競標價格
        BigDecimal currentPrice,

        // 成功出價次數
        Integer bidCount,

        // 競標上架時間
        LocalDateTime createdAt,

        // 競標結束時間
        LocalDateTime endAt,

        // 競標目前狀態
        AuctionStatus status,
        Long brandId,
        Long categoryId,
        String brandName,
        String categoryName,

        // 商品主圖；沒有圖片時為 null
        String imageUrl

) {
}
