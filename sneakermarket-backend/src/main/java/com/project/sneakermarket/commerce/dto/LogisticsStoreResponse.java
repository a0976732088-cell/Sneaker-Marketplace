package com.project.sneakermarket.commerce.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LogisticsStoreResponse {

    private String storeId;

    private String storeName;

    private String address;

    private String phone;

    private String type;
}