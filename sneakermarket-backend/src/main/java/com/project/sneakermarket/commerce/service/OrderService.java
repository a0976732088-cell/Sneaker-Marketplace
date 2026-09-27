package com.project.sneakermarket.commerce.service;

import com.project.sneakermarket.commerce.dto.OrderDetailResponse;
import com.project.sneakermarket.commerce.dto.OrderItemResponse;
import com.project.sneakermarket.commerce.dto.OrderListItemResponse;
import com.project.sneakermarket.commerce.email.OrderCreatedEvent;
import com.project.sneakermarket.commerce.email.PaymentSuccessEvent;
import com.project.sneakermarket.commerce.entity.Cart;
import com.project.sneakermarket.commerce.entity.CartItem;
import com.project.sneakermarket.commerce.entity.Order;
import com.project.sneakermarket.commerce.entity.OrderItem;
import com.project.sneakermarket.commerce.entity.ProductImage;
import com.project.sneakermarket.commerce.repository.*;
import com.project.sneakermarket.common.exception.ForbiddenException;
import com.project.sneakermarket.coupon.service.CouponService;
import com.project.sneakermarket.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ProductImageRepository productImageRepository;
    private final MemberService memberService;
    private final CouponService couponService;
    private final ApplicationEventPublisher eventPublisher;

    public List<OrderListItemResponse> getMyOrders(Long memberId) {

        List<Order> orders = orderRepository.findByMemberId(memberId);

        return orders.stream()
                .map(order -> {
                    // ① 查這筆訂單的所有明細
                    List<OrderItem> orderItems = orderItemRepository.findByOrderOrderId(order.getOrderId());

                    String firstProductName = null;
                    String firstImageUrl = null;

                    // ② 有明細才取第一筆（防呆：避免空清單 get(0) 爆錯）
                    if (!orderItems.isEmpty()) {
                        OrderItem firstItem = orderItems.get(0);
                        firstProductName = firstItem.getProductName();

                        // ③ variant 可能是 null（規格被刪除），有的話才查主圖
                        if (firstItem.getVariant() != null) {
                            Long productId = firstItem.getVariant().getProduct().getProductId();
                            List<ProductImage> images =
                                    productImageRepository.findByProductProductIdAndIsPrimaryTrue(productId);
                            firstImageUrl = images.isEmpty() ? null : images.get(0).getImageUrl();
                        }
                    }

                    // ④ 總件數：把每筆明細的數量加起來
                    int itemCount = orderItems.stream()
                            .mapToInt(OrderItem::getQuantity)
                            .sum();

                    return new OrderListItemResponse(
                            order.getOrderId(),
                            order.getOrderNumber(),
                            order.getStatus(),
                            order.getTotalAmount(),
                            order.getCreatedAt(),
                            firstProductName,
                            firstImageUrl,
                            itemCount
                    );
                })
                .collect(Collectors.toList());
    }

    // 查看「單一訂單」的完整內容：訂單本身資料 + 買了哪些商品
    public OrderDetailResponse getOrderDetail(Long memberId, Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("找不到這張訂單"));

        boolean isOwner = order.getMemberId().equals(memberId);
        boolean isAdmin = memberService.isAdmin(memberId);

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("無權查看他人的訂單");
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderOrderId(order.getOrderId());

        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(orderItem -> new OrderItemResponse(
                        orderItem.getOrderItemId(),
                        orderItem.getProductName(),
                        orderItem.getSpecification(),
                        orderItem.getUnitPrice(),
                        orderItem.getQuantity(),
                        orderItem.getLineTotal(),
                        orderItem.getImageUrl()
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
                order.getShippingMethod(),
                order.getCvsStoreId(),
                order.getCvsStoreName(),
                order.getCvsAddress(),
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
                                           String shippingAddress,
                                           Long couponId,
                                           String shippingMethod,
                                           String cvsStoreId,
                                           String cvsStoreName,
                                           String cvsAddress,
                                           String paymentMethod) {

        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new RuntimeException("找不到這個購物車"));

        List<CartItem> cartItems = cartItemRepository.findByCartCartId(cart.getCartId());
        List<CartItem> selectedItems = cartItems.stream()
                .filter(CartItem::getSelected)
                .collect(Collectors.toList());

        if (selectedItems.isEmpty()) {
            throw new RuntimeException("購物車沒有勾選任何商品");
        }

        for (CartItem item : selectedItems) {
            // 商品可能在「加入購物車之後、結帳之前」被下架，結帳當下要再檢查一次
            if (!"ON_SALE".equals(item.getVariant().getProduct().getStatus())) {
                throw new RuntimeException("商品「" + item.getVariant().getProduct().getProductName() + "」已下架，請從購物車移除");
            }

            if (item.getQuantity() > item.getVariant().getStock()) {
                throw new RuntimeException("庫存不足");
            }
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : selectedItems) {
            BigDecimal lineTotal = item.getVariant().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(lineTotal);
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (couponId != null) {
            discount = couponService.useCoupon(memberId, couponId, subtotal);
        }

        String orderNumber = "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", new Random().nextInt(10000));

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setMemberId(memberId);
        order.setCouponId(couponId);
        order.setSubtotal(subtotal);
        order.setDiscountAmount(discount);
        order.setShippingFee(BigDecimal.valueOf(100));
        order.setTotalAmount(subtotal.add(BigDecimal.valueOf(100)).subtract(discount));
        order.setPaymentMethod("LINE_PAY".equals(paymentMethod) ? "LINE_PAY" : "ECPAY_CREDIT_CARD");
        order.setStatus("PENDING");
        order.setReceiverName(receiverName);
        order.setReceiverPhone(receiverPhone);
        order.setShippingAddress(shippingAddress);
        order.setShippingMethod(shippingMethod);
        order.setCvsStoreId(cvsStoreId);
        order.setCvsStoreName(cvsStoreName);
        order.setCvsAddress(cvsAddress);
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

            Long productId = item.getVariant().getProduct().getProductId();
            List<ProductImage> images =
                    productImageRepository.findByProductProductIdAndIsPrimaryTrue(productId);
            String imageUrl = images.isEmpty() ? null : images.get(0).getImageUrl();
            orderItem.setImageUrl(imageUrl);

            orderItemRepository.save(orderItem);

            int newStock = item.getVariant().getStock() - item.getQuantity();
            item.getVariant().setStock(newStock);
            productVariantRepository.save(item.getVariant());
        }

        // 迴圈跑完、全部品項都處理完才清空購物車，不能提早清
        cartItemRepository.deleteAll(selectedItems);

        // 交易完成後要通知會員的下訂單事件（此時交易尚未 commit，事件會排隊等 commit 後才觸發）
        eventPublisher.publishEvent(new OrderCreatedEvent(order.getOrderId(), memberId));

        // 全部存完才組回傳值（借用 getOrderDetail 邏輯）
        return getOrderDetail(memberId, order.getOrderId());
    }

    // 綠界確認付款成功後呼叫，把訂單狀態改成已付款
    @Transactional
    public void confirmPayment(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("找不到這張訂單"));
        if ("PAID".equals(order.getStatus())) return;
        order.setStatus("PAID");
        order.setPaidAt(LocalDateTime.now());

        orderRepository.save(order);

        eventPublisher.publishEvent(
                new PaymentSuccessEvent(
                        order.getOrderId(),
                        order.getMemberId()
                )
        );
    }

    //綠界回報付款失敗時呼叫，取消訂單並還原庫存
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("找不到這張訂單"));

        if (!"PENDING".equals(order.getStatus())) return;

        for (OrderItem item : orderItemRepository.findByOrderOrderId(orderId)) {
            if (item.getVariant() != null) {
                item.getVariant().setStock(item.getVariant().getStock() + item.getQuantity());
                productVariantRepository.save(item.getVariant());
            }
        }

        order.setStatus("CANCELLED");
        orderRepository.save(order);
    }
}