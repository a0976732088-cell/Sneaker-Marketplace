package com.project.sneakermarket.coupon.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.project.sneakermarket.coupon.entity.Coupon;
import com.project.sneakermarket.coupon.entity.CouponStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponse {

    private Long couponId;

    private Long memberId;

    private String couponName;

    private BigDecimal discountAmount;

    private BigDecimal minSpend;

    private CouponStatus status;

    private LocalDateTime issuedAt;

    private LocalDateTime expiresAt;

    private LocalDateTime usedAt;


    public static CouponResponse from(
            Coupon coupon
    ) {

        return new CouponResponse(

                coupon.getCouponId(),

                coupon.getMember()
                        .getMemberId(),

                coupon.getCouponName(),

                coupon.getDiscountAmount(),

                coupon.getMinSpend(),

                coupon.getStatus(),

                coupon.getIssuedAt(),

                coupon.getExpiresAt(),

                coupon.getUsedAt()
        );
    }
}