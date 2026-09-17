package com.project.sneakermarket.notification.scheduler;

import com.project.sneakermarket.notification.entity.NotificationEmailStatus;
import com.project.sneakermarket.notification.repository.NotificationEmailJobRepository;
import com.project.sneakermarket.notification.service.NotificationEmailDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

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
        var jobs = jobRepository.findReadyJobs(
                NotificationEmailStatus.PENDING,
                NotificationEmailStatus.RETRY_WAIT,
                LocalDateTime.now(ZoneId.of("Asia/Taipei")),
                PageRequest.of(0, 50)
        );

        for (var job : jobs) {
            try {
                dispatchService.sendJob(job.getId());
            } catch (Exception exception) {
                log.error(
                        "通知 Email 工作處理失敗，jobId={}",
                        job.getId(),
                        exception
                );
            }
        }
    }
}