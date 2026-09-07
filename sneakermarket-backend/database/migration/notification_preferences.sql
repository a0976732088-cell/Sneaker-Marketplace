/*
 * 會員通知偏好設定資料表初始化。
 * 設定全站通知開關、特定活動類別開關與品牌偏好。
 */

/* ==============================
 * notification_preferences
 * 會員通知偏好設定表
 * ============================== */

CREATE TABLE dbo.notification_preferences
(
    -- 通知偏好設定檔唯一編號
    preference_id BIGINT IDENTITY(1,1) NOT NULL,

    -- 會員 ID
    member_id BIGINT NOT NULL,

    -- 全站通知總開關 (1=開啟, 0=關閉)
    all_enabled BIT NOT NULL
        CONSTRAINT DF_notification_preferences_all_enabled DEFAULT (1),

    -- 競標活動通知開關 (1=開啟, 0=關閉)
    auction_enabled BIT NOT NULL
        CONSTRAINT DF_notification_preferences_auction_enabled DEFAULT (1),

    -- 特賣促銷通知開關 (1=開啟, 0=關閉)
    promotion_enabled BIT NOT NULL
        CONSTRAINT DF_notification_preferences_promotion_enabled DEFAULT (1),

    -- 新品發售通知開關 (1=開啟, 0=關閉)
    release_enabled BIT NOT NULL
        CONSTRAINT DF_notification_preferences_release_enabled DEFAULT (1),

    -- 建立時間
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_notification_preferences_created_at DEFAULT (SYSDATETIME()),

    -- 最後更新時間
    updated_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_notification_preferences_updated_at DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_notification_preferences
        PRIMARY KEY (preference_id),

    CONSTRAINT UQ_notification_preferences_member
        UNIQUE (member_id)
);
GO