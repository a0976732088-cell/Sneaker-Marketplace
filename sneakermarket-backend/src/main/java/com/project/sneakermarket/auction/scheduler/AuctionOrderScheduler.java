package com.project.sneakermarket.auction.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.project.sneakermarket.auction.repository.AuctionResultRepository;
import com.project.sneakermarket.auction.service.AuctionOrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "auction.order.enabled", havingValue = "true", matchIfMissing = true)
public class AuctionOrderScheduler {
    private final AuctionResultRepository results;
    private final AuctionOrderService orders;

    @Scheduled(cron = "${auction.order.cron:*/10 * * * * *}", zone = "Asia/Taipei")
    public void createAuctionOrders() {
        // 不開整批交易；只消費已提交的 SOLD Result，每次 Service 呼叫獨立提交。
        for (Long auctionId : results.findOrderCandidateIds()) {
            try {
                orders.createOrderForAuction(auctionId);
            } catch (RuntimeException ex) {
                log.error("Auction order failed: auctionId={}", auctionId, ex);
            }
        }
    }
}
