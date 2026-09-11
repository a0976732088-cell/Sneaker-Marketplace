package com.project.sneakermarket.auction.scheduler;

import com.project.sneakermarket.auction.email.*;
import com.project.sneakermarket.auction.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "auction.email.enabled", havingValue = "true")
public class AuctionEmailRecoveryScheduler {
    private final AuctionResultRepository results;
    private final AuctionEmailLogRepository logs;
    private final AuctionEmailService emails;
    private final AuctionEmailDispatchService dispatch;
    private final AuctionEmailSettings settings;

    @Scheduled(cron = "${auction.email.cron:*/30 * * * * *}", zone = "Asia/Taipei")
    public void recoverEmails() {
        if (!settings.enabled()) return;
        // 掃描本身與每筆處理各自隔離，補漏失敗仍可重試既有工作。
        try {
            for (Long auctionId : results.findEmailReadinessCandidateIds()) {
                try {
                    emails.ensureEmailLogsForAuction(auctionId);
                } catch (RuntimeException ex) {
                    log.error("Auction email recovery failed: auctionId={}", auctionId);
                }
            }
        } catch (RuntimeException ex) {
            log.error("Auction email readiness scan failed");
        }
        try {
            for (Long id : logs.findDispatchCandidateIds(null, settings.maxRetries())) {
                try {
                    dispatch.sendEmailLog(id);
                } catch (RuntimeException ex) {
                    log.error("Auction email retry transaction failed: emailLogId={}", id);
                }
            }
        } catch (RuntimeException ex) {
            log.error("Auction email retry scan failed");
        }
    }
}
