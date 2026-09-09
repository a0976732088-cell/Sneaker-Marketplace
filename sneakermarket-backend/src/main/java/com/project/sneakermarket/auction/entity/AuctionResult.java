package com.project.sneakermarket.auction.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.project.sneakermarket.member.entity.Member;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 競標結標結果，與競標狀態於同一交易保存；不承擔訂單或收件資料責任。 */
@Entity
@Table(name = "auction_results", check = @CheckConstraint(
        name = "CK_auction_results_status",
        constraint = "result_status IN ('SOLD', 'UNSOLD', 'CANCELLED', 'REMOVED')"))
@Getter
@Setter
@NoArgsConstructor
public class AuctionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long resultId;

    @Column(name = "auction_id", nullable = false, unique = true)
    private Long auctionId;

    @Column(name = "winner_id")
    private Long winnerId;

    // scalar ID 負責寫入；唯讀關聯讓 Hibernate 描述正式 FK，不要求 Service fetch Entity。
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false, insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_auction_results_auctions"))
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id", nullable = true, insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_auction_results_members"))
    private Member winner;

    @Column(precision = 12, scale = 2, columnDefinition = "DECIMAL(12,2)")
    private BigDecimal finalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "NVARCHAR(20)")
    private AuctionResultStatus resultStatus;

    @Column(nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime endedAt;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String reason;

    @Column(nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime createdAt;
}
