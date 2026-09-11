package com.project.sneakermarket.notification.service;

import com.project.sneakermarket.calendar.entity.CalendarEvent;
import com.project.sneakermarket.calendar.entity.EventFollow;
import com.project.sneakermarket.calendar.repository.CalendarEventRepository;
import com.project.sneakermarket.calendar.repository.EventFollowRepository;
import com.project.sneakermarket.notification.entity.Notification;
import com.project.sneakermarket.notification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EventReminderService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    private final EventFollowRepository eventFollowRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final NotificationRepository notificationRepository;

    /**
     * 每一筆關注各自使用一個 Transaction。
     *
     * 通知建立失敗時，reminded 不會被更新；
     * 下一輪排程還可以重新處理。
     */
    @Transactional
    public void createReminder(
            Long followId,
            LocalDateTime now
    ) {
        EventFollow follow = eventFollowRepository
                .findByIdForUpdate(followId)
                .orElse(null);

        if (follow == null
                || Boolean.TRUE.equals(follow.getReminded())) {
            return;
        }

        CalendarEvent event = calendarEventRepository
                .findById(follow.getEventId())
                .orElse(null);

        /*
         * 活動不存在時通常代表資料異常。
         * 標記完成，避免排程永遠重複查詢。
         */
        if (event == null) {
            follow.setReminded(true);
            return;
        }

        /*
         * 活動已開始或已過期，不補發過期提醒。
         */
        if (!event.getStartAt().isAfter(now)) {
            follow.setReminded(true);
            return;
        }

        /*
         * 再次確認活動目前可以發送提醒。
         */
        if ("INACTIVE".equals(event.getStatus())
                || "DRAFT".equals(event.getStatus())
                || "CANCELLED".equals(event.getStatus())) {
            return;
        }

        boolean alreadyExists =
                notificationRepository
                        .existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
                                follow.getMemberId(),
                                "EVENT_REMINDER",
                                "EVENT",
                                event.getEventId()
                        );

        if (!alreadyExists) {
            Notification notification = new Notification();

            notification.setMemberId(follow.getMemberId());
            notification.setNotificationType("EVENT_REMINDER");
            notification.setTitle(
                    "活動即將開始：" + event.getTitle()
            );
            notification.setContent(
                    "您關注的活動將於 "
                            + event.getStartAt().format(DATE_TIME_FORMATTER)
                            + " 開始。"
            );
            notification.setTargetType("EVENT");
            notification.setTargetId(event.getEventId());
            notification.setTargetUrl(event.getTargetUrl());

            notification.setIsRead(false);
            notification.setIsSent(true);
            notification.setSentAt(now);

            notificationRepository.save(notification);
        }

        /*
         * 不管是剛建立，或通知已經存在，
         * 都代表這筆關注不需要再次提醒。
         */
        follow.setReminded(true);
    }
}