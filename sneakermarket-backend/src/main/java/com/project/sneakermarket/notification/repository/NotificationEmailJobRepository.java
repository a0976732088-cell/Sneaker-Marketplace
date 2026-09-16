package com.project.sneakermarket.notification.repository;

import com.project.sneakermarket.notification.entity.NotificationEmailJob;
import com.project.sneakermarket.notification.entity.NotificationEmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.util.List;

public interface NotificationEmailJobRepository
        extends JpaRepository<NotificationEmailJob, Long> {

    boolean existsByNotificationId(Long notificationId);

    List<NotificationEmailJob>
    findTop50ByStatusOrderByCreatedAtAsc(
            NotificationEmailStatus status
    );

//    單筆鎖定查詢
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT job
    FROM NotificationEmailJob job
    WHERE job.id = :jobId
    """)
    Optional<NotificationEmailJob> findByIdForUpdate(
            @Param("jobId") Long jobId
    );
}