package com.project.sneakermarket.coupon.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.sneakermarket.coupon.entity.Coupon;
import com.project.sneakermarket.coupon.entity.CouponStatus;

public interface CouponRepository
        extends JpaRepository<Coupon, Long> {


    // 查詢會員全部 Coupon
    List<Coupon>
    findByMemberMemberIdOrderByIssuedAtDesc(
            Long memberId
    );


    // 查詢會員指定狀態 Coupon
    List<Coupon>
    findByMemberMemberIdAndStatusOrderByIssuedAtDesc(
            Long memberId,
            CouponStatus status
    );


    // 查詢某張 Coupon
    // 同時確認 Coupon 是這個會員的
    Optional<Coupon>
    findByCouponIdAndMemberMemberId(
            Long couponId,
            Long memberId
    );


    // 刪除會員時一起刪除 Coupon
    void deleteByMemberMemberId(
            Long memberId
    );
}