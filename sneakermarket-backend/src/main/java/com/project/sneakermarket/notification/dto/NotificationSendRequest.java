package com.project.sneakermarket.notification.dto;

import lombok.Data;

@Data
public class NotificationSendRequest {

    /**
     * ALL_MEMBERS：發給全體會員
     * EVENT_FOLLOWERS：發給指定活動的關注者
     */
    private String audienceType;
}