import httpClient from "@/service/httpClient";

export async function createAdvertisement(payload) {
  const response = await httpClient.post("/admin/ads/add", payload);
  return response.data;
}

export async function getAllAdvertisements() {
  const response = await httpClient.get("/admin/ads/findAll");
  return response.data;
}

export async function deleteAdvertisement(adId) {
  const response = await httpClient.delete(`/admin/ads/delete/${adId}`);
  return response.data;
}

export async function updateAdvertisement(adId, payload) {
  const response = await httpClient.put(`/admin/ads/put/${adId}`, payload);
  return response.data;
}

export async function getAdvertisementsByPage(pageNumber = 0) {
  const response = await httpClient.get("/admin/ads/findPage", {
    // Axios 會將 params 轉成 ?pageNumber=0 的查詢參數
    params: { pageNumber },
  });

  return response.data;
}
