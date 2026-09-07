/*
 * 事件關注與提醒設定資料表初始化。
 * 會員關注行事曆事件及自訂提醒時間。
 */

/* ==============================
 * event_follows
 * 事件關注資料表
 * ============================== */

CREATE TABLE dbo.event_follows
(
    -- 事件關注唯一編號
    follow_id BIGINT IDENTITY(1,1) NOT NULL,

    -- 關注事件的會員 ID
    member_id BIGINT NOT NULL,

    -- 被關注的行事曆事件 ID
    event_id BIGINT NOT NULL,

    -- 事件開始前幾分鐘提醒（例如 15, 60, 1440）
    remind_before_minutes INT NOT NULL,

    -- 此提醒是否已經產生通知 (0=未提醒, 1=已提醒)
    reminded BIT NOT NULL
        CONSTRAINT DF_event_follows_reminded DEFAULT (0),

    -- 加入關注時間
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_event_follows_created_at DEFAULT (SYSDATETIME()),

    -- 提醒設定最後更新時間
    updated_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_event_follows_updated_at DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_event_follows
        PRIMARY KEY (follow_id),

    CONSTRAINT UQ_event_follows_member_event
        UNIQUE (member_id, event_id),

    CONSTRAINT FK_event_follows_calendar_events
        FOREIGN KEY (event_id)
            REFERENCES dbo.calendar_events(event_id)
            ON DELETE CASCADE
);
GO