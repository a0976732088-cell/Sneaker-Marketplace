package com.project.sneakermarket.auction.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 競標主資料 Entity。
 * 對應資料庫中的 dbo.auctions 資料表。
 *
 * 負責保存一場競標的價格、時間、狀態、
 * 目前最高出價會員與出價次數等資訊。
 */
@Entity
@Table(name = "auctions", indexes = {
        @Index(name = "IX_auctions_status_start_at", columnList = "status, start_at"),
        @Index(name = "IX_auctions_status_end_at", columnList = "status, end_at")
})
@Getter
@Setter
@NoArgsConstructor
public class Auction {

    // 競標唯一編號
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long auctionId;

    // 此場競標所對應的競標商品 ID
    @Column(nullable = false, unique = true)
    private Long auctionProductId;

    // 起標價格
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal startPrice;

    // 目前最高價格
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal currentPrice;

    // 每次最低加價金額
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal minIncrement;

    // 競標開始時間
    @Column(nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime startAt;

    // 競標結束時間
    @Column(nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime endAt;

    // 競標目前狀態
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuctionStatus status;

    // 目前最高出價會員 ID，尚未有人出價時為 null
    private Long highestBidderId;

    // 成功出價總次數
    @Column(nullable = false)
    private Integer bidCount;

    /**
     * JPA 樂觀鎖版本欄位。
     * 每次資料被更新時版本號會自動增加，
     * 可協助偵測同一筆競標資料被同時修改的情況。
     */
    @Version
    @Column(nullable = false)
    private Integer version;

    // 競標建立時間
    @Column(nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime createdAt;

    // 競標最後更新時間
    @Column(nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime updatedAt;

    // 平台下架時間；終局交易狀態仍保留在 status。
    @Column(columnDefinition = "DATETIME2(0)")
    private LocalDateTime removedAt;

    // 執行下架的管理員會員 ID，採 scalar ID 避免跨模組寫入關聯。
    private Long removedByMemberId;

    @Column(length = 500, columnDefinition = "NVARCHAR(500)")
    private String removeReason;

    // 下架當下的競標狀態，供管理端顯示管理資訊。
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AuctionStatus removedFromStatus;

    // 管理端 Soft Delete metadata；不實體刪除任何競標帳本資料。
    @Column(columnDefinition = "DATETIME2(0)")
    private LocalDateTime deletedAt;

    private Long deletedByMemberId;

    @Column(length = 500, columnDefinition = "NVARCHAR(500)")
    private String deleteReason;
}
