import httpClient from "@/service/httpClient";

/** 競標圖片儲存為 /uploads/auctions/...，瀏覽器需向 API 所在的後端主機取圖。 */
export function resolveAuctionImageUrl(imageUrl) {
  if (!imageUrl) return "";
  if (/^https?:\/\//i.test(imageUrl)) return imageUrl;

  const apiBaseUrl = httpClient.defaults.baseURL || "";
  const backendBaseUrl = apiBaseUrl.replace(/\/api\/?$/, "").replace(/\/$/, "");
  return backendBaseUrl + (imageUrl.startsWith("/") ? "" : "/") + imageUrl;
}
