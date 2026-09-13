package com.project.sneakermarket.notification.queue;

import com.project.sneakermarket.notification.service.NotificationJobLogService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.project.sneakermarket.notification.dto.NotificationSendRequest;
import com.project.sneakermarket.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

//每秒執行一次
//    ↓
//確認 notification-workers 群組存在
//    ↓
//從 notification-send-jobs 讀取一筆任務
//    ↓
//取得 notificationId、audienceType
//    ↓
//建立 NotificationSendRequest
//    ↓
//呼叫原本的 sendNotification()
//    ↓
//成功：ACK 並刪除 Redis 任務
// 失敗：標記為 FAILED，ACK 並刪除 Redis 任務

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "notification.redis.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class NotificationQueueConsumer {

    private final StringRedisTemplate redisTemplate;
    private final NotificationService notificationService;
    private final NotificationJobLogService notificationJobLogService;

    @Value("${notification.redis.stream-key}")
    private String streamKey;

    @Value("${notification.redis.consumer-group}")
    private String consumerGroup;

    @Value("${notification.redis.consumer-name}")
    private String consumerName;

    private boolean consumerGroupReady = false;

    /**
     * 每秒檢查一次 Redis 是否有新的通知任務。
     */
    @Scheduled(
            fixedDelayString =
                    "${notification.redis.poll-delay-ms:1000}"
    )
    public void consumeNotificationJobs() {

        try {
            ensureConsumerGroup();

            List<MapRecord<String, Object, Object>> records =
                    redisTemplate.opsForStream().read(
                            Consumer.from(
                                    consumerGroup,
                                    consumerName
                            ),
                            StreamReadOptions.empty()
                                    .count(1),
                            StreamOffset.create(
                                    streamKey,
                                    ReadOffset.lastConsumed()
                            )
                    );

            if (records == null || records.isEmpty()) {
                return;
            }

            for (MapRecord<String, Object, Object> record
                    : records) {

                processRecord(record);
            }

        } catch (Exception exception) {
            log.error(
                    "讀取 Redis 通知任務失敗",
                    exception
            );
        }
    }

    /**
     * 建立 Redis Consumer Group。
     *
     * ReadOffset.from("0-0") 表示：
     * 如果 Stream 中原本已經有任務，也要從第一筆開始處理。
     */
    private void ensureConsumerGroup() {

        if (consumerGroupReady) {
            return;
        }

        try {
            redisTemplate.opsForStream().createGroup(
                    streamKey,
                    ReadOffset.from("0-0"),
                    consumerGroup
            );

            log.info(
                    "Redis 通知 Consumer Group 建立完成：{}",
                    consumerGroup
            );

        } catch (DataAccessException exception) {

            /*
             * Spring Boot 重新啟動時，Group 可能已經存在。
             * Redis 會回傳 BUSYGROUP，這是正常狀況。
             */
            if (!containsBusyGroupMessage(exception)) {
                throw exception;
            }

            log.info(
                    "Redis 通知 Consumer Group 已存在：{}",
                    consumerGroup
            );
        }

        consumerGroupReady = true;
    }

    /**
     * 處理一筆 Redis 通知任務。
     */
    private void processRecord(
            MapRecord<String, Object, Object> record
    ) {
        Long logId = null;

        try {
            Map<Object, Object> values =
                    record.getValue();

            Object notificationIdValue =
                    values.get("notificationId");

            Object audienceTypeValue =
                    values.get("audienceType");

            Object logIdValue =
                    values.get("logId");

            if (notificationIdValue == null) {
                throw new IllegalArgumentException(
                        "Redis 任務缺少 notificationId"
                );
            }

            if (audienceTypeValue == null) {
                throw new IllegalArgumentException(
                        "Redis 任務缺少 audienceType"
                );
            }

            if (logIdValue == null) {
                throw new IllegalArgumentException(
                        "Redis 任務缺少 logId"
                );
            }

            Long notificationId =
                    Long.valueOf(
                            notificationIdValue.toString()
                    );

            String audienceType =
                    audienceTypeValue.toString();

            logId =
                    Long.valueOf(
                            logIdValue.toString()
                    );

            /*
             * Consumer 已取得任務。
             */
            notificationJobLogService.markProcessing(
                    logId
            );

            NotificationSendRequest request =
                    new NotificationSendRequest();

            request.setAudienceType(audienceType);

            /*
             * false 表示不要另外建立 COMPLETED Log，
             * 因為已經有 PENDING Log。
             */
            int totalSent =
                    notificationService.sendNotification(
                            notificationId,
                            request,
                            false
                    );

            notificationJobLogService.markCompleted(
                    logId,
                    totalSent
            );

            redisTemplate.opsForStream().acknowledge(
                    consumerGroup,
                    record
            );

            redisTemplate.opsForStream().delete(record);

            log.info(
                    "Redis 通知任務完成，recordId={}，logId={}，notificationId={}，totalSent={}",
                    record.getId().getValue(),
                    logId,
                    notificationId,
                    totalSent
            );

        } catch (Exception exception) {
            /*
             * SQL 發送失敗時，sendNotification 的交易會回滾。
             */
            if (logId != null) {
                try {
                    notificationJobLogService.markFailed(
                            logId,
                            exception.getMessage()
                    );
                } catch (Exception logException) {
                    log.error(
                            "更新通知任務失敗狀態時發生錯誤，logId={}",
                            logId,
                            logException
                    );
                }
            }

            /*
             * 目前尚未實作自動重試。
             * 先將失敗訊息 ACK 並刪除，
             * 管理員可以修正問題後重新發送。
             */
            try {
                redisTemplate.opsForStream().acknowledge(
                        consumerGroup,
                        record
                );

                redisTemplate.opsForStream().delete(record);
            } catch (Exception redisException) {
                log.error(
                        "清除失敗的 Redis 任務時發生錯誤，recordId={}",
                        record.getId().getValue(),
                        redisException
                );
            }

            log.error(
                    "Redis 通知任務執行失敗，recordId={}，logId={}",
                    record.getId().getValue(),
                    logId,
                    exception
            );
        }
    }

    /**
     * 檢查例外中是否包含 Redis 的 BUSYGROUP 訊息。
     */
    private boolean containsBusyGroupMessage(
            Throwable throwable
    ) {
        Throwable current = throwable;

        while (current != null) {
            String message = current.getMessage();

            if (message != null
                    && message.contains("BUSYGROUP")) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}