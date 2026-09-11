package com.project.sneakermarket.notification.scheduler;

import com.project.sneakermarket.calendar.repository.EventFollowRepository;
import com.project.sneakermarket.notification.service.EventReminderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "notification.event-reminder.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class EventReminderScheduler {

    private static final ZoneId TAIPEI_ZONE =
            ZoneId.of("Asia/Taipei");

    private final EventFollowRepository eventFollowRepository;
    private final EventReminderService eventReminderService;

    @Scheduled(
            cron = "${notification.event-reminder.cron:0 * * * * *}",
            zone = "Asia/Taipei"
    )
    public void sendEventReminders() {

        LocalDateTime now =
                LocalDateTime.now(TAIPEI_ZONE);

        List<Long> followIds =
                eventFollowRepository.findDueReminderIds(now);

        for (Long followId : followIds) {
            try {
                eventReminderService.createReminder(
                        followId,
                        now
                );
            } catch (Exception exception) {
                /*
                 * 一位會員發送失敗，
                 * 不影響同批其他會員。
                 */
                log.error(
                        "活動提醒建立失敗，followId={}",
                        followId,
                        exception
                );
            }
        }
    }
}