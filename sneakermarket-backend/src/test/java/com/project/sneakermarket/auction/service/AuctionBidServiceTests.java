package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.project.sneakermarket.auction.dto.BidCreateRequest;
import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionBid;
import com.project.sneakermarket.auction.entity.AuctionProduct;
import com.project.sneakermarket.auction.entity.AuctionStatus;
import com.project.sneakermarket.auction.exception.AuctionConflictException;
import com.project.sneakermarket.auction.repository.AuctionBidRepository;
import com.project.sneakermarket.auction.repository.AuctionProductRepository;
import com.project.sneakermarket.auction.repository.AuctionRepository;
import com.project.sneakermarket.common.exception.BusinessException;
import com.project.sneakermarket.common.exception.ForbiddenException;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;

class AuctionBidServiceTests {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 8, 12, 0);
    private static final Long AUCTION_ID = 12L;
    private static final Long SELLER_ID = 42L;
    private static final Long BUYER_ID = 77L;

    private AuctionRepository auctions;
    private AuctionProductRepository products;
    private AuctionBidRepository bids;
    private AuctionBidService service;
    private Auction auction;

    @BeforeEach
    void setup() {
        auctions = mock(AuctionRepository.class);
        products = mock(AuctionProductRepository.class);
        bids = mock(AuctionBidRepository.class);

        AuctionTime time = new AuctionTime(
                Clock.fixed(Instant.parse("2026-09-08T04:00:00Z"), ZoneOffset.UTC)
        );
        service = new AuctionBidService(auctions, products, bids, time);

        auction = new Auction();
        auction.setAuctionId(AUCTION_ID);
        auction.setAuctionProductId(3L);
        auction.setStartPrice(new BigDecimal("3000.00"));
        auction.setCurrentPrice(new BigDecimal("3000.00"));
        auction.setMinIncrement(new BigDecimal("500.00"));
        auction.setStartAt(NOW.minusHours(1));
        auction.setEndAt(NOW.plusHours(1));
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setBidCount(0);
        auction.setUpdatedAt(NOW.minusMinutes(1));

        AuctionProduct product = new AuctionProduct();
        product.setAuctionProductId(3L);
        product.setSellerId(SELLER_ID);

        when(auctions.findByIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(auction));
        when(products.findById(3L)).thenReturn(Optional.of(product));
    }

    @Test
    void firstBidAtNextMinimumPersistsConsistentBidAndAuctionState() {
        var response = service.placeBid(
                AUCTION_ID,
                BUYER_ID,
                new BidCreateRequest(new BigDecimal("3500"))
        );

        ArgumentCaptor<AuctionBid> bidCaptor = ArgumentCaptor.forClass(AuctionBid.class);
        verify(bids).save(bidCaptor.capture());
        AuctionBid savedBid = bidCaptor.getValue();

        assertEquals(AUCTION_ID, savedBid.getAuctionId());
        assertEquals(BUYER_ID, savedBid.getBidderId());
        assertAmount("3500.00", savedBid.getBidAmount());
        assertEquals(NOW, savedBid.getCreatedAt());
        assertAmount("3500.00", auction.getCurrentPrice());
        assertEquals(BUYER_ID, auction.getHighestBidderId());
        assertEquals(1, auction.getBidCount());
        assertAmount("4000.00", response.nextMinimumBid());
        assertEquals(NOW, response.updatedAt());

        var readOrder = inOrder(auctions, products);
        readOrder.verify(auctions).findByIdForUpdate(AUCTION_ID);
        readOrder.verify(products).findById(3L);

        var order = inOrder(bids, auctions);
        order.verify(bids).save(savedBid);
        order.verify(auctions).save(auction);
    }

    @Test
    void firstBidWithZeroFractionIsAcceptedAsIntegerTwd() {
        var response = service.placeBid(
                AUCTION_ID,
                BUYER_ID,
                new BidCreateRequest(new BigDecimal("3500.00"))
        );

        assertAmount("3500.00", response.currentPrice());
        assertAmount("4000.00", response.nextMinimumBid());
        verify(bids).save(any(AuctionBid.class));
    }

    @Test
    void firstBidMayIncreaseByTwoIncrements() {
        var response = service.placeBid(
                AUCTION_ID,
                BUYER_ID,
                new BidCreateRequest(new BigDecimal("4000"))
        );

        assertAmount("4000.00", response.currentPrice());
        assertEquals(1, response.bidCount());
        assertAmount("4500.00", response.nextMinimumBid());
    }

    @Test
    void firstBidBelowCurrentPricePlusIncrementIsRejected() {
        for (String amount : new String[]{"3000", "3001", "3499", "3499.99"}) {
            assertThrows(
                    BusinessException.class,
                    () -> service.placeBid(
                            AUCTION_ID,
                            BUYER_ID,
                            new BidCreateRequest(new BigDecimal(amount))
                    )
            );
        }

        assertAmount("3000.00", auction.getCurrentPrice());
        assertEquals(0, auction.getBidCount());
        verifyNoWrites();
    }

    @Test
    void bidWithNonZeroFractionIsRejected() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.placeBid(
                        AUCTION_ID,
                        BUYER_ID,
                        new BidCreateRequest(new BigDecimal("3500.50"))
                )
        );

        assertTrue(exception.getMessage().contains("整數新台幣"));
        verifyNoWrites();
    }

    @Test
    void firstBidOutsideIncrementGridIsRejected() {
        for (String amount : new String[]{"3501", "3750"}) {
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> service.placeBid(
                            AUCTION_ID,
                            BUYER_ID,
                            new BidCreateRequest(new BigDecimal(amount))
                    )
            );
            assertTrue(exception.getMessage().contains("加價級距"));
        }

        verifyNoWrites();
    }

    @Test
    void secondBidAtCurrentPricePlusIncrementSucceeds() {
        auction.setCurrentPrice(new BigDecimal("3500.00"));
        auction.setBidCount(1);

        var response = service.placeBid(
                AUCTION_ID,
                BUYER_ID,
                new BidCreateRequest(new BigDecimal("4000.00"))
        );

        assertAmount("4000.00", response.currentPrice());
        assertEquals(2, response.bidCount());
        assertAmount("4500.00", response.nextMinimumBid());
        verify(bids).save(any(AuctionBid.class));
        verify(auctions).save(auction);
    }

    @Test
    void bidBelowLatestMinimumLeavesAuctionAndHistoryUnchanged() {
        auction.setCurrentPrice(new BigDecimal("3500.00"));
        auction.setHighestBidderId(66L);
        auction.setBidCount(1);
        LocalDateTime originalUpdatedAt = auction.getUpdatedAt();

        for (String amount : new String[]{"3999", "3999.99"}) {
            assertThrows(
                    BusinessException.class,
                    () -> service.placeBid(
                            AUCTION_ID,
                            BUYER_ID,
                            new BidCreateRequest(new BigDecimal(amount))
                    )
            );
        }

        assertAmount("3500.00", auction.getCurrentPrice());
        assertEquals(66L, auction.getHighestBidderId());
        assertEquals(1, auction.getBidCount());
        assertEquals(originalUpdatedAt, auction.getUpdatedAt());
        verify(bids, never()).save(any());
        verify(auctions, never()).save(any());
    }

    @Test
    void laterBidOutsideIncrementGridIsRejected() {
        auction.setCurrentPrice(new BigDecimal("3500.00"));
        auction.setBidCount(1);

        for (String amount : new String[]{"4001", "4250"}) {
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> service.placeBid(
                            AUCTION_ID,
                            BUYER_ID,
                            new BidCreateRequest(new BigDecimal(amount))
                    )
            );
            assertTrue(exception.getMessage().contains("加價級距"));
        }

        verifyNoWrites();
    }

    @Test
    void laterBidMayIncreaseByTwoIncrements() {
        auction.setCurrentPrice(new BigDecimal("3500.00"));
        auction.setBidCount(1);

        var response = service.placeBid(
                AUCTION_ID,
                BUYER_ID,
                new BidCreateRequest(new BigDecimal("4500"))
        );

        assertAmount("4500.00", response.currentPrice());
        assertAmount("5000.00", response.nextMinimumBid());
    }

    @Test
    void sellerCannotBidOnOwnAuction() {
        assertThrows(
                ForbiddenException.class,
                () -> service.placeBid(
                        AUCTION_ID,
                        SELLER_ID,
                        new BidCreateRequest(new BigDecimal("5000.00"))
                )
        );
        verifyNoWrites();
    }

    @Test
    void nonexistentAuctionReturnsNotFound() {
        when(auctions.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.placeBid(
                        99L,
                        BUYER_ID,
                        new BidCreateRequest(new BigDecimal("5000.00"))
                )
        );
        verifyNoWrites();
    }

    @Test
    void missingAuctionProductReturnsNotFoundWithoutWriting() {
        when(products.findById(3L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.placeBid(
                        AUCTION_ID,
                        BUYER_ID,
                        new BidCreateRequest(new BigDecimal("5000.00"))
                )
        );
        verifyNoWrites();
    }

    @Test
    void scheduledAndTerminalStatusesReturnConflict() {
        for (AuctionStatus status : new AuctionStatus[]{
                AuctionStatus.DRAFT,
                AuctionStatus.SCHEDULED,
                AuctionStatus.CANCELLED,
                AuctionStatus.SOLD,
                AuctionStatus.UNSOLD,
                AuctionStatus.REMOVED
        }) {
            auction.setStatus(status);
            RuntimeException exception = assertThrows(
                    AuctionConflictException.class,
                    () -> service.placeBid(
                            AUCTION_ID,
                            BUYER_ID,
                            new BidCreateRequest(new BigDecimal("5000.00"))
                    )
            );
            assertInstanceOf(AuctionConflictException.class, exception);
        }
        verifyNoWrites();
    }

    @Test
    void activeButExpiredAuctionReturnsConflictWithoutWriting() {
        auction.setEndAt(NOW);

        assertThrows(
                AuctionConflictException.class,
                () -> service.placeBid(
                        AUCTION_ID,
                        BUYER_ID,
                        new BidCreateRequest(new BigDecimal("5000.00"))
                )
        );
        verifyNoWrites();
    }

    @Test
    void activeButNotStartedAuctionReturnsConflictWithoutWriting() {
        auction.setStartAt(NOW.plusMinutes(1));

        assertThrows(
                AuctionConflictException.class,
                () -> service.placeBid(
                        AUCTION_ID,
                        BUYER_ID,
                        new BidCreateRequest(new BigDecimal("5000.00"))
                )
        );
        verifyNoWrites();
    }

    private void verifyNoWrites() {
        verify(bids, never()).save(any());
        verify(auctions, never()).save(any());
    }

    private void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
