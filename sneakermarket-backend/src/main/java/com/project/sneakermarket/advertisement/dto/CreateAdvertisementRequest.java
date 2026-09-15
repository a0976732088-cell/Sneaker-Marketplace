package com.project.sneakermarket.advertisement.dto;

import java.time.LocalDateTime;

import com.project.sneakermarket.advertisement.entity.AdvertisementPlacement;
import com.project.sneakermarket.advertisement.entity.AdvertisementTargetType;
import com.project.sneakermarket.advertisement.entity.AdvertisementType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class CreateAdvertisementRequest {

    @NotNull(message = "建立者會員編號不可為空")
    @Positive(message = "建立者會員編號必須大於 0")
    private Long createdBy;

    @NotBlank(message = "廣告名稱不可為空")
    @Size(max = 120, message = "廣告名稱不可超過 120 個字元")
    private String adName;

    @NotBlank(message = "圖片網址不可為空")
    @Size(max = 500, message = "圖片網址不可超過 500 個字元")
    private String imageUrl;

    @Size(max = 1000, message = "廣告說明不可超過 1000 個字元")
    private String adDescription;

    @NotNull(message = "廣告類型不可為空")
    private AdvertisementType adType;

    @NotNull(message = "廣告位置不可為空")
    private AdvertisementPlacement adPlacement;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @NotNull(message = "導向類型不可為空")
    private AdvertisementTargetType targetType;

    @Positive(message = "導向目標編號必須大於 0")
    private Long targetId;

    @Size(max = 500, message = "導向網址不可超過 500 個字元")
    private String targetUrl;

    @PositiveOrZero(message = "排序不可小於 0")
    private Integer sortOrder = 0;
}