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

export async function getAdvertisementsByPage(pageNumber = 0, adStatus = null) {
  const response = await httpClient.get("/admin/ads/findPage", {
    // adStatus 沒有值時查詢全部；有值時由後端先篩選狀態，再進行分頁。
    params: {
      pageNumber,
      adStatus: adStatus || undefined,
    },
  });
  return response.data;
}

export async function updateAdvertisementStatus(adId, adStatus) {
  const response = await httpClient.put(`/admin/ads/put/${adId}/status`, {adStatus});

  return response.data;
}
