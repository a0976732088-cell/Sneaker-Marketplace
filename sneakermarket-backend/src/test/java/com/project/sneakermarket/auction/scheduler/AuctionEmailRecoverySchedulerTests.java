package com.project.sneakermarket.auction.scheduler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.project.sneakermarket.auction.email.*;
import com.project.sneakermarket.auction.repository.*;

class AuctionEmailRecoverySchedulerTests {
    private final AuctionResultRepository results = mock(AuctionResultRepository.class);
    private final AuctionEmailLogRepository logs = mock(AuctionEmailLogRepository.class);
    private final AuctionEmailService emails = mock(AuctionEmailService.class);
    private final AuctionEmailDispatchService dispatch = mock(AuctionEmailDispatchService.class);
    private final AuctionEmailSettings settings = new AuctionEmailSettings(true, 3, "from@example.invalid", "https://example.invalid");

    @Test
    void perItemFailuresNeverStopLaterCreationOrDispatch() {
        when(results.findEmailReadinessCandidateIds()).thenReturn(List.of(1L, 2L));
        when(emails.ensureEmailLogsForAuction(1L)).thenThrow(new IllegalStateException("fixture"));
        when(logs.findDispatchCandidateIds(null, 3)).thenReturn(List.of(10L, 11L));
        doThrow(new IllegalStateException("fixture")).when(dispatch).sendEmailLog(10L);
        scheduler().recoverEmails();
        verify(emails).ensureEmailLogsForAuction(2L);
        verify(dispatch).sendEmailLog(11L);
        verify(results, never()).findAll();
        verify(logs, never()).findAll();
    }

    @Test
    void readinessScanFailureStillDispatchesExistingPendingTasks() {
        when(results.findEmailReadinessCandidateIds()).thenThrow(new IllegalStateException("fixture"));
        when(logs.findDispatchCandidateIds(null, 3)).thenReturn(List.of(10L));
        scheduler().recoverEmails();
        verify(dispatch).sendEmailLog(10L);
    }

    @Test
    void configuredCronActuallyRuns() throws Exception {
        var invoked = new CountDownLatch(1);
        when(results.findEmailReadinessCandidateIds()).thenReturn(List.of(1L));
        when(emails.ensureEmailLogsForAuction(1L)).thenAnswer(c -> { invoked.countDown(); return List.of(); });
        try (var context = context(Map.of("auction.email.enabled", "true", "auction.email.cron", "*/1 * * * * *"))) {
            assertTrue(invoked.await(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void missingEnabledPropertyKeepsSchedulerAbsent() {
        try (var context = context(Map.of())) {
            assertTrue(context.getBeansOfType(AuctionEmailRecoveryScheduler.class).isEmpty());
            verifyNoInteractions(results, logs, emails, dispatch);
        }
    }

    @Test
    void explicitDisabledPropertyKeepsSchedulerAbsent() {
        try (var context = context(Map.of("auction.email.enabled", "false"))) {
            assertTrue(context.getBeansOfType(AuctionEmailRecoveryScheduler.class).isEmpty());
            verifyNoInteractions(results, logs, emails, dispatch);
        }
    }

    private AuctionEmailRecoveryScheduler scheduler() {
        return new AuctionEmailRecoveryScheduler(results, logs, emails, dispatch, settings);
    }

    private AnnotationConfigApplicationContext context(Map<String, Object> properties) {
        var context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("email-test", properties));
        context.registerBean(AuctionResultRepository.class, () -> results);
        context.registerBean(AuctionEmailLogRepository.class, () -> logs);
        context.registerBean(AuctionEmailService.class, () -> emails);
        context.registerBean(AuctionEmailDispatchService.class, () -> dispatch);
        context.registerBean(AuctionEmailSettings.class, () -> settings);
        context.register(SchedulingConfig.class, AuctionEmailRecoveryScheduler.class);
        context.refresh();
        return context;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    static class SchedulingConfig {}
}
