package com.project.sneakermarket.coupon.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.sneakermarket.common.exception.BusinessException;
import com.project.sneakermarket.coupon.dto.CouponResponse;
import com.project.sneakermarket.coupon.entity.Coupon;
import com.project.sneakermarket.coupon.entity.CouponStatus;
import com.project.sneakermarket.coupon.repository.CouponRepository;
import com.project.sneakermarket.member.entity.Member;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;


    // =====================================================
    // 新會員 Coupon 規則
    // =====================================================

    // 折抵 100 元
    private static final BigDecimal WELCOME_DISCOUNT =
            new BigDecimal("100.00");

    // 必須超過 1000 元
    private static final BigDecimal WELCOME_MIN_SPEND =
            new BigDecimal("1000.00");

    // 有效期限 30 天
    private static final int WELCOME_VALID_DAYS = 30;


    // =====================================================
    // 註冊成功後發放新會員 Coupon
    // =====================================================
    @Transactional
    public CouponResponse issueWelcomeCoupon(
            Member member
    ) {

        LocalDateTime now =
                LocalDateTime.now();


        Coupon coupon =
                Coupon.builder()

                        .member(member)

                        .couponName(
                                "新會員 100 元折價券"
                        )

                        .discountAmount(
                                WELCOME_DISCOUNT
                        )

                        .minSpend(
                                WELCOME_MIN_SPEND
                        )

                        .status(
                                CouponStatus.AVAILABLE
                        )

                        .issuedAt(now)

                        // 30 天後到期
                        .expiresAt(
                                now.plusDays(
                                        WELCOME_VALID_DAYS
                                )
                        )

                        .build();


        Coupon savedCoupon =
                couponRepository.save(
                        coupon
                );


        return CouponResponse.from(
                savedCoupon
        );
    }


    // =====================================================
    // 查詢會員全部 Coupon
    // =====================================================
    @Transactional
    public List<CouponResponse> getMyCoupons(
            Long memberId
    ) {

        return couponRepository
                .findByMemberMemberIdOrderByIssuedAtDesc(
                        memberId
                )

                .stream()

                // 查詢時順便檢查是否已過期
                .map(
                        this::refreshExpiredStatus
                )

                .map(
                        CouponResponse::from
                )

                .toList();
    }


    // =====================================================
    // 查詢目前可使用 Coupon
    // =====================================================
    @Transactional
    public List<CouponResponse> getAvailableCoupons(
            Long memberId
    ) {

        return couponRepository
                .findByMemberMemberIdAndStatusOrderByIssuedAtDesc(

                        memberId,

                        CouponStatus.AVAILABLE
                )

                .stream()

                .map(
                        this::refreshExpiredStatus
                )

                // refresh 後只保留 AVAILABLE
                .filter(
                        coupon ->

                                coupon.getStatus()

                                        ==

                                CouponStatus.AVAILABLE
                )

                .map(
                        CouponResponse::from
                )

                .toList();
    }


    // =====================================================
    // 試算折價
    //
    // 不會核銷 Coupon
    //
    // Checkout 畫面可以使用
    // =====================================================
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(

            Long memberId,

            Long couponId,

            BigDecimal orderSubtotal
    ) {

        Coupon coupon =
                getCouponForMember(

                        memberId,

                        couponId
                );


        validateCoupon(

                coupon,

                orderSubtotal
        );


        // 正常為 100
        // 另外做安全保護，折價不能高過小計
        return coupon
                .getDiscountAmount()

                .min(
                        orderSubtotal
                );
    }


    // =====================================================
    // 正式核銷 Coupon
    //
    // 只能由結帳 OrderService 呼叫
    // =====================================================
    @Transactional
    public BigDecimal useCoupon(

            Long memberId,

            Long couponId,

            BigDecimal orderSubtotal
    ) {

        Coupon coupon =
                getCouponForMember(

                        memberId,

                        couponId
                );


        validateCoupon(

                coupon,

                orderSubtotal
        );


        BigDecimal discount =
                coupon
                        .getDiscountAmount()

                        .min(
                                orderSubtotal
                        );


        // Coupon 改成已使用
        coupon.setStatus(
                CouponStatus.USED
        );

        coupon.setUsedAt(
                LocalDateTime.now()
        );

        // 不一定需要 save
        // 因為 JPA Dirty Checking 也會更新
        // 但保留會比較清楚
        couponRepository.save(
                coupon
        );

        return discount;
    }

    // =====================================================
    // 刪除會員時先刪 Coupon
    // =====================================================
    @Transactional
    public void deleteCouponsByMemberId(
            Long memberId
    ) {

        couponRepository
                .deleteByMemberMemberId(
                        memberId
                );
    }

    // =====================================================
    // 找會員自己的 Coupon
    //
    // 避免會員 A 使用會員 B 的 Coupon
    // =====================================================
    private Coupon getCouponForMember(

            Long memberId,

            Long couponId
    ) {

        return couponRepository

                .findByCouponIdAndMemberMemberId(

                        couponId,

                        memberId
                )

                .orElseThrow(

                        () ->
                                new BusinessException(

                                        "找不到此折價券，或此折價券不屬於目前會員"
                                )
                );
    }

    // =====================================================
    // Coupon 商業規則驗證
    // =====================================================
    private void validateCoupon(

            Coupon coupon,

            BigDecimal orderSubtotal
    ) {


        if (orderSubtotal == null) {

            throw new BusinessException(
                    "訂單金額不可為空"
            );
        }

        if (orderSubtotal.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new BusinessException(
                    "訂單金額必須大於 0"
            );
        }

        // =================================================
        // 已使用
        // =================================================

        if (coupon.getStatus()
                == CouponStatus.USED) {

            throw new BusinessException(
                    "此折價券已使用"
            );
        }

        // =================================================
        // 已過期
        // =================================================

        if (coupon.getStatus()
                == CouponStatus.EXPIRED

                ||

                isExpired(coupon)) {

            throw new BusinessException(
                    "此折價券已過期"
            );
        }

        // =================================================
        // 你的需求：
        //
        // 必須「超過」1000
        //
        // 999  → 不可
        // 1000 → 不可
        // 1001 → 可以
        // =================================================

        if (orderSubtotal.compareTo(
                coupon.getMinSpend()
        ) <= 0) {

            throw new BusinessException(

                    "此折價券需單筆商品小計超過 "

                            + coupon
                                    .getMinSpend()
                                    .stripTrailingZeros()
                                    .toPlainString()

                            + " 元才可使用"
            );
        }
    }

    // =====================================================
    // 判斷是否過期
    // =====================================================
    private boolean isExpired(
            Coupon coupon
    ) {

        if (coupon.getExpiresAt() == null) {

            return false;
        }

        // 現在時間 >= expiresAt
        // 代表已過期
        return !LocalDateTime.now()
                .isBefore(
                        coupon.getExpiresAt()
                );
    }

    // =====================================================
    // 查詢 Coupon 時自動更新過期狀態
    // =====================================================
    private Coupon refreshExpiredStatus(
            Coupon coupon
    ) {

        if (

                coupon.getStatus()
                        == CouponStatus.AVAILABLE
                &&
                isExpired(coupon)
        ) {

            coupon.setStatus(
                    CouponStatus.EXPIRED
            );

            couponRepository.save(
                    coupon
            );
        }

        return coupon;
    }
}