package com.project.sneakermarket.auction.entity;

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
 * 競標商品圖片 Entity。
 * 對應資料庫中的 dbo.auction_product_images 資料表。
 *
 * 一個競標商品可以擁有多張圖片，
 * 此 Entity 負責保存圖片路徑、顯示順序與主圖設定。
 */
@Entity
@Table(name = "auction_product_images")
@Getter
@Setter
@NoArgsConstructor
public class AuctionProductImage {

    // 圖片唯一編號
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long imageId;

    // 此圖片所屬的競標商品 ID
    @Column(nullable = false)
    private Long auctionProductId;

    // 圖片 URL 或檔案路徑
    @Column(nullable = false, length = 500)
    private String imageUrl;

    // 圖片顯示順序，數字越小越前面
    @Column(nullable = false)
    private Integer sortOrder;

    // 是否為商品主圖
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary;

    // 圖片資料建立時間
    @Column(nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime createdAt;
}