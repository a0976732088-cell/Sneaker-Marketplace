package com.project.sneakermarket.auction.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import com.project.sneakermarket.auction.dto.AuctionUpdateRequest;
import com.project.sneakermarket.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import com.project.sneakermarket.auction.realtime.AuctionRealtimeEventPublisher;
import com.project.sneakermarket.auction.realtime.AuctionRealtimeEventType;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

    @MockitoBean
    private AuctionRealtimeEventPublisher realtimeEvents;

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

    /**
     * 驗證賣家在尚未有人出價時，
     * 可以成功取消自己的競標。
     *
     * 取消採狀態變更，
     * 不會實際刪除 Auction 或 AuctionProduct。
     */
    @Test
    void cancelAuction_NoBid_ShouldChangeStatusToCancelled() {

        LocalDateTime now =
                LocalDateTime.now().withNano(0);

        LocalDateTime startAt =
                now.plusHours(1);

        LocalDateTime endAt =
                now.plusDays(1);

        // 建立一場尚未開始、尚未有人出價的測試競標
        AuctionCreateRequest request =
                new AuctionCreateRequest(

                        1L,
                        1L,
                        "Auction Cancel Test Shoe",
                        "CANCEL-TEST-001",
                        "US 9",
                        AuctionCondition.NEW,
                        "取消競標整合測試商品",
                        null,
                        "測試商品",
                        "台北市",
                        "宅配",
                        new BigDecimal("60.00"),
                        "轉帳",
                        true,
                        false,
                        null,
                        new BigDecimal("5000.00"),
                        new BigDecimal("500.00"),
                        startAt,
                        endAt
                );

        Long sellerId = 1L;

        MockMultipartFile image =
                new MockMultipartFile(
                        "images",
                        "cancel-test.jpg",
                        "image/jpeg",
                        "fake-cancel-test-image".getBytes()
                );

        Long auctionId =
                auctionCommandService.createAuction(
                        sellerId,
                        request,
                        List.of(image)
                );

        /*
         * 建立後先確認這場競標確實沒有出價。
         */
        Auction beforeCancel =
                auctionRepository.findById(auctionId)
                        .orElseThrow(() ->
                                new AssertionError(
                                        "取消測試前找不到 Auction"
                                )
                        );

        assertEquals(
                AuctionStatus.SCHEDULED,
                beforeCancel.getStatus()
        );

        assertEquals(
                0,
                beforeCancel.getBidCount()
        );

        Long auctionProductId =
                beforeCancel.getAuctionProductId();

        // 執行取消
        auctionCommandService.cancelAuction(
                auctionId,
                sellerId
        );

        /*
         * 因為目前是在同一個 Persistence Context，
         * 直接重新取得該 Auction 驗證最新狀態。
         */
        Auction cancelledAuction =
                auctionRepository.findById(auctionId)
                        .orElseThrow(() ->
                                new AssertionError(
                                        "取消後 Auction 不應被刪除"
                                )
                        );

        // 取消只改狀態
        assertEquals(
                AuctionStatus.CANCELLED,
                cancelledAuction.getStatus()
        );

        // 出價次數不應被異動
        assertEquals(
                0,
                cancelledAuction.getBidCount()
        );

        /*
         * 商品本身也不應因取消競標而 DELETE。
         */
        assertTrue(
                auctionProductRepository
                        .existsById(auctionProductId),
                "取消競標後 AuctionProduct 仍應存在"
        );
        verify(realtimeEvents).publish(AuctionRealtimeEventType.AUCTION_CANCELLED, cancelledAuction);
    }

    /**
     * 驗證 SCHEDULED 且尚未有人出價時，
     * 賣家可以修改競標商品與競標設定。
     *
     * 同時確認：
     * 1. startPrice 修改時 currentPrice 必須同步。
     * 2. SCHEDULED 狀態可以修改 startAt / endAt。
     */
    @Test
    void updateAuction_ScheduledNoBid_ShouldUpdateSuccessfully() {

        LocalDateTime now =
                LocalDateTime.now().withNano(0);

        LocalDateTime originalStartAt =
                now.plusDays(2);

        LocalDateTime originalEndAt =
                now.plusDays(3);

        Long sellerId = 1L;

        /*
         * 先建立一場尚未開始、
         * 且沒有任何成功出價的競標。
         */
        AuctionCreateRequest createRequest =
                new AuctionCreateRequest(

                        1L,
                        1L,
                        "Auction Update Test Shoe",
                        "UPDATE-TEST-001",
                        "US 9",
                        AuctionCondition.NEW,
                        "修改競標整合測試商品",
                        null,
                        "測試商品",
                        "台北市",
                        "宅配",
                        new BigDecimal("60.00"),
                        "轉帳",
                        true,
                        false,
                        null,
                        new BigDecimal("5000.00"),
                        new BigDecimal("500.00"),
                        originalStartAt,
                        originalEndAt
                );

        MockMultipartFile image =
                new MockMultipartFile(
                        "images",
                        "update-test.jpg",
                        "image/jpeg",
                        "fake-update-test-image".getBytes()
                );

        Long auctionId =
                auctionCommandService.createAuction(
                        sellerId,
                        createRequest,
                        List.of(image)
                );

        // 修改後仍保持未來開始
        LocalDateTime newStartAt =
                now.plusDays(4);

        LocalDateTime newEndAt =
                now.plusDays(5);

        AuctionUpdateRequest updateRequest =
                new AuctionUpdateRequest(

                        1L,
                        1L,
                        "Auction Update Test Shoe Edited",
                        "UPDATE-TEST-002",
                        "US 9.5",
                        AuctionCondition.LIKE_NEW,
                        "修改後的競標商品說明",
                        "鞋盒輕微壓痕",
                        "附原鞋盒",
                        "新北市",
                        "宅配",
                        new BigDecimal("80.00"),
                        "轉帳",
                        true,
                        false,
                        "測試售後說明",
                        new BigDecimal("4500.00"),
                        new BigDecimal("300.00"),
                        newStartAt,
                        newEndAt
                );

        // 執行競標修改
        auctionCommandService.updateAuction(
                auctionId,
                sellerId,
                updateRequest
        );

        Auction updatedAuction =
                auctionRepository.findById(auctionId)
                        .orElseThrow(() ->
                                new AssertionError(
                                        "修改後找不到 Auction"
                                )
                        );

        AuctionProduct updatedProduct =
                auctionProductRepository
                        .findById(
                                updatedAuction.getAuctionProductId()
                        )
                        .orElseThrow(() ->
                                new AssertionError(
                                        "修改後找不到 AuctionProduct"
                                )
                        );

        // 商品資料應成功修改
        assertEquals(
                "Auction Update Test Shoe Edited",
                updatedProduct.getProductName()
        );

        assertEquals(
                "US 9.5",
                updatedProduct.getSize()
        );

        assertEquals(
                AuctionCondition.LIKE_NEW,
                updatedProduct.getCondition()
        );

        assertEquals(
                "修改後的競標商品說明",
                updatedProduct.getDescription()
        );

        /*
         * 尚未有人出價時，
         * 修改起標價必須同步 currentPrice。
         */
        assertEquals(
                0,
                new BigDecimal("4500.00")
                        .compareTo(
                                updatedAuction.getStartPrice()
                        )
        );

        assertEquals(
                0,
                new BigDecimal("4500.00")
                        .compareTo(
                                updatedAuction.getCurrentPrice()
                        )
        );

        assertEquals(
                0,
                new BigDecimal("300.00")
                        .compareTo(
                                updatedAuction.getMinIncrement()
                        )
        );

        // SCHEDULED 尚未開始，因此時間可以修改
        assertEquals(
                newStartAt,
                updatedAuction.getStartAt()
        );

        assertEquals(
                newEndAt,
                updatedAuction.getEndAt()
        );

        // 修改後開始時間仍在未來，所以狀態維持 SCHEDULED
        assertEquals(
                AuctionStatus.SCHEDULED,
                updatedAuction.getStatus()
        );

        // 編輯不應改變出價次數
        assertEquals(
                0,
                updatedAuction.getBidCount()
        );
    }

    /**
     * 驗證競標一旦已經存在成功出價，
     * 賣家就不能再修改競標內容。
     *
     * 此測試重點是確認：
     * 1. Service 會重新讀取目前 bidCount。
     * 2. bidCount > 0 時拒絕修改。
     * 3. 被拒絕後，商品名稱與價格都保持原值。
     */
    @Test
    void updateAuction_HasBid_ShouldRejectUpdate() {

        LocalDateTime now =
                LocalDateTime.now().withNano(0);

        LocalDateTime startAt =
                now.minusHours(1);

        LocalDateTime endAt =
                now.plusDays(1);

        Long sellerId = 1L;

        /*
         * 先建立一場目前已開始的競標。
         */
        AuctionCreateRequest createRequest =
                new AuctionCreateRequest(

                        1L,
                        1L,
                        "Auction Has Bid Test Shoe",
                        "HAS-BID-TEST-001",
                        "US 9",
                        AuctionCondition.NEW,
                        "已有出價後禁止修改測試",
                        null,
                        "測試商品",
                        "台北市",
                        "宅配",
                        new BigDecimal("60.00"),
                        "轉帳",
                        true,
                        false,
                        null,
                        new BigDecimal("5000.00"),
                        new BigDecimal("500.00"),
                        startAt,
                        endAt
                );

        MockMultipartFile image =
                new MockMultipartFile(
                        "images",
                        "has-bid-test.jpg",
                        "image/jpeg",
                        "fake-has-bid-test-image".getBytes()
                );

        Long auctionId =
                auctionCommandService.createAuction(
                        sellerId,
                        createRequest,
                        List.of(image)
                );

        /*
         * 本測試只驗證「編輯 Service 對 bidCount 的保護」，
         * 因此直接將 bidCount 模擬為已有成功出價。
         *
         * 真正 POST 出價與 auction_bids 寫入，
         * 會在功能 5 另外做完整 Transaction / Lock 測試。
         */
        Auction auction =
                auctionRepository.findById(auctionId)
                        .orElseThrow(() ->
                                new AssertionError(
                                        "測試前找不到 Auction"
                                )
                        );

        auction.setBidCount(1);

        auction.setCurrentPrice(
                new BigDecimal("5500.00")
        );

        auctionRepository.saveAndFlush(auction);

        // 保存修改前資料，後面確認不能被改掉
        AuctionProduct productBefore =
                auctionProductRepository
                        .findById(
                                auction.getAuctionProductId()
                        )
                        .orElseThrow(() ->
                                new AssertionError(
                                        "測試前找不到 AuctionProduct"
                                )
                        );

        String originalProductName =
                productBefore.getProductName();

        BigDecimal originalStartPrice =
                auction.getStartPrice();

        BigDecimal originalCurrentPrice =
                auction.getCurrentPrice();

        /*
         * 刻意準備一份完全不同的修改內容。
         * 如果 Service 規則失效，
         * 這些資料就可能被錯誤寫進 DB。
         */
        AuctionUpdateRequest updateRequest =
                new AuctionUpdateRequest(

                        1L,
                        1L,
                        "這個商品名稱不應該被修改",
                        "SHOULD-NOT-UPDATE",
                        "US 10",
                        AuctionCondition.LIKE_NEW,
                        "這段描述也不應該被修改",
                        "測試瑕疵",
                        "測試來源",
                        "新北市",
                        "宅配",
                        new BigDecimal("80.00"),
                        "轉帳",
                        true,
                        false,
                        "測試售後",
                        new BigDecimal("4000.00"),
                        new BigDecimal("200.00"),
                        startAt,
                        endAt.plusDays(1)
                );

        /*
         * 已有出價，因此預期 Service 拋出 BusinessException。
         */
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                auctionCommandService.updateAuction(
                                        auctionId,
                                        sellerId,
                                        updateRequest
                                )
                );

        assertEquals(
                "此競標已有出價紀錄，無法修改",
                exception.getMessage()
        );

        /*
         * 重新查詢資料，
         * 確認失敗操作沒有修改任何內容。
         */
        Auction auctionAfter =
                auctionRepository.findById(auctionId)
                        .orElseThrow(() ->
                                new AssertionError(
                                        "拒絕修改後找不到 Auction"
                                )
                        );

        AuctionProduct productAfter =
                auctionProductRepository
                        .findById(
                                auctionAfter.getAuctionProductId()
                        )
                        .orElseThrow(() ->
                                new AssertionError(
                                        "拒絕修改後找不到 AuctionProduct"
                                )
                        );

        // 商品名稱不得被修改
        assertEquals(
                originalProductName,
                productAfter.getProductName()
        );

        // 起標價不得被修改
        assertEquals(
                0,
                originalStartPrice.compareTo(
                        auctionAfter.getStartPrice()
                )
        );

        // 目前最高價不得被修改
        assertEquals(
                0,
                originalCurrentPrice.compareTo(
                        auctionAfter.getCurrentPrice()
                )
        );

        // 出價次數仍然維持 1
        assertEquals(
                1,
                auctionAfter.getBidCount()
        );
    }

    /**
     * 驗證競標一旦已有成功出價，
     * 賣家就不能再自行取消競標。
     *
     * 此測試確認：
     * 1. bidCount > 0 時取消會被拒絕。
     * 2. 競標狀態不可被改成 CANCELLED。
     * 3. 出價次數與目前價格不可被異動。
     */
    @Test
    void cancelAuction_HasBid_ShouldRejectCancel() {

        LocalDateTime now =
                LocalDateTime.now().withNano(0);

        Long sellerId = 1L;

        AuctionCreateRequest createRequest =
                new AuctionCreateRequest(

                        1L,
                        1L,
                        "Auction Cancel Has Bid Test Shoe",
                        "CANCEL-HAS-BID-001",
                        "US 9",
                        AuctionCondition.NEW,
                        "已有出價後禁止取消測試",
                        null,
                        "測試商品",
                        "台北市",
                        "宅配",
                        new BigDecimal("60.00"),
                        "轉帳",
                        true,
                        false,
                        null,
                        new BigDecimal("5000.00"),
                        new BigDecimal("500.00"),

                        // 建立成目前進行中的 ACTIVE 競標
                        now.minusHours(1),
                        now.plusDays(1)
                );

        MockMultipartFile image =
                new MockMultipartFile(
                        "images",
                        "cancel-has-bid-test.jpg",
                        "image/jpeg",
                        "fake-cancel-has-bid-image".getBytes()
                );

        Long auctionId =
                auctionCommandService.createAuction(
                        sellerId,
                        createRequest,
                        List.of(image)
                );

        /*
         * 功能 5 尚未開始，
         * 所以目前直接模擬「已有成功出價」後
         * auctions 應有的狀態。
         */
        Auction auction =
                auctionRepository.findById(auctionId)
                        .orElseThrow(() ->
                                new AssertionError(
                                        "取消測試前找不到 Auction"
                                )
                        );

        auction.setBidCount(1);

        auction.setCurrentPrice(
                new BigDecimal("5500.00")
        );

        auction.setHighestBidderId(2L);

        auctionRepository.saveAndFlush(auction);

        // 保存原始資料供後續驗證
        AuctionStatus originalStatus =
                auction.getStatus();

        BigDecimal originalCurrentPrice =
                auction.getCurrentPrice();

        Long originalHighestBidderId =
                auction.getHighestBidderId();

        /*
         * 已有成功出價後，
         * 賣家取消應被 BusinessException 拒絕。
         */
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                auctionCommandService.cancelAuction(
                                        auctionId,
                                        sellerId
                                )
                );

        assertEquals(
                "此競標已有出價紀錄，無法自行取消",
                exception.getMessage()
        );

        /*
         * 重新讀取競標，
         * 確認拒絕取消後資料沒有被改寫。
         */
        Auction auctionAfter =
                auctionRepository.findById(auctionId)
                        .orElseThrow(() ->
                                new AssertionError(
                                        "拒絕取消後找不到 Auction"
                                )
                        );

        // 不可被改成 CANCELLED
        assertEquals(
                originalStatus,
                auctionAfter.getStatus()
        );

        // 成功出價次數保持不變
        assertEquals(
                1,
                auctionAfter.getBidCount()
        );

        // 目前價格保持不變
        assertEquals(
                0,
                originalCurrentPrice.compareTo(
                        auctionAfter.getCurrentPrice()
                )
        );

        // 最高出價者保持不變
        assertEquals(
                originalHighestBidderId,
                auctionAfter.getHighestBidderId()
        );
        verifyNoInteractions(realtimeEvents);
    }

    /**
     * 驗證 ACTIVE 且尚未有人出價時，
     * 賣家不可修改已經發生的競標開始時間。
     *
     * 即使 bidCount = 0，
     * startAt 仍屬於已發生的歷史資訊。
     */
    @Test
    void updateAuction_ActiveNoBidChangeStartAt_ShouldRejectUpdate() {

        LocalDateTime now =
                LocalDateTime.now().withNano(0);

        Long sellerId = 1L;

        LocalDateTime originalStartAt =
                now.minusHours(1);

        LocalDateTime originalEndAt =
                now.plusDays(1);

        /*
         * startAt 已經過去，
         * 因此建立後狀態應為 ACTIVE。
         */
        AuctionCreateRequest createRequest =
                new AuctionCreateRequest(

                        1L,
                        1L,
                        "Auction Active StartAt Test Shoe",
                        "ACTIVE-START-TEST-001",
                        "US 9",
                        AuctionCondition.NEW,
                        "ACTIVE 開始時間禁止修改測試",
                        null,
                        "測試商品",
                        "台北市",
                        "宅配",
                        new BigDecimal("60.00"),
                        "轉帳",
                        true,
                        false,
                        null,
                        new BigDecimal("5000.00"),
                        new BigDecimal("500.00"),
                        originalStartAt,
                        originalEndAt
                );

        MockMultipartFile image =
                new MockMultipartFile(
                        "images",
                        "active-start-test.jpg",
                        "image/jpeg",
                        "fake-active-start-test-image".getBytes()
                );

        Long auctionId =
                auctionCommandService.createAuction(
                        sellerId,
                        createRequest,
                        List.of(image)
                );

        Auction beforeUpdate =
                auctionRepository.findById(auctionId)
                        .orElseThrow(() ->
                                new AssertionError(
                                        "測試前找不到 Auction"
                                )
                        );

        // 確認測試前提
        assertEquals(
                AuctionStatus.ACTIVE,
                beforeUpdate.getStatus()
        );

        assertEquals(
                0,
                beforeUpdate.getBidCount()
        );

        /*
         * 故意修改 startAt。
         *
         * ACTIVE 後即使沒有出價，
         * 這個修改仍必須被拒絕。
         */
        LocalDateTime illegalNewStartAt =
                originalStartAt.plusMinutes(30);

        AuctionUpdateRequest updateRequest =
                new AuctionUpdateRequest(

                        1L,
                        1L,
                        "Auction Active StartAt Test Shoe",
                        "ACTIVE-START-TEST-001",
                        "US 9",
                        AuctionCondition.NEW,
                        "這段描述不應因失敗操作而被修改",
                        null,
                        "測試商品",
                        "台北市",
                        "宅配",
                        new BigDecimal("60.00"),
                        "轉帳",
                        true,
                        false,
                        null,
                        new BigDecimal("4500.00"),
                        new BigDecimal("300.00"),

                        // 故意與原本 startAt 不同
                        illegalNewStartAt,

                        originalEndAt.plusHours(1)
                );

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                auctionCommandService.updateAuction(
                                        auctionId,
                                        sellerId,
                                        updateRequest
                                )
                );

        assertEquals(
                "競標開始後不可修改開始時間",
                exception.getMessage()
        );

        /*
         * 重新讀取並確認失敗操作沒有污染資料。
         */
        Auction afterUpdate =
                auctionRepository.findById(auctionId)
                        .orElseThrow(() ->
                                new AssertionError(
                                        "拒絕修改後找不到 Auction"
                                )
                        );

        assertEquals(
                originalStartAt,
                afterUpdate.getStartAt()
        );

        assertEquals(
                AuctionStatus.ACTIVE,
                afterUpdate.getStatus()
        );

        assertEquals(
                0,
                afterUpdate.getBidCount()
        );

        // 起標價也不能因整筆失敗 Request 而被修改
        assertEquals(
                0,
                new BigDecimal("5000.00")
                        .compareTo(
                                afterUpdate.getStartPrice()
                        )
        );

        assertEquals(
                0,
                new BigDecimal("5000.00")
                        .compareTo(
                                afterUpdate.getCurrentPrice()
                        )
        );
    }

}
