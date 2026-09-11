package com.project.sneakermarket.calendar.repository;

import com.project.sneakermarket.calendar.entity.EventFollow;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventFollowRepository
        extends JpaRepository<EventFollow, Long> {

    Optional<EventFollow> findByMemberIdAndEventId(
            Long memberId,
            Long eventId
    );

    List<EventFollow> findByEventId(Long eventId);

    List<EventFollow> findByMemberIdOrderByCreatedAtDesc(
            Long memberId
    );

    boolean existsByMemberIdAndEventId(
            Long memberId,
            Long eventId
    );

    void deleteByMemberIdAndEventId(
            Long memberId,
            Long eventId
    );

    //會員已收到同活動 EVENT_REMINDER
    //→ 即使取消關注後又重新關注
    //→ 不會再次產生相同通知
    /**
     * 找出已進入提醒時間、尚未產生通知的關注資料。
     *
     * 使用 SQL Server DATEADD：
     * 活動時間 - 提前提醒分鐘 <= 現在時間
     */
    @Query(
            value = """
                    SELECT TOP (100) f.follow_id
                    FROM dbo.event_follows f
                    INNER JOIN dbo.calendar_events e
                        ON e.event_id = f.event_id
                    WHERE f.reminded = 0
                      AND e.status NOT IN ('INACTIVE', 'DRAFT')
                      AND DATEADD(
                            MINUTE,
                            -f.remind_before_minutes,
                            e.start_at
                          ) <= :now
                    ORDER BY e.start_at, f.follow_id
                    """,
            nativeQuery = true
    )
    List<Long> findDueReminderIds(
            @Param("now") LocalDateTime now
    );

    /**
     * 鎖定單筆關注資料，避免同一筆提醒同時被重複處理。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT f
            FROM EventFollow f
            WHERE f.followId = :followId
            """)
    Optional<EventFollow> findByIdForUpdate(
            @Param("followId") Long followId
    );



}