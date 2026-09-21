package com.project.sneakermarket.notification.service;

import com.project.sneakermarket.commerce.entity.Brand;
import com.project.sneakermarket.commerce.entity.Category;
import com.project.sneakermarket.commerce.repository.BrandRepository;
import com.project.sneakermarket.commerce.repository.CategoryRepository;
import com.project.sneakermarket.notification.dto.NotificationSubscriptionRequest;
import com.project.sneakermarket.notification.dto.NotificationSubscriptionResponse;
import com.project.sneakermarket.notification.entity.NotificationSubscription;
import com.project.sneakermarket.notification.entity.NotificationSubscriptionScope;
import com.project.sneakermarket.notification.entity.NotificationSubscriptionTargetType;
import com.project.sneakermarket.notification.repository.NotificationSubscriptionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificationSubscriptionServiceTests {

    private NotificationSubscriptionRepository
            subscriptionRepository;

    private BrandRepository
            brandRepository;

    private CategoryRepository
            categoryRepository;

    private NotificationSubscriptionService
            service;

    @BeforeEach
    void setUp() {
        subscriptionRepository =
                mock(
                        NotificationSubscriptionRepository.class
                );

        brandRepository =
                mock(BrandRepository.class);

        categoryRepository =
                mock(CategoryRepository.class);

        service =
                new NotificationSubscriptionService(
                        subscriptionRepository,
                        brandRepository,
                        categoryRepository
                );
    }

    @Test
    void newBrandSubscriptionIsCreated() {
        Brand brand =
                new Brand();

        brand.setBrandId(10L);
        brand.setBrandName("Nike");

        NotificationSubscriptionRequest request =
                new NotificationSubscriptionRequest(
                        NotificationSubscriptionTargetType.BRAND,
                        10L,
                        NotificationSubscriptionScope.NEW_PRODUCT
                );

        when(
                brandRepository.existsById(10L)
        ).thenReturn(true);

        when(
                subscriptionRepository
                        .findByMemberIdAndTargetTypeAndTargetIdAndScope(
                                1L,
                                NotificationSubscriptionTargetType.BRAND,
                                10L,
                                NotificationSubscriptionScope.NEW_PRODUCT
                        )
        ).thenReturn(Optional.empty());

        when(
                subscriptionRepository.save(
                        any(NotificationSubscription.class)
                )
        ).thenAnswer(invocation -> {
            NotificationSubscription saved =
                    invocation.getArgument(0);

            saved.setSubscriptionId(100L);
            saved.setCreatedAt(
                    LocalDateTime.of(
                            2026,
                            9,
                            20,
                            12,
                            0
                    )
            );

            return saved;
        });

        when(
                brandRepository.findById(10L)
        ).thenReturn(
                Optional.of(brand)
        );

        NotificationSubscriptionResponse response =
                service.subscribe(
                        1L,
                        request
                );

        assertEquals(
                100L,
                response.subscriptionId()
        );

        assertEquals(
                NotificationSubscriptionTargetType.BRAND,
                response.targetType()
        );

        assertEquals(
                10L,
                response.targetId()
        );

        assertEquals(
                "Nike",
                response.targetName()
        );

        assertEquals(
                NotificationSubscriptionScope.NEW_PRODUCT,
                response.scope()
        );

        verify(
                subscriptionRepository
        ).save(
                any(NotificationSubscription.class)
        );

        verifyNoInteractions(
                categoryRepository
        );
    }

    @Test
    void duplicateSubscriptionReturnsExistingRecord() {
        Brand brand =
                new Brand();

        brand.setBrandId(10L);
        brand.setBrandName("Nike");

        NotificationSubscription existing =
                createSubscription(
                        100L,
                        1L,
                        NotificationSubscriptionTargetType.BRAND,
                        10L
                );

        NotificationSubscriptionRequest request =
                new NotificationSubscriptionRequest(
                        NotificationSubscriptionTargetType.BRAND,
                        10L,
                        NotificationSubscriptionScope.NEW_PRODUCT
                );

        when(
                brandRepository.existsById(10L)
        ).thenReturn(true);

        when(
                subscriptionRepository
                        .findByMemberIdAndTargetTypeAndTargetIdAndScope(
                                1L,
                                NotificationSubscriptionTargetType.BRAND,
                                10L,
                                NotificationSubscriptionScope.NEW_PRODUCT
                        )
        ).thenReturn(
                Optional.of(existing)
        );

        when(
                brandRepository.findById(10L)
        ).thenReturn(
                Optional.of(brand)
        );

        NotificationSubscriptionResponse response =
                service.subscribe(
                        1L,
                        request
                );

        assertEquals(
                100L,
                response.subscriptionId()
        );

        assertEquals(
                "Nike",
                response.targetName()
        );

        verify(
                subscriptionRepository,
                never()
        ).save(any());
    }

    @Test
    void categorySubscriptionUsesCategoryRepository() {
        Category category =
                new Category();

        category.setCategoryId(20L);
        category.setCategoryName("籃球鞋");

        NotificationSubscriptionRequest request =
                new NotificationSubscriptionRequest(
                        NotificationSubscriptionTargetType.CATEGORY,
                        20L,
                        NotificationSubscriptionScope.NEW_PRODUCT
                );

        when(
                categoryRepository.existsById(20L)
        ).thenReturn(true);

        when(
                subscriptionRepository
                        .findByMemberIdAndTargetTypeAndTargetIdAndScope(
                                1L,
                                NotificationSubscriptionTargetType.CATEGORY,
                                20L,
                                NotificationSubscriptionScope.NEW_PRODUCT
                        )
        ).thenReturn(Optional.empty());

        when(
                subscriptionRepository.save(
                        any(NotificationSubscription.class)
                )
        ).thenAnswer(invocation -> {
            NotificationSubscription saved =
                    invocation.getArgument(0);

            saved.setSubscriptionId(200L);
            saved.setCreatedAt(
                    LocalDateTime.now()
            );

            return saved;
        });

        when(
                categoryRepository.findById(20L)
        ).thenReturn(
                Optional.of(category)
        );

        NotificationSubscriptionResponse response =
                service.subscribe(
                        1L,
                        request
                );

        assertEquals(
                200L,
                response.subscriptionId()
        );

        assertEquals(
                NotificationSubscriptionTargetType.CATEGORY,
                response.targetType()
        );

        assertEquals(
                "籃球鞋",
                response.targetName()
        );

        verifyNoInteractions(
                brandRepository
        );
    }

    @Test
    void missingBrandIsRejected() {
        NotificationSubscriptionRequest request =
                new NotificationSubscriptionRequest(
                        NotificationSubscriptionTargetType.BRAND,
                        999L,
                        NotificationSubscriptionScope.NEW_PRODUCT
                );

        when(
                brandRepository.existsById(999L)
        ).thenReturn(false);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.subscribe(
                                1L,
                                request
                        )
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verifyNoInteractions(
                subscriptionRepository
        );

        verifyNoInteractions(
                categoryRepository
        );
    }

    @Test
    void memberCanDeleteOwnSubscription() {
        NotificationSubscription subscription =
                createSubscription(
                        100L,
                        1L,
                        NotificationSubscriptionTargetType.BRAND,
                        10L
                );

        when(
                subscriptionRepository
                        .findBySubscriptionIdAndMemberId(
                                100L,
                                1L
                        )
        ).thenReturn(
                Optional.of(subscription)
        );

        service.unsubscribe(
                1L,
                100L
        );

        verify(
                subscriptionRepository
        ).delete(subscription);
    }

    @Test
    void memberCannotDeleteAnotherMembersSubscription() {
        when(
                subscriptionRepository
                        .findBySubscriptionIdAndMemberId(
                                100L,
                                2L
                        )
        ).thenReturn(
                Optional.empty()
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.unsubscribe(
                                2L,
                                100L
                        )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(
                subscriptionRepository,
                never()
        ).delete(any());
    }

    private NotificationSubscription createSubscription(
            Long subscriptionId,
            Long memberId,
            NotificationSubscriptionTargetType targetType,
            Long targetId
    ) {
        NotificationSubscription result =
                new NotificationSubscription();

        result.setSubscriptionId(subscriptionId);
        result.setMemberId(memberId);
        result.setTargetType(targetType);
        result.setTargetId(targetId);

        result.setScope(
                NotificationSubscriptionScope.NEW_PRODUCT
        );

        result.setCreatedAt(
                LocalDateTime.of(
                        2026,
                        9,
                        20,
                        12,
                        0
                )
        );

        return result;
    }
}