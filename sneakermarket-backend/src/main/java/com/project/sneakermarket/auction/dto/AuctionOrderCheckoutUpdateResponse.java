package com.project.sneakermarket.auction.dto;

import java.time.LocalDateTime;

public record AuctionOrderCheckoutUpdateResponse(
        String orderNumber,
        String receiverName,
        String receiverPhone,
        String shippingAddress,
        LocalDateTime updatedAt) {
}
