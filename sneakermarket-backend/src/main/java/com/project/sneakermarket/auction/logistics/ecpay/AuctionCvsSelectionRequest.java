package com.project.sneakermarket.auction.logistics.ecpay;

/** 前端只能回傳店別與店號；名稱、地址、電話一律由官方清單決定。 */
public record AuctionCvsSelectionRequest(
        AuctionCvsStoreType storeType,
        String storeId) {
}
