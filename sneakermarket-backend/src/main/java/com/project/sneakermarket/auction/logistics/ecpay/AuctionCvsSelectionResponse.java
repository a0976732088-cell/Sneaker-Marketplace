package com.project.sneakermarket.auction.logistics.ecpay;

import java.time.LocalDateTime;

public record AuctionCvsSelectionResponse(
        boolean selected,
        AuctionCvsStoreType storeType,
        String logisticsSubType,
        String cvsStoreId,
        String cvsStoreName,
        String cvsAddress,
        String cvsTelephone,
        String cvsOutside,
        LocalDateTime selectedAt) {

    public static AuctionCvsSelectionResponse notSelected() {
        return new AuctionCvsSelectionResponse(false, null, null, null, null, null, null, null, null);
    }
}
