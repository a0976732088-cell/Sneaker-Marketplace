package com.project.sneakermarket.auction.dto;

public record AuctionAdminBatchItemResponse(
        Long auctionId,
        boolean success,
        String message
) {
}
