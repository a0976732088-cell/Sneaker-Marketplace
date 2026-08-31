package com.project.sneakermarket.advertisement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "advertisements")
@NoArgsConstructor
@Getter
@Setter
public class Advertisement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ad_id")
    private Long adId;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "ad_name", nullable = false)
    private String adName;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "ad_description")
    private String adDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "ad_type", nullable = false)
    private AdvertisementType adType;

    @Enumerated(EnumType.STRING)
    @Column(name = "ad_placement",nullable = false)
    private AdvertisementPlacement adPlacement;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private AdvertisementTargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "target_url")
    private String targetUrl;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "ad_status", nullable = false)
    private AdvertisementStatus  adStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate(){
        if(createdAt == null){
            createdAt = LocalDateTime.now();
        }
        if(updatedAt == null){
            updatedAt = LocalDateTime.now();
        }

    }
    @PreUpdate
    public void onUpdate(){
            updatedAt = LocalDateTime.now();
    }

}
