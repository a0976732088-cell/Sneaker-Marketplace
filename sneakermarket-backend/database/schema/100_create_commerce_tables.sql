-- 一般商城模組：products、product_variants、product_images、carts、cart_items、orders、order_items
-- 注意：products 有外鍵參照 brands、categories（跨模組共用主檔），
-- 執行本腳本前，請先確認 099_create_shared_tables.sql 已經執行過

-- SneakerMarketDB.dbo.carts definition

CREATE TABLE SneakerMarketDB.dbo.carts (
                                           cart_id bigint IDENTITY(1,1) NOT NULL,
                                           member_id bigint NOT NULL,
                                           created_at datetime2(0) DEFAULT sysdatetime() NOT NULL,
                                           updated_at datetime2(0) DEFAULT sysdatetime() NOT NULL,
                                           CONSTRAINT PK_carts PRIMARY KEY (cart_id),
                                           CONSTRAINT UQ_carts_member_id UNIQUE (member_id)
);


-- SneakerMarketDB.dbo.orders definition

CREATE TABLE SneakerMarketDB.dbo.orders (
                                            order_id bigint IDENTITY(1,1) NOT NULL,
                                            order_number nvarchar(40) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
                                            member_id bigint NOT NULL,
                                            coupon_id bigint NULL,
                                            subtotal decimal(12,2) NOT NULL,
                                            discount_amount decimal(12,2) DEFAULT 0 NOT NULL,
                                            shipping_fee decimal(12,2) DEFAULT 0 NOT NULL,
                                            total_amount decimal(12,2) NOT NULL,
                                            payment_method nvarchar(30) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
                                            status nvarchar(30) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
                                            receiver_name nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
                                            receiver_phone nvarchar(20) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
                                            shipping_address nvarchar(300) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
                                            created_at datetime2(0) DEFAULT sysdatetime() NOT NULL,
                                            updated_at datetime2(0) DEFAULT sysdatetime() NOT NULL,
                                            paid_at datetime2(0) NULL,
                                            completed_at datetime2(0) NULL,
                                            CONSTRAINT PK_orders PRIMARY KEY (order_id),
                                            CONSTRAINT UQ_orders_order_number UNIQUE (order_number)
);


-- SneakerMarketDB.dbo.products definition

CREATE TABLE SneakerMarketDB.dbo.products (
                                              product_id bigint IDENTITY(1,1) NOT NULL,
                                              brand_id bigint NOT NULL,
                                              category_id bigint NOT NULL,
                                              product_name nvarchar(150) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
                                              description nvarchar(MAX) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
                                              status nvarchar(20) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
                                              created_at datetime2(0) DEFAULT sysdatetime() NOT NULL,
                                              updated_at datetime2(0) DEFAULT sysdatetime() NOT NULL,
                                              CONSTRAINT PK_products PRIMARY KEY (product_id),
                                              CONSTRAINT FK_products_brands FOREIGN KEY (brand_id) REFERENCES SneakerMarketDB.dbo.brands(brand_id),
                                              CONSTRAINT FK_products_categories FOREIGN KEY (category_id) REFERENCES SneakerMarketDB.dbo.categories(category_id)
);


-- SneakerMarketDB.dbo.product_images definition

CREATE TABLE SneakerMarketDB.dbo.product_images (
                                                    image_id bigint IDENTITY(1,1) NOT NULL,
                                                    product_id bigint NOT NULL,
                                                    image_url nvarchar(500) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
                                                    sort_order int DEFAULT 0 NOT NULL,
                                                    is_primary bit DEFAULT 0 NOT NULL,
                                                    created_at datetime2(0) DEFAULT sysdatetime() NOT NULL,
                                                    CONSTRAINT PK_product_images PRIMARY KEY (image_id),
                                                    CONSTRAINT FK_product_images_products FOREIGN KEY (product_id) REFERENCES SneakerMarketDB.dbo.products(product_id)
);


-- SneakerMarketDB.dbo.product_variants definition

CREATE TABLE SneakerMarketDB.dbo.product_variants (
                                                      variant_id bigint IDENTITY(1,1) NOT NULL,
                                                      product_id bigint NOT NULL,
                                                      sku nvarchar(80) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
    [size] nvarchar(20) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
    color nvarchar(50) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
    [condition] nvarchar(20) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
    price decimal(12,2) NOT NULL,
    stock int DEFAULT 0 NOT NULL,
    created_at datetime2(0) DEFAULT sysdatetime() NOT NULL,
    updated_at datetime2(0) DEFAULT sysdatetime() NOT NULL,
    CONSTRAINT PK_product_variants PRIMARY KEY (variant_id),
    CONSTRAINT FK_product_variants_products FOREIGN KEY (product_id) REFERENCES SneakerMarketDB.dbo.products(product_id)
    );
CREATE UNIQUE NONCLUSTERED INDEX UX_product_variants_sku ON SneakerMarketDB.dbo.product_variants (  sku ASC  )
     WHERE  ([sku] IS NOT NULL)
     WITH (  PAD_INDEX = OFF ,FILLFACTOR = 100  ,SORT_IN_TEMPDB = OFF , IGNORE_DUP_KEY = OFF , STATISTICS_NORECOMPUTE = OFF , ONLINE = OFF , ALLOW_ROW_LOCKS = ON , ALLOW_PAGE_LOCKS = ON  )
     ON [PRIMARY ] ;


-- SneakerMarketDB.dbo.cart_items definition

CREATE TABLE SneakerMarketDB.dbo.cart_items (
                                                cart_item_id bigint IDENTITY(1,1) NOT NULL,
                                                cart_id bigint NOT NULL,
                                                variant_id bigint NOT NULL,
                                                quantity int NOT NULL,
                                                selected bit DEFAULT 1 NOT NULL,
                                                created_at datetime2(0) DEFAULT sysdatetime() NOT NULL,
                                                updated_at datetime2(0) DEFAULT sysdatetime() NOT NULL,
                                                CONSTRAINT PK_cart_items PRIMARY KEY (cart_item_id),
                                                CONSTRAINT FK_cart_items_carts FOREIGN KEY (cart_id) REFERENCES SneakerMarketDB.dbo.carts(cart_id),
                                                CONSTRAINT FK_cart_items_variants FOREIGN KEY (variant_id) REFERENCES SneakerMarketDB.dbo.product_variants(variant_id)
);


-- SneakerMarketDB.dbo.order_items definition

CREATE TABLE SneakerMarketDB.dbo.order_items (
                                                 order_item_id bigint IDENTITY(1,1) NOT NULL,
                                                 order_id bigint NOT NULL,
                                                 variant_id bigint NULL,
                                                 product_name nvarchar(150) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
                                                 specification nvarchar(150) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
                                                 unit_price decimal(12,2) NOT NULL,
                                                 quantity int NOT NULL,
                                                 line_total decimal(12,2) NOT NULL,
                                                 CONSTRAINT PK_order_items PRIMARY KEY (order_item_id),
                                                 CONSTRAINT FK_order_items_orders FOREIGN KEY (order_id) REFERENCES SneakerMarketDB.dbo.orders(order_id),
                                                 CONSTRAINT FK_order_items_variants FOREIGN KEY (variant_id) REFERENCES SneakerMarketDB.dbo.product_variants(variant_id)
);