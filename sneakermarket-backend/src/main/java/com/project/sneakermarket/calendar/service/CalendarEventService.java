package com.project.sneakermarket.calendar.service;

import com.project.sneakermarket.calendar.dto.CalendarEventDTO;
import com.project.sneakermarket.calendar.entity.CalendarEvent;
import com.project.sneakermarket.calendar.repository.CalendarEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

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

            calendarEventRepo.save(calendarEvent);
        }
    }

    public void deleteEventById(Long eventId){
        calendarEventRepo.deleteById(eventId);
    }

    public Page<CalendarEvent> findMsgByPage(Integer pageNumber){
        Pageable pgb = PageRequest.of(pageNumber-1, 3, Sort.Direction.DESC, "createdAt");//從0開始所以要-1

        return calendarEventRepo.findAll(pgb);
    }

}
