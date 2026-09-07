package com.project.sneakermarket.auction.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.project.sneakermarket.auction.entity.AuctionProduct;

/**
 * AuctionProductRepository 整合測試。
 *
 * 用來確認 Spring Data JPA 是否能正常連接 SneakerMarketDB，
 * 並從 dbo.auction_products 讀取競標商品資料。
 */
@SpringBootTest
class AuctionProductRepositoryTests {

    // 注入競標商品 Repository
    @Autowired
    private AuctionProductRepository auctionProductRepository;

    /**
     * 測試是否可以從資料庫取得競標商品。
     */
    @Test
    void findAll_ShouldReturnAuctionProducts() {

        // 從 auction_products 查詢全部競標商品
        List<AuctionProduct> products = auctionProductRepository.findAll();

        // 印出查詢結果，方便開發階段確認資料
        products.forEach(product -> {
            System.out.println(
                    "競標商品 ID：" + product.getAuctionProductId()
                            + "，商品名稱：" + product.getProductName()
            );
        });

        // 資料庫目前已有測試資料，因此結果不應為空
        assertFalse(products.isEmpty());
    }
}