package com.project.sneakermarket.auction.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 競標圖片儲存服務介面。
 *
 * 上層競標功能只依賴此介面，
 * 不直接關心圖片實際存放在本機、雲端或其他 Storage。
 */
public interface ImageStorageService {

    /**
     * 儲存一張競標商品圖片。
     *
     * @param file               使用者上傳的圖片
     * @param auctionProductId   所屬競標商品 ID
     * @return 可保存至 auction_product_images.image_url 的圖片路徑
     */
    String store(
            MultipartFile file,
            Long auctionProductId
    );

    /**
     * 刪除已儲存的圖片。
     *
     * 建立競標途中若資料庫交易失敗，
     * 可利用此方法清除已經寫入本機但尚未正式完成的圖片。
     *
     * @param imageUrl 資料庫保存的圖片路徑
     */
    void delete(String imageUrl);
}