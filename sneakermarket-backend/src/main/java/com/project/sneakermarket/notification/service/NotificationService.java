package com.project.sneakermarket.notification.service;


import com.project.sneakermarket.member.entity.Member;
import com.project.sneakermarket.notification.entity.NotificationJobStatus;
import com.project.sneakermarket.calendar.repository.EventFollowRepository;
import com.project.sneakermarket.member.repository.MemberRepository;
import com.project.sneakermarket.notification.dto.MemberNotificationResponse;
import com.project.sneakermarket.notification.dto.NotificationDTO;
import com.project.sneakermarket.notification.dto.NotificationSendRequest;
import com.project.sneakermarket.notification.entity.Notification;
import com.project.sneakermarket.notification.entity.NotificationLog;
import com.project.sneakermarket.notification.repository.NotificationLogRepository;
import com.project.sneakermarket.notification.repository.NotificationRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.project.sneakermarket.calendar.entity.CalendarEvent;
import com.project.sneakermarket.notification.service.NotificationPreferencePolicyService.DeliveryDecision;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    //發送消息批次數量:100人次
    private static final int NOTIFICATION_BATCH_SIZE = 100;

    private final NotificationRepository notificationRepo;
    private final NotificationLogRepository notificationLogRepo;
    private final MemberRepository memberRepo;
    private final EventFollowRepository eventFollowRepo;
    private final NotificationEmailJobService emailJobService;
    private final NotificationPreferencePolicyService preferencePolicyService;

    @PersistenceContext
    private EntityManager entityManager;


    public Notification createNotice(NotificationDTO dto) {
        Notification notice = new Notification();

        notice.setNotificationType(dto.getNotificationType());
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setTargetType(dto.getTargetType());
        notice.setTargetId(dto.getTargetId());
        notice.setTargetUrl(dto.getTargetUrl());

        return notificationRepo.save(notice);
    }

    public void updateNoticeById(
            Long notificationId,
            NotificationDTO dto
    ) {
        Notification notification =
                notificationRepo
                        .findById(notificationId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "找不到指定通知"
                                )
                        );

        if (Boolean.TRUE.equals(notification.getIsSent())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "已發送的通知不可編輯"
            );
        }

        notification.setNotificationType(
                dto.getNotificationType()
        );

        notification.setTitle(dto.getTitle());
        notification.setContent(dto.getContent());

        notification.setTargetType(
                dto.getTargetType()
        );

        notification.setTargetId(dto.getTargetId());
        notification.setTargetUrl(dto.getTargetUrl());

        notificationRepo.save(notification);
    }


    public void deleteNoticeById(Long notificationId) {
        notificationRepo.deleteById(notificationId);
    }

    public Page<Notification> searchNotifications(
            // Long memberId, // ⏳ 待會員系統完成後解開
            String title,
            String notificationType,
            String targetType,
            Boolean isRead,
            Boolean isSent,
            LocalDateTime startCreatedAt,
            LocalDateTime endCreatedAt,
            LocalDateTime startReadAt,
            LocalDateTime endReadAt,
            Integer pageNumber,
            Integer pageSize) {

        Specification<Notification> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(
                    cb.isNull(root.get("memberId"))
            );
            /* ⏳ 待會員系統完成後解开：
            if (memberId != null) {
                predicates.add(cb.equal(root.get("memberId"), memberId));
            }
            */
            // 1. 模糊搜尋通知標題
            if (title != null && !title.trim().isEmpty()) {
                predicates.add(cb.like(root.get("title"), "%" + title.trim() + "%"));
            }
            // 2. 精確匹配通知類型 (如: ORDER_CREATED, SYSTEM)
            if (notificationType != null && !notificationType.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("notificationType"), notificationType.trim()));
            }
            // 3. 精確匹配跳轉資料類型 (如: ORDER, AUCTION, EVENT)
            if (targetType != null && !targetType.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("targetType"), targetType.trim()));
            }
            // 4. 是否已讀
            if (isRead != null) {
                predicates.add(cb.equal(root.get("isRead"), isRead));
            }
            if (isSent != null) {
                predicates.add(
                        cb.equal(root.get("isSent"), isSent)
                );
            }
            // 5. 通知產生時間區間 (createdAt >= startCreatedAt & <= endCreatedAt)
            if (startCreatedAt != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startCreatedAt));
            }
            if (endCreatedAt != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endCreatedAt));
            }
            // 6. 會員讀取通知時間區間 (readAt >= startReadAt & <= endReadAt)
            if (startReadAt != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("readAt"), startReadAt));
            }
            if (endReadAt != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("readAt"), endReadAt));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        // 分頁處理（第 1 頁轉成索引 0）
        int page = (pageNumber != null && pageNumber > 0) ? pageNumber - 1 : 0;
        int size = (pageSize != null && pageSize > 0) ? pageSize : 10;
        // 預設依照建立時間（最新到最舊）排序
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        return notificationRepo.findAll(spec, pageable);
    }

    // 發送通知邏輯
    /**
     * 同步發送使用：
     * 發送完成後建立一筆 COMPLETED Log。
     */
    @Transactional
    public int sendNotification(
            Long notificationId,
            NotificationSendRequest request
    ) {
        return sendNotification(
                notificationId,
                request,
                true
        );
    }

    /**
     * createCompletedLog：
     * true  = 同步發送，由這個方法建立 Log
     * false = Redis 發送，由 NotificationJobLogService 更新既有 Log
     * 實際執行發送
     */
    @Transactional
    public int sendNotification(
            Long notificationId,
            NotificationSendRequest request,
            boolean createCompletedLog
    ) {
        // 1. 取得管理員建立的通知範本
        Notification template = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "找不到通知，ID：" + notificationId
                ));

        // 2. 有 memberId 代表這是某位會員的通知，不可以再次群發
        if (template.getMemberId() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "會員專屬通知不能作為群發範本"
            );
        }

        // 3. 避免管理員重複按下發送
        if (Boolean.TRUE.equals(template.getIsSent())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "這則通知已經發送過"
            );
        }

        // 4. 取得所有有效會員 ID
        String audienceType = request.getAudienceType();

        List<Long> memberIds =
                resolveRecipientMemberIds(
                        template,
                        audienceType
                );

        if (memberIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "目前沒有可以接收通知的會員"
            );
        }

        List<Long> uniqueMemberIds =
                memberIds.stream()
                        .distinct()
                        .toList();

        Map<Long, DeliveryDecision> deliveryDecisions =
                preferencePolicyService.resolveForMembers(
                        uniqueMemberIds,
                        template.getNotificationType(),
                        template.getTargetType()
                );

        List<Long> deliverableMemberIds =
                uniqueMemberIds.stream()
                        .filter(memberId -> {
                            DeliveryDecision decision =
                                    deliveryDecisions.get(memberId);

                            return decision != null
                                    && decision.anyEnabled();
                        })
                        .toList();

        if (deliverableMemberIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "所有收件人都已關閉此類通知"
            );
        }

        LocalDateTime sentAt = LocalDateTime.now();

        int totalSent = 0;

        for (
                int start = 0;
                start < deliverableMemberIds.size();
                start += NOTIFICATION_BATCH_SIZE
        ) {
            int end = Math.min(
                    start + NOTIFICATION_BATCH_SIZE,
                    deliverableMemberIds.size()
            );

            List<Long> batchMemberIds =
                    deliverableMemberIds.subList(start, end);

            List<Notification> batchNotifications =
                    batchMemberIds.stream()
                            .map(memberId ->
                                    createMemberNotification(
                                            template,
                                            memberId,
                                            sentAt,
                                            deliveryDecisions.get(memberId)
                                    )
                            )
                            .toList();


            List<Notification> savedNotifications =
                    notificationRepo.saveAll(batchNotifications);

            createEmailJobs(
                    savedNotifications,
                    deliveryDecisions
            );

            entityManager.flush();


            entityManager.clear();

            totalSent += batchNotifications.size();
        }

        // 6. 把原始範本標記為已發送
        template.setIsSent(true);
        template.setSentAt(sentAt);
        notificationRepo.save(template);

        if (createCompletedLog) {
            // 7. 建立發送紀錄
            NotificationLog log = new NotificationLog();

            // 對應管理員建立的通知範本
            log.setNotificationId(
                    template.getNotificationId()
            );

            // 發送對象：ALL_MEMBERS 或 EVENT_FOLLOWERS
            log.setSendType(audienceType);

            log.setTitle(template.getTitle());

            log.setTargetType(
                    template.getTargetType() != null
                            ? template.getTargetType()
                            : "SYSTEM"
            );

            // 只有活動關注者通知才有 eventId
            if ("EVENT_FOLLOWERS".equals(audienceType)) {
                log.setEventId(template.getTargetId());
            } else {
                log.setEventId(null);
            }

            // 目前仍是同步執行，執行到這裡就代表全部成功
            log.setStatus(
                    NotificationJobStatus.COMPLETED
            );

            // 預計收件人數
            log.setTotalRecipients(totalSent);

            // 實際成功人數
            log.setTotalSent(totalSent);

            // 失敗人數
            log.setFailedCount(0);

            // 開始與完成時間
            log.setStartedAt(sentAt);
            log.setCompletedAt(LocalDateTime.now());

            // 成功時沒有錯誤訊息
            log.setErrorMessage(null);

            notificationLogRepo.save(log);
        }
        // 9. 回傳實際發送數量
        return totalSent;
    }

    // 取得已發送通知
    public List<Notification> getSentNotifications() {
        return notificationRepo.findByIsSentTrueOrderByCreatedAtDesc();
    }

    @Transactional
    public void markAsRead(
            Long notificationId,
            Long memberId
    ) {
        Notification notification = notificationRepo
                .findByNotificationIdAndMemberIdAndInAppVisibleTrue(
                        notificationId,
                        memberId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "找不到通知"
                ));

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
    }

    @Transactional
    public void markAllAsRead(Long memberId) {
        LocalDateTime now = LocalDateTime.now();

        List<Notification> notifications = notificationRepo
                .findByMemberIdAndInAppVisibleTrueAndIsSentTrueOrderByCreatedAtDesc(
                        memberId
                );

        for (Notification notification : notifications) {
            if (!Boolean.TRUE.equals(notification.getIsRead())) {
                notification.setIsRead(true);
                notification.setReadAt(now);
            }
        }
    }

    @Transactional
    public List<MemberNotificationResponse> getMemberNotifications(
            Long memberId
    ) {
        return notificationRepo
                .findByMemberIdAndInAppVisibleTrueAndIsSentTrueOrderByCreatedAtDesc(
                        memberId
                )
                .stream()
                .map(MemberNotificationResponse::from)
                .toList();
    }

    @Transactional
    public void createEventTimeChangedNotifications(
            CalendarEvent event,
            LocalDateTime previousStartAt,
            List<Long> followerMemberIds
    ) {
        if (followerMemberIds == null
                || followerMemberIds.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "yyyy/MM/dd HH:mm"
                );

        String oldTime =
                previousStartAt.format(formatter);

        String newTime =
                event.getStartAt().format(formatter);

        String notificationTitle =
                "活動時間異動：" + event.getTitle();

        /*
         * 一次取得全部活動關注者的通知偏好。
         */
        Map<Long, DeliveryDecision> deliveryDecisions =
                preferencePolicyService.resolveForMembers(
                        followerMemberIds,
                        "EVENT_TIME_CHANGED",
                        "EVENT"
                );

        /*
         * 站內通知與 Email 都關閉的會員不建立通知。
         */
        List<Long> deliverableMemberIds =
                followerMemberIds.stream()
                        .distinct()
                        .filter(memberId -> {
                            DeliveryDecision decision =
                                    deliveryDecisions.get(
                                            memberId
                                    );

                            return decision != null
                                    && decision.anyEnabled();
                        })
                        .toList();

        if (deliverableMemberIds.isEmpty()) {
            return;
        }

        List<Notification> notifications =
                deliverableMemberIds.stream()
                        .map(memberId -> {
                            DeliveryDecision decision =
                                    deliveryDecisions.get(
                                            memberId
                                    );

                            Notification notification =
                                    new Notification();

                            notification.setMemberId(memberId);

                            notification.setNotificationType(
                                    "EVENT_TIME_CHANGED"
                            );

                            notification.setTitle(
                                    notificationTitle
                            );

                            notification.setContent(
                                    "您關注的活動時間已從 "
                                            + oldTime
                                            + " 調整為 "
                                            + newTime
                                            + "。"
                            );

                            notification.setTargetType(
                                    "EVENT"
                            );

                            notification.setTargetId(
                                    event.getEventId()
                            );

                            notification.setTargetUrl(
                                    event.getTargetUrl()
                            );

                            notification.setIsRead(false);
                            notification.setIsSent(true);
                            notification.setSentAt(now);

                            /*
                             * Email-only 的通知仍保存於資料庫，
                             * 但不顯示在會員通知抽屜。
                             */
                            notification.setInAppVisible(
                                    decision != null
                                            && decision.inAppEnabled()
                            );

                            return notification;
                        })
                        .toList();

        List<Notification> savedNotifications =
                notificationRepo.saveAll(
                        notifications
                );

        /*
         * 只有開啟 Email 的會員才建立 Email Job。
         */
        createEmailJobs(
                savedNotifications,
                deliveryDecisions
        );

        createCompletedAutomaticEventLog(
                event,
                notificationTitle,
                savedNotifications.size(),
                now
        );
    }

    @Transactional
    public void createEventCancelledNotifications(
            CalendarEvent event,
            List<Long> followerMemberIds
    ) {
        if (followerMemberIds == null
                || followerMemberIds.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "yyyy/MM/dd HH:mm"
                );

        String originalStartTime =
                event.getStartAt().format(formatter);

        String notificationTitle =
                "活動已取消：" + event.getTitle();

        /*
         * 活動取消也使用活動通知偏好。
         */
        Map<Long, DeliveryDecision> deliveryDecisions =
                preferencePolicyService.resolveForMembers(
                        followerMemberIds,
                        "EVENT_CANCELLED",
                        "EVENT_CANCELLED"
                );

        List<Long> deliverableMemberIds =
                followerMemberIds.stream()
                        .distinct()
                        .filter(memberId -> {
                            DeliveryDecision decision =
                                    deliveryDecisions.get(
                                            memberId
                                    );

                            return decision != null
                                    && decision.anyEnabled();
                        })
                        .toList();

        if (deliverableMemberIds.isEmpty()) {
            return;
        }

        List<Notification> notifications =
                deliverableMemberIds.stream()
                        .map(memberId -> {
                            DeliveryDecision decision =
                                    deliveryDecisions.get(
                                            memberId
                                    );

                            Notification notification =
                                    new Notification();

                            notification.setMemberId(memberId);

                            notification.setNotificationType(
                                    "EVENT_CANCELLED"
                            );

                            notification.setTitle(
                                    notificationTitle
                            );

                            notification.setContent(
                                    "您關注的活動原訂於 "
                                            + originalStartTime
                                            + " 開始，目前已由管理員取消。"
                            );

                            /*
                             * 活動取消後不能再進入活動內容，
                             * 因此保留 eventId，但不提供跳轉網址。
                             */
                            notification.setTargetType(
                                    "EVENT_CANCELLED"
                            );

                            notification.setTargetId(
                                    event.getEventId()
                            );

                            notification.setTargetUrl(null);

                            notification.setIsRead(false);
                            notification.setIsSent(true);
                            notification.setSentAt(now);

                            notification.setInAppVisible(
                                    decision != null
                                            && decision.inAppEnabled()
                            );

                            return notification;
                        })
                        .toList();

        List<Notification> savedNotifications =
                notificationRepo.saveAll(
                        notifications
                );

        createEmailJobs(
                savedNotifications,
                deliveryDecisions
        );

        createCompletedAutomaticEventLog(
                event,
                notificationTitle,
                savedNotifications.size(),
                now
        );
    }

    private void createEmailJobs(
            List<Notification> savedNotifications,
            Map<Long, DeliveryDecision> deliveryDecisions
    ) {
        if (savedNotifications == null
                || savedNotifications.isEmpty()) {
            return;
        }

        List<Long> emailMemberIds =
                savedNotifications.stream()
                        .map(Notification::getMemberId)
                        .filter(memberId -> memberId != null)
                        .filter(memberId -> {
                            DeliveryDecision decision =
                                    deliveryDecisions.get(memberId);

                            return decision != null
                                    && decision.emailEnabled();
                        })
                        .distinct()
                        .toList();

        if (emailMemberIds.isEmpty()) {
            return;
        }

        Map<Long, Member> memberMap =
                memberRepo.findAllById(emailMemberIds)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Member::getMemberId,
                                        Function.identity()
                                )
                        );

        for (Notification notification : savedNotifications) {
            DeliveryDecision decision =
                    deliveryDecisions.get(
                            notification.getMemberId()
                    );

            if (decision == null
                    || !decision.emailEnabled()) {
                continue;
            }

            Member member =
                    memberMap.get(
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
    }

    private void createCompletedAutomaticEventLog(
            CalendarEvent event,
            String title,
            int totalSent,
            LocalDateTime startedAt
    ) {
        NotificationLog log = new NotificationLog();

        /*
         * 自動通知沒有管理員建立的通知範本，
         * 因此 notificationId 保持 null。
         */
        log.setNotificationId(null);

        log.setSendType("EVENT_FOLLOWERS");
        log.setTitle(title);
        log.setTargetType("EVENT");
        log.setEventId(event.getEventId());

        log.setStatus(NotificationJobStatus.COMPLETED);
        log.setTotalRecipients(totalSent);
        log.setTotalSent(totalSent);
        log.setFailedCount(0);

        log.setStartedAt(startedAt);
        log.setCompletedAt(LocalDateTime.now());
        log.setErrorMessage(null);

        notificationLogRepo.save(log);
    }



    private Notification createMemberNotification(
            Notification template,
            Long memberId,
            LocalDateTime sentAt,
            DeliveryDecision decision
    ) {
        Notification notification = new Notification();

        notification.setMemberId(memberId);

        notification.setNotificationType(
                template.getNotificationType()
        );

        notification.setTitle(
                template.getTitle()
        );

        notification.setContent(
                template.getContent()
        );

        notification.setTargetType(
                template.getTargetType()
        );

        notification.setTargetId(
                template.getTargetId()
        );

        notification.setTargetUrl(
                template.getTargetUrl()
        );

        notification.setIsRead(false);
        notification.setReadAt(null);

        notification.setIsSent(true);
        notification.setSentAt(sentAt);

        notification.setInAppVisible(
                decision != null
                        && decision.inAppEnabled()
        );

        return notification;
    }
    private List<Long> resolveRecipientMemberIds(
            Notification template,
            String audienceType
    ) {
        if (audienceType == null
                || audienceType.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "請選擇通知發送對象"
            );
        }

        // 全體有效會員
        if ("ALL_MEMBERS".equals(audienceType)) {
            return getActiveMemberIds();
        }

        // 指定活動的關注者
        if ("EVENT_FOLLOWERS".equals(audienceType)) {

            if (!"EVENT".equals(template.getTargetType())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "發送給活動關注者時，關聯類型必須是 EVENT"
                );
            }

            if (template.getTargetId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "發送給活動關注者時，必須指定活動 ID"
                );
            }

            List<Long> activeMemberIds =
                    getActiveMemberIds();

            return eventFollowRepo
                    .findByEventId(template.getTargetId())
                    .stream()
                    .map(eventFollow ->
                            eventFollow.getMemberId()
                    )
                    .filter(activeMemberIds::contains)
                    .distinct()
                    .toList();
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "不支援的發送對象：" + audienceType
        );
    }

    private List<Long> getActiveMemberIds() {
        return memberRepo.findAll()
                .stream()
                .filter(member ->
                        member.getDeletedAt() == null
                )
                .map(member ->
                        member.getMemberId()
                )
                .distinct()
                .toList();
    }

}
