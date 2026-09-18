package com.project.sneakermarket.notification.controller;

import com.project.sneakermarket.notification.service.NotificationJobLogService;
import org.springframework.beans.factory.annotation.Value;
import com.project.sneakermarket.notification.dto.MemberNotificationResponse;
import com.project.sneakermarket.notification.dto.NotificationDTO;
import com.project.sneakermarket.notification.entity.Notification;
import com.project.sneakermarket.notification.queue.NotificationQueueProducer;
import com.project.sneakermarket.notification.service.NotificationService;
import com.project.sneakermarket.notification.service.NotificationSessionMemberResolver;
import com.project.sneakermarket.notification.dto.NotificationSendRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
public class NotificationController {

    @Value("${notification.redis.enabled:true}")
    private boolean redisEnabled;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationSessionMemberResolver sessionMemberResolver;

    @Autowired
    private NotificationQueueProducer notificationQueueProducer;

    @Autowired
    private NotificationJobLogService notificationJobLogService;

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
            @RequestParam(required = false) Boolean isSent,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startCreatedAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endCreatedAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startReadAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endReadAt,
            @RequestParam(defaultValue = "1") Integer pageNumber,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Page<Notification> result =
                notificationService.searchNotifications(
                        title,
                        notificationType,
                        targetType,
                        isRead,
                        isSent,
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
    public ResponseEntity<Map<String, Object>> sendNotification(
            @PathVariable("id") Long notificationId,
            @RequestBody NotificationSendRequest request
    ) {
        String audienceType =
                request.getAudienceType();

        /*
         * 先檢查發送對象是否合法，
         * 避免錯誤的任務進入 Redis。
         */
        boolean validAudienceType =
                "ALL_MEMBERS".equals(audienceType)
                        || "EVENT_FOLLOWERS".equals(audienceType);

        if (!validAudienceType) {
            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    "發送對象必須是 ALL_MEMBERS 或 EVENT_FOLLOWERS"
                            )
                    );
        }

        if (redisEnabled) {
            /*
             * 只有 Redis 模式才建立 PENDING 紀錄。
             */
            Long logId =
                    notificationJobLogService.createPendingLog(
                            notificationId,
                            audienceType
                    );

            try {
                String jobId =
                        notificationQueueProducer.enqueue(
                                notificationId,
                                audienceType,
                                logId
                        );

                return ResponseEntity
                        .status(HttpStatus.ACCEPTED)
                        .body(
                                Map.of(
                                        "message", "通知已加入發送佇列",
                                        "mode", "ASYNC",
                                        "jobId", jobId,
                                        "logId", logId,
                                        "notificationId", notificationId,
                                        "audienceType", audienceType
                                )
                        );

            } catch (RuntimeException exception) {
                /*
                 * PENDING 已建立，但 Redis 寫入失敗，
                 * 將紀錄改成 FAILED。
                 */
                notificationJobLogService.markFailed(
                        logId,
                        exception.getMessage()
                );

                throw exception;
            }
        }

        /*
         * Redis 關閉時，使用原本的同步發送。
         */
        int totalSent =
                notificationService.sendNotification(
                        notificationId,
                        request
                );

        return ResponseEntity.ok(
                Map.of(
                        "message", "通知發送成功",
                        "mode", "SYNC",
                        "notificationId", notificationId,
                        "audienceType", audienceType,
                        "totalSent", totalSent
                )
        );
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

    @PutMapping("/api/notification/read-all")
    public ResponseEntity<Void> markAllAsRead(
            HttpServletRequest request
    ) {
        Long memberId =
                sessionMemberResolver.requireMemberId(request);

        notificationService.markAllAsRead(memberId);

        return ResponseEntity.noContent().build();
    }
}
