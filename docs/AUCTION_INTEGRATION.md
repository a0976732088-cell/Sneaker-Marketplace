# 競標模組整合修正紀錄

日期：2026-09-07

本次修正競標既有功能的會員身分、共用主資料與時間處理。修改範圍為後端 auction package、競標測試、前端競標頁、競標專用工具，以及 application-local.properties 中的 auction.dev 設定。未修改會員、商城、通知、行事曆、廣告的程式或資料表。

## 已完成

- 移除所有固定會員 fallback。使用會員組既有 Session["memberId"]，型別為 Long。
- 競標寫入前確認 members 中會員仍存在且 deleted_at 為 NULL；訪客、已失效 Session、已刪除會員回 401。非賣家本人仍回 403。
- 公開詳細查詢不建立 Session；失效會員可匿名瀏覽，isOwner 為 false。
- 競標頁收到 401 導向既有 /login，403 導向既有 /forbidden；未改共用 router、httpClient 或會員登入頁。
- 建立、編輯及列表選項改由後端讀取實際 brands/categories，不依赖 Seed ID。建立與可修改品牌分類的編輯操作驗證引用存在性；已開始的競標仍保留原品牌／分類並驗證其存在性。
- 列表與詳細回應補品牌／分類名稱。歷史引用不存在時名稱為 null，畫面顯示資料未提供。
- 競標後端固定 Asia/Taipei 業務時間；前端將無 offset 日期視為 +08:00，顯示時固定 Asia/Taipei。保留既有 LocalDateTime / DATETIME2 契約。
- 編輯／取消取得 Auction row lock 後檢查截止時間。已到期即拒絕，不等待結標排程；SCHEDULED 但實際已開拍的競標也禁止改開始時間與品牌／分類。

## 競標 API 增補

原有 API 路徑及 ApiResponse 外框保持不變。

| API | 行為 |
| --- | --- |
| GET /api/auctions/options | 公開唯讀選項；data 為 { brands: [{ id, name }], categories: [{ id, name }] }，排序依 displayOrder、ID。 |
| GET /api/auctions/session | 驗證相同 API 主機上的 Session；有效回 200、data=null，無效回 401，不公開會員個資。 |
| GET /api/auctions | items 增加 brandId、categoryId、brandName、categoryName。既有 brandId/categoryId 篩選接上前端。 |
| GET /api/auctions/{auctionId} | 增加 brandName、categoryName、canManage、hasStarted。canManage 為當次查詢快照，寫入時仍重新驗證。 |

AuctionReferenceRepository 只查詢會員／商城 owner 已存在的 JPA Entity，不提供寫入。待 owner 有正式唯讀 Service 契約時，集中替換此整合入口即可。沒有新增品牌／分類／會員 Entity 或共用資料表。

## 保留的跨組事項

以下事項無法在「其他功能不做更動」的範圍內完整修正，尚未接上，不能標記為完成。

| 項目 | 本次界線與後續配合 |
| --- | --- |
| 會員實體刪除與 FK | 競標拒絕已不存在／deleted_at 非 NULL 的會員。會員組仍有實體刪除；需 owner 決定刪除策略，再清查歷史資料並補 FK。本次沒有執行 DDL 或清理資料，服務層檢查不取代 FK，也無法完全封鎖跨交易的刪除競態。 |
| 通知收件人與權限 | 通知 DTO／Entity 的 memberId 尚未啟用；競標沒有呼叫未完成的通知流程。需通知組補收件會員、本人查閱及冪等契約後，於競標交易成功後串接。 |
| 行事曆同步 | 尚未建立／更新／停用 calendar_events。後續依 auctionId 識別來源，開拍／截標各一事件；改期更新既有事件，取消停用事件及提醒。 |
| 廣告目標有效性 | 廣告尚未呼叫競標有效性判斷。後續使用 auctionId（不是 auctionProductId）與 /auctions/{id}；依 startAt/endAt/status 判斷可展示性。 |
| 登入服務位址 | 競標 session 檢查與寫入均走共用 httpClient 的 VITE_API_BASE_URL。會員 authService 仍固定 localhost，部署前需會員組統一；本次未修改它。 |
| 共用權限配置 | SecurityConfig、會員角色、全站登入策略維持原狀。競標自身寫入有身分與本人檢查；不代表全站角色權限已完成。 |

一般訂單／折價券／庫存維持商城責任。出價、結標、競標訂單、Email 與自動同步仍是後續功能，本次沒有宣稱完成。

## 驗證

- Maven 編譯主程式與所有既有測試通過。
- 競標 MockMvc／單元測試涵蓋 Session、401/403、公開查詢、刪除會員、主資料拒絕、名稱與操作權契約、截止時間及台北業務時間。
- 前端 node:test 覆蓋台北日期解析、offset、跨日顯示與無效日期；在 UTC 環境執行。
- Vite production build 通過。
- 未執行實際 SQL Server 整合測試或瀏覽器端到端驗收；原有 @SpringBootTest 測試仍需要本機資料庫與合法的會員／品牌／分類測試資料。
- 專案 Maven wrapper 缺 .mvn/wrapper/maven-wrapper.properties，本次以本機既有 Maven 3.9.16 執行，不修改共用建置設定。

可重跑（後端目錄使用可用的 mvn）：

~~~text
mvn -Dtest=AuctionAccessTests,AuctionCommandValidationTests,AuctionQueryContractTests,AuctionTimeTests test
~~~

前端目錄：

~~~text
node --test tests/auctionTime.test.js
npm run build
~~~

實際環境驗收順序：訪客進建立頁應轉登入；會員 A 登入後建立／編輯／取消自己的競標；會員 B 操作 A 刊登回 403；登出後直接操作回 401；無效品牌／分類回 400；確認選項及名稱符合實際主資料。
