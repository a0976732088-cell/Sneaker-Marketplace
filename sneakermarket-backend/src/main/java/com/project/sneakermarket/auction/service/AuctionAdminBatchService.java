package com.project.sneakermarket.auction.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Service;

import com.project.sneakermarket.auction.dto.AuctionAdminBatchItemResponse;
import com.project.sneakermarket.auction.dto.AuctionAdminBatchResponse;
import com.project.sneakermarket.auction.exception.AuctionConflictException;
import com.project.sneakermarket.common.exception.BusinessException;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/** 外層不開交易；每筆呼叫另一個 Spring Bean 的 REQUIRES_NEW 方法。 */
@Service
@RequiredArgsConstructor
public class AuctionAdminBatchService {
    private final AuctionAdminMutationService mutations;

    public AuctionAdminBatchResponse removeAuctions(List<Long> auctionIds, Long adminId, String reason) {
        return process(auctionIds, id -> mutations.removeAuction(id, adminId, reason), "下架成功");
    }

    public AuctionAdminBatchResponse restoreAuctions(List<Long> auctionIds, Long adminId) {
        return process(auctionIds, id -> mutations.restoreAuction(id, adminId), "恢復顯示成功");
    }

    public AuctionAdminBatchResponse deleteAuctions(List<Long> auctionIds, Long adminId, String reason) {
        return process(auctionIds, id -> mutations.deleteAuction(id, adminId, reason), "刪除成功");
    }

    private AuctionAdminBatchResponse process(List<Long> auctionIds, AuctionOperation operation, String successMessage) {
        if (auctionIds == null || auctionIds.isEmpty()) throw new BusinessException("auctionIds 不可為空");
        if (auctionIds.size() > 100) throw new BusinessException("一次最多處理 100 筆競標");

        var distinctIds = new LinkedHashSet<>(auctionIds);
        List<AuctionAdminBatchItemResponse> items = new ArrayList<>();
        int successCount = 0;
        for (Long auctionId : distinctIds) {
            try {
                operation.execute(auctionId);
                items.add(new AuctionAdminBatchItemResponse(auctionId, true, successMessage));
                successCount++;
            } catch (ResourceNotFoundException | AuctionConflictException | BusinessException ex) {
                items.add(new AuctionAdminBatchItemResponse(auctionId, false, ex.getMessage()));
            } catch (RuntimeException ex) {
                items.add(new AuctionAdminBatchItemResponse(auctionId, false, "處理失敗"));
            }
        }
        return new AuctionAdminBatchResponse(
                items.size(), successCount, items.size() - successCount, List.copyOf(items));
    }

    @FunctionalInterface
    private interface AuctionOperation {
        void execute(Long auctionId);
    }
}
