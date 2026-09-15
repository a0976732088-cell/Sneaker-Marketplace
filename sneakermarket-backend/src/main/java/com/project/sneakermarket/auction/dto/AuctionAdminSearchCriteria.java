package com.project.sneakermarket.auction.dto;

import com.project.sneakermarket.auction.entity.AuctionOrderStatus;
import com.project.sneakermarket.auction.entity.AuctionStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuctionAdminSearchCriteria {
    private String keyword;
    private AuctionStatus status;
    private AuctionOrderStatus orderStatus;
    private AuctionAdminManagementState managementState;
}
