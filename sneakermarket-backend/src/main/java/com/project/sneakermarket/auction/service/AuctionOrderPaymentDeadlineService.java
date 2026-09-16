package com.project.sneakermarket.auction.service;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.project.sneakermarket.auction.email.AuctionEmailReadyEvent;
import com.project.sneakermarket.auction.email.AuctionEmailService;
import com.project.sneakermarket.auction.entity.AuctionOrder;
import com.project.sneakermarket.auction.entity.AuctionOrderStatus;
import com.project.sneakermarket.auction.entity.AuctionPaymentStatus;
import com.project.sneakermarket.auction.notification.AuctionPrivateNotificationEventPublisher;
import com.project.sneakermarket.auction.repository.AuctionOrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionOrderPaymentDeadlineService {
    private final AuctionOrderRepository orders;
    private final AuctionTime time;
    private final AuctionEmailService emails;
    private final AuctionPrivateNotificationEventPublisher privateNotificationEvents;
    private final ApplicationEventPublisher events;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireOverdueOrder(Long orderId) {
        AuctionOrder order = orders.findByIdForUpdate(orderId).orElse(null);
        if (order == null
                || order.getPaymentStatus() != AuctionPaymentStatus.PENDING
                || order.getOrderStatus() != AuctionOrderStatus.PENDING_PAYMENT
                || order.getPaymentDueAt() == null) {
            return;
        }

        LocalDateTime now = time.now();
        if (now.isBefore(order.getPaymentDueAt())) return;

        order.setOrderStatus(AuctionOrderStatus.ABANDONED);
        order.setAbandonedAt(now);
        order.setUpdatedAt(now);
        orders.save(order);
        emails.ensureAbandonedEmailLogs(order);
        privateNotificationEvents.publishOrderAbandoned(
                order.getAuctionId(),
                order.getAuctionOrderId(),
                order.getOrderNumber(),
                order.getBuyerId(),
                order.getSellerId(),
                now
        );
        events.publishEvent(new AuctionEmailReadyEvent(order.getAuctionId()));
    }
}
