package com.project.sneakermarket.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(
        name = "notification_email_jobs",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_notification_email_jobs_notification",
                columnNames = "notification_id"
        ),
        indexes = @Index(
                name = "IX_notification_email_jobs_status_created",
                columnList = "status, created_at"
        )
)
public class NotificationEmailJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 一筆會員站內通知只建立一個 Email 工作
    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    // 建立工作當下記錄收件地址，避免會員日後改信箱影響這次工作
    @Column(name = "recipient_email", nullable = false, length = 100)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationEmailStatus status = NotificationEmailStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "last_error", length = 300)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}