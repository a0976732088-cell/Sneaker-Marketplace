package com.project.sneakermarket.auction.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.sneakermarket.auction.dto.AuctionStatusResponse;
import com.project.sneakermarket.auction.dto.BidCreateRequest;
import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionBid;
import com.project.sneakermarket.auction.entity.AuctionProduct;
import com.project.sneakermarket.auction.entity.AuctionStatus;
import com.project.sneakermarket.auction.exception.AuctionConflictException;
import com.project.sneakermarket.auction.exception.AuctionUnauthorizedException;
import com.project.sneakermarket.auction.repository.AuctionBidRepository;
import com.project.sneakermarket.auction.repository.AuctionProductRepository;
import com.project.sneakermarket.auction.repository.AuctionRepository;
import com.project.sneakermarket.common.exception.BusinessException;
import com.project.sneakermarket.common.exception.ForbiddenException;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * 競標出價寫入 Service。
 *
 * 每次出價都先鎖定 Auction row，再依最新資料重新驗證並於同一交易內
 * 新增出價紀錄及更新競標狀態。
 */
@Service
@RequiredArgsConstructor
public class AuctionBidService {

    private final AuctionRepository auctionRepository;
    private final AuctionProductRepository auctionProductRepository;
    private final AuctionBidRepository auctionBidRepository;
    private final AuctionTime auctionTime;

    /**
     * 對指定競標出價。
     *
     * @param auctionId 競標 ID
     * @param bidderId 由有效 HttpSession 解析出的會員 ID
     * @param request 僅包含出價金額的 Request
     * @return 出價成功後的最新公開競標狀態
     */
    @Transactional
    public AuctionStatusResponse placeBid(
            Long auctionId,
            Long bidderId,
            BidCreateRequest request) {

        if (bidderId == null || bidderId <= 0) {
            throw new AuctionUnauthorizedException();
        }

        /*
         * 第一個資料庫讀取即取得悲觀寫入鎖。
         * 後續所有規則都以等待 Lock 後取得的最新 Auction 資料判定。
         */
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("找不到指定的競標資料")
                );

        AuctionProduct product = auctionProductRepository
                .findById(auction.getAuctionProductId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("找不到指定的競標商品資料")
                );

        if (bidderId.equals(product.getSellerId())) {
            throw new ForbiddenException("賣家不可對自己的競標出價");
        }

        if (auction.getRemovedAt() != null || auction.getDeletedAt() != null) {
            throw new AuctionConflictException("競標已下架，無法出價");
        }
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new AuctionConflictException("目前競標狀態不允許出價");
        }

        LocalDateTime now = auctionTime.now();

        if (auction.getStartAt().isAfter(now)) {
            throw new AuctionConflictException("競標尚未開始，無法出價");
        }

        if (!auction.getEndAt().isAfter(now)) {
            throw new AuctionConflictException("競標已截止，無法出價");
        }

        BigDecimal minimumBid = calculateNextMinimumBid(auction);
        BigDecimal bidAmount = request == null ? null : request.bidAmount();

        if (bidAmount == null || bidAmount.signum() <= 0) {
            throw new BusinessException("出價金額必須大於 0");
        }

        if (bidAmount.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException("競標金額僅接受整數新台幣");
        }

        if (bidAmount.compareTo(minimumBid) < 0) {
            throw new BusinessException(
                    "出價金額不足，目前最低可出價為 NT$ "
                            + formatTwdAmount(minimumBid)
            );
        }

        BigDecimal difference = bidAmount.subtract(auction.getCurrentPrice());

        if (difference.remainder(auction.getMinIncrement())
                .compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(
                    "出價金額必須以 NT$ "
                            + formatTwdAmount(auction.getMinIncrement())
                            + " 為加價級距，目前最低可出價為 NT$ "
                            + formatTwdAmount(minimumBid)
            );
        }

        AuctionBid bid = new AuctionBid();
        bid.setAuctionId(auction.getAuctionId());
        bid.setBidderId(bidderId);
        bid.setBidAmount(bidAmount);
        bid.setCreatedAt(now);
        auctionBidRepository.save(bid);

        int updatedBidCount = auction.getBidCount() + 1;
        auction.setCurrentPrice(bidAmount);
        auction.setHighestBidderId(bidderId);
        auction.setBidCount(updatedBidCount);
        auction.setUpdatedAt(now);
        auctionRepository.save(auction);

        return new AuctionStatusResponse(
                auction.getAuctionId(),
                auction.getCurrentPrice(),
                auction.getBidCount(),
                calculateNextMinimumBid(auction),
                auction.getStatus(),
                auction.getEndAt(),
                auction.getUpdatedAt()
        );
    }

    private BigDecimal calculateNextMinimumBid(Auction auction) {
        return auction.getCurrentPrice().add(auction.getMinIncrement());
    }

    private String formatTwdAmount(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }
}
