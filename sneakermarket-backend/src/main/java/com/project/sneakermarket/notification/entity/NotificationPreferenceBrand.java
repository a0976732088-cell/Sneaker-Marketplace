package com.project.sneakermarket.notification.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "notification_preference_brands",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"preference_id", "brand_id"})
        }
)
public class NotificationPreferenceBrand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preference_brand_id", nullable = false)
    private Long preferenceBrandId;

    // 🎯 建立與 NotificationPreference 的 FK 關聯
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preference_id", nullable = false)
    private NotificationPreference notificationPreference;

    // 🎯 建立與 Brand 的 FK 關聯
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "brand_id", nullable = false)
//    private Brand brand;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 🎯 由 Java 程式控制建立與更新時間
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
