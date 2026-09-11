package com.project.sneakermarket.notification.repository;

import com.project.sneakermarket.notification.entity.Notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends
        JpaRepository<Notification, Long>,
        JpaSpecificationExecutor<Notification> {

    // 後台原本使用的查詢
    List<Notification> findByIsSentTrueOrderByCreatedAtDesc();

    // 前台：只取得目前會員的通知
    List<Notification>
    findByMemberIdAndIsSentTrueOrderByCreatedAtDesc(
            Long memberId
    );

    // 已讀操作：通知 ID 和會員 ID 必須同時符合
    Optional<Notification>
    findByNotificationIdAndMemberId(
            Long notificationId,
            Long memberId
    );

    boolean existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
            Long memberId,
            String notificationType,
            String targetType,
            Long targetId
    );
}