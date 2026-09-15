package com.project.sneakermarket.coupon.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.project.sneakermarket.member.entity.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long couponId;


    // =====================================================
    // 此 Coupon 屬於哪一位會員
    // =====================================================
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "member_id",
            nullable = false
    )
    private Member member;


    // =====================================================
    // Coupon 名稱
    // =====================================================
    @Column(
            name = "coupon_name",
            nullable = false,
            length = 100
    )
    private String couponName;


    // =====================================================
    // 折抵金額
    //
    // 新會員優惠券固定 100 元
    // =====================================================
    @Column(
            name = "discount_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal discountAmount;


    // =====================================================
    // 最低消費門檻
    //
    // 單筆商品小計必須「超過」1000 元
    // =====================================================
    @Column(
            name = "min_spend",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal minSpend;


    // =====================================================
    // AVAILABLE / USED / EXPIRED
    // =====================================================
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private CouponStatus status;


    // =====================================================
    // 發放時間
    // =====================================================
    @Column(
            name = "issued_at",
            nullable = false
    )
    private LocalDateTime issuedAt;


    // =====================================================
    // 到期時間
    //
    // 新會員券 = 發放後 30 天
    // =====================================================
    @Column(
            name = "expires_at",
            nullable = false
    )
    private LocalDateTime expiresAt;


    // =====================================================
    // 使用時間
    //
    // 尚未使用時為 null
    // =====================================================
    @Column(name = "used_at")
    private LocalDateTime usedAt;


    @PrePersist
    public void prePersist() {

        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }

        if (status == null) {
            status = CouponStatus.AVAILABLE;
        }
    }
}