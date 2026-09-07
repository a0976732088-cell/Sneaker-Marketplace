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
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preference_id", nullable = false)
    private Long preferenceId;

    // 🎯 建立與 Member 的 FK 關聯
//    @OneToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "member_id", nullable = false, unique = true)[cite: 10]
//    private Member member;

    @Column(name = "all_enabled", nullable = false)
    private Boolean allEnabled = true;

    @Column(name = "auction_enabled", nullable = false)
    private Boolean auctionEnabled = true;

    @Column(name = "promotion_enabled", nullable = false)
    private Boolean promotionEnabled = true;

    @Column(name = "release_enabled", nullable = false)
    private Boolean releaseEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 🎯 在 Java 新增與更新時由程式寫入時間
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}