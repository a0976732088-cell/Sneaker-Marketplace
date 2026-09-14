package com.project.sneakermarket.auction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 競標出價紀錄 Response DTO。
 * <p>
 * 用來回傳競標詳細頁中的公開出價紀錄。
 * 不直接回傳 bidderId、會員帳號或 Email，
 * 避免暴露會員識別資訊。
 */
public record BidHistoryResponse(

        // 成功出價金額
        BigDecimal bidAmount,

        // 出價成功時間
        LocalDateTime createdAt,

        // 出價會員名稱
        String bidderName

) {
}