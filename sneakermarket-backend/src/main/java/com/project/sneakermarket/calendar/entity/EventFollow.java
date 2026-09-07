package com.project.sneakermarket.calendar.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "event_follows",
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_member_event", columnNames = {"member_id", "event_id"})
        }
)
@Getter
@Setter
public class EventFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_id")
    private Long followId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "remind_before_minutes", nullable = false)
    private Integer remindBeforeMinutes;

    @Column(name = "reminded", nullable = false)
    private Boolean reminded = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
