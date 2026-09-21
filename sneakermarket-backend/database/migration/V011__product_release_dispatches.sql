/*
 * 通知模組用來記錄哪些商品已經處理過新品上架通知。
 *
 * migration 執行當下已經上架的商品先建立基準紀錄，
 * 避免系統部署後突然通知所有歷史商品。
 */
CREATE TABLE dbo.product_release_notification_dispatches
(
    dispatch_id BIGINT IDENTITY(1,1) NOT NULL,

    product_id BIGINT NOT NULL,

    notification_count INT NOT NULL
        CONSTRAINT DF_product_release_dispatch_count
        DEFAULT (0),

    processed_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_product_release_dispatch_processed_at
        DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_product_release_notification_dispatches
        PRIMARY KEY (dispatch_id),

    CONSTRAINT UQ_product_release_notification_product
        UNIQUE (product_id)
);
GO

/*
 * 將 migration 執行前已經上架的商品標記為歷史資料，
 * 不對這些商品補發新品通知。
 */
INSERT INTO dbo.product_release_notification_dispatches
(
    product_id,
    notification_count,
    processed_at
)
SELECT
    product_id,
    0,
    SYSDATETIME()
FROM dbo.products
WHERE status = 'ON_SALE';
GO