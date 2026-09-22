package com.project.sneakermarket.notification.dto;

import com.project.sneakermarket.notification.entity.NotificationSubscriptionScope;
import com.project.sneakermarket.notification.entity.NotificationSubscriptionTargetType;

import java.time.LocalDateTime;

public record NotificationSubscriptionResponse(
        Long subscriptionId,
        NotificationSubscriptionTargetType targetType,
        Long targetId,
        String targetName,
        NotificationSubscriptionScope scope,
        LocalDateTime createdAt
) {
}