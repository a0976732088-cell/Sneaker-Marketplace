-- 跨模組共用主檔：brands（品牌）、categories（分類）


-- SneakerMarketDB.dbo.brands definition

CREATE TABLE SneakerMarketDB.dbo.brands (
                                            brand_id bigint IDENTITY(1,1) NOT NULL,
                                            brand_name nvarchar(80) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
                                            description nvarchar(500) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
                                            display_order int DEFAULT 0 NOT NULL,
                                            created_at datetime2(0) DEFAULT sysdatetime() NOT NULL,
                                            updated_at datetime2(0) DEFAULT sysdatetime() NOT NULL,
                                            CONSTRAINT PK_brands PRIMARY KEY (brand_id),
                                            CONSTRAINT UQ_brands_brand_name UNIQUE (brand_name)
);


-- SneakerMarketDB.dbo.categories definition

CREATE TABLE SneakerMarketDB.dbo.categories (
                                                category_id bigint IDENTITY(1,1) NOT NULL,
                                                category_name nvarchar(80) COLLATE SQL_Latin1_General_CP1_CI_AS NOT NULL,
                                                description nvarchar(500) COLLATE SQL_Latin1_General_CP1_CI_AS NULL,
                                                display_order int DEFAULT 0 NOT NULL,
                                                created_at datetime2(0) DEFAULT sysdatetime() NOT NULL,
                                                updated_at datetime2(0) DEFAULT sysdatetime() NOT NULL,
                                                CONSTRAINT PK_categories PRIMARY KEY (category_id),
                                                CONSTRAINT UQ_categories_category_name UNIQUE (category_name)
);