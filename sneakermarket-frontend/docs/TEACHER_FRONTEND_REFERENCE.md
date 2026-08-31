# 課堂前端專案參考差異

本 BASE 參考 `front_stu_vue202608`：

保留：
- Vue 3 + Vite
- Vue Router
- Axios
- Bootstrap
- VueUse
- Vite 5173 Port
- `@` 對應 `src` alias
- Composition API / `<script setup>`

調整：
- 專案名稱改為 `sneakermarket-frontend`
- Axios 從 `main.js` 移至 `src/service/httpClient.js`
- Backend Base URL 改為 `VITE_API_BASE_URL=http://localhost:8080/api`
- 導覽列改成 SneakerMarket 五大模組
- Bootstrap JS 改用 bundle，確保 Navbar collapse 所需功能完整
- 新增 `/system-check` 呼叫 Backend `/api/health`
- 依五人專題拆分 views 資料夾
