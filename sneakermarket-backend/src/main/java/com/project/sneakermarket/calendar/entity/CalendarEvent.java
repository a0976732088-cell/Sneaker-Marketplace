package com.project.sneakermarket.calendar.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "calendar_events")
@Getter
@Setter
public class CalendarEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "brand_id")
    private Long brandId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Nationalized
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Nationalized
    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(
            name = "venue_name",
            length = 150
    )

    @Nationalized
    private String venueName;

    @Nationalized
    @Column(
            name = "address",
            length = 300
    )
    private String address;

    @Column(
            name = "latitude",
            precision = 9,
            scale = 6
    )
    private BigDecimal latitude;

    @Column(
            name = "longitude",
            precision = 10,
            scale = 6
    )
    private BigDecimal longitude;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "target_url", length = 500)
    private String targetUrl;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @JsonIgnore
    @Lob
    private byte[] photo;

    @JsonProperty("hasPhoto")
    public boolean getHasPhoto() {
        return photo != null && photo.length > 0;
    }

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }

    }
}
