package com.project.sneakermarket.auction.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.project.sneakermarket.common.exception.ForbiddenException;
import com.project.sneakermarket.auction.exception.AuctionConflictException;
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
import com.project.sneakermarket.auction.realtime.AuctionRealtimeEventPublisher;
import com.project.sneakermarket.auction.realtime.AuctionRealtimeEventType;
import com.project.sneakermarket.common.exception.BusinessException;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;
import com.project.sneakermarket.auction.dto.AuctionUpdateRequest;

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
    private final AuctionReferenceService referenceService;
    private final AuctionTime auctionTime;
    private final AuctionRealtimeEventPublisher realtimeEvents;

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
        referenceService.validateProductReferences(request.brandId(), request.categoryId());
        validateAuctionTime(request);
        validateImages(images);

        LocalDateTime now = auctionTime.now();

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
     * 修改指定競標。
     *
     * 規則：
     * 1. 只有商品賣家本人可以修改。
     * 2. 已有成功出價後不可修改。
     * 3. SCHEDULED 且無出價時，可修改所有一般可編輯欄位。
     * 4. ACTIVE 且無出價時，可修改商品、交易、價格與結束時間，
     *    但不可修改已經發生的開始時間。
     * 5. SOLD、UNSOLD、CANCELLED、REMOVED 不可修改。
     *
     * @param auctionId 競標 ID
     * @param sellerId 目前登入會員 ID
     * @param request 修改資料
     */
    @Transactional
    public void updateAuction(
            Long auctionId,
            Long sellerId,
            AuctionUpdateRequest request) {

        validateSellerId(sellerId);

        /*
         * 編輯與未來的出價功能都鎖定同一筆 Auction。
         *
         * 取得鎖後才重新判斷 bidCount，
         * 避免賣家修改價格時剛好有人成功出價。
         */
        Auction auction =
                auctionRepository
                        .findByIdForUpdate(auctionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "找不到指定的競標資料"
                                )
                        );

        AuctionProduct product =
                auctionProductRepository
                        .findById(
                                auction.getAuctionProductId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "找不到指定的競標商品資料"
                                )
                        );

        if (auction.getRemovedAt() != null || auction.getDeletedAt() != null) {
            throw new AuctionConflictException("競標已下架，無法修改");
        }
        // 只有刊登者本人可以修改
        if (!product.getSellerId().equals(sellerId)) {
            throw new ForbiddenException(
                    "你無權修改此競標"
            );
        }

        /*
         * 必須在取得 Lock 後才檢查。
         * 如果剛剛有人先完成出價，
         * 這裡就會讀到最新的 bidCount。
         */
        if (auction.getBidCount() != null
                && auction.getBidCount() > 0) {

            throw new BusinessException(
                    "此競標已有出價紀錄，無法修改"
            );
        }

        LocalDateTime now = auctionTime.now();
        if (!auction.getEndAt().isAfter(now)) {
            throw new BusinessException("競標已到截止時間，無法修改");
        }
        AuctionStatus effectiveStatus = auction.getStatus();
        // 排程尚未更新狀態時，開始時間仍是本次操作規則的最終依據。
        if (effectiveStatus == AuctionStatus.SCHEDULED && !auction.getStartAt().isAfter(now)) {
            effectiveStatus = AuctionStatus.ACTIVE;
        }

        /*
         * 已經結束、取消或下架的競標
         * 不允許再修改。
         */
        if (effectiveStatus == AuctionStatus.SOLD
                || effectiveStatus == AuctionStatus.UNSOLD
                || effectiveStatus == AuctionStatus.CANCELLED
                || effectiveStatus == AuctionStatus.REMOVED) {

            throw new BusinessException(
                    "目前競標狀態不允許修改"
            );
        }

        /*
         * 目前功能 4 正式支援的編輯狀態
         * 只有 SCHEDULED 與 ACTIVE。
         */
        if (effectiveStatus != AuctionStatus.SCHEDULED
                && effectiveStatus != AuctionStatus.ACTIVE) {

            throw new BusinessException(
                    "目前競標狀態不允許修改"
            );
        }

        /*
         * ACTIVE 代表競標已經開始，
         * startAt 已經成為歷史事實，因此不可修改。
         */
        if (effectiveStatus == AuctionStatus.ACTIVE
                && !auction.getStartAt()
                .equals(request.startAt())) {

            throw new BusinessException(
                    "競標開始後不可修改開始時間"
            );
        }

        /*
         * SCHEDULED 時使用新的 startAt。
         * ACTIVE 時則一定沿用原本 startAt。
         */
        LocalDateTime effectiveStartAt =
                effectiveStatus == AuctionStatus.SCHEDULED
                        ? request.startAt()
                        : auction.getStartAt();

        // 結束時間一定必須晚於開始時間
        if (!request.endAt()
                .isAfter(effectiveStartAt)) {

            throw new BusinessException(
                    "競標結束時間必須晚於開始時間"
            );
        }

        // 修改後不能產生已經過期的競標
        if (!request.endAt()
                .isAfter(now)) {

            throw new BusinessException(
                    "競標結束時間必須晚於目前時間"
            );
        }

        /*
         * 修改商品資料。
         *
         * SCHEDULED 尚未開始時：
         * brand/category 也可以修改。
         *
         * ACTIVE 後：
         * 依目前定案規則保留原本 brand/category，
         * 其他商品資訊仍可修改。
         */
        referenceService.validateProductReferences(
                effectiveStatus == AuctionStatus.SCHEDULED ? request.brandId() : product.getBrandId(),
                effectiveStatus == AuctionStatus.SCHEDULED ? request.categoryId() : product.getCategoryId());
        if (effectiveStatus == AuctionStatus.SCHEDULED) {
            product.setBrandId(
                    request.brandId()
            );
            product.setCategoryId(
                    request.categoryId()
            );
        }

        product.setProductName(
                request.productName()
        );

        product.setModel(
                request.model()
        );

        product.setSize(
                request.size()
        );

        product.setCondition(
                request.condition()
        );

        product.setDescription(
                request.description()
        );

        product.setDefectNote(
                request.defectNote()
        );

        product.setAuthenticityNote(
                request.authenticityNote()
        );

        product.setLocation(
                request.location()
        );

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

        product.setUpdatedAt(now);

        /*
         * bidCount 已確認為 0，
         * 因此目前 currentPrice 必然屬於尚未成交的起始價格。
         *
         * 修改 startPrice 時，
         * currentPrice 必須一起同步。
         */
        auction.setStartPrice(
                request.startPrice()
        );

        auction.setCurrentPrice(
                request.startPrice()
        );

        auction.setMinIncrement(
                request.minIncrement()
        );

        /*
         * SCHEDULED 尚未開始，
         * 因此允許改 startAt。
         *
         * ACTIVE 則完全保留原本值。
         */
        if (effectiveStatus == AuctionStatus.SCHEDULED) {
            auction.setStartAt(
                    request.startAt()
            );
        }

        auction.setEndAt(
                request.endAt()
        );

        /*
         * effectiveStatus 只供本次編輯的權限與欄位規則判斷。
         * PUT 不負責持久化 SCHEDULED → ACTIVE；
         * 正式生命週期轉換留給後續 Scheduler。
         */

        auction.setUpdatedAt(now);

        /*
         * 目前使用 managed Entity，
         * Transaction Commit 時 Dirty Checking
         * 本身就能寫回 DB。
         *
         * 這裡仍明確 save，
         * 延續目前專案較容易閱讀的寫入方式。
         */
        auctionProductRepository.save(product);
        auctionRepository.save(auction);
    }

    /**
     * 取消一場競標。
     *
     * 規則：
     * 1. 競標必須存在。
     * 2. 只有刊登此商品的賣家本人可以取消。
     * 3. 只要尚未有人成功出價，賣家即可取消。
     * 4. 已有任何成功出價後，不允許賣家自行取消。
     *
     * 取消採邏輯取消，只將狀態改為 CANCELLED，
     * 不直接刪除 auctions、auction_products 或圖片資料。
     *
     * @param auctionId 要取消的競標 ID
     * @param sellerId  目前登入會員 ID
     */
    @Transactional
    public void cancelAuction(
            Long auctionId,
            Long sellerId) {

        validateSellerId(sellerId);

        /*
         * 使用 PESSIMISTIC_WRITE 鎖住這場競標。
         *
         * 避免賣家讀到 bidCount=0 的同時，
         * 另一個會員剛好完成出價，
         * 最後卻又被賣家取消。
         */
        Auction auction =
                auctionRepository
                        .findByIdForUpdate(auctionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "找不到指定的競標資料"
                                )
                        );

        /*
         * Auction 本身目前只有 auctionProductId，
         * sellerId 存在 AuctionProduct，
         * 因此需要取得對應商品確認擁有者。
         */
        AuctionProduct product =
                auctionProductRepository
                        .findById(
                                auction.getAuctionProductId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "找不到指定的競標商品資料"
                                )
                        );

        if (auction.getRemovedAt() != null || auction.getDeletedAt() != null) {
            throw new AuctionConflictException("競標已下架，無法取消");
        }
        // 只有刊登商品的賣家本人可以取消
        if (!product.getSellerId().equals(sellerId)) {
            throw new ForbiddenException(
                    "你無權取消此競標"
            );
        }

        /*
         * 核心規則：
         * 只要已經存在成功出價，
         * 賣家便不能再自行取消競標。
         */
        if (auction.getBidCount() != null
                && auction.getBidCount() > 0) {

            throw new BusinessException(
                    "此競標已有出價紀錄，無法自行取消"
            );
        }

        /*
         * 已經進入終止狀態的競標，
         * 不允許再由賣家改成 CANCELLED。
         *
         * UNSOLD 即使 bidCount = 0，
         * 也代表競標已正式結束並流標，
         * 不應事後改寫歷史狀態。
         */
        if (auction.getStatus() == AuctionStatus.SOLD) {
            throw new BusinessException(
                    "此競標已成交，無法取消"
            );
        }

        if (auction.getStatus() == AuctionStatus.UNSOLD) {
            throw new BusinessException(
                    "此競標已流標，無法取消"
            );
        }

        if (auction.getStatus() == AuctionStatus.CANCELLED) {
            throw new BusinessException(
                    "此競標已經取消"
            );
        }

        if (auction.getStatus() == AuctionStatus.REMOVED) {
            throw new BusinessException(
                    "此競標已廢標"
            );
        }

        if (auction.getStatus() != AuctionStatus.SCHEDULED
                && auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new BusinessException("目前競標狀態不允許取消");
        }
        if (!auction.getEndAt().isAfter(auctionTime.now())) {
            throw new BusinessException("競標已到截止時間，無法取消");
        }

        /*
         * 不實際 DELETE。
         *
         * 保留商品、競標與歷史資料，
         * 只將競標生命週期改為 CANCELLED。
         */
        auction.setStatus(
                AuctionStatus.CANCELLED
        );

        auction.setUpdatedAt(
                auctionTime.now()
        );

        /*
         * 因為目前方法有 @Transactional，
         * Auction 又是 Persistence Context 中的 managed Entity，
         * 理論上交易結束時 Dirty Checking 就會 UPDATE。
         *
         * 此處仍明確 save，讓目前初期專案的寫入流程比較容易理解。
         */
        auctionRepository.save(auction);
        realtimeEvents.publish(AuctionRealtimeEventType.AUCTION_CANCELLED, auction);
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
         * DRAFT 為 Reserved / 未啟用狀態，正常建立流程不產生。
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
        referenceService.requireActiveMember(sellerId);
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
                .isAfter(auctionTime.now())) {

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
