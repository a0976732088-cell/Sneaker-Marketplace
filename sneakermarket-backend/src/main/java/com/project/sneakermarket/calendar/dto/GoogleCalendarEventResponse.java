package com.project.sneakermarket.calendar.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleCalendarEventResponse(
        String id
) {
}