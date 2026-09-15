import { parseAuctionTime } from "./auctionTime.js";

const EVENT_ORDER = {
  AUCTION_CREATED: 10,
  AUCTION_STARTED: 20,
  FIRST_BID: 30,
  LAST_BID: 40,
  AUCTION_REMOVED: 45,
  AUCTION_ENDED: 50,
  RESULT_CREATED: 60,
  ORDER_CREATED: 70,
  PAYMENT_DUE: 80,
  ORDER_PAID: 90,
  ORDER_ABANDONED: 91,
  ORDER_SHIPPED: 100,
  ORDER_COMPLETED: 110,
  AUCTION_DELETED: 120,
};

const RESULT_LABELS = {
  SOLD: "競標成交",
  UNSOLD: "競標流標",
  CANCELLED: "競標取消",
  REMOVED: "競標廢標",
};

function timestamp(value) {
  const parsed = parseAuctionTime(value);
  return parsed.getTime();
}

/**
 * 僅用管理員詳情 DTO 內已持久化的時間推導顯示事件。
 * 不使用瀏覽器現在時間，也不以 updatedAt 猜測業務事件。
 */
export function buildAdminAuctionTimeline(detail) {
  if (!detail) return [];

  const auction = detail.auction || {};
  const order = detail.order;
  const result = detail.result;
  const events = [];
  let insertionOrder = 0;

  function add(type, label, occurredAt, metadata = {}) {
    const sortAt = timestamp(occurredAt);
    if (!occurredAt || Number.isNaN(sortAt)) return;
    events.push({
      type,
      label,
      occurredAt,
      ...metadata,
      sortAt,
      eventOrder: EVENT_ORDER[type],
      insertionOrder: insertionOrder++,
    });
  }

  add("AUCTION_CREATED", "競標建立", auction.createdAt);
  add("AUCTION_STARTED", "排定開始", auction.startAt);

  const bids = (Array.isArray(detail.bids) ? detail.bids : [])
    .map((bid, sourceOrder) => ({
      bid,
      sourceOrder,
      sortAt: timestamp(bid.createdAt),
    }))
    .filter(({ sortAt }) => !Number.isNaN(sortAt))
    .sort(
      (left, right) =>
        left.sortAt - right.sortAt || right.sourceOrder - left.sourceOrder,
    );

  if (bids.length > 0) {
    const first = bids[0].bid;
    add("FIRST_BID", "首次出價", first.createdAt, {
      amount: first.bidAmount,
      actorName: first.bidderName,
    });
  }
  if (bids.length > 1) {
    const last = bids[bids.length - 1].bid;
    add("LAST_BID", "最後出價", last.createdAt, {
      amount: last.bidAmount,
      actorName: last.bidderName,
    });
  }

  add("AUCTION_REMOVED", "管理員下架", auction.removedAt);
  add("AUCTION_ENDED", "競標排定截止", auction.endAt);

  if (result) {
    add(
      "RESULT_CREATED",
      RESULT_LABELS[result.resultStatus] || "競標結果建立",
      result.endedAt || result.createdAt,
      {
        amount: result.finalPrice,
        actorName: result.winnerName,
        resultStatus: result.resultStatus,
      },
    );
  }

  if (order) {
    add("ORDER_CREATED", "訂單建立，進入等待付款", order.createdAt, {
      orderNumber: order.orderNumber,
    });
    add("PAYMENT_DUE", "付款期限", order.paymentDueAt);
    add("ORDER_PAID", "買家付款", order.paidAt);
    add("ORDER_ABANDONED", "買家逾期未付款，訂單已棄標", order.abandonedAt);
    add("ORDER_SHIPPED", "賣家出貨", order.shippedAt);
    add("ORDER_COMPLETED", "訂單完成", order.completedAt);
  }

  add("AUCTION_DELETED", "管理員刪除", auction.deletedAt);

  return events
    .sort(
      (left, right) =>
        left.sortAt - right.sortAt ||
        left.eventOrder - right.eventOrder ||
        left.insertionOrder - right.insertionOrder,
    )
    .map(({ sortAt, eventOrder, insertionOrder: ignored, ...event }) => event);
}
