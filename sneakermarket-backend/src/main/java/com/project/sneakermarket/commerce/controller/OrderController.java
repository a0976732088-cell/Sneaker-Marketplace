package com.project.sneakermarket.commerce.controller;

import com.project.sneakermarket.commerce.dto.CreateOrderRequest;
import com.project.sneakermarket.commerce.dto.OrderDetailResponse;
import com.project.sneakermarket.commerce.dto.OrderListItemResponse;
import com.project.sneakermarket.commerce.service.OrderService;
import com.project.sneakermarket.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;


    //查我的訂單列表
    @GetMapping("/api/orders/me")
    public List<OrderListItemResponse> getMyOrders(HttpServletRequest request) {

        Long memberId = requireLoggedInMemberId(request);

        return orderService.getMyOrders(memberId);
    }

    //查單一訂單明細
    @GetMapping("/api/orders/{id}")
    public OrderDetailResponse getOrderDetail(@PathVariable Long id,
                                              HttpServletRequest request) {

        Long memberId = requireLoggedInMemberId(request);

        return orderService.getOrderDetail(memberId,id);
    }

    //新增一筆訂單資料
    @PostMapping("/api/orders")
    public OrderDetailResponse createOrder(@RequestBody CreateOrderRequest request,
                                           HttpServletRequest httpRequest) {

        Long memberId = requireLoggedInMemberId(httpRequest);

        return orderService.createOrder(
                memberId,
                request.getReceiverName(),
                request.getReceiverPhone(),
                request.getShippingAddress(),
                request.getCouponId()
        );
    }

    // 共用：取得目前 Session 中的 memberId

    private Long requireLoggedInMemberId(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "尚未登入"
            );
        }

        Long memberId = (Long) session.getAttribute("memberId");

        if (memberId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "尚未登入"
            );
        }

        return memberId;
    }
}

