package com.project.sneakermarket.notification.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NotificationLogDTO {
    private Long id;
    private String sendType;
    private String title;
    private String targetType;
    private Integer totalSent;
    private LocalDateTime createdAt;
    private Long eventId;
}