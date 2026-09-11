package com.project.sneakermarket.auction.scheduler;

import static org.mockito.Mockito.*;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.sneakermarket.auction.repository.AuctionRepository;
import com.project.sneakermarket.auction.service.AuctionLifecycleService;
import com.project.sneakermarket.auction.service.AuctionTime;

class AuctionLifecycleSchedulerTests {
    @Test
    void failingAuctionDoesNotPreventRemainingCandidates() {
        var auctions = mock(AuctionRepository.class);
        var service = mock(AuctionLifecycleService.class);
        var time = mock(AuctionTime.class);
        var now = LocalDateTime.of(2026, 9, 10, 12, 0);
        when(time.now()).thenReturn(now);
        when(auctions.findLifecycleCandidateIds(now)).thenReturn(List.of(1L, 2L, 3L));
        doThrow(new IllegalStateException("inconsistent fixture")).when(service).processAuction(2L);

        new AuctionLifecycleScheduler(auctions, service, time).updateAuctionLifecycle();

        var order = inOrder(service);
        order.verify(service).processAuction(1L);
        order.verify(service).processAuction(2L);
        order.verify(service).processAuction(3L);
        verify(auctions, never()).findAll();
    }

    @Test
    void springActuallyRunsConfiguredCron() throws Exception {
        var auctions = mock(AuctionRepository.class);
        var service = mock(AuctionLifecycleService.class);
        var time = mock(AuctionTime.class);
        var now = LocalDateTime.of(2026, 9, 10, 12, 0);
        var invoked = new java.util.concurrent.CountDownLatch(1);
        when(time.now()).thenReturn(now);
        when(auctions.findLifecycleCandidateIds(now)).thenReturn(List.of(1L));
        doAnswer(call -> { invoked.countDown(); return null; }).when(service).processAuction(1L);

        try (var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(
                    new org.springframework.core.env.MapPropertySource("test-cron", java.util.Map.of(
                            "auction.lifecycle.enabled", "true",
                            "auction.lifecycle.cron", "*/1 * * * * *")));
            context.registerBean(AuctionRepository.class, () -> auctions);
            context.registerBean(AuctionLifecycleService.class, () -> service);
            context.registerBean(AuctionTime.class, () -> time);
            context.register(SchedulingConfig.class, AuctionLifecycleScheduler.class);
            context.refresh();
            org.junit.jupiter.api.Assertions.assertTrue(
                    invoked.await(5, java.util.concurrent.TimeUnit.SECONDS));
            verify(service, atLeastOnce()).processAuction(1L);
        }
    }

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @org.springframework.scheduling.annotation.EnableScheduling
    static class SchedulingConfig {
    }
}
