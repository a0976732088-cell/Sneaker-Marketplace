package com.project.sneakermarket.auction.realtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.project.sneakermarket.auction.dto.AuctionStatusResponse;
import com.project.sneakermarket.auction.entity.AuctionStatus;

class AuctionRealtimeEventListenerTests {

    @Test
    void sendsPublicSnapshotToAuctionSpecificTopic() {
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        AuctionRealtimeEventListener listener = new AuctionRealtimeEventListener(messaging);
        LocalDateTime endAt = LocalDateTime.of(2026, 9, 16, 15, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 9, 16, 12, 30);
        AuctionRealtimeMessage message = new AuctionRealtimeMessage(
                AuctionRealtimeEventType.BID_PLACED,
                new AuctionStatusResponse(
                        123L,
                        new BigDecimal("6500.00"),
                        4,
                        new BigDecimal("7000.00"),
                        AuctionStatus.ACTIVE,
                        endAt,
                        updatedAt
                )
        );

        listener.onAuctionRealtimeEvent(new AuctionRealtimeEvent(message));

        ArgumentCaptor<String> destination = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(messaging).convertAndSend(destination.capture(), payload.capture());
        assertEquals("/topic/auctions/123", destination.getValue());
        assertSame(message, payload.getValue());
        assertEquals(AuctionRealtimeEventType.BID_PLACED, message.eventType());
        assertEquals(123L, message.data().auctionId());
        assertEquals(new BigDecimal("6500.00"), message.data().currentPrice());
        assertEquals(4, message.data().bidCount());
        assertEquals(new BigDecimal("7000.00"), message.data().nextMinimumBid());
        assertEquals(AuctionStatus.ACTIVE, message.data().status());
        assertEquals(endAt, message.data().endAt());
        assertEquals(updatedAt, message.data().updatedAt());
    }

    @Test
    void publicMessageContractContainsNoPrivateMemberIdentityField() {
        Set<String> envelopeFields = recordFields(AuctionRealtimeMessage.class);
        Set<String> dataFields = recordFields(AuctionStatusResponse.class);

        assertEquals(Set.of("eventType", "data"), envelopeFields);
        assertEquals(Set.of(
                "auctionId", "currentPrice", "bidCount", "nextMinimumBid",
                "status", "endAt", "updatedAt"
        ), dataFields);
        assertFalse(dataFields.stream().anyMatch(name -> Set.of(
                "highestBidderId", "sellerId", "buyerId", "bidderId",
                "memberId", "email", "phone", "address"
        ).contains(name)));
    }

    private Set<String> recordFields(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(component -> component.getName())
                .collect(Collectors.toSet());
    }
}
