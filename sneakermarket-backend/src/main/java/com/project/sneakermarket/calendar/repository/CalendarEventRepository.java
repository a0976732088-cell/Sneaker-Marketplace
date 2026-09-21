package com.project.sneakermarket.calendar.repository;

import com.project.sneakermarket.calendar.entity.CalendarEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface CalendarEventRepository extends
        JpaRepository<CalendarEvent, Long>,
        JpaSpecificationExecutor<CalendarEvent> { // 👈 2. 繼承 JpaSpecificationExecutor[cite: 2]
    @Query("from CalendarEvent")
    Page<CalendarEvent> findLastest(Pageable pgb);

//     計算指定狀態的活動數量。

    long countByStatus(String status);

//     計算尚未開始，而且狀態不在排除清單內的活動。

    long countByStartAtAfterAndStatusNotIn(
            LocalDateTime now,
            Collection<String> excludedStatuses
    );

    List<CalendarEvent> findByCreatedAtBetween(
            LocalDateTime start,
            LocalDateTime end
    );

    List<CalendarEvent> findByStartAtBetweenOrderByStartAtAsc(
            LocalDateTime start,
            LocalDateTime end
    );
}
