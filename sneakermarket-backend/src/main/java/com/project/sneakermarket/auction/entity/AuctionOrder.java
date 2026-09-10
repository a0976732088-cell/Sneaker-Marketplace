package com.project.sneakermarket.auction.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.project.sneakermarket.member.entity.Member;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 已提交 SOLD Result 的競標專用訂單；收件欄位是建立當下的會員快照。 */
@Entity
@Table(name = "auction_orders", schema = "dbo", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_auction_orders_auction_id", columnNames = "auction_id"),
        @UniqueConstraint(name = "UQ_auction_orders_order_number", columnNames = "order_number")
}, indexes = {
        @Index(name = "IX_auction_orders_buyer_created_at", columnList = "buyer_id, created_at"),
        @Index(name = "IX_auction_orders_seller_created_at", columnList = "seller_id, created_at")
}, check = {
        @CheckConstraint(name = "CK_auction_orders_payment_status",
                constraint = "payment_status IN ('PENDING', 'PAID', 'FAILED')"),
        @CheckConstraint(name = "CK_auction_orders_shipping_status",
                constraint = "shipping_status IN ('NOT_SHIPPED', 'SHIPPED', 'DELIVERED')"),
        @CheckConstraint(name = "CK_auction_orders_order_status",
                constraint = "order_status IN ('PENDING_PAYMENT', 'PAID', 'SHIPPED', 'COMPLETED', 'CANCELLED')")
})
@Getter
@Setter
@NoArgsConstructor
public class AuctionOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auction_order_id")
    private Long auctionOrderId;

    @Column(nullable = false, length = 40, columnDefinition = "NVARCHAR(40)")
    private String orderNumber;

    @Column(name = "auction_id", nullable = false)
    private Long auctionId;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    // 沿用 Result：scalar ID 寫入，唯讀關聯描述 FK；不 cascade 會員或競標。
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false, insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_auction_orders_auctions"))
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false, insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_auction_orders_buyer"))
    private Member buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false, insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_auction_orders_seller"))
    private Member seller;

    @Column(nullable = false, precision = 12, scale = 2, columnDefinition = "DECIMAL(12,2)")
    private BigDecimal finalPrice;

    @Column(nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private AuctionPaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private AuctionShippingStatus shippingStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "NVARCHAR(30)")
    private AuctionOrderStatus orderStatus;

    @Column(nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String receiverName;

    @Column(nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private String receiverPhone;

    @Column(nullable = false, length = 300, columnDefinition = "NVARCHAR(300)")
    private String shippingAddress;

    @Column(nullable = false, updatable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime createdAt;

    @Column(nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime updatedAt;

    @Column(columnDefinition = "DATETIME2(0)")
    private LocalDateTime paidAt;

    @Column(columnDefinition = "DATETIME2(0)")
    private LocalDateTime shippedAt;

    @Column(columnDefinition = "DATETIME2(0)")
    private LocalDateTime completedAt;
}
