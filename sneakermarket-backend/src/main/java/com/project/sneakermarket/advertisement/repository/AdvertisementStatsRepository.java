package com.project.sneakermarket.advertisement.repository;

import com.project.sneakermarket.advertisement.entity.AdvertisementStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AdvertisementStatsRepository extends JpaRepository<AdvertisementStats, Long> {
    // 查詢有無資料
    Optional<AdvertisementStats> findByAdvertisementAdIdAndStatDate(Long adId, LocalDate statDate);

    // 一次查詢多則廣告，不用多筆資料還多次查詢
    @Query("""
        SELECT stats.advertisement.adId AS adId,
               COALESCE(SUM(stats.viewCount), 0) AS views,
               COALESCE(SUM(stats.clickCount), 0) AS clicks
        FROM AdvertisementStats stats
        WHERE stats.advertisement.adId IN :adIds
        GROUP BY stats.advertisement.adId
        """)
    List<AdvertisementStatsTotalProjection> findTotalsByAdvertisementIds(
            @Param("adIds") Collection<Long> adIds
    );

    // 日期區間總計查詢
    @Query("""
    SELECT COALESCE(SUM(stats.viewCount), 0) AS views,
           COALESCE(SUM(stats.clickCount), 0) AS clicks
    FROM AdvertisementStats stats
    WHERE stats.statDate >= :startDate
      AND stats.statDate < :endDate
    """)
    AdvertisementStatsSummaryProjection findSummaryByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Repository內部查詢結果形狀，與JPQL別名對應
    interface AdvertisementStatsTotalProjection {

        Long getAdId();

        Long getViews();

        Long getClicks();
    }

    interface AdvertisementStatsSummaryProjection {

        Long getViews();

        Long getClicks();
    }
}
