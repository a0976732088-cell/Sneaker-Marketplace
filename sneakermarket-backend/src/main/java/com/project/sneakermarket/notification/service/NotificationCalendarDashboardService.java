package com.project.sneakermarket.notification.service;

import com.project.sneakermarket.calendar.repository.CalendarEventRepository;
import com.project.sneakermarket.calendar.repository.EventFollowRepository;
import com.project.sneakermarket.notification.dto.NotificationCalendarDashboardDTO;
import com.project.sneakermarket.notification.entity.NotificationJobStatus;
import com.project.sneakermarket.notification.repository.NotificationLogRepository;
import com.project.sneakermarket.notification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationCalendarDashboardService {

    private static final ZoneId TAIPEI_ZONE =
            ZoneId.of("Asia/Taipei");

    private final CalendarEventRepository calendarEventRepository;
    private final EventFollowRepository eventFollowRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationLogRepository notificationLogRepository;

    @Transactional(readOnly = true)
    public NotificationCalendarDashboardDTO getDashboard() {

        LocalDateTime now =
                LocalDateTime.now(TAIPEI_ZONE);

        NotificationCalendarDashboardDTO dto =
                new NotificationCalendarDashboardDTO();

        /*
         * 行事曆統計
         */

        dto.setTotalEvents(
                calendarEventRepository.count()
        );

        dto.setUpcomingEvents(
                calendarEventRepository
                        .countByStartAtAfterAndStatusNotIn(
                                now,
                                List.of(
                                        "INACTIVE",
                                        "DRAFT",
                                        "CANCELLED"
                                )
                        )
        );

        dto.setCancelledEvents(
                calendarEventRepository
                        .countByStatus("CANCELLED")
        );

        dto.setTotalFollows(
                eventFollowRepository.count()
        );

        /*
         * 通知統計
         */

        dto.setTotalNotifications(
                notificationRepository.count()
        );

        Long totalSent =
                notificationLogRepository
                        .sumTotalSentByStatus(
                                NotificationJobStatus.COMPLETED
                        );

        dto.setTotalSent(
                totalSent != null ? totalSent : 0L
        );

        dto.setPendingJobs(
                notificationLogRepository.countByStatus(
                        NotificationJobStatus.PENDING
                )
        );

        dto.setProcessingJobs(
                notificationLogRepository.countByStatus(
                        NotificationJobStatus.PROCESSING
                )
        );

        dto.setCompletedJobs(
                notificationLogRepository.countByStatus(
                        NotificationJobStatus.COMPLETED
                )
        );

        dto.setFailedJobs(
                notificationLogRepository.countByStatus(
                        NotificationJobStatus.FAILED
                )
        );

        return dto;
    }
}