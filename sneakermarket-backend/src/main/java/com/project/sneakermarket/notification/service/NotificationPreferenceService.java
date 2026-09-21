package com.project.sneakermarket.notification.service;

import com.project.sneakermarket.notification.dto.NotificationPreferenceResponse;
import com.project.sneakermarket.notification.dto.NotificationPreferenceUpdateRequest;
import com.project.sneakermarket.notification.entity.NotificationPreference;
import com.project.sneakermarket.notification.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    /*
     * 第一次進入設定頁時，如果會員尚未有偏好資料，
     * 就建立一份全部開啟的預設設定。
     */
    @Transactional
    public NotificationPreferenceResponse getMyPreference(
            Long memberId
    ) {
        NotificationPreference preference =
                preferenceRepository
                        .findByMemberId(memberId)
                        .orElseGet(() -> createDefault(memberId));

        return NotificationPreferenceResponse.from(preference);
    }

    @Transactional
    public NotificationPreferenceResponse updateMyPreference(
            Long memberId,
            NotificationPreferenceUpdateRequest request
    ) {
        NotificationPreference preference =
                preferenceRepository
                        .findByMemberId(memberId)
                        .orElseGet(() -> newPreference(memberId));

        preference.setAllEnabled(request.allEnabled());

        preference.setEventInAppEnabled(
                request.eventInAppEnabled()
        );
        preference.setEventEmailEnabled(
                request.eventEmailEnabled()
        );

        preference.setSystemInAppEnabled(
                request.systemInAppEnabled()
        );
        preference.setSystemEmailEnabled(
                request.systemEmailEnabled()
        );

        preference.setReleaseInAppEnabled(
                request.releaseInAppEnabled()
        );
        preference.setReleaseEmailEnabled(
                request.releaseEmailEnabled()
        );

        NotificationPreference saved =
                preferenceRepository.save(preference);

        return NotificationPreferenceResponse.from(saved);
    }

    private NotificationPreference createDefault(Long memberId) {
        return preferenceRepository.save(
                newPreference(memberId)
        );
    }

    private NotificationPreference newPreference(Long memberId) {
        NotificationPreference preference =
                new NotificationPreference();

        preference.setMemberId(memberId);
        preference.setAllEnabled(true);

        /*
         * 舊版欄位仍為 NOT NULL，因此新增資料時一併設定。
         */
        preference.setAuctionEnabled(true);
        preference.setPromotionEnabled(true);
        preference.setReleaseEnabled(true);

        preference.setEventInAppEnabled(true);
        preference.setEventEmailEnabled(true);

        preference.setSystemInAppEnabled(true);
        preference.setSystemEmailEnabled(true);

        preference.setReleaseInAppEnabled(true);
        preference.setReleaseEmailEnabled(true);

        return preference;
    }
}