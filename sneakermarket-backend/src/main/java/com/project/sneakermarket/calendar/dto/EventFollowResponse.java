package com.project.sneakermarket.calendar.dto;

import com.project.sneakermarket.calendar.entity.EventFollow;

import java.time.LocalDateTime;

public record EventFollowResponse(
        Long followId,
        Long eventId,
        Integer remindBeforeMinutes,
        Boolean reminded,
        LocalDateTime createdAt
) {

    public static EventFollowResponse from(EventFollow follow) {
        return new EventFollowResponse(
                follow.getFollowId(),
                follow.getEventId(),
                follow.getRemindBeforeMinutes(),
                follow.getReminded(),
                follow.getCreatedAt()
        );
    }
}
