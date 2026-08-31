/*
 * 競標核心資料表初始化。
 * 建立競標商品資料與競標主資料。
 */


/* ==============================
 * auction_products
 * 競標商品資料表
 * ============================== */

CREATE TABLE dbo.auction_products
(
    -- 競標商品唯一編號
    auction_product_id BIGINT IDENTITY(1,1) PRIMARY KEY,

    -- 刊登會員 ID
    seller_id BIGINT NOT NULL,

    -- 品牌 ID
    brand_id BIGINT NOT NULL,

    -- 分類 ID
    category_id BIGINT NOT NULL,

    -- 商品名稱
    product_name NVARCHAR(150) NOT NULL,

    -- 商品型號
    model NVARCHAR(100) NULL,

    -- 球鞋尺寸
    size NVARCHAR(20) NOT NULL,

    -- 商品狀況：NEW、LIKE_NEW、USED
    condition NVARCHAR(20) NOT NULL,

    -- 商品完整介紹
    description NVARCHAR(MAX) NOT NULL,

    -- 瑕疵說明
    defect_note NVARCHAR(1000) NULL,

    -- 真偽或購買來源說明
    authenticity_note NVARCHAR(1000) NULL,

    -- 商品所在地
    location NVARCHAR(100) NULL,

    -- 運送方式
    shipping_method NVARCHAR(50) NOT NULL,

    -- 運費
    shipping_fee DECIMAL(12,2) NOT NULL
        CONSTRAINT DF_auction_products_shipping_fee DEFAULT (0),

    -- 付款方式
    payment_method NVARCHAR(50) NOT NULL,

    -- 是否接受面交
    allow_meetup BIT NOT NULL
        CONSTRAINT DF_auction_products_allow_meetup DEFAULT (0),

    -- 是否接受退貨
    allow_return BIT NOT NULL
        CONSTRAINT DF_auction_products_allow_return DEFAULT (0),

    -- 保固或售後說明
    warranty_note NVARCHAR(500) NULL,

    -- 建立時間
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_auction_products_created_at DEFAULT (SYSDATETIME()),

    -- 最後更新時間
    updated_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_auction_products_updated_at DEFAULT (SYSDATETIME())
);
GO


/* ==============================
 * auctions
 * 競標主資料表
 * ============================== */

CREATE TABLE dbo.auctions
(
    -- 競標唯一編號
    auction_id BIGINT IDENTITY(1,1) NOT NULL,

    -- 對應競標商品
    auction_product_id BIGINT NOT NULL,

    -- 起標價格
    start_price DECIMAL(12,2) NOT NULL,

    -- 目前價格
    current_price DECIMAL(12,2) NOT NULL,

    -- 最低加價金額
    min_increment DECIMAL(12,2) NOT NULL,

    -- 開始時間
    start_at DATETIME2(0) NOT NULL,

    -- 結束時間
    end_at DATETIME2(0) NOT NULL,

    -- 競標狀態
    status NVARCHAR(20) NOT NULL,

    -- 目前最高出價會員
    highest_bidder_id BIGINT NULL,

    -- 成功出價次數
    bid_count INT NOT NULL
        CONSTRAINT DF_auctions_bid_count DEFAULT (0),

    -- JPA 樂觀鎖版本
    version INT NOT NULL
        CONSTRAINT DF_auctions_version DEFAULT (0),

    -- 建立時間
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_auctions_created_at DEFAULT (SYSDATETIME()),

    -- 最後更新時間
    updated_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_auctions_updated_at DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_auctions
        PRIMARY KEY (auction_id),

    CONSTRAINT UQ_auctions_auction_product
        UNIQUE (auction_product_id),

    CONSTRAINT FK_auctions_auction_products
        FOREIGN KEY (auction_product_id)
            REFERENCES dbo.auction_products(auction_product_id),

    CONSTRAINT CK_auctions_start_price
        CHECK (start_price > 0),

    CONSTRAINT CK_auctions_min_increment
        CHECK (min_increment > 0),

    CONSTRAINT CK_auctions_time
        CHECK (end_at > start_at),

    CONSTRAINT CK_auctions_status
        CHECK (
            status IN (
                       N'DRAFT',
                       N'SCHEDULED',
                       N'ACTIVE',
                       N'SOLD',
                       N'UNSOLD',
                       N'CANCELLED',
                       N'REMOVED'
                )
            )
);
GO