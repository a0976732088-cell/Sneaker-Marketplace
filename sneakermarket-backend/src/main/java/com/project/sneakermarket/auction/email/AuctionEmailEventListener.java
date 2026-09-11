package com.project.sneakermarket.auction.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionEmailEventListener {
    private final AuctionEmailService emails;
    private final AuctionEmailDispatchService dispatch;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionEmailReady(AuctionEmailReadyEvent event) {
        try {
            for (Long id : emails.ensureEmailLogsForAuction(event.auctionId())) {
                try {
                    dispatch.sendEmailLog(id);
                } catch (RuntimeException ex) {
                    log.error("Auction email dispatch transaction failed: emailLogId={}", id);
                }
            }
        } catch (RuntimeException ex) {
            // 原主交易已提交；交由 Recovery 補漏，不把副作用錯誤傳回 Lifecycle/Order。
            log.error("Auction email readiness failed: auctionId={}", event.auctionId());
        }
    }
}
