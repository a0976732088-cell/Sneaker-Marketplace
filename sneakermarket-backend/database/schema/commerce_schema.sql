/* =====================================================================
   KICKS（SneakerMarket）一般商城模組 — 資料表結構
   負責人：郁涵（一般商城模組）
   產生日期：2026-09-27（由 SneakerMarketDB 現行結構匯出後整理）

   內容：建立 10 張資料表
     共用主檔：brands、categories（競標模組的 auction_products 也會參照）
     門市資料：cvs_stores（7-11 店到店地圖選店用）
     商品：products、product_variants、product_images
     購物車：carts、cart_items
     訂單：orders、order_items

   執行說明：
     1. 請先在 DBeaver / SSMS 切換到 Demo 用的資料庫再執行本檔。
     2. 本檔必須在「競標模組」的建表腳本之前執行，
        因為 auction_products 需要參照 brands、categories。
     3. 本檔不含 DROP TABLE。若 Demo 資料庫已有同名資料表，
        請先手動刪除（刪除順序要先刪子表，例如先刪 auction_products、
        order_items、cart_items、product_images、product_variants、products，
        最後才刪 brands、categories）。
     4. 外鍵統一放在檔案最後建立，建表順序不會互相卡住。
     5. 執行完本檔後，再執行 commerce_seed.sql 匯入假資料。
   ===================================================================== */


/* ---------------------------------------------------------------------
   一、共用主檔
   --------------------------------------------------------------------- */

-- 品牌主檔（跨模組共用：一般商城 products、競標 auction_products 都會參照）
CREATE TABLE brands (
                        brand_id        bigint IDENTITY(1, 1) NOT NULL,         -- 品牌編號（主鍵）
                        brand_name      nvarchar(80)  NOT NULL,                 -- 品牌名稱（不可重複）
                        description     nvarchar(500) NULL,                     -- 品牌介紹
                        display_order   int DEFAULT 0 NOT NULL,                 -- 前台顯示順序
                        created_at      datetime2(0) DEFAULT sysdatetime() NOT NULL,
                        updated_at      datetime2(0) DEFAULT sysdatetime() NOT NULL,
                        CONSTRAINT PK_brands PRIMARY KEY (brand_id),
                        CONSTRAINT UQ_brands_brand_name UNIQUE (brand_name)
);

-- 球鞋分類主檔（跨模組共用：籃球鞋、慢跑鞋、休閒鞋、滑板鞋）
CREATE TABLE categories (
                            category_id     bigint IDENTITY(1, 1) NOT NULL,         -- 分類編號（主鍵）
                            category_name   nvarchar(80)  NOT NULL,                 -- 分類名稱（不可重複）
                            description     nvarchar(500) NULL,                     -- 分類說明
                            display_order   int DEFAULT 0 NOT NULL,                 -- 前台顯示順序
                            created_at      datetime2(0) DEFAULT sysdatetime() NOT NULL,
                            updated_at      datetime2(0) DEFAULT sysdatetime() NOT NULL,
                            CONSTRAINT PK_categories PRIMARY KEY (category_id),
                            CONSTRAINT UQ_categories_category_name UNIQUE (category_name)
);


/* ---------------------------------------------------------------------
   二、超商門市
   --------------------------------------------------------------------- */

-- 7-11 門市資料（含經緯度，給結帳頁的地圖選店功能使用）
CREATE TABLE cvs_stores (
                            id              bigint IDENTITY(1, 1) NOT NULL,         -- 流水號（主鍵）
                            store_id        nvarchar(20)  NOT NULL,                 -- 門市代號（綠界物流用）
                            store_name      nvarchar(100) NOT NULL,                 -- 門市名稱
                            address         nvarchar(200) NOT NULL,                 -- 門市地址
                            phone           nvarchar(30)  NULL,                     -- 門市電話
    [chain]         nvarchar(20)  NOT NULL,                 -- 超商別（UNIMART = 7-11）
    city            nvarchar(20)  NOT NULL,                 -- 縣市（篩選用）
    latitude        decimal(10, 7) NULL,                    -- 緯度
    longitude       decimal(10, 7) NULL,                    -- 經度
    CONSTRAINT PK_cvs_stores PRIMARY KEY (id),
    CONSTRAINT UQ_cvs_stores_store_id_chain UNIQUE (store_id, [chain])  -- 同一超商的門市代號不可重複
    );


/* ---------------------------------------------------------------------
   三、商品
   --------------------------------------------------------------------- */

-- 商品主檔（一雙鞋款一筆）
CREATE TABLE products (
                          product_id      bigint IDENTITY(1, 1) NOT NULL,         -- 商品編號（主鍵）
                          brand_id        bigint NOT NULL,                        -- 品牌（外鍵 → brands）
                          category_id     bigint NOT NULL,                        -- 分類（外鍵 → categories）
                          product_name    nvarchar(150) NOT NULL,                 -- 商品名稱
                          description     nvarchar(MAX) NULL,                     -- 商品介紹
                          status          nvarchar(20)  NOT NULL,                 -- 狀態：ON_SALE 上架／OFF_SHELF 下架／DRAFT 草稿
                          created_at      datetime2(0) DEFAULT sysdatetime() NOT NULL,
                          updated_at      datetime2(0) DEFAULT sysdatetime() NOT NULL,
                          CONSTRAINT PK_products PRIMARY KEY (product_id)
);

-- 商品規格（同一鞋款的不同尺寸，各自有價格與庫存）
CREATE TABLE product_variants (
                                  variant_id      bigint IDENTITY(1, 1) NOT NULL,         -- 規格編號（主鍵）
                                  product_id      bigint NOT NULL,                        -- 所屬商品（外鍵 → products）
                                  sku             nvarchar(80)  NULL,                     -- 商品貨號（可為空，有填則不可重複）
    [size]          nvarchar(20)  NOT NULL,                 -- 尺寸，例如 US 7.5
    color           nvarchar(50)  NOT NULL,                 -- 配色
    [condition]     nvarchar(20)  NOT NULL,                 -- 商品狀況（一般商城皆為 NEW 全新）
    price           decimal(12, 2) NOT NULL,                -- 售價
    stock           int DEFAULT 0 NOT NULL,                 -- 庫存數量
    created_at      datetime2(0) DEFAULT sysdatetime() NOT NULL,
    updated_at      datetime2(0) DEFAULT sysdatetime() NOT NULL,
    CONSTRAINT PK_product_variants PRIMARY KEY (variant_id)
    );

-- sku 有填值時不可重複（sku 為 NULL 的規格不受限制）
CREATE UNIQUE NONCLUSTERED INDEX UX_product_variants_sku
    ON product_variants (sku ASC)
    WHERE [sku] IS NOT NULL;

-- 商品圖片（圖片檔放在後端 static/images/products/，會跟著 Git 一起過去）
CREATE TABLE product_images (
                                image_id        bigint IDENTITY(1, 1) NOT NULL,         -- 圖片編號（主鍵）
                                product_id      bigint NOT NULL,                        -- 所屬商品（外鍵 → products）
                                image_url       nvarchar(500) NOT NULL,                 -- 圖片路徑，例如 /images/products/xxx.jpg
                                sort_order      int DEFAULT 0 NOT NULL,                 -- 圖片排序
                                is_primary      bit DEFAULT 0 NOT NULL,                 -- 是否為主圖（1 = 主圖）
                                created_at      datetime2(0) DEFAULT sysdatetime() NOT NULL,
                                CONSTRAINT PK_product_images PRIMARY KEY (image_id)
);


/* ---------------------------------------------------------------------
   四、購物車
   --------------------------------------------------------------------- */

-- 購物車（每位會員一台）
CREATE TABLE carts (
                       cart_id         bigint IDENTITY(1, 1) NOT NULL,         -- 購物車編號（主鍵）
                       member_id       bigint NOT NULL,                        -- 會員編號（每位會員只能有一台購物車）
                       created_at      datetime2(0) DEFAULT sysdatetime() NOT NULL,
                       updated_at      datetime2(0) DEFAULT sysdatetime() NOT NULL,
                       CONSTRAINT PK_carts PRIMARY KEY (cart_id),
                       CONSTRAINT UQ_carts_member_id UNIQUE (member_id)
);

-- 購物車明細（購物車裡的每一個商品規格）
CREATE TABLE cart_items (
                            cart_item_id    bigint IDENTITY(1, 1) NOT NULL,         -- 明細編號（主鍵）
                            cart_id         bigint NOT NULL,                        -- 所屬購物車（外鍵 → carts）
                            variant_id      bigint NOT NULL,                        -- 商品規格（外鍵 → product_variants）
                            quantity        int NOT NULL,                           -- 數量
                            selected        bit DEFAULT 1 NOT NULL,                 -- 是否勾選要結帳（1 = 勾選）
                            created_at      datetime2(0) DEFAULT sysdatetime() NOT NULL,
                            updated_at      datetime2(0) DEFAULT sysdatetime() NOT NULL,
                            CONSTRAINT PK_cart_items PRIMARY KEY (cart_item_id)
);


/* ---------------------------------------------------------------------
   五、訂單
   --------------------------------------------------------------------- */

-- 一般商城訂單（與競標模組的 auction_orders 分開）
CREATE TABLE orders (
                        order_id        bigint IDENTITY(1, 1) NOT NULL,         -- 訂單編號（主鍵）
                        order_number    nvarchar(40)  NOT NULL,                 -- 對外顯示的訂單號碼（不可重複）
                        member_id       bigint NOT NULL,                        -- 下單會員
                        coupon_id       bigint NULL,                            -- 使用的優惠券（沒用則為 NULL）
                        subtotal        decimal(12, 2) NOT NULL,                -- 商品小計
                        discount_amount decimal(12, 2) DEFAULT 0 NOT NULL,      -- 折扣金額
                        shipping_fee    decimal(12, 2) DEFAULT 0 NOT NULL,      -- 運費
                        total_amount    decimal(12, 2) NOT NULL,                -- 應付總額
                        payment_method  nvarchar(30)  NOT NULL,                 -- 付款方式（綠界信用卡、LINE Pay）
                        status          nvarchar(30)  NOT NULL,                 -- 訂單狀態
                        receiver_name   nvarchar(50)  NOT NULL,                 -- 收件人姓名
                        receiver_phone  nvarchar(20)  NOT NULL,                 -- 收件人電話
                        shipping_address nvarchar(300) NOT NULL,                -- 收件地址（超商取貨時存門市地址）
                        created_at      datetime2(0) DEFAULT sysdatetime() NOT NULL,
                        updated_at      datetime2(0) DEFAULT sysdatetime() NOT NULL,
                        paid_at         datetime2(0) NULL,                      -- 付款完成時間
                        completed_at    datetime2(0) NULL,                      -- 訂單完成時間
                        shipping_method varchar(30)   NULL,                     -- 配送方式（宅配／超商取貨）
                        cvs_store_id    nvarchar(20)  NULL,                     -- 7-11 取貨門市代號
                        cvs_store_name  nvarchar(100) NULL,                     -- 7-11 取貨門市名稱
                        cvs_address     nvarchar(200) NULL,                     -- 7-11 取貨門市地址
                        CONSTRAINT PK_orders PRIMARY KEY (order_id),
                        CONSTRAINT UQ_orders_order_number UNIQUE (order_number)
);

-- 訂單明細（下單當下的商品快照，商品之後改價或刪除都不影響）
CREATE TABLE order_items (
                             order_item_id   bigint IDENTITY(1, 1) NOT NULL,         -- 明細編號（主鍵）
                             order_id        bigint NOT NULL,                        -- 所屬訂單（外鍵 → orders）
                             variant_id      bigint NULL,                            -- 商品規格（外鍵 → product_variants，規格被刪除時可為 NULL）
                             product_name    nvarchar(150) NOT NULL,                 -- 下單時的商品名稱
                             specification   nvarchar(150) NOT NULL,                 -- 下單時的規格（尺寸／配色）
                             unit_price      decimal(12, 2) NOT NULL,                -- 下單時的單價
                             quantity        int NOT NULL,                           -- 數量
                             line_total      decimal(12, 2) NOT NULL,                -- 小計（單價 × 數量）
                             image_url       varchar(500)  NULL,                     -- 下單時的商品主圖路徑
                             CONSTRAINT PK_order_items PRIMARY KEY (order_item_id)
);


/* ---------------------------------------------------------------------
   六、外鍵（所有表建立完成後再加上）
   --------------------------------------------------------------------- */

ALTER TABLE products         ADD CONSTRAINT FK_products_brands           FOREIGN KEY (brand_id)    REFERENCES brands(brand_id);
ALTER TABLE products         ADD CONSTRAINT FK_products_categories       FOREIGN KEY (category_id) REFERENCES categories(category_id);
ALTER TABLE product_variants ADD CONSTRAINT FK_product_variants_products FOREIGN KEY (product_id)  REFERENCES products(product_id);
ALTER TABLE product_images   ADD CONSTRAINT FK_product_images_products   FOREIGN KEY (product_id)  REFERENCES products(product_id);
ALTER TABLE cart_items       ADD CONSTRAINT FK_cart_items_carts          FOREIGN KEY (cart_id)     REFERENCES carts(cart_id);
ALTER TABLE cart_items       ADD CONSTRAINT FK_cart_items_variants       FOREIGN KEY (variant_id)  REFERENCES product_variants(variant_id);
ALTER TABLE order_items      ADD CONSTRAINT FK_order_items_orders        FOREIGN KEY (order_id)    REFERENCES orders(order_id);
ALTER TABLE order_items      ADD CONSTRAINT FK_order_items_variants      FOREIGN KEY (variant_id)  REFERENCES product_variants(variant_id);