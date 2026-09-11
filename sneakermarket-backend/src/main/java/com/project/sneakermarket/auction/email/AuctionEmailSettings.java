package com.project.sneakermarket.auction.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record AuctionEmailSettings(boolean enabled, int maxRetries, String from, String frontendBaseUrl) {
    public AuctionEmailSettings(
            @Value("${auction.email.enabled:false}") boolean enabled,
            @Value("${auction.email.max-retries:3}") int maxRetries,
            @Value("${auction.email.from}") String from,
            @Value("${app.frontend-base-url}") String frontendBaseUrl) {
        if (maxRetries < 1) throw new IllegalArgumentException("auction.email.max-retries must be positive");
        this.enabled = enabled;
        this.maxRetries = maxRetries;
        this.from = from;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }
}
