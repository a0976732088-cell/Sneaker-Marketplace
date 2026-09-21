package com.project.sneakermarket.auction.payment.linepay;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LinePaySignatureServiceTests {
    @Test
    void signsExactV4PostMessageWithFixedVector() {
        String signature = new LinePaySignatureService().sign(
                "/v4/payments/request",
                "{\"amount\":100,\"currency\":\"TWD\"}",
                "nonce-123",
                "secret");

        assertEquals("I7bR/RNum/N6SU8rg+sLN+2W72ctjgfLgNvQNGS+3Ts=", signature);
    }

    @Test
    void signsExactV4GetMessageWithFixedVector() {
        String signature = new LinePaySignatureService().signGet(
                "/v4/payments/requests/998877665544/check",
                "",
                "nonce-456",
                "secret");

        assertEquals("+NyQrXGpGFeBs+KFl3JYI4Y6yCMwl3ebO4DbHgSwAeE=", signature);
    }
}
