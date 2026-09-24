package com.project.sneakermarket.advertisement.service;

import com.project.sneakermarket.advertisement.dto.AdvertisementStatsResponse;
import com.project.sneakermarket.advertisement.repository.AdvertisementRepository;
import com.project.sneakermarket.advertisement.repository.AdvertisementStatsRepository;
import com.project.sneakermarket.advertisement.repository.AdvertisementStatsRepository.AdvertisementStatsTotalProjection;
import com.project.sneakermarket.advertisement.dto.AdvertisementDashboardSummaryResponse;
import com.project.sneakermarket.advertisement.entity.AdvertisementStatus;
import com.project.sneakermarket.advertisement.repository.AdvertisementStatsRepository.AdvertisementStatsSummaryProjection;
import com.project.sneakermarket.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpSession;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdvertisementStatsService {

    // 保證統計日期依台灣時間計算
    private static final ZoneId TAIPEI_ZONE =
            ZoneId.of("Asia/Taipei");

    private final AdvertisementRepository advertisementRepository;
    private final AdvertisementStatsRepository advertisementStatsRepository;
    // Session 裡保存廣告追蹤狀態時使用的名稱
    private static final String TRACKING_STATE_SESSION_KEY =
            "advertisementTrackingState";

    /**
     * 同一個 Session 當天已經曝光、點擊過哪些廣告。
     * <p>
     * 日期改變時會建立新的狀態，讓隔天可以重新計算。
     */
    private static final class AdvertisementTrackingState
            implements Serializable {

        private final LocalDate statDate;

        private final Set<Long> viewedAdIds = new HashSet<>();

        private final Set<Long> clickedAdIds = new HashSet<>();

        private AdvertisementTrackingState(LocalDate statDate) {
            this.statDate = statDate;
        }
    }

    @Transactional
    public void recordView(Long adId, HttpSession session) {
        /*
         * 同一個 Session 可能同時送出多個請求，
         * synchronized 可以避免兩個請求同時通過去重檢查。
         */
        synchronized (session) {
            AdvertisementTrackingState trackingState =
                    getOrCreateTrackingState(session);

            // 今天這個 Session 已看過此廣告，不重複計算
            if (trackingState.viewedAdIds.contains(adId)) {
                return;
            }

            incrementTodayStats(adId, 1L, 0L);

            // 資料庫操作成功後，再記錄到 Session
            trackingState.viewedAdIds.add(adId);
        }
    }

    @Transactional
    public void recordClick(Long adId, HttpSession session) {
        synchronized (session) {
            AdvertisementTrackingState trackingState =
                    getOrCreateTrackingState(session);

            // 今天這個 Session 已點擊過此廣告，不重複計算
            if (trackingState.clickedAdIds.contains(adId)) {
                return;
            }

            /*
             * 使用者可能在廣告顯示未滿一秒時就點擊。
             * 如果尚未計算曝光，點擊時補上一筆曝光，
             * 確保 clickCount 不會大於 viewCount。
             */
            boolean needsView =
                    !trackingState.viewedAdIds.contains(adId);

            incrementTodayStats(adId, needsView ? 1L : 0L, 1L);

            if (needsView) {
                trackingState.viewedAdIds.add(adId);
            }

            trackingState.clickedAdIds.add(adId);
        }
    }

    // 批次查詢方法
    @Transactional(readOnly = true)
    public List<AdvertisementStatsResponse> getTotalsByAdvertisementIds(
            Collection<Long> adIds
    ) {
        List<Long> uniqueAdIds = adIds == null
                ? List.of()
                : adIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (uniqueAdIds.isEmpty()) {
            return List.of();
        }

        Map<Long, AdvertisementStatsTotalProjection> totalsByAdId =
                advertisementStatsRepository
                        .findTotalsByAdvertisementIds(uniqueAdIds)
                        .stream()
                        .collect(Collectors.toMap(
                                AdvertisementStatsTotalProjection::getAdId,
                                Function.identity()
                        ));

        return uniqueAdIds.stream()
                .map(adId -> {
                    AdvertisementStatsTotalProjection totals =
                            totalsByAdId.get(adId);

                    long views = totals == null
                            ? 0L
                            : totals.getViews();

                    long clicks = totals == null
                            ? 0L
                            : totals.getClicks();

                    return new AdvertisementStatsResponse(
                            adId,
                            views,
                            clicks,
                            calculateCtr(views, clicks)
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public AdvertisementDashboardSummaryResponse getDashboardSummary() {
        LocalDate today = LocalDate.now(TAIPEI_ZONE);

        // 例如9月1號到10月1號
        LocalDate startDate = today.withDayOfMonth(1);
        LocalDate endDate = startDate.plusMonths(1);

        LocalDateTime now = LocalDateTime.now(TAIPEI_ZONE);

        long activeAdvertisements =
                advertisementRepository
                        .countByAdStatusAndStartAtLessThanEqualAndEndAtAfter(
                                AdvertisementStatus.ACTIVE,
                                now,
                                now
                        );

        AdvertisementStatsSummaryProjection summary =
                advertisementStatsRepository.findSummaryByDateRange(
                        startDate,
                        endDate
                );

        long views = summary.getViews();
        long clicks = summary.getClicks();

        return new AdvertisementDashboardSummaryResponse(
                activeAdvertisements,
                views,
                clicks,
                calculateCtr(views, clicks)
        );
    }

    /**
     * 取得今天的廣告追蹤狀態。
     * 如果 Session 沒有資料，或日期已改變，就建立新的狀態。
     */
    private AdvertisementTrackingState getOrCreateTrackingState(
            HttpSession session
    ) {
        LocalDate today = LocalDate.now(TAIPEI_ZONE);

        Object savedState =
                session.getAttribute(TRACKING_STATE_SESSION_KEY);

        if (savedState instanceof AdvertisementTrackingState trackingState
                && today.equals(trackingState.statDate)) {
            return trackingState;
        }

        AdvertisementTrackingState newState =
                new AdvertisementTrackingState(today);

        session.setAttribute(
                TRACKING_STATE_SESSION_KEY,
                newState
        );

        return newState;
    }

    private void incrementTodayStats(
            Long adId,
            Long viewDelta,
            Long clickDelta
    ) {
        if (!advertisementRepository.existsById(adId)) {
            throw new BusinessException("找不到指定的廣告");
        }

        LocalDate statDate = LocalDate.now(TAIPEI_ZONE);
        LocalDateTime updatedAt = LocalDateTime.now(TAIPEI_ZONE);

        advertisementStatsRepository.incrementOrCreate(
                adId, statDate, viewDelta, clickDelta, updatedAt
        );
    }

    // 計算CTR = (點擊/曝光) x 100%
    private BigDecimal calculateCtr(long views, long clicks) {
        if (views <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        // 方法鏈式呼叫
        return BigDecimal.valueOf(clicks)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(views),
                        2,
                        RoundingMode.HALF_UP
                );
    }
}