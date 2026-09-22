package com.project.sneakermarket.calendar.entity;

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
@Table(name = "google_calendar_connections")
public class GoogleCalendarConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "connection_id")
    private Long connectionId;

    @Column(
            name = "member_id",
            nullable = false,
            unique = true
    )
    private Long memberId;

    @Column(
            name = "access_token_encrypted",
            nullable = false,
            columnDefinition = "NVARCHAR(MAX)"
    )
    private String accessTokenEncrypted;

    @Column(
            name = "refresh_token_encrypted",
            nullable = false,
            columnDefinition = "NVARCHAR(MAX)"
    )
    private String refreshTokenEncrypted;

    @Column(
            name = "access_token_expires_at",
            nullable = false
    )
    private LocalDateTime accessTokenExpiresAt;

    @Column(
            name = "granted_scope",
            length = 1000
    )
    private String grantedScope;

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

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}