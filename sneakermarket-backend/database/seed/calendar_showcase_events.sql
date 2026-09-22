/*
 * 活動探索頁展示用假資料，全部活動名稱、時間及地點均為虛構。
 * 先執行 V007__calendar_event_photo_url.sql，再於 SneakerMarketDB 執行本檔。
 * 照片採用 Unsplash 現成圖片，僅作情境示意，不代表品牌或活動官方素材。
 * 圖片原頁：
 * https://unsplash.com/photos/a-pair-of-new-balance-shoes-on-the-ground-uzrgLnpKgeg
 * https://unsplash.com/photos/white-and-black-adidas-low-top-sneakers-QR40t1LtlC8
 * https://unsplash.com/photos/a-pair-of-white-and-black-sneakers-on-a-table-yS6HWxmL-FY
 * https://unsplash.com/photos/a-pair-of-grey-nike-air-force-sneakers-T31E6zBL_n0
 * https://unsplash.com/photos/a-large-group-of-runners-participating-in-a-marathon-race-fW-drUfhcwc
 * 再次執行不會重複新增相同標題與開始時間的資料。
 */
USE [SneakerMarketDB];
GO

-- 若畫面原有的三筆示範活動已存在，也補上相符的情境照片。
UPDATE dbo.calendar_events
SET photo_url = CASE
    WHEN title = N'Nike Dunk Low "Panda" 經典補貨發售'
        THEN N'https://images.unsplash.com/photo-1641745900309-75ceed0153e1?auto=format&fit=crop&w=800&q=80'
    WHEN title = N'Travis Scott x Air Jordan 1 Low 聯名發售'
        THEN N'https://images.unsplash.com/photo-1646640735429-6584464899f7?auto=format&fit=crop&w=800&q=80'
    WHEN title = N'New Balance 990v6 美製灰調上市'
        THEN N'https://images.unsplash.com/photo-1632765259214-9afeef8669e2?auto=format&fit=crop&w=800&q=80'
END
WHERE title IN (
    N'Nike Dunk Low "Panda" 經典補貨發售',
    N'Travis Scott x Air Jordan 1 Low 聯名發售',
    N'New Balance 990v6 美製灰調上市'
) AND photo_url IS NULL;
GO

INSERT INTO dbo.calendar_events
    (brand_id, created_by, event_type, title, description, start_at, end_at,
     target_url, photo_url, source_type, source_id, status, created_at, updated_at)
SELECT sample.brand_id, NULL, sample.event_type, sample.title, sample.description,
       sample.start_at, sample.end_at, NULL, sample.photo_url,
       'MANUAL', NULL, 'PUBLISHED', SYSDATETIME(), SYSDATETIME()
FROM (VALUES
    (NULL, 'EVENT', N'台北球鞋文化週末展',
     N'收藏家交流、經典鞋款展示與球鞋文化分享，週末一起逛逛。',
     CAST('2026-09-19T11:00:00' AS DATETIME2(0)), CAST('2026-09-19T18:00:00' AS DATETIME2(0)),
     N'https://images.unsplash.com/photo-1632765259214-9afeef8669e2?auto=format&fit=crop&w=800&q=80'),
    (NULL, 'EVENT', N'城市跑友晨跑集結',
     N'從輕鬆配速出發，和跑友一起用雙腳認識城市。',
     CAST('2026-09-20T06:30:00' AS DATETIME2(0)), CAST('2026-09-20T09:00:00' AS DATETIME2(0)),
     N'https://images.unsplash.com/photo-1613937574892-25f441264a09?auto=format&fit=crop&w=800&q=80'),
    (NULL, 'RELEASE', N'Nike Air Max 秋季新色體驗日',
     N'試穿不同配色與穿搭風格，找到適合自己的日常鞋款。',
     CAST('2026-09-22T12:00:00' AS DATETIME2(0)), CAST('2026-09-22T19:00:00' AS DATETIME2(0)),
     N'https://images.unsplash.com/photo-1646640735429-6584464899f7?auto=format&fit=crop&w=800&q=80'),
    (NULL, 'RELEASE', N'adidas Originals 經典鞋款預覽會',
     N'從復古輪廓到街頭搭配，一起看看經典鞋款的新靈感。',
     CAST('2026-09-24T13:00:00' AS DATETIME2(0)), CAST('2026-09-24T20:00:00' AS DATETIME2(0)),
     N'https://images.unsplash.com/photo-1587568331862-6a7c41833ecc?auto=format&fit=crop&w=800&q=80'),
    (NULL, 'EVENT', N'球鞋清潔保養小教室',
     N'練習日常清潔、材質辨識與收納技巧，讓愛鞋陪你更久。',
     CAST('2026-09-26T14:00:00' AS DATETIME2(0)), CAST('2026-09-26T16:00:00' AS DATETIME2(0)),
     N'https://images.unsplash.com/photo-1641745900309-75ceed0153e1?auto=format&fit=crop&w=800&q=80'),
    (NULL, 'EVENT', N'河濱半程馬拉松同樂跑',
     N'沿著河岸挑戰 21 公里，也歡迎初跑者參加短距離組。',
     CAST('2026-09-27T05:30:00' AS DATETIME2(0)), CAST('2026-09-27T11:00:00' AS DATETIME2(0)),
     N'https://images.unsplash.com/photo-1777788613992-5f2655a97e72?auto=format&fit=crop&w=800&q=80'),
    (NULL, 'RELEASE', N'New Balance 灰系跑鞋試穿會',
     N'體驗灰系經典風格與舒適腳感，分享你的日常跑鞋搭配。',
     CAST('2026-09-29T11:00:00' AS DATETIME2(0)), CAST('2026-09-29T18:00:00' AS DATETIME2(0)),
     N'https://images.unsplash.com/photo-1632765259214-9afeef8669e2?auto=format&fit=crop&w=800&q=80')
) AS sample (brand_id, event_type, title, description, start_at, end_at, photo_url)
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.calendar_events AS existing
    WHERE existing.title = sample.title AND existing.start_at = sample.start_at
);
GO
