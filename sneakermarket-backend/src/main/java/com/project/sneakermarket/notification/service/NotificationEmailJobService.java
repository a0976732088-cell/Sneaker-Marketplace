package com.project.sneakermarket.notification.service;

import com.project.sneakermarket.notification.entity.NotificationEmailJob;
import com.project.sneakermarket.notification.entity.NotificationEmailStatus;
import com.project.sneakermarket.notification.repository.NotificationEmailJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationEmailJobService {

    private final NotificationEmailJobRepository jobRepository;

    public void createPending(
            Long notificationId,
            Long memberId,
            String recipientEmail
    ) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return;
        }

        // Entity 的唯一限制是最後一道防線
        if (jobRepository.existsByNotificationId(notificationId)) {
            return;
        }

        NotificationEmailJob job = new NotificationEmailJob();
        job.setNotificationId(notificationId);
        job.setMemberId(memberId);
        job.setRecipientEmail(recipientEmail.trim());
        job.setStatus(NotificationEmailStatus.PENDING);
        job.setRetryCount(0);

        jobRepository.save(job);
    }
}