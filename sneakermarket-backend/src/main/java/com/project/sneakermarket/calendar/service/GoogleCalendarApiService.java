package com.project.sneakermarket.calendar.service;

import com.project.sneakermarket.calendar.dto.GoogleCalendarEventResponse;
import com.project.sneakermarket.calendar.entity.CalendarEvent;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "google.calendar.enabled",
        havingValue = "true"
)
public class GoogleCalendarApiService {

    private static final String EVENTS_URL =
            "https://www.googleapis.com/calendar/v3"
                    + "/calendars/primary/events";

    private static final ZoneId EVENT_ZONE =
            ZoneId.of("Asia/Taipei");

    private final GoogleCalendarOAuthService oauthService;

    private final RestClient restClient =
            RestClient.create();

    public String createEvent(
            Long memberId,
            CalendarEvent event,
            Integer remindBeforeMinutes
    ) {
        String accessToken =
                oauthService.getValidAccessToken(
                        memberId
                );

        GoogleCalendarEventResponse response =
                restClient.post()
                        .uri(EVENTS_URL)
                        .headers(headers ->
                                headers.setBearerAuth(
                                        accessToken
                                )
                        )
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .body(
                                buildEventBody(
                                        event,
                                        remindBeforeMinutes
                                )
                        )
                        .retrieve()
                        .body(
                                GoogleCalendarEventResponse.class
                        );

        if (response == null
                || response.id() == null
                || response.id().isBlank()) {
            throw new IllegalStateException(
                    "Google Calendar 未回傳行程 ID"
            );
        }

        return response.id();
    }

    public void updateEvent(
            Long memberId,
            String googleEventId,
            CalendarEvent event,
            Integer remindBeforeMinutes
    ) {
        String accessToken =
                oauthService.getValidAccessToken(
                        memberId
                );

        restClient.put()
                .uri(
                        EVENTS_URL + "/{googleEventId}",
                        googleEventId
                )
                .headers(headers ->
                        headers.setBearerAuth(
                                accessToken
                        )
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .body(
                        buildEventBody(
                                event,
                                remindBeforeMinutes
                        )
                )
                .retrieve()
                .toBodilessEntity();
    }

    public void deleteEvent(
            Long memberId,
            String googleEventId
    ) {
        String accessToken =
                oauthService.getValidAccessToken(
                        memberId
                );

        restClient.delete()
                .uri(
                        EVENTS_URL + "/{googleEventId}",
                        googleEventId
                )
                .headers(headers ->
                        headers.setBearerAuth(
                                accessToken
                        )
                )
                .retrieve()
                .onStatus(
                        status ->
                                status.value() == 404
                                        || status.value() == 410,
                        (request, response) -> {
                            /*
                             * Google 行程已不存在，
                             * 視同刪除成功。
                             */
                        }
                )
                .toBodilessEntity();
    }

    Map<String, Object> buildEventBody(
            CalendarEvent event,
            Integer remindBeforeMinutes
    ) {
        LocalDateTime startAt =
                event.getStartAt();

        LocalDateTime endAt =
                event.getEndAt();

        if (endAt == null
                || !endAt.isAfter(startAt)) {
            endAt = startAt.plusHours(1);
        }

        Map<String, Object> request =
                new LinkedHashMap<>();

        request.put(
                "summary",
                event.getTitle()
        );

        String description =
                buildDescription(event);

        if (!description.isBlank()) {
            request.put(
                    "description",
                    description
            );
        }

        String location =
                buildLocation(event);

        if (!location.isBlank()) {
            request.put(
                    "location",
                    location
            );
        }

        request.put(
                "start",
                buildDateTime(startAt)
        );

        request.put(
                "end",
                buildDateTime(endAt)
        );

        request.put(
                "reminders",
                Map.of(
                        "useDefault",
                        false,
                        "overrides",
                        List.of(
                                Map.of(
                                        "method",
                                        "popup",
                                        "minutes",
                                        remindBeforeMinutes
                                )
                        )
                )
        );

        return request;
    }

    private Map<String, String> buildDateTime(
            LocalDateTime dateTime
    ) {
        String formatted =
                dateTime.atZone(EVENT_ZONE)
                        .format(
                                DateTimeFormatter
                                        .ISO_OFFSET_DATE_TIME
                        );

        return Map.of(
                "dateTime",
                formatted,
                "timeZone",
                EVENT_ZONE.getId()
        );
    }

    private String buildLocation(
            CalendarEvent event
    ) {
        String venueName =
                event.getVenueName() != null
                        ? event.getVenueName().trim()
                        : "";

        String address =
                event.getAddress() != null
                        ? event.getAddress().trim()
                        : "";

        if (!venueName.isBlank()
                && !address.isBlank()) {
            return venueName
                    + "，"
                    + address;
        }

        if (!venueName.isBlank()) {
            return venueName;
        }

        return address;
    }

    private String buildDescription(
            CalendarEvent event
    ) {
        StringBuilder description =
                new StringBuilder();

        if (event.getDescription() != null
                && !event.getDescription().isBlank()) {
            description.append(
                    event.getDescription().trim()
            );
        }

        if (event.getTargetUrl() != null
                && !event.getTargetUrl().isBlank()) {

            if (!description.isEmpty()) {
                description.append(
                        System.lineSeparator()
                );
                description.append(
                        System.lineSeparator()
                );
            }

            description.append("活動網址：");
            description.append(
                    event.getTargetUrl().trim()
            );
        }

        return description.toString();
    }
}