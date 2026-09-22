package com.project.sneakermarket.notification.entity;

import com.project.sneakermarket.member.entity.Member;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    //      建立與 Member 的 FK 關聯
    @Column(name = "member_id")
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

    @Column(name = "is_sent", nullable = false)
    private Boolean isSent = false; // 新增：是否已點擊發送

    @Column(name = "in_app_visible", nullable = false)
    private Boolean inAppVisible = true;

    @Column(name = "sent_at")
    private LocalDateTime sentAt; // 新增：發送時間

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    // 由 Java 程式控制建立時間
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }


}
