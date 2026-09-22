package com.project.sneakermarket.notification.dto;

import lombok.Data;

@Data
public class NotificationDTO {
//    private Long memberId;
    private String notificationType; // 映照 eventType[cite: 7, 8]
    private String title;            // 映照 title[cite: 7, 8]
    private String content;          // 映照 description[cite: 7, 8]

    private String targetType;       // 例如 "EVENT"[cite: 7]
    private Long targetId;           // 映照 eventId[cite: 7, 8]
    private String targetUrl;        // 映照 targetUrl[cite: 7, 8]
}

