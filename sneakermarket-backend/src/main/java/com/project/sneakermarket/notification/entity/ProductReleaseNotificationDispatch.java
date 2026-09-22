package com.project.sneakermarket.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "product_release_notification_dispatches",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_product_release_notification_product",
                columnNames = "product_id"
        )
)
public class ProductReleaseNotificationDispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dispatch_id")
    private Long dispatchId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "notification_count", nullable = false)
    private Integer notificationCount = 0;

    @Column(
            name = "processed_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime processedAt;

    @PrePersist
    public void onCreate() {
        if (processedAt == null) {
            processedAt = LocalDateTime.now();
        }

        if (notificationCount == null) {
            notificationCount = 0;
        }
    }
}