package com.project.sneakermarket.auction.entity;

import java.time.LocalDateTime;

import com.project.sneakermarket.auction.logistics.ecpay.AuctionCvsStoreType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 一張競標訂單目前有效的超商門市。
 * mapToken 與 requested* 欄位僅保留供既有 SQL Server schema 相容，站內選店不再使用 callback。
 */
@Entity
@Table(name = "auction_cvs_store_selections", schema = "dbo", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_auction_cvs_store_selections_auction_order_id",
                columnNames = "auction_order_id"),
        @UniqueConstraint(name = "UQ_auction_cvs_store_selections_map_token",
                columnNames = "map_token")
}, check = {
        @CheckConstraint(name = "CK_auction_cvs_store_selections_store_type",
                constraint = "store_type IS NULL OR store_type IN ('UNIMART', 'FAMI', 'HILIFE')"),
        @CheckConstraint(name = "CK_auction_cvs_store_selections_requested_store_type",
                constraint = "requested_store_type IS NULL OR requested_store_type IN ('UNIMART', 'FAMI', 'HILIFE')")
})
@Getter
@Setter
@NoArgsConstructor
public class AuctionCvsStoreSelection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auction_order_id", nullable = false, updatable = false)
    private Long auctionOrderId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_order_id", nullable = false, insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "FK_auction_cvs_store_selections_auction_orders"))
    private AuctionOrder auctionOrder;

    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private String mapToken;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, columnDefinition = "NVARCHAR(20)")
    private AuctionCvsStoreType storeType;

    @Column(length = 30, columnDefinition = "NVARCHAR(30)")
    private String logisticsSubType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, columnDefinition = "NVARCHAR(20)")
    private AuctionCvsStoreType requestedStoreType;

    @Column(length = 30, columnDefinition = "NVARCHAR(30)")
    private String requestedLogisticsSubType;

    @Column(length = 30, columnDefinition = "NVARCHAR(30)")
    private String cvsStoreId;

    @Column(length = 100, columnDefinition = "NVARCHAR(100)")
    private String cvsStoreName;

    @Column(length = 300, columnDefinition = "NVARCHAR(300)")
    private String cvsAddress;

    @Column(length = 50, columnDefinition = "NVARCHAR(50)")
    private String cvsTelephone;

    @Column(length = 20, columnDefinition = "NVARCHAR(20)")
    private String cvsOutside;

    @Column(columnDefinition = "DATETIME2(0)")
    private LocalDateTime selectedAt;

    @Column(nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime updatedAt;
}
