/*
 * 競標商城列表測試資料。
 *
 * 用途：
 * 搜尋、篩選、排序與分頁功能開發測試。
 *
 * 注意：
 * seller_id、brand_id、category_id 目前尚未與其他模組建立正式 FK，
 * 此處先使用測試用 ID。
 */

USE SneakerMarketDB;
GO

SET XACT_ABORT ON;
BEGIN TRANSACTION;

DECLARE @productId BIGINT;


/* ==============================
 * 1. Nike Dunk Low Panda
 * ACTIVE / NEW / US 8
 * ============================== */

SET @productId = (
    SELECT TOP 1 auction_product_id
    FROM dbo.auction_products
    WHERE model = N'DD1503-101'
);

IF @productId IS NULL
BEGIN
INSERT INTO dbo.auction_products
(
    seller_id,
    brand_id,
    category_id,
    product_name,
    model,
    size,
    condition,
    description,
    defect_note,
    authenticity_note,
    location,
    shipping_method,
    shipping_fee,
    payment_method,
    allow_meetup,
    allow_return,
    warranty_note
)
VALUES
    (
        1,
        1,
        1,
        N'Nike Dunk Low Panda',
        N'DD1503-101',
        N'US 8',
        N'NEW',
        N'全新 Nike Dunk Low Panda，競標列表功能測試資料。',
        NULL,
        N'附原鞋盒。',
        N'台北',
        N'宅配',
        60.00,
        N'轉帳',
        1,
        0,
        NULL
    );

SET @productId = SCOPE_IDENTITY();
END;

IF NOT EXISTS (
    SELECT 1
    FROM dbo.auctions
    WHERE auction_product_id = @productId
)
BEGIN
INSERT INTO dbo.auctions
(
    auction_product_id,
    start_price,
    current_price,
    min_increment,
    start_at,
    end_at,
    status,
    highest_bidder_id,
    bid_count
)
VALUES
    (
        @productId,
        2800.00,
        3800.00,
        300.00,
        DATEADD(DAY, -1, SYSDATETIME()),
        DATEADD(DAY, 2, SYSDATETIME()),
        N'ACTIVE',
        2,
        3
    );
END;


/* ==============================
 * 2. adidas Samba OG
 * ACTIVE / LIKE_NEW / US 9
 * ============================== */

SET @productId = (
    SELECT TOP 1 auction_product_id
    FROM dbo.auction_products
    WHERE model = N'B75806'
);

IF @productId IS NULL
BEGIN
INSERT INTO dbo.auction_products
(
    seller_id,
    brand_id,
    category_id,
    product_name,
    model,
    size,
    condition,
    description,
    defect_note,
    authenticity_note,
    location,
    shipping_method,
    shipping_fee,
    payment_method,
    allow_meetup,
    allow_return,
    warranty_note
)
VALUES
    (
        2,
        2,
        1,
        N'adidas Samba OG White Black',
        N'B75806',
        N'US 9',
        N'LIKE_NEW',
        N'adidas Samba OG，九成新，競標測試資料。',
        N'鞋底有輕微使用痕跡。',
        N'附購買證明。',
        N'新北',
        N'超商取貨',
        60.00,
        N'轉帳',
        0,
        0,
        NULL
    );

SET @productId = SCOPE_IDENTITY();
END;

IF NOT EXISTS (
    SELECT 1
    FROM dbo.auctions
    WHERE auction_product_id = @productId
)
BEGIN
INSERT INTO dbo.auctions
(
    auction_product_id,
    start_price,
    current_price,
    min_increment,
    start_at,
    end_at,
    status,
    highest_bidder_id,
    bid_count
)
VALUES
    (
        @productId,
        3200.00,
        4100.00,
        300.00,
        DATEADD(DAY, -2, SYSDATETIME()),
        DATEADD(DAY, 4, SYSDATETIME()),
        N'ACTIVE',
        3,
        4
    );
END;


/* ==============================
 * 3. New Balance 990v6
 * ACTIVE / USED / US 10
 * ============================== */

SET @productId = (
    SELECT TOP 1 auction_product_id
    FROM dbo.auction_products
    WHERE model = N'M990GL6'
);

IF @productId IS NULL
BEGIN
INSERT INTO dbo.auction_products
(
    seller_id,
    brand_id,
    category_id,
    product_name,
    model,
    size,
    condition,
    description,
    defect_note,
    authenticity_note,
    location,
    shipping_method,
    shipping_fee,
    payment_method,
    allow_meetup,
    allow_return,
    warranty_note
)
VALUES
    (
        3,
        3,
        2,
        N'New Balance 990v6 Grey',
        N'M990GL6',
        N'US 10',
        N'USED',
        N'New Balance 990v6 灰色，正常使用品。',
        N'鞋面有正常使用痕跡。',
        N'公司貨。',
        N'台中',
        N'宅配',
        80.00,
        N'轉帳',
        1,
        0,
        NULL
    );

SET @productId = SCOPE_IDENTITY();
END;

IF NOT EXISTS (
    SELECT 1
    FROM dbo.auctions
    WHERE auction_product_id = @productId
)
BEGIN
INSERT INTO dbo.auctions
(
    auction_product_id,
    start_price,
    current_price,
    min_increment,
    start_at,
    end_at,
    status,
    highest_bidder_id,
    bid_count
)
VALUES
    (
        @productId,
        4000.00,
        5200.00,
        400.00,
        DATEADD(DAY, -1, SYSDATETIME()),
        DATEADD(DAY, 1, SYSDATETIME()),
        N'ACTIVE',
        2,
        3
    );
END;


/* ==============================
 * 4. Nike Air Force 1
 * SCHEDULED / NEW / US 9.5
 * ============================== */

SET @productId = (
    SELECT TOP 1 auction_product_id
    FROM dbo.auction_products
    WHERE model = N'CW2288-111'
);

IF @productId IS NULL
BEGIN
INSERT INTO dbo.auction_products
(
    seller_id,
    brand_id,
    category_id,
    product_name,
    model,
    size,
    condition,
    description,
    defect_note,
    authenticity_note,
    location,
    shipping_method,
    shipping_fee,
    payment_method,
    allow_meetup,
    allow_return,
    warranty_note
)
VALUES
    (
        1,
        1,
        1,
        N'Nike Air Force 1 07 White',
        N'CW2288-111',
        N'US 9.5',
        N'NEW',
        N'全新白色 Air Force 1。',
        NULL,
        N'公司貨。',
        N'台北',
        N'宅配',
        60.00,
        N'轉帳',
        1,
        0,
        NULL
    );

SET @productId = SCOPE_IDENTITY();
END;

IF NOT EXISTS (
    SELECT 1
    FROM dbo.auctions
    WHERE auction_product_id = @productId
)
BEGIN
INSERT INTO dbo.auctions
(
    auction_product_id,
    start_price,
    current_price,
    min_increment,
    start_at,
    end_at,
    status,
    highest_bidder_id,
    bid_count
)
VALUES
    (
        @productId,
        2500.00,
        2500.00,
        200.00,
        DATEADD(DAY, 1, SYSDATETIME()),
        DATEADD(DAY, 5, SYSDATETIME()),
        N'SCHEDULED',
        NULL,
        0
    );
END;


/* ==============================
 * 5. Yeezy Boost 350 V2 Zebra
 * ACTIVE / LIKE_NEW / US 10.5
 * ============================== */

SET @productId = (
    SELECT TOP 1 auction_product_id
    FROM dbo.auction_products
    WHERE model = N'CP9654'
);

IF @productId IS NULL
BEGIN
INSERT INTO dbo.auction_products
(
    seller_id,
    brand_id,
    category_id,
    product_name,
    model,
    size,
    condition,
    description,
    defect_note,
    authenticity_note,
    location,
    shipping_method,
    shipping_fee,
    payment_method,
    allow_meetup,
    allow_return,
    warranty_note
)
VALUES
    (
        2,
        2,
        1,
        N'adidas Yeezy Boost 350 V2 Zebra',
        N'CP9654',
        N'US 10.5',
        N'LIKE_NEW',
        N'Yeezy Zebra，保存良好。',
        N'鞋底有少量使用痕跡。',
        N'附原盒與購買資料。',
        N'桃園',
        N'宅配',
        80.00,
        N'轉帳',
        0,
        0,
        NULL
    );

SET @productId = SCOPE_IDENTITY();
END;

IF NOT EXISTS (
    SELECT 1
    FROM dbo.auctions
    WHERE auction_product_id = @productId
)
BEGIN
INSERT INTO dbo.auctions
(
    auction_product_id,
    start_price,
    current_price,
    min_increment,
    start_at,
    end_at,
    status,
    highest_bidder_id,
    bid_count
)
VALUES
    (
        @productId,
        5000.00,
        6500.00,
        500.00,
        DATEADD(DAY, -3, SYSDATETIME()),
        DATEADD(DAY, 3, SYSDATETIME()),
        N'ACTIVE',
        3,
        4
    );
END;


/* ==============================
 * 6. ASICS GEL-KAYANO 14
 * ACTIVE / LIKE_NEW / US 9.5
 * ============================== */

SET @productId = (
    SELECT TOP 1 auction_product_id
    FROM dbo.auction_products
    WHERE model = N'1201A019-100'
);

IF @productId IS NULL
BEGIN
INSERT INTO dbo.auction_products
(
    seller_id,
    brand_id,
    category_id,
    product_name,
    model,
    size,
    condition,
    description,
    defect_note,
    authenticity_note,
    location,
    shipping_method,
    shipping_fee,
    payment_method,
    allow_meetup,
    allow_return,
    warranty_note
)
VALUES
    (
        3,
        4,
        2,
        N'ASICS GEL-KAYANO 14 Silver',
        N'1201A019-100',
        N'US 9.5',
        N'LIKE_NEW',
        N'ASICS GEL-KAYANO 14 銀色款。',
        N'輕微穿著痕跡。',
        N'購自官方通路。',
        N'高雄',
        N'超商取貨',
        60.00,
        N'轉帳',
        0,
        0,
        NULL
    );

SET @productId = SCOPE_IDENTITY();
END;

IF NOT EXISTS (
    SELECT 1
    FROM dbo.auctions
    WHERE auction_product_id = @productId
)
BEGIN
INSERT INTO dbo.auctions
(
    auction_product_id,
    start_price,
    current_price,
    min_increment,
    start_at,
    end_at,
    status,
    highest_bidder_id,
    bid_count
)
VALUES
    (
        @productId,
        3800.00,
        4600.00,
        200.00,
        DATEADD(DAY, -1, SYSDATETIME()),
        DATEADD(DAY, 6, SYSDATETIME()),
        N'ACTIVE',
        1,
        4
    );
END;


/* ==============================
 * 7. Converse Chuck 70 Hi
 * UNSOLD / NEW / US 9
 * ============================== */

SET @productId = (
    SELECT TOP 1 auction_product_id
    FROM dbo.auction_products
    WHERE model = N'162050C'
);

IF @productId IS NULL
BEGIN
INSERT INTO dbo.auction_products
(
    seller_id,
    brand_id,
    category_id,
    product_name,
    model,
    size,
    condition,
    description,
    defect_note,
    authenticity_note,
    location,
    shipping_method,
    shipping_fee,
    payment_method,
    allow_meetup,
    allow_return,
    warranty_note
)
VALUES
    (
        1,
        5,
        1,
        N'Converse Chuck 70 Hi Black',
        N'162050C',
        N'US 9',
        N'NEW',
        N'全新 Converse Chuck 70 黑色高筒。',
        NULL,
        N'官方門市購入。',
        N'台北',
        N'超商取貨',
        60.00,
        N'轉帳',
        1,
        0,
        NULL
    );

SET @productId = SCOPE_IDENTITY();
END;

IF NOT EXISTS (
    SELECT 1
    FROM dbo.auctions
    WHERE auction_product_id = @productId
)
BEGIN
INSERT INTO dbo.auctions
(
    auction_product_id,
    start_price,
    current_price,
    min_increment,
    start_at,
    end_at,
    status,
    highest_bidder_id,
    bid_count
)
VALUES
    (
        @productId,
        1900.00,
        1900.00,
        200.00,
        DATEADD(DAY, -7, SYSDATETIME()),
        DATEADD(DAY, -2, SYSDATETIME()),
        N'UNSOLD',
        NULL,
        0
    );
END;


/* ==============================
 * 8. New Balance 2002R
 * SOLD / USED / US 8.5
 * ============================== */

SET @productId = (
    SELECT TOP 1 auction_product_id
    FROM dbo.auction_products
    WHERE model = N'M2002RDA'
);

IF @productId IS NULL
BEGIN
INSERT INTO dbo.auction_products
(
    seller_id,
    brand_id,
    category_id,
    product_name,
    model,
    size,
    condition,
    description,
    defect_note,
    authenticity_note,
    location,
    shipping_method,
    shipping_fee,
    payment_method,
    allow_meetup,
    allow_return,
    warranty_note
)
VALUES
    (
        2,
        3,
        1,
        N'New Balance 2002R Protection Pack Rain Cloud',
        N'M2002RDA',
        N'US 8.5',
        N'USED',
        N'New Balance 2002R Protection Pack 測試商品。',
        N'鞋底與鞋面有一般使用痕跡。',
        N'附鞋盒。',
        N'台中',
        N'宅配',
        80.00,
        N'轉帳',
        0,
        0,
        NULL
    );

SET @productId = SCOPE_IDENTITY();
END;

IF NOT EXISTS (
    SELECT 1
    FROM dbo.auctions
    WHERE auction_product_id = @productId
)
BEGIN
INSERT INTO dbo.auctions
(
    auction_product_id,
    start_price,
    current_price,
    min_increment,
    start_at,
    end_at,
    status,
    highest_bidder_id,
    bid_count
)
VALUES
    (
        @productId,
        4200.00,
        6800.00,
        300.00,
        DATEADD(DAY, -10, SYSDATETIME()),
        DATEADD(DAY, -3, SYSDATETIME()),
        N'SOLD',
        3,
        7
    );
END;


COMMIT;
GO