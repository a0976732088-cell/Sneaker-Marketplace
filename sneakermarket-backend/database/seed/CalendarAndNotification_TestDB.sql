/* ==============================================================
 * 測試假資料插入指令碼 (SQL Server Management Studio 適用)
 * 注意：包含外鍵關聯，請依序執行檔案或逐一執行。
 * ============================================================== */

USE [SneakerMarketDB];
GO

-- --------------------------------------------------------------
-- 1. 插入 calendar_events (行事曆事件) 3 筆資料
-- --------------------------------------------------------------
INSERT INTO dbo.calendar_events
(
    brand_id, created_by, event_type, title, description,
    start_at, end_at, target_url, source_type, source_id, status, photo
)
VALUES
(
    1, 101, 'AUCTION_START', N'Air Jordan 1 High OG 經典限量競標開拍',
    N'極少見全新原廠盒裝，機會難得錯過不再！', '2026-09-10 10:00:00', '2026-09-15 22:00:00',
    N'https://example.com/auctions/1001', 'AUCTION', 1001, 'PUBLISHED', NULL
),
(
    2, 102, 'PRODUCT_RELEASE', N'Yeezy Boost 350 V2 新色全台首發',
    N'今年度最具話題性的配色，限量登記抽籤中。', '2026-09-20 00:00:00', '2026-09-22 23:59:59',
    N'https://example.com/releases/yeezy-350', 'MANUAL', NULL, 'PUBLISHED', NULL
),
(
    3, 101, 'PROMOTION', N'PUMA 秋季潮流特賣會 滿千折百',
    N'全館精選指定鞋款與服飾，滿 $3000 現折 $300。', '2026-10-01 00:00:00', '2026-10-10 23:59:59',
    N'https://example.com/promotions/autumn-2026', 'ADVERTISEMENT', 501, 'DRAFT', NULL
);
GO


-- --------------------------------------------------------------
-- 2. 插入 event_follows (事件關注) 3 筆資料
-- (使用 SCOPE_IDENTITY() 取用剛產生的 event_id 確保關聯性)
-- --------------------------------------------------------------
DECLARE @event_id_1 BIGINT, @event_id_2 BIGINT;

SELECT TOP 1 @event_id_1 = event_id FROM dbo.calendar_events WHERE title LIKE N'%Air Jordan%';
SELECT TOP 1 @event_id_2 = event_id FROM dbo.calendar_events WHERE title LIKE N'%Yeezy%';

INSERT INTO dbo.event_follows
(
    member_id, event_id, remind_before_minutes, reminded
)
VALUES
(
    2001, @event_id_1, 15, 0  -- 會員 2001 關注 AJ1，開始前 15 分鐘提醒
),
(
    2002, @event_id_1, 60, 0  -- 會員 2002 關注 AJ1，開始前 1 小時提醒
),
(
    2001, @event_id_2, 1440, 0 -- 會員 2001 關注 Yeezy，開始前 1 天提醒
);
GO


-- --------------------------------------------------------------
-- 3. 插入 notification_preferences 與 notification_preference_brands
-- (會員通知偏好與品牌訂閱) 3 筆資料
-- --------------------------------------------------------------
-- 新增 3 筆會員偏好設定
INSERT INTO dbo.notification_preferences
(
    member_id, all_enabled, auction_enabled, promotion_enabled, release_enabled
)
VALUES
(2001, 1, 1, 1, 1),
(2002, 1, 1, 0, 1),
(2003, 0, 0, 0, 0);
GO

-- 針對上述設定檔新增 3 筆品牌偏好細項
DECLARE @pref_id_1 BIGINT, @pref_id_2 BIGINT;

SELECT TOP 1 @pref_id_1 = preference_id FROM dbo.notification_preferences WHERE member_id = 2001;
SELECT TOP 1 @pref_id_2 = preference_id FROM dbo.notification_preferences WHERE member_id = 2002;

INSERT INTO dbo.notification_preference_brands
(
    preference_id, brand_id, enabled
)
VALUES
(@pref_id_1, 1, 1), -- 會員 2001 訂閱品牌 1 (Nike)
(@pref_id_1, 2, 1), -- 會員 2001 訂閱品牌 2 (adidas)
(@pref_id_2, 1, 0); -- 會員 2002 關閉品牌 1 (Nike)
GO


-- --------------------------------------------------------------
-- 4. 插入 notifications (站內通知) 3 筆資料
-- --------------------------------------------------------------
INSERT INTO dbo.notifications
(
    member_id, notification_type, title, content,
    target_type, target_id, target_url, is_read, read_at
)
VALUES
(
    2001, 'EVENT_REMINDER', N'活動開拍提醒', N'您關注的「Air Jordan 1 High OG」競標即將於 15 分鐘後開拍！',
    'EVENT', 1, N'https://example.com/auctions/1001', 0, NULL
),
(
    2001, 'OUTBID', N'被出價超越通知', N'遺憾！您在「Nike Dunk Low」的出價已被其他買家超越，請儘速重新出價。',
    'AUCTION', 1002, N'https://example.com/auctions/1002', 1, '2026-09-06 18:30:00'
),
(
    2002, 'ORDER_CREATED', N'訂單建立成功', N'恭喜得標！請於 24 小時內完成付款以利賣家安排寄送。',
    'ORDER', 8001, N'https://example.com/orders/8001', 0, NULL
);
GO


-- --------------------------------------------------------------
-- 5. 插入 notification_logs (推播日誌) 3 筆資料
-- --------------------------------------------------------------
DECLARE @evt_id_1 BIGINT, @evt_id_2 BIGINT;

SELECT TOP 1 @evt_id_1 = event_id FROM dbo.calendar_events WHERE title LIKE N'%Air Jordan%';
SELECT TOP 1 @evt_id_2 = event_id FROM dbo.calendar_events WHERE title LIKE N'%Yeezy%';

INSERT INTO dbo.notification_logs
(
    event_id, send_type, title, target_type, total_sent
)
VALUES
(
    @evt_id_1, 'APP_PUSH', N'Air Jordan 1 開拍開播提醒', 'SPECIFIC_GROUP', 150
),
(
    @evt_id_2, 'EMAIL', N'Yeezy 350 V2 新品首發登記通知', 'ALL_MEMBERS', 3200
),
(
    @evt_id_1, 'SYSTEM', N'競標結束預告通知', 'SPECIFIC_GROUP', 45
);
GO

/* ==============================================================
 * notification_preference_brands
 * 品牌通知偏好設定表 3 筆假資料
 * ============================================================== */

-- 取得相對應的 preference_id 來建立關聯
DECLARE @pref_id_1 BIGINT, @pref_id_2 BIGINT, @pref_id_3 BIGINT;

SELECT TOP 1 @pref_id_1 = preference_id FROM dbo.notification_preferences WHERE member_id = 2001;
SELECT TOP 1 @pref_id_2 = preference_id FROM dbo.notification_preferences WHERE member_id = 2002;
SELECT TOP 1 @pref_id_3 = preference_id FROM dbo.notification_preferences WHERE member_id = 2003;

INSERT INTO dbo.notification_preference_brands
(
    preference_id, brand_id, enabled
)
VALUES
    (
        @pref_id_1, 1, 1  -- 會員 2001 訂閱品牌 1 (例如 Nike)
    ),
    (
        @pref_id_1, 2, 1  -- 會員 2001 訂閱品牌 2 (例如 adidas)
    ),
    (
        @pref_id_2, 1, 0  -- 會員 2002 關閉品牌 1 (例如 Nike) 的通知
    );
GO