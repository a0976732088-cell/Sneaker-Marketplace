package com.project.sneakermarket.auction.exception;

/** 競標操作需要有效的登入會員。 */
public class AuctionUnauthorizedException extends RuntimeException {
    public AuctionUnauthorizedException() {
        super("登入已失效，請重新登入會員");
    }
}
