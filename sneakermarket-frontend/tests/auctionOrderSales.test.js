import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";
import { computed, ref } from "vue";
import { formatAuctionOrderStatus } from "../src/utils/auctionMemberStatus.js";

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
    formatAuctionOrderStatus,
  };
  vm.runInNewContext(
    viewScript +
      "\nglobalThis.subject = { activeTab, isBuyerOrders, isSellerOrders, activeEmptyMessage, activeErrorMessage, selectTab, fetchActiveOrders, sellerPaymentText, sellerPaymentBadgeClass };",
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

test("seller payment statuses use seller-specific wording and safe fallbacks", () => {
  const h = viewHarness();
  assert.equal(
    h.sellerPaymentText({ paymentStatus: "PENDING", orderStatus: "PENDING_PAYMENT" }),
    "等待買家付款",
  );
  assert.equal(h.sellerPaymentBadgeClass({ paymentStatus: "PENDING", orderStatus: "PENDING_PAYMENT" }), "text-bg-warning");
  assert.equal(h.sellerPaymentText({ paymentStatus: "PAID", orderStatus: "PAID" }), "買家已付款");
  assert.equal(h.sellerPaymentBadgeClass({ paymentStatus: "PAID", orderStatus: "PAID" }), "text-bg-success");
  assert.equal(h.sellerPaymentText({ paymentStatus: "FAILED", orderStatus: "PENDING_PAYMENT" }), "付款失敗");
  assert.equal(
    h.sellerPaymentText({ paymentStatus: "PENDING", orderStatus: "ABANDONED" }),
    "買家逾期未付款",
  );
  assert.equal(
    h.sellerPaymentBadgeClass({ paymentStatus: "PENDING", orderStatus: "ABANDONED" }),
    "text-bg-danger",
  );
  assert.equal(h.sellerPaymentText({ paymentStatus: "UNKNOWN", orderStatus: "UNKNOWN" }), "UNKNOWN");
});

test("seller cards show summary fields without buyer payment or detail actions", () => {
  const sellerStart = viewSource.indexOf('<div v-else class="row g-3">');
  const sellerEnd = viewSource.indexOf("</template>", sellerStart);
  const sellerTemplate = viewSource.slice(sellerStart, sellerEnd);
  for (const label of ["商品名稱", "訂單編號", "競標編號", "成交價格", "付款方式", "付款狀態", "訂單狀態", "建立時間"]) {
    if (label === "商品名稱") {
      assert.ok(sellerTemplate.includes("order.productName"));
    } else {
      assert.ok(sellerTemplate.includes(label));
    }
  }
  assert.equal(sellerTemplate.includes("前往付款"), false);
  assert.equal(sellerTemplate.includes("立即付款"), false);
  assert.equal(sellerTemplate.includes("查看訂單"), false);
  assert.equal(sellerTemplate.includes("auction-order-payment"), false);
});

test("buyer cards preserve payment and order detail entries", () => {
  const sellerStart = viewSource.indexOf('<div v-else class="row g-3">');
  const buyerTemplate = viewSource.slice(0, sellerStart);
  assert.ok(buyerTemplate.includes("查看競標"));
  assert.ok(buyerTemplate.includes("前往付款"));
  assert.ok(buyerTemplate.includes("查看訂單"));
  assert.ok(buyerTemplate.includes("auction-order-payment"));
  assert.ok(buyerTemplate.includes("order.paymentStatus === 'PENDING' && order.orderStatus === 'PENDING_PAYMENT'"));
});

test("role changes remount pagination at page zero without sharing rows", () => {
  assert.ok(viewSource.includes(':key="activeTab"'));
  assert.ok(viewSource.includes(':fetch-page="fetchActiveOrders"'));
  const h = viewHarness();
  h.selectTab("seller");
  assert.equal(h.activeTab.value, "seller");
  h.selectTab("buyer");
  assert.equal(h.activeTab.value, "buyer");
});

test("seller empty, loading, error, and auth handling reuse the member page contract", async () => {
  const memberPage = await readFile(
    new URL("../src/components/auction/AuctionMemberPage.vue", import.meta.url),
    "utf8",
  );
  const h = viewHarness();
  h.selectTab("seller");
  assert.equal(h.activeEmptyMessage.value, "目前沒有賣出的競標訂單");
  assert.equal(h.activeErrorMessage.value, "賣出訂單載入失敗，請稍後再試。");
  assert.ok(memberPage.includes('v-if="loading"'));
  assert.ok(memberPage.includes("props.errorMessageFallback"));
  assert.ok(memberPage.includes("handleAuctionAccessError(error, router)"));
});
