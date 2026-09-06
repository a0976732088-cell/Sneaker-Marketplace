CREATE TABLE dbo.notification_logs
(
    -- 推播日誌唯一編號
    id BIGINT IDENTITY(1,1) NOT NULL,

    -- 被發送的行事曆事件 ID
    event_id BIGINT NOT NULL,

    -- 發送方式：APP_PUSH, EMAIL, SYSTEM
    send_type VARCHAR(20) NOT NULL,

    -- 推播標題
    title NVARCHAR(100) NOT NULL,

    -- 發送目標對象類型：ALL_MEMBERS, SPECIFIC_GROUP
    target_type VARCHAR(30) NOT NULL,

    -- 成功發送總筆數
    total_sent INT NOT NULL
        CONSTRAINT DF_notification_logs_total_sent DEFAULT (0),

    -- 推播建立/發送時間
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_notification_logs_created_at DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_notification_logs
        PRIMARY KEY (id),

    CONSTRAINT FK_notification_logs_calendar_events
        FOREIGN KEY (event_id)
            REFERENCES dbo.calendar_events(event_id)
);
GO