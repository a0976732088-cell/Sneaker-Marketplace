package com.project.sneakermarket.auction.scheduler;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.project.sneakermarket.auction.repository.AuctionOrderRepository;
import com.project.sneakermarket.auction.service.AuctionOrderPaymentDeadlineService;
import com.project.sneakermarket.auction.service.AuctionTime;

class AuctionOrderPaymentDeadlineSchedulerTests {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 16, 20, 0);

    @Test
    void onlyCandidateIdsAreProcessedAndOneFailureDoesNotStopNext() {
        var orders = mock(AuctionOrderRepository.class);
        var deadlines = mock(AuctionOrderPaymentDeadlineService.class);
        var time = mock(AuctionTime.class);
        when(time.now()).thenReturn(NOW);
        when(orders.findPaymentDeadlineCandidateIds(NOW)).thenReturn(List.of(1L, 2L));
        doThrow(new IllegalStateException("fixture")).when(deadlines).expireOverdueOrder(1L);

        new AuctionOrderPaymentDeadlineScheduler(orders, deadlines, time).expireOverdueOrders();

        verify(orders).findPaymentDeadlineCandidateIds(NOW);
        verify(orders, never()).findAll();
        var sequence = inOrder(deadlines);
        sequence.verify(deadlines).expireOverdueOrder(1L);
        sequence.verify(deadlines).expireOverdueOrder(2L);
    }

    @Test
    void configuredCronActuallyRuns() throws Exception {
        var orders = mock(AuctionOrderRepository.class);
        var deadlines = mock(AuctionOrderPaymentDeadlineService.class);
        var time = mock(AuctionTime.class);
        var invoked = new CountDownLatch(1);
        when(time.now()).thenReturn(NOW);
        when(orders.findPaymentDeadlineCandidateIds(NOW)).thenReturn(List.of(1L));
        doAnswer(call -> {
            invoked.countDown();
            return null;
        }).when(deadlines).expireOverdueOrder(1L);

        try (var context = context(orders, deadlines, time, true)) {
            assertTrue(invoked.await(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void disabledOrderSchedulingDoesNotRegisterDeadlineScheduler() {
        var orders = mock(AuctionOrderRepository.class);
        var deadlines = mock(AuctionOrderPaymentDeadlineService.class);
        var time = mock(AuctionTime.class);

        try (var context = context(orders, deadlines, time, false)) {
            assertTrue(context.getBeansOfType(AuctionOrderPaymentDeadlineScheduler.class).isEmpty());
            verifyNoInteractions(orders, deadlines, time);
        }
    }

    private AnnotationConfigApplicationContext context(AuctionOrderRepository orders,
            AuctionOrderPaymentDeadlineService deadlines, AuctionTime time, boolean enabled) {
        var context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                "payment-deadline-cron",
                Map.of("auction.order.enabled", Boolean.toString(enabled),
                        "auction.order.payment-deadline.cron", "*/1 * * * * *")));
        context.registerBean(AuctionOrderRepository.class, () -> orders);
        context.registerBean(AuctionOrderPaymentDeadlineService.class, () -> deadlines);
        context.registerBean(AuctionTime.class, () -> time);
        context.register(SchedulingConfig.class, AuctionOrderPaymentDeadlineScheduler.class);
        context.refresh();
        return context;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    static class SchedulingConfig {}
}
