export function formatMemberAuctionStatus(status) {
  return {
    DRAFT: "草稿", SCHEDULED: "尚未開始", ACTIVE: "競標中",
    SOLD: "已成交", UNSOLD: "流標", CANCELLED: "已取消", REMOVED: "廢標",
  }[status] ?? status ?? "尚未結標";
}

export function formatMemberManagementState(state) {
  return {
    VISIBLE: "公開中", REMOVED: "已由平台下架", DELETED: "已由平台刪除",
  }[state] ?? state ?? "-";
}

export function formatMemberPublicDetailNotice(state) {
  if (state === "DELETED") return "已由平台刪除，公開詳細頁不可查看";
  if (state === "REMOVED") return "已由平台下架，公開詳細頁不可查看";
  return "";
}

export function formatMyBidOutcome(bid) {
  if (bid.managementState === "DELETED") return "已由平台刪除";
  if (bid.resultStatus === "REMOVED" || bid.status === "REMOVED") return "廢標";
  if (bid.managementState === "REMOVED") return "已由平台下架";
  if (bid.resultStatus === "SOLD") return bid.won ? "已得標" : "未得標";
  if ((bid.managementState ?? "VISIBLE") === "VISIBLE" && bid.status === "ACTIVE") {
    return bid.leading ? "目前領先" : "已被超價";
  }
  return formatMemberAuctionStatus(bid.resultStatus ?? bid.status);
}

export function formatAuctionOrderStatus(status) {
  return {
    PENDING: "待付款", PAID: "已付款", FAILED: "付款失敗",
    NOT_SHIPPED: "未出貨", SHIPPED: "已出貨", DELIVERED: "已送達",
    PENDING_PAYMENT: "待付款", COMPLETED: "已完成", CANCELLED: "已取消",
  }[status] ?? status ?? "-";
}
