package com.project.sneakermarket.notification.service;


import com.project.sneakermarket.notification.dto.NotificationDTO;
import com.project.sneakermarket.notification.entity.Notification;
import com.project.sneakermarket.notification.repository.NotificationRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepo;

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
}

