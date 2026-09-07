/*
 * 站內通知中心資料表初始化。
 * 全平台共用站內通知，保存內容、未讀狀態與跳轉目標。
 */

/* ==============================
 * notifications
 * 站內通知資料表
 * ============================== */

CREATE TABLE dbo.notifications
(
    -- 通知唯一編號
    notification_id BIGINT IDENTITY(1,1) NOT NULL,

    -- 接收通知的會員 ID
    member_id BIGINT NOT NULL,

    -- 通知類型：ORDER_CREATED, OUTBID, AUCTION_WON, EVENT_REMINDER
    notification_type VARCHAR(40) NOT NULL,

    -- 通知標題
    title NVARCHAR(150) NOT NULL,

    -- 通知內文或摘要
    content NVARCHAR(1000) NOT NULL,

    -- 跳轉資料類型：ORDER, AUCTION, EVENT
    target_type VARCHAR(30) NULL,

    -- 跳轉目標資料編號（例如 order_id, auction_id）
    target_id BIGINT NULL,

    -- 點擊通知後前往的頁面 URL
    target_url NVARCHAR(500) NULL,

    -- 通知是否已讀 (0=未讀, 1=已讀)
    is_read BIT NOT NULL
        CONSTRAINT DF_notifications_is_read DEFAULT (0),

    -- 會員讀取通知的時間
    read_at DATETIME2(0) NULL,

    -- 通知產生時間
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_notifications_created_at DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_notifications
        PRIMARY KEY (notification_id)
);
GO

-- 查詢效能優化索引：加速取得特定會員未讀/已讀通知清單
CREATE NONCLUSTERED INDEX IX_notifications_member_read
    ON dbo.notifications (member_id, is_read);
GO