# 酷客 Kicks｜球鞋電子商城與競標平台

> SneakerMarket 是五人團隊共同開發的球鞋電子商務專題，以同一會員身分整合一般商城、競標交易、廣告、行事曆與通知服務。

本 Repository 採 **Monorepo** 管理，前端與後端為兩個獨立專案。系統目前以 **Vue 3 + Spring Boot + Microsoft SQL Server** 為主要技術架構，並整合即時通訊、排程、Email、第三方金流、超商門市、Google Calendar 與 AI 客服等功能。

> 本 README 以目前各模組的最新整合內容為準；`main` 為穩定基線，開發與整合內容分布於 `dev` 與各模組分支。

## 個人負責模組｜Advertisement System（廣告管理系統）

**Backend：** Java 17、Spring Boot、Spring Data JPA、SQL Server

**Frontend：** Vue 3、JavaScript、Axios

### 模組介紹

負責球鞋電商平台的廣告管理系統，包含廣告 CRUD、Banner 輪播、彈窗、上下架排程及廣告成效統計，並完成前後端 API 串接。

### 主要功能

- **廣告管理**：新增、查詢、修改、刪除及廣告狀態控制。
- **排程管理**：設定廣告開始與結束時間，透過 Spring Scheduler 處理上下架。
- **成效統計**：紀錄廣告曝光次數、點擊次數並計算 CTR。
- **曝光追蹤**：使用瀏覽器 IntersectionObserver API 判斷廣告是否達到曝光條件。
- **前後端整合**：Vue 3 透過 Axios 呼叫 Spring Boot REST API。

 ### 個人實作與設計重點

- **分層架構**：採用 Controller、Service、Repository 分層設計，將 HTTP 請求處理、業務邏輯與資料存取分離，並透過 DTO 傳輸 API 資料。
- **廣告曝光判定**：使用 IntersectionObserver API，當廣告可見比例達 50% 且持續 1 秒時才觸發曝光紀錄，減少短暫滑過造成的統計誤差。
- **重複統計控制**：透過 HttpSession 記錄同一 Session 當日已曝光及點擊的廣告，避免重複計數。
- **廣告成效分析**：由後端統計曝光數、點擊數，並計算 CTR，供廣告管理後台查看。
- **廣告排程管理**：利用 Spring Scheduler 每分鐘檢查已到期的廣告，並透過時間及狀態條件篩選前台可顯示的廣告。

### 原始碼

- [後端廣告模組](sneakermarket-backend/src/main/java/com/project/sneakermarket/advertisement)
- [前端廣告元件](sneakermarket-frontend/src/components/advertisement)
  
## 系統模組

| 模組 | 主要功能 |
| --- | --- |
| **會員系統 Member** | 註冊、登入、個人資料、會員中心、角色與管理功能 |
| **一般商城 Commerce** | 商品瀏覽、搜尋／篩選、商品規格、購物車、優惠券、結帳、訂單 |
| **競標商城 Auction** | 競標刊登、搜尋／篩選、出價、自動結標、競標訂單、付款、管理後台 |
| **廣告系統 Advertisement** | Banner／彈窗／小型廣告、排程上下架、導流、曝光與點擊統計 |
| **行事曆／通知 Calendar & Notification** | 活動行事曆、關注／提醒、站內通知、Email、Google Calendar 同步 |

另外包含共用的 **Coupon、Admin、Security、Exception、Response** 等支援模組。

## 專案特色

- **一般商城與競標商城並存**：固定售價商品與競標商品採不同交易流程，但共用會員身分與網站導覽。
- **完整競標生命週期**：商品刊登 → 開標 → 出價 → 自動結標 → 得標訂單 → 付款／逾期棄標。
- **競標併發控制**：後端使用交易與 `PESSIMISTIC_WRITE` 鎖定，送出出價時重新驗證最新價格與競標狀態。
- **即時競標更新**：使用 WebSocket／STOMP 推送競標價格、出價次數、狀態及會員私人競標通知。
- **排程與通知機制**：包含競標生命週期、結標、付款期限、活動提醒、Email 重試等背景工作。
- **第三方服務整合**：LINE Pay、ECPay、Google Calendar、Gmail SMTP、Gemini API、OpenStreetMap／Nominatim。
- **多種交易與物流情境**：一般商城與競標訂單皆具獨立流程；競標支援 LINE Pay 與 ECPay 超商門市選擇。
- **前端元件化**：Vue 3 搭配 Router、Axios 與共用 Component，拆分商城、競標、會員、行事曆與管理後台畫面。

## 系統架構

~~~text
Web Browser
    │
    ▼
Vue 3 + Vite
Vue Router / Axios / Bootstrap
    │
    ├── RESTful API / HTTP
    └── WebSocket / STOMP
            │
            ▼
Spring Boot 4.0.8
    ├── Member
    ├── Commerce
    ├── Auction
    ├── Advertisement
    ├── Calendar / Notification
    ├── Coupon
    └── Admin
            │
            ▼
Spring Data JPA / Hibernate
            │
            ▼
Microsoft SQL Server

External Services
    ├── LINE Pay
    ├── ECPay
    ├── Google Calendar API
    ├── Gmail SMTP
    ├── Gemini API
    ├── OpenStreetMap / Nominatim
    └── Redis（通知工作佇列，可選）
~~~

## 技術棧

### Frontend

- Vue `3.5.41`
- Vite `7.3.6`
- Vue Router
- Axios
- Bootstrap 5
- Font Awesome
- STOMP.js
- VueUse
- Vue Datepicker
- Chart.js
- Leaflet
- HTML5 / CSS3 / JavaScript

預設開發 Port：`5173`

### Backend

- Java `17`
- Spring Boot `4.0.8`
- Spring Web MVC
- Spring Security
- HttpSession-based authentication
- Spring Data JPA / Hibernate
- Spring Validation
- Spring WebSocket / STOMP
- Spring Mail / JavaMailSender
- Scheduler
- Spring Data Redis
- Maven
- Lombok

預設開發 Port：`8080`

### Database

- Microsoft SQL Server（MSSQL）
- Database：`SneakerMarketDB`
- Schema：`dbo`
- SQL / T-SQL

## 第三方服務

| 服務 | 用途 |
| --- | --- |
| **LINE Pay** | 一般商城／競標訂單行動支付 |
| **ECPay** | 金流及超商門市／物流整合 |
| **Google Calendar API** | 行事曆 OAuth 與事件同步 |
| **Gmail SMTP** | 競標、訂單與通知 Email |
| **Gemini API** | 商城 AI 客服 |
| **OpenStreetMap / Nominatim** | 地圖顯示與地址地理編碼 |
| **Redis** | 通知背景工作佇列（可選） |

## Repository 結構

~~~text
SneakerMarket/
├── sneakermarket-frontend/      # Vue 3 + Vite
│   ├── src/components/          # 共用與各模組 Component
│   ├── src/views/               # 頁面 View
│   ├── src/service/             # API / WebSocket service
│   └── src/router/              # Vue Router
│
├── sneakermarket-backend/       # Spring Boot 4.0.8
│   ├── src/main/java/
│   │   └── com/project/sneakermarket/
│   │       ├── member/
│   │       ├── commerce/
│   │       ├── auction/
│   │       ├── advertisement/
│   │       ├── calendar/
│   │       ├── notification/
│   │       ├── coupon/
│   │       ├── admin/
│   │       └── common/
│   └── database/
│       ├── schema/
│       ├── migration/
│       └── seed/
│
├── docs/                        # 共用規範與系統文件
├── tools/                       # 專案輔助工具
└── README.md
~~~

## 本機開發

### 1. 環境需求

- JDK 17
- Node.js / npm
- Microsoft SQL Server
- Maven Wrapper（Repository 已提供）
- Redis：只有啟用通知 Redis Queue 時才需要
- 第三方服務憑證：依欲測試的功能個別設定

### 2. Database

先建立資料庫：

~~~text
SneakerMarketDB
~~~

目前 Schema 以 **JPA Entity** 為主要維護來源：

- local：`spring.jpa.hibernate.ddl-auto=update`
- production：預設以 `validate` 為主
- `database/schema/`、`database/migration/` 保留既有 SQL 與歷史調整紀錄
- `database/seed/` 提供開發／展示測試資料

### 3. Backend

Windows：

~~~bat
cd sneakermarket-backend
mvnw.cmd spring-boot:run
~~~

macOS / Linux：

~~~bash
cd sneakermarket-backend
./mvnw spring-boot:run
~~~

預設：`http://localhost:8080`

至少需依本機環境設定資料庫連線資訊，例如：

~~~text
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=SneakerMarketDB;encrypt=true;trustServerCertificate=true
DB_USERNAME=sa
DB_PASSWORD=<your-password>
~~~

其他如 SMTP、LINE Pay、ECPay、Google Calendar、Gemini 與 Redis 等設定，依需要啟用。**所有帳密、Secret 與 API Key 均不得提交至 Git。**

### 4. Frontend

~~~bash
cd sneakermarket-frontend
npm install
npm run dev
~~~

預設：`http://localhost:5173`

可使用 Vite 環境變數調整 API／WebSocket 位址，例如：

~~~text
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_URL=ws://localhost:8080/ws
~~~

## 登入與權限

目前系統採 **HttpSession + Cookie** 保存登入狀態，前端 Axios 以 `withCredentials` 傳送 Session Cookie。

- 匿名使用者可瀏覽公開商城、競標與活動內容。
- 會員登入後可使用購物車、訂單、競標、關注與通知等會員功能。
- 管理員可進入各模組管理後台。

> 早期文件曾規劃 JWT；目前實際程式以 HttpSession 為準。

## Git Flow

目前主要分支：

~~~text
main
└── dev
    ├── member-by國信
    ├── commerce-by郁涵
    ├── advertisement-by致維
    ├── calendar-notification-by志宗
    └── auction-by伯瑋
~~~

- `main`：穩定／展示基線。
- `dev`：團隊整合與測試。
- 各模組分支：模組開發與驗證。
- 跨模組 API、資料表與 Router 異動需同步確認，避免破壞其他模組。

## 文件

`docs/` 主要包含：

- 系統需求與設計規範
- API 共通規範
- 模組與資料表責任
- Git 協作流程
- 開發環境與設定規範

競標、資料庫與其他模組亦保留各自的實作／測試文件與 SQL 紀錄。

## 安全注意事項

- 不提交 `.env`、資料庫密碼、SMTP 密碼、API Secret、Token 或第三方服務憑證。
- `node_modules/`、`dist/`、`target/`、本機上傳檔案不應納入版本控制。
- 第三方金流與物流測試環境設定不可直接視為正式環境設定。
- 正式部署前需重新檢視 Session Cookie、CSRF、CORS、第三方 Callback URL 與 Secret 管理。

---
## Copyright & Usage（著作權與使用聲明）

本專案為五人團隊共同開發之學習專題，公開原始碼僅供求職作品集展示及技術審查使用。

專案各部分之著作權由相應權利人保留（All Rights Reserved）。

本 Repository 未提供開源授權。除適用法律或 GitHub 服務條款允許的情況外，未經相關著作權人事先同意，不得重製、修改、重新散布或進行商業利用。
**酷客 Kicks** — 將一般購物、競標交易、活動追蹤與通知整合於同一個球鞋平台。
