/*
 * 建立競標出價紀錄資料表。
 *
 * 每一筆資料代表一次成功的出價紀錄。
 * 被拒絕或不符合最低出價規則的出價，
 * 未來不應寫入此資料表。
 */


/* ==============================
 * auction_bids
 * 競標出價紀錄資料表
 * ============================== */

CREATE TABLE dbo.auction_bids
(
    -- 出價紀錄唯一編號
    bid_id BIGINT IDENTITY(1,1) NOT NULL,

    -- 所屬競標 ID
    auction_id BIGINT NOT NULL,

    -- 出價會員 ID
    bidder_id BIGINT NOT NULL,

    -- 成功出價金額
    bid_amount DECIMAL(12,2) NOT NULL,

    -- 出價成功時間
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_auction_bids_created_at DEFAULT (SYSDATETIME()),

    -- Primary Key
    CONSTRAINT PK_auction_bids
        PRIMARY KEY (bid_id),

    -- 每筆出價必須對應一場存在的競標
    CONSTRAINT FK_auction_bids_auctions
        FOREIGN KEY (auction_id)
            REFERENCES dbo.auctions(auction_id)
);
GO


/*
 * 詳細頁會依 auction_id 查詢某場競標的出價紀錄，
 * 並依時間排列，因此建立複合索引。
 */
CREATE INDEX IX_auction_bids_auction_created_at
    ON dbo.auction_bids (auction_id, created_at);
GO


/*
 * 後續「我的出價」功能會依 bidder_id 查詢會員出價紀錄，
 * 先依核心資料表規格建立索引。
 */
CREATE INDEX IX_auction_bids_bidder_created_at
    ON dbo.auction_bids (bidder_id, created_at);
GO