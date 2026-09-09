package com.project.sneakermarket.notification.controller;

import com.project.sneakermarket.notification.dto.NotificationLogDTO;
import com.project.sneakermarket.notification.service.NotificationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationLogController {

    @Autowired
    private NotificationLogService notificationLogService;

    @CrossOrigin
    @GetMapping("/api/notificationLog/searchlogs")
    public ResponseEntity<?> getNotificationLogs(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String sendType,
            @RequestParam(defaultValue = "1") Integer pageNumber,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        try {
            Page<NotificationLogDTO> result = notificationLogService.searchLogs(title, sendType, pageNumber, pageSize);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace(); // 👈 可以在 IntelliJ Console 看到完整的錯誤細節
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("伺服器錯誤細節: " + e.getMessage());
        }
    }
}
