package com.project.sneakermarket.notification.dto;

import com.project.sneakermarket.notification.entity.Notification;

import java.time.LocalDateTime;

public record MemberNotificationResponse(
        Long notificationId,
        String notificationType,
        String title,
        String content,
        String targetType,
        Long targetId,
        String targetUrl,
        Boolean isRead,
        LocalDateTime sentAt,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {

    public static MemberNotificationResponse from(
            Notification notification
    ) {
        return new MemberNotificationResponse(
                notification.getNotificationId(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getTargetType(),
                notification.getTargetId(),
                notification.getTargetUrl(),
                notification.getIsRead(),
                notification.getSentAt(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}