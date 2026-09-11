# BASE 合併前 Checklist

- [ ] 所有人使用 Java 17。
- [ ] 後端皆從這份 `sneakermarket-backend` BASE 分支開始。
- [ ] 每人只在自己的模組 package 開發為主。
- [ ] 共用 package 變更先在群組說明。
- [ ] `members`、`brands`、`categories` 等共用資料表由指定擁有者維護。
- [ ] 所有資料表以 JPA Entity 作為 Schema 建立與維護來源，完整描述欄位、FK、UNIQUE、INDEX 與 CHECK；不要求人工 CREATE TABLE 或手動 migration。
- [ ] local 維持 `spring.jpa.hibernate.ddl-auto=update`；production 使用 `${JPA_DDL_AUTO:validate}`，新環境首次建置可明確設定 `JPA_DDL_AUTO=update`，完成後切回 `validate`。
- [ ] 以 Hibernate Schema 測試驗證 Entity 建表、重啟資料保留及 validate 相容性；既有資料表不可 DROP 或重建。
- [ ] 不提交 DB 密碼、MAIL 密碼、JWT Secret。
- [ ] Controller 不直接呼叫 Repository。
- [ ] 前後端 API 路徑以 `/api` 開頭。
- [ ] JWT 未完成前，`SecurityConfig` 的 permitAll 只是開發暫時設定。
