# SneakerMarket Frontend

SneakerMarket 球鞋電子商城＋競標平台的 Vue 前端 BASE 專案。

## 技術

- Vue 3
- Vite
- Vue Router
- Axios
- Bootstrap 5
- VueUse

本專案參考課堂前端專案的技術與寫法，並對應 `sneakermarket-backend` 的 REST API 架構。

## 執行

```bash
npm install
npm run dev
```

預設網址：

```text
http://localhost:5173
```

後端預設網址：

```text
http://localhost:8080
```

## 後端 API 設定

複製 `.env.example` 為 `.env.local`：

```text
VITE_API_BASE_URL=http://localhost:8080/api
```

如果沒有建立 `.env.local`，`src/service/httpClient.js` 也會預設使用上面的網址。

## 驗證前後端是否接通

1. 啟動 `sneakermarket-backend`。
2. 確認後端 `http://localhost:8080/api/health` 可正常回應。
3. 啟動 Vue：`npm run dev`。

## 目錄

```text
src/
├── assets/
│   └── css/
├── components/
│   ├── common/
│   └── layout/
├── router/
├── service/
└── views/
    ├── member/
    ├── commerce/
    ├── advertisement/
    ├── calendar/
    ├── notification/
    ├── auction/
    └── admin/
```

## 團隊規則

- 共用導覽列、Footer 與全域 CSS 不要由各組自行複製。
- REST API 一律透過 `src/service/httpClient.js`。
- 各模組主要修改自己的 `views/<module>` 與對應 service。
- 不要在 View 裡硬寫 `http://localhost:8080`。
- `.env.local` 不提交 Git。
