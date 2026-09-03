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
