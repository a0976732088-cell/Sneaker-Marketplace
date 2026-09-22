package com.project.sneakermarket.auction.payment.linepay;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class AuctionLinePaySettingsTests {
    @Test
    void providerCredentialsAndApiBaseUrlAreSufficientWithoutPublicUrl() {
        AuctionLinePaySettings settings = new AuctionLinePaySettings(
                " channel-id ",
                " channel-secret ",
                "https://sandbox-api-pay.line.me/");

        assertDoesNotThrow(settings::requireProviderConfigured);
        assertEquals("https://sandbox-api-pay.line.me/v4/payments/request",
                settings.apiUrl("/v4/payments/request"));
        assertFalse(settings.toString().contains("channel-secret"));
    }
}
