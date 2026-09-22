package com.project.sneakermarket.notification.service;

import com.project.sneakermarket.commerce.entity.Brand;
import com.project.sneakermarket.commerce.entity.Category;
import com.project.sneakermarket.commerce.entity.Product;
import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.member.repository.MemberRepository;
import com.project.sneakermarket.notification.entity.Notification;
import com.project.sneakermarket.notification.entity.NotificationLog;
import com.project.sneakermarket.notification.entity.NotificationSubscription;
import com.project.sneakermarket.notification.entity.NotificationSubscriptionScope;
import com.project.sneakermarket.notification.entity.NotificationSubscriptionTargetType;
import com.project.sneakermarket.notification.entity.ProductReleaseNotificationDispatch;
import com.project.sneakermarket.notification.repository.NotificationLogRepository;
import com.project.sneakermarket.notification.repository.NotificationRepository;
import com.project.sneakermarket.notification.repository.NotificationSubscriptionRepository;
import com.project.sneakermarket.notification.repository.ProductReleaseNotificationDispatchRepository;
import com.project.sneakermarket.notification.service.NotificationPreferencePolicyService.DeliveryDecision;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProductReleaseNotificationServiceTests {

    private ProductReleaseNotificationDispatchRepository
            dispatchRepository;

    private NotificationSubscriptionRepository
            subscriptionRepository;

    private NotificationRepository
            notificationRepository;

    private NotificationLogRepository
            notificationLogRepository;

    private MemberRepository
            memberRepository;

    private NotificationEmailJobService
            emailJobService;

    private NotificationPreferencePolicyService
            preferencePolicyService;

    private EntityManager
            entityManager;

    private ProductReleaseNotificationService
            service;

    private Product product;

    private Member member;

    @BeforeEach
    void setUp() {
        dispatchRepository =
                mock(
                        ProductReleaseNotificationDispatchRepository.class
                );

        subscriptionRepository =
                mock(
                        NotificationSubscriptionRepository.class
                );

        notificationRepository =
                mock(NotificationRepository.class);

        notificationLogRepository =
                mock(NotificationLogRepository.class);

        memberRepository =
                mock(MemberRepository.class);

        emailJobService =
                mock(NotificationEmailJobService.class);

        preferencePolicyService =
                mock(
                        NotificationPreferencePolicyService.class
                );

        entityManager =
                mock(EntityManager.class);

        service =
                new ProductReleaseNotificationService(
                        dispatchRepository,
                        subscriptionRepository,
                        notificationRepository,
                        notificationLogRepository,
                        memberRepository,
                        emailJobService,
                        preferencePolicyService
                );

        ReflectionTestUtils.setField(
                service,
                "entityManager",
                entityManager
        );

        product = createProduct();
        member = createMember();

        when(
                entityManager.find(
                        Product.class,
                        100L,
                        LockModeType.PESSIMISTIC_WRITE
                )
        ).thenReturn(product);
    }

    @Test
    void brandAndCategorySubscriptionCreatesOnlyOneNotification() {
        NotificationSubscription brandSubscription =
                createSubscription(
                        NotificationSubscriptionTargetType.BRAND,
                        10L
                );

        NotificationSubscription categorySubscription =
                createSubscription(
                        NotificationSubscriptionTargetType.CATEGORY,
                        20L
                );

        when(
                dispatchRepository.existsByProductId(
                        100L
                )
        ).thenReturn(false);

        when(
                subscriptionRepository
                        .findByScopeAndTargetTypeAndTargetId(
                                NotificationSubscriptionScope.NEW_PRODUCT,
                                NotificationSubscriptionTargetType.BRAND,
                                10L
                        )
        ).thenReturn(
                List.of(brandSubscription)
        );

        when(
                subscriptionRepository
                        .findByScopeAndTargetTypeAndTargetId(
                                NotificationSubscriptionScope.NEW_PRODUCT,
                                NotificationSubscriptionTargetType.CATEGORY,
                                20L
                        )
        ).thenReturn(
                List.of(categorySubscription)
        );

        when(
                memberRepository.findAllById(
                        any()
                )
        ).thenReturn(
                List.of(member)
        );

        when(
                preferencePolicyService.resolveForMembers(
                        anyCollection(),
                        any(),
                        any()
                )
        ).thenReturn(
                Map.of(
                        1L,
                        DeliveryDecision.allEnabled()
                )
        );

        when(
                notificationRepository
                        .existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
                                1L,
                                "PRODUCT_RELEASE",
                                "PRODUCT",
                                100L
                        )
        ).thenReturn(false);

        List<Notification> capturedNotifications =
                new ArrayList<>();

        when(
                notificationRepository.saveAll(
                        anyList()
                )
        ).thenAnswer(invocation -> {
            List<Notification> notifications =
                    invocation.getArgument(0);

            capturedNotifications.addAll(
                    notifications
            );

            notifications.get(0)
                    .setNotificationId(1000L);

            return notifications;
        });

        service.processProduct(100L);

        assertEquals(
                1,
                capturedNotifications.size()
        );

        Notification notification =
                capturedNotifications.get(0);

        assertEquals(
                1L,
                notification.getMemberId()
        );

        assertEquals(
                "PRODUCT_RELEASE",
                notification.getNotificationType()
        );

        assertEquals(
                "PRODUCT",
                notification.getTargetType()
        );

        assertEquals(
                100L,
                notification.getTargetId()
        );

        assertEquals(
                "/products/100",
                notification.getTargetUrl()
        );

        assertTrue(
                notification.getInAppVisible()
        );

        verify(
                emailJobService
        ).createPending(
                1000L,
                1L,
                "member@example.com"
        );

        ArgumentCaptor<ProductReleaseNotificationDispatch>
                dispatchCaptor =
                ArgumentCaptor.forClass(
                        ProductReleaseNotificationDispatch.class
                );

        verify(
                dispatchRepository
        ).save(
                dispatchCaptor.capture()
        );

        assertEquals(
                1,
                dispatchCaptor.getValue()
                        .getNotificationCount()
        );

        ArgumentCaptor<NotificationLog> logCaptor =
                ArgumentCaptor.forClass(
                        NotificationLog.class
                );

        verify(
                notificationLogRepository
        ).save(
                logCaptor.capture()
        );

        assertEquals(
                1,
                logCaptor.getValue()
                        .getTotalSent()
        );
    }

    @Test
    void processedProductIsNotSentAgain() {
        when(
                dispatchRepository.existsByProductId(
                        100L
                )
        ).thenReturn(true);

        service.processProduct(100L);

        verifyNoInteractions(
                subscriptionRepository
        );

        verifyNoInteractions(
                memberRepository
        );

        verifyNoInteractions(
                preferencePolicyService
        );

        verifyNoInteractions(
                notificationRepository
        );

        verify(
                dispatchRepository,
                never()
        ).save(any());

        verifyNoInteractions(
                notificationLogRepository
        );

        verifyNoInteractions(
                emailJobService
        );
    }

    @Test
    void disabledPreferenceCreatesNoNotification() {
        stubSingleBrandSubscriber();

        when(
                preferencePolicyService.resolveForMembers(
                        anyCollection(),
                        any(),
                        any()
                )
        ).thenReturn(
                Map.of(
                        1L,
                        DeliveryDecision.none()
                )
        );

        service.processProduct(100L);

        verify(
                notificationRepository,
                never()
        ).saveAll(anyList());

        verifyNoInteractions(
                emailJobService
        );

        verifyNoInteractions(
                notificationLogRepository
        );

        ArgumentCaptor<ProductReleaseNotificationDispatch>
                dispatchCaptor =
                ArgumentCaptor.forClass(
                        ProductReleaseNotificationDispatch.class
                );

        verify(
                dispatchRepository
        ).save(
                dispatchCaptor.capture()
        );

        assertEquals(
                0,
                dispatchCaptor.getValue()
                        .getNotificationCount()
        );
    }

    @Test
    void existingMemberProductNotificationIsNotDuplicated() {
        stubSingleBrandSubscriber();

        when(
                preferencePolicyService.resolveForMembers(
                        anyCollection(),
                        any(),
                        any()
                )
        ).thenReturn(
                Map.of(
                        1L,
                        DeliveryDecision.allEnabled()
                )
        );

        when(
                notificationRepository
                        .existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
                                1L,
                                "PRODUCT_RELEASE",
                                "PRODUCT",
                                100L
                        )
        ).thenReturn(true);

        service.processProduct(100L);

        verify(
                notificationRepository,
                never()
        ).saveAll(anyList());

        verifyNoInteractions(
                emailJobService
        );

        verifyNoInteractions(
                notificationLogRepository
        );

        ArgumentCaptor<ProductReleaseNotificationDispatch>
                dispatchCaptor =
                ArgumentCaptor.forClass(
                        ProductReleaseNotificationDispatch.class
                );

        verify(
                dispatchRepository
        ).save(
                dispatchCaptor.capture()
        );

        assertEquals(
                0,
                dispatchCaptor.getValue()
                        .getNotificationCount()
        );
    }

    private void stubSingleBrandSubscriber() {
        NotificationSubscription subscription =
                createSubscription(
                        NotificationSubscriptionTargetType.BRAND,
                        10L
                );

        when(
                dispatchRepository.existsByProductId(
                        100L
                )
        ).thenReturn(false);

        when(
                subscriptionRepository
                        .findByScopeAndTargetTypeAndTargetId(
                                NotificationSubscriptionScope.NEW_PRODUCT,
                                NotificationSubscriptionTargetType.BRAND,
                                10L
                        )
        ).thenReturn(
                List.of(subscription)
        );

        when(
                subscriptionRepository
                        .findByScopeAndTargetTypeAndTargetId(
                                NotificationSubscriptionScope.NEW_PRODUCT,
                                NotificationSubscriptionTargetType.CATEGORY,
                                20L
                        )
        ).thenReturn(
                List.of()
        );

        when(
                memberRepository.findAllById(
                        any()
                )
        ).thenReturn(
                List.of(member)
        );
    }

    private Product createProduct() {
        Brand brand =
                new Brand();

        brand.setBrandId(10L);
        brand.setBrandName("Nike");

        Category category =
                new Category();

        category.setCategoryId(20L);
        category.setCategoryName("籃球鞋");

        Product result =
                new Product();

        result.setProductId(100L);
        result.setProductName(
                "Air Test One"
        );

        result.setBrand(brand);
        result.setCategory(category);
        result.setStatus("ON_SALE");

        return result;
    }

    private Member createMember() {
        Member result =
                new Member();

        result.setMemberId(1L);
        result.setEmail(
                "member@example.com"
        );

        result.setDeletedAt(null);

        return result;
    }

    private NotificationSubscription createSubscription(
            NotificationSubscriptionTargetType targetType,
            Long targetId
    ) {
        NotificationSubscription result =
                new NotificationSubscription();

        result.setMemberId(1L);
        result.setTargetType(targetType);
        result.setTargetId(targetId);

        result.setScope(
                NotificationSubscriptionScope.NEW_PRODUCT
        );

        return result;
    }
}