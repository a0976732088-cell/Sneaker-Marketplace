package com.project.sneakermarket.notification.service;

import com.project.sneakermarket.calendar.repository.CalendarEventRepository;
import com.project.sneakermarket.calendar.repository.EventFollowRepository;
import com.project.sneakermarket.notification.dto.NotificationCalendarDashboardDTO;
import com.project.sneakermarket.notification.entity.NotificationJobStatus;
import com.project.sneakermarket.notification.repository.NotificationLogRepository;
import com.project.sneakermarket.notification.repository.NotificationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationCalendarDashboardServiceTests {

    private CalendarEventRepository
            calendarEventRepository;

    private EventFollowRepository
            eventFollowRepository;

    private NotificationRepository
            notificationRepository;

    private NotificationLogRepository
            notificationLogRepository;

    private NotificationCalendarDashboardService
            service;

    @BeforeEach
    void setUp() {
        calendarEventRepository =
                mock(CalendarEventRepository.class);

        eventFollowRepository =
                mock(EventFollowRepository.class);

        notificationRepository =
                mock(NotificationRepository.class);

        notificationLogRepository =
                mock(NotificationLogRepository.class);

        service =
                new NotificationCalendarDashboardService(
                        calendarEventRepository,
                        eventFollowRepository,
                        notificationRepository,
                        notificationLogRepository
                );
    }

    @Test
    void dashboardReturnsCalendarAndNotificationStatistics() {
        when(
                calendarEventRepository.count()
        ).thenReturn(12L);

        when(
                calendarEventRepository
                        .countByStartAtAfterAndStatusNotIn(
                                any(LocalDateTime.class),
                                anyCollection()
                        )
        ).thenReturn(7L);

        when(
                calendarEventRepository
                        .countByStatus("CANCELLED")
        ).thenReturn(2L);

        when(
                eventFollowRepository.count()
        ).thenReturn(20L);

        when(
                notificationRepository.count()
        ).thenReturn(30L);

        when(
                notificationLogRepository
                        .sumTotalSentByStatus(
                                NotificationJobStatus.COMPLETED
                        )
        ).thenReturn(25L);

        when(
                notificationLogRepository.countByStatus(
                        NotificationJobStatus.PENDING
                )
        ).thenReturn(3L);

        when(
                notificationLogRepository.countByStatus(
                        NotificationJobStatus.PROCESSING
                )
        ).thenReturn(1L);

        when(
                notificationLogRepository.countByStatus(
                        NotificationJobStatus.COMPLETED
                )
        ).thenReturn(8L);

        when(
                notificationLogRepository.countByStatus(
                        NotificationJobStatus.FAILED
                )
        ).thenReturn(2L);

        NotificationCalendarDashboardDTO result =
                service.getDashboard();

        assertAll(
                () -> assertEquals(
                        12L,
                        result.getTotalEvents()
                ),
                () -> assertEquals(
                        7L,
                        result.getUpcomingEvents()
                ),
                () -> assertEquals(
                        2L,
                        result.getCancelledEvents()
                ),
                () -> assertEquals(
                        20L,
                        result.getTotalFollows()
                ),
                () -> assertEquals(
                        30L,
                        result.getTotalNotifications()
                ),
                () -> assertEquals(
                        25L,
                        result.getTotalSent()
                ),
                () -> assertEquals(
                        3L,
                        result.getPendingJobs()
                ),
                () -> assertEquals(
                        1L,
                        result.getProcessingJobs()
                ),
                () -> assertEquals(
                        8L,
                        result.getCompletedJobs()
                ),
                () -> assertEquals(
                        2L,
                        result.getFailedJobs()
                )
        );

        verify(
                calendarEventRepository
        ).countByStartAtAfterAndStatusNotIn(
                any(LocalDateTime.class),
                argThat(
                        (Collection<String> statuses) ->
                                statuses.contains("INACTIVE")
                                        && statuses.contains("DRAFT")
                                        && statuses.contains("CANCELLED")
                )
        );
    }

    @Test
    void nullTotalSentIsReturnedAsZero() {
        when(
                notificationLogRepository
                        .sumTotalSentByStatus(
                                NotificationJobStatus.COMPLETED
                        )
        ).thenReturn(null);

        NotificationCalendarDashboardDTO result =
                service.getDashboard();

        assertEquals(
                0L,
                result.getTotalSent()
        );
    }
}