package com.project.sneakermarket.calendar.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "google_calendar_event_links",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UQ_google_calendar_event_links_member_event",
                        columnNames = {
                                "member_id",
                                "event_id"
                        }
                )
        }
)
public class GoogleCalendarEventLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "link_id")
    private Long linkId;

    @Column(
            name = "member_id",
            nullable = false
    )
    private Long memberId;

    @Column(
            name = "event_id",
            nullable = false
    )
    private Long eventId;

    @Column(
            name = "google_event_id",
            nullable = false,
            length = 255
    )
    private String googleEventId;

    @Column(
            name = "sync_status",
            nullable = false,
            length = 20
    )
    private String syncStatus;

    @Column(
            name = "last_error",
            length = 500
    )
    private String lastError;

    @Column(
            name = "last_synced_at",
            nullable = false
    )
    private LocalDateTime lastSyncedAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (syncStatus == null) {
            syncStatus = "SYNCED";
        }

        if (lastSyncedAt == null) {
            lastSyncedAt = now;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}