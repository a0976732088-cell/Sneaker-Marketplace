package com.project.sneakermarket.auction.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.project.sneakermarket.auction.entity.Auction;
import com.project.sneakermarket.auction.entity.AuctionStatus;
import com.project.sneakermarket.auction.exception.AuctionConflictException;
import com.project.sneakermarket.auction.exception.AuctionUnauthorizedException;
import com.project.sneakermarket.auction.repository.AuctionRepository;
import com.project.sneakermarket.auction.realtime.AuctionRealtimeEventPublisher;
import com.project.sneakermarket.auction.realtime.AuctionRealtimeEventType;
import com.project.sneakermarket.common.exception.BusinessException;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/** 單場管理操作；batch 必須從另一個 Bean 呼叫，確保每筆各自提交或回滾。 */
@Service
@RequiredArgsConstructor
public class AuctionAdminMutationService {
    private final AuctionRepository auctions;
    private final AuctionTime time;
    private final AuctionRealtimeEventPublisher realtimeEvents;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeAuction(Long auctionId, Long adminId, String reason) {
        validateActor(adminId);
        String normalizedReason = normalizeReason(reason);

        // 第一個 Auction DB read 必須取得與 Bid/Command/Lifecycle 相同的寫入鎖。
        Auction auction = auctions.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到指定的競標資料"));

        // 已有正式 metadata 時為冪等成功，不覆寫原執行者、原因或時間。
        if (auction.getRemovedAt() != null) return;

        LocalDateTime now = time.now();
        auction.setRemovedAt(now);
        auction.setRemovedByMemberId(adminId);
        auction.setRemoveReason(normalizedReason);
        auction.setRemovedFromStatus(auction.getStatus());
        auction.setUpdatedAt(now);
        auctions.save(auction);
        realtimeEvents.publish(AuctionRealtimeEventType.AUCTION_REMOVED, auction);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restoreAuction(Long auctionId, Long adminId) {
        validateActor(adminId);

        // 第一個 Auction DB read 必須取得與 Bid/Command/Lifecycle/Delete 相同的寫入鎖。
        Auction auction = auctions.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到指定的競標資料"));

        if (auction.getDeletedAt() != null) {
            throw new AuctionConflictException("已刪除的競標無法重新上架");
        }
        // REMOVED 是 Lifecycle 判定的廢標終局狀態；legacy REMOVED 也不得復活。
        if (auction.getStatus() == AuctionStatus.REMOVED) {
            throw new AuctionConflictException("廢標競標無法重新上架");
        }
        // 已公開資料重複 restore 採冪等成功。
        if (auction.getRemovedAt() == null) return;

        LocalDateTime now = time.now();
        switch (auction.getStatus()) {
            case DRAFT -> {
                // 草稿僅解除管理員下架 metadata。
            }
            case SCHEDULED -> {
                if (!now.isBefore(auction.getEndAt())) throw expiredRestoreConflict();
                if (!now.isBefore(auction.getStartAt())) auction.setStatus(AuctionStatus.ACTIVE);
            }
            case ACTIVE -> {
                if (!now.isBefore(auction.getEndAt())) throw expiredRestoreConflict();
            }
            case SOLD, UNSOLD, CANCELLED ->
                throw new AuctionConflictException("已結標或終止的競標無法重新上架");
            case REMOVED -> throw new AuctionConflictException("廢標競標無法重新上架");
        }
        auction.setRemovedAt(null);
        auction.setRemovedByMemberId(null);
        auction.setRemoveReason(null);
        auction.setRemovedFromStatus(null);
        auction.setUpdatedAt(now);
        auctions.save(auction);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteAuction(Long auctionId, Long adminId, String reason) {
        validateActor(adminId);
        String normalizedReason = normalizeReason(reason);

        Auction auction = auctions.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到指定的競標資料"));

        // 重複 delete 採冪等成功，且不覆寫第一次刪除的 metadata。
        if (auction.getDeletedAt() != null) return;
        if (auction.getRemovedAt() == null) {
            throw new AuctionConflictException("競標必須先下架才能刪除");
        }

        LocalDateTime now = time.now();
        auction.setDeletedAt(now);
        auction.setDeletedByMemberId(adminId);
        auction.setDeleteReason(normalizedReason);
        auction.setUpdatedAt(now);
        auctions.save(auction);
    }

    private void validateActor(Long adminId) {
        if (adminId == null || adminId <= 0) throw new AuctionUnauthorizedException();
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) throw new BusinessException("管理原因不可空白");
        String normalized = reason.trim();
        if (normalized.length() > 500) throw new BusinessException("管理原因不可超過 500 字");
        return normalized;
    }

    private AuctionConflictException expiredRestoreConflict() {
        return new AuctionConflictException("競標下架期間已超過原截止時間，無法直接重新上架");
    }
}
