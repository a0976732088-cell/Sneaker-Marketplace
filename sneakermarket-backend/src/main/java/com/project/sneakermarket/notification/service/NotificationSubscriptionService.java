package com.project.sneakermarket.notification.service;

import com.project.sneakermarket.commerce.entity.Brand;
import com.project.sneakermarket.commerce.entity.Category;
import com.project.sneakermarket.notification.dto.NotificationSubscriptionRequest;
import com.project.sneakermarket.notification.dto.NotificationSubscriptionResponse;
import com.project.sneakermarket.notification.entity.NotificationSubscription;
import com.project.sneakermarket.notification.entity.NotificationSubscriptionTargetType;
import com.project.sneakermarket.notification.repository.NotificationSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.project.sneakermarket.commerce.repository.BrandRepository;
import com.project.sneakermarket.commerce.repository.CategoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationSubscriptionService {

    private final NotificationSubscriptionRepository subscriptionRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<NotificationSubscriptionResponse> getMySubscriptions(
            Long memberId
    ) {
        return subscriptionRepository
                .findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /*
     * 重複送出相同關注條件時直接回傳原本資料，
     * 不建立重複紀錄。
     */
    @Transactional
    public NotificationSubscriptionResponse subscribe(
            Long memberId,
            NotificationSubscriptionRequest request
    ) {
        validateTarget(
                request.targetType(),
                request.targetId()
        );

        NotificationSubscription subscription =
                subscriptionRepository
                        .findByMemberIdAndTargetTypeAndTargetIdAndScope(
                                memberId,
                                request.targetType(),
                                request.targetId(),
                                request.scope()
                        )
                        .orElseGet(() -> {
                            NotificationSubscription created =
                                    new NotificationSubscription();

                            created.setMemberId(memberId);
                            created.setTargetType(
                                    request.targetType()
                            );
                            created.setTargetId(
                                    request.targetId()
                            );
                            created.setScope(
                                    request.scope()
                            );

                            return subscriptionRepository.save(
                                    created
                            );
                        });

        return toResponse(subscription);
    }

    @Transactional
    public void unsubscribe(
            Long memberId,
            Long subscriptionId
    ) {
        NotificationSubscription subscription =
                subscriptionRepository
                        .findBySubscriptionIdAndMemberId(
                                subscriptionId,
                                memberId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "找不到關注資料"
                                )
                        );

        subscriptionRepository.delete(subscription);
    }

    private void validateTarget(
            NotificationSubscriptionTargetType targetType,
            Long targetId
    ) {
        boolean exists = switch (targetType) {
            case BRAND ->
                    brandRepository.existsById(targetId);

            case CATEGORY ->
                    categoryRepository.existsById(targetId);
        };

        if (!exists) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    targetType == NotificationSubscriptionTargetType.BRAND
                            ? "指定的品牌不存在"
                            : "指定的鞋款類別不存在"
            );
        }
    }

    private NotificationSubscriptionResponse toResponse(
            NotificationSubscription subscription
    ) {
        return new NotificationSubscriptionResponse(
                subscription.getSubscriptionId(),
                subscription.getTargetType(),
                subscription.getTargetId(),
                getTargetName(subscription),
                subscription.getScope(),
                subscription.getCreatedAt()
        );
    }

    private String getTargetName(
            NotificationSubscription subscription
    ) {
        if (subscription.getTargetType()
                == NotificationSubscriptionTargetType.BRAND) {
            return brandRepository
                    .findById(subscription.getTargetId())
                    .map(Brand::getBrandName)
                    .orElse("已移除的品牌");
        }

        return categoryRepository
                .findById(subscription.getTargetId())
                .map(Category::getCategoryName)
                .orElse("已移除的類別");
    }
}