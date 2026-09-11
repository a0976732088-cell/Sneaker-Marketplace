package com.project.sneakermarket.auction.service;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;
/** 競標 DATETIME2 / LocalDateTime 契約為台北時間，不改動其他模組時區。 */
@Component
public class AuctionTime {
    private static final ZoneId ZONE = ZoneId.of("Asia/Taipei");
    private final Clock clock;
    public AuctionTime() { this(Clock.system(ZONE)); }
    public AuctionTime(Clock clock) { this.clock = clock.withZone(ZONE); }
    public LocalDateTime now() { return LocalDateTime.now(clock); }
}
