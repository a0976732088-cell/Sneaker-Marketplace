package com.project.sneakermarket.auction.notification;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionCondition;
import com.project.sneakermarket.auction.entity.AuctionProduct;
import com.project.sneakermarket.auction.entity.AuctionStatus;
import com.project.sneakermarket.auction.repository.AuctionProductRepository;
import com.project.sneakermarket.auction.repository.AuctionRepository;
import com.project.sneakermarket.auction.service.AuctionAdminMutationService;
import com.project.sneakermarket.auction.service.AuctionLifecycleService;
import com.project.sneakermarket.auction.service.AuctionTime;
import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.member.repository.MemberRepository;

/** 真實 Auction transaction + mock notification persistence boundary 的失敗隔離測試。 */
@SpringBootTest(properties = {
        "auction.lifecycle.enabled=false", "auction.order.enabled=false",
        "auction.email.enabled=false", "spring.jpa.hibernate.ddl-auto=validate"
})
class AuctionMajorEventNotificationFailureIntegrationTests {
    private static final String PRIVATE_DESTINATION = "/queue/auction-events";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 17, 10, 0);

    @Autowired private AuctionLifecycleService lifecycle;
    @Autowired private AuctionAdminMutationService adminMutations;
    @Autowired private AuctionRepository auctions;
    @Autowired private AuctionProductRepository products;
    @Autowired private MemberRepository members;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private AuctionTime time;
    @MockitoBean private AuctionNotificationIntegrationService notificationIntegration;
    @MockitoBean private SimpMessagingTemplate messaging;

    private Long adminId;
    private Long sellerId;
    private Long productId;
    private Long auctionId;

    @BeforeEach
    void setup() {
        when(time.now()).thenReturn(NOW);
        adminId = member("重大事件通知失敗管理員", "ROLE_ADMIN");
        sellerId = member("重大事件通知失敗賣家", "ROLE_MEMBER");
    }

    @AfterEach
    void cleanup() {
        if (auctionId != null) {
            jdbc.update(
                    "DELETE FROM dbo.notifications WHERE target_type='AUCTION' AND target_id=?",
                    auctionId
            );
            if (auctions.existsById(auctionId)) auctions.deleteById(auctionId);
        }
        if (productId != null && products.existsById(productId)) products.deleteById(productId);
        if (sellerId != null && members.existsById(sellerId)) members.deleteById(sellerId);
        if (adminId != null && members.existsById(adminId)) members.deleteById(adminId);
    }

    @Test
    void startedNotificationFailureAfterCommitKeepsScheduledToActiveTransition() {
        auctionId = fixture(AuctionStatus.SCHEDULED, NOW, false);
        AtomicBoolean observedCommittedState = new AtomicBoolean();
        when(notificationIntegration.createStartedSellerNotification(
                auctionId, sellerId, NOW
        )).thenAnswer(invocation -> {
            assertEquals(1, committedAuctionCount(
                    "status='ACTIVE' AND updated_at=?", NOW
            ));
            observedCommittedState.set(true);
            throw notificationFailure();
        });

        assertDoesNotThrow(() -> lifecycle.processAuction(auctionId));

        Auction persisted = persisted();
        assertEquals(AuctionStatus.ACTIVE, persisted.getStatus());
        assertEquals(NOW, persisted.getUpdatedAt());
        assertTrue(observedCommittedState.get());
        verify(notificationIntegration).createStartedSellerNotification(
                auctionId, sellerId, NOW
        );
        assertNoPrivateMessageOrNotification();
    }

    @Test
    void removedNotificationFailureAfterCommitKeepsRemovalMetadata() {
        auctionId = fixture(AuctionStatus.ACTIVE, NOW.minusHours(1), false);
        AtomicBoolean observedCommittedState = new AtomicBoolean();
        when(notificationIntegration.createRemovedSellerNotification(
                auctionId, sellerId, "違反平台規範", NOW
        )).thenAnswer(invocation -> {
            assertEquals(1, committedAuctionCount(
                    "status='ACTIVE' AND removed_at=? AND removed_by_member_id=? "
                            + "AND remove_reason=? AND removed_from_status='ACTIVE'",
                    NOW, adminId, "違反平台規範"
            ));
            observedCommittedState.set(true);
            throw notificationFailure();
        });

        assertDoesNotThrow(() -> adminMutations.removeAuction(
                auctionId, adminId, "違反平台規範"
        ));

        Auction persisted = persisted();
        assertEquals(AuctionStatus.ACTIVE, persisted.getStatus());
        assertEquals(AuctionStatus.ACTIVE, persisted.getRemovedFromStatus());
        assertEquals(NOW, persisted.getRemovedAt());
        assertEquals(adminId, persisted.getRemovedByMemberId());
        assertEquals("違反平台規範", persisted.getRemoveReason());
        assertEquals(NOW, persisted.getUpdatedAt());
        assertTrue(observedCommittedState.get());
        verify(notificationIntegration).createRemovedSellerNotification(
                auctionId, sellerId, "違反平台規範", NOW
        );
        assertNoPrivateMessageOrNotification();
    }

    @Test
    void restoredNotificationFailureAfterCommitKeepsClearedRemovalMetadata() {
        auctionId = fixture(AuctionStatus.ACTIVE, NOW.minusHours(1), true);
        AtomicBoolean observedCommittedState = new AtomicBoolean();
        when(notificationIntegration.createRestoredSellerNotification(
                auctionId, sellerId, NOW
        )).thenAnswer(invocation -> {
            assertEquals(1, committedAuctionCount(
                    "status='ACTIVE' AND removed_at IS NULL "
                            + "AND removed_by_member_id IS NULL AND remove_reason IS NULL "
                            + "AND removed_from_status IS NULL AND updated_at=?",
                    NOW
            ));
            observedCommittedState.set(true);
            throw notificationFailure();
        });

        assertDoesNotThrow(() -> adminMutations.restoreAuction(auctionId, adminId));

        Auction persisted = persisted();
        assertEquals(AuctionStatus.ACTIVE, persisted.getStatus());
        assertNull(persisted.getRemovedAt());
        assertNull(persisted.getRemovedByMemberId());
        assertNull(persisted.getRemoveReason());
        assertNull(persisted.getRemovedFromStatus());
        assertEquals(NOW, persisted.getUpdatedAt());
        assertTrue(observedCommittedState.get());
        verify(notificationIntegration).createRestoredSellerNotification(
                auctionId, sellerId, NOW
        );
        assertNoPrivateMessageOrNotification();
    }

    private Long fixture(AuctionStatus status, LocalDateTime startAt, boolean removed) {
        AuctionProduct product = new AuctionProduct();
        product.setSellerId(sellerId);
        product.setBrandId(1L);
        product.setCategoryId(1L);
        product.setProductName("Major event notification failure shoe");
        product.setModel("MAJOR-NOTIFY-FAIL");
        product.setSize("US 9");
        product.setCondition(AuctionCondition.NEW);
        product.setDescription("Major event notification failure fixture");
        product.setLocation("台北市");
        product.setShippingMethod("宅配");
        product.setShippingFee(BigDecimal.ZERO);
        product.setPaymentMethod("轉帳");
        product.setAllowMeetup(false);
        product.setAllowReturn(false);
        product.setCreatedAt(NOW.minusHours(2));
        product.setUpdatedAt(NOW.minusHours(2));
        product = products.saveAndFlush(product);
        productId = product.getAuctionProductId();

        Auction auction = new Auction();
        auction.setAuctionProductId(productId);
        auction.setStartPrice(new BigDecimal("5000.00"));
        auction.setCurrentPrice(new BigDecimal("5000.00"));
        auction.setMinIncrement(new BigDecimal("500.00"));
        auction.setStartAt(startAt);
        auction.setEndAt(NOW.plusHours(1));
        auction.setStatus(status);
        auction.setBidCount(0);
        auction.setCreatedAt(NOW.minusHours(2));
        auction.setUpdatedAt(NOW.minusHours(2));
        if (removed) {
            auction.setRemovedAt(NOW.minusMinutes(10));
            auction.setRemovedByMemberId(adminId);
            auction.setRemoveReason("原下架原因");
            auction.setRemovedFromStatus(status);
        }
        return auctions.saveAndFlush(auction).getAuctionId();
    }

    private Long member(String name, String role) {
        String token = UUID.randomUUID().toString();
        Member member = new Member();
        member.setAccount("major-notify-" + token);
        member.setEmail(token + "@example.invalid");
        member.setPasswordHash("integration-fixture-not-a-login");
        member.setName(name);
        member.setPhone("0912345678");
        member.setAddress("台北市測試路 17 號");
        member.setRole(role);
        return members.saveAndFlush(member).getMemberId();
    }

    private Auction persisted() {
        return auctions.findById(auctionId).orElseThrow();
    }

    private int committedAuctionCount(String predicate, Object... arguments) {
        Object[] queryArguments = new Object[arguments.length + 1];
        queryArguments[0] = auctionId;
        System.arraycopy(arguments, 0, queryArguments, 1, arguments.length);
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dbo.auctions WHERE auction_id=? AND " + predicate,
                Integer.class,
                queryArguments
        );
        return count == null ? 0 : count;
    }

    private void assertNoPrivateMessageOrNotification() {
        verify(messaging, never()).convertAndSendToUser(
                anyString(), eq(PRIVATE_DESTINATION), any()
        );
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM dbo.notifications "
                        + "WHERE target_type='AUCTION' AND target_id=?",
                Integer.class,
                auctionId
        ));
    }

    private DataIntegrityViolationException notificationFailure() {
        return new DataIntegrityViolationException("notification persistence unavailable");
    }
}
