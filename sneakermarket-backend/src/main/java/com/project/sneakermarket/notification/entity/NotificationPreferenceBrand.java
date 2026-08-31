package com.project.sneakermarket.notification.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notification_preference_brands",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_pref_brand", columnNames = {"preference_id", "brand_id"})
        }
)
@Getter
@Setter
public class NotificationPreferenceBrand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preference_brand_id")
    private Long preferenceBrandId;

    @Column(name = "preference_id", nullable = false)
    private Long preferenceId;

    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
