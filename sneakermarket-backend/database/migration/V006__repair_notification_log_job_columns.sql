/* =========================================================
   修復既有 notification_logs 的通知任務欄位

   可安全重複執行。先以 NULL 欄位加入並回填舊資料，避免 MSSQL
   對已有資料的資料表直接新增 NOT NULL 欄位時失敗。
   ========================================================= */

IF OBJECT_ID(N'dbo.notification_logs', N'U') IS NULL
    THROW 50001, 'dbo.notification_logs does not exist.', 1;

IF COL_LENGTH(N'dbo.notification_logs', N'status') IS NULL
    ALTER TABLE dbo.notification_logs ADD status VARCHAR(20) NULL;

IF COL_LENGTH(N'dbo.notification_logs', N'total_recipients') IS NULL
    ALTER TABLE dbo.notification_logs ADD total_recipients INT NULL;

IF COL_LENGTH(N'dbo.notification_logs', N'failed_count') IS NULL
    ALTER TABLE dbo.notification_logs ADD failed_count INT NULL;

UPDATE dbo.notification_logs
SET status = COALESCE(status, 'COMPLETED'),
    total_recipients = COALESCE(total_recipients, total_sent, 0),
    failed_count = COALESCE(failed_count, 0),
    started_at = COALESCE(started_at, created_at),
    completed_at = COALESCE(completed_at, created_at)
WHERE status IS NULL
   OR total_recipients IS NULL
   OR failed_count IS NULL;

ALTER TABLE dbo.notification_logs ALTER COLUMN status VARCHAR(20) NOT NULL;
ALTER TABLE dbo.notification_logs ALTER COLUMN total_recipients INT NOT NULL;
ALTER TABLE dbo.notification_logs ALTER COLUMN failed_count INT NOT NULL;

IF NOT EXISTS (
    SELECT 1
    FROM sys.default_constraints AS defaultConstraint
    INNER JOIN sys.columns AS tableColumn
        ON tableColumn.default_object_id = defaultConstraint.object_id
    WHERE defaultConstraint.parent_object_id = OBJECT_ID(N'dbo.notification_logs')
      AND tableColumn.name = N'status'
)
    ALTER TABLE dbo.notification_logs
        ADD CONSTRAINT DF_notification_logs_status
        DEFAULT ('PENDING') FOR status;

IF NOT EXISTS (
    SELECT 1
    FROM sys.default_constraints AS defaultConstraint
    INNER JOIN sys.columns AS tableColumn
        ON tableColumn.default_object_id = defaultConstraint.object_id
    WHERE defaultConstraint.parent_object_id = OBJECT_ID(N'dbo.notification_logs')
      AND tableColumn.name = N'total_recipients'
)
    ALTER TABLE dbo.notification_logs
        ADD CONSTRAINT DF_notification_logs_total_recipients
        DEFAULT (0) FOR total_recipients;

IF NOT EXISTS (
    SELECT 1
    FROM sys.default_constraints AS defaultConstraint
    INNER JOIN sys.columns AS tableColumn
        ON tableColumn.default_object_id = defaultConstraint.object_id
    WHERE defaultConstraint.parent_object_id = OBJECT_ID(N'dbo.notification_logs')
      AND tableColumn.name = N'failed_count'
)
    ALTER TABLE dbo.notification_logs
        ADD CONSTRAINT DF_notification_logs_failed_count
        DEFAULT (0) FOR failed_count;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.notification_logs')
      AND name = N'IX_notification_logs_status_created_at'
)
    CREATE NONCLUSTERED INDEX IX_notification_logs_status_created_at
        ON dbo.notification_logs (status, created_at DESC);
