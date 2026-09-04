package com.project.sneakermarket.advertisement.scheduler;

import com.project.sneakermarket.advertisement.service.AdvertisementService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdvertisementStatusScheduler {

    private final AdvertisementService advertisementService;

    // 每分鐘的第 0 秒執行一次
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Taipei")
    public void updateExpiredAdvertisements() {
        advertisementService.expireEndedAdvertisements();
    }
}