/* =========================================================
   notification_logs 升級成通知發送任務紀錄
   ========================================================= */

/*
 * 系統公告沒有 event_id。
 * 先移除 FK，將 event_id 改成可為 NULL，再重新建立 FK。
 */
IF EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = 'FK_notification_logs_calendar_events'
      AND parent_object_id =
          OBJECT_ID('dbo.notification_logs')
)
BEGIN
ALTER TABLE dbo.notification_logs
DROP CONSTRAINT FK_notification_logs_calendar_events;
END
GO

ALTER TABLE dbo.notification_logs
ALTER COLUMN event_id BIGINT NULL;
GO

-- 舊程式使用 0 代表沒有活動，改成真正的 NULL
UPDATE notificationLog
SET event_id = NULL
    FROM dbo.notification_logs AS notificationLog
LEFT JOIN dbo.calendar_events AS calendarEvent
ON calendarEvent.event_id = notificationLog.event_id
WHERE notificationLog.event_id IS NOT NULL
  AND calendarEvent.event_id IS NULL;
GO

ALTER TABLE dbo.notification_logs
    ADD CONSTRAINT FK_notification_logs_calendar_events
        FOREIGN KEY (event_id)
            REFERENCES dbo.calendar_events(event_id);
GO

/*
 * notification_id 指向管理員建立的通知範本
 */
ALTER TABLE dbo.notification_logs
    ADD notification_id BIGINT NULL;
GO

/*
 * 發送任務狀態
 */
ALTER TABLE dbo.notification_logs
    ADD status VARCHAR(20) NULL;
GO

/*
 * 預計收件人數
 */
ALTER TABLE dbo.notification_logs
    ADD total_recipients INT NULL;
GO

/*
 * 發送失敗人數
 */
ALTER TABLE dbo.notification_logs
    ADD failed_count INT NULL;
GO

/*
 * 實際開始與完成時間
 */
ALTER TABLE dbo.notification_logs
    ADD started_at DATETIME2(0) NULL;
GO

ALTER TABLE dbo.notification_logs
    ADD completed_at DATETIME2(0) NULL;
GO

/*
 * 失敗原因
 */
ALTER TABLE dbo.notification_logs
    ADD error_message NVARCHAR(1000) NULL;
GO

/*
 * 舊紀錄視為已完成
 */
UPDATE dbo.notification_logs
SET
    status = 'COMPLETED',
    total_recipients = total_sent,
    failed_count = 0,
    started_at = created_at,
    completed_at = created_at;
GO

/*
 * 完成舊資料更新後，加入 NOT NULL
 */
ALTER TABLE dbo.notification_logs
ALTER COLUMN status VARCHAR(20) NOT NULL;
GO

ALTER TABLE dbo.notification_logs
ALTER COLUMN total_recipients INT NOT NULL;
GO

ALTER TABLE dbo.notification_logs
ALTER COLUMN failed_count INT NOT NULL;
GO

/*
 * 新資料的預設值
 */
ALTER TABLE dbo.notification_logs
    ADD CONSTRAINT DF_notification_logs_status
        DEFAULT ('PENDING') FOR status;
GO

ALTER TABLE dbo.notification_logs
    ADD CONSTRAINT DF_notification_logs_total_recipients
        DEFAULT (0) FOR total_recipients;
GO

ALTER TABLE dbo.notification_logs
    ADD CONSTRAINT DF_notification_logs_failed_count
        DEFAULT (0) FOR failed_count;
GO

/*
 * 通知標題與 notifications.title 長度統一
 */
ALTER TABLE dbo.notification_logs
ALTER COLUMN title NVARCHAR(150) NOT NULL;
GO

/*
 * 發送紀錄對通知範本的 FK
 */
ALTER TABLE dbo.notification_logs
    ADD CONSTRAINT FK_notification_logs_notifications
        FOREIGN KEY (notification_id)
            REFERENCES dbo.notifications(notification_id);
GO

/*
 * Dashboard 之後會依狀態與時間查詢
 */
CREATE NONCLUSTERED INDEX IX_notification_logs_status_created_at
ON dbo.notification_logs (status, created_at DESC);
GO