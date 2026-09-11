package com.project.sneakermarket.calendar.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EventFollowRequest(

        @NotNull(message = "請選擇提醒時間")
        @Min(value = 1, message = "提醒時間至少為 1 分鐘")
        @Max(value = 10080, message = "提醒時間最多為 7 天")
        Integer remindBeforeMinutes
) {
}
