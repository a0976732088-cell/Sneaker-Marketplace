package com.project.sneakermarket.auction.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 競標出價紀錄 Entity。
 * 對應資料庫中的 dbo.auction_bids 資料表。
 *
 * 每一筆資料代表一次成功的競標出價紀錄。
 * 被拒絕或不符合最低出價條件的出價，不應寫入此資料表。
 */
@Entity
@Table(name = "auction_bids")
@Getter
@Setter
@NoArgsConstructor
public class AuctionBid {

    // 出價紀錄唯一編號
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bidId;

    // 此筆出價所屬的競標 ID
    @Column(nullable = false)
    private Long auctionId;

    // 出價會員 ID
    @Column(nullable = false)
    private Long bidderId;

    // 成功出價金額
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal bidAmount;

    // 出價成功時間
    @Column(nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime createdAt;
}