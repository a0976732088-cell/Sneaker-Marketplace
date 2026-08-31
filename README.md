# SneakerMarket

球鞋電子商城與競標平台。此 Repository 採 **Monorepo** 管理，但前端與後端仍是兩個獨立專案。

## 專案模組

1. 會員系統 Member
2. 一般商城 Commerce
3. 廣告系統 Advertisement
4. 行事曆／通知 Calendar & Notification
5. 競標系統 Auction

## Repository 結構

```text
SneakerMarket/
├── sneakermarket-frontend/   # Vue 3 + Vite 前端
├── sneakermarket-backend/    # Spring Boot 4.0.8 後端
├── docs/                     # 全團隊共用規範與需求文件
├── .github/                  # GitHub Pull Request 範本
├── .gitignore
├── .gitattributes
├── .editorconfig
└── README.md
```

> 資料庫 SQL 目前維持在 `sneakermarket-backend/database/`，避免在正式開發前搬動既有路徑。

## 技術基線

### Frontend
- Vue 3
- Vite
- Vue Router
- Axios
- Bootstrap
- 預設開發 Port：`5173`

### Backend
- Java
- Spring Boot `4.0.8`
- Spring Security
- Spring Data JPA / Hibernate
- Maven
- Microsoft SQL Server
- 預設開發 Port：`8080`

### Database
- Microsoft SQL Server
- Database：`SneakerMarketDB`
- Schema：`dbo`

## Frontend 啟動

```bash
cd sneakermarket-frontend
npm install
npm run dev
```

預設網址：

```text
http://localhost:5173
```

前端環境設定請參考：

```text
sneakermarket-frontend/.env.example
```

## Backend 啟動

Windows：

```bat
cd sneakermarket-backend
mvnw.cmd spring-boot:run
```

或直接以 IntelliJ 執行 `SneakerMarketBackendApplication`。

預設網址：

```text
http://localhost:8080
```

後端環境設定請參考：

```text
sneakermarket-backend/.env.example
```

至少需依個人環境設定 MSSQL 帳密，例如：

```text
DB_USERNAME=sa
DB_PASSWORD=<自己的 SQL Server 密碼>
```

敏感資訊不得提交至 Git。

## 前後端連線測試

1. 啟動 Spring Boot Backend。
2. 啟動 Vue Frontend。
3. 瀏覽：

```text
http://localhost:5173/system-check
```

前端會呼叫：

```text
GET http://localhost:8080/api/health
```

## Database

資料庫相關腳本目前位於：

```text
sneakermarket-backend/database/
├── schema/
├── migration/
└── seed/
```

建立資料庫可先參考：

```text
sneakermarket-backend/database/schema/000_create_database.sql
```

## Git Flow

```text
main
└── develop
    ├── feature/member
    ├── feature/commerce
    ├── feature/advertisement
    ├── feature/calendar-notification
    └── feature/auction
```

- `main`：穩定、可展示版本。
- `develop`：整合與測試版本。
- `feature/*`：各模組開發分支。
- 功能完成後以 Pull Request 合併到 `develop`。
- 不直接將未整合完成的功能 Push 到 `main`。

詳細規範請查看 `docs/GIT_FLOW.md`。

## 文件

全團隊共用文件放在：

```text
docs/
```

主要包含：
- 系統需求規格
- 系統設計與實作規範
- API 規範
- 模組與資料表責任
- Git 協作流程
- 環境設定交付規範

## 注意事項

- `node_modules/`、`dist/`、`target/` 不提交 Git。
- `.env`、密碼、SMTP 憑證、JWT Secret 等敏感資訊不提交 Git。
- `.env.example` 必須提交，讓其他組員知道需要哪些設定。
- `package-lock.json`、`pom.xml`、`mvnw`、`mvnw.cmd`、`.mvn/` 必須提交。
- 修改共用 API、資料表、Router 或共用設定前先通知其他組員。
