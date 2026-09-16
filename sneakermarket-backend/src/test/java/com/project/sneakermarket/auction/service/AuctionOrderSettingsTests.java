package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AuctionOrderSettingsTests {
    @Test
    void positivePaymentTimeoutIsAccepted() {
        assertEquals(24, new AuctionOrderSettings(24).paymentTimeoutHours());
    }

    @Test
    void zeroOrNegativePaymentTimeoutIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AuctionOrderSettings(0));
        assertThrows(IllegalArgumentException.class, () -> new AuctionOrderSettings(-1));
    }
}
