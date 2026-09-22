package com.project.sneakermarket.auction.logistics.ecpay;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.project.sneakermarket.auction.exception.AuctionIntegrationException;

/**
 * ECPay GetStoreList 設定。
 *
 * <p>預設值是 ECPay 官方公開的 Stage C2C 測試資料，只能用於開發與測試；
 * 正式環境必須以環境變數覆寫成商家自己的正式 MerchantID、HashKey、HashIV 與 URL。</p>
 */
@Component
public record AuctionEcpayStoreListSettings(
        String storeListUrl,
        String merchantId,
        String hashKey,
        String hashIv) {

    public static final String STAGE_STORE_LIST_URL =
            "https://logistics-stage.ecpay.com.tw/Helper/GetStoreList";
    public static final String STAGE_C2C_MERCHANT_ID = "2000933";
    public static final String STAGE_C2C_HASH_KEY = "XBERn1YOvpM9nfZc";
    public static final String STAGE_C2C_HASH_IV = "h1ONHk4P4yqbl5LK";

    public AuctionEcpayStoreListSettings(
            @Value("${AUCTION_ECPAY_STORE_LIST_URL:https://logistics-stage.ecpay.com.tw/Helper/GetStoreList}")
                    String storeListUrl,
            @Value("${AUCTION_ECPAY_LOGISTICS_MERCHANT_ID:2000933}") String merchantId,
            @Value("${AUCTION_ECPAY_LOGISTICS_HASH_KEY:XBERn1YOvpM9nfZc}") String hashKey,
            @Value("${AUCTION_ECPAY_LOGISTICS_HASH_IV:h1ONHk4P4yqbl5LK}") String hashIv) {
        this.storeListUrl = trim(storeListUrl);
        this.merchantId = trim(merchantId);
        this.hashKey = trim(hashKey);
        this.hashIv = trim(hashIv);
    }

    public void requireConfigured() {
        if (storeListUrl.isBlank() || merchantId.isBlank() || hashKey.isBlank() || hashIv.isBlank()) {
            throw AuctionIntegrationException.unavailable("競標超商門市清單設定未完成");
        }
        try {
            URI uri = URI.create(storeListUrl);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw AuctionIntegrationException.unavailable("競標超商門市清單網址設定不正確");
        }
    }

    /** 避免設定物件被記錄時洩漏 HashKey / HashIV。 */
    @Override
    public String toString() {
        return "AuctionEcpayStoreListSettings[storeListUrl=" + storeListUrl
                + ", merchantId=" + merchantId + ", hashKey=***, hashIv=***]";
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
