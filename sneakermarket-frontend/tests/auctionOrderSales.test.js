import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";
import { computed, ref } from "vue";
import {
  canPayAuctionOrder,
  formatBuyerAuctionOrderStatus,
  formatSellerAuctionOrderStatus,
  getBuyerAuctionOrderStatusTone,
  getSellerAuctionOrderStatusTone,
} from "../src/utils/auctionMemberStatus.js";

const serviceSource = (await readFile(new URL("../src/service/auctionService.js", import.meta.url), "utf8"))
  .replace(/^import[^\n]+\n/gm, "")
  .replace(/^export /gm, "");
const serviceContext = {
  httpClient: {
    get: (url, config) => ({ url, config }),
    post: () => {},
    put: () => {},
    delete: () => {},
  },
};
vm.runInNewContext(
  serviceSource + "\nglobalThis.service = { getMyAuctionOrders, getMyAuctionSales };",
  serviceContext,
);

const viewSource = await readFile(
  new URL("../src/views/auction/AuctionOrderListView.vue", import.meta.url),
  "utf8",
);
const viewScript = viewSource.match(/<script setup>([\s\S]*?)<\/script>/)[1]
  .replace(/^import[^\n]+\n/gm, "");

const buyerStart = viewSource.indexOf('<div v-if="isBuyerOrders" class="auction-record-list">');
const sellerStart = viewSource.indexOf('<div v-else class="auction-record-list">', buyerStart);
const recordsEnd = viewSource.lastIndexOf("</AuctionMemberPage>");
assert.notEqual(buyerStart, -1, "buyer order list section");
assert.notEqual(sellerStart, -1, "seller order list section");
assert.notEqual(recordsEnd, -1, "order member page boundary");
const buyerTemplate = viewSource.slice(buyerStart, sellerStart);
const sellerTemplate = viewSource.slice(sellerStart, recordsEnd);

function viewHarness() {
  const calls = [];
  const context = {
    computed,
    ref,
    getMyAuctionOrders: (params) => {
      calls.push({ role: "buyer", params });
      return "buyer-response";
    },
    getMyAuctionSales: (params) => {
      calls.push({ role: "seller", params });
      return "seller-response";
    },
  };
  vm.runInNewContext(
    viewScript +
      "\nglobalThis.subject = { activeTab, isBuyerOrders, isSellerOrders, activeEmptyTitle, activeEmptyMessage, activeErrorMessage, selectTab, fetchActiveOrders, canViewPublicAuction };",
    context,
  );
  return { ...context.subject, calls };
}

test("getMyAuctionSales uses the shared client and sends only page and size", () => {
  const request = serviceContext.service.getMyAuctionSales({
    page: 3,
    size: 20,
    sellerId: 99,
    memberId: 88,
    buyerId: 77,
  });
  assert.equal(request.url, "/auction-orders/me/sales");
  assert.deepEqual(JSON.parse(JSON.stringify(request.config.params)), { page: 3, size: 20 });
  assert.deepEqual(
    JSON.parse(JSON.stringify(serviceContext.service.getMyAuctionSales().config.params)),
    { page: 0, size: 10 },
  );
});

test("buyer orders are the default and seller orders load only after switching tabs", () => {
  const h = viewHarness();
  assert.equal(h.activeTab.value, "buyer");
  assert.equal(h.isBuyerOrders.value, true);
  assert.equal(h.isSellerOrders.value, false);
  assert.equal(h.calls.length, 0);

  assert.equal(h.fetchActiveOrders({ page: 0, size: 10 }), "buyer-response");
  assert.deepEqual(h.calls.map((call) => call.role), ["buyer"]);

  h.selectTab("seller");
  assert.equal(h.isSellerOrders.value, true);
  assert.equal(h.fetchActiveOrders({ page: 0, size: 10 }), "seller-response");
  assert.deepEqual(h.calls.map((call) => call.role), ["buyer", "seller"]);
});

test("buyer order exposes only pending paid and abandoned customer statuses", () => {
  const pending = { paymentStatus: "PENDING", orderStatus: "PENDING_PAYMENT" };
  const paid = { paymentStatus: "PAID", orderStatus: "PAID" };
  const abandoned = { paymentStatus: "PENDING", orderStatus: "ABANDONED" };
  const cancelled = { paymentStatus: "PENDING", orderStatus: "CANCELLED" };

  assert.equal(formatBuyerAuctionOrderStatus(pending), "等待付款");
  assert.equal(getBuyerAuctionOrderStatusTone(pending), "warning");
  assert.equal(canPayAuctionOrder(pending), true);
  assert.equal(formatBuyerAuctionOrderStatus(paid), "已付款");
  assert.equal(getBuyerAuctionOrderStatusTone(paid), "positive");
  assert.equal(canPayAuctionOrder(paid), false);
  assert.equal(formatBuyerAuctionOrderStatus(abandoned), "已棄標");
  assert.equal(getBuyerAuctionOrderStatusTone(abandoned), "danger");
  assert.equal(canPayAuctionOrder(abandoned), false);
  assert.equal(formatBuyerAuctionOrderStatus(cancelled), "訂單狀態更新中");
  assert.equal(getBuyerAuctionOrderStatusTone(cancelled), "neutral");
  assert.equal(canPayAuctionOrder({ paymentStatus: "FAILED", orderStatus: "PENDING_PAYMENT" }), false);
});

test("buyer payment action requires an unexpired deadline", () => {
  const now = Date.now();
  const pending = {
    paymentStatus: "PENDING",
    orderStatus: "PENDING_PAYMENT",
  };

  assert.equal(
    canPayAuctionOrder({ ...pending, paymentDueAt: new Date(now + 60_000).toISOString() }),
    true,
  );
  assert.equal(
    canPayAuctionOrder({ ...pending, paymentDueAt: new Date(now - 60_000).toISOString() }),
    false,
  );
  assert.equal(
    canPayAuctionOrder({ ...pending, paymentDueAt: new Date(now).toISOString() }),
    false,
  );
  assert.equal(canPayAuctionOrder({ ...pending, paymentDueAt: null }), true);
  assert.equal(canPayAuctionOrder(pending), true);
  assert.equal(canPayAuctionOrder({ ...pending, paymentDueAt: "invalid" }), true);
});

test("seller order exposes the three role-specific customer statuses", () => {
  const pending = { paymentStatus: "PENDING", orderStatus: "PENDING_PAYMENT" };
  const paid = { paymentStatus: "PAID", orderStatus: "PAID" };
  const abandoned = { paymentStatus: "PENDING", orderStatus: "ABANDONED" };
  const cancelled = { paymentStatus: "PENDING", orderStatus: "CANCELLED" };

  assert.equal(formatSellerAuctionOrderStatus(pending), "等待買家付款");
  assert.equal(getSellerAuctionOrderStatusTone(pending), "warning");
  assert.equal(formatSellerAuctionOrderStatus(paid), "買家已付款");
  assert.equal(getSellerAuctionOrderStatusTone(paid), "positive");
  assert.equal(formatSellerAuctionOrderStatus(abandoned), "買家逾期未付款");
  assert.equal(getSellerAuctionOrderStatusTone(abandoned), "danger");
  assert.equal(formatSellerAuctionOrderStatus(cancelled), "訂單狀態更新中");
  assert.equal(getSellerAuctionOrderStatusTone(cancelled), "neutral");
});

test("buyer order rows use commerce summary fields, deadline and pending-only payment action", () => {
  for (const binding of [
    ':image-url="order.imageUrl"',
    "order.productName",
    "formatProductMeta(order)",
    "formatTwd(order.finalPrice)",
    "formatBuyerAuctionOrderStatus(order)",
    "getBuyerAuctionOrderStatusTone(order)",
  ]) {
    assert.ok(buyerTemplate.includes(binding), binding);
  }

  assert.match(
    buyerTemplate,
    /v-if="canPayAuctionOrder\(order\) && order\.paymentDueAt"[\s\S]*?請於 \{\{ formatAuctionTime\(order\.paymentDueAt\) \}\} 前完成付款/,
  );
  assert.match(buyerTemplate, /v-if="canPayAuctionOrder\(order\)"[\s\S]*?>\s*立即付款\s*</);
  assert.match(buyerTemplate, /v-else[\s\S]*?>\s*查看訂單\s*</);
  assert.match(buyerTemplate, /name: 'auction-detail'/);
  assert.match(buyerTemplate, /v-if="canPayAuctionOrder\(order\)"[\s\S]*?name: 'auction-order-checkout'/);
  assert.match(buyerTemplate, /v-else[\s\S]*?name: 'auction-order-detail'/);
});

test("buyer public auction action is limited to visible records without affecting payment", () => {
  const h = viewHarness();
  const pendingOrder = {
    paymentStatus: "PENDING",
    orderStatus: "PENDING_PAYMENT",
  };

  assert.equal(h.canViewPublicAuction({ managementState: "VISIBLE" }), true);
  assert.equal(h.canViewPublicAuction({ managementState: "REMOVED" }), false);
  assert.equal(h.canViewPublicAuction({ managementState: "DELETED" }), false);
  assert.equal(canPayAuctionOrder({ ...pendingOrder, managementState: "VISIBLE" }), true);
  assert.equal(canPayAuctionOrder({ ...pendingOrder, managementState: "REMOVED" }), true);
  assert.equal(canPayAuctionOrder({ ...pendingOrder, managementState: "DELETED" }), true);

  assert.match(
    buyerTemplate,
    /v-if="canViewPublicAuction\(order\)"[\s\S]*?name: 'auction-detail'/,
  );
  assert.match(
    buyerTemplate,
    /v-else class="record-action-notice"[\s\S]*?formatMemberPublicDetailNotice\(order\.managementState\)/,
  );
});

test("member order list does not present the dormant logistics workflow", () => {
  for (const forbidden of ["shippingStatus", "物流狀態", "未出貨", "已出貨", "已送達"]) {
    assert.equal(viewSource.includes(forbidden), false, forbidden);
  }
});

test("seller order rows show summary, status and a read-only detail action", () => {
  for (const binding of [
    ':image-url="order.imageUrl"',
    "order.productName",
    "formatProductMeta(order)",
    "formatTwd(order.finalPrice)",
    "formatSellerAuctionOrderStatus(order)",
    "getSellerAuctionOrderStatusTone(order)",
  ]) {
    assert.ok(sellerTemplate.includes(binding), binding);
  }

  assert.match(sellerTemplate, /#actions/);
  assert.match(sellerTemplate, /name: 'auction-order-detail'/);
  assert.match(sellerTemplate, />\s*查看訂單\s*</);

  for (const forbidden of ["立即付款", "auction-order-payment", "auction-order-checkout"]) {
    assert.equal(sellerTemplate.includes(forbidden), false, forbidden);
  }
});

test("buyer and seller selector is an accessible underline tab control", () => {
  assert.match(viewSource, /role="tablist"[^>]*aria-label="競標訂單角色切換"/);
  assert.match(viewSource, /role="tab"[\s\S]*?:aria-selected="isBuyerOrders"/);
  assert.match(viewSource, /role="tab"[\s\S]*?:aria-selected="isSellerOrders"/);
  assert.match(viewSource, /\.order-role-tab\s*\{[\s\S]*?border-bottom:\s*2px solid transparent/);
  assert.match(viewSource, /\.order-role-tab\.is-active\s*\{[\s\S]*?border-bottom-color:\s*#111/);
  assert.doesNotMatch(viewSource, /btn-group|btn-outline-primary/);
});

test("role changes remount pagination at page zero without sharing rows", () => {
  assert.ok(viewSource.includes(':key="activeTab"'));
  assert.ok(viewSource.includes(':fetch-page="fetchActiveOrders"'));
  const h = viewHarness();
  h.selectTab("seller");
  assert.equal(h.activeTab.value, "seller");
  h.selectTab("unsupported");
  assert.equal(h.activeTab.value, "seller");
  h.selectTab("buyer");
  assert.equal(h.activeTab.value, "buyer");
});

test("buyer and seller empty, loading, error and auth states reuse the member page contract", async () => {
  const memberPage = await readFile(
    new URL("../src/components/auction/AuctionMemberPage.vue", import.meta.url),
    "utf8",
  );
  const h = viewHarness();
  assert.equal(h.activeEmptyTitle.value, "目前沒有競標訂單");
  assert.equal(h.activeEmptyMessage.value, "得標後的訂單會顯示在這裡");

  h.selectTab("seller");
  assert.equal(h.activeEmptyTitle.value, "目前沒有賣出的競標訂單");
  assert.equal(h.activeEmptyMessage.value, "競標商品成交後，賣出訂單會顯示在這裡");
  assert.equal(h.activeErrorMessage.value, "賣出訂單載入失敗，請稍後再試。");
  assert.ok(memberPage.includes('v-if="loading"'));
  assert.ok(memberPage.includes("props.errorMessageFallback"));
  assert.ok(memberPage.includes("handleAuctionAccessError(error, router)"));
});
