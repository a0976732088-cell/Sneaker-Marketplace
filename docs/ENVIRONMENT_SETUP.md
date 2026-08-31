# SneakerMarket 環境設定交付規範

## 原則

真正的密碼與 Secret 不透過 Git 分享。

Git 保存：

```text
.env.example
application.properties
application-local.properties
application-production.properties
```

前提是上述檔案不得寫入真實 Secret。

Git 不保存：

```text
.env
.env.local
.env.*.local
DB 密碼
SMTP 密碼
JWT Secret
其他私密 API Secret
```

## Backend

後端目前使用環境變數，例如：

```text
DB_USERNAME
DB_PASSWORD
DB_URL
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
```

每位開發者 Clone 後，依 `sneakermarket-backend/.env.example` 建立自己的本機設定。

若使用 IntelliJ，可在 Run Configuration → Environment variables 設定。

## Frontend

Vite 可讀取 `VITE_` 開頭的前端環境變數，例如：

```text
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_NAME=SneakerMarket
```

前端環境變數會進入瀏覽器端程式，因此不得放：

```text
資料庫密碼
JWT 簽章 Secret
SMTP 密碼
其他後端私密憑證
```

## 組員 Clone 後

### Frontend

```bash
cd sneakermarket-frontend
npm install
npm run dev
```

### Backend

1. 建立 `SneakerMarketDB`。
2. 設定自己的 DB 帳密。
3. 執行 Spring Boot。
4. 以 `/api/health` 驗證服務。
