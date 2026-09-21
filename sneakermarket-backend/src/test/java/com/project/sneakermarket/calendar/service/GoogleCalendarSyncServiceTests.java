package com.project.sneakermarket.calendar.service;

import com.project.sneakermarket.calendar.entity.CalendarEvent;
import com.project.sneakermarket.calendar.entity.EventFollow;
import com.project.sneakermarket.calendar.entity.GoogleCalendarEventLink;
import com.project.sneakermarket.calendar.repository.CalendarEventRepository;
import com.project.sneakermarket.calendar.repository.EventFollowRepository;
import com.project.sneakermarket.calendar.repository.GoogleCalendarConnectionRepository;
import com.project.sneakermarket.calendar.repository.GoogleCalendarEventLinkRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GoogleCalendarSyncServiceTests {

    private GoogleCalendarConnectionRepository
            connectionRepository;

    private GoogleCalendarEventLinkRepository
            eventLinkRepository;

    private EventFollowRepository
            eventFollowRepository;

    private CalendarEventRepository
            calendarEventRepository;

    private ObjectProvider<GoogleCalendarApiService>
            apiServiceProvider;

    private GoogleCalendarApiService
            apiService;

    private GoogleCalendarSyncService
            service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        connectionRepository =
                mock(
                        GoogleCalendarConnectionRepository.class
                );

        eventLinkRepository =
                mock(
                        GoogleCalendarEventLinkRepository.class
                );

        eventFollowRepository =
                mock(
                        EventFollowRepository.class
                );

        calendarEventRepository =
                mock(
                        CalendarEventRepository.class
                );

        apiServiceProvider =
                mock(ObjectProvider.class);

        apiService =
                mock(GoogleCalendarApiService.class);

        service =
                new GoogleCalendarSyncService(
                        connectionRepository,
                        eventLinkRepository,
                        eventFollowRepository,
                        calendarEventRepository,
                        apiServiceProvider
                );
    }

    @Test
    void memberWithoutGoogleConnectionIsSkipped() {
        CalendarEvent event =
                createEvent();

        when(
                connectionRepository
                        .existsByMemberId(1L)
        ).thenReturn(false);

        service.syncFollow(
                1L,
                event,
                30
        );

        verifyNoInteractions(
                apiServiceProvider
        );

        verifyNoInteractions(
                eventLinkRepository
        );

        verifyNoInteractions(
                apiService
        );
    }

    @Test
    void newFollowCreatesGoogleEventAndLink() {
        CalendarEvent event =
                createEvent();

        when(
                connectionRepository
                        .existsByMemberId(1L)
        ).thenReturn(true);

        when(
                apiServiceProvider
                        .getIfAvailable()
        ).thenReturn(apiService);

        when(
                eventLinkRepository
                        .findByMemberIdAndEventId(
                                1L,
                                100L
                        )
        ).thenReturn(Optional.empty());

        when(
                apiService.createEvent(
                        1L,
                        event,
                        30
                )
        ).thenReturn("google-event-100");

        service.syncFollow(
                1L,
                event,
                30
        );

        ArgumentCaptor<GoogleCalendarEventLink> captor =
                ArgumentCaptor.forClass(
                        GoogleCalendarEventLink.class
                );

        verify(
                eventLinkRepository
        ).save(
                captor.capture()
        );

        GoogleCalendarEventLink saved =
                captor.getValue();

        assertEquals(
                1L,
                saved.getMemberId()
        );

        assertEquals(
                100L,
                saved.getEventId()
        );

        assertEquals(
                "google-event-100",
                saved.getGoogleEventId()
        );

        assertEquals(
                "SYNCED",
                saved.getSyncStatus()
        );

        assertNull(
                saved.getLastError()
        );

        assertNotNull(
                saved.getLastSyncedAt()
        );
    }

    @Test
    void existingLinkUpdatesOriginalGoogleEvent() {
        CalendarEvent event =
                createEvent();

        GoogleCalendarEventLink link =
                createLink();

        when(
                connectionRepository
                        .existsByMemberId(1L)
        ).thenReturn(true);

        when(
                apiServiceProvider
                        .getIfAvailable()
        ).thenReturn(apiService);

        when(
                eventLinkRepository
                        .findByMemberIdAndEventId(
                                1L,
                                100L
                        )
        ).thenReturn(
                Optional.of(link)
        );

        service.syncFollow(
                1L,
                event,
                60
        );

        verify(
                apiService
        ).updateEvent(
                1L,
                "google-event-100",
                event,
                60
        );

        verify(
                apiService,
                never()
        ).createEvent(
                any(),
                any(),
                any()
        );

        ArgumentCaptor<GoogleCalendarEventLink> captor =
                ArgumentCaptor.forClass(
                        GoogleCalendarEventLink.class
                );

        verify(
                eventLinkRepository
        ).save(
                captor.capture()
        );

        assertSame(
                link,
                captor.getValue()
        );

        assertEquals(
                "SYNCED",
                link.getSyncStatus()
        );

        assertNull(
                link.getLastError()
        );
    }

    @Test
    void googleFailureDoesNotBreakSiteFollow() {
        CalendarEvent event =
                createEvent();

        when(
                connectionRepository
                        .existsByMemberId(1L)
        ).thenReturn(true);

        when(
                apiServiceProvider
                        .getIfAvailable()
        ).thenReturn(apiService);

        when(
                eventLinkRepository
                        .findByMemberIdAndEventId(
                                1L,
                                100L
                        )
        ).thenReturn(Optional.empty());

        when(
                apiService.createEvent(
                        1L,
                        event,
                        30
                )
        ).thenThrow(
                new RuntimeException(
                        "Google API 暫時無法使用"
                )
        );

        assertDoesNotThrow(() ->
                service.syncFollow(
                        1L,
                        event,
                        30
                )
        );

        verify(
                eventLinkRepository,
                never()
        ).save(any());
    }

    @Test
    void unfollowDeletesGoogleEventAndLocalLink() {
        GoogleCalendarEventLink link =
                createLink();

        when(
                eventLinkRepository
                        .findByMemberIdAndEventId(
                                1L,
                                100L
                        )
        ).thenReturn(
                Optional.of(link)
        );

        when(
                connectionRepository
                        .existsByMemberId(1L)
        ).thenReturn(true);

        when(
                apiServiceProvider
                        .getIfAvailable()
        ).thenReturn(apiService);

        service.removeFollow(
                1L,
                100L
        );

        verify(
                apiService
        ).deleteEvent(
                1L,
                "google-event-100"
        );

        verify(
                eventLinkRepository
        ).delete(link);
    }

    @Test
    void authorizationSyncsExistingFutureFollow() {
        CalendarEvent event =
                createEvent();

        EventFollow follow =
                new EventFollow();

        follow.setMemberId(1L);
        follow.setEventId(100L);
        follow.setRemindBeforeMinutes(45);
        follow.setReminded(false);

        when(
                eventFollowRepository
                        .findByMemberIdOrderByCreatedAtDesc(
                                1L
                        )
        ).thenReturn(
                List.of(follow)
        );

        when(
                calendarEventRepository.findById(
                        100L
                )
        ).thenReturn(
                Optional.of(event)
        );

        when(
                connectionRepository
                        .existsByMemberId(1L)
        ).thenReturn(true);

        when(
                apiServiceProvider
                        .getIfAvailable()
        ).thenReturn(apiService);

        when(
                eventLinkRepository
                        .findByMemberIdAndEventId(
                                1L,
                                100L
                        )
        ).thenReturn(Optional.empty());

        when(
                apiService.createEvent(
                        1L,
                        event,
                        45
                )
        ).thenReturn(
                "google-event-100"
        );

        service.syncExistingFollows(
                1L
        );

        verify(
                apiService
        ).createEvent(
                1L,
                event,
                45
        );
    }

    private CalendarEvent createEvent() {
        CalendarEvent event =
                new CalendarEvent();

        event.setEventId(100L);
        event.setTitle("測試活動");
        event.setDescription("測試活動說明");

        event.setStartAt(
                LocalDateTime.now()
                        .plusDays(2)
        );

        event.setEndAt(
                LocalDateTime.now()
                        .plusDays(2)
                        .plusHours(2)
        );

        event.setStatus("ACTIVE");
        event.setTargetUrl(
                "http://localhost:5173/events/100"
        );

        return event;
    }

    private GoogleCalendarEventLink createLink() {
        GoogleCalendarEventLink link =
                new GoogleCalendarEventLink();

        link.setLinkId(10L);
        link.setMemberId(1L);
        link.setEventId(100L);

        link.setGoogleEventId(
                "google-event-100"
        );

        link.setSyncStatus("SYNCED");
        link.setLastSyncedAt(
                LocalDateTime.now()
        );

        return link;
    }
}