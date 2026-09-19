package com.project.sneakermarket.advertisement.dto;

import java.math.BigDecimal;

public record AdvertisementStatsResponse(
        Long adId,
        long views,
        long clicks,
        BigDecimal ctr
) {
}
