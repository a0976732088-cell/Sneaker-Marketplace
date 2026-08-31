package com.project.sneakermarket.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "IX_notifications_member_read", columnList = "member_id, is_read")
        }
)
@Getter
@Setter
public class Notification {@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "notification_id")
private Long notificationId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "notification_type", nullable = false, length = 40)
    private String notificationType;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "target_type", length = 30)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "target_url", length = 500)
    private String targetUrl;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}