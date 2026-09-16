import httpClient from "@/service/httpClient";

// 資料庫存的商品圖片可能是完整外部網址（舊資料），也可能是本地上傳後端回傳的相對路徑（例如 /uploads/products/xxx.webp）。
// 相對路徑要另外組上後端主機位址，瀏覽器才抓得到圖片。
export function resolveProductImageUrl(imageUrl) {
  if (!imageUrl) return "";
  if (/^https?:\/\//i.test(imageUrl)) return imageUrl;

  const apiBaseUrl = httpClient.defaults.baseURL || "";
  const backendBaseUrl = apiBaseUrl.replace(/\/api\/?$/, "").replace(/\/$/, "");
  return backendBaseUrl + (imageUrl.startsWith("/") ? "" : "/") + imageUrl;
}