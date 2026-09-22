package com.project.sneakermarket.auction.logistics.ecpay;

public enum AuctionCvsStoreType {
    UNIMART("UNIMARTC2C", "7-ELEVEN"),
    FAMI("FAMIC2C", "全家"),
    HILIFE("HILIFEC2C", "萊爾富");

    private final String logisticsSubType;
    private final String displayName;

    AuctionCvsStoreType(String logisticsSubType, String displayName) {
        this.logisticsSubType = logisticsSubType;
        this.displayName = displayName;
    }

    public String logisticsSubType() { return logisticsSubType; }
    public String displayName() { return displayName; }
}
