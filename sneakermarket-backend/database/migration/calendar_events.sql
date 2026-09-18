/*
 * 行事曆事件資料表初始化。
 * 保存平台競標、品牌促銷、球鞋發售與優惠券活動等行事曆事件。
 */

/* ==============================
 * calendar_events
 * 行事曆事件資料表
 * ============================== */

CREATE TABLE dbo.calendar_events
(
    -- 行事曆事件唯一編號
    event_id BIGINT IDENTITY(1,1) NOT NULL,

    -- 事件所屬品牌 ID（平台通用活動可為 NULL）
    brand_id BIGINT NULL,

    -- 建立事件的管理員 ID（系統自動產生時可為 NULL）
    created_by BIGINT NULL,

    -- 事件類型：AUCTION_START, AUCTION_END, PROMOTION, PRODUCT_RELEASE, COUPON
    event_type VARCHAR(30) NOT NULL,

    -- 事件標題
    title NVARCHAR(150) NOT NULL,

    -- 事件完整說明與內容
    description NVARCHAR(MAX) NULL,

    -- 事件開始時間
    start_at DATETIME2(0) NOT NULL,

    -- 事件結束時間（單一時間點事件可為 NULL）
    end_at DATETIME2(0) NULL,

    -- 點擊前往的頁面連結 URL
    target_url NVARCHAR(500) NULL,

    -- 可直接使用現成的網路活動圖片
    photo_url NVARCHAR(500) NULL,

    -- 事件來源類型：MANUAL, AUCTION, ADVERTISEMENT, COUPON
    source_type VARCHAR(30) NOT NULL,

    -- 來源資料編號（例如 auction_id）
    source_id BIGINT NULL,

    -- 事件狀態：DRAFT, PUBLISHED, CANCELLED, ENDED
    status VARCHAR(20) NOT NULL,

    -- 事件活動圖片（二進位檔）
    photo VARBINARY(MAX) NULL,

    -- 建立時間
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_calendar_events_created_at DEFAULT (SYSDATETIME()),

    -- 最後更新時間
    updated_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_calendar_events_updated_at DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_calendar_events
        PRIMARY KEY (event_id)
);
GO
