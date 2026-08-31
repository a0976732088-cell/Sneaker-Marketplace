import httpClient from '@/service/httpClient'

// 對應 sneakermarket-backend 的 GET /api/health
export async function getBackendHealth() {
  const response = await httpClient.get('/health')
  return response.data
}
