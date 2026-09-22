package com.project.sneakermarket.auction.dto;

import java.math.BigDecimal;
import java.util.List;

import com.project.sneakermarket.auction.entity.AuctionCondition;
import com.project.sneakermarket.auction.entity.AuctionStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 競標列表搜尋條件 DTO。
 *
 * 用來接收競標商城列表的搜尋與篩選條件。
 * 未提供的欄位代表不使用該條件進行篩選。
 */
@Getter
@Setter
@NoArgsConstructor
public class AuctionSearchCriteria {

    // 商品名稱或型號搜尋關鍵字
    private String keyword;

    // 商品品牌 ID
    private Long brandId;

    // 商品品牌 ID 多選；保留 brandId 以相容既有單值 Query Parameter
    private List<Long> brandIds;

    // 商品分類 ID
    private Long categoryId;

    // 商品分類 ID 多選；保留 categoryId 以相容既有單值 Query Parameter
    private List<Long> categoryIds;

    // 球鞋尺寸，例如 US 9、US 10
    private String shoeSize;

    // 球鞋尺寸多選；保留 shoeSize 以相容既有單值 Query Parameter
    private List<String> shoeSizes;

    // 商品狀況：NEW、LIKE_NEW、USED
    private AuctionCondition condition;

    // 商品狀況多選；保留 condition 以相容既有單值 Query Parameter
    private List<AuctionCondition> conditions;

    // 競標狀態
    private AuctionStatus status;

    // 競標狀態多選；保留 status 以相容既有單值 Query Parameter
    private List<AuctionStatus> statuses;

    // 最低目前價格
    private BigDecimal minPrice;

    // 最高目前價格
    private BigDecimal maxPrice;
}
