package com.project.sneakermarket.auction.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.project.sneakermarket.auction.dto.AuctionCreateRequest;
import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionProduct;
import com.project.sneakermarket.auction.entity.AuctionProductImage;
import com.project.sneakermarket.auction.entity.AuctionStatus;
import com.project.sneakermarket.auction.repository.AuctionProductImageRepository;
import com.project.sneakermarket.auction.repository.AuctionProductRepository;
import com.project.sneakermarket.auction.repository.AuctionRepository;
import com.project.sneakermarket.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * 競標寫入 Service。
 *
 * 負責建立、修改、取消等會改變競標資料的操作。
 *
 * 查詢功能仍由 AuctionQueryService 負責，
 * 避免將讀取與寫入邏輯全部集中在同一個 Service。
 */
@Service
@RequiredArgsConstructor
public class AuctionCommandService {

    private static final int MAX_IMAGE_COUNT = 5;

    private final AuctionProductRepository auctionProductRepository;
    private final AuctionProductImageRepository auctionProductImageRepository;
    private final AuctionRepository auctionRepository;
    private final ImageStorageService imageStorageService;

    /**
     * 建立一場新的競標。
     *
     * @param sellerId 賣家會員 ID，正式呼叫時由 HttpSession 取得
     * @param request  建立競標表單資料
     * @param images   競標商品圖片
     * @return 建立完成的 auctionId
     */
    @Transactional
    public Long createAuction(
            Long sellerId,
            AuctionCreateRequest request,
            List<MultipartFile> images) {

        validateSellerId(sellerId);
        validateAuctionTime(request);
        validateImages(images);

        LocalDateTime now = LocalDateTime.now();

        /*
         * 紀錄本次已成功寫入檔案系統的圖片。
         *
         * 如果後面的 DB 操作失敗，
         * @Transactional 可以 Rollback DB，
         * 但硬碟檔案不會自動 Rollback，
         * 因此需要另外清理。
         */
        List<String> storedImageUrls = new ArrayList<>();

        try {

            // 1. 建立競標商品
            AuctionProduct product =
                    createAuctionProduct(
                            sellerId,
                            request,
                            now
                    );

            /*
             * 使用 IDENTITY 主鍵，
             * saveAndFlush 後即可取得 auctionProductId，
             * 後面的圖片資料夾也會使用這個 ID。
             */
            product =
                    auctionProductRepository
                            .saveAndFlush(product);

            // 2. 將圖片寫入本機
            List<AuctionProductImage> productImages =
                    createProductImages(
                            product.getAuctionProductId(),
                            images,
                            storedImageUrls,
                            now
                    );

            // 3. 建立圖片資料庫紀錄
            auctionProductImageRepository
                    .saveAllAndFlush(productImages);

            // 4. 建立競標主資料
            Auction auction =
                    createAuctionEntity(
                            product.getAuctionProductId(),
                            request,
                            now
                    );

            auction =
                    auctionRepository
                            .saveAndFlush(auction);

            return auction.getAuctionId();

        } catch (RuntimeException ex) {

            /*
             * DB Transaction 會因例外 Rollback。
             *
             * 但已經寫進本機 uploads 的圖片不屬於 DB Transaction，
             * 所以這裡做補償刪除。
             */
            cleanupStoredImages(
                    storedImageUrls,
                    ex
            );

            throw ex;
        }
    }

    /**
     * 建立 AuctionProduct Entity。
     */
    private AuctionProduct createAuctionProduct(
            Long sellerId,
            AuctionCreateRequest request,
            LocalDateTime now) {

        AuctionProduct product = new AuctionProduct();

        product.setSellerId(sellerId);
        product.setBrandId(request.brandId());
        product.setCategoryId(request.categoryId());
        product.setProductName(request.productName());
        product.setModel(request.model());
        product.setSize(request.size());
        product.setCondition(request.condition());
        product.setDescription(request.description());
        product.setDefectNote(request.defectNote());
        product.setAuthenticityNote(
                request.authenticityNote()
        );
        product.setLocation(request.location());

        product.setShippingMethod(
                request.shippingMethod()
        );
        product.setShippingFee(
                request.shippingFee()
        );
        product.setPaymentMethod(
                request.paymentMethod()
        );
        product.setAllowMeetup(
                request.allowMeetup()
        );
        product.setAllowReturn(
                request.allowReturn()
        );
        product.setWarrantyNote(
                request.warrantyNote()
        );

        product.setCreatedAt(now);
        product.setUpdatedAt(now);

        return product;
    }

    /**
     * 儲存圖片並建立 AuctionProductImage Entity。
     */
    private List<AuctionProductImage> createProductImages(
            Long auctionProductId,
            List<MultipartFile> images,
            List<String> storedImageUrls,
            LocalDateTime now) {

        List<AuctionProductImage> productImages =
                new ArrayList<>();

        for (int i = 0; i < images.size(); i++) {

            MultipartFile image = images.get(i);

            // 寫入實體圖片檔案
            String imageUrl =
                    imageStorageService.store(
                            image,
                            auctionProductId
                    );

            storedImageUrls.add(imageUrl);

            // 建立對應圖片 DB 資料
            AuctionProductImage productImage =
                    new AuctionProductImage();

            productImage.setAuctionProductId(
                    auctionProductId
            );
            productImage.setImageUrl(imageUrl);

            // 第一張圖片排序為 0，後面依序增加
            productImage.setSortOrder(i);

            // 第一張圖片預設作為主圖
            productImage.setIsPrimary(i == 0);

            productImage.setCreatedAt(now);

            productImages.add(productImage);
        }

        return productImages;
    }

    /**
     * 建立 Auction Entity。
     */
    private Auction createAuctionEntity(
            Long auctionProductId,
            AuctionCreateRequest request,
            LocalDateTime now) {

        Auction auction = new Auction();

        auction.setAuctionProductId(
                auctionProductId
        );

        auction.setStartPrice(
                request.startPrice()
        );

        /*
         * 尚未有任何出價時，
         * currentPrice 先等於起標價。
         */
        auction.setCurrentPrice(
                request.startPrice()
        );

        auction.setMinIncrement(
                request.minIncrement()
        );

        auction.setStartAt(
                request.startAt()
        );

        auction.setEndAt(
                request.endAt()
        );

        /*
         * 未來開始 → SCHEDULED
         * 已到開始時間 → ACTIVE
         */
        AuctionStatus status =
                request.startAt().isAfter(now)
                        ? AuctionStatus.SCHEDULED
                        : AuctionStatus.ACTIVE;

        auction.setStatus(status);

        // 尚未有人出價
        auction.setHighestBidderId(null);
        auction.setBidCount(0);

        /*
         * version 為 @Version，
         * 交由 Hibernate 管理，
         * 不由建立競標流程自行修改。
         */

        auction.setCreatedAt(now);
        auction.setUpdatedAt(now);

        return auction;
    }

    /**
     * 驗證建立競標使用的會員 ID。
     *
     * 正式 Controller 會先從 HttpSession 判斷是否登入；
     * 此處仍保留 Service 最後一道資料防線。
     */
    private void validateSellerId(Long sellerId) {

        if (sellerId == null || sellerId <= 0) {
            throw new BusinessException(
                    "會員身分資料不正確"
            );
        }
    }

    /**
     * 驗證競標時間。
     */
    private void validateAuctionTime(
            AuctionCreateRequest request) {

        if (!request.endAt()
                .isAfter(request.startAt())) {

            throw new BusinessException(
                    "競標結束時間必須晚於開始時間"
            );
        }

        /*
         * 不允許建立一筆建立完成時
         * 就已經過期的競標。
         */
        if (!request.endAt()
                .isAfter(LocalDateTime.now())) {

            throw new BusinessException(
                    "競標結束時間必須晚於目前時間"
            );
        }
    }

    /**
     * 驗證圖片數量。
     */
    private void validateImages(
            List<MultipartFile> images) {

        if (images == null || images.isEmpty()) {
            throw new BusinessException(
                    "建立競標至少需要上傳一張圖片"
            );
        }

        if (images.size() > MAX_IMAGE_COUNT) {
            throw new BusinessException(
                    "競標商品最多只能上傳 5 張圖片"
            );
        }
    }

    /**
     * 發生建立失敗時，
     * 清除本次已經寫入本機的圖片。
     */
    private void cleanupStoredImages(
            List<String> storedImageUrls,
            RuntimeException originalException) {

        for (String imageUrl : storedImageUrls) {

            try {
                imageStorageService.delete(imageUrl);

            } catch (RuntimeException cleanupException) {

                /*
                 * 清理圖片失敗時不能把原本真正的建立錯誤蓋掉。
                 * 將清理錯誤掛到原例外上，方便除錯。
                 */
                originalException.addSuppressed(
                        cleanupException
                );
            }
        }
    }
}