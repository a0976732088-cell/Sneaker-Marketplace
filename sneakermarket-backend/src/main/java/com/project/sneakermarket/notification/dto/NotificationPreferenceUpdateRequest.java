package com.project.sneakermarket.notification.dto;

import jakarta.validation.constraints.NotNull;

public record NotificationPreferenceUpdateRequest(

        @NotNull
        Boolean allEnabled,

        @NotNull
        Boolean eventInAppEnabled,

        @NotNull
        Boolean eventEmailEnabled,

        @NotNull
        Boolean systemInAppEnabled,

        @NotNull
        Boolean systemEmailEnabled,

        @NotNull
        Boolean releaseInAppEnabled,

        @NotNull
        Boolean releaseEmailEnabled
) {
}