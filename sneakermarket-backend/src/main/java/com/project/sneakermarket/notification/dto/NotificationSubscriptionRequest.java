package com.project.sneakermarket.notification.dto;

import com.project.sneakermarket.notification.entity.NotificationSubscriptionScope;
import com.project.sneakermarket.notification.entity.NotificationSubscriptionTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record NotificationSubscriptionRequest(

        @NotNull
        NotificationSubscriptionTargetType targetType,

        @NotNull
        @Positive
        Long targetId,

        @NotNull
        NotificationSubscriptionScope scope
) {
}