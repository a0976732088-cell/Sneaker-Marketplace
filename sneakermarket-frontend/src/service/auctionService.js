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
