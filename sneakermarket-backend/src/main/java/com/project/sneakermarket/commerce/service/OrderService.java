package com.project.sneakermarket.commerce.service;

import com.project.sneakermarket.commerce.dto.OrderDetailResponse;
import com.project.sneakermarket.commerce.dto.OrderItemResponse;
import com.project.sneakermarket.commerce.dto.OrderListItemResponse;
import com.project.sneakermarket.commerce.entity.Order;
import com.project.sneakermarket.commerce.entity.OrderItem;
import com.project.sneakermarket.commerce.repository.OrderItemRepository;
import com.project.sneakermarket.commerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    // 查看「我的訂單列表」：一次列出這個會員的所有訂單摘要
    public List<OrderListItemResponse> getMyOrders(Long memberId) {

        List<Order> orders = orderRepository.findByMemberId(memberId);

        return orders.stream()
                .map(order -> new OrderListItemResponse(
                        order.getOrderId(),
                        order.getOrderNumber(),
                        order.getStatus(),
                        order.getTotalAmount(),
                        order.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    // 查看「單一訂單」的完整內容：訂單本身資料 + 買了哪些商品
    public OrderDetailResponse getOrderDetail(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("找不到這張訂單"));

        // order_items 沒有存 memberId，是透過 order_id 關聯回訂單
        // 所以要用剛查到的 order 的 ID，去找這張訂單底下所有的商品明細
        List<OrderItem> orderItems = orderItemRepository.findByOrderOrderId(order.getOrderId());

        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(orderItem -> new OrderItemResponse(
                        orderItem.getOrderItemId(),
                        orderItem.getProductName(),
                        orderItem.getSpecification(),
                        orderItem.getUnitPrice(),
                        orderItem.getQuantity(),
                        orderItem.getLineTotal()
                ))
                .collect(Collectors.toList());

        return new OrderDetailResponse(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getShippingFee(),
                order.getTotalAmount(),
                order.getPaymentMethod(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getShippingAddress(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getCompletedAt(),
                itemResponses
        );
    }

}