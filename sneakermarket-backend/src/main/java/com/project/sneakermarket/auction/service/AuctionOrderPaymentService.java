package com.project.sneakermarket.auction.service;

import java.time.LocalDateTime;
import java.util.EnumSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.sneakermarket.auction.dto.AuctionOrderPaymentResponse;
import com.project.sneakermarket.auction.entity.AuctionOrder;
import com.project.sneakermarket.auction.entity.AuctionOrderStatus;
import com.project.sneakermarket.auction.entity.AuctionPaymentStatus;
import com.project.sneakermarket.auction.exception.AuctionConflictException;
import com.project.sneakermarket.auction.exception.AuctionUnauthorizedException;
import com.project.sneakermarket.auction.repository.AuctionOrderRepository;
import com.project.sneakermarket.common.exception.ForbiddenException;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionOrderPaymentService {
    private static final EnumSet<AuctionOrderStatus> PAID_ORDER_STATES = EnumSet.of(
            AuctionOrderStatus.PAID,
            AuctionOrderStatus.SHIPPED,
            AuctionOrderStatus.COMPLETED);

    private final AuctionOrderRepository orders;
    private final AuctionTime time;

    @Transactional
    public AuctionOrderPaymentResponse pay(String orderNumber, Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new AuctionUnauthorizedException();
        }

        AuctionOrder order = orders.findByOrderNumberForUpdate(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("找不到競標訂單"));

        if (!memberId.equals(order.getBuyerId())) {
            throw new ForbiddenException("你沒有權限付款此競標訂單");
        }

        if (order.getPaymentStatus() == AuctionPaymentStatus.PAID
                && PAID_ORDER_STATES.contains(order.getOrderStatus())) {
            return toResponse(order);
        }

        if (order.getPaymentStatus() != AuctionPaymentStatus.PENDING
                || order.getOrderStatus() != AuctionOrderStatus.PENDING_PAYMENT) {
            throw new AuctionConflictException("此競標訂單目前無法付款");
        }

        LocalDateTime paidAt = time.now();
        order.setPaymentStatus(AuctionPaymentStatus.PAID);
        order.setOrderStatus(AuctionOrderStatus.PAID);
        order.setPaidAt(paidAt);
        order.setUpdatedAt(paidAt);
        orders.save(order);

        return toResponse(order);
    }

    private AuctionOrderPaymentResponse toResponse(AuctionOrder order) {
        return new AuctionOrderPaymentResponse(
                order.getOrderNumber(),
                order.getAuctionId(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getOrderStatus(),
                order.getPaidAt(),
                order.getUpdatedAt());
    }
}
