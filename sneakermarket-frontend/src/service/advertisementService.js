import httpClient from "@/service/httpClient";

export function resolveAdvertisementImageUrl(imageUrl) {
  if (!imageUrl) return "";

  const apiBaseUrl = httpClient.defaults.baseURL || "";
  const backendBaseUrl = apiBaseUrl.replace(/\/api\/?$/, "");

  return `${backendBaseUrl}${imageUrl}`;
}

export async function createAdvertisement(payload) {
  const response = await httpClient.post("/admin/ads/add", payload);
  return response.data;
}

export async function getPublicActiveAdvertisements() {
  const response = await httpClient.get("/admin/ads/findActive");
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

export async function getAdvertisementsByPage(
  pageNumber = 0,
  adStatus = null,
  deliveryStatus = null,
  adPlacement = null,
  keyword = null,
) {
  const response = await httpClient.get("/admin/ads/findPage", {
    // adStatus 沒有值時查詢全部；有值時由後端先篩選狀態，再進行分頁。
    params: {
      pageNumber,
      adStatus: adStatus || undefined,
      deliveryStatus: deliveryStatus || undefined,
      adPlacement: adPlacement || undefined,
      keyword: keyword || undefined,
    },
  });
  return response.data;
}

export async function updateAdvertisementStatus(adId, adStatus) {
  const response = await httpClient.put(`/admin/ads/put/${adId}/status`, {
    adStatus,
  });

  return response.data;
}

export async function uploadAdvertisementImage(file, adPlacement) {
  const formData = new FormData();

  formData.append("file", file);
  formData.append("adPlacement", adPlacement);

  const response = await httpClient.post("/admin/ads/upload-image", formData);

  return response.data;
}

export async function recordAdvertisementView(adId) {
  const response = await httpClient.post(`/ads/${adId}/views`);

  return response.data;
}

export async function recordAdvertisementClick(adId) {
  const response = await httpClient.post(`/ads/${adId}/clicks`);

  return response.data;
}

export async function getAdvertisementStats(adIds) {
  const response = await httpClient.get("/admin/ads/stats", {
    params: {
      adIds: adIds.join(","),
    },
  });

  return response.data;
}

export async function getAdvertisementDashboardSummary() {
  const response = await httpClient.get("/admin/ads/dashboard-summary");

  return response.data;
}
