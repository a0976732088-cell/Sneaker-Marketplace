package com.project.sneakermarket.notification.service;

import com.project.sneakermarket.notification.entity.Notification;
import com.project.sneakermarket.notification.entity.NotificationEmailJob;
import com.project.sneakermarket.notification.entity.NotificationEmailStatus;
import com.project.sneakermarket.notification.repository.NotificationEmailJobRepository;
import com.project.sneakermarket.notification.repository.NotificationRepository;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificationEmailDispatchServiceTests {

    private NotificationEmailJobRepository
            jobRepository;

    private NotificationRepository
            notificationRepository;

    private JavaMailSender
            mailSender;

    private NotificationEmailDispatchService
            service;

    @BeforeEach
    void setUp() {
        jobRepository =
                mock(
                        NotificationEmailJobRepository.class
                );

        notificationRepository =
                mock(
                        NotificationRepository.class
                );

        mailSender =
                mock(JavaMailSender.class);

        service =
                new NotificationEmailDispatchService(
                        jobRepository,
                        notificationRepository,
                        mailSender
                );

        ReflectionTestUtils.setField(
                service,
                "frontendBaseUrl",
                "http://localhost:5173"
        );
    }

    @Test
    void firstFailureSchedulesRetry() {
        NotificationEmailJob job =
                createJob();

        Notification notification =
                createNotification();

        when(
                jobRepository.findByIdForUpdate(
                        10L
                )
        ).thenReturn(
                Optional.of(job)
        );

        when(
                notificationRepository.findById(
                        100L
                )
        ).thenReturn(
                Optional.of(notification)
        );

        /*
         * 不設定 mailFrom，
         * 模擬 Email 設定錯誤。
         */
        ReflectionTestUtils.setField(
                service,
                "mailFrom",
                ""
        );

        service.sendJob(10L);

        assertEquals(
                NotificationEmailStatus.RETRY_WAIT,
                job.getStatus()
        );

        assertEquals(
                1,
                job.getRetryCount()
        );

        assertNotNull(
                job.getNextRetryAt()
        );

        assertEquals(
                "Email 準備或寄送失敗，請檢查後端設定與日誌",
                job.getLastError()
        );

        assertNull(
                job.getSentAt()
        );

        verify(
                jobRepository
        ).save(job);

        verify(
                mailSender,
                never()
        ).send(
                org.mockito.ArgumentMatchers
                        .any(MimeMessage.class)
        );
    }

    @Test
    void thirdFailureBecomesFinalFailure() {
        NotificationEmailJob job =
                createJob();

        job.setRetryCount(2);

        Notification notification =
                createNotification();

        when(
                jobRepository.findByIdForUpdate(
                        10L
                )
        ).thenReturn(
                Optional.of(job)
        );

        when(
                notificationRepository.findById(
                        100L
                )
        ).thenReturn(
                Optional.of(notification)
        );

        ReflectionTestUtils.setField(
                service,
                "mailFrom",
                ""
        );

        service.sendJob(10L);

        assertEquals(
                NotificationEmailStatus.FAILED,
                job.getStatus()
        );

        assertEquals(
                3,
                job.getRetryCount()
        );

        assertNull(
                job.getNextRetryAt()
        );

        assertNotNull(
                job.getLastError()
        );

        verify(
                jobRepository
        ).save(job);
    }

    @Test
    void retryWaitBeforeScheduledTimeIsSkipped() {
        NotificationEmailJob job =
                createJob();

        job.setStatus(
                NotificationEmailStatus.RETRY_WAIT
        );

        job.setNextRetryAt(
                LocalDateTime.now()
                        .plusHours(1)
        );

        when(
                jobRepository.findByIdForUpdate(
                        10L
                )
        ).thenReturn(
                Optional.of(job)
        );

        service.sendJob(10L);

        verifyNoInteractions(
                notificationRepository
        );

        verifyNoInteractions(
                mailSender
        );

        verify(
                jobRepository,
                never()
        ).save(job);

        assertEquals(
                NotificationEmailStatus.RETRY_WAIT,
                job.getStatus()
        );

        assertEquals(
                0,
                job.getRetryCount()
        );
    }

    @Test
    void successfulSendMarksJobAsSent() {
        NotificationEmailJob job =
                createJob();

        Notification notification =
                createNotification();

        MimeMessage mimeMessage =
                new MimeMessage(
                        Session.getInstance(
                                new Properties()
                        )
                );

        when(
                jobRepository.findByIdForUpdate(
                        10L
                )
        ).thenReturn(
                Optional.of(job)
        );

        when(
                notificationRepository.findById(
                        100L
                )
        ).thenReturn(
                Optional.of(notification)
        );

        when(
                mailSender.createMimeMessage()
        ).thenReturn(mimeMessage);

        ReflectionTestUtils.setField(
                service,
                "mailFrom",
                "no-reply@sneakermarket.local"
        );

        service.sendJob(10L);

        assertEquals(
                NotificationEmailStatus.SENT,
                job.getStatus()
        );

        assertEquals(
                0,
                job.getRetryCount()
        );

        assertNotNull(
                job.getSentAt()
        );

        assertNull(
                job.getNextRetryAt()
        );

        assertNull(
                job.getLastError()
        );

        verify(
                mailSender
        ).send(mimeMessage);

        verify(
                jobRepository
        ).save(job);
    }

    private NotificationEmailJob createJob() {
        NotificationEmailJob job =
                new NotificationEmailJob();

        job.setId(10L);
        job.setNotificationId(100L);
        job.setMemberId(1L);

        job.setRecipientEmail(
                "member@example.com"
        );

        job.setStatus(
                NotificationEmailStatus.PENDING
        );

        job.setRetryCount(0);

        return job;
    }

    private Notification createNotification() {
        Notification notification =
                new Notification();

        notification.setNotificationId(100L);
        notification.setMemberId(1L);

        notification.setNotificationType(
                "EVENT_REMINDER"
        );

        notification.setTitle(
                "活動即將開始"
        );

        notification.setContent(
                "您關注的活動即將開始。"
        );

        notification.setTargetType("EVENT");
        notification.setTargetId(200L);
        notification.setTargetUrl(
                "/calendar"
        );

        return notification;
    }
}