package com.project.sneakermarket.auction.payment.linepay;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.project.sneakermarket.auction.exception.AuctionIntegrationException;

@Component
public record AuctionLinePaySettings(
        String channelId,
        String channelSecret,
        String apiBaseUrl) {

    public AuctionLinePaySettings(
            @Value("${AUCTION_LINE_PAY_CHANNEL_ID:}") String channelId,
            @Value("${AUCTION_LINE_PAY_CHANNEL_SECRET:}") String channelSecret,
            @Value("${AUCTION_LINE_PAY_API_BASE_URL:}") String apiBaseUrl) {
        this.channelId = trim(channelId);
        this.channelSecret = trim(channelSecret);
        this.apiBaseUrl = stripTrailingSlash(apiBaseUrl);
    }

    public void requireProviderConfigured() {
        if (channelId.isBlank() || channelSecret.isBlank() || apiBaseUrl.isBlank()) {
            throw AuctionIntegrationException.unavailable("競標 LINE Pay 設定未完成");
        }
        requireHttpUrl(apiBaseUrl, "競標 LINE Pay API 網址設定不正確");
    }

    public String apiUrl(String apiPath) {
        requireProviderConfigured();
        if (apiPath == null || !apiPath.startsWith("/")) {
            throw new IllegalArgumentException("LINE Pay apiPath must begin with /");
        }
        return apiBaseUrl + apiPath;
    }

    private static void requireHttpUrl(String value, String message) {
        try {
            URI uri = URI.create(value);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw AuctionIntegrationException.unavailable(message);
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String stripTrailingSlash(String value) {
        return trim(value).replaceAll("/+$", "");
    }

    /** 避免 record 預設 toString 將 Channel Secret 寫入 log 或錯誤報告。 */
    @Override
    public String toString() {
        return "AuctionLinePaySettings[channelId=" + channelId
                + ", channelSecret=***, apiBaseUrl=" + apiBaseUrl + "]";
    }
}
