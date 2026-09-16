package com.project.sneakermarket.notification.scheduler;

import com.project.sneakermarket.notification.entity.NotificationEmailStatus;
import com.project.sneakermarket.notification.repository.NotificationEmailJobRepository;
import com.project.sneakermarket.notification.service.NotificationEmailDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "notification.email.enabled",
        havingValue = "true"
)
public class NotificationEmailScheduler {

    private final NotificationEmailJobRepository jobRepository;
    private final NotificationEmailDispatchService dispatchService;

    @Scheduled(
            cron = "${notification.email.cron:0 * * * * *}",
            zone = "Asia/Taipei"
    )
    public void sendPendingEmails() {
        var jobs = jobRepository
                .findTop50ByStatusOrderByCreatedAtAsc(
                        NotificationEmailStatus.PENDING
                );

        for (var job : jobs) {
            try {
                dispatchService.sendJob(job.getId());
            } catch (Exception exception) {
                log.error(
                        "通知 Email 工作處理失敗，jobId={}",
                        job.getId()
                );
            }
        }
    }
}