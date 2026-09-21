package com.project.sneakermarket.notification.service;

import com.project.sneakermarket.notification.entity.NotificationPreference;
import com.project.sneakermarket.notification.repository.NotificationPreferenceRepository;
import com.project.sneakermarket.notification.service.NotificationPreferencePolicyService.DeliveryDecision;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationPreferencePolicyServiceTests {

    private NotificationPreferenceRepository
            preferenceRepository;

    private NotificationPreferencePolicyService
            service;

    @BeforeEach
    void setUp() {
        preferenceRepository =
                mock(
                        NotificationPreferenceRepository.class
                );

        service =
                new NotificationPreferencePolicyService(
                        preferenceRepository
                );
    }

    @Test
    void memberWithoutPreferenceUsesDefaultEnabled() {
        when(
                preferenceRepository.findByMemberId(
                        1L
                )
        ).thenReturn(Optional.empty());

        DeliveryDecision decision =
                service.resolve(
                        1L,
                        "EVENT_REMINDER",
                        "EVENT"
                );

        assertAll(
                () -> assertTrue(
                        decision.inAppEnabled()
                ),
                () -> assertTrue(
                        decision.emailEnabled()
                ),
                () -> assertTrue(
                        decision.anyEnabled()
                )
        );
    }

    @Test
    void masterSwitchDisablesManagedNotification() {
        NotificationPreference preference =
                createPreference(1L);

        preference.setAllEnabled(false);

        when(
                preferenceRepository.findByMemberId(
                        1L
                )
        ).thenReturn(
                Optional.of(preference)
        );

        DeliveryDecision decision =
                service.resolve(
                        1L,
                        "EVENT_REMINDER",
                        "EVENT"
                );

        assertFalse(
                decision.inAppEnabled()
        );

        assertFalse(
                decision.emailEnabled()
        );

        assertFalse(
                decision.anyEnabled()
        );
    }

    @Test
    void eventChannelsAreResolvedIndependently() {
        NotificationPreference preference =
                createPreference(1L);

        preference.setEventInAppEnabled(true);
        preference.setEventEmailEnabled(false);

        when(
                preferenceRepository.findByMemberId(
                        1L
                )
        ).thenReturn(
                Optional.of(preference)
        );

        DeliveryDecision decision =
                service.resolve(
                        1L,
                        "EVENT_REMINDER",
                        "EVENT"
                );

        assertTrue(
                decision.inAppEnabled()
        );

        assertFalse(
                decision.emailEnabled()
        );
    }

    @Test
    void releaseChannelsUseReleasePreference() {
        NotificationPreference preference =
                createPreference(1L);

        preference.setReleaseInAppEnabled(false);
        preference.setReleaseEmailEnabled(true);

        when(
                preferenceRepository.findByMemberId(
                        1L
                )
        ).thenReturn(
                Optional.of(preference)
        );

        DeliveryDecision decision =
                service.resolve(
                        1L,
                        "PRODUCT_RELEASE",
                        "PRODUCT"
                );

        assertFalse(
                decision.inAppEnabled()
        );

        assertTrue(
                decision.emailEnabled()
        );
    }

    @Test
    void auctionNotificationKeepsExistingBehavior() {
        NotificationPreference preference =
                createPreference(1L);

        /*
         * 競標功能目前不納入第 4 點偏好設定，
         * 即使總開關關閉仍維持既有通知行為。
         */
        preference.setAllEnabled(false);

        when(
                preferenceRepository.findByMemberId(
                        1L
                )
        ).thenReturn(
                Optional.of(preference)
        );

        DeliveryDecision decision =
                service.resolve(
                        1L,
                        "AUCTION_WON",
                        "AUCTION"
                );

        assertTrue(
                decision.inAppEnabled()
        );

        assertTrue(
                decision.emailEnabled()
        );
    }

    @Test
    void batchResolveIncludesMembersWithoutPreference() {
        NotificationPreference preference =
                createPreference(1L);

        preference.setSystemInAppEnabled(false);
        preference.setSystemEmailEnabled(true);

        when(
                preferenceRepository.findAllByMemberIdIn(
                        List.of(1L, 2L)
                )
        ).thenReturn(
                List.of(preference)
        );

        Map<Long, DeliveryDecision> decisions =
                service.resolveForMembers(
                        List.of(1L, 2L),
                        "SYSTEM_ANNOUNCEMENT",
                        "SYSTEM"
                );

        assertAll(
                () -> assertFalse(
                        decisions.get(1L)
                                .inAppEnabled()
                ),
                () -> assertTrue(
                        decisions.get(1L)
                                .emailEnabled()
                ),
                () -> assertTrue(
                        decisions.get(2L)
                                .inAppEnabled()
                ),
                () -> assertTrue(
                        decisions.get(2L)
                                .emailEnabled()
                )
        );
    }

    private NotificationPreference createPreference(
            Long memberId
    ) {
        NotificationPreference preference =
                new NotificationPreference();

        preference.setMemberId(memberId);
        preference.setAllEnabled(true);

        preference.setEventInAppEnabled(true);
        preference.setEventEmailEnabled(true);

        preference.setSystemInAppEnabled(true);
        preference.setSystemEmailEnabled(true);

        preference.setReleaseInAppEnabled(true);
        preference.setReleaseEmailEnabled(true);

        return preference;
    }
}