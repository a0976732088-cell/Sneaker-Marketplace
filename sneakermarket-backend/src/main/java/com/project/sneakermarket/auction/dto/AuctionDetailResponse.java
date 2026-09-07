package com.project.sneakermarket.auction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.project.sneakermarket.auction.entity.AuctionCondition;
import com.project.sneakermarket.auction.entity.AuctionStatus;

/**
 * 競標詳細資料 Response DTO。
 *
 * 用於 GET /api/auctions/{id}，
 * 回傳單一競標詳細頁需要的商品、圖片、價格、
 * 時間、交易條件與競標狀態資訊。
 */
public record AuctionDetailResponse(

        // 競標 ID
        Long auctionId,

        // 競標商品 ID
        Long auctionProductId,

        // 商品品牌 ID，正式品牌名稱待商城模組整合
        Long brandId,

        // 商品分類 ID，正式分類名稱待商城模組整合
        Long categoryId,

        // 商品名稱
        String productName,

        // 商品型號／系列
        String model,

        // 球鞋尺寸
        String size,

        // 商品狀況
        AuctionCondition condition,

        // 商品詳細介紹
        String description,

        // 商品瑕疵說明
        String defectNote,

        // 真偽／來源說明
        String authenticityNote,

        // 商品所在地
        String location,

        // 商品圖片 URL，依 sortOrder 排序
        List<String> imageUrls,

        // 運送方式
        String shippingMethod,

        // 運費
        BigDecimal shippingFee,

        // 付款方式
        String paymentMethod,

        // 是否允許面交
        Boolean allowMeetup,

        // 是否允許退貨
        Boolean allowReturn,

        // 保固／售後說明
        String warrantyNote,

        // 起標價格
        BigDecimal startPrice,

        // 目前最高價格
        BigDecimal currentPrice,

        // 每次最低加價金額
        BigDecimal minIncrement,

        // 下一次最低可出價金額
        BigDecimal nextMinimumBid,

        // 競標開始時間
        LocalDateTime startAt,

        // 競標結束時間
        LocalDateTime endAt,

        // 競標狀態
        AuctionStatus status,

        // 成功出價次數
        Integer bidCount,

        // 商品刊登建立時間
        LocalDateTime createdAt,

        // 目前瀏覽者是否為此競標的賣家本人
        Boolean isOwner

) {
}