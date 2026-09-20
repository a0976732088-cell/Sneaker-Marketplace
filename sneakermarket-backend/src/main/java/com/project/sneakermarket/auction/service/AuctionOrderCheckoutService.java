package com.project.sneakermarket.auction.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.sneakermarket.auction.dto.AuctionOrderCheckoutUpdateRequest;
import com.project.sneakermarket.auction.dto.AuctionOrderCheckoutUpdateResponse;
import com.project.sneakermarket.auction.entity.AuctionOrder;
import com.project.sneakermarket.auction.entity.AuctionOrderStatus;
import com.project.sneakermarket.auction.entity.AuctionPaymentStatus;
import com.project.sneakermarket.auction.exception.AuctionConflictException;
import com.project.sneakermarket.auction.exception.AuctionUnauthorizedException;
import com.project.sneakermarket.auction.repository.AuctionOrderRepository;
import com.project.sneakermarket.common.exception.BusinessException;
import com.project.sneakermarket.common.exception.ForbiddenException;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/** 付款前更新訂單收件快照；不修改會員資料或任何交易條件。 */
@Service
@RequiredArgsConstructor
public class AuctionOrderCheckoutService {
    private final AuctionOrderRepository orders;
    private final AuctionTime time;

    @Transactional
    public AuctionOrderCheckoutUpdateResponse update(
            String orderNumber,
            Long memberId,
            AuctionOrderCheckoutUpdateRequest request) {
        if (memberId == null || memberId <= 0) {
            throw new AuctionUnauthorizedException();
        }

        AuctionOrder order = orders.findByOrderNumberForUpdate(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("找不到競標訂單"));

        if (!memberId.equals(order.getBuyerId())) {
            throw new ForbiddenException("你沒有權限修改此競標訂單");
        }

        if (order.getPaymentStatus() != AuctionPaymentStatus.PENDING
                || order.getOrderStatus() != AuctionOrderStatus.PENDING_PAYMENT) {
            throw new AuctionConflictException("此競標訂單目前無法修改收件資料");
        }

        LocalDateTime updatedAt = time.now();
        if (order.getPaymentDueAt() == null
                || !updatedAt.isBefore(order.getPaymentDueAt())) {
            throw new AuctionConflictException("競標訂單付款期限已過");
        }

        order.setReceiverName(normalize(request.receiverName(), 50, "收件人姓名"));
        order.setReceiverPhone(normalize(request.receiverPhone(), 20, "收件人電話"));
        order.setShippingAddress(normalize(request.shippingAddress(), 300, "配送地址"));
        order.setUpdatedAt(updatedAt);
        orders.save(order);

        return new AuctionOrderCheckoutUpdateResponse(
                order.getOrderNumber(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getShippingAddress(),
                order.getUpdatedAt());
    }

    private String normalize(String value, int maxLength, String fieldName) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new BusinessException(fieldName + "格式不正確");
        }
        return normalized;
    }
}
