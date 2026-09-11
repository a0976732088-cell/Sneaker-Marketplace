package com.project.sneakermarket.notification.controller;

import com.project.sneakermarket.calendar.dto.CalendarEventDTO;
import com.project.sneakermarket.notification.dto.MemberNotificationResponse;
import com.project.sneakermarket.notification.dto.NotificationDTO;
import com.project.sneakermarket.notification.entity.Notification;
import com.project.sneakermarket.notification.service.NotificationService;
import com.project.sneakermarket.notification.service.NotificationSessionMemberResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationSessionMemberResolver sessionMemberResolver;

    @PostMapping("/api/notification/createNotice")
    public ResponseEntity<Notification>  createNotice(@RequestBody NotificationDTO request){
        Notification created = notificationService.createNotice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/api/notification/NoticeUpdate/{id}")
    public ResponseEntity<?> updateNotice(
            @PathVariable("id") Long notificationId,
            @RequestBody NotificationDTO request){
        notificationService.updateNoticeById(notificationId, request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/api/notification/notificationDelete")
    public ResponseEntity<?> deleteNotification(@RequestParam Long notificationId){
        notificationService.deleteNoticeById(notificationId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/api/notification/notificationPage")
    public ResponseEntity<Page<Notification>> searchNotifications(
// ⏳ 待會員系統完成後解開註解：
            // @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String notificationType,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startCreatedAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endCreatedAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startReadAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endReadAt,
            @RequestParam(defaultValue = "1") Integer pageNumber,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Page<Notification> result = notificationService.searchNotifications(
                // memberId, // ⏳ 待會員系統完成後帶入
                title,
                notificationType,
                targetType,
                isRead,
                startCreatedAt,
                endCreatedAt,
                startReadAt,
                endReadAt,
                pageNumber,
                pageSize
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/notification/send/{id}")
    public ResponseEntity<?> sendNotification(@PathVariable("id") Long notificationId) {
        try {
            notificationService.sendNotification(notificationId);
            return ResponseEntity.ok("通知發送成功！");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("發送失敗：" + e.getMessage());
        }
    }

    /**
     * 前台： NotificationDrawer 撈取目前所有已發送通知
     */
    @GetMapping("/api/notification/noticeList")
    public ResponseEntity<List<MemberNotificationResponse>>
    getUserNotifications(HttpServletRequest request) {

        Long memberId =
                sessionMemberResolver.requireMemberId(request);

        return ResponseEntity.ok(
                notificationService.getMemberNotifications(memberId)
        );
    }

    @PutMapping("/api/notification/read/{id}")
    public ResponseEntity<Void> markAsRead(
            @PathVariable("id") Long notificationId,
            HttpServletRequest request
    ) {
        Long memberId =
                sessionMemberResolver.requireMemberId(request);

        notificationService.markAsRead(
                notificationId,
                memberId
        );

        return ResponseEntity.noContent().build();
    }
}
