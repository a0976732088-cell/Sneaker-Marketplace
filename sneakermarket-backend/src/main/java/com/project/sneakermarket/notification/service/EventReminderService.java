package com.project.sneakermarket.notification.service;

import com.project.sneakermarket.calendar.entity.CalendarEvent;
import com.project.sneakermarket.calendar.entity.EventFollow;
import com.project.sneakermarket.calendar.repository.CalendarEventRepository;
import com.project.sneakermarket.calendar.repository.EventFollowRepository;
import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.member.repository.MemberRepository;
import com.project.sneakermarket.notification.entity.Notification;
import com.project.sneakermarket.notification.repository.NotificationRepository;
import com.project.sneakermarket.notification.service.NotificationPreferencePolicyService.DeliveryDecision;
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
    private final MemberRepository memberRepository;
    private final NotificationEmailJobService emailJobService;
    private final NotificationPreferencePolicyService preferencePolicyService;

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

        if (event == null) {
            follow.setReminded(true);
            return;
        }

        if (!event.getStartAt().isAfter(now)) {
            follow.setReminded(true);
            return;
        }

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

        if (alreadyExists) {
            follow.setReminded(true);
            return;
        }

        DeliveryDecision decision =
                preferencePolicyService.resolve(
                        follow.getMemberId(),
                        "EVENT_REMINDER",
                        "EVENT"
                );

        /*
         * 會員兩種管道都關閉時不建立通知，
         * 但仍標記 reminded，避免排程每次重複處理。
         */
        if (!decision.anyEnabled()) {
            follow.setReminded(true);
            return;
        }

        Notification notification = new Notification();

        notification.setMemberId(follow.getMemberId());
        notification.setNotificationType("EVENT_REMINDER");

        notification.setTitle(
                "活動即將開始：" + event.getTitle()
        );

        notification.setContent(
                "您關注的活動將於 "
                        + event.getStartAt().format(
                        DATE_TIME_FORMATTER
                )
                        + " 開始。"
        );

        notification.setTargetType("EVENT");
        notification.setTargetId(event.getEventId());
        notification.setTargetUrl(event.getTargetUrl());

        notification.setIsRead(false);
        notification.setIsSent(true);
        notification.setSentAt(now);

        notification.setInAppVisible(
                decision.inAppEnabled()
        );

        Notification saved =
                notificationRepository.save(notification);

        if (decision.emailEnabled()) {
            Member member = memberRepository
                    .findById(follow.getMemberId())
                    .orElse(null);

            if (member != null) {
                emailJobService.createPending(
                        saved.getNotificationId(),
                        member.getMemberId(),
                        member.getEmail()
                );
            }
        }

        follow.setReminded(true);
    }
}