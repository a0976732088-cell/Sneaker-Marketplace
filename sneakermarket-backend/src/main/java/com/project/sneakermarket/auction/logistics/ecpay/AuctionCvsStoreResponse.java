package com.project.sneakermarket.auction.logistics.ecpay;

/** 站內門市清單固定 DTO，僅含選店畫面需要的 canonical provider 資料。 */
public record AuctionCvsStoreResponse(
        AuctionCvsStoreType storeType,
        String storeId,
        String storeName,
        String storeAddress,
        String storePhone) {
}
