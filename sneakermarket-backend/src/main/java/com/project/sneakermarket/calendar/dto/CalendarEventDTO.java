package com.project.sneakermarket.calendar.dto;

import lombok.Data;


import java.time.LocalDateTime;

@Data
public class CalendarEventDTO {
    private Long brandId;
    private Long createdBy;
    private String eventType;
    private String title;
    private String description;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String targetUrl;
    private String sourceType;
    private Long sourceId;
    private String status;
}
