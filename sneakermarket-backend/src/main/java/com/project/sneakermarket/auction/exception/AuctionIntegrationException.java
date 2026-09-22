package com.project.sneakermarket.auction.exception;

import org.springframework.http.HttpStatus;

/** 第三方設定或 provider 回應失敗；訊息不得包含 credential 或 signature。 */
public class AuctionIntegrationException extends RuntimeException {
    private final HttpStatus status;

    public AuctionIntegrationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() { return status; }

    public static AuctionIntegrationException unavailable(String message) {
        return new AuctionIntegrationException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    public static AuctionIntegrationException provider(String message) {
        return new AuctionIntegrationException(HttpStatus.BAD_GATEWAY, message);
    }
}
