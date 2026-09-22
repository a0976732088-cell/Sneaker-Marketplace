package com.project.sneakermarket.auction.logistics.ecpay;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** ECPay GetStoreList provider response；不直接暴露給前端。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EcpayStoreListProviderResponse(
        @JsonProperty("RtnCode") int rtnCode,
        @JsonProperty("RtnMsg") String rtnMsg,
        @JsonProperty("StoreList") List<StoreGroup> storeList) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StoreGroup(
            @JsonProperty("CvsType") String cvsType,
            @JsonProperty("StoreInfo") List<StoreInfo> storeInfo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StoreInfo(
            @JsonProperty("StoreId") String storeId,
            @JsonProperty("StoreName") String storeName,
            @JsonProperty("StoreAddr") String storeAddress,
            @JsonProperty("StorePhone") String storePhone) {
    }
}
