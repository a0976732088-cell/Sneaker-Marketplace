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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EventReminderServiceTests {

    private EventFollowRepository
            eventFollowRepository;

    private CalendarEventRepository
            calendarEventRepository;

    private NotificationRepository
            notificationRepository;

    private MemberRepository
            memberRepository;

    private NotificationEmailJobService
            emailJobService;

    private NotificationPreferencePolicyService
            preferencePolicyService;

    private EventReminderService
            service;

    private LocalDateTime now;

    private EventFollow follow;

    private CalendarEvent event;

    @BeforeEach
    void setUp() {
        eventFollowRepository =
                mock(EventFollowRepository.class);

        calendarEventRepository =
                mock(CalendarEventRepository.class);

        notificationRepository =
                mock(NotificationRepository.class);

        memberRepository =
                mock(MemberRepository.class);

        emailJobService =
                mock(NotificationEmailJobService.class);

        preferencePolicyService =
                mock(
                        NotificationPreferencePolicyService.class
                );

        service =
                new EventReminderService(
                        eventFollowRepository,
                        calendarEventRepository,
                        notificationRepository,
                        memberRepository,
                        emailJobService,
                        preferencePolicyService
                );

        now =
                LocalDateTime.of(
                        2026,
                        9,
                        20,
                        12,
                        0
                );

        follow = createFollow();
        event = createEvent();

        when(
                eventFollowRepository
                        .findByIdForUpdate(10L)
        ).thenReturn(
                Optional.of(follow)
        );

        when(
                calendarEventRepository.findById(
                        100L
                )
        ).thenReturn(
                Optional.of(event)
        );
    }

    @Test
    void reminderCreatesNotificationAndEmailJob() {
        when(
                notificationRepository
                        .existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
                                1L,
                                "EVENT_REMINDER",
                                "EVENT",
                                100L
                        )
        ).thenReturn(false);

        when(
                preferencePolicyService.resolve(
                        1L,
                        "EVENT_REMINDER",
                        "EVENT"
                )
        ).thenReturn(
                new DeliveryDecision(
                        true,
                        true
                )
        );

        when(
                notificationRepository.save(
                        any(Notification.class)
                )
        ).thenAnswer(invocation -> {
            Notification notification =
                    invocation.getArgument(0);

            notification.setNotificationId(
                    500L
            );

            return notification;
        });

        Member member =
                new Member();

        member.setMemberId(1L);
        member.setEmail(
                "member@example.com"
        );

        when(
                memberRepository.findById(1L)
        ).thenReturn(
                Optional.of(member)
        );

        service.createReminder(
                10L,
                now
        );

        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(
                        Notification.class
                );

        verify(
                notificationRepository
        ).save(
                captor.capture()
        );

        Notification saved =
                captor.getValue();

        assertEquals(
                1L,
                saved.getMemberId()
        );

        assertEquals(
                "EVENT_REMINDER",
                saved.getNotificationType()
        );

        assertEquals(
                "EVENT",
                saved.getTargetType()
        );

        assertEquals(
                100L,
                saved.getTargetId()
        );

        assertEquals(
                "/events/100",
                saved.getTargetUrl()
        );

        assertTrue(
                saved.getInAppVisible()
        );

        assertTrue(
                saved.getIsSent()
        );

        assertFalse(
                saved.getIsRead()
        );

        assertEquals(
                now,
                saved.getSentAt()
        );

        verify(
                emailJobService
        ).createPending(
                500L,
                1L,
                "member@example.com"
        );

        assertTrue(
                follow.getReminded()
        );
    }

    @Test
    void disabledChannelsSkipNotificationButFinishReminder() {
        when(
                notificationRepository
                        .existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
                                1L,
                                "EVENT_REMINDER",
                                "EVENT",
                                100L
                        )
        ).thenReturn(false);

        when(
                preferencePolicyService.resolve(
                        1L,
                        "EVENT_REMINDER",
                        "EVENT"
                )
        ).thenReturn(
                DeliveryDecision.none()
        );

        service.createReminder(
                10L,
                now
        );

        verify(
                notificationRepository,
                never()
        ).save(any());

        verifyNoInteractions(
                memberRepository
        );

        verifyNoInteractions(
                emailJobService
        );

        assertTrue(
                follow.getReminded()
        );
    }

    @Test
    void existingReminderIsNotCreatedTwice() {
        when(
                notificationRepository
                        .existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
                                1L,
                                "EVENT_REMINDER",
                                "EVENT",
                                100L
                        )
        ).thenReturn(true);

        service.createReminder(
                10L,
                now
        );

        verify(
                notificationRepository,
                never()
        ).save(any());

        verifyNoInteractions(
                preferencePolicyService
        );

        verifyNoInteractions(
                emailJobService
        );

        assertTrue(
                follow.getReminded()
        );
    }

    @Test
    void emailDisabledCreatesOnlyInAppNotification() {
        when(
                notificationRepository
                        .existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
                                1L,
                                "EVENT_REMINDER",
                                "EVENT",
                                100L
                        )
        ).thenReturn(false);

        when(
                preferencePolicyService.resolve(
                        1L,
                        "EVENT_REMINDER",
                        "EVENT"
                )
        ).thenReturn(
                new DeliveryDecision(
                        true,
                        false
                )
        );

        when(
                notificationRepository.save(
                        any(Notification.class)
                )
        ).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        service.createReminder(
                10L,
                now
        );

        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(
                        Notification.class
                );

        verify(
                notificationRepository
        ).save(
                captor.capture()
        );

        assertTrue(
                captor.getValue()
                        .getInAppVisible()
        );

        verifyNoInteractions(
                memberRepository
        );

        verifyNoInteractions(
                emailJobService
        );

        assertTrue(
                follow.getReminded()
        );
    }

    @Test
    void alreadyRemindedFollowIsSkipped() {
        follow.setReminded(true);

        service.createReminder(
                10L,
                now
        );

        verifyNoInteractions(
                calendarEventRepository
        );

        verifyNoInteractions(
                notificationRepository
        );

        verifyNoInteractions(
                preferencePolicyService
        );

        verifyNoInteractions(
                emailJobService
        );
    }

    private EventFollow createFollow() {
        EventFollow result =
                new EventFollow();

        result.setFollowId(10L);
        result.setMemberId(1L);
        result.setEventId(100L);
        result.setRemindBeforeMinutes(30);
        result.setReminded(false);

        return result;
    }

    private CalendarEvent createEvent() {
        CalendarEvent result =
                new CalendarEvent();

        result.setEventId(100L);
        result.setTitle("球鞋文化交流日");

        result.setStartAt(
                LocalDateTime.of(
                        2026,
                        9,
                        20,
                        14,
                        0
                )
        );

        result.setEndAt(
                LocalDateTime.of(
                        2026,
                        9,
                        20,
                        17,
                        0
                )
        );

        result.setStatus("ACTIVE");
        result.setTargetUrl(
                "/events/100"
        );

        return result;
    }
}