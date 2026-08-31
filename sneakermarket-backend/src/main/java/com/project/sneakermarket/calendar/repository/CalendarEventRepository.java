package com.project.sneakermarket.calendar.repository;

import com.project.sneakermarket.calendar.entity.CalendarEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {
    @Query("from CalendarEvent")
    Page<CalendarEvent> findLastest(Pageable pgb);
}
