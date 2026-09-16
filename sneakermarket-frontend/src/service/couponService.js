import httpClient from "./httpClient";

// ============================================
// 查詢自己的全部折價券
// GET /api/coupons/me
// ============================================
export function getMyCoupons() {
    return httpClient.get("/coupons/me");
}


// ============================================
// 查詢目前可使用的折價券
// GET /api/coupons/me/available
// ============================================
export function getAvailableCoupons() {
    return httpClient.get("/coupons/me/available");
}


// ============================================
// 試算折價券
// GET /api/coupons/{couponId}/discount
// ============================================
export function calculateCouponDiscount(
    couponId,
    orderSubtotal
) {
    return httpClient.get(
        `/coupons/${couponId}/discount`,
        {
            params: {
                orderSubtotal,
            },
        }
    );
}