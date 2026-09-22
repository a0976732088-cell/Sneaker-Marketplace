package com.project.sneakermarket.notification.repository;

import com.project.sneakermarket.notification.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationPreferenceRepository
        extends JpaRepository<NotificationPreference, Long> {

    Optional<NotificationPreference> findByMemberId(Long memberId);

    List<NotificationPreference> findAllByMemberIdIn(
            Collection<Long> memberIds
    );
}