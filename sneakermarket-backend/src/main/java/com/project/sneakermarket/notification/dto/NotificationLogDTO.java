package com.project.sneakermarket.notification.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NotificationLogDTO {

    private Long id;
    private Long notificationId;

    private String sendType;
    private String title;
    private String targetType;
    private Long eventId;

    private String status;

    private Integer totalRecipients;
    private Integer totalSent;
    private Integer failedCount;

    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private String errorMessage;
}