package com.project.sneakermarket.auction.service;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
class AuctionTimeTests {
    @Test
    void utcHostStillUsesTaipeiBusinessTime() {
        var time = new AuctionTime(Clock.fixed(Instant.parse("2026-09-07T20:30:00Z"), ZoneOffset.UTC));
        assertEquals(LocalDateTime.of(2026, 9, 8, 4, 30), time.now());
    }
}
