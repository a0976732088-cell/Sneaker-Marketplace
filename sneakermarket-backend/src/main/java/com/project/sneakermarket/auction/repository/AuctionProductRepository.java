package com.project.sneakermarket.auction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.project.sneakermarket.auction.entity.AuctionProduct;

/**
 * 競標商品 Repository。
 * 負責存取 dbo.auction_products 資料表。
 *
 * JpaRepository 已提供基本 CRUD 功能，
 * JpaSpecificationExecutor 則提供動態條件查詢能力。
 */
public interface AuctionProductRepository
        extends JpaRepository<AuctionProduct, Long>,
        JpaSpecificationExecutor<AuctionProduct> {

}