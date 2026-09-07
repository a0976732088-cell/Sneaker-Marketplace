package com.project.sneakermarket.auction.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.sneakermarket.auction.entity.AuctionProductImage;

/**
 * 競標商品圖片 Repository。
 * 負責存取 dbo.auction_product_images 資料表。
 */
public interface AuctionProductImageRepository
        extends JpaRepository<AuctionProductImage, Long> {

    /**
     * 依競標商品 ID 查詢所有圖片，
     * 並依 sortOrder 由小到大排列。
     *
     * @param auctionProductId 競標商品 ID
     * @return 此商品的圖片清單
     */
    List<AuctionProductImage> findByAuctionProductIdOrderBySortOrderAsc(
            Long auctionProductId
    );
}