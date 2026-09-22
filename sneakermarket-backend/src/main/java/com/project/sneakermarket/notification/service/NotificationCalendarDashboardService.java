package com.project.sneakermarket.notification.service;

import com.project.sneakermarket.calendar.entity.CalendarEvent;
import com.project.sneakermarket.calendar.entity.EventFollow;
import com.project.sneakermarket.calendar.repository.CalendarEventRepository;
import com.project.sneakermarket.calendar.repository.EventFollowRepository;
import com.project.sneakermarket.notification.dto.NotificationCalendarDashboardDTO;
import com.project.sneakermarket.notification.entity.NotificationJobStatus;
import com.project.sneakermarket.notification.entity.NotificationLog;
import com.project.sneakermarket.notification.repository.NotificationLogRepository;
import com.project.sneakermarket.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationCalendarDashboardService {

    private static final ZoneId TAIPEI_ZONE =
            ZoneId.of("Asia/Taipei");

    private static final DateTimeFormatter WEEK_LABEL_FORMAT =
            DateTimeFormatter.ofPattern("M/d");

    private final CalendarEventRepository calendarEventRepository;
    private final EventFollowRepository eventFollowRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationLogRepository notificationLogRepository;

    @Transactional(readOnly = true)
    public NotificationCalendarDashboardDTO getDashboard() {
        LocalDateTime now = LocalDateTime.now(TAIPEI_ZONE);

        NotificationCalendarDashboardDTO dto =
                new NotificationCalendarDashboardDTO();

        populateSummary(dto, now);
        populateEventStatus(dto, now);
        populateActivityTrend(dto, now);
        populateDeliveryTrend(dto, now);
        populateUpcomingSchedule(dto, now);
        populateDeliveryRate(dto);

        return dto;
    }

    private void populateSummary(
            NotificationCalendarDashboardDTO dto,
            LocalDateTime now
    ) {
        dto.setTotalEvents(calendarEventRepository.count());

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
                calendarEventRepository.countByStatus("CANCELLED")
        );

        dto.setTotalFollows(eventFollowRepository.count());
        dto.setTotalNotifications(notificationRepository.count());

        Long totalSent =
                notificationLogRepository.sumTotalSentByStatus(
                        NotificationJobStatus.COMPLETED
                );

        dto.setTotalSent(totalSent != null ? totalSent : 0L);

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
    }

    private void populateEventStatus(
            NotificationCalendarDashboardDTO dto,
            LocalDateTime now
    ) {
        List<CalendarEvent> events = calendarEventRepository.findAll();

        long active = events.stream()
                .filter(event ->
                        "ACTIVE".equals(event.getStatus())
                                && !event.getStartAt().isAfter(now)
                                && (
                                event.getEndAt() == null
                                        || !event.getEndAt().isBefore(now)
                        )
                )
                .count();

        long ended = events.stream()
                .filter(event ->
                        !"DRAFT".equals(event.getStatus())
                                && !"CANCELLED".equals(event.getStatus())
                                && event.getEndAt() != null
                                && event.getEndAt().isBefore(now)
                )
                .count();

        long drafts = events.stream()
                .filter(event -> "DRAFT".equals(event.getStatus()))
                .count();

        dto.setActiveEvents(active);
        dto.setEndedEvents(ended);
        dto.setDraftEvents(drafts);
    }

    private void populateActivityTrend(
            NotificationCalendarDashboardDTO dto,
            LocalDateTime now
    ) {
        LocalDate endDate = now.toLocalDate();
        LocalDate startDate = endDate.minusDays(29);

        LocalDateTime start =
                startDate.atStartOfDay();

        LocalDateTime end =
                endDate.atTime(LocalTime.MAX);

        List<CalendarEvent> events =
                calendarEventRepository.findByCreatedAtBetween(start, end);

        List<EventFollow> follows =
                eventFollowRepository.findByCreatedAtBetween(start, end);

        Map<LocalDate, Long> eventsByDate =
                events.stream()
                        .collect(
                                Collectors.groupingBy(
                                        event -> event
                                                .getCreatedAt()
                                                .toLocalDate(),
                                        Collectors.counting()
                                )
                        );

        Map<LocalDate, Long> followsByDate =
                follows.stream()
                        .collect(
                                Collectors.groupingBy(
                                        follow -> follow
                                                .getCreatedAt()
                                                .toLocalDate(),
                                        Collectors.counting()
                                )
                        );

        List<NotificationCalendarDashboardDTO.TrendPoint> result =
                new ArrayList<>();

        for (
                LocalDate date = startDate;
                !date.isAfter(endDate);
                date = date.plusDays(1)
        ) {
            NotificationCalendarDashboardDTO.TrendPoint point =
                    new NotificationCalendarDashboardDTO.TrendPoint();

            point.setDate(date);
            point.setEventCount(eventsByDate.getOrDefault(date, 0L));
            point.setFollowCount(followsByDate.getOrDefault(date, 0L));

            result.add(point);
        }

        dto.setActivityTrend(result);
    }

    private void populateDeliveryTrend(
            NotificationCalendarDashboardDTO dto,
            LocalDateTime now
    ) {
        LocalDate currentDate = now.toLocalDate();
        LocalDate firstDate = currentDate.minusDays(27);

        List<NotificationLog> logs =
                notificationLogRepository.findByCreatedAtBetween(
                        firstDate.atStartOfDay(),
                        currentDate.atTime(LocalTime.MAX)
                );

        List<NotificationCalendarDashboardDTO.DeliveryPoint> result =
                new ArrayList<>();

        for (int week = 0; week < 4; week++) {
            LocalDate weekStart =
                    firstDate.plusDays((long) week * 7);

            LocalDate weekEnd =
                    weekStart.plusDays(6);

            List<NotificationLog> weekLogs =
                    logs.stream()
                            .filter(log -> {
                                LocalDate createdDate =
                                        log.getCreatedAt().toLocalDate();

                                return !createdDate.isBefore(weekStart)
                                        && !createdDate.isAfter(weekEnd);
                            })
                            .toList();

            NotificationCalendarDashboardDTO.DeliveryPoint point =
                    new NotificationCalendarDashboardDTO.DeliveryPoint();

            point.setLabel(
                    weekStart.format(WEEK_LABEL_FORMAT)
                            + "－"
                            + weekEnd.format(WEEK_LABEL_FORMAT)
            );

            point.setCompleted(
                    sumRecipientsByStatus(
                            weekLogs,
                            NotificationJobStatus.COMPLETED
                    )
            );

            point.setProcessing(
                    sumRecipientsByStatus(
                            weekLogs,
                            NotificationJobStatus.PROCESSING
                    )
            );

            point.setFailed(
                    weekLogs.stream()
                            .map(NotificationLog::getFailedCount)
                            .mapToLong(value -> value == null ? 0 : value)
                            .sum()
            );

            result.add(point);
        }

        dto.setNotificationDeliveryTrend(result);
    }

    private long sumRecipientsByStatus(
            List<NotificationLog> logs,
            NotificationJobStatus status
    ) {
        return logs.stream()
                .filter(log -> log.getStatus() == status)
                .map(NotificationLog::getTotalSent)
                .mapToLong(value -> value == null ? 0 : value)
                .sum();
    }

    private void populateUpcomingSchedule(
            NotificationCalendarDashboardDTO dto,
            LocalDateTime now
    ) {
        LocalDate firstDate = now.toLocalDate();
        LocalDate lastDate = firstDate.plusDays(6);

        List<CalendarEvent> events =
                calendarEventRepository
                        .findByStartAtBetweenOrderByStartAtAsc(
                                firstDate.atStartOfDay(),
                                lastDate.atTime(LocalTime.MAX)
                        )
                        .stream()
                        .filter(event ->
                                !"INACTIVE".equals(event.getStatus())
                                        && !"DRAFT".equals(event.getStatus())
                                        && !"CANCELLED".equals(event.getStatus())
                        )
                        .toList();

        Map<LocalDate, List<CalendarEvent>> eventsByDate =
                events.stream()
                        .collect(
                                Collectors.groupingBy(
                                        event -> event
                                                .getStartAt()
                                                .toLocalDate()
                                )
                        );

        List<NotificationCalendarDashboardDTO.ScheduleDay> result =
                new ArrayList<>();

        for (
                LocalDate date = firstDate;
                !date.isAfter(lastDate);
                date = date.plusDays(1)
        ) {
            NotificationCalendarDashboardDTO.ScheduleDay day =
                    new NotificationCalendarDashboardDTO.ScheduleDay();

            day.setDate(date);

            List<NotificationCalendarDashboardDTO.ScheduleItem> items =
                    eventsByDate
                            .getOrDefault(date, List.of())
                            .stream()
                            .map(this::toScheduleItem)
                            .toList();

            day.setItems(items);
            result.add(day);
        }

        dto.setUpcomingSchedule(result);
    }

    private NotificationCalendarDashboardDTO.ScheduleItem toScheduleItem(
            CalendarEvent event
    ) {
        NotificationCalendarDashboardDTO.ScheduleItem item =
                new NotificationCalendarDashboardDTO.ScheduleItem();

        item.setType("EVENT");
        item.setId(event.getEventId());
        item.setTitle(event.getTitle());
        item.setScheduledAt(event.getStartAt());

        return item;
    }

    private void populateDeliveryRate(
            NotificationCalendarDashboardDTO dto
    ) {
        List<NotificationLog> logs =
                notificationLogRepository.findAll();

        long totalRecipients =
                logs.stream()
                        .map(NotificationLog::getTotalRecipients)
                        .mapToLong(value -> value == null ? 0 : value)
                        .sum();

        long totalDelivered =
                logs.stream()
                        .map(NotificationLog::getTotalSent)
                        .mapToLong(value -> value == null ? 0 : value)
                        .sum();

        double deliveryRate =
                totalRecipients == 0
                        ? 0.0
                        : totalDelivered * 100.0 / totalRecipients;

        dto.setDeliveryRate(
                Math.round(deliveryRate * 10.0) / 10.0
        );
    }
}