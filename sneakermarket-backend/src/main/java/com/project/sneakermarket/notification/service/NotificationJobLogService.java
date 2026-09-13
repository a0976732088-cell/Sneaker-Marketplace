package com.project.sneakermarket.notification.service;

import com.project.sneakermarket.notification.entity.Notification;
import com.project.sneakermarket.notification.entity.NotificationJobStatus;
import com.project.sneakermarket.notification.entity.NotificationLog;
import com.project.sneakermarket.notification.repository.NotificationLogRepository;
import com.project.sneakermarket.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationJobLogService {

    private final NotificationRepository notificationRepo;
    private final NotificationLogRepository notificationLogRepo;

    /**
     * 通知加入 Redis 以前建立 PENDING 紀錄。
     */
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public Long createPendingLog(
            Long notificationId,
            String audienceType
    ) {
        Notification template =
                notificationRepo.findById(notificationId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "找不到通知，ID：" + notificationId
                                )
                        );

        if (template.getMemberId() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "會員專屬通知不能作為群發範本"
            );
        }

        if (Boolean.TRUE.equals(template.getIsSent())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "這則通知已經發送過"
            );
        }

        boolean activeJobExists =
                notificationLogRepo
                        .existsByNotificationIdAndStatusIn(
                                notificationId,
                                List.of(
                                        NotificationJobStatus.PENDING,
                                        NotificationJobStatus.PROCESSING
                                )
                        );

        if (activeJobExists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "這則通知已經在發送佇列中"
            );
        }

        NotificationLog log =
                new NotificationLog();

        log.setNotificationId(
                template.getNotificationId()
        );

        log.setSendType(audienceType);
        log.setTitle(template.getTitle());

        log.setTargetType(
                template.getTargetType() != null
                        ? template.getTargetType()
                        : "SYSTEM"
        );

        if ("EVENT_FOLLOWERS".equals(audienceType)) {
            log.setEventId(template.getTargetId());
        } else {
            log.setEventId(null);
        }

        log.setStatus(
                NotificationJobStatus.PENDING
        );

        log.setTotalRecipients(0);
        log.setTotalSent(0);
        log.setFailedCount(0);

        log.setStartedAt(null);
        log.setCompletedAt(null);
        log.setErrorMessage(null);

        NotificationLog savedLog =
                notificationLogRepo.save(log);

        return savedLog.getId();
    }

    /**
     * Consumer 開始執行任務。
     */
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void markProcessing(Long logId) {
        NotificationLog log =
                findLog(logId);

        log.setStatus(
                NotificationJobStatus.PROCESSING
        );

        log.setStartedAt(
                LocalDateTime.now()
        );

        log.setCompletedAt(null);
        log.setErrorMessage(null);

        notificationLogRepo.save(log);
    }

    /**
     * 通知全部發送成功。
     */
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void markCompleted(
            Long logId,
            int totalSent
    ) {
        NotificationLog log =
                findLog(logId);

        log.setStatus(
                NotificationJobStatus.COMPLETED
        );

        log.setTotalRecipients(totalSent);
        log.setTotalSent(totalSent);
        log.setFailedCount(0);

        log.setCompletedAt(
                LocalDateTime.now()
        );

        log.setErrorMessage(null);

        notificationLogRepo.save(log);
    }

    /**
     * Redis 加入失敗或 Consumer 執行失敗。
     */
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void markFailed(
            Long logId,
            String errorMessage
    ) {
        NotificationLog log =
                findLog(logId);

        log.setStatus(
                NotificationJobStatus.FAILED
        );

        log.setCompletedAt(
                LocalDateTime.now()
        );

        String message =
                errorMessage != null
                        ? errorMessage
                        : "未知錯誤";

        if (message.length() > 1000) {
            message = message.substring(0, 1000);
        }

        log.setErrorMessage(message);

        notificationLogRepo.save(log);
    }

    private NotificationLog findLog(Long logId) {
        return notificationLogRepo
                .findById(logId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "找不到通知任務紀錄，ID：" + logId
                        )
                );
    }
}