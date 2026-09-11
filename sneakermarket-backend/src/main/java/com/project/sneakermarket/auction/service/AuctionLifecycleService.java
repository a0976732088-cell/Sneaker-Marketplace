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

import lombok.RequiredArgsConstructor;

/** 單場競標的生命週期交易；與出價、編輯、取消競爭同一筆 Auction lock。 */
@Service
@RequiredArgsConstructor
public class AuctionLifecycleService {

    private final AuctionRepository auctionRepository;
    private final AuctionResultRepository auctionResultRepository;
    private final AuctionTime auctionTime;

    @Transactional
    public void processAuction(Long auctionId) {
        // 第一個 DB 操作即鎖定 Auction，不能使用掃描時的舊快照判定。
        Auction auction = auctionRepository.findByIdForUpdate(auctionId).orElse(null);
        if (auction == null || (auction.getStatus() != AuctionStatus.SCHEDULED
                && auction.getStatus() != AuctionStatus.ACTIVE)) {
            return;
        }

        LocalDateTime now = auctionTime.now();
        if (!auction.getEndAt().isAfter(now)) {
            closeAuction(auction, now);
        } else if (auction.getStatus() == AuctionStatus.SCHEDULED
                && !auction.getStartAt().isAfter(now)) {
            auction.setStatus(AuctionStatus.ACTIVE);
            auction.setUpdatedAt(now);
            auctionRepository.save(auction);
        }
    }

    // 已在公開方法的交易與鎖內；此 helper 不建立另一個交易。
    private void closeAuction(Auction auction, LocalDateTime now) {
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

        AuctionResult result = new AuctionResult();
        result.setAuctionId(auction.getAuctionId());
        result.setWinnerId(sold ? auction.getHighestBidderId() : null);
        result.setFinalPrice(sold ? auction.getCurrentPrice() : null);
        result.setResultStatus(sold ? AuctionResultStatus.SOLD : AuctionResultStatus.UNSOLD);
        result.setEndedAt(now);
        result.setReason(sold ? null : "截止時無成功出價");
        result.setCreatedAt(now);

        auction.setStatus(sold ? AuctionStatus.SOLD : AuctionStatus.UNSOLD);
        auction.setUpdatedAt(now);
        auctionRepository.save(auction);
        // UNIQUE(auction_id) 為最後防線；寫入或提交失敗時 Auction 一併回滾。
        auctionResultRepository.save(result);
    }
}
