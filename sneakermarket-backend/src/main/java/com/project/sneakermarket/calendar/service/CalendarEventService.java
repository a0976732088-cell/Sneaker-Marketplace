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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CalendarEventService {

    private final CalendarEventRepository calendarEventRepo;


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

    public void updateEventById(Long eventId, CalendarEventDTO dto) {
        Optional<CalendarEvent> op = calendarEventRepo.findById(eventId);

        if(op.isPresent()) {
            CalendarEvent calendarEvent = op.get();
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
            calendarEvent.setPhoto(dto.getPhoto());

            calendarEventRepo.save(calendarEvent);
        }
    }

    public void deleteEventById(Long eventId){
        calendarEventRepo.deleteById(eventId);
    }

    public Page<CalendarEvent> searchEvent(
            String title,
            String eventType,
            String status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer pageNumber){
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
            // 4. 開始時間起 (>= startDate)
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startAt"), startDate));
            }
            // 5. 結束時間止 (<= endDate)
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("endAt"), endDate));
            }
            // 將所有條件用 AND 串起來
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Pageable pgb = PageRequest.of(pageNumber-1, 10, Sort.Direction.DESC, "startAt");//從0開始所以要-1

        return calendarEventRepo.findAll(spec, pgb);
    }






}
