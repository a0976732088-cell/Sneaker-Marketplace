package com.project.sneakermarket.calendar.service;

import com.project.sneakermarket.calendar.entity.CalendarEvent;
import com.project.sneakermarket.calendar.entity.EventFollow;
import com.project.sneakermarket.calendar.entity.GoogleCalendarEventLink;
import com.project.sneakermarket.calendar.repository.EventFollowRepository;
import com.project.sneakermarket.calendar.repository.GoogleCalendarConnectionRepository;
import com.project.sneakermarket.calendar.repository.GoogleCalendarEventLinkRepository;
import com.project.sneakermarket.calendar.repository.CalendarEventRepository;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor

public class GoogleCalendarSyncService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GoogleCalendarSyncService.class
            );

    private final GoogleCalendarConnectionRepository
            connectionRepository;

    private final GoogleCalendarEventLinkRepository
            eventLinkRepository;

    private final EventFollowRepository
            eventFollowRepository;

    private final CalendarEventRepository
            calendarEventRepository;

    private final ObjectProvider<GoogleCalendarApiService>
            apiServiceProvider;

    /**
     * Google 授權完成後，
     * 將會員原本已關注且尚未開始的活動補進 Google Calendar。
     */
    @Transactional
    public void syncExistingFollows(
            Long memberId
    ) {
        List<EventFollow> follows =
                eventFollowRepository
                        .findByMemberIdOrderByCreatedAtDesc(
                                memberId
                        );

        LocalDateTime now =
                LocalDateTime.now();

        for (EventFollow follow : follows) {
            calendarEventRepository
                    .findById(
                            follow.getEventId()
                    )
                    .filter(event ->
                            event.getStartAt() != null
                                    && event.getStartAt()
                                    .isAfter(now)
                    )
                    .filter(this::canRemainOnCalendar)
                    .ifPresent(event ->
                            syncFollow(
                                    memberId,
                                    event,
                                    follow.getRemindBeforeMinutes()
                            )
                    );
        }
    }

    @Transactional
    public void syncFollow(
            Long memberId,
            CalendarEvent event,
            Integer remindBeforeMinutes
    ) {
        if (!connectionRepository.existsByMemberId(
                memberId
        )) {
            return;
        }

        GoogleCalendarApiService apiService =
                apiServiceProvider.getIfAvailable();

        if (apiService == null) {
            return;
        }

        Optional<GoogleCalendarEventLink> existingLink =
                eventLinkRepository
                        .findByMemberIdAndEventId(
                                memberId,
                                event.getEventId()
                        );

        try {
            if (existingLink.isPresent()) {
                GoogleCalendarEventLink link =
                        existingLink.get();

                apiService.updateEvent(
                        memberId,
                        link.getGoogleEventId(),
                        event,
                        remindBeforeMinutes
                );

                markSynced(link);
                eventLinkRepository.save(link);

                return;
            }

            String googleEventId =
                    apiService.createEvent(
                            memberId,
                            event,
                            remindBeforeMinutes
                    );

            GoogleCalendarEventLink link =
                    new GoogleCalendarEventLink();

            link.setMemberId(memberId);
            link.setEventId(event.getEventId());
            link.setGoogleEventId(googleEventId);

            markSynced(link);
            eventLinkRepository.save(link);

        } catch (RuntimeException exception) {
            log.warn(
                    "Google Calendar 同步失敗，memberId={}, eventId={}",
                    memberId,
                    event.getEventId(),
                    exception
            );

            existingLink.ifPresent(link -> {
                markFailed(link, exception);
                eventLinkRepository.save(link);
            });
        }
    }

    /**
     * 活動內容或時間修改時，
     * 更新所有關注會員的 Google 行程。
     */
    @Transactional
    public void syncEventForAllFollowers(
            CalendarEvent event
    ) {
        if (!canRemainOnCalendar(event)) {
            removeEventFromAllCalendars(
                    event.getEventId()
            );

            return;
        }

        List<EventFollow> follows =
                eventFollowRepository.findByEventId(
                        event.getEventId()
                );

        for (EventFollow follow : follows) {
            syncFollow(
                    follow.getMemberId(),
                    event,
                    follow.getRemindBeforeMinutes()
            );
        }
    }

    /**
     * 活動取消時，移除所有會員的 Google 行程。
     */
    @Transactional
    public void removeEventFromAllCalendars(
            Long eventId
    ) {
        List<GoogleCalendarEventLink> links =
                eventLinkRepository.findByEventId(
                        eventId
                );

        for (GoogleCalendarEventLink link : links) {
            removeLink(link);
        }
    }

    /**
     * 會員解除 Google Calendar 連線時：
     * 1. 嘗試刪除所有由本站建立的 Google 行程。
     * 2. 清除本機行程對應資料。
     *
     * 即使某一筆 Google 行程刪除失敗，
     * 也不阻止會員解除連線。
     */
    @Transactional
    public void removeAllForMember(
            Long memberId
    ) {
        List<GoogleCalendarEventLink> links =
                eventLinkRepository.findByMemberId(
                        memberId
                );

        for (GoogleCalendarEventLink link : links) {
            removeLink(link);
        }

        /*
         * removeLink() 遇到 Google API 失敗時
         * 會留下 FAILED 紀錄。
         * 但會員現在是主動解除連線，
         * 因此最後仍要清除本機對應資料。
         */
        eventLinkRepository.deleteByMemberId(
                memberId
        );
    }

    @Transactional
    public void removeFollow(
            Long memberId,
            Long eventId
    ) {
        eventLinkRepository
                .findByMemberIdAndEventId(
                        memberId,
                        eventId
                )
                .ifPresent(this::removeLink);
    }

    private void removeLink(
            GoogleCalendarEventLink link
    ) {
        GoogleCalendarApiService apiService =
                apiServiceProvider.getIfAvailable();

        if (apiService == null
                || !connectionRepository.existsByMemberId(
                link.getMemberId()
        )) {

            link.setSyncStatus("FAILED");
            link.setLastError(
                    "會員目前未連結 Google Calendar"
            );

            eventLinkRepository.save(link);

            return;
        }

        try {
            apiService.deleteEvent(
                    link.getMemberId(),
                    link.getGoogleEventId()
            );

            eventLinkRepository.delete(link);

        } catch (RuntimeException exception) {
            markFailed(link, exception);
            eventLinkRepository.save(link);
        }
    }

    private boolean canRemainOnCalendar(
            CalendarEvent event
    ) {
        return !"DRAFT".equals(event.getStatus())
                && !"INACTIVE".equals(event.getStatus())
                && !"CANCELLED".equals(event.getStatus());
    }

    private void markSynced(
            GoogleCalendarEventLink link
    ) {
        link.setSyncStatus("SYNCED");
        link.setLastError(null);
        link.setLastSyncedAt(
                LocalDateTime.now()
        );
    }

    private void markFailed(
            GoogleCalendarEventLink link,
            RuntimeException exception
    ) {
        String errorMessage =
                exception.getMessage();

        if (errorMessage == null
                || errorMessage.isBlank()) {
            errorMessage =
                    exception.getClass()
                            .getSimpleName();
        }

        if (errorMessage.length() > 500) {
            errorMessage =
                    errorMessage.substring(
                            0,
                            500
                    );
        }

        link.setSyncStatus("FAILED");
        link.setLastError(errorMessage);
    }
}