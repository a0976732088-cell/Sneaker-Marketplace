package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionProduct;
import com.project.sneakermarket.auction.entity.AuctionStatus;
import com.project.sneakermarket.auction.exception.AuctionConflictException;
import com.project.sneakermarket.auction.notification.AuctionPrivateNotificationEventPublisher;
import com.project.sneakermarket.auction.repository.AuctionBidRepository;
import com.project.sneakermarket.auction.repository.AuctionProductRepository;
import com.project.sneakermarket.auction.repository.AuctionRepository;
import com.project.sneakermarket.auction.realtime.AuctionRealtimeEventPublisher;
import com.project.sneakermarket.auction.realtime.AuctionRealtimeEventType;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;

class AuctionAdminMutationServiceTests {
    private static final Long AUCTION_ID = 12L;
    private static final Long PRODUCT_ID = 21L;
    private static final Long ADMIN_ID = 7L;
    private static final Long SELLER_ID = 101L;
    private static final Long BIDDER_A_ID = 201L;
    private static final Long BIDDER_B_ID = 202L;
    private static final Long BIDDER_C_ID = 203L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 12, 20, 0);

    private AuctionRepository auctions;
    private AuctionProductRepository products;
    private AuctionBidRepository bids;
    private AuctionRealtimeEventPublisher realtimeEvents;
    private AuctionPrivateNotificationEventPublisher privateNotificationEvents;
    private AuctionAdminMutationService service;

    @BeforeEach
    void setup() {
        auctions = mock(AuctionRepository.class);
        products = mock(AuctionProductRepository.class);
        bids = mock(AuctionBidRepository.class);
        realtimeEvents = mock(AuctionRealtimeEventPublisher.class);
        privateNotificationEvents = mock(AuctionPrivateNotificationEventPublisher.class);
        AuctionTime time = new AuctionTime(Clock.fixed(Instant.parse("2026-09-12T12:00:00Z"), ZoneOffset.UTC));
        service = new AuctionAdminMutationService(
                auctions,
                products,
                bids,
                time,
                realtimeEvents,
                privateNotificationEvents
        );

        AuctionProduct product = new AuctionProduct();
        product.setAuctionProductId(PRODUCT_ID);
        product.setSellerId(SELLER_ID);
        when(products.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        // Repository 應 distinct；service 仍防禦性去重並排除 seller。
        when(bids.findDistinctBidderIds(AUCTION_ID)).thenReturn(List.of(
                SELLER_ID,
                BIDDER_A_ID,
                BIDDER_A_ID,
                BIDDER_B_ID,
                BIDDER_C_ID
        ));
    }

    @ParameterizedTest
    @EnumSource(AuctionStatus.class)
    void adminRemoveOnlyWritesMetadataForEveryAuctionStatus(AuctionStatus status) {
        Auction auction = auction(status);
        when(auctions.findByIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(auction));

        service.removeAuction(AUCTION_ID, ADMIN_ID, "  違反平台規範  ");

        assertEquals(status, auction.getStatus());
        assertEquals(NOW, auction.getRemovedAt());
        assertEquals(ADMIN_ID, auction.getRemovedByMemberId());
        assertEquals("違反平台規範", auction.getRemoveReason());
        assertEquals(status, auction.getRemovedFromStatus());
        assertEquals(NOW, auction.getUpdatedAt());
        verify(auctions).findByIdForUpdate(AUCTION_ID);
        verify(auctions).save(auction);
        verify(realtimeEvents).publish(AuctionRealtimeEventType.AUCTION_REMOVED, auction);
        verify(privateNotificationEvents).publishRemoved(
                AUCTION_ID,
                SELLER_ID,
                List.of(BIDDER_A_ID, BIDDER_B_ID, BIDDER_C_ID),
                "違反平台規範",
                NOW
        );
        verify(auctions, never()).delete(any(Auction.class));
        verify(auctions, never()).deleteById(anyLong());
    }

    @ParameterizedTest
    @EnumSource(value = AuctionStatus.class, names = {"SCHEDULED", "ACTIVE"})
    void removeDoesNotChangeBidLedger(AuctionStatus status) {
        Auction auction = auction(status);
        auction.setCurrentPrice(new BigDecimal("6500.00"));
        auction.setHighestBidderId(88L);
        auction.setBidCount(3);
        when(auctions.findByIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(auction));

        service.removeAuction(AUCTION_ID, ADMIN_ID, "疑似違規");

        assertEquals(status, auction.getStatus());
        assertEquals(0, new BigDecimal("6500.00").compareTo(auction.getCurrentPrice()));
        assertEquals(88L, auction.getHighestBidderId());
        assertEquals(3, auction.getBidCount());
    }

    @Test
    void repeatedRemoveIsIdempotentAndKeepsOriginalMetadata() {
        Auction auction = auction(AuctionStatus.ACTIVE);
        markRemoved(auction);
        when(auctions.findByIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(auction));

        service.removeAuction(AUCTION_ID, ADMIN_ID, "新的原因");

        assertEquals(6L, auction.getRemovedByMemberId());
        assertEquals("原始原因", auction.getRemoveReason());
        verify(auctions, never()).save(any());
        verifyNoInteractions(realtimeEvents);
        verifyNoInteractions(privateNotificationEvents);
        verifyNoInteractions(products, bids);
    }

    @Test
    void restoreActiveBeforeEndKeepsActiveAndClearsRemovalMetadata() {
        Auction auction = removedAuction(AuctionStatus.ACTIVE);
        auction.setEndAt(NOW.plusMinutes(1));
        when(auctions.findByIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(auction));

        service.restoreAuction(AUCTION_ID, ADMIN_ID);

        assertEquals(AuctionStatus.ACTIVE, auction.getStatus());
        assertRemovalMetadataCleared(auction);
        assertEquals(NOW, auction.getUpdatedAt());
        verify(auctions).save(auction);
        verify(privateNotificationEvents).publishRestored(
                AUCTION_ID,
                SELLER_ID,
                List.of(BIDDER_A_ID, BIDDER_B_ID, BIDDER_C_ID),
                "原始原因",
                NOW
        );
    }

    @Test
    void restoreScheduledBeforeStartKeepsScheduled() {
        Auction auction = removedAuction(AuctionStatus.SCHEDULED);
        auction.setStartAt(NOW.plusMinutes(1));
        auction.setEndAt(NOW.plusMinutes(2));
        when(auctions.findByIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(auction));

        service.restoreAuction(AUCTION_ID, ADMIN_ID);

        assertEquals(AuctionStatus.SCHEDULED, auction.getStatus());
        assertRemovalMetadataCleared(auction);
    }

    @Test
    void restoreScheduledAfterStartAndBeforeEndActivatesImmediately() {
        Auction auction = removedAuction(AuctionStatus.SCHEDULED);
        auction.setStartAt(NOW.minusMinutes(1));
        auction.setEndAt(NOW.plusMinutes(1));
        when(auctions.findByIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(auction));

        service.restoreAuction(AUCTION_ID, ADMIN_ID);

        assertEquals(AuctionStatus.ACTIVE, auction.getStatus());
        assertRemovalMetadataCleared(auction);
    }

    @Test
    void restoreLegacyBlankReasonPublishesSafeNullReason() {
        Auction auction = removedAuction(AuctionStatus.ACTIVE);
        auction.setRemoveReason("   ");
        when(auctions.findByIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(auction));

        service.restoreAuction(AUCTION_ID, ADMIN_ID);

        verify(privateNotificationEvents).publishRestored(
                AUCTION_ID,
                SELLER_ID,
                List.of(BIDDER_A_ID, BIDDER_B_ID, BIDDER_C_ID),
                null,
                NOW
        );
    }

    @ParameterizedTest
    @EnumSource(value = AuctionStatus.class, names = {"SCHEDULED", "ACTIVE"})
    void restoreExpiredAuctionIsRejectedAndKeepsRemovalMetadata(AuctionStatus status) {
        Auction auction = removedAuction(status);
        auction.setStartAt(NOW.minusHours(2));
        auction.setEndAt(NOW);
        when(auctions.findByIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(auction));

        AuctionConflictException error = assertThrows(AuctionConflictException.class,
                () -> service.restoreAuction(AUCTION_ID, ADMIN_ID));

        assertEquals("競標下架期間已超過原截止時間，無法直接重新上架", error.getMessage());
        assertEquals(status, auction.getStatus());
        assertNotNull(auction.getRemovedAt());
        verify(auctions, never()).save(any());
    }

    @Test
    void restoreRemovedDraftClearsVisibilityMetadataAndKeepsDraft() {
        Auction auction = removedAuction(AuctionStatus.DRAFT);
        when(auctions.findByIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(auction));

        service.restoreAuction(AUCTION_ID, ADMIN_ID);

        assertEquals(AuctionStatus.DRAFT, auction.getStatus());
        assertRemovalMetadataCleared(auction);
        verify(auctions).save(auction);
    }

    @ParameterizedTest
    @EnumSource(value = AuctionStatus.class, names = {"SOLD", "UNSOLD", "CANCELLED"})
    void restoreRemovedTerminalAuctionIsRejected(AuctionStatus status) {
        Auction auction = removedAuction(status);
        when(auctions.findByIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(auction));

        AuctionConflictException error = assertThrows(AuctionConflictException.class,
                () -> service.restoreAuction(AUCTION_ID, ADMIN_ID));

        assertEquals("已結標或終止的競標無法重新上架", error.getMessage());
        assertEquals(status, auction.getStatus());
        assertNotNull(auction.getRemovedAt());
        verify(auctions, never()).save(any());
    }

    @Test
    void restoreDeletedAuctionIsRejected() {
        Auction auction = removedAuction(AuctionStatus.SOLD);
        auction.setDeletedAt(NOW.minusMinutes(1));
        when(auctions.findByIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(auction));

        AuctionConflictException error = assertThrows(AuctionConflictException.class,
                () -> service.restoreAuction(AUCTION_ID, ADMIN_ID));

        assertEquals("已刪除的競標無法重新上架", error.getMessage());
        verify(auctions, never()).save(any());
    }

    @Test
    void repeatedRestoreOfVisibleAuctionIsIdempotent() {
        Auction auction = auction(AuctionStatus.ACTIVE);
        when(auctions.findByIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(auction));

        assertDoesNotThrow(() -> service.restoreAuction(AUCTION_ID, ADMIN_ID));

        verify(auctions, never()).save(any());
        verifyNoInteractions(privateNotificationEvents);
        verifyNoInteractions(products, bids);
    }

    @Test
    void removeRestoreRemovePublishesEachRealTransitionButNotRepeatedCalls() {
        Auction auction = auction(AuctionStatus.ACTIVE);
        when(auctions.findByIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(auction));

        service.removeAuction(AUCTION_ID, ADMIN_ID, "第一次下架");
        service.removeAuction(AUCTION_ID, ADMIN_ID, "重複下架");
        service.restoreAuction(AUCTION_ID, ADMIN_ID);
        service.restoreAuction(AUCTION_ID, ADMIN_ID);
        service.removeAuction(AUCTION_ID, ADMIN_ID, "第二次下架");

        List<Long> participants = List.of(BIDDER_A_ID, BIDDER_B_ID, BIDDER_C_ID);
        verify(privateNotificationEvents).publishRemoved(
                AUCTION_ID, SELLER_ID, participants, "第一次下架", NOW);
        verify(privateNotificationEvents).publishRestored(
                AUCTION_ID, SELLER_ID, participants, "第一次下架", NOW);
        verify(privateNotificationEvents).publishRemoved(
                AUCTION_ID, SELLER_ID, participants, "第二次下架", NOW);
        verifyNoMoreInteractions(privateNotificationEvents);
        verify(products, times(3)).findById(PRODUCT_ID);
        verify(bids, times(3)).findDistinctBidderIds(AUCTION_ID);
        verify(realtimeEvents, times(2)).publish(AuctionRealtimeEventType.AUCTION_REMOVED, auction);
    }

    @Test
    void failedMutationDoesNotPublishPrivateEvent() {
        Auction auction = auction(AuctionStatus.ACTIVE);
        when(auctions.findByIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(auction));
        when(auctions.save(auction)).thenThrow(new IllegalStateException("persistence failed"));

        assertThrows(IllegalStateException.class,
                () -> service.removeAuction(AUCTION_ID, ADMIN_ID, "下架原因"));

        verifyNoInteractions(realtimeEvents, privateNotificationEvents);
    }

    @Test
    void legacyRemovedStatusIsNeverRevived() {
        Auction auction = removedAuction(AuctionStatus.REMOVED);
        when(auctions.findByIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(auction));

        AuctionConflictException error = assertThrows(AuctionConflictException.class,
                () -> service.restoreAuction(AUCTION_ID, ADMIN_ID));

        assertEquals("廢標競標無法重新上架", error.getMessage());
        assertNotNull(auction.getRemovedAt());
        verify(auctions, never()).save(any());
    }

    @Test
    void deleteRequiresRemoveMetadata() {
        Auction auction = auction(AuctionStatus.SOLD);
        when(auctions.findByIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(auction));

        AuctionConflictException error = assertThrows(AuctionConflictException.class,
                () -> service.deleteAuction(AUCTION_ID, ADMIN_ID, "刪除原因"));

        assertEquals("競標必須先下架才能刪除", error.getMessage());
        assertNull(auction.getDeletedAt());
        verify(auctions, never()).save(any());
    }

    @Test
    void deleteOnlyWritesSoftDeleteMetadata() {
        Auction auction = removedAuction(AuctionStatus.SOLD);
        when(auctions.findByIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(auction));

        service.deleteAuction(AUCTION_ID, ADMIN_ID, "  重複違規  ");

        assertEquals(AuctionStatus.SOLD, auction.getStatus());
        assertEquals(NOW, auction.getDeletedAt());
        assertEquals(ADMIN_ID, auction.getDeletedByMemberId());
        assertEquals("重複違規", auction.getDeleteReason());
        verify(auctions).save(auction);
        verify(auctions, never()).delete(any(Auction.class));
        verify(auctions, never()).deleteById(anyLong());
    }

    @Test
    void repeatedDeleteIsIdempotent() {
        Auction auction = removedAuction(AuctionStatus.CANCELLED);
        auction.setDeletedAt(NOW.minusHours(1));
        auction.setDeletedByMemberId(6L);
        auction.setDeleteReason("第一次原因");
        when(auctions.findByIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(auction));

        service.deleteAuction(AUCTION_ID, ADMIN_ID, "第二次原因");

        assertEquals(6L, auction.getDeletedByMemberId());
        assertEquals("第一次原因", auction.getDeleteReason());
        verify(auctions, never()).save(any());
    }

    @Test
    void missingAuctionReturnsNotFoundAfterLockLookup() {
        when(auctions.findByIdForUpdate(AUCTION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.removeAuction(AUCTION_ID, ADMIN_ID, "原因"));
        assertThrows(ResourceNotFoundException.class,
                () -> service.restoreAuction(AUCTION_ID, ADMIN_ID));
        assertThrows(ResourceNotFoundException.class,
                () -> service.deleteAuction(AUCTION_ID, ADMIN_ID, "原因"));
        verify(auctions, times(3)).findByIdForUpdate(AUCTION_ID);
        verifyNoInteractions(realtimeEvents);
    }

    private Auction removedAuction(AuctionStatus status) {
        Auction auction = auction(status);
        markRemoved(auction);
        return auction;
    }

    private void markRemoved(Auction auction) {
        auction.setRemovedAt(NOW.minusHours(1));
        auction.setRemovedByMemberId(6L);
        auction.setRemoveReason("原始原因");
        auction.setRemovedFromStatus(auction.getStatus());
    }

    private void assertRemovalMetadataCleared(Auction auction) {
        assertNull(auction.getRemovedAt());
        assertNull(auction.getRemovedByMemberId());
        assertNull(auction.getRemoveReason());
        assertNull(auction.getRemovedFromStatus());
    }

    private Auction auction(AuctionStatus status) {
        Auction auction = new Auction();
        auction.setAuctionId(AUCTION_ID);
        auction.setAuctionProductId(PRODUCT_ID);
        auction.setStatus(status);
        auction.setCurrentPrice(new BigDecimal("5000.00"));
        auction.setHighestBidderId(null);
        auction.setBidCount(0);
        auction.setStartAt(NOW.minusHours(1));
        auction.setEndAt(NOW.plusHours(1));
        auction.setUpdatedAt(NOW.minusDays(1));
        return auction;
    }
}
