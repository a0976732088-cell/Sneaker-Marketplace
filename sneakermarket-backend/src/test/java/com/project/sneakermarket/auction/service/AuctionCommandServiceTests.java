package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.project.sneakermarket.auction.dto.AuctionCreateRequest;
import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionCondition;
import com.project.sneakermarket.auction.entity.AuctionProduct;
import com.project.sneakermarket.auction.entity.AuctionProductImage;
import com.project.sneakermarket.auction.entity.AuctionStatus;
import com.project.sneakermarket.auction.repository.AuctionProductImageRepository;
import com.project.sneakermarket.auction.repository.AuctionProductRepository;
import com.project.sneakermarket.auction.repository.AuctionRepository;

import java.time.LocalDateTime;

/**
 * AuctionCommandService 整合測試。
 *
 * 驗證建立競標時：
 * 1. auction_products 正常建立
 * 2. 圖片檔案正常寫入
 * 3. auction_product_images 正常建立
 * 4. auctions 正常建立
 *
 * DB 資料會由 @Transactional 在測試結束後 Rollback。
 * 圖片檔案則另外使用 target/test-uploads 測試目錄。
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "auction.image.upload-dir=target/test-uploads/auctions"
})
class AuctionCommandServiceTests {

    private static final Path TEST_UPLOAD_ROOT =
            Path.of("target/test-uploads/auctions")
                    .toAbsolutePath()
                    .normalize();

    @Autowired
    private AuctionCommandService auctionCommandService;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private AuctionProductRepository auctionProductRepository;

    @Autowired
    private AuctionProductImageRepository auctionProductImageRepository;

    /**
     * 驗證完整建立競標流程。
     */
    @Test
    void createAuction_ShouldCreateProductImageAndAuction() {

        // DATETIME2(0) 為秒級精度，因此測試時間移除奈秒。
        LocalDateTime now =
                LocalDateTime.now().withNano(0);

        LocalDateTime startAt =
                now.plusHours(1);

        LocalDateTime endAt =
                now.plusDays(1);

        // 建立競標 Request
        AuctionCreateRequest request =
                new AuctionCreateRequest(

                        // brandId
                        1L,

                        // categoryId
                        1L,

                        // productName
                        "Auction Command Test Shoe",

                        // model
                        "TEST-001",

                        // size
                        "US 9",

                        // condition
                        AuctionCondition.NEW,

                        // description
                        "AuctionCommandService 整合測試商品",

                        // defectNote
                        null,

                        // authenticityNote
                        "測試用真偽說明",

                        // location
                        "台北市",

                        // shippingMethod
                        "宅配",

                        // shippingFee
                        new BigDecimal("60.00"),

                        // paymentMethod
                        "轉帳",

                        // allowMeetup
                        true,

                        // allowReturn
                        false,

                        // warrantyNote
                        null,

                        // startPrice
                        new BigDecimal("5000.00"),

                        // minIncrement
                        new BigDecimal("500.00"),

                        // startAt
                        startAt,

                        // endAt
                        endAt
                );

        /*
         * 目前 Session 尚未正式接入 Controller，
         * 所以 Service Test 直接傳入測試 sellerId。
         */
        Long sellerId = 1L;

        // 模擬使用者上傳一張 JPG。
        MockMultipartFile image =
                new MockMultipartFile(
                        "images",
                        "test-shoe.jpg",
                        "image/jpeg",
                        "fake-auction-image".getBytes()
                );

        // 執行建立競標
        Long auctionId =
                auctionCommandService.createAuction(
                        sellerId,
                        request,
                        List.of(image)
                );

        // ========================================
        // 1. 驗證 Auction
        // ========================================

        assertNotNull(auctionId);

        Auction auction =
                auctionRepository.findById(auctionId)
                        .orElseThrow(() ->
                                new AssertionError(
                                        "建立後找不到 Auction"
                                )
                        );

        assertNotNull(
                auction.getAuctionProductId()
        );

        assertEquals(
                AuctionStatus.SCHEDULED,
                auction.getStatus()
        );

        assertEquals(
                0,
                new BigDecimal("5000.00")
                        .compareTo(
                                auction.getStartPrice()
                        )
        );

        assertEquals(
                0,
                new BigDecimal("5000.00")
                        .compareTo(
                                auction.getCurrentPrice()
                        )
        );

        assertEquals(
                0,
                auction.getBidCount()
        );

        assertEquals(
                null,
                auction.getHighestBidderId()
        );

        // ========================================
        // 2. 驗證 AuctionProduct
        // ========================================

        AuctionProduct product =
                auctionProductRepository
                        .findById(
                                auction.getAuctionProductId()
                        )
                        .orElseThrow(() ->
                                new AssertionError(
                                        "建立後找不到 AuctionProduct"
                                )
                        );

        assertEquals(
                sellerId,
                product.getSellerId()
        );

        assertEquals(
                "Auction Command Test Shoe",
                product.getProductName()
        );

        assertEquals(
                "TEST-001",
                product.getModel()
        );

        assertEquals(
                "US 9",
                product.getSize()
        );

        assertEquals(
                AuctionCondition.NEW,
                product.getCondition()
        );

        // ========================================
        // 3. 驗證 AuctionProductImage
        // ========================================

        List<AuctionProductImage> productImages =
                auctionProductImageRepository
                        .findByAuctionProductIdOrderBySortOrderAsc(
                                product.getAuctionProductId()
                        );

        assertEquals(
                1,
                productImages.size()
        );

        AuctionProductImage productImage =
                productImages.get(0);

        assertEquals(
                0,
                productImage.getSortOrder()
        );

        assertTrue(
                productImage.getIsPrimary()
        );

        assertTrue(
                productImage.getImageUrl()
                        .startsWith(
                                "/uploads/auctions/"
                                        + product.getAuctionProductId()
                                        + "/"
                        )
        );

        // ========================================
        // 4. 驗證實體圖片真的存在
        // ========================================

        String relativeImagePath =
                productImage.getImageUrl()
                        .substring(
                                "/uploads/auctions/"
                                        .length()
                        );

        Path storedImage =
                TEST_UPLOAD_ROOT
                        .resolve(relativeImagePath)
                        .normalize();

        assertTrue(
                Files.exists(storedImage),
                "競標圖片應該已經實際寫入測試目錄"
        );

        assertFalse(
                Files.isDirectory(storedImage),
                "圖片路徑不應該是資料夾"
        );

        // 開發階段方便人工確認
        System.out.println(
                "建立競標成功："
                        + "auctionId=" + auctionId
                        + ", auctionProductId="
                        + product.getAuctionProductId()
                        + ", imageUrl="
                        + productImage.getImageUrl()
        );
    }

    /**
     * 每次測試後清除本機測試圖片。
     *
     * DB 可以 Rollback，
     * 但檔案系統不會跟著 Transaction Rollback。
     */
    @AfterEach
    void cleanupTestImages() throws IOException {

        if (!Files.exists(TEST_UPLOAD_ROOT)) {
            return;
        }

        /*
         * 由最深層檔案開始刪除，
         * 最後才刪除資料夾。
         */
        try (var paths = Files.walk(TEST_UPLOAD_ROOT)) {

            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {

                        try {
                            Files.deleteIfExists(path);

                        } catch (IOException e) {
                            throw new RuntimeException(
                                    "測試圖片清理失敗：" + path,
                                    e
                            );
                        }
                    });
        }
    }
}