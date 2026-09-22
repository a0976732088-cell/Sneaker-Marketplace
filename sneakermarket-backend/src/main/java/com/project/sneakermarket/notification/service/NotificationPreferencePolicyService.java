package com.project.sneakermarket.notification.service;

import com.project.sneakermarket.notification.entity.NotificationPreference;
import com.project.sneakermarket.notification.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationPreferencePolicyService {

    private final NotificationPreferenceRepository preferenceRepository;

    /*
     * 單一收件人的通知管道判斷。
     * 適合活動提醒排程等一次處理一位會員的流程。
     */
    @Transactional(readOnly = true)
    public DeliveryDecision resolve(
            Long memberId,
            String notificationType,
            String targetType
    ) {
        NotificationPreference preference =
                preferenceRepository
                        .findByMemberId(memberId)
                        .orElse(null);

        return decide(
                preference,
                notificationType,
                targetType
        );
    }

    /*
     * 群發通知使用，一次取得全部會員偏好，
     * 避免每位會員各查一次資料庫。
     */
    @Transactional(readOnly = true)
    public Map<Long, DeliveryDecision> resolveForMembers(
            Collection<Long> memberIds,
            String notificationType,
            String targetType
    ) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, NotificationPreference> preferences =
                preferenceRepository
                        .findAllByMemberIdIn(memberIds)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        NotificationPreference::getMemberId,
                                        Function.identity()
                                )
                        );

        Map<Long, DeliveryDecision> result =
                new HashMap<>();

        for (Long memberId : memberIds) {
            result.put(
                    memberId,
                    decide(
                            preferences.get(memberId),
                            notificationType,
                            targetType
                    )
            );
        }

        return result;
    }

    private DeliveryDecision decide(
            NotificationPreference preference,
            String notificationType,
            String targetType
    ) {
        /*
         * 尚未建立偏好的會員使用預設值：
         * 站內通知與 Email 都開啟。
         */
        if (preference == null) {
            return DeliveryDecision.allEnabled();
        }

        NotificationCategory category =
                resolveCategory(
                        notificationType,
                        targetType
                );

        /*
         * 目前競標及訂單通知不納入第 4 點的三種類型，
         * 保持既有行為，避免付款、得標等重要通知被關閉。
         */
        if (category == NotificationCategory.UNMANAGED) {
            return DeliveryDecision.allEnabled();
        }

        if (!Boolean.TRUE.equals(
                preference.getAllEnabled()
        )) {
            return DeliveryDecision.none();
        }

        return switch (category) {
            case EVENT -> new DeliveryDecision(
                    Boolean.TRUE.equals(
                            preference.getEventInAppEnabled()
                    ),
                    Boolean.TRUE.equals(
                            preference.getEventEmailEnabled()
                    )
            );

            case RELEASE -> new DeliveryDecision(
                    Boolean.TRUE.equals(
                            preference.getReleaseInAppEnabled()
                    ),
                    Boolean.TRUE.equals(
                            preference.getReleaseEmailEnabled()
                    )
            );

            case SYSTEM -> new DeliveryDecision(
                    Boolean.TRUE.equals(
                            preference.getSystemInAppEnabled()
                    ),
                    Boolean.TRUE.equals(
                            preference.getSystemEmailEnabled()
                    )
            );

            case UNMANAGED ->
                    DeliveryDecision.allEnabled();
        };
    }

    private NotificationCategory resolveCategory(
            String notificationType,
            String targetType
    ) {
        String cleanType = normalize(notificationType);
        String cleanTarget = normalize(targetType);

        if ("EVENT".equals(cleanTarget)
                || "EVENT_CANCELLED".equals(cleanTarget)
                || cleanType.startsWith("EVENT_")) {
            return NotificationCategory.EVENT;
        }

        if ("PRODUCT".equals(cleanTarget)
                || "PRODUCT_RELEASE".equals(cleanType)
                || "NEW_PRODUCT".equals(cleanType)
                || cleanType.contains("RELEASE")) {
            return NotificationCategory.RELEASE;
        }

        /*
         * 競標與交易通知先維持既有行為。
         */
        if ("AUCTION".equals(cleanTarget)
                || "ORDER".equals(cleanTarget)
                || cleanType.startsWith("AUCTION_")
                || cleanType.startsWith("ORDER_")
                || cleanType.contains("OUTBID")
                || cleanType.contains("BID_")) {
            return NotificationCategory.UNMANAGED;
        }

        /*
         * 後台手動建立但沒有明確分類的通知，
         * 預設視為系統公告。
         */
        return NotificationCategory.SYSTEM;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private enum NotificationCategory {
        EVENT,
        SYSTEM,
        RELEASE,
        UNMANAGED
    }

    public record DeliveryDecision(
            boolean inAppEnabled,
            boolean emailEnabled
    ) {
        public static DeliveryDecision allEnabled() {
            return new DeliveryDecision(true, true);
        }

        public static DeliveryDecision none() {
            return new DeliveryDecision(false, false);
        }

        public boolean anyEnabled() {
            return inAppEnabled || emailEnabled;
        }
    }
}