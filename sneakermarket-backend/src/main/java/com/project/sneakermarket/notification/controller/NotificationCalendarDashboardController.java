package com.project.sneakermarket.notification.controller;

import com.project.sneakermarket.notification.dto.NotificationCalendarDashboardDTO;

import com.project.sneakermarket.notification.service.NotificationCalendarDashboardService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class NotificationCalendarDashboardController {

    private final NotificationCalendarDashboardService dashboardService;

    /**
     * 取得通知與行事曆後台首頁統計。
     */
    @GetMapping("/notification-calendar")
    public ResponseEntity<NotificationCalendarDashboardDTO>
    getNotificationCalendarDashboard() {

        NotificationCalendarDashboardDTO dashboard =
                dashboardService.getDashboard();

        return ResponseEntity.ok(dashboard);
    }
}
