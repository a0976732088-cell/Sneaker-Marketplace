package com.project.sneakermarket.calendar.service;

import com.project.sneakermarket.calendar.dto.EventFollowResponse;
import com.project.sneakermarket.calendar.entity.CalendarEvent;
import com.project.sneakermarket.calendar.entity.EventFollow;
import com.project.sneakermarket.calendar.repository.CalendarEventRepository;
import com.project.sneakermarket.calendar.repository.EventFollowRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventFollowService {

    private final EventFollowRepository eventFollowRepository;
    private final CalendarEventRepository calendarEventRepository;

    @Transactional
    public EventFollowResponse followEvent(
            Long memberId,
            Long eventId,
            Integer remindBeforeMinutes
    ) {
        CalendarEvent event = calendarEventRepository
                .findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "找不到活動"
                ));

        if ("INACTIVE".equals(event.getStatus())
                || "DRAFT".equals(event.getStatus())
                || "CANCELLED".equals(event.getStatus())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "目前無法關注這個活動"
            );
        }

        if (!event.getStartAt().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "活動已經開始或結束"
            );
        }

        EventFollow follow = eventFollowRepository
                .findByMemberIdAndEventId(memberId, eventId)
                .orElseGet(() -> {
                    EventFollow newFollow = new EventFollow();
                    newFollow.setMemberId(memberId);
                    newFollow.setEventId(eventId);
                    newFollow.setReminded(false);
                    return newFollow;
                });

        follow.setRemindBeforeMinutes(remindBeforeMinutes);

        EventFollow saved = eventFollowRepository.save(follow);

        return EventFollowResponse.from(saved);
    }

    @Transactional
    public void unfollowEvent(
            Long memberId,
            Long eventId
    ) {
        eventFollowRepository
                .findByMemberIdAndEventId(memberId, eventId)
                .ifPresent(eventFollowRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<EventFollowResponse> getMyFollows(
            Long memberId
    ) {
        return eventFollowRepository
                .findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(EventFollowResponse::from)
                .toList();
    }
}