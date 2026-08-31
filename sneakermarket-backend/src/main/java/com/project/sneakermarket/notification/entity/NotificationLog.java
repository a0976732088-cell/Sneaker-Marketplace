package com.project.sneakermarket.notification.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs")
@Getter
@Setter
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "send_type", nullable = false, length = 20)
    private String sendType;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType;

    @Column(name = "total_sent", nullable = false)
    private Integer totalSent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
