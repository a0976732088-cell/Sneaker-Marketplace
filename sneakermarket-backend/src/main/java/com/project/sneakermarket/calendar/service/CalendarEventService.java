package com.project.sneakermarket.calendar.service;

import com.project.sneakermarket.calendar.dto.CalendarEventDTO;
import com.project.sneakermarket.calendar.entity.CalendarEvent;
import com.project.sneakermarket.calendar.repository.CalendarEventRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.project.sneakermarket.calendar.entity.EventFollow;
import com.project.sneakermarket.calendar.repository.EventFollowRepository;
import com.project.sneakermarket.notification.service.NotificationService;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Objects;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CalendarEventService {

    private final CalendarEventRepository calendarEventRepo;
    private final EventFollowRepository eventFollowRepository;
    private final NotificationService notificationService;

    public CalendarEvent createEvent(CalendarEventDTO dto) {
        CalendarEvent event = new CalendarEvent();
        event.setBrandId(dto.getBrandId());
        event.setCreatedBy(dto.getCreatedBy());
        event.setEventType(dto.getEventType());
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setStartAt(dto.getStartAt());
        event.setEndAt(dto.getEndAt());
        event.setTargetUrl(dto.getTargetUrl());
        event.setSourceType(dto.getSourceType());
        event.setSourceId(dto.getSourceId());
        event.setStatus(dto.getStatus());
        event.setPhoto(dto.getPhoto());

        return calendarEventRepo.save(event);
    }

    @Transactional
    public void updateEventById(
            Long eventId,
            CalendarEventDTO dto
    ) {
        CalendarEvent calendarEvent =
                calendarEventRepo.findById(eventId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "找不到活動"
                                )
                        );
        /*
         * 已取消活動不可恢復，也不可再修改內容。
         */
        if ("CANCELLED".equals(calendarEvent.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "已取消活動不能重新啟用或修改"
            );
        }

        LocalDateTime previousStartAt =
                calendarEvent.getStartAt();

        String previousStatus =
                calendarEvent.getStatus();

        calendarEvent.setBrandId(dto.getBrandId());
        calendarEvent.setCreatedBy(dto.getCreatedBy());
        calendarEvent.setEventType(dto.getEventType());
        calendarEvent.setTitle(dto.getTitle());
        calendarEvent.setDescription(dto.getDescription());
        calendarEvent.setStartAt(dto.getStartAt());
        calendarEvent.setEndAt(dto.getEndAt());
        calendarEvent.setTargetUrl(dto.getTargetUrl());
        calendarEvent.setSourceType(dto.getSourceType());
        calendarEvent.setSourceId(dto.getSourceId());
        calendarEvent.setStatus(dto.getStatus());
        if (dto.getPhoto() != null) {
            calendarEvent.setPhoto(dto.getPhoto());
        }

        boolean startTimeChanged =
                !Objects.equals(
                        previousStartAt,
                        calendarEvent.getStartAt()
                );

        boolean becameCancelled =
                !"CANCELLED".equals(previousStatus)
                        && "CANCELLED".equals(
                        calendarEvent.getStatus()
                );

        boolean canNotifyTimeChange =
                !"DRAFT".equals(calendarEvent.getStatus())
                        && !"INACTIVE".equals(calendarEvent.getStatus())
                        && !"CANCELLED".equals(calendarEvent.getStatus());

        calendarEventRepo.save(calendarEvent);

        if (becameCancelled) {
            List<Long> followerMemberIds =
                    eventFollowRepository
                            .findByEventId(eventId)
                            .stream()
                            .map(EventFollow::getMemberId)
                            .distinct()
                            .toList();

            notificationService
                    .createEventCancelledNotifications(
                            calendarEvent,
                            followerMemberIds
                    );

            return;
        }

        if (startTimeChanged && canNotifyTimeChange) {
            List<Long> followerMemberIds =
                    eventFollowRepository
                            .findByEventId(eventId)
                            .stream()
                            .map(EventFollow::getMemberId)
                            .distinct()
                            .toList();

            notificationService
                    .createEventTimeChangedNotifications(
                            calendarEvent,
                            previousStartAt,
                            followerMemberIds
                    );
        }
    }

    @Transactional
    public void cancelEventById(Long eventId) {

        CalendarEvent event =
                calendarEventRepo.findById(eventId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "找不到活動"
                                )
                        );

        /*
         * 重複呼叫取消 API 時直接結束，
         * 避免關注會員收到重複通知。
         */
        if ("CANCELLED".equals(event.getStatus())) {
            return;
        }

        List<Long> followerMemberIds =
                eventFollowRepository
                        .findByEventId(eventId)
                        .stream()
                        .map(EventFollow::getMemberId)
                        .distinct()
                        .toList();

        event.setStatus("CANCELLED");

        calendarEventRepo.save(event);

        notificationService
                .createEventCancelledNotifications(
                        event,
                        followerMemberIds
                );
    }

    public Page<CalendarEvent> searchEvent(
            String title,
            String eventType,
            String status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            boolean overlap,
            Integer pageNumber,
            Integer pageSize){
        Specification<CalendarEvent> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 1. 標題模糊查詢 (LIKE %title%)
            if (title != null && !title.trim().isEmpty()) {
                predicates.add(cb.like(root.get("title"), "%" + title.trim() + "%"));
            }
            // 2. 活動類型 (EQUAL)
            if (eventType != null && !eventType.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("eventType"), eventType));
            }
            // 3. 活動狀態 (EQUAL)
            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (overlap && startDate != null && endDate != null) {
                // 活動開始時間不晚於月底，且結束時間不早於月初。
                predicates.add(
                        cb.lessThanOrEqualTo(root.get("startAt"), endDate)
                );
                predicates.add(
                        cb.or(
                                cb.greaterThanOrEqualTo(root.get("endAt"), startDate),
                                cb.and(
                                        cb.isNull(root.get("endAt")),
                                        cb.greaterThanOrEqualTo(root.get("startAt"), startDate)
                                )
                        )
                );
            } else {
                // 保留原本管理頁使用的時間篩選語意。
                if (startDate != null) {
                    predicates.add(
                            cb.greaterThanOrEqualTo(root.get("startAt"), startDate)
                    );
                }
                if (endDate != null) {
                    predicates.add(
                            cb.lessThanOrEqualTo(root.get("endAt"), endDate)
                    );
                }
            }
            // 將所有條件用 AND 串起來
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        int safePage =
                pageNumber != null && pageNumber > 0
                        ? pageNumber - 1
                        : 0;

        int safePageSize =
                pageSize != null && pageSize > 0
                        ? Math.min(pageSize, 100)
                        : 10;

        Pageable pgb = PageRequest.of(
                safePage,
                safePageSize,
                Sort.Direction.DESC,
                "startAt"
        );
        return calendarEventRepo.findAll(spec, pgb);
    }






}
