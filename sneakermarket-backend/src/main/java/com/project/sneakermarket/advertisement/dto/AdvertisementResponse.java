package com.project.sneakermarket.advertisement.dto;

import com.project.sneakermarket.advertisement.entity.AdvertisementPlacement;
import com.project.sneakermarket.advertisement.entity.AdvertisementStatus;
import com.project.sneakermarket.advertisement.entity.AdvertisementTargetType;
import com.project.sneakermarket.advertisement.entity.AdvertisementType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdvertisementResponse {

    private Long adId;
    private String adName;
    private String imageUrl;
    private String adDescription;

    private AdvertisementType adType;
    private AdvertisementPlacement adPlacement;
    private AdvertisementTargetType targetType;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private Long targetId;
    private String targetUrl;

    private Integer sortOrder;
    private AdvertisementStatus adStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}
