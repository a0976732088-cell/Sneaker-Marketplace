/*
 * 建立競標商品圖片資料表。
 *
 * 一筆 auction_products 可以對應多張圖片，
 * 圖片本身只保存 URL / 路徑，不將實際圖片檔存入資料庫。
 */


/* ==============================
 * auction_product_images
 * 競標商品圖片資料表
 * ============================== */

CREATE TABLE dbo.auction_product_images
(
    -- 圖片唯一編號
    image_id BIGINT IDENTITY(1,1) NOT NULL,

    -- 所屬競標商品 ID
    auction_product_id BIGINT NOT NULL,

    -- 圖片 URL 或檔案路徑
    image_url NVARCHAR(500) NOT NULL,

    -- 圖片顯示順序
    sort_order INT NOT NULL
        CONSTRAINT DF_auction_product_images_sort_order DEFAULT (0),

    -- 是否為商品主圖
    is_primary BIT NOT NULL
        CONSTRAINT DF_auction_product_images_is_primary DEFAULT (0),

    -- 圖片資料建立時間
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_auction_product_images_created_at DEFAULT (SYSDATETIME()),

    -- Primary Key
    CONSTRAINT PK_auction_product_images
        PRIMARY KEY (image_id),

    -- 每張圖片必須屬於一筆競標商品
    CONSTRAINT FK_auction_product_images_auction_products
        FOREIGN KEY (auction_product_id)
            REFERENCES dbo.auction_products(auction_product_id)
);
GO