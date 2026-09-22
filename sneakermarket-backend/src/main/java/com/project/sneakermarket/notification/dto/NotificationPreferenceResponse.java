package com.project.sneakermarket.notification.dto;

import com.project.sneakermarket.notification.entity.NotificationPreference;

import java.time.LocalDateTime;

public record NotificationPreferenceResponse(
        Long preferenceId,
        Boolean allEnabled,
        Boolean eventInAppEnabled,
        Boolean eventEmailEnabled,
        Boolean systemInAppEnabled,
        Boolean systemEmailEnabled,
        Boolean releaseInAppEnabled,
        Boolean releaseEmailEnabled,
        LocalDateTime updatedAt
) {

    public static NotificationPreferenceResponse from(
            NotificationPreference preference
    ) {
        return new NotificationPreferenceResponse(
                preference.getPreferenceId(),
                preference.getAllEnabled(),
                preference.getEventInAppEnabled(),
                preference.getEventEmailEnabled(),
                preference.getSystemInAppEnabled(),
                preference.getSystemEmailEnabled(),
                preference.getReleaseInAppEnabled(),
                preference.getReleaseEmailEnabled(),
                preference.getUpdatedAt()
        );
    }
}