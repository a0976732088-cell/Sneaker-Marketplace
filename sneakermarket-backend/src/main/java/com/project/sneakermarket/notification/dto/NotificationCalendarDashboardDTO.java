package com.project.sneakermarket.notification.dto;

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
}