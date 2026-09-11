/*
 * 唯讀啟用前檢查。輸出含收件信箱，只供本機人工核對，不放入公開 API 或 Git 日誌。
 * Email 預設關閉；確認歷史資料均可通知後才設定 AUCTION_EMAIL_ENABLED=true。
 */
WITH ready AS (
    SELECT r.auction_id, r.result_status, r.winner_id, p.seller_id
    FROM dbo.auction_results r
    JOIN dbo.auctions a ON a.auction_id = r.auction_id
    JOIN dbo.auction_products p ON p.auction_product_id = a.auction_product_id
    WHERE (r.result_status = 'UNSOLD' OR
        (r.result_status = 'SOLD' AND EXISTS
            (SELECT 1 FROM dbo.auction_orders o WHERE o.auction_id = r.auction_id)))
      AND NOT EXISTS (SELECT 1 FROM dbo.auction_email_logs e WHERE e.auction_id = r.auction_id)
), recipients AS (
    SELECT auction_id, seller_id AS member_id,
        CASE WHEN result_status = 'SOLD' THEN 'SOLD_SELLER' ELSE 'UNSOLD_SELLER' END AS email_type FROM ready
    UNION ALL
    SELECT auction_id, winner_id, 'WON' FROM ready WHERE result_status = 'SOLD'
    UNION ALL
    SELECT DISTINCT r.auction_id, b.bidder_id, 'LOST'
    FROM ready r JOIN dbo.auction_bids b ON b.auction_id = r.auction_id
    WHERE r.result_status = 'SOLD' AND b.bidder_id <> r.winner_id
)
SELECT r.*, m.email AS recipient_email, COUNT(*) OVER () AS new_task_count
FROM recipients r LEFT JOIN dbo.members m ON m.member_id = r.member_id
ORDER BY r.auction_id, r.email_type, r.member_id;

SELECT email_log_id, auction_id, recipient_email, email_type, send_status, retry_count,
       COUNT(*) OVER () AS retry_candidate_count
FROM dbo.auction_email_logs
WHERE send_status IN ('PENDING', 'FAILED') AND retry_count < 3
ORDER BY created_at, email_log_id;
