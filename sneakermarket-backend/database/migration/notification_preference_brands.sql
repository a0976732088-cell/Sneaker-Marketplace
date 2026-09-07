/* ==============================
 * notification_preference_brands
 * 品牌通知偏好設定表
 * ============================== */

CREATE TABLE dbo.notification_preference_brands
(
    -- 品牌通知偏好唯一編號
    preference_brand_id BIGINT IDENTITY(1,1) NOT NULL,

    -- 對應的通知偏好設定 ID
    preference_id BIGINT NOT NULL,

    -- 特定品牌 ID
    brand_id BIGINT NOT NULL,

    -- 該品牌的通知訂閱開關 (1=開啟, 0=關閉)
    enabled BIT NOT NULL
        CONSTRAINT DF_notification_preference_brands_enabled DEFAULT (1),

    -- 建立時間
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_notification_preference_brands_created_at DEFAULT (SYSDATETIME()),

    -- 最後更新時間
    updated_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_notification_preference_brands_updated_at DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_notification_preference_brands
        PRIMARY KEY (preference_brand_id),

    CONSTRAINT UQ_notification_preference_brands_pref_brand
        UNIQUE (preference_id, brand_id),

    CONSTRAINT FK_notification_preference_brands_preferences
        FOREIGN KEY (preference_id)
            REFERENCES dbo.notification_preferences(preference_id)
            ON DELETE CASCADE
);
GO