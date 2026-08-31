import axios from 'axios'

// Vite 的自訂環境變數必須以 VITE_ 開頭。
// 若沒有另外建立 .env.local，預設直接連到目前 SneakerMarket Backend。
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

const httpClient = axios.create({
  baseURL: apiBaseUrl,
  timeout: 10000,

  // 老師範例使用跨域 Cookie，因此 BASE 先保留。
  // 未來若會員系統完全改採 Authorization Bearer JWT，仍可再依登入架構調整。
  withCredentials: true,
})

// 預留：會員組完成 JWT 後，可在這裡統一加入 Authorization Header。
httpClient.interceptors.request.use((config) => {
  return config
})

// 統一先把錯誤原樣交給各 View / Service 處理。
httpClient.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(error),
)

export default httpClient
