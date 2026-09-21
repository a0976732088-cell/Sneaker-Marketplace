package com.project.sneakermarket.notification.repository;

import com.project.sneakermarket.notification.entity.NotificationSubscription;
import com.project.sneakermarket.notification.entity.NotificationSubscriptionScope;
import com.project.sneakermarket.notification.entity.NotificationSubscriptionTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationSubscriptionRepository
        extends JpaRepository<NotificationSubscription, Long> {

    List<NotificationSubscription>
    findByMemberIdOrderByCreatedAtDesc(Long memberId);

    Optional<NotificationSubscription>
    findByMemberIdAndTargetTypeAndTargetIdAndScope(
            Long memberId,
            NotificationSubscriptionTargetType targetType,
            Long targetId,
            NotificationSubscriptionScope scope
    );

    Optional<NotificationSubscription>
    findBySubscriptionIdAndMemberId(
            Long subscriptionId,
            Long memberId
    );

    List<NotificationSubscription>
    findByScopeAndTargetTypeAndTargetId(
            NotificationSubscriptionScope scope,
            NotificationSubscriptionTargetType targetType,
            Long targetId
    );
}