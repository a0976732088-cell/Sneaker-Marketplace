package com.project.sneakermarket.notification.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationCalendarDashboardDTO {

    // 行事曆統計
    private Long totalEvents;
    private Long upcomingEvents;
    private Long cancelledEvents;
    private Long totalFollows;

    // 通知統計
    private Long totalNotifications;
    private Long totalSent;
    private Long pendingJobs;
    private Long processingJobs;
    private Long completedJobs;
    private Long failedJobs;

    // KPI
    private Double deliveryRate;

    // 活動狀態環形圖
    private Long activeEvents;
    private Long endedEvents;
    private Long draftEvents;

    // 最近 30 天趨勢
    private List<TrendPoint> activityTrend;

    // 最近四週通知成效
    private List<DeliveryPoint> notificationDeliveryTrend;

    // 近七天排程
    private List<ScheduleDay> upcomingSchedule;

    @Getter
    @Setter
    public static class TrendPoint {
        private LocalDate date;
        private Long eventCount;
        private Long followCount;
    }

    @Getter
    @Setter
    public static class DeliveryPoint {
        private String label;
        private Long completed;
        private Long processing;
        private Long failed;
    }

    @Getter
    @Setter
    public static class ScheduleDay {
        private LocalDate date;
        private List<ScheduleItem> items;
    }

    @Getter
    @Setter
    public static class ScheduleItem {
        private String type;
        private Long id;
        private String title;
        private LocalDateTime scheduledAt;
    }
}