package com.project.sneakermarket.auction.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.project.sneakermarket.auction.entity.Auction;

/**
 * AuctionRepository 整合測試。
 *
 * 用來確認 Spring Data JPA 是否能正常從
 * dbo.auctions 讀取競標主資料。
 */
@SpringBootTest
class AuctionRepositoryTests {

    // 注入競標主資料 Repository
    @Autowired
    private AuctionRepository auctionRepository;

    /**
     * 測試是否可以從資料庫取得競標資料。
     */
    @Test
    void findAll_ShouldReturnAuctions() {

        // 查詢全部競標資料
        List<Auction> auctions = auctionRepository.findAll();

        // 印出查詢結果，方便確認 JPA 映射是否正確
        auctions.forEach(auction -> {
            System.out.println(
                    "競標 ID：" + auction.getAuctionId()
                            + "，目前價格：" + auction.getCurrentPrice()
                            + "，狀態：" + auction.getStatus()
            );
        });

        // 目前已有測試資料，因此結果不應為空
        assertFalse(auctions.isEmpty());
    }
}