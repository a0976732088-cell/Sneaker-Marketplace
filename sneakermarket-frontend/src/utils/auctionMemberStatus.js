import { parseAuctionTime } from "./auctionTime.js";

export function formatMemberAuctionStatus(status) {
  return (
    {
      DRAFT: "草稿",
      SCHEDULED: "尚未開始",
      ACTIVE: "競標中",
      SOLD: "已結標",
      UNSOLD: "流標",
      CANCELLED: "已取消",
      REMOVED: "廢標",
    }[status] ??
    status ??
    "尚未結標"
  );
}

export function formatMemberManagementState(state) {
  return (
    {
      VISIBLE: "公開中",
      REMOVED: "已由平台下架",
      DELETED: "已由平台刪除",
    }[state] ??
    state ??
    "-"
  );
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
  if (
    (bid.managementState ?? "VISIBLE") === "VISIBLE" &&
    bid.status === "ACTIVE"
  ) {
    return bid.leading ? "目前領先" : "已被超價";
  }
  return formatMemberAuctionStatus(bid.resultStatus ?? bid.status);
}

export function getMyBidStatusTone(bid) {
  const status = formatMyBidOutcome(bid);

  if (status === "目前領先" || status === "已得標") return "positive";
  if (status === "已被超價" || status === "尚未開始") return "warning";
  if (["已由平台刪除", "已由平台下架", "廢標", "已取消"].includes(status)) {
    return "danger";
  }

  return "neutral";
}

export function formatMyListingStatus(listing) {
  if (listing.managementState === "DELETED") return "已刪除";
  if (listing.resultStatus === "REMOVED") return "廢標";
  if (listing.managementState === "REMOVED") return "已下架";

  const status = listing.resultStatus ?? listing.status;

  if (status === "REMOVED") return "廢標";
  if (status === "DRAFT" || status === "SCHEDULED") return "未開始";

  return formatMemberAuctionStatus(status);
}

export function getMyListingStatusTone(listing) {
  const status = formatMyListingStatus(listing);

  if (status === "競標中" || status === "已結標") return "positive";
  if (status === "未開始") return "warning";
  if (["已取消", "已下架", "已刪除", "廢標"].includes(status)) return "danger";

  return "neutral";
}

export function formatBuyerAuctionOrderStatus(order) {
  if (order.orderStatus === "ABANDONED") return "已棄標";
  if (order.orderStatus === "PENDING_PAYMENT") return "等待付款";
  if (["PAID", "SHIPPED", "COMPLETED"].includes(order.orderStatus)) {
    return "已付款";
  }

  return "訂單狀態更新中";
}

export function getBuyerAuctionOrderStatusTone(order) {
  const status = formatBuyerAuctionOrderStatus(order);

  if (status === "已付款") return "positive";
  if (status === "已棄標") return "danger";
  if (status === "等待付款") return "warning";
  return "neutral";
}

export function formatSellerAuctionOrderStatus(order) {
  if (order.orderStatus === "ABANDONED") {
    return "買家逾期未付款";
  }
  if (order.orderStatus === "PENDING_PAYMENT") return "等待買家付款";
  if (["PAID", "SHIPPED", "COMPLETED"].includes(order.orderStatus)) {
    return "買家已付款";
  }

  return "訂單狀態更新中";
}

export function getSellerAuctionOrderStatusTone(order) {
  const status = formatSellerAuctionOrderStatus(order);

  if (status === "買家已付款") return "positive";
  if (status === "買家逾期未付款") return "danger";
  if (status === "等待買家付款") return "warning";
  return "neutral";
}

export function canPayAuctionOrder(order) {
  if (
    order.paymentStatus !== "PENDING" ||
    order.orderStatus !== "PENDING_PAYMENT"
  ) {
    return false;
  }

  if (!order.paymentDueAt) return true;

  const paymentDueAt = parseAuctionTime(order.paymentDueAt).getTime();
  return !Number.isFinite(paymentDueAt) || Date.now() < paymentDueAt;
}

export function formatAuctionOrderStatus(status) {
  return (
    {
      PENDING: "等待付款",
      PAID: "已付款",
      FAILED: "付款失敗",
      NOT_SHIPPED: "未出貨",
      SHIPPED: "已出貨",
      DELIVERED: "已送達",
      PENDING_PAYMENT: "等待付款",
      COMPLETED: "已完成",
      CANCELLED: "已取消",
      ABANDONED: "已棄標",
    }[status] ??
    status ??
    "-"
  );
}

export function formatAuctionResultStatus(status) {
  return (
    {
      SOLD: "成交",
      UNSOLD: "流標",
      CANCELLED: "已取消",
      REMOVED: "廢標",
    }[status] ??
    status ??
    "-"
  );
}
