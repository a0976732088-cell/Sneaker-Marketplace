/*
 * 通知內容可能只用於 Email，不一定要顯示在站內通知中心。
 */
IF COL_LENGTH(
    'dbo.notifications',
    'in_app_visible'
) IS NULL
BEGIN
ALTER TABLE dbo.notifications
    ADD in_app_visible BIT NOT NULL
    CONSTRAINT DF_notifications_in_app_visible
    DEFAULT (1);
END;
GO

/*
 * 加速會員通知抽屜查詢。
 */
IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_notifications_member_visible_sent_created'
      AND object_id = OBJECT_ID('dbo.notifications')
)
BEGIN
    CREATE NONCLUSTERED INDEX
        IX_notifications_member_visible_sent_created
    ON dbo.notifications (
        member_id,
        in_app_visible,
        is_sent,
        created_at DESC
    );
END;
GO