package com.project.sneakermarket.notification.service;

import com.project.sneakermarket.commerce.entity.Product;
import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.member.repository.MemberRepository;
import com.project.sneakermarket.notification.entity.Notification;
import com.project.sneakermarket.notification.entity.NotificationJobStatus;
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
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductReleaseNotificationService {

    private static final int SCAN_BATCH_SIZE = 50;

    private final ProductReleaseNotificationDispatchRepository
            dispatchRepository;

    private final NotificationSubscriptionRepository
            subscriptionRepository;

    private final NotificationRepository notificationRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final MemberRepository memberRepository;
    private final NotificationEmailJobService emailJobService;
    private final NotificationPreferencePolicyService preferencePolicyService;

    @PersistenceContext
    private EntityManager entityManager;

    /*
     * 只讀取尚未處理的 ON_SALE 商品 ID。
     * 不修改 ProductRepository。
     */
    @Transactional(readOnly = true)
    public List<Long> findPendingProductIds() {
        return entityManager
                .createQuery(
                        """
                        SELECT product.productId
                        FROM Product product
                        WHERE product.status = 'ON_SALE'
                          AND NOT EXISTS (
                              SELECT dispatch.dispatchId
                              FROM ProductReleaseNotificationDispatch dispatch
                              WHERE dispatch.productId = product.productId
                          )
                        ORDER BY product.productId ASC
                        """,
                        Long.class
                )
                .setMaxResults(SCAN_BATCH_SIZE)
                .getResultList();
    }

    /*
     * 每項商品使用獨立 Transaction 處理。
     */
    @Transactional
    public void processProduct(Long productId) {
        /*
         * 鎖定商品資料，避免多個排程同時處理同一商品。
         * 只做資料庫鎖定，不修改商品內容。
         */
        Product product = entityManager.find(
                Product.class,
                productId,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (product == null
                || !"ON_SALE".equals(product.getStatus())) {
            return;
        }

        if (dispatchRepository.existsByProductId(productId)) {
            return;
        }

        List<NotificationSubscription> brandSubscriptions =
                subscriptionRepository
                        .findByScopeAndTargetTypeAndTargetId(
                                NotificationSubscriptionScope.NEW_PRODUCT,
                                NotificationSubscriptionTargetType.BRAND,
                                product.getBrand().getBrandId()
                        );

        List<NotificationSubscription> categorySubscriptions =
                subscriptionRepository
                        .findByScopeAndTargetTypeAndTargetId(
                                NotificationSubscriptionScope.NEW_PRODUCT,
                                NotificationSubscriptionTargetType.CATEGORY,
                                product.getCategory().getCategoryId()
                        );

        /*
         * LinkedHashSet 同時負責品牌／類別收件人去重。
         */
        Set<Long> subscribedMemberIds =
                new LinkedHashSet<>();

        brandSubscriptions.stream()
                .map(NotificationSubscription::getMemberId)
                .forEach(subscribedMemberIds::add);

        categorySubscriptions.stream()
                .map(NotificationSubscription::getMemberId)
                .forEach(subscribedMemberIds::add);

        int notificationCount = 0;

        if (!subscribedMemberIds.isEmpty()) {
            notificationCount =
                    createNotifications(
                            product,
                            subscribedMemberIds
                    );
        }

        /*
         * 無論目前有沒有關注者，都標記商品已處理。
         * 之後編輯商品時不會補發或重複發送。
         */
        ProductReleaseNotificationDispatch dispatch =
                new ProductReleaseNotificationDispatch();

        dispatch.setProductId(productId);
        dispatch.setNotificationCount(
                notificationCount
        );

        dispatchRepository.save(dispatch);

        if (notificationCount > 0) {
            createNotificationLog(
                    product,
                    notificationCount
            );
        }
    }

    private int createNotifications(
            Product product,
            Set<Long> subscribedMemberIds
    ) {
        Map<Long, Member> activeMembers =
                memberRepository
                        .findAllById(subscribedMemberIds)
                        .stream()
                        .filter(member ->
                                member.getDeletedAt() == null
                        )
                        .collect(
                                Collectors.toMap(
                                        Member::getMemberId,
                                        Function.identity()
                                )
                        );

        if (activeMembers.isEmpty()) {
            return 0;
        }

        Map<Long, DeliveryDecision> decisions =
                preferencePolicyService.resolveForMembers(
                        activeMembers.keySet(),
                        "PRODUCT_RELEASE",
                        "PRODUCT"
                );

        LocalDateTime now =
                LocalDateTime.now();

        String title =
                "新品上架："
                        + product.getProductName();

        String content =
                product.getBrand().getBrandName()
                        + " 的新品「"
                        + product.getProductName()
                        + "」已正式上架。";

        List<Notification> notifications =
                activeMembers.keySet()
                        .stream()
                        .filter(memberId -> {
                            DeliveryDecision decision =
                                    decisions.get(memberId);

                            return decision != null
                                    && decision.anyEnabled();
                        })
                        /*
                         * 資料庫已有相同會員／商品通知時跳過，
                         * 作為第二層防重。
                         */
                        .filter(memberId ->
                                !notificationRepository
                                        .existsByMemberIdAndNotificationTypeAndTargetTypeAndTargetId(
                                                memberId,
                                                "PRODUCT_RELEASE",
                                                "PRODUCT",
                                                product.getProductId()
                                        )
                        )
                        .map(memberId -> {
                            DeliveryDecision decision =
                                    decisions.get(memberId);

                            Notification notification =
                                    new Notification();

                            notification.setMemberId(memberId);
                            notification.setNotificationType(
                                    "PRODUCT_RELEASE"
                            );

                            notification.setTitle(title);
                            notification.setContent(content);

                            notification.setTargetType(
                                    "PRODUCT"
                            );

                            notification.setTargetId(
                                    product.getProductId()
                            );

                            notification.setTargetUrl(
                                    "/products/"
                                            + product.getProductId()
                            );

                            notification.setIsRead(false);
                            notification.setIsSent(true);
                            notification.setSentAt(now);

                            notification.setInAppVisible(
                                    decision.inAppEnabled()
                            );

                            return notification;
                        })
                        .toList();

        if (notifications.isEmpty()) {
            return 0;
        }

        List<Notification> savedNotifications =
                notificationRepository.saveAll(
                        notifications
                );

        for (Notification notification
                : savedNotifications) {

            DeliveryDecision decision =
                    decisions.get(
                            notification.getMemberId()
                    );

            if (decision == null
                    || !decision.emailEnabled()) {
                continue;
            }

            Member member =
                    activeMembers.get(
                            notification.getMemberId()
                    );

            if (member == null) {
                continue;
            }

            emailJobService.createPending(
                    notification.getNotificationId(),
                    member.getMemberId(),
                    member.getEmail()
            );
        }

        return savedNotifications.size();
    }

    private void createNotificationLog(
            Product product,
            int notificationCount
    ) {
        LocalDateTime now =
                LocalDateTime.now();

        NotificationLog log =
                new NotificationLog();

        log.setNotificationId(null);
        log.setSendType("PRODUCT_SUBSCRIBERS");

        log.setTitle(
                "新品上架："
                        + product.getProductName()
        );

        log.setTargetType("PRODUCT");
        log.setEventId(null);

        log.setStatus(
                NotificationJobStatus.COMPLETED
        );

        log.setTotalRecipients(notificationCount);
        log.setTotalSent(notificationCount);
        log.setFailedCount(0);

        log.setStartedAt(now);
        log.setCompletedAt(now);
        log.setErrorMessage(null);

        notificationLogRepository.save(log);
    }
}