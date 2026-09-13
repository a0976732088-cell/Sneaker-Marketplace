import httpClient from "./httpClient";

function compactParams(params = {}) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== "" && value !== null && value !== undefined),
  );
}

export function getAdminAuctions(params = {}) {
  return httpClient.get("/admin/auctions", { params: compactParams(params) });
}

export function getAdminAuctionDetail(auctionId) {
  return httpClient.get("/admin/auctions/" + auctionId);
}

export function removeAdminAuction(auctionId, reason) {
  return httpClient.put(`/admin/auctions/${auctionId}/remove`, { reason });
}

export function restoreAdminAuction(auctionId) {
  return httpClient.put(`/admin/auctions/${auctionId}/restore`);
}

export function deleteAdminAuction(auctionId, reason) {
  return httpClient.put(`/admin/auctions/${auctionId}/delete`, { reason });
}

export function removeAdminAuctionsBatch(auctionIds, reason) {
  return httpClient.put("/admin/auctions/batch/remove", { auctionIds, reason });
}

export function restoreAdminAuctionsBatch(auctionIds) {
  return httpClient.put("/admin/auctions/batch/restore", { auctionIds });
}

export function deleteAdminAuctionsBatch(auctionIds, reason) {
  return httpClient.put("/admin/auctions/batch/delete", { auctionIds, reason });
}
