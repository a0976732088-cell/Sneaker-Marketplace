package com.project.sneakermarket.notification.entity;


import com.project.sneakermarket.notification.entity.NotificationJobStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "notification_logs")
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id; //[cite: 10]

    @Column(name = "send_type", nullable = false, length = 20)
    private String sendType; //[cite: 10]

    @Column(name = "title", nullable = false, length = 150)
    private String title; //[cite: 10]

    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType; //[cite: 10]

    @Column(name = "total_sent", nullable = false)
    private Integer totalSent = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; //[cite: 10]

    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "notification_id")
    private Long notificationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationJobStatus status =
            NotificationJobStatus.PENDING;

    @Column(name = "total_recipients", nullable = false)
    private Integer totalRecipients = 0;

    @Column(name = "failed_count", nullable = false)
    private Integer failedCount = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status = NotificationJobStatus.PENDING;
        }

        if (totalRecipients == null) {
            totalRecipients = 0;
        }

        if (totalSent == null) {
            totalSent = 0;
        }

        if (failedCount == null) {
            failedCount = 0;
        }
    }

}