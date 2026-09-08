package com.project.sneakermarket.commerce.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// 整張訂單的[完整內容]（一張訂單只有一份）
@Getter
@AllArgsConstructor
public class OrderDetailResponse {
    private Long orderId;
    private String orderNumber;
    private String status;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String receiverName;
    private String receiverPhone;
    private String shippingAddress;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime completedAt;

    private List<OrderItemResponse> items; // 這張訂單買了哪些商品，每筆都是一個 OrderItemResponse
}