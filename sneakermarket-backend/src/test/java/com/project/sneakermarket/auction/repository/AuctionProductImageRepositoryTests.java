package com.project.sneakermarket.auction.repository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.project.sneakermarket.auction.entity.AuctionProduct;
import com.project.sneakermarket.auction.entity.AuctionProductImage;

/**
 * AuctionProductImageRepository 整合測試。
 *
 * 用來確認 Spring Data JPA 是否能正常存取
 * dbo.auction_product_images，並依商品 ID 與排序欄位查詢圖片。
 */
@SpringBootTest
@Transactional
class AuctionProductImageRepositoryTests {

    // 注入競標商品圖片 Repository
    @Autowired
    private AuctionProductImageRepository auctionProductImageRepository;

    // 注入既有競標商品 Repository，取得測試用商品
    @Autowired
    private AuctionProductRepository auctionProductRepository;

    /**
     * 測試競標商品圖片是否可以正常新增，
     * 並依 sortOrder 由小到大查詢。
     */
    @Test
    void findByAuctionProductIdOrderBySortOrderAsc_ShouldReturnImagesInOrder() {

        // 取得資料庫中既有的一筆競標商品，
        // 避免測試硬編 auctionProductId = 1。
        AuctionProduct product = auctionProductRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("目前沒有競標商品測試資料"));

        // 建立第一張測試圖片
        AuctionProductImage image1 = new AuctionProductImage();
        image1.setAuctionProductId(product.getAuctionProductId());
        image1.setImageUrl("/test/auction/image-front.jpg");
        image1.setSortOrder(9001);
        image1.setIsPrimary(true);
        image1.setCreatedAt(LocalDateTime.now());

        // 建立第二張測試圖片
        AuctionProductImage image2 = new AuctionProductImage();
        image2.setAuctionProductId(product.getAuctionProductId());
        image2.setImageUrl("/test/auction/image-side.jpg");
        image2.setSortOrder(9002);
        image2.setIsPrimary(false);
        image2.setCreatedAt(LocalDateTime.now());

        // 寫入測試圖片資料
        auctionProductImageRepository.saveAndFlush(image1);
        auctionProductImageRepository.saveAndFlush(image2);

        // 確認 Identity 主鍵有正常產生
        assertNotNull(image1.getImageId());
        assertNotNull(image2.getImageId());

        // 依商品 ID 查詢圖片，並由 sortOrder 小到大排序
        List<AuctionProductImage> images =
                auctionProductImageRepository
                        .findByAuctionProductIdOrderBySortOrderAsc(
                                product.getAuctionProductId()
                        );

        // 找出剛才新增的兩張圖片在查詢結果中的位置
        int image1Index = findImageIndex(images, image1.getImageId());
        int image2Index = findImageIndex(images, image2.getImageId());

        // 兩張圖片都必須查得到
        assertTrue(image1Index >= 0);
        assertTrue(image2Index >= 0);

        // sortOrder 9001 必須排在 9002 前面
        assertTrue(image1Index < image2Index);

        // 印出查詢結果，方便開發階段確認
        images.forEach(image -> {
            System.out.println(
                    "圖片 ID：" + image.getImageId()
                            + "，商品 ID：" + image.getAuctionProductId()
                            + "，路徑：" + image.getImageUrl()
                            + "，排序：" + image.getSortOrder()
                            + "，主圖：" + image.getIsPrimary()
            );
        });
    }

    /**
     * 依圖片 ID 找出圖片在查詢結果中的位置。
     *
     * @param images  圖片清單
     * @param imageId 圖片 ID
     * @return 圖片所在位置；找不到時回 -1
     */
    private int findImageIndex(
            List<AuctionProductImage> images,
            Long imageId
    ) {

        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getImageId().equals(imageId)) {
                return i;
            }
        }

        return -1;
    }
}