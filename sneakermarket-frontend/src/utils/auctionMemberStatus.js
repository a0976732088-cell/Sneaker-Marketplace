export function formatMemberAuctionStatus(status) {
  return {
    DRAFT: "草稿", SCHEDULED: "尚未開始", ACTIVE: "競標中",
    SOLD: "已成交", UNSOLD: "流標", CANCELLED: "已取消", REMOVED: "已下架",
  }[status] ?? status ?? "尚未結標";
}

export function formatMyBidOutcome(bid) {
  if (bid.resultStatus === "SOLD") return bid.won ? "已得標" : "未得標";
  if (bid.status === "ACTIVE") return bid.leading ? "目前領先" : "已被超價";
  return formatMemberAuctionStatus(bid.resultStatus ?? bid.status);
}

export function formatAuctionOrderStatus(status) {
  return {
    PENDING: "待付款", PAID: "已付款", FAILED: "付款失敗",
    NOT_SHIPPED: "未出貨", SHIPPED: "已出貨", DELIVERED: "已送達",
    PENDING_PAYMENT: "待付款", COMPLETED: "已完成", CANCELLED: "已取消",
  }[status] ?? status ?? "-";
}
