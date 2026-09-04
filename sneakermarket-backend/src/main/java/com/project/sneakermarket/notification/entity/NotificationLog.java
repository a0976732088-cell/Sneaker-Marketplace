package com.project.sneakermarket.notification.entity;


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

    @Column(name = "title", nullable = false, length = 100)
    private String title; //[cite: 10]

    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType; //[cite: 10]

    @Column(name = "total_sent", nullable = false)
    private Integer totalSent; //[cite: 10]

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; //[cite: 10]

    @Column(name = "event_id", nullable = false)
    private Long eventId; //[cite: 10]
}