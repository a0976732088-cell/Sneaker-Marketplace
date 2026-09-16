package com.project.sneakermarket.auction.realtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionStatus;

class AuctionRealtimeEventPublisherTests {

    @Test
    void publishesImmutablePublicStatusSnapshotFromUpdatedAuction() {
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        AuctionRealtimeEventPublisher publisher = new AuctionRealtimeEventPublisher(events);
        Auction auction = new Auction();
        LocalDateTime endAt = LocalDateTime.of(2026, 9, 16, 15, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 9, 16, 12, 30);
        auction.setAuctionId(123L);
        auction.setCurrentPrice(new BigDecimal("6500.00"));
        auction.setMinIncrement(new BigDecimal("500.00"));
        auction.setBidCount(4);
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setEndAt(endAt);
        auction.setUpdatedAt(updatedAt);
        auction.setHighestBidderId(999L);
        auction.setRemovedByMemberId(888L);

        publisher.publish(AuctionRealtimeEventType.BID_PLACED, auction);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(eventCaptor.capture());
        AuctionRealtimeEvent event = (AuctionRealtimeEvent) eventCaptor.getValue();
        assertEquals(AuctionRealtimeEventType.BID_PLACED, event.message().eventType());
        assertEquals(123L, event.message().data().auctionId());
        assertEquals(new BigDecimal("6500.00"), event.message().data().currentPrice());
        assertEquals(new BigDecimal("7000.00"), event.message().data().nextMinimumBid());
        assertEquals(4, event.message().data().bidCount());
        assertEquals(AuctionStatus.ACTIVE, event.message().data().status());
        assertEquals(endAt, event.message().data().endAt());
        assertEquals(updatedAt, event.message().data().updatedAt());

        auction.setCurrentPrice(new BigDecimal("9000.00"));
        auction.setBidCount(9);
        assertEquals(new BigDecimal("6500.00"), event.message().data().currentPrice());
        assertEquals(4, event.message().data().bidCount());
    }
}
