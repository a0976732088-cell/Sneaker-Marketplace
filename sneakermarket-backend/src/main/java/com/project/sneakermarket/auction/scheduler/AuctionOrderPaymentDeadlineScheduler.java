package com.project.sneakermarket.auction.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.project.sneakermarket.auction.repository.AuctionOrderRepository;
import com.project.sneakermarket.auction.service.AuctionOrderPaymentDeadlineService;
import com.project.sneakermarket.auction.service.AuctionTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "auction.order.enabled", havingValue = "true", matchIfMissing = true)
public class AuctionOrderPaymentDeadlineScheduler {
    private final AuctionOrderRepository orders;
    private final AuctionOrderPaymentDeadlineService deadlines;
    private final AuctionTime time;

    @Scheduled(cron = "${auction.order.payment-deadline.cron:*/10 * * * * *}", zone = "Asia/Taipei")
    public void expireOverdueOrders() {
        for (Long orderId : orders.findPaymentDeadlineCandidateIds(time.now())) {
            try {
                deadlines.expireOverdueOrder(orderId);
            } catch (RuntimeException ex) {
                log.error("Auction order payment deadline failed: auctionOrderId={}", orderId, ex);
            }
        }
    }
}
