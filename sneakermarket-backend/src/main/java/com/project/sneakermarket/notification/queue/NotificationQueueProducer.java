package com.project.sneakermarket.notification.queue;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StringRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationQueueProducer {

    private final StringRedisTemplate redisTemplate;

    @Value("${notification.redis.stream-key}")
    private String streamKey;

    /**
     * 保留原本呼叫方式，避免目前 Controller 編譯失敗。
     */
    public String enqueue(
            Long notificationId,
            String audienceType
    ) {
        return enqueue(
                notificationId,
                audienceType,
                null
        );
    }

    /**
     * 新版 Redis 任務會一併保存 notification_logs 的 ID。
     */
    public String enqueue(
            Long notificationId,
            String audienceType,
            Long logId
    ) {
        Map<String, String> payload =
                new LinkedHashMap<>();

        payload.put(
                "notificationId",
                notificationId.toString()
        );

        payload.put(
                "audienceType",
                audienceType
        );

        /*
         * 尚未建立任務紀錄時允許 logId 為 null，
         * 方便目前程式漸進修改。
         */
        if (logId != null) {
            payload.put(
                    "logId",
                    logId.toString()
            );
        }

        StringRecord record =
                StreamRecords.string(payload)
                        .withStreamKey(streamKey);

        RecordId recordId =
                redisTemplate.opsForStream()
                        .add(record);

        if (recordId == null) {
            throw new IllegalStateException(
                    "Redis 通知任務建立失敗"
            );
        }

        return recordId.getValue();
    }
}