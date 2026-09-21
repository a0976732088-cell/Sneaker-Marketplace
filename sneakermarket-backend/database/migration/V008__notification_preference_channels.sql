/*
 * 擴充會員通知偏好：
 * 每一類通知可分別選擇站內通知與 Email。
 *
 * 保留既有 auction_enabled、promotion_enabled、release_enabled，
 * 避免破壞既有資料；新的發送流程改用以下六個欄位。
 */

IF COL_LENGTH(
    'dbo.notification_preferences',
    'event_in_app_enabled'
) IS NULL
BEGIN
ALTER TABLE dbo.notification_preferences
    ADD event_in_app_enabled BIT NOT NULL
    CONSTRAINT DF_notification_preferences_event_in_app
    DEFAULT (1);
END;
GO

IF COL_LENGTH(
    'dbo.notification_preferences',
    'event_email_enabled'
) IS NULL
BEGIN
ALTER TABLE dbo.notification_preferences
    ADD event_email_enabled BIT NOT NULL
    CONSTRAINT DF_notification_preferences_event_email
    DEFAULT (1);
END;
GO

IF COL_LENGTH(
    'dbo.notification_preferences',
    'system_in_app_enabled'
) IS NULL
BEGIN
ALTER TABLE dbo.notification_preferences
    ADD system_in_app_enabled BIT NOT NULL
    CONSTRAINT DF_notification_preferences_system_in_app
    DEFAULT (1);
END;
GO

IF COL_LENGTH(
    'dbo.notification_preferences',
    'system_email_enabled'
) IS NULL
BEGIN
ALTER TABLE dbo.notification_preferences
    ADD system_email_enabled BIT NOT NULL
    CONSTRAINT DF_notification_preferences_system_email
    DEFAULT (1);
END;
GO

IF COL_LENGTH(
    'dbo.notification_preferences',
    'release_in_app_enabled'
) IS NULL
BEGIN
ALTER TABLE dbo.notification_preferences
    ADD release_in_app_enabled BIT NOT NULL
    CONSTRAINT DF_notification_preferences_release_in_app
    DEFAULT (1);
END;
GO

IF COL_LENGTH(
    'dbo.notification_preferences',
    'release_email_enabled'
) IS NULL
BEGIN
ALTER TABLE dbo.notification_preferences
    ADD release_email_enabled BIT NOT NULL
    CONSTRAINT DF_notification_preferences_release_email
    DEFAULT (1);
END;
GO

/*
 * 將既有 release_enabled = 0 的設定帶入新欄位。
 */
UPDATE dbo.notification_preferences
SET
    release_in_app_enabled = 0,
    release_email_enabled = 0
WHERE release_enabled = 0;
GO