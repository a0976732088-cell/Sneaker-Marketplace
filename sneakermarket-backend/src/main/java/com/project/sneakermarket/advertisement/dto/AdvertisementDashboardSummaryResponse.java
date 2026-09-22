package com.project.sneakermarket.advertisement.dto;

import java.math.BigDecimal;

// 回傳整個後台摘要
public record AdvertisementDashboardSummaryResponse (
        long activeAdvertisements,
        long views,
        long clicks,
        BigDecimal ctr
) {
}
