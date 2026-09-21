package com.project.sneakermarket.notification.repository;

import com.project.sneakermarket.notification.entity.NotificationJobStatus;
import com.project.sneakermarket.notification.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

import java.util.Collection;

@Repository
public interface
NotificationLogRepository extends JpaRepository<NotificationLog, Long>,
        JpaSpecificationExecutor<NotificationLog> {

    boolean existsByNotificationIdAndStatusIn(
            Long notificationId,
            Collection<NotificationJobStatus> statuses
    );


//     計算指定任務狀態的紀錄數。
    long countByStatus(NotificationJobStatus status);

//     加總指定狀態任務的實際成功發送人數。
    @Query("""
        SELECT COALESCE(SUM(log.totalSent), 0)
        FROM NotificationLog log
        WHERE log.status = :status
        """)
    Long sumTotalSentByStatus(
            @Param("status") NotificationJobStatus status
    );

    List<NotificationLog> findByCreatedAtBetween(
            LocalDateTime start,
            LocalDateTime end
    );
}
