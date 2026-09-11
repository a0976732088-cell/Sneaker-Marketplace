package com.project.sneakermarket.calendar.controller;

import com.project.sneakermarket.calendar.dto.EventFollowRequest;
import com.project.sneakermarket.calendar.dto.EventFollowResponse;
import com.project.sneakermarket.calendar.service.CalendarSessionMemberResolver;
import com.project.sneakermarket.calendar.service.EventFollowService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calendar/events")
@RequiredArgsConstructor
public class EventFollowController {

    private final EventFollowService eventFollowService;
    private final CalendarSessionMemberResolver sessionMemberResolver;

    @PutMapping("/{eventId}/follow")
    public ResponseEntity<EventFollowResponse> followEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody EventFollowRequest requestBody,
            HttpServletRequest request
    ) {
        Long memberId =
                sessionMemberResolver.requireMemberId(request);

        EventFollowResponse result =
                eventFollowService.followEvent(
                        memberId,
                        eventId,
                        requestBody.remindBeforeMinutes()
                );

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{eventId}/follow")
    public ResponseEntity<Void> unfollowEvent(
            @PathVariable Long eventId,
            HttpServletRequest request
    ) {
        Long memberId =
                sessionMemberResolver.requireMemberId(request);

        eventFollowService.unfollowEvent(memberId, eventId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/follows/me")
    public ResponseEntity<List<EventFollowResponse>> getMyFollows(
            HttpServletRequest request
    ) {
        Long memberId =
                sessionMemberResolver.requireMemberId(request);

        return ResponseEntity.ok(
                eventFollowService.getMyFollows(memberId)
        );
    }
}