package com.project.sneakermarket.auction.dto;

import java.util.List;

/**
 * 分頁資料 Response DTO。
 * <p>
 * 用來統一回傳競標列表的資料內容、
 * 目前頁數與總資料筆數等分頁資訊。
 */
public record PageData<T>(

        // 目前頁面的資料
        List<T> items,

        // 目前頁碼，從 0 開始
        int page,

        // 每頁資料筆數
        int size,

        // 符合條件的總資料筆數
        long totalElements,

        // 總頁數
        int totalPages

) {
}