package com.project.sneakermarket.auction.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Auction 專用 LINE Pay payment attempt；同一訂單可保留多次嘗試。 */
@Entity
@Table(name = "auction_line_pay_transactions", schema = "dbo", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_auction_line_pay_transactions_merchant_order_id",
                columnNames = "merchant_order_id"),
        @UniqueConstraint(name = "UQ_auction_line_pay_transactions_line_pay_transaction_id",
                columnNames = "line_pay_transaction_id")
}, check = @CheckConstraint(name = "CK_auction_line_pay_transactions_status",
        constraint = "status IN ('INITIATED', 'REQUESTED', 'CONFIRMED', 'CANCELLED', 'FAILED')"))
@Getter
@Setter
@NoArgsConstructor
public class AuctionLinePayTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auction_order_id", nullable = false, updatable = false)
    private Long auctionOrderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_order_id", nullable = false, insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_auction_line_pay_transactions_auction_orders"))
    private AuctionOrder auctionOrder;

    @Column(nullable = false, updatable = false, length = 80, columnDefinition = "NVARCHAR(80)")
    private String merchantOrderId;

    @Column(length = 100, columnDefinition = "NVARCHAR(100)")
    private String linePayTransactionId;

    @Column(nullable = false, precision = 12, scale = 2, columnDefinition = "DECIMAL(12,2)")
    private BigDecimal amount;

    @Column(nullable = false, length = 3, columnDefinition = "NVARCHAR(3)")
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private AuctionLinePayTransactionStatus status;

    @Column(length = 20, columnDefinition = "NVARCHAR(20)")
    private String returnCode;

    @Column(length = 500, columnDefinition = "NVARCHAR(500)")
    private String returnMessage;

    @Column(nullable = false, updatable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime createdAt;

    @Column(nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime updatedAt;

    @Column(columnDefinition = "DATETIME2(0)")
    private LocalDateTime confirmedAt;

    @Column(columnDefinition = "DATETIME2(0)")
    private LocalDateTime cancelledAt;
}
