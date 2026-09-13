package com.project.sneakermarket.auction.dto;

import java.math.BigDecimal;
import java.util.List;

import com.project.sneakermarket.auction.entity.AuctionCondition;

/** 管理員專用唯讀詳情，包含下架及刪除後仍保留的商品資料。 */
public record AuctionAdminDetailResponse(
        AuctionAdminSummaryResponse auction,
        String size,
        AuctionCondition condition,
        String description,
        String defectNote,
        String authenticityNote,
        String location,
        String shippingMethod,
        BigDecimal shippingFee,
        String paymentMethod,
        Boolean allowMeetup,
        Boolean allowReturn,
        String warrantyNote,
        List<String> imageUrls
) {
}
