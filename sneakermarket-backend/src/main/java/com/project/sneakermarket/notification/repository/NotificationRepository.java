package com.project.sneakermarket.notification.repository;

import com.project.sneakermarket.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends
        JpaRepository<Notification, Long>,
        JpaSpecificationExecutor<Notification> {

    List<Notification>
    findByIsSentTrueOrderByCreatedAtDesc();

    /*
     * 會員通知抽屜只顯示允許站內通知的資料。
     */
    List<Notification>
    findByMemberIdAndInAppVisibleTrueAndIsSentTrueOrderByCreatedAtDesc(
            Long memberId
    );

    /*
     * 會員只能讀取自己可見的站內通知。
     */
    Optional<Notification>
    findByNotificationIdAndMemberIdAndInAppVisibleTrue(
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
