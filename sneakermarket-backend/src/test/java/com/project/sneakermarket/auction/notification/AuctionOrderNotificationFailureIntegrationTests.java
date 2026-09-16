package com.project.sneakermarket.auction.notification;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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
import com.project.sneakermarket.auction.entity.AuctionOrder;
import com.project.sneakermarket.auction.entity.AuctionOrderStatus;
import com.project.sneakermarket.auction.entity.AuctionPaymentStatus;
import com.project.sneakermarket.auction.entity.AuctionProduct;
import com.project.sneakermarket.auction.entity.AuctionShippingStatus;
import com.project.sneakermarket.auction.entity.AuctionStatus;
import com.project.sneakermarket.auction.repository.AuctionOrderRepository;
import com.project.sneakermarket.auction.repository.AuctionProductRepository;
import com.project.sneakermarket.auction.repository.AuctionRepository;
import com.project.sneakermarket.auction.service.AuctionOrderPaymentService;
import com.project.sneakermarket.auction.service.AuctionTime;
import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.member.repository.MemberRepository;

/** 真實 Order transaction + mock notification persistence boundary 的失敗隔離測試。 */
@SpringBootTest(properties = {
        "auction.lifecycle.enabled=false", "auction.order.enabled=false",
        "auction.email.enabled=false", "spring.jpa.hibernate.ddl-auto=validate"
})
class AuctionOrderNotificationFailureIntegrationTests {
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 9, 16, 8, 0);
    private static final LocalDateTime PAID_AT = LocalDateTime.of(2026, 9, 16, 10, 0);

    @Autowired private AuctionOrderPaymentService payments;
    @Autowired private AuctionOrderRepository orders;
    @Autowired private AuctionRepository auctions;
    @Autowired private AuctionProductRepository products;
    @Autowired private MemberRepository members;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private AuctionTime time;
    @MockitoBean private AuctionNotificationIntegrationService notificationIntegration;
    @MockitoBean private SimpMessagingTemplate messaging;

    private Long buyerId;
    private Long sellerId;
    private Long productId;
    private Long auctionId;
    private Long orderId;
    private String orderNumber;

    @BeforeEach
    void setup() {
        buyerId = member("通知失敗付款買家");
        sellerId = member("通知失敗競標賣家");
        createCommittedPendingOrder();
    }

    @AfterEach
    void cleanup() {
        // 即使 mock 設定失誤留下資料，也先於 order/member fixture 清除。
        if (orderId != null) {
            jdbc.update("DELETE FROM dbo.notifications WHERE target_type = 'AUCTION_ORDER' AND target_id = ?",
                    orderId);
        }
        if (orderId != null && orders.existsById(orderId)) orders.deleteById(orderId);
        if (auctionId != null && auctions.existsById(auctionId)) auctions.deleteById(auctionId);
        if (productId != null && products.existsById(productId)) products.deleteById(productId);
        if (buyerId != null && members.existsById(buyerId)) members.deleteById(buyerId);
        if (sellerId != null && members.existsById(sellerId)) members.deleteById(sellerId);
    }

    @Test
    void notificationPersistenceFailureAfterCommitDoesNotRollBackPaidOrder() {
        when(time.now()).thenReturn(PAID_AT);
        when(notificationIntegration.createOrderPaidNotification(
                auctionId, orderId, sellerId, PAID_AT))
                .thenThrow(new DataIntegrityViolationException("notification persistence unavailable"));

        assertDoesNotThrow(() -> payments.pay(orderNumber, buyerId));

        AuctionOrder persisted = orders.findById(orderId).orElseThrow();
        assertEquals(AuctionPaymentStatus.PAID, persisted.getPaymentStatus());
        assertEquals(AuctionOrderStatus.PAID, persisted.getOrderStatus());
        assertEquals(PAID_AT, persisted.getPaidAt());
        verify(notificationIntegration).createOrderPaidNotification(
                auctionId, orderId, sellerId, PAID_AT);
        verifyNoInteractions(messaging);
    }

    private Long member(String name) {
        String token = UUID.randomUUID().toString();
        Member member = new Member();
        member.setAccount("nf-" + token);
        member.setEmail(token + "@example.invalid");
        member.setPasswordHash("integration-fixture-not-a-login");
        member.setName(name);
        member.setPhone("0912345678");
        member.setAddress("台北市測試路 1 號");
        return members.saveAndFlush(member).getMemberId();
    }

    private void createCommittedPendingOrder() {
        AuctionProduct product = new AuctionProduct();
        product.setSellerId(sellerId);
        product.setBrandId(1L);
        product.setCategoryId(1L);
        product.setProductName("Notification failure integration shoe");
        product.setModel("NOTIFY-FAIL");
        product.setSize("US 9");
        product.setCondition(AuctionCondition.NEW);
        product.setDescription("Notification failure integration fixture");
        product.setLocation("台北市");
        product.setShippingMethod("宅配");
        product.setShippingFee(new BigDecimal("100.00"));
        product.setPaymentMethod("轉帳");
        product.setAllowMeetup(false);
        product.setAllowReturn(false);
        product.setCreatedAt(CREATED_AT);
        product.setUpdatedAt(CREATED_AT);
        product = products.saveAndFlush(product);
        productId = product.getAuctionProductId();

        Auction auction = new Auction();
        auction.setAuctionProductId(productId);
        auction.setStartPrice(new BigDecimal("6000.00"));
        auction.setCurrentPrice(new BigDecimal("6800.00"));
        auction.setMinIncrement(new BigDecimal("100.00"));
        auction.setStartAt(CREATED_AT.minusHours(2));
        auction.setEndAt(CREATED_AT.minusHours(1));
        auction.setStatus(AuctionStatus.SOLD);
        auction.setHighestBidderId(buyerId);
        auction.setBidCount(2);
        auction.setCreatedAt(CREATED_AT.minusHours(3));
        auction.setUpdatedAt(CREATED_AT.minusHours(1));
        auction = auctions.saveAndFlush(auction);
        auctionId = auction.getAuctionId();

        orderNumber = "AO-NOTIFY-" + UUID.randomUUID().toString().substring(0, 12);
        AuctionOrder order = new AuctionOrder();
        order.setOrderNumber(orderNumber);
        order.setAuctionId(auctionId);
        order.setBuyerId(buyerId);
        order.setSellerId(sellerId);
        order.setFinalPrice(new BigDecimal("6800.00"));
        order.setPaymentMethod("轉帳");
        order.setPaymentStatus(AuctionPaymentStatus.PENDING);
        order.setShippingStatus(AuctionShippingStatus.NOT_SHIPPED);
        order.setOrderStatus(AuctionOrderStatus.PENDING_PAYMENT);
        order.setReceiverName("通知失敗付款買家");
        order.setReceiverPhone("0912345678");
        order.setShippingAddress("台北市測試路 1 號");
        order.setCreatedAt(CREATED_AT);
        order.setUpdatedAt(CREATED_AT);
        order.setPaymentDueAt(PAID_AT.plusHours(1));
        orderId = orders.saveAndFlush(order).getAuctionOrderId();
        assertNull(order.getPaidAt());
    }
}
