package com.project.sneakermarket.commerce.controller;

import com.project.sneakermarket.commerce.dto.CreateOrderRequest;
import com.project.sneakermarket.commerce.dto.OrderDetailResponse;
import com.project.sneakermarket.commerce.dto.OrderListItemResponse;
import com.project.sneakermarket.commerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    //查我的訂單列表
    @GetMapping("/api/orders/me")
    public List<OrderListItemResponse> getMyOrders(@RequestParam Long memberId) {
        return orderService.getMyOrders(memberId);
    }

    //查單一訂單明細
    @GetMapping("/api/orders/{id}")
    public OrderDetailResponse getOrderDetail(@PathVariable Long id) {

        return orderService.getOrderDetail(id);
    }

    //新增一筆訂單資料
    @PostMapping("/api/orders")
    public OrderDetailResponse createOrder(@RequestBody CreateOrderRequest request) {
        return orderService.createOrder(
                request.getMemberId(),
                request.getReceiverName(),
                request.getReceiverPhone(),
                request.getShippingAddress()
        );
    }
}

