package com.project.sneakermarket.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preference_id", nullable = false)
    private Long preferenceId;

    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    /*
     * 全部通知的總開關。
     */
    @Column(name = "all_enabled", nullable = false)
    private Boolean allEnabled = true;

    /*
     * 以下三個欄位是舊版資料表欄位。
     * 目前先保留，避免破壞既有資料。
     */
    @Column(name = "auction_enabled", nullable = false)
    private Boolean auctionEnabled = true;

    @Column(name = "promotion_enabled", nullable = false)
    private Boolean promotionEnabled = true;

    @Column(name = "release_enabled", nullable = false)
    private Boolean releaseEnabled = true;

    /*
     * 活動通知管道。
     */
    @Column(name = "event_in_app_enabled", nullable = false)
    private Boolean eventInAppEnabled = true;

    @Column(name = "event_email_enabled", nullable = false)
    private Boolean eventEmailEnabled = true;

    /*
     * 系統公告管道。
     */
    @Column(name = "system_in_app_enabled", nullable = false)
    private Boolean systemInAppEnabled = true;

    @Column(name = "system_email_enabled", nullable = false)
    private Boolean systemEmailEnabled = true;

    /*
     * 新品通知管道。
     */
    @Column(name = "release_in_app_enabled", nullable = false)
    private Boolean releaseInAppEnabled = true;

    @Column(name = "release_email_enabled", nullable = false)
    private Boolean releaseEmailEnabled = true;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}