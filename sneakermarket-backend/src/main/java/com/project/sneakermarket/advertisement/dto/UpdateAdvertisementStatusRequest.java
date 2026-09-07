package com.project.sneakermarket.advertisement.dto;

import com.project.sneakermarket.advertisement.entity.AdvertisementStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAdvertisementStatusRequest {

    @NotNull(message = "廣告狀態不可為空")
    private AdvertisementStatus adStatus;
}
