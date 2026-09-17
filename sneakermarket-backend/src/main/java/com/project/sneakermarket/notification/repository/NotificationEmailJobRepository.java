package com.project.sneakermarket.notification.repository;

import com.project.sneakermarket.notification.entity.NotificationEmailJob;
import com.project.sneakermarket.notification.entity.NotificationEmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;

import java.util.List;

public interface NotificationEmailJobRepository
        extends JpaRepository<NotificationEmailJob, Long> {

    boolean existsByNotificationId(Long notificationId);

    @Query("""
    SELECT job
    FROM NotificationEmailJob job
    WHERE job.status = :pending
       OR (job.status = :waiting AND job.nextRetryAt <= :now)
    ORDER BY job.createdAt ASC
    """)
    List<NotificationEmailJob> findReadyJobs(
            @Param("pending") NotificationEmailStatus pending,
            @Param("waiting") NotificationEmailStatus waiting,
            @Param("now") LocalDateTime now,
            Pageable pageable
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

    Page<NotificationEmailJob> findByStatusOrderByCreatedAtDesc(
            NotificationEmailStatus status,
            Pageable pageable
    );
}