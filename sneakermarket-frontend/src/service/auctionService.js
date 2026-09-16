import httpClient from "./httpClient";

/** 我的資料只允許 page / size，身分完全由 Session 決定。 */
export function getMyBids({ page = 0, size = 10 } = {}) {
  return httpClient.get("/auctions/me/bids", { params: { page, size } });
}

export function getMyListings({ page = 0, size = 10 } = {}) {
  return httpClient.get("/auctions/me/listings", { params: { page, size } });
}

export function getMyAuctionOrders({ page = 0, size = 10 } = {}) {
  return httpClient.get("/auction-orders/me", { params: { page, size } });
}

export function getMyAuctionSales({ page = 0, size = 10 } = {}) {
  return httpClient.get("/auction-orders/me/sales", { params: { page, size } });
}

/** 單筆競標訂單由後端 Session 驗證 buyer 身分。 */
export function getAuctionOrderDetail(orderNumber) {
  return httpClient.get("/auction-orders/" + encodeURIComponent(orderNumber));
}

/** 付款內容完全由後端既有訂單決定，不傳 Request Body。 */
export function payAuctionOrder(orderNumber) {
  return httpClient.post(
    "/auction-orders/" + encodeURIComponent(orderNumber) + "/pay",
  );
}


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
 * 查詢指定競標的最新公開狀態。
 *
 * API：
 * GET /api/auctions/{auctionId}/status
 *
 * @param {number|string} auctionId 競標 ID
 * @returns {Promise} API Response
 */
export function getAuctionStatus(auctionId) {
  return httpClient.get(`/auctions/${auctionId}/status`);
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
 * 對指定競標出價。
 *
 * bidderId 由後端 Session 取得，payload 只包含 bidAmount。
 *
 * @param {number|string} auctionId 競標 ID
 * @param {{ bidAmount: string|number }} payload 出價金額
 * @returns {Promise} API Response
 */
export function placeBid(auctionId, payload) {
  return httpClient.post(`/auctions/${auctionId}/bids`, {
    bidAmount: payload.bidAmount,
  });
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

/**
 * 修改指定競標。
 *
 * @param {number|string} auctionId 競標 ID
 * @param {Object} request 修改競標資料
 * @returns {Promise} Axios Response
 */
export const updateAuction = (auctionId, request) => {
  return httpClient.put(`/auctions/${auctionId}`, request);
};

/**
 * 取消指定競標。
 *
 * 實際後端不會 DELETE 資料，
 * 而是將競標狀態改為 CANCELLED。
 *
 * @param {number|string} auctionId 競標 ID
 * @returns {Promise} Axios Response
 */
export const cancelAuction = (auctionId) => {
  return httpClient.delete(`/auctions/${auctionId}`);
};

/** 選項由競標後端唯讀引用共用品牌／分類，沒有 Seed ID 假設。 */
export function getAuctionOptions() {
  return httpClient.get("/auctions/options");
}

/** 與競標寫入使用相同 baseURL 及 Cookie，不複製會員登入。 */
export function requireAuctionSession() {
  return httpClient.get("/auctions/session");
}

/** 僅供競標頁使用，不更動全站路由或共用 HTTP interceptor。 */
export async function handleAuctionAccessError(error, router) {
  if (error.response?.status === 401) {
    await router.replace({ name: "login" });
    return true;
  }
  if (error.response?.status === 403) {
    await router.replace({ name: "forbidden" });
    return true;
  }
  return false;
}
