import httpClient from "./httpClient";

// =====================================================
// 查詢某會員的一般商城訂單列表
//
// GET /api/orders/me?memberId=xxx
// =====================================================
export function getMyOrders(memberId) {
  return httpClient.get("/orders/me", {
    params: {
      memberId,
    },
  });
}

// =====================================================
// 查詢單筆訂單明細
//
// GET /api/orders/{orderId}
// =====================================================
export function getOrderDetail(orderId) {
  return httpClient.get(`/orders/${orderId}`);
}
