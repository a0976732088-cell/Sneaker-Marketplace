package com.project.sneakermarket.notification.service;

import com.project.sneakermarket.notification.entity.NotificationEmailJob;
import com.project.sneakermarket.notification.entity.NotificationEmailStatus;
import com.project.sneakermarket.notification.repository.NotificationEmailJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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


    @Transactional
    public void retryFailed(Long jobId) {
        NotificationEmailJob job = jobRepository.findByIdForUpdate(jobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "找不到 Email 任務"
                ));

        if (job.getStatus() != NotificationEmailStatus.FAILED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "只有最終失敗的 Email 任務可以手動重試"
            );
        }

        job.setStatus(NotificationEmailStatus.PENDING);
        job.setRetryCount(0);
        job.setNextRetryAt(null);
        job.setLastError(null);
        job.setSentAt(null);
    }
}