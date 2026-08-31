package com.project.sneakermarket.auction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.project.sneakermarket.auction.entity.Auction;

/**
 * 競標主資料 Repository。
 * 負責存取 dbo.auctions 資料表。
 *
 * JpaRepository 已提供基本 CRUD 功能，
 * JpaSpecificationExecutor 則提供動態條件查詢能力。
 */
public interface AuctionRepository
        extends JpaRepository<Auction, Long>,
        JpaSpecificationExecutor<Auction> {

}