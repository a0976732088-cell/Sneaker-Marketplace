package com.project.sneakermarket.calendar.controller;

import com.project.sneakermarket.calendar.dto.CalendarEventDTO;
import com.project.sneakermarket.calendar.entity.CalendarEvent;
import com.project.sneakermarket.calendar.service.CalendarEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.PublicKey;


@RestController
public class CalendarEventController {

    @Autowired
    private CalendarEventService calendarEventService;

    @PostMapping("/api/calendar/eventCreate")
    public ResponseEntity<CalendarEvent> createEvent(@RequestBody CalendarEventDTO request){
        CalendarEvent created = calendarEventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/api/calendar/eventUpdate/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable("id") Long eventId, @RequestBody CalendarEventDTO request){
        calendarEventService.updateEventById(eventId, request);

        return ResponseEntity.status(HttpStatus.CREATED).build();

    }

    @DeleteMapping("/api/calendar/eventDelete")
    public ResponseEntity<?> deleteEvent(@RequestParam Long eventId){
        calendarEventService.deleteEventById(eventId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/api/calendar/eventPage")
    public Page<CalendarEvent> findByPage(@RequestParam(defaultValue = "1") Integer pageNumber) {

        return calendarEventService.findMsgByPage(pageNumber);
    }
}
