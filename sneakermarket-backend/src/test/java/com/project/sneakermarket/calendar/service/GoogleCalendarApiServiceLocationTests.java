package com.project.sneakermarket.calendar.service;

import com.project.sneakermarket.calendar.entity.CalendarEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class GoogleCalendarApiServiceLocationTests {

    private GoogleCalendarApiService service;

    @BeforeEach
    void setUp() {
        GoogleCalendarOAuthService
                oauthService =
                mock(
                        GoogleCalendarOAuthService.class
                );

        service =
                new GoogleCalendarApiService(
                        oauthService
                );
    }

    @Test
    void venueAndAddressAreIncludedInLocation() {
        CalendarEvent event =
                createEvent();

        event.setVenueName(
                " 台北流行音樂中心 "
        );

        event.setAddress(
                " 台北市南港區市民大道八段99號 "
        );

        Map<String, Object> body =
                service.buildEventBody(
                        event,
                        60
                );

        assertEquals(
                "台北流行音樂中心，"
                        + "台北市南港區市民大道八段99號",
                body.get("location")
        );
    }

    @Test
    void venueOnlyIsIncludedInLocation() {
        CalendarEvent event =
                createEvent();

        event.setVenueName(
                "台北流行音樂中心"
        );

        Map<String, Object> body =
                service.buildEventBody(
                        event,
                        60
                );

        assertEquals(
                "台北流行音樂中心",
                body.get("location")
        );
    }

    @Test
    void addressOnlyIsIncludedInLocation() {
        CalendarEvent event =
                createEvent();

        event.setAddress(
                "台北市南港區市民大道八段99號"
        );

        Map<String, Object> body =
                service.buildEventBody(
                        event,
                        60
                );

        assertEquals(
                "台北市南港區市民大道八段99號",
                body.get("location")
        );
    }

    @Test
    void emptyLocationIsNotSentToGoogle() {
        CalendarEvent event =
                createEvent();

        event.setVenueName(" ");
        event.setAddress(null);

        Map<String, Object> body =
                service.buildEventBody(
                        event,
                        60
                );

        assertFalse(
                body.containsKey(
                        "location"
                )
        );
    }

    private CalendarEvent createEvent() {
        CalendarEvent event =
                new CalendarEvent();

        event.setEventId(100L);
        event.setTitle("測試活動");
        event.setDescription("測試說明");

        event.setStartAt(
                LocalDateTime.of(
                        2026,
                        10,
                        10,
                        14,
                        0
                )
        );

        event.setEndAt(
                LocalDateTime.of(
                        2026,
                        10,
                        10,
                        16,
                        0
                )
        );

        event.setStatus("ACTIVE");

        return event;
    }
}