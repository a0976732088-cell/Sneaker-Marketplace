package com.project.sneakermarket.calendar.dto;

import java.time.LocalDateTime;

public record GoogleCalendarConnectionResponse(
        boolean connected,
        LocalDateTime updatedAt
) {
}