package com.project.sneakermarket.calendar.repository;

import com.project.sneakermarket.calendar.entity.CalendarEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface CalendarEventRepository extends
        JpaRepository<CalendarEvent, Long>,
        JpaSpecificationExecutor<CalendarEvent> { // 👈 2. 繼承 JpaSpecificationExecutor[cite: 2]
    @Query("from CalendarEvent")
    Page<CalendarEvent> findLastest(Pageable pgb);
}
