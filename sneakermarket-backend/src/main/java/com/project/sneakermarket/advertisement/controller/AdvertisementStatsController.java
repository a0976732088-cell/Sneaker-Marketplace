package com.project.sneakermarket.advertisement.controller;

import com.project.sneakermarket.advertisement.service.AdvertisementStatsService;
import com.project.sneakermarket.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/ads") // 職責分離，點擊曝光為前台觸發
@RequiredArgsConstructor
public class AdvertisementStatsController {
    private final AdvertisementStatsService advertisementStatsService;

    @PostMapping("/{adId}/views")
    public ResponseEntity<ApiResponse<Void>> recordView(
            @PathVariable Long adId,
            HttpServletRequest request
    ) {
        // 即使訪客沒登入，也建立一個匿名 Session
        HttpSession session = request.getSession(true);

        advertisementStatsService.recordView(adId, session);

        return ResponseEntity.ok(
                ApiResponse.ok("廣告曝光紀錄成功", null)
        );
    }

    @PostMapping("/{adId}/clicks")
    public ResponseEntity<ApiResponse<Void>> recordClick(
            @PathVariable Long adId,
            HttpServletRequest request
    ) {
        // 取得訪客或會員目前使用的 Session
        HttpSession session = request.getSession(true);

        advertisementStatsService.recordClick(adId, session);

        return ResponseEntity.ok(
                ApiResponse.ok("廣告點擊紀錄成功", null)
        );
    }
}
