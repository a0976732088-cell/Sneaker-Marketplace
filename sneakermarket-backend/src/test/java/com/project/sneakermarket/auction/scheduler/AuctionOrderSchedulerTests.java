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

import com.project.sneakermarket.auction.repository.AuctionResultRepository;
import com.project.sneakermarket.auction.service.AuctionOrderService;

class AuctionOrderSchedulerTests {
    @Test
    void singleFailureDoesNotStopRemainingCandidates() {
        var results = mock(AuctionResultRepository.class);
        var service = mock(AuctionOrderService.class);
        when(results.findOrderCandidateIds()).thenReturn(List.of(1L, 2L));
        doThrow(new IllegalStateException("fixture")).when(service).createOrderForAuction(1L);
        new AuctionOrderScheduler(results, service).createAuctionOrders();
        var sequence = inOrder(service);
        sequence.verify(service).createOrderForAuction(1L);
        sequence.verify(service).createOrderForAuction(2L);
        verify(results, never()).findAll();
    }

    @Test
    void configuredCronActuallyRuns() throws Exception {
        var results = mock(AuctionResultRepository.class);
        var service = mock(AuctionOrderService.class);
        var invoked = new CountDownLatch(1);
        when(results.findOrderCandidateIds()).thenReturn(List.of(1L));
        doAnswer(call -> { invoked.countDown(); return null; }).when(service).createOrderForAuction(1L);
        try (var context = context(results, service, true)) {
            assertTrue(invoked.await(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void disabledPropertyDoesNotRegisterScheduler() {
        var results = mock(AuctionResultRepository.class);
        var service = mock(AuctionOrderService.class);
        try (var context = context(results, service, false)) {
            assertTrue(context.getBeansOfType(AuctionOrderScheduler.class).isEmpty());
            verifyNoInteractions(results, service);
        }
    }

    private AnnotationConfigApplicationContext context(AuctionResultRepository results,
            AuctionOrderService service, boolean enabled) {
        var context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("order-cron", Map.of(
                "auction.order.enabled", Boolean.toString(enabled), "auction.order.cron", "*/1 * * * * *")));
        context.registerBean(AuctionResultRepository.class, () -> results);
        context.registerBean(AuctionOrderService.class, () -> service);
        context.register(SchedulingConfig.class, AuctionOrderScheduler.class);
        context.refresh();
        return context;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    static class SchedulingConfig {}
}
