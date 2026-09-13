ALTER TABLE notifications
ALTER COLUMN member_id BIGINT NULL;

ALTER TABLE notifications
    ADD is_sent BIT NOT NULL
    CONSTRAINT DF_notifications_is_sent DEFAULT 0;

ALTER TABLE notifications
    ADD sent_at DATETIME2 NULL;

ALTER TABLE notifications
    ADD CONSTRAINT FK_notifications_members
        FOREIGN KEY (member_id)
            REFERENCES members(member_id);

ALTER TABLE event_follows
    ADD CONSTRAINT FK_event_follows_members
        FOREIGN KEY (member_id)
            REFERENCES members(member_id);


-- 組員拿到新的 Commit 後，只要執行這個 migration SQL，就能把自己的資料庫更新到相同版本。
-- 執行 FK 之前，先檢查是否有無效資料，例如：
-- SELECT ef.member_id
-- FROM event_follows ef
--          LEFT JOIN members m ON m.member_id = ef.member_id
-- WHERE m.member_id IS NULL;
-- 如果查出資料，要先清理或修正，否則新增 FK 會失敗。
-- 你目前使用 ddl-auto=update，Hibernate 可能自動新增部分欄位，但不應把它當成團隊正式的 migration。它不一定能正確處理：
--     - 欄位改為可空值。
--     - 外鍵新增或修改。
--     - 舊資料轉換。
--     - 不同組員資料庫版本不一致。
-- 專題目前可以先採用「版本化 SQL 檔＋組員手動執行」。如果之後想再完善，可以導入 Flyway，讓 Spring Boot 啟動時自動執行尚未套用的 migration。