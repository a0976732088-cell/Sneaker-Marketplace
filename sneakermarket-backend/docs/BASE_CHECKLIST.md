# BASE 合併前 Checklist

- [ ] 所有人使用 Java 17。
- [ ] 後端皆從這份 `sneakermarket-backend` BASE 分支開始。
- [ ] 每人只在自己的模組 package 開發為主。
- [ ] 共用 package 變更先在群組說明。
- [ ] `members`、`brands`、`categories` 等共用資料表由指定擁有者維護。
- [ ] DB 變更新增 migration SQL，不直接改掉已合併腳本。
- [ ] 不提交 DB 密碼、MAIL 密碼、JWT Secret。
- [ ] Controller 不直接呼叫 Repository。
- [ ] 前後端 API 路徑以 `/api` 開頭。
- [ ] JWT 未完成前，`SecurityConfig` 的 permitAll 只是開發暫時設定。
