import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";
import { computed, ref } from "vue";

import {
  formatBuyerAuctionOrderStatus,
  formatSellerAuctionOrderStatus,
  getBuyerAuctionOrderStatusTone,
  getSellerAuctionOrderStatusTone,
} from "../src/utils/auctionMemberStatus.js";
import { formatTwd } from "../src/utils/auctionMoney.js";
import { formatAuctionTime } from "../src/utils/auctionTime.js";
import { AUCTION_PAYMENT_METHOD } from "../src/constants/auctionPayment.js";

const detailSource = await readFile(
  new URL("../src/views/auction/AuctionOrderDetailView.vue", import.meta.url),
  "utf8",
);
const detailScript = detailSource
  .match(/<script setup>([\s\S]*?)<\/script>/)[1]
  .replace(/import\s+[\s\S]*?\s+from\s+["'][^"']+["'];\s*/g, "");

function detail(overrides = {}) {
  return {
    orderNumber: "AO-123456",
    auctionId: 7,
    viewerRole: "BUYER",
    receiverInfoVisible: true,
    productName: "Jordan 1 Retro High",
    brandName: "Nike",
    model: "Retro High",
    size: "US 9",
    imageUrl: "/uploads/auctions/jordan.jpg",
    finalPrice: 6800,
    shippingFee: 100,
    totalAmount: 6900,
    shippingMethod: "宅配",
    paymentMethod: "信用卡",
    paymentStatus: "PAID",
    orderStatus: "PAID",
    receiverName: "王小明",
    receiverPhone: "0912345678",
    shippingAddress: "台北市測試路 1 號",
    createdAt: "2026-09-14T10:00:00",
    paymentDueAt: "2026-09-16T20:00:00",
    paidAt: "2026-09-15T18:00:00",
    abandonedAt: null,
    ...overrides,
  };
}

function detailHarness(responseDetail) {
  let mounted;
  const routes = [];
  const route = { params: { orderNumber: responseDetail.orderNumber } };
  const context = {
    computed,
    ref,
    watch: () => {},
    onMounted: (callback) => {
      mounted = callback;
    },
    onUnmounted: () => {},
    useRoute: () => route,
    useRouter: () => ({ replace: async (target) => routes.push(target) }),
    AUCTION_PAYMENT_METHOD,
    getAuctionOrderDetail: async () => ({ data: { data: responseDetail } }),
    handleAuctionAccessError: async () => false,
    resolveAuctionImageUrl: (value) => (value ? `resolved:${value}` : ""),
    formatBuyerAuctionOrderStatus,
    formatSellerAuctionOrderStatus,
    getBuyerAuctionOrderStatusTone,
    getSellerAuctionOrderStatusTone,
    formatTwd,
    formatAuctionTime,
  };
  vm.runInNewContext(
    detailScript +
      "\nglobalThis.subject = { order, loading, errorMessage, isBuyer, isSeller, pageTitle, " +
      "receiverInfoVisible, statusText, statusTone, productImageUrl, productMeta, paymentMethodLabel, " +
      "buyerAbandoned, sellerPending, sellerAbandoned, receiverPrivacyMessage, loadOrder };",
    context,
  );
  return { ...context.subject, routes, mount: () => mounted() };
}

test("buyer paid detail shows paid status and permits its receiver snapshot", async () => {
  const h = detailHarness(detail());
  await h.mount();
  assert.equal(h.pageTitle.value, "競標訂單明細");
  assert.equal(h.statusText.value, "已付款");
  assert.equal(h.statusTone.value, "positive");
  assert.equal(h.receiverInfoVisible.value, true);
  assert.equal(h.order.value.receiverPhone, "0912345678");
  assert.match(detailSource, /v-if="receiverInfoVisible"/);
  assert.match(detailSource, /order\.paidAt/);
});

test("buyer abandoned detail is read-only and explains that payment cannot resume", async () => {
  const h = detailHarness(
    detail({
      paymentStatus: "PENDING",
      orderStatus: "ABANDONED",
      paidAt: null,
      abandonedAt: "2026-09-16T20:00:05",
    }),
  );
  await h.mount();
  assert.equal(h.statusText.value, "已棄標");
  assert.equal(h.buyerAbandoned.value, true);
  assert.match(detailSource, /已因逾期未付款而棄標，無法再次付款/);
});

test("seller pending detail hides all receiver data behind the visibility contract", async () => {
  const h = detailHarness(
    detail({
      viewerRole: "SELLER",
      receiverInfoVisible: false,
      paymentStatus: "PENDING",
      orderStatus: "PENDING_PAYMENT",
      receiverName: null,
      receiverPhone: null,
      shippingAddress: null,
      paidAt: null,
    }),
  );
  await h.mount();
  assert.equal(h.pageTitle.value, "賣出訂單明細");
  assert.equal(h.statusText.value, "等待買家付款");
  assert.equal(h.receiverInfoVisible.value, false);
  assert.equal(h.receiverPrivacyMessage.value, "買家完成付款後將顯示收件資訊。");
});

test("seller paid detail may render receiver data for fulfillment", async () => {
  const h = detailHarness(detail({ viewerRole: "SELLER", receiverInfoVisible: true }));
  await h.mount();
  assert.equal(h.statusText.value, "買家已付款");
  assert.equal(h.receiverInfoVisible.value, true);
  assert.equal(h.order.value.shippingAddress, "台北市測試路 1 號");
});

test("seller abandoned detail keeps receiver private and needs no fulfillment", async () => {
  const h = detailHarness(
    detail({
      viewerRole: "SELLER",
      receiverInfoVisible: false,
      paymentStatus: "PENDING",
      orderStatus: "ABANDONED",
      receiverName: null,
      receiverPhone: null,
      shippingAddress: null,
      paidAt: null,
      abandonedAt: "2026-09-16T20:00:05",
    }),
  );
  await h.mount();
  assert.equal(h.statusText.value, "買家逾期未付款");
  assert.equal(h.receiverPrivacyMessage.value, "訂單已棄標，無需進行出貨處理。");
});

test("detail has no payment form, receiver editing, simulated secrets or dormant logistics workflow", () => {
  for (const forbidden of [
    "payAuctionOrder",
    "updateAuctionOrderCheckout",
    "cardNumber",
    "cardCvv",
    "cardExpiry",
    "cardHolderName",
    "remitterName",
    "accountLastFive",
    "shippingStatus",
    "物流狀態",
    "出貨按鈕",
    "物流單號",
    "buyerId",
    "sellerId",
  ]) {
    assert.equal(detailSource.includes(forbidden), false, forbidden);
  }
  assert.doesNotMatch(detailSource, /<input|<textarea/);
  assert.doesNotMatch(detailSource, /立即付款|確認並付款|修改收件資料/);
});
