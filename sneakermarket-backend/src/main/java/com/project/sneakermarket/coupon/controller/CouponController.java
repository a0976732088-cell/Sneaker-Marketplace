package com.project.sneakermarket.coupon.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.project.sneakermarket.coupon.dto.CouponResponse;
import com.project.sneakermarket.coupon.service.CouponService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;


    @GetMapping("/me")
    public List<CouponResponse> getMyCoupons(
            HttpServletRequest request
    ) {

        Long memberId =
                requireLoggedInMemberId(
                        request
                );


        return couponService
                .getMyCoupons(
                        memberId
                );
    }

    @GetMapping("/me/available")
    public List<CouponResponse> getAvailableCoupons(
            HttpServletRequest request
    ) {

        Long memberId =
                requireLoggedInMemberId(
                        request
                );


        return couponService
                .getAvailableCoupons(
                        memberId
                );
    }


    // =====================================================
    // 試算 Coupon
    //
    // GET
    // /api/coupons/{couponId}/discount
    // ?orderAmount=1200
    // =====================================================
    @GetMapping("/{couponId}/discount")
    public BigDecimal calculateDiscount(
            @PathVariable Long couponId,
            @RequestParam BigDecimal orderAmount,
            HttpServletRequest request
    ) {

        Long memberId =
                requireLoggedInMemberId(
                        request
                );


        return couponService
                .calculateDiscount(
                        memberId,
                        couponId,
                        orderAmount
                );
    }

    @PostMapping("/{couponId}/use")
    public BigDecimal useCoupon(
            @PathVariable Long couponId,
            @RequestParam BigDecimal orderAmount,
            HttpServletRequest request
    ) {

        Long memberId =
                requireLoggedInMemberId(
                        request
                );


        return couponService
                .useCoupon(
                        memberId,
                        couponId,
                        orderAmount
                );
    }


    private Long requireLoggedInMemberId(
            HttpServletRequest request
    ) {

        HttpSession session =
                request.getSession(false);


        if (session == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "尚未登入"
            );
        }


        Long memberId =
                (Long) session.getAttribute(
                        "memberId"
                );


        if (memberId == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "尚未登入"
            );
        }


        return memberId;
    }
}