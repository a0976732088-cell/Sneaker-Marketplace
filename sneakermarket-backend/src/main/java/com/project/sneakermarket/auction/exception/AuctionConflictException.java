package com.project.sneakermarket.auction.exception;

/**
 * 競標目前狀態或時間與要求的操作衝突。
 */
public class AuctionConflictException extends RuntimeException {

    public AuctionConflictException(String message) {
        super(message);
    }
}
