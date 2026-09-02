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