package com.project.sneakermarket.commerce.service;

import com.project.sneakermarket.commerce.dto.OrderDetailResponse;
import com.project.sneakermarket.commerce.dto.OrderItemResponse;
import com.project.sneakermarket.commerce.dto.OrderListItemResponse;
import com.project.sneakermarket.commerce.entity.Cart;
import com.project.sneakermarket.commerce.entity.CartItem;
import com.project.sneakermarket.commerce.entity.Order;
import com.project.sneakermarket.commerce.entity.OrderItem;
import com.project.sneakermarket.commerce.repository.*;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;

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

    // 建立訂單（結帳）
    @Transactional
    public OrderDetailResponse createOrder(Long memberId,
                                           String receiverName,
                                           String receiverPhone,
                                           String shippingAddress) {

        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new RuntimeException("找不到這個購物車"));

        List<CartItem> cartItems = cartItemRepository.findByCartCartId(cart.getCartId());
        List<CartItem> selectedItems = cartItems.stream()
                .filter(item -> item.getSelected())
                .collect(Collectors.toList());

        if (selectedItems.isEmpty()) {
            throw new RuntimeException("購物車沒有勾選任何商品");
        }

        for (CartItem item : selectedItems) {
            if (item.getQuantity() > item.getVariant().getStock()) {
                throw new RuntimeException("庫存不足");
            }
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : selectedItems) {
            BigDecimal lineTotal = item.getVariant().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(lineTotal);
        }

        String orderNumber = "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", new Random().nextInt(10000));

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setMemberId(memberId);
        order.setSubtotal(subtotal);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setShippingFee(BigDecimal.valueOf(100));
        order.setTotalAmount(subtotal.add(BigDecimal.valueOf(100)));
        order.setPaymentMethod("MOCK_CREDIT_CARD");
        order.setStatus("PAID");
        order.setReceiverName(receiverName);
        order.setReceiverPhone(receiverPhone);
        order.setShippingAddress(shippingAddress);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        // 每個勾選品項各自建立一筆訂單明細，並扣掉對應庫存
        for (CartItem item : selectedItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setVariant(item.getVariant());
            orderItem.setProductName(item.getVariant().getProduct().getProductName());
            orderItem.setSpecification(item.getVariant().getSize() + " / " + item.getVariant().getColor());
            orderItem.setUnitPrice(item.getVariant().getPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setLineTotal(item.getVariant().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            orderItemRepository.save(orderItem);

            int newStock = item.getVariant().getStock() - item.getQuantity();
            item.getVariant().setStock(newStock);
            productVariantRepository.save(item.getVariant());
        }

        // 迴圈跑完、全部品項都處理完才清空購物車，不能提早清
        cartItemRepository.deleteAll(selectedItems);

        // 全部存完才組回傳值（借用 getOrderDetail 邏輯）
        return getOrderDetail(order.getOrderId());
    }
}