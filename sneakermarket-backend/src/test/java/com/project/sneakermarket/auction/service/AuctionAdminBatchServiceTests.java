package com.project.sneakermarket.auction.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.sneakermarket.auction.exception.AuctionConflictException;
import com.project.sneakermarket.common.exception.ResourceNotFoundException;

class AuctionAdminBatchServiceTests {
    private AuctionAdminMutationService mutations;
    private AuctionAdminBatchService batches;

    @BeforeEach
    void setup() {
        mutations = mock(AuctionAdminMutationService.class);
        batches = new AuctionAdminBatchService(mutations);
    }

    @Test
    void removeUsesDistinctIdsAndDoesNotRollbackSuccessfulItems() {
        doThrow(new ResourceNotFoundException("找不到指定的競標資料"))
                .when(mutations).removeAuction(12L, 7L, "原因");

        var result = batches.removeAuctions(List.of(11L, 12L, 11L, 13L), 7L, "原因");

        assertEquals(3, result.totalCount());
        assertEquals(2, result.successCount());
        assertEquals(1, result.failureCount());
        assertEquals(List.of(11L, 12L, 13L), result.items().stream().map(i -> i.auctionId()).toList());
        assertTrue(result.items().get(0).success());
        assertFalse(result.items().get(1).success());
        assertEquals("找不到指定的競標資料", result.items().get(1).message());
        assertTrue(result.items().get(2).success());
        verify(mutations).removeAuction(13L, 7L, "原因");
    }

    @Test
    void restoreAllItemsSuccessfully() {
        var result = batches.restoreAuctions(List.of(11L, 12L, 13L), 7L);

        assertEquals(3, result.totalCount());
        assertEquals(3, result.successCount());
        assertEquals(0, result.failureCount());
        assertTrue(result.items().stream().allMatch(item -> item.success()));
        assertTrue(result.items().stream().allMatch(item -> "恢復顯示成功".equals(item.message())));
    }

    @Test
    void restoreReturnsPerItemConflictAndContinues() {
        doThrow(new AuctionConflictException("已結標或終止的競標無法重新上架"))
                .when(mutations).restoreAuction(12L, 7L);

        var result = batches.restoreAuctions(List.of(11L, 12L, 13L), 7L);

        assertEquals(3, result.totalCount());
        assertEquals(2, result.successCount());
        assertEquals(1, result.failureCount());
        assertEquals("已結標或終止的競標無法重新上架", result.items().get(1).message());
        verify(mutations).restoreAuction(13L, 7L);
    }

    @Test
    void restoreUsesDistinctIdsInInputOrder() {
        var result = batches.restoreAuctions(List.of(11L, 12L, 11L, 13L, 12L), 7L);

        assertEquals(3, result.totalCount());
        assertEquals(List.of(11L, 12L, 13L), result.items().stream().map(i -> i.auctionId()).toList());
        verify(mutations, times(1)).restoreAuction(11L, 7L);
        verify(mutations, times(1)).restoreAuction(12L, 7L);
        verify(mutations, times(1)).restoreAuction(13L, 7L);
    }

    @Test
    void deleteReturnsPerItemConflictAndContinues() {
        doThrow(new AuctionConflictException("競標必須先下架才能刪除"))
                .when(mutations).deleteAuction(14L, 7L, "原因");

        var result = batches.deleteAuctions(List.of(13L, 14L, 15L), 7L, "原因");

        assertEquals(2, result.successCount());
        assertEquals(1, result.failureCount());
        assertEquals("競標必須先下架才能刪除", result.items().get(1).message());
        verify(mutations).deleteAuction(15L, 7L, "原因");
    }
}
