package com.project.sneakermarket.auction.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionResult;
import com.project.sneakermarket.auction.entity.AuctionResultStatus;
import com.project.sneakermarket.auction.entity.AuctionStatus;
import com.project.sneakermarket.auction.repository.AuctionRepository;
import com.project.sneakermarket.auction.repository.AuctionResultRepository;
import com.project.sneakermarket.auction.realtime.AuctionRealtimeEventPublisher;
import com.project.sneakermarket.auction.realtime.AuctionRealtimeEventType;

import lombok.RequiredArgsConstructor;

/** 單場競標的生命週期交易；與出價、編輯、取消競爭同一筆 Auction lock。 */
@Service
@RequiredArgsConstructor
public class AuctionLifecycleService {

    private final AuctionRepository auctionRepository;
    private final AuctionResultRepository auctionResultRepository;
    private final AuctionTime auctionTime;
    private final org.springframework.context.ApplicationEventPublisher events;
    private final AuctionRealtimeEventPublisher realtimeEvents;

    @Transactional
    public void processAuction(Long auctionId) {
        // 第一個 DB 操作即鎖定 Auction，不能使用掃描時的舊快照判定。
        Auction auction = auctionRepository.findByIdForUpdate(auctionId).orElse(null);
        if (auction == null || auction.getDeletedAt() != null
                || (auction.getStatus() != AuctionStatus.SCHEDULED
                && auction.getStatus() != AuctionStatus.ACTIVE)) {
            return;
        }

        LocalDateTime now = auctionTime.now();
        if (!auction.getEndAt().isAfter(now)) {
            closeAuction(auction, now);
            return;
        }

        // 下架只停止公開與交易操作；下架中的 SCHEDULED 不可自動轉為 ACTIVE。
        if (auction.getRemovedAt() != null) return;

        if (auction.getStatus() == AuctionStatus.SCHEDULED
                && !auction.getStartAt().isAfter(now)) {
            auction.setStatus(AuctionStatus.ACTIVE);
            auction.setUpdatedAt(now);
            auctionRepository.save(auction);
            realtimeEvents.publish(AuctionRealtimeEventType.AUCTION_STARTED, auction);
        }
    }

    // 已在公開方法的交易與鎖內；此 helper 不建立另一個交易。
    private void closeAuction(Auction auction, LocalDateTime now) {
        // 終局狀態通常會擋下重試；這層也避免既有異常資料重複建立 Result。
        if (auctionResultRepository.findByAuctionId(auction.getAuctionId()).isPresent()) return;

        if (auction.getRemovedAt() != null) {
            AuctionResult result = result(
                    auction, AuctionResultStatus.REMOVED, null, null, now,
                    "競標於平台下架期間到達截止時間，判定廢標");
            auction.setStatus(AuctionStatus.REMOVED);
            auction.setUpdatedAt(now);
            auctionRepository.save(auction);
            auctionResultRepository.save(result);
            realtimeEvents.publish(AuctionRealtimeEventType.AUCTION_REMOVED, auction);
            // 廢標不發布既有 SOLD／UNSOLD Email 工作事件。
            return;
        }

        Integer bidCount = auction.getBidCount();
        boolean sold = bidCount != null && bidCount > 0 && auction.getHighestBidderId() != null;
        boolean unsold = Integer.valueOf(0).equals(bidCount) && auction.getHighestBidderId() == null;
        if ((!sold && !unsold) || (sold && (auction.getCurrentPrice() == null
                || auction.getCurrentPrice().signum() <= 0))) {
            // 不猜測得標者；本筆回滾，由 Scheduler 記錄並繼續。
            throw new IllegalStateException("Inconsistent auction closing data: auctionId="
                    + auction.getAuctionId() + ", bidCount=" + bidCount
                    + ", hasHighestBidder=" + (auction.getHighestBidderId() != null));
        }

        AuctionResult result = result(
                auction,
                sold ? AuctionResultStatus.SOLD : AuctionResultStatus.UNSOLD,
                sold ? auction.getHighestBidderId() : null,
                sold ? auction.getCurrentPrice() : null,
                now,
                sold ? null : "截止時無成功出價");

        auction.setStatus(sold ? AuctionStatus.SOLD : AuctionStatus.UNSOLD);
        auction.setUpdatedAt(now);
        auctionRepository.save(auction);
        // UNIQUE(auction_id) 為最後防線；寫入或提交失敗時 Auction 一併回滾。
        auctionResultRepository.save(result);
        realtimeEvents.publish(
                sold ? AuctionRealtimeEventType.AUCTION_SOLD : AuctionRealtimeEventType.AUCTION_UNSOLD,
                auction
        );
        events.publishEvent(new com.project.sneakermarket.auction.email.AuctionEmailReadyEvent(auction.getAuctionId()));
    }

    private AuctionResult result(Auction auction, AuctionResultStatus status, Long winnerId,
            java.math.BigDecimal finalPrice, LocalDateTime now, String reason) {
        AuctionResult result = new AuctionResult();
        result.setAuctionId(auction.getAuctionId());
        result.setWinnerId(winnerId);
        result.setFinalPrice(finalPrice);
        result.setResultStatus(status);
        result.setEndedAt(now);
        result.setReason(reason);
        result.setCreatedAt(now);
        return result;
    }
}
