# REST API BASE 規則

- 所有後端 API 以 `/api` 為共同前綴。
- 路徑使用名詞複數，不使用 `/getXXX`、`/findXXX` 這類動詞式 URL。
- Controller 不直接存取 Repository；流程維持 Controller → Service → Repository。
- API 原則上回傳 DTO，不直接把 JPA Entity 當公開 API 契約。
- 共用回應外框使用 `ApiResponse<T>`。
- 例外交由 `GlobalExceptionHandler` 統一處理。
- 前端開發環境預設為 `http://localhost:5173`。

建議模組前綴：

- `/api/auth/**`, `/api/members/**`
- `/api/products/**`, `/api/cart/**`, `/api/orders/**`
- `/api/ads/**`
- `/api/calendar/**`, `/api/notifications/**`
- `/api/auctions/**`, `/api/auction-orders/**`
- `/api/admin/**`
