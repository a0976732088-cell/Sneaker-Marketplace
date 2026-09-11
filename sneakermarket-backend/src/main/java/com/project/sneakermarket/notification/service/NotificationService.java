package com.project.sneakermarket.notification.service;


import com.project.sneakermarket.notification.dto.MemberNotificationResponse;
import com.project.sneakermarket.notification.dto.NotificationDTO;
import com.project.sneakermarket.notification.entity.Notification;
import com.project.sneakermarket.notification.entity.NotificationLog;
import com.project.sneakermarket.notification.repository.NotificationLogRepository;
import com.project.sneakermarket.notification.repository.NotificationRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.project.sneakermarket.calendar.entity.CalendarEvent;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final NotificationLogRepository notificationLogRepo;

    public Notification createNotice(NotificationDTO dto) {
        Notification notice = new Notification();
        notice.setNotificationType(dto.getNotificationType());
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setTargetType(dto.getTargetType());
        notice.setTargetId(dto.getTargetId());
        notice.setTargetUrl(dto.getTargetUrl());

        return notificationRepo.save(notice);
    }

    public void updateNoticeById(Long notificationId, NotificationDTO dto) {
        Optional<Notification> op = notificationRepo.findById(notificationId);
        if (op.isPresent()) {
            Notification notification = op.get();
            notification.setNotificationType(dto.getNotificationType());
            notification.setTitle(dto.getTitle());
            notification.setContent(dto.getContent());
            notification.setTargetType(dto.getTargetType());
            notification.setTargetId(dto.getTargetId());
            notification.setTargetUrl(dto.getTargetUrl());
            notificationRepo.save(notification);
        }
    }

    public void deleteNoticeById(Long notificationId) {
        notificationRepo.deleteById(notificationId);
    }

    public Page<Notification> searchNotifications(
            // Long memberId, // ⏳ 待會員系統完成後解開
            String title,
            String notificationType,
            String targetType,
            Boolean isRead,
            LocalDateTime startCreatedAt,
            LocalDateTime endCreatedAt,
            LocalDateTime startReadAt,
            LocalDateTime endReadAt,
            Integer pageNumber,
            Integer pageSize) {

        Specification<Notification> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            /* ⏳ 待會員系統完成後解开：
            if (memberId != null) {
                predicates.add(cb.equal(root.get("memberId"), memberId));
            }
            */
            // 1. 模糊搜尋通知標題
            if (title != null && !title.trim().isEmpty()) {
                predicates.add(cb.like(root.get("title"), "%" + title.trim() + "%"));
            }
            // 2. 精確匹配通知類型 (如: ORDER_CREATED, SYSTEM)
            if (notificationType != null && !notificationType.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("notificationType"), notificationType.trim()));
            }
            // 3. 精確匹配跳轉資料類型 (如: ORDER, AUCTION, EVENT)
            if (targetType != null && !targetType.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("targetType"), targetType.trim()));
            }
            // 4. 是否已讀
            if (isRead != null) {
                predicates.add(cb.equal(root.get("isRead"), isRead));
            }
            // 5. 通知產生時間區間 (createdAt >= startCreatedAt & <= endCreatedAt)
            if (startCreatedAt != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startCreatedAt));
            }
            if (endCreatedAt != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endCreatedAt));
            }
            // 6. 會員讀取通知時間區間 (readAt >= startReadAt & <= endReadAt)
            if (startReadAt != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("readAt"), startReadAt));
            }
            if (endReadAt != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("readAt"), endReadAt));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        // 分頁處理（第 1 頁轉成索引 0）
        int page = (pageNumber != null && pageNumber > 0) ? pageNumber - 1 : 0;
        int size = (pageSize != null && pageSize > 0) ? pageSize : 10;
        // 預設依照建立時間（最新到最舊）排序
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        return notificationRepo.findAll(spec, pageable);
    }

    // 發送通知邏輯
    @Transactional
    public void sendNotification(Long notificationId) {
        // A. 撈取原通知
        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("找不到該筆通知，ID: " + notificationId));

        // B. 更新 Notification 發送狀態
        notification.setIsSent(true);
        notification.setSentAt(LocalDateTime.now());
        notificationRepo.save(notification);

        // C. 新增 NotificationLog 紀錄
        NotificationLog log = new NotificationLog();
        log.setSendType("ALL"); // 發送對象暫定為所有人 (全體)
        log.setTitle(notification.getTitle());
        log.setTargetType(notification.getTargetType() != null ? notification.getTargetType() : "SYSTEM");

        // 如果 targetId 為空，可給預設值 0L，避免 DB Not Null 限制
        log.setEventId(notification.getTargetId() != null ? notification.getTargetId() : 0L);

        // 假設發送人數暫時填 1 或 預設全體人數（待會員系統整合）
        log.setTotalSent(1);

        // 寫入 Log 資料庫
        notificationLogRepo.save(log);
    }

    // 取得已發送通知
    public List<Notification> getSentNotifications() {
        return notificationRepo.findByIsSentTrueOrderByCreatedAtDesc();
    }

    @Transactional
    public void markAsRead(
            Long notificationId,
            Long memberId
    ) {
        Notification notification = notificationRepo
                .findByNotificationIdAndMemberId(
                        notificationId,
                        memberId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "找不到通知"
                ));

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
    }

    @Transactional
    public List<MemberNotificationResponse> getMemberNotifications(
            Long memberId
    ) {
        return notificationRepo
                .findByMemberIdAndIsSentTrueOrderByCreatedAtDesc(memberId)
                .stream()
                .map(MemberNotificationResponse::from)
                .toList();
    }

    @Transactional
    public void createEventTimeChangedNotifications(
            CalendarEvent event,
            LocalDateTime previousStartAt,
            List<Long> followerMemberIds
    ) {
        if (followerMemberIds == null
                || followerMemberIds.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

        String oldTime = previousStartAt.format(formatter);
        String newTime = event.getStartAt().format(formatter);

        List<Notification> notifications =
                followerMemberIds.stream()
                        .distinct()
                        .map(memberId -> {
                            Notification notification =
                                    new Notification();

                            notification.setMemberId(memberId);
                            notification.setNotificationType(
                                    "EVENT_TIME_CHANGED"
                            );

                            notification.setTitle(
                                    "活動時間異動：" + event.getTitle()
                            );

                            notification.setContent(
                                    "您關注的活動時間已從 "
                                            + oldTime
                                            + " 調整為 "
                                            + newTime
                                            + "。"
                            );

                            notification.setTargetType("EVENT");
                            notification.setTargetId(
                                    event.getEventId()
                            );
                            notification.setTargetUrl(
                                    event.getTargetUrl()
                            );

                            notification.setIsRead(false);
                            notification.setIsSent(true);
                            notification.setSentAt(now);

                            return notification;
                        })
                        .toList();

        notificationRepo.saveAll(notifications);
    }

    @Transactional
    public void createEventCancelledNotifications(
            CalendarEvent event,
            List<Long> followerMemberIds
    ) {
        if (followerMemberIds == null
                || followerMemberIds.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

        String originalStartTime =
                event.getStartAt().format(formatter);

        List<Notification> notifications =
                followerMemberIds.stream()
                        .distinct()
                        .map(memberId -> {
                            Notification notification =
                                    new Notification();

                            notification.setMemberId(memberId);
                            notification.setNotificationType(
                                    "EVENT_CANCELLED"
                            );

                            notification.setTitle(
                                    "活動已取消：" + event.getTitle()
                            );

                            notification.setContent(
                                    "您關注的活動原訂於 "
                                            + originalStartTime
                                            + " 開始，目前已由管理員取消。"
                            );

                            /*
                             * 取消後前台不再顯示活動，
                             * 所以通知只保留 eventId，不提供跳轉。
                             */
                            notification.setTargetType(
                                    "EVENT_CANCELLED"
                            );
                            notification.setTargetId(
                                    event.getEventId()
                            );
                            notification.setTargetUrl(null);

                            notification.setIsRead(false);
                            notification.setIsSent(true);
                            notification.setSentAt(now);

                            return notification;
                        })
                        .toList();

        notificationRepo.saveAll(notifications);
    }
}


