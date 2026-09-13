package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;

import com.project.sneakermarket.auction.dto.AuctionAdminManagementState;
import com.project.sneakermarket.auction.entity.*;
import com.project.sneakermarket.auction.repository.*;
import com.project.sneakermarket.common.exception.BusinessException;
import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.member.repository.MemberRepository;

/** 真實 MSSQL 驗證聚合、count 與 OFFSET/FETCH；每個案例僅建立自己的 fixture 並回滾。 */
@SpringBootTest(properties = {
        "auction.lifecycle.enabled=false", "auction.order.enabled=false",
        "spring.jpa.hibernate.ddl-auto=update", "spring.jpa.properties.hibernate.generate_statistics=true"
})
@Transactional
class AuctionMemberQueryIntegrationTests {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 11, 12, 0);
    @Autowired AuctionMemberQueryService service;
    @Autowired AuctionRepository auctions;
    @Autowired AuctionProductRepository products;
    @Autowired AuctionBidRepository bids;
    @Autowired AuctionResultRepository results;
    @Autowired AuctionOrderRepository orders;
    @Autowired MemberRepository members;
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    Long a;
    Long b;
    Long seller;

    @BeforeEach
    void setup() {
        a = member();
        b = member();
        seller = member();
    }

    @Test
    void bidsAggregateBeforePaginationAndExcludeOtherMember() {
        var first = auction(seller, AuctionStatus.ACTIVE, NOW);
        var second = auction(seller, AuctionStatus.ACTIVE, NOW);
        var other = auction(seller, AuctionStatus.ACTIVE, NOW);
        bid(first, a, "5000", NOW.minusMinutes(3));
        bid(first, a, "6500", NOW);
        bid(first, a, "5500", NOW.minusMinutes(1));
        bid(first, b, "9000", NOW.plusMinutes(1));
        bid(second, a, "6000", NOW.minusMinutes(2));
        bid(other, b, "7000", NOW);
        flush();
        var page = service.getMyBids(a, 0, 1);
        assertEquals(2, page.totalElements());
        assertEquals(2, page.totalPages());
        assertEquals(1, page.items().size());
        assertEquals(first.getAuctionId(), page.items().get(0).auctionId());
        assertMoney("6500", page.items().get(0).myHighestBid());
        assertEquals(NOW, page.items().get(0).lastBidAt());
        assertEquals(second.getAuctionId(), service.getMyBids(a, 1, 1).items().get(0).auctionId());
        var beyond = service.getMyBids(a, 2, 1);
        assertTrue(beyond.items().isEmpty());
        assertEquals(2, beyond.totalElements());
        assertEquals(2, service.getMyBids(b, 0, 10).totalElements());
    }

    @Test
    void activeLeadingOnlyUsesCurrentMemberAndNeverClaimsWon() {
        var leading = auction(seller, AuctionStatus.ACTIVE, NOW);
        leading.setHighestBidderId(a);
        var outbid = auction(seller, AuctionStatus.ACTIVE, NOW);
        outbid.setHighestBidderId(b);
        bid(leading, a, "6500", NOW);
        bid(outbid, a, "5500", NOW.minusSeconds(1));
        flush();
        var rows = service.getMyBids(a, 0, 10).items();
        assertTrue(rows.get(0).leading());
        assertFalse(rows.get(1).leading());
        for (var row : rows) {
            assertFalse(row.won());
            assertNull(row.resultStatus());
            assertNull(row.finalPrice());
            assertEquals(AuctionAdminManagementState.VISIBLE, row.managementState());
        }
    }

    @Test
    void soldWonComesFromResultEvenWhenWinnerAddressAndOrderAreMissing() {
        members.findById(a).orElseThrow().setAddress(null);
        var sold = auction(seller, AuctionStatus.SOLD, NOW);
        sold.setHighestBidderId(a);
        bid(sold, a, "6500", NOW);
        bid(sold, b, "5500", NOW.minusSeconds(1));
        result(sold, AuctionResultStatus.SOLD, a);
        flush();
        var winner = service.getMyBids(a, 0, 10).items().get(0);
        var loser = service.getMyBids(b, 0, 10).items().get(0);
        assertTrue(winner.won());
        assertFalse(loser.won());
        assertFalse(winner.leading());
        assertFalse(loser.leading());
        assertEquals(AuctionResultStatus.SOLD, winner.resultStatus());
        assertEquals(AuctionResultStatus.SOLD, loser.resultStatus());
        assertMoney("6500", winner.finalPrice());
        assertMoney("6500", loser.finalPrice());
        assertTrue(service.getMyAuctionOrders(a, 0, 10).items().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(value = AuctionStatus.class, names = {"SOLD", "UNSOLD", "CANCELLED", "REMOVED"})
    void bidHistoryKeepsEveryTerminalStatus(AuctionStatus status) {
        var auction = auction(seller, status, NOW);
        bid(auction, a, "6500", NOW);
        result(auction, AuctionResultStatus.valueOf(status.name()), status == AuctionStatus.SOLD ? b : null);
        flush();
        var row = service.getMyBids(a, 0, 10).items().get(0);
        assertEquals(status, row.status());
        assertEquals(status == AuctionStatus.REMOVED ? AuctionAdminManagementState.REMOVED
                : AuctionAdminManagementState.VISIBLE, row.managementState());
        assertEquals(status.name(), row.resultStatus().name());
        assertFalse(row.leading());
        assertFalse(row.won());
        if (status != AuctionStatus.SOLD) assertNull(row.finalPrice());
    }

    @ParameterizedTest
    @EnumSource(value = AuctionStatus.class, names = {"SCHEDULED", "ACTIVE", "SOLD", "UNSOLD", "CANCELLED", "REMOVED"})
    void listingsKeepAllStatusesAndOnlyCurrentSeller(AuctionStatus status) {
        var mine = auction(a, status, NOW);
        auction(b, status, NOW.plusDays(1));
        if (status != AuctionStatus.ACTIVE && status != AuctionStatus.SCHEDULED) {
            result(mine, AuctionResultStatus.valueOf(status.name()), status == AuctionStatus.SOLD ? b : null);
        }
        flush();
        var page = service.getMyListings(a, 0, 10);
        assertEquals(1, page.totalElements());
        var row = page.items().get(0);
        assertEquals(mine.getAuctionId(), row.auctionId());
        assertEquals(status, row.status());
        assertEquals(status == AuctionStatus.REMOVED ? AuctionAdminManagementState.REMOVED
                : AuctionAdminManagementState.VISIBLE, row.managementState());
        assertEquals("Member query fixture", row.productName());
        assertEquals("Model 9", row.model());
        assertEquals("US 9", row.size());
        assertMoney("6500", row.currentPrice());
        assertEquals(3, row.bidCount());
        assertEquals(NOW, row.createdAt());
        assertEquals(NOW.minusHours(1), row.startAt());
        assertEquals(NOW.plusHours(1), row.endAt());
        if (status == AuctionStatus.SOLD) assertMoney("6500", row.finalPrice());
        if (status == AuctionStatus.ACTIVE || status == AuctionStatus.SCHEDULED) assertNull(row.resultStatus());
        else assertEquals(status.name(), row.resultStatus().name());
    }

    @Test
    void removedAndDeletedActiveAuctionsRemainInMemberHistoryWithManagementState() {
        var removed = auction(a, AuctionStatus.ACTIVE, NOW.minusMinutes(2));
        removed.setHighestBidderId(a);
        removed.setRemovedAt(NOW.minusMinutes(1));
        removed.setRemovedByMemberId(seller);
        removed.setRemoveReason("平台下架測試");

        var deleted = auction(a, AuctionStatus.ACTIVE, NOW.minusMinutes(1));
        deleted.setHighestBidderId(a);
        deleted.setRemovedAt(NOW.minusSeconds(30));
        deleted.setRemovedByMemberId(seller);
        deleted.setRemoveReason("刪除前下架");
        deleted.setDeletedAt(NOW);
        deleted.setDeletedByMemberId(seller);
        deleted.setDeleteReason("平台刪除測試");

        bid(removed, a, "6500", NOW.minusMinutes(1));
        bid(deleted, a, "6500", NOW);
        flush();

        var bidRows = service.getMyBids(a, 0, 10).items();
        var removedBid = bidRows.stream().filter(row -> row.auctionId().equals(removed.getAuctionId()))
                .findFirst().orElseThrow();
        var deletedBid = bidRows.stream().filter(row -> row.auctionId().equals(deleted.getAuctionId()))
                .findFirst().orElseThrow();
        assertEquals(AuctionAdminManagementState.REMOVED, removedBid.managementState());
        assertEquals(AuctionAdminManagementState.DELETED, deletedBid.managementState());
        assertFalse(removedBid.leading());
        assertFalse(deletedBid.leading());

        var listingRows = service.getMyListings(a, 0, 10).items();
        assertEquals(AuctionAdminManagementState.REMOVED,
                listingRows.stream().filter(row -> row.auctionId().equals(removed.getAuctionId()))
                        .findFirst().orElseThrow().managementState());
        assertEquals(AuctionAdminManagementState.DELETED,
                listingRows.stream().filter(row -> row.auctionId().equals(deleted.getAuctionId()))
                        .findFirst().orElseThrow().managementState());
    }
    @Test
    void listingsPageByCreationTimeThenId() {
        var old = auction(a, AuctionStatus.CANCELLED, NOW.minusDays(1));
        var sameTimeFirst = auction(a, AuctionStatus.SOLD, NOW);
        var sameTimeLast = auction(a, AuctionStatus.ACTIVE, NOW);
        flush();
        var page = service.getMyListings(a, 0, 2);
        assertEquals(3, page.totalElements());
        assertEquals(2, page.totalPages());
        assertEquals(List.of(sameTimeLast.getAuctionId(), sameTimeFirst.getAuctionId()),
                page.items().stream().map(row -> row.auctionId()).toList());
        assertEquals(old.getAuctionId(), service.getMyListings(a, 1, 2).items().get(0).auctionId());
    }

    @Test
    void ordersAreBuyerScopedEvenWhenMemberSellsOtherOrders() {
        var old = order(auction(seller, AuctionStatus.SOLD, NOW), a, seller, NOW.minusDays(1));
        var recent = order(auction(seller, AuctionStatus.SOLD, NOW), a, seller, NOW);
        order(auction(a, AuctionStatus.SOLD, NOW), b, a, NOW.plusDays(1));
        flush();
        var page = service.getMyAuctionOrders(a, 0, 1);
        assertEquals(2, page.totalElements());
        assertEquals(2, page.totalPages());
        var row = page.items().get(0);
        assertEquals(recent.getOrderNumber(), row.orderNumber());
        assertEquals(recent.getAuctionId(), row.auctionId());
        assertEquals("Member query fixture", row.productName());
        assertMoney("6500", row.finalPrice());
        assertEquals("Transfer", row.paymentMethod());
        assertEquals(AuctionPaymentStatus.PENDING, row.paymentStatus());
        assertEquals(AuctionShippingStatus.NOT_SHIPPED, row.shippingStatus());
        assertEquals(AuctionOrderStatus.PENDING_PAYMENT, row.orderStatus());
        assertEquals(NOW, row.createdAt());
        assertEquals(NOW, row.updatedAt());
        assertEquals(old.getOrderNumber(), service.getMyAuctionOrders(a, 1, 1).items().get(0).orderNumber());
        assertEquals(1, service.getMyAuctionOrders(b, 0, 10).totalElements());
        assertTrue(service.getMyAuctionOrders(seller, 0, 10).items().isEmpty());
    }

    @Test
    void emptyPagesAndBoundedSizesUseExistingPageContract() {
        for (var page : List.of(service.getMyBids(a, 0, 1000),
                service.getMyListings(a, 0, 1000), service.getMyAuctionOrders(a, 0, 1000))) {
            assertEquals(100, page.size());
            assertEquals(0, page.page());
            assertEquals(0, page.totalElements());
            assertEquals(0, page.totalPages());
            assertTrue(page.items().isEmpty());
        }
        assertThrows(BusinessException.class, () -> service.getMyBids(a, -1, 10));
        assertThrows(BusinessException.class, () -> service.getMyBids(a, 0, 0));
        assertThrows(BusinessException.class, () -> service.getMyListings(a, -1, 10));
        assertThrows(BusinessException.class, () -> service.getMyListings(a, 0, 0));
        assertThrows(BusinessException.class, () -> service.getMyAuctionOrders(a, -1, 10));
        assertThrows(BusinessException.class, () -> service.getMyAuctionOrders(a, 0, 0));
    }

    @Test
    void databaseQueriesAreBoundedPerPageAndNeverWriteEntities() {
        for (int i = 0; i < 6; i++) {
            var auction = auction(seller, AuctionStatus.SOLD, NOW);
            bid(auction, a, "6500", NOW);
            result(auction, AuctionResultStatus.SOLD, a);
            order(auction, a, seller, NOW);
        }
        flush();
        var stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        var bidPage = service.getMyBids(a, 0, 5);
        assertEquals(6, bidPage.totalElements());
        assertEquals(5, bidPage.items().size());
        assertEquals(5, stats.getPrepareStatementCount()); // 聚合 + count + Auction + Product + Result
        em.clear();
        stats.clear();
        service.getMyListings(seller, 0, 5);
        assertEquals(4, stats.getPrepareStatementCount()); // Auction + count + Product + Result
        em.clear();
        stats.clear();
        service.getMyAuctionOrders(a, 0, 5);
        assertEquals(4, stats.getPrepareStatementCount()); // Order + count + Auction + Product
        em.flush();
        assertEquals(0, stats.getEntityInsertCount());
        assertEquals(0, stats.getEntityUpdateCount());
        assertEquals(0, stats.getEntityDeleteCount());
    }

    private void flush() { em.flush(); em.clear(); }
    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
    private Long member() {
        String token = UUID.randomUUID().toString();
        var member = new Member();
        member.setAccount("am-" + token);
        member.setEmail(token + "@example.invalid");
        member.setPasswordHash("fixture-not-a-login");
        member.setName("查詢測試");
        member.setPhone("0912345678");
        member.setAddress("測試地址");
        return members.saveAndFlush(member).getMemberId();
    }
    private Auction auction(Long owner, AuctionStatus status, LocalDateTime createdAt) {
        var p = new AuctionProduct();
        p.setSellerId(owner);
        p.setBrandId(1L);
        p.setCategoryId(1L);
        p.setProductName("Member query fixture");
        p.setModel("Model 9");
        p.setSize("US 9");
        p.setCondition(AuctionCondition.NEW);
        p.setDescription("Rollback-only member query fixture");
        p.setShippingMethod("Delivery");
        p.setShippingFee(BigDecimal.ZERO);
        p.setPaymentMethod("Transfer");
        p.setAllowMeetup(false);
        p.setAllowReturn(false);
        p.setCreatedAt(createdAt);
        p.setUpdatedAt(createdAt);
        products.saveAndFlush(p);
        var auction = new Auction();
        auction.setAuctionProductId(p.getAuctionProductId());
        auction.setStartPrice(new BigDecimal("5000"));
        auction.setCurrentPrice(new BigDecimal("6500"));
        auction.setMinIncrement(new BigDecimal("500"));
        auction.setStartAt(NOW.minusHours(1));
        auction.setEndAt(NOW.plusHours(1));
        auction.setStatus(status);
        auction.setBidCount(3);
        auction.setCreatedAt(createdAt);
        auction.setUpdatedAt(createdAt);
        return auctions.saveAndFlush(auction);
    }
    private void bid(Auction auction, Long bidder, String amount, LocalDateTime at) {
        var bid = new AuctionBid();
        bid.setAuctionId(auction.getAuctionId());
        bid.setBidderId(bidder);
        bid.setBidAmount(new BigDecimal(amount));
        bid.setCreatedAt(at);
        bids.saveAndFlush(bid);
    }
    private void result(Auction auction, AuctionResultStatus status, Long winner) {
        var result = new AuctionResult();
        result.setAuctionId(auction.getAuctionId());
        result.setWinnerId(winner);
        result.setResultStatus(status);
        result.setFinalPrice(status == AuctionResultStatus.SOLD ? new BigDecimal("6500") : null);
        result.setEndedAt(NOW);
        result.setCreatedAt(NOW);
        results.saveAndFlush(result);
    }
    private AuctionOrder order(Auction auction, Long buyer, Long owner, LocalDateTime at) {
        var order = new AuctionOrder();
        order.setAuctionId(auction.getAuctionId());
        order.setOrderNumber("AO-" + UUID.randomUUID().toString().replace("-", ""));
        order.setBuyerId(buyer);
        order.setSellerId(owner);
        order.setFinalPrice(new BigDecimal("6500"));
        order.setPaymentMethod("Transfer");
        order.setPaymentStatus(AuctionPaymentStatus.PENDING);
        order.setShippingStatus(AuctionShippingStatus.NOT_SHIPPED);
        order.setOrderStatus(AuctionOrderStatus.PENDING_PAYMENT);
        order.setReceiverName("Private");
        order.setReceiverPhone("0912345678");
        order.setShippingAddress("Private address");
        order.setCreatedAt(at);
        order.setUpdatedAt(at);
        return orders.saveAndFlush(order);
    }
}
