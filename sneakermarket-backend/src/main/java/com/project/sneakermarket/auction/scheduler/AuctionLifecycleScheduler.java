package com.project.sneakermarket.auction.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.project.sneakermarket.auction.repository.AuctionRepository;
import com.project.sneakermarket.auction.service.AuctionLifecycleService;
import com.project.sneakermarket.auction.service.AuctionTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "auction.lifecycle.enabled", havingValue = "true", matchIfMissing = true)
public class AuctionLifecycleScheduler {

    private final AuctionRepository auctionRepository;
    private final AuctionLifecycleService auctionLifecycleService;
    private final AuctionTime auctionTime;

    @Scheduled(cron = "${auction.lifecycle.cron:*/10 * * * * *}", zone = "Asia/Taipei")
    public void updateAuctionLifecycle() {
        // 不開整批交易；另一個 Bean 的每次呼叫各自提交或回滾。
        for (Long auctionId : auctionRepository.findLifecycleCandidateIds(auctionTime.now())) {
            try {
                auctionLifecycleService.processAuction(auctionId);
            } catch (RuntimeException ex) {
                log.error("Auction lifecycle failed: auctionId={}", auctionId, ex);
            }
        }
    }
}
