package com.project.sneakermarket.advertisement.repository;

import com.project.sneakermarket.advertisement.entity.AdvertisementStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;

public interface AdvertisementStatsRepository extends JpaRepository<AdvertisementStats, Long> {

    @SuppressWarnings({
            "SqlNoDataSourceInspection",
            "SqlDialectInspection",
            "SqlResolve"
    })
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    UPDATE dbo.advertisement_stats WITH (UPDLOCK, HOLDLOCK)
                    SET view_count = view_count + :viewDelta,
                        click_count = click_count + :clickDelta,
                        updated_at = :updatedAt
                    WHERE ad_id = :adId
                      AND stat_date = :statDate;
                    IF @@ROWCOUNT = 0
                    BEGIN
                        INSERT INTO dbo.advertisement_stats (
                            ad_id,
                            stat_date,
                            view_count,
                            click_count,
                            updated_at
                        )
                        VALUES (
                            :adId,
                            :statDate,
                            :viewDelta,
                            :clickDelta,
                            :updatedAt
                        );
                    END
                    """,
            nativeQuery = true
    )
    void incrementOrCreate(
            @Param("adId") Long adId,
            @Param("statDate") LocalDate statDate,
            @Param("viewDelta") Long viewDelta,
            @Param("clickDelta") Long clickDelta,
            @Param("updatedAt") LocalDateTime updatedAt
    );

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
