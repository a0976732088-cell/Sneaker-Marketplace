package com.project.sneakermarket.auction.exception;
import com.project.sneakermarket.common.response.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
/** 僅處理競標專用的認證與狀態衝突錯誤，保留其他模組行為。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.project.sneakermarket.auction.controller")
public class AuctionExceptionHandler {
    @ExceptionHandler(AuctionUnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> unauthorized(AuctionUnauthorizedException ex) {
        return ResponseEntity.status(401).body(ApiResponse.fail(ex.getMessage(), null));
    }

    @ExceptionHandler(AuctionConflictException.class)
    public ResponseEntity<ApiResponse<Void>> conflict(AuctionConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail(ex.getMessage(), null));
    }

    @ExceptionHandler(AuctionIntegrationException.class)
    public ResponseEntity<ApiResponse<Void>> integration(AuctionIntegrationException ex) {
        return ResponseEntity.status(ex.status())
                .body(ApiResponse.fail(ex.getMessage(), null));
    }
}
