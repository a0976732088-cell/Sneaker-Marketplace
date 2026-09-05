import httpClient from "./httpClient";

/**
 * 查詢競標列表。
 *
 * @param {Object} params 搜尋、篩選、排序與分頁條件
 * @returns {Promise} API Response
 */
export function getAuctions(params = {}) {
  return httpClient.get("/auctions", {
    params,
  });
}

/**
 * 查詢指定競標的完整詳細資料。
 *
 * API：
 * GET /api/auctions/{auctionId}
 *
 * @param {number|string} auctionId 競標 ID
 * @returns {Promise} API Response
 */
export function getAuctionDetail(auctionId) {
  return httpClient.get(`/auctions/${auctionId}`);
}

/**
 * 查詢指定競標的公開出價紀錄。
 *
 * API：
 * GET /api/auctions/{auctionId}/bids
 *
 * @param {number|string} auctionId 競標 ID
 * @returns {Promise} API Response
 */
export function getBidHistory(auctionId) {
  return httpClient.get(`/auctions/${auctionId}/bids`);
}

/**
 * 建立新的競標。
 *
 * API：
 * POST /api/auctions
 *
 * 後端使用 multipart/form-data 接收：
 * - request：建立競標 JSON
 * - images：商品圖片
 *
 * @param {Object} request 建立競標表單資料
 * @param {File[]} images 商品圖片
 * @returns {Promise} API Response
 */
export function createAuction(request, images) {
  const formData = new FormData();

  /*
   * AuctionCreateRequest 必須以 application/json
   * 的 multipart Part 傳給 Spring @RequestPart("request")。
   */
  const requestBlob = new Blob([JSON.stringify(request)], {
    type: "application/json",
  });

  formData.append("request", requestBlob);

  /*
   * 後端接收 List<MultipartFile> images，
   * 因此每一張圖片都使用同一個 Part 名稱 images。
   */
  images.forEach((image) => {
    formData.append("images", image);
  });

  return httpClient.post("/auctions", formData);
}
