# Database 與 Schema 管理

本專題使用 Microsoft SQL Server（MSSQL），所有資料表一律以 **JPA Entity** 作為 Schema 建立與維護來源。Entity 必須描述欄位、型別、長度、nullable、PK / IDENTITY、FK、UNIQUE、INDEX 與 CHECK；不使用人工 CREATE TABLE 或手動 migration 作為功能必要步驟。

- local：維持 `spring.jpa.hibernate.ddl-auto=update`，由 Hibernate 根據 Entity 建立／更新 Schema。
- production：`spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO:validate}`。新環境首次建置可明確設定 `JPA_DDL_AUTO=update`；完成後移除覆寫或改為 `validate`。
- 既有資料表與資料必須保留，不以 DROP／重建處理 Schema 更新。
- Schema 變更同步更新 Entity 與測試；除 Hibernate validate 外，另驗證 FK、UNIQUE、INDEX、CHECK 等約束。

目錄中的 `schema/`、`migration/` 既有 SQL 保留為歷史參考，不作為新環境建表或功能啟用的必要步驟；本輪不刪改其他模組的既有 SQL。`seed/` 為開發測試資料，與 Schema 建置分開。

功能 10 的 `auction_email_logs` 由 `AuctionEmailLog` Entity 完整描述。既有本機表直接沿用，不重新建立。

`auction_email_preflight.sql` 保留為 Email 啟用前的唯讀資料檢查，用於核對候選筆數及收件信箱；它不建立或變更 Schema，也不是 migration。完整交易、設定與測試說明見 [競標 Email](../docs/auction-email.md)。
