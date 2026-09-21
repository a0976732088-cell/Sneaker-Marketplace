package com.project.sneakermarket.calendar.repository;

import com.project.sneakermarket.calendar.entity.GoogleCalendarEventLink;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoogleCalendarEventLinkRepository
        extends JpaRepository<GoogleCalendarEventLink, Long> {

    Optional<GoogleCalendarEventLink>
    findByMemberIdAndEventId(
            Long memberId,
            Long eventId
    );

    List<GoogleCalendarEventLink>
    findByEventId(Long eventId);

    List<GoogleCalendarEventLink>
    findByMemberId(Long memberId);

    boolean existsByMemberIdAndEventId(
            Long memberId,
            Long eventId
    );

    void deleteByMemberIdAndEventId(
            Long memberId,
            Long eventId
    );

    void deleteByMemberId(Long memberId);
}