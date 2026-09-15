package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.project.sneakermarket.auction.dto.AuctionAdminBatchResponse;
import com.project.sneakermarket.auction.dto.AuctionAdminManagementState;
import com.project.sneakermarket.auction.dto.AuctionAdminSearchCriteria;
import com.project.sneakermarket.auction.dto.AuctionSearchCriteria;
import com.project.sneakermarket.auction.dto.BidCreateRequest;
import com.project.sneakermarket.auction.entity.*;
import com.project.sneakermarket.auction.exception.AuctionConflictException;
import com.project.sneakermarket.auction.repository.*;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;
import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.member.repository.MemberRepository;

/** 真實 MSSQL：驗證管理下架、恢復、Soft Delete、公開可見性與帳本資料保留。 */
@SpringBootTest(properties = {
        "auction.lifecycle.enabled=false", "auction.order.enabled=false",
        "auction.email.enabled=false", "spring.jpa.hibernate.ddl-auto=update"
})
class AuctionAdminIntegrationTests {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 12, 10, 0);

    @Autowired private AuctionAdminMutationService mutations;
    @Autowired private AuctionAdminBatchService batches;
    @Autowired private AuctionAdminQueryService adminQueries;
    @Autowired private AuctionQueryService publicQueries;
    @Autowired private AuctionBidService bidding;
    @Autowired private AuctionCommandService commands;
    @Autowired private AuctionLifecycleService lifecycle;
    @Autowired private AuctionRepository auctions;
    @Autowired private AuctionProductRepository products;
    @Autowired private AuctionProductImageRepository images;
    @Autowired private AuctionBidRepository bids;
    @Autowired private AuctionResultRepository results;
    @Autowired private AuctionOrderRepository orders;
    @Autowired private AuctionEmailLogRepository emailLogs;
    @Autowired private MemberRepository members;
    @MockitoBean private AuctionTime time;

    private final List<Long> auctionIds = new ArrayList<>();
    private final List<Long> productIds = new ArrayList<>();
    private final List<Long> memberIds = new ArrayList<>();
    private Long adminId;
    private Long sellerId;
    private Long buyerId;

    @BeforeEach
    void setup() {
        org.mockito.Mockito.when(time.now()).thenReturn(NOW);
        adminId = member("管理員", "ROLE_ADMIN");
        sellerId = member("賣家", "ROLE_MEMBER");
        buyerId = member("買家", "ROLE_MEMBER");
    }

    @AfterEach
    void cleanupCommittedFixtures() {
        for (Long auctionId : auctionIds) {
            emailLogs.deleteAllInBatch(emailLogs.findByAuctionId(auctionId));
            orders.findByAuctionId(auctionId).ifPresent(orders::delete);
            results.findByAuctionId(auctionId).ifPresent(results::delete);
            bids.deleteAllInBatch(bids.findByAuctionIdOrderByCreatedAtDesc(auctionId));
            if (auctions.existsById(auctionId)) auctions.deleteById(auctionId);
        }
        for (Long productId : productIds) {
            images.deleteAllInBatch(images.findByAuctionProductIdOrderBySortOrderAsc(productId));
            if (products.existsById(productId)) products.deleteById(productId);
        }
        for (Long memberId : memberIds) {
            if (members.existsById(memberId)) members.deleteById(memberId);
        }
    }

    @Test
    void soldRemoveRejectsRestoreAndSoftDeletePreservesBidResultOrderEmailProductAndImage() {
        Long auctionId = fixture(AuctionStatus.SOLD, true, "帳本保留測試鞋");
        Long productId = persisted(auctionId).getAuctionProductId();
        AuctionResult result = soldResult(auctionId);
        AuctionOrder order = soldOrder(auctionId);
        AuctionEmailLog email = sentSellerEmail(auctionId);

        Auction before = persisted(auctionId);
        AuctionBid bidBefore = bids.findByAuctionIdOrderByCreatedAtDesc(auctionId).get(0);
        BigDecimal priceBefore = before.getCurrentPrice();
        Integer countBefore = before.getBidCount();
        Long bidderBefore = before.getHighestBidderId();
        Long resultId = result.getResultId();
        Long orderId = order.getAuctionOrderId();
        Long emailId = email.getEmailLogId();

        mutations.removeAuction(auctionId, adminId, "違反平台規範");

        Auction removed = persisted(auctionId);
        assertEquals(AuctionStatus.SOLD, removed.getStatus());
        assertEquals(AuctionStatus.SOLD, removed.getRemovedFromStatus());
        assertEquals(adminId, removed.getRemovedByMemberId());
        assertEquals("違反平台規範", removed.getRemoveReason());
        assertEquals(NOW, removed.getRemovedAt());
        assertLedger(auctionId, priceBefore, countBefore, bidderBefore, bidBefore.getBidId());
        assertSoldLedgerUnchanged(auctionId, resultId, orderId, emailId);
        assertTrue(products.existsById(productId));
        assertEquals(1, images.findByAuctionProductIdOrderBySortOrderAsc(productId).size());
        assertPubliclyHidden(auctionId);

        AuctionAdminSearchCriteria removedCriteria = new AuctionAdminSearchCriteria();
        removedCriteria.setKeyword("帳本保留");
        removedCriteria.setStatus(AuctionStatus.SOLD);
        removedCriteria.setManagementState(AuctionAdminManagementState.REMOVED);
        var removedPage = adminQueries.search(removedCriteria, PageRequest.of(0, 10));
        assertEquals(1, removedPage.items().stream()
                .filter(item -> item.auctionId().equals(auctionId)).count());
        var removedItem = removedPage.items().stream()
                .filter(item -> item.auctionId().equals(auctionId)).findFirst().orElseThrow();
        assertEquals(AuctionAdminManagementState.REMOVED, removedItem.managementState());
        assertTrue(removedItem.imageUrl().startsWith("/uploads/auction/admin-11-"));
        var removedDetail = adminQueries.detail(auctionId);
        assertEquals(AuctionAdminManagementState.REMOVED, removedDetail.auction().managementState());
        assertEquals(1, removedDetail.imageUrls().size());

        AuctionConflictException restoreError = assertThrows(AuctionConflictException.class,
                () -> mutations.restoreAuction(auctionId, adminId));
        assertEquals("已結標或終止的競標無法重新上架", restoreError.getMessage());
        assertEquals(AuctionStatus.SOLD, persisted(auctionId).getStatus());
        assertNotNull(persisted(auctionId).getRemovedAt());
        assertSoldLedgerUnchanged(auctionId, resultId, orderId, emailId);
        assertPubliclyHidden(auctionId);

        mutations.deleteAuction(auctionId, adminId, "管理端清理");
        mutations.deleteAuction(auctionId, adminId, "不可覆寫第一次原因");

        Auction deleted = persisted(auctionId);
        assertEquals(AuctionStatus.SOLD, deleted.getStatus());
        assertEquals(NOW, deleted.getDeletedAt());
        assertEquals(adminId, deleted.getDeletedByMemberId());
        assertEquals("管理端清理", deleted.getDeleteReason());
        assertLedger(auctionId, priceBefore, countBefore, bidderBefore, bidBefore.getBidId());
        assertSoldLedgerUnchanged(auctionId, resultId, orderId, emailId);
        assertTrue(products.existsById(productId));
        assertEquals(1, images.findByAuctionProductIdOrderBySortOrderAsc(productId).size());

        AuctionAdminSearchCriteria deletedCriteria = new AuctionAdminSearchCriteria();
        deletedCriteria.setManagementState(AuctionAdminManagementState.DELETED);
        var deletedPage = adminQueries.search(deletedCriteria, PageRequest.of(0, 1));
        assertEquals(1, deletedPage.size());
        assertTrue(deletedPage.totalElements() >= 1);
        assertEquals(AuctionAdminManagementState.DELETED, deletedPage.items().get(0).managementState());
        assertEquals(AuctionAdminManagementState.DELETED,
                adminQueries.detail(auctionId).auction().managementState());
    }

    @Test
    void activeRemovePreservesBidsBlocksMutationsThenRestoreMakesItBidEligibleAgain() {
        Long auctionId = fixture(AuctionStatus.ACTIVE, true, "公開隱藏測試鞋");
        Long productId = persisted(auctionId).getAuctionProductId();
        Auction before = persisted(auctionId);
        AuctionBid bidBefore = bids.findByAuctionIdOrderByCreatedAtDesc(auctionId).get(0);

        AuctionConflictException notRemoved = assertThrows(AuctionConflictException.class,
                () -> mutations.deleteAuction(auctionId, adminId, "尚未下架"));
        assertEquals("競標必須先下架才能刪除", notRemoved.getMessage());

        mutations.removeAuction(auctionId, adminId, "強制停止");
        mutations.removeAuction(auctionId, adminId, "重複下架不覆寫");

        Auction removed = persisted(auctionId);
        assertEquals(AuctionStatus.ACTIVE, removed.getStatus());
        assertEquals(AuctionStatus.ACTIVE, removed.getRemovedFromStatus());
        assertEquals("強制停止", removed.getRemoveReason());
        assertLedger(auctionId, before.getCurrentPrice(), before.getBidCount(),
                before.getHighestBidderId(), bidBefore.getBidId());
        assertTrue(results.findByAuctionId(auctionId).isEmpty(), "Remove 不得建立 AuctionResult");

        assertThrows(AuctionConflictException.class, () -> bidding.placeBid(
                auctionId, adminId, new BidCreateRequest(new BigDecimal("6000.00"))));
        assertThrows(AuctionConflictException.class, () -> commands.cancelAuction(auctionId, sellerId));
        assertThrows(AuctionConflictException.class, () -> commands.updateAuction(auctionId, sellerId, null));
        lifecycle.processAuction(auctionId);
        assertEquals(AuctionStatus.ACTIVE, persisted(auctionId).getStatus());
        assertTrue(results.findByAuctionId(auctionId).isEmpty());
        assertTrue(auctions.findLifecycleCandidateIds(NOW.plusDays(1)).contains(auctionId));
        assertPubliclyHidden(auctionId);

        mutations.restoreAuction(auctionId, adminId);

        Auction restored = persisted(auctionId);
        assertEquals(AuctionStatus.ACTIVE, restored.getStatus());
        assertNull(restored.getRemovedAt());
        assertPubliclyVisible(auctionId);
        bidding.placeBid(auctionId, adminId, new BidCreateRequest(new BigDecimal("6000.00")));
        assertEquals(2, persisted(auctionId).getBidCount());
        assertEquals(adminId, persisted(auctionId).getHighestBidderId());
        assertAmount("6000.00", persisted(auctionId).getCurrentPrice());
        assertTrue(results.findByAuctionId(auctionId).isEmpty());

        mutations.removeAuction(auctionId, adminId, "刪除前再次下架");
        mutations.deleteAuction(auctionId, adminId, "刪除");
        assertTrue(auctions.existsById(auctionId));
        assertEquals(2, bids.findByAuctionIdOrderByCreatedAtDesc(auctionId).size());
        assertTrue(results.findByAuctionId(auctionId).isEmpty());
        assertTrue(products.existsById(productId));
        assertEquals(1, images.findByAuctionProductIdOrderBySortOrderAsc(productId).size());
    }

    @Test
    void batchRemoveRestoreAndDeleteUseDistinctIdsAndKeepPartialSuccess() {
        Long first = fixture(AuctionStatus.DRAFT, false, "批次一");
        Long second = fixture(AuctionStatus.DRAFT, false, "批次二");
        Long untouched = fixture(AuctionStatus.DRAFT, false, "批次三");
        Long missing = Long.MAX_VALUE - 12345;

        AuctionAdminBatchResponse remove = batches.removeAuctions(
                List.of(first, missing, first, second), adminId, "批次下架");
        assertEquals(3, remove.totalCount());
        assertEquals(2, remove.successCount());
        assertEquals(1, remove.failureCount());
        assertNotNull(persisted(first).getRemovedAt());
        assertNotNull(persisted(second).getRemovedAt());

        AuctionAdminBatchResponse restore = batches.restoreAuctions(
                List.of(first, missing, first, second), adminId);
        assertEquals(3, restore.totalCount());
        assertEquals(2, restore.successCount());
        assertEquals(1, restore.failureCount());
        assertNull(persisted(first).getRemovedAt());
        assertNull(persisted(second).getRemovedAt());
        assertPubliclyHidden(first);
        assertPubliclyHidden(second);

        batches.removeAuctions(List.of(first, second), adminId, "刪除前下架");
        AuctionAdminBatchResponse delete = batches.deleteAuctions(
                List.of(first, untouched, missing, first), adminId, "批次刪除");
        assertEquals(3, delete.totalCount());
        assertEquals(1, delete.successCount());
        assertEquals(2, delete.failureCount());
        assertNotNull(persisted(first).getDeletedAt());
        assertNull(persisted(untouched).getRemovedAt());
        assertNull(persisted(untouched).getDeletedAt());
        assertNotNull(persisted(second).getRemovedAt());
        assertNull(persisted(second).getDeletedAt());

        AuctionAdminBatchResponse retry = batches.deleteAuctions(
                List.of(second, second), adminId, "批次刪除成功項目");
        assertEquals(1, retry.totalCount());
        assertEquals(1, retry.successCount());
        assertNotNull(persisted(second).getDeletedAt());
    }

    @Test
    void batchRestoreRejectsEveryRemovedTerminalStatusPerItem() {
        List<Long> terminalIds = new ArrayList<>();
        for (AuctionStatus status : List.of(
                AuctionStatus.SOLD, AuctionStatus.UNSOLD,
                AuctionStatus.CANCELLED, AuctionStatus.REMOVED)) {
            Long id = fixture(status, false, "終局批次恢復 " + status);
            mutations.removeAuction(id, adminId, "終局競標下架");
            terminalIds.add(id);
        }

        AuctionAdminBatchResponse response = batches.restoreAuctions(terminalIds, adminId);

        assertEquals(4, response.totalCount());
        assertEquals(0, response.successCount());
        assertEquals(4, response.failureCount());
        assertTrue(response.items().stream().allMatch(item -> !item.success()));
        terminalIds.forEach(id -> assertNotNull(persisted(id).getRemovedAt()));
        assertEquals("廢標競標無法重新上架", response.items().get(3).message());
    }

    private void assertPubliclyHidden(Long auctionId) {
        assertTrue(auctions.findPublicById(auctionId).isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> publicQueries.getAuctionStatus(auctionId));
        assertThrows(ResourceNotFoundException.class, () -> publicQueries.getAuctionDetail(auctionId, null));
        assertThrows(ResourceNotFoundException.class, () -> publicQueries.getBidHistory(auctionId));
        AuctionSearchCriteria criteria = new AuctionSearchCriteria();
        criteria.setKeyword("測試鞋");
        assertFalse(publicQueries.searchAuctions(criteria, PageRequest.of(0, 100)).items().stream()
                .anyMatch(item -> item.auctionId().equals(auctionId)));
        assertFalse(publicQueries.getAllAuctions().stream()
                .anyMatch(item -> item.auctionId().equals(auctionId)));
    }

    private void assertPubliclyVisible(Long auctionId) {
        assertTrue(auctions.findPublicById(auctionId).isPresent());
        assertDoesNotThrow(() -> publicQueries.getAuctionStatus(auctionId));
        assertDoesNotThrow(() -> publicQueries.getAuctionDetail(auctionId, null));
        assertDoesNotThrow(() -> publicQueries.getBidHistory(auctionId));
        assertTrue(publicQueries.getAllAuctions().stream()
                .anyMatch(item -> item.auctionId().equals(auctionId)));
    }

    private void assertSoldLedgerUnchanged(Long auctionId, Long resultId, Long orderId, Long emailId) {
        AuctionResult result = results.findByAuctionId(auctionId).orElseThrow();
        assertEquals(resultId, result.getResultId());
        assertEquals(AuctionResultStatus.SOLD, result.getResultStatus());
        assertEquals(buyerId, result.getWinnerId());
        assertAmount("5500.00", result.getFinalPrice());
        assertEquals(orderId, orders.findByAuctionId(auctionId).orElseThrow().getAuctionOrderId());
        assertEquals(emailId, emailLogs.findByAuctionId(auctionId).get(0).getEmailLogId());
        assertEquals(1, emailLogs.findByAuctionId(auctionId).size());
    }

    private void assertLedger(Long auctionId, BigDecimal price, Integer bidCount,
            Long highestBidderId, Long bidId) {
        Auction auction = persisted(auctionId);
        assertEquals(0, price.compareTo(auction.getCurrentPrice()));
        assertEquals(bidCount, auction.getBidCount());
        assertEquals(highestBidderId, auction.getHighestBidderId());
        List<AuctionBid> persistedBids = bids.findByAuctionIdOrderByCreatedAtDesc(auctionId);
        assertEquals(1, persistedBids.size());
        assertEquals(bidId, persistedBids.get(0).getBidId());
        assertAmount("5500.00", persistedBids.get(0).getBidAmount());
    }
    private Long fixture(AuctionStatus status, boolean withBid, String productName) {
        AuctionProduct product = new AuctionProduct();
        product.setSellerId(sellerId);
        product.setBrandId(1L);
        product.setCategoryId(1L);
        product.setProductName(productName);
        product.setModel("ADMIN-11");
        product.setSize("US 9");
        product.setCondition(AuctionCondition.NEW);
        product.setDescription("Feature 11 integration fixture");
        product.setLocation("台北市");
        product.setShippingMethod("宅配");
        product.setShippingFee(BigDecimal.ZERO);
        product.setPaymentMethod("Transfer");
        product.setAllowMeetup(false);
        product.setAllowReturn(false);
        product.setCreatedAt(NOW.minusHours(2));
        product.setUpdatedAt(NOW.minusHours(2));
        product = products.saveAndFlush(product);
        productIds.add(product.getAuctionProductId());

        AuctionProductImage image = new AuctionProductImage();
        image.setAuctionProductId(product.getAuctionProductId());
        image.setImageUrl("/uploads/auction/admin-11-" + UUID.randomUUID() + ".jpg");
        image.setSortOrder(0);
        image.setIsPrimary(true);
        image.setCreatedAt(NOW.minusHours(2));
        images.saveAndFlush(image);

        Auction auction = new Auction();
        auction.setAuctionProductId(product.getAuctionProductId());
        auction.setStartPrice(new BigDecimal("5000.00"));
        auction.setCurrentPrice(withBid ? new BigDecimal("5500.00") : new BigDecimal("5000.00"));
        auction.setMinIncrement(new BigDecimal("500.00"));
        auction.setStartAt(NOW.minusHours(1));
        auction.setEndAt(NOW.plusHours(1));
        auction.setStatus(status);
        auction.setHighestBidderId(withBid ? buyerId : null);
        auction.setBidCount(withBid ? 1 : 0);
        auction.setCreatedAt(NOW.minusHours(2));
        auction.setUpdatedAt(NOW.minusHours(2));
        auction = auctions.saveAndFlush(auction);
        auctionIds.add(auction.getAuctionId());

        if (withBid) {
            AuctionBid bid = new AuctionBid();
            bid.setAuctionId(auction.getAuctionId());
            bid.setBidderId(buyerId);
            bid.setBidAmount(new BigDecimal("5500.00"));
            bid.setCreatedAt(NOW.minusMinutes(30));
            bids.saveAndFlush(bid);
        }
        return auction.getAuctionId();
    }

    private AuctionResult soldResult(Long auctionId) {
        AuctionResult result = new AuctionResult();
        result.setAuctionId(auctionId);
        result.setWinnerId(buyerId);
        result.setFinalPrice(new BigDecimal("5500.00"));
        result.setResultStatus(AuctionResultStatus.SOLD);
        result.setEndedAt(NOW.minusMinutes(10));
        result.setCreatedAt(NOW.minusMinutes(10));
        return results.saveAndFlush(result);
    }

    private AuctionOrder soldOrder(Long auctionId) {
        AuctionOrder order = new AuctionOrder();
        order.setOrderNumber("ADM-" + UUID.randomUUID().toString().replace("-", ""));
        order.setAuctionId(auctionId);
        order.setBuyerId(buyerId);
        order.setSellerId(sellerId);
        order.setFinalPrice(new BigDecimal("5500.00"));
        order.setPaymentMethod("Transfer");
        order.setPaymentStatus(AuctionPaymentStatus.PENDING);
        order.setShippingStatus(AuctionShippingStatus.NOT_SHIPPED);
        order.setOrderStatus(AuctionOrderStatus.PENDING_PAYMENT);
        order.setReceiverName("買家");
        order.setReceiverPhone("0912345678");
        order.setShippingAddress("台北市測試路 11 號");
        order.setCreatedAt(NOW.minusMinutes(9));
        order.setUpdatedAt(NOW.minusMinutes(9));
        return orders.saveAndFlush(order);
    }

    private AuctionEmailLog sentSellerEmail(Long auctionId) {
        AuctionEmailLog log = new AuctionEmailLog();
        log.setAuctionId(auctionId);
        log.setRecipientMemberId(sellerId);
        log.setRecipientEmail(members.findById(sellerId).orElseThrow().getEmail());
        log.setRecipientRole(AuctionEmailRecipientRole.SELLER);
        log.setEmailType(AuctionEmailType.SOLD_SELLER);
        log.setSubject("成交通知");
        log.setSendStatus(AuctionEmailSendStatus.SENT);
        log.setRetryCount(0);
        log.setCreatedAt(NOW.minusMinutes(8));
        log.setUpdatedAt(NOW.minusMinutes(7));
        log.setSentAt(NOW.minusMinutes(7));
        return emailLogs.saveAndFlush(log);
    }

    private Long member(String name, String role) {
        String token = UUID.randomUUID().toString();
        Member member = new Member();
        member.setAccount("admin11-" + token);
        member.setPasswordHash("integration-fixture-not-a-login");
        member.setName(name);
        member.setEmail(token + "@example.invalid");
        member.setPhone("0900000011");
        member.setAddress("台北市測試路 11 號");
        member.setRole(role);
        member = members.saveAndFlush(member);
        memberIds.add(member.getMemberId());
        return member.getMemberId();
    }

    private Auction persisted(Long auctionId) {
        return auctions.findById(auctionId).orElseThrow();
    }

    private void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
