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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 競標商品 Entity。
 * 對應資料庫中的 dbo.auction_products 資料表。
 *
 * 此 Entity 負責保存競標商品本身的基本資料，
 * 例如商品名稱、品牌、尺寸、商品狀況、運送方式與交易條件。
 */
@Entity
@Table(name = "auction_products")
@Getter
@Setter
@NoArgsConstructor
public class AuctionProduct {

    // 競標商品唯一編號
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long auctionProductId;

    // 刊登此商品的會員 ID
    @Column(nullable = false)
    private Long sellerId;

    // 商品品牌 ID
    @Column(nullable = false)
    private Long brandId;

    // 商品分類 ID
    @Column(nullable = false)
    private Long categoryId;

    // 商品名稱
    @Column(nullable = false, length = 150)
    private String productName;

    // 球鞋型號或系列名稱
    @Column(length = 100)
    private String model;

    // 球鞋尺寸，例如 US 9、US 10
    @Column(nullable = false, length = 20)
    private String size;

    // 商品狀況：NEW、LIKE_NEW、USED
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuctionCondition condition;

    // 商品完整介紹
    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String description;

    // 瑕疵或使用痕跡說明
    @Column(length = 1000)
    private String defectNote;

    // 真偽、購買來源或相關說明
    @Column(length = 1000)
    private String authenticityNote;

    // 商品所在地
    @Column(length = 100)
    private String location;

    // 運送方式
    @Column(nullable = false, length = 50)
    private String shippingMethod;

    // 運費
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingFee;

    // 接受的付款方式
    @Column(nullable = false, length = 50)
    private String paymentMethod;

    // 是否接受面交
    @Column(nullable = false)
    private Boolean allowMeetup;

    // 是否接受退貨
    @Column(nullable = false)
    private Boolean allowReturn;

    // 保固或售後說明
    @Column(length = 500)
    private String warrantyNote;

    // 商品資料建立時間
    @Column(nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime createdAt;

    // 商品資料最後更新時間
    @Column(nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime updatedAt;
}