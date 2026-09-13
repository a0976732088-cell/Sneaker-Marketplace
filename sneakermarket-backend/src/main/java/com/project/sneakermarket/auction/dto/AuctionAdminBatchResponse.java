package com.project.sneakermarket.auction.dto;

import java.util.List;

public record AuctionAdminBatchResponse(
        int totalCount,
        int successCount,
        int failureCount,
        List<AuctionAdminBatchItemResponse> items
) {
}
