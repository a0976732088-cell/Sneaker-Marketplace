import httpClient from "@/service/httpClient";

export async function createAdvertisement(payload) {
  const response = await httpClient.post("/admin/ads/add", payload);
  return response.data;
}
