package com.project.sneakermarket.notification.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notification_preferences",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_preference_member", columnNames = {"member_id"})
        }
)
@Getter
@Setter
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preference_id")
    private Long preferenceId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "all_enabled", nullable = false)
    private Boolean allEnabled = true;

    @Column(name = "auction_enabled", nullable = false)
    private Boolean auctionEnabled = true;

    @Column(name = "promotion_enabled", nullable = false)
    private Boolean promotionEnabled = true;

    @Column(name = "release_enabled", nullable = false)
    private Boolean releaseEnabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
