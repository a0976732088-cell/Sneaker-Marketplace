import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";
import { computed, reactive, ref } from "vue";

import {
  AUCTION_PAYMENT_METHOD,
  AUCTION_TRANSFER_ACCOUNT,
} from "../src/constants/auctionPayment.js";
import { formatTwd } from "../src/utils/auctionMoney.js";
import { formatAuctionTime, parseAuctionTime } from "../src/utils/auctionTime.js";
import {
  formatAccountLastFive,
  formatCardCvv,
  formatCardExpiry,
  formatCardNumber,
  validateCreditCardFormValues,
  validateShippingFormValues,
  validateTransferFormValues,
} from "../src/utils/paymentForm.js";

const checkoutSource = await readFile(
  new URL("../src/views/auction/AuctionOrderCheckoutView.vue", import.meta.url),
  "utf8",
);
const checkoutScript = checkoutSource
  .match(/<script setup>([\s\S]*?)<\/script>/)[1]
  .replace(/import\s+[\s\S]*?\s+from\s+["'][^"']+["'];\s*/g, "");

const DEFAULT_NOW = Date.parse("2026-09-15T12:00:18Z");

function order(overrides = {}) {
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
    paymentStatus: "PENDING",
    orderStatus: "PENDING_PAYMENT",
    receiverName: "原收件人",
    receiverPhone: "0911000000",
    shippingAddress: "原地址",
    createdAt: "2026-09-14T10:00:00",
    paymentDueAt: "2026-09-16T20:00:00",
    paidAt: null,
    abandonedAt: null,
    updatedAt: "2026-09-14T10:00:00",
    ...overrides,
  };
}

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, resolve, reject };
}

function httpError(status, message) {
  const error = new Error(message || (status ? `HTTP ${status}` : "Network error"));
  if (status !== undefined) error.response = { status, data: message ? { message } : undefined };
  return error;
}

function checkoutHarness({
  details = [order()],
  nowMs = DEFAULT_NOW,
  updateImplementation = async (_orderNumber, payload) => ({
    data: { data: { ...payload, updatedAt: "2026-09-15T12:01:00" } },
  }),
  payImplementation = async () => ({ data: { data: {} } }),
  member = {
    name: "會員姓名",
    phone: "0922000000",
    address: "會員地址",
    email: "member@example.com",
  },
} = {}) {
  let mounted;
  let unmounted;
  let intervalCallback;
  let currentNow = nowMs;
  let detailIndex = 0;
  const calls = [];
  const routes = [];
  const route = { params: { orderNumber: "AO-123456" } };
  const router = {
    replace: async (target) => {
      routes.push(target);
    },
  };

  const context = {
    computed,
    reactive,
    ref,
    watch: () => {},
    onMounted: (callback) => {
      mounted = callback;
    },
    onUnmounted: (callback) => {
      unmounted = callback;
    },
    useRoute: () => route,
    useRouter: () => router,
    AUCTION_PAYMENT_METHOD,
    AUCTION_TRANSFER_ACCOUNT,
    getCurrentMember: async () => member,
    getAuctionOrderDetail: async (orderNumber) => {
      calls.push({ type: "GET", orderNumber });
      const candidate = details[Math.min(detailIndex++, details.length - 1)];
      const value = await candidate;
      if (value instanceof Error) throw value;
      return { data: { data: value } };
    },
    updateAuctionOrderCheckout: async (orderNumber, payload) => {
      calls.push({ type: "PATCH", orderNumber, payload: { ...payload } });
      return updateImplementation(orderNumber, payload);
    },
    payAuctionOrder: async (orderNumber) => {
      calls.push({ type: "POST", orderNumber });
      return payImplementation(orderNumber);
    },
    handleAuctionAccessError: async (error, targetRouter) => {
      if (error.response?.status === 401) {
        await targetRouter.replace({ name: "login" });
        return true;
      }
      if (error.response?.status === 403) {
        await targetRouter.replace({ name: "forbidden" });
        return true;
      }
      return false;
    },
    resolveAuctionImageUrl: (value) => (value ? `resolved:${value}` : ""),
    formatTwd,
    formatAuctionTime,
    parseAuctionTime,
    formatAccountLastFive,
    formatCardCvv,
    formatCardExpiry,
    formatCardNumber,
    validateCreditCardFormValues,
    validateShippingFormValues,
    validateTransferFormValues,
    Date: { now: () => currentNow },
    setInterval: (callback) => {
      intervalCallback = callback;
      return 1;
    },
    clearInterval: () => {},
  };

  vm.runInNewContext(
    checkoutScript +
      "\nglobalThis.subject = { order, loading, redirecting, errorMessage, operationError, saveMessage, " +
      "isSaving, isPaying, currentTimeMs, memberEmail, memberInfo, shippingForm, shippingErrors, cardForm, " +
      "cardErrors, transferForm, transferErrors, isBuyer, isPendingPayment, paymentDeadlineMs, " +
      "hasValidPaymentDeadline, isPaymentDeadlineReached, canOperateCheckout, isCreditCard, isTransfer, " +
      "isSupportedPaymentMethod, isBusy, canSubmitPayment, remainingPaymentTimeText, deadlineNotice, " +
      "validateShippingForm, validateCreditCardForm, validateTransferForm, fillFromMember, checkoutPayload, " +
      "loadOrder, refreshExpiredOrderOnce, tickPaymentDeadline, saveCheckout, confirmAndPay };",
    context,
  );

  return {
    ...context.subject,
    calls,
    routes,
    mount: () => mounted(),
    unmount: () => unmounted(),
    tick: () => intervalCallback?.(),
    setNow: (value) => {
      currentNow = value;
    },
  };
}

test("eligible buyer checkout initializes the order snapshot and optional member profile", async () => {
  const h = checkoutHarness();
  await h.mount();

  assert.equal(h.isBuyer.value, true);
  assert.equal(h.isPendingPayment.value, true);
  assert.equal(h.hasValidPaymentDeadline.value, true);
  assert.equal(h.canOperateCheckout.value, true);
  assert.equal(h.shippingForm.receiverName, "原收件人");
  assert.equal(h.memberEmail.value, "member@example.com");
  h.fillFromMember();
  assert.deepEqual(JSON.parse(JSON.stringify(h.shippingForm)), {
    receiverName: "會員姓名",
    receiverPhone: "0922000000",
    shippingAddress: "會員地址",
  });
});

for (const [label, overrides] of [
  ["seller", { viewerRole: "SELLER" }],
  ["paid buyer", { paymentStatus: "PAID", orderStatus: "PAID" }],
  ["abandoned buyer", { orderStatus: "ABANDONED" }],
]) {
  test(`${label} is redirected from checkout to detail`, async () => {
    const h = checkoutHarness({ details: [order(overrides)] });
    await h.mount();
    assert.deepEqual(JSON.parse(JSON.stringify(h.routes)), [
      { name: "auction-order-detail", params: { orderNumber: "AO-123456" } },
    ]);
    assert.equal(h.calls.some((call) => call.type === "PATCH" || call.type === "POST"), false);
  });
}

for (const [label, paymentDueAt, nowMs] of [
  ["missing", null, DEFAULT_NOW],
  ["invalid", "not-a-date", DEFAULT_NOW],
  ["exact", "2026-09-15T20:00:18", DEFAULT_NOW],
  ["past", "2026-09-15T20:00:00", DEFAULT_NOW],
]) {
  test(`${label} payment deadline fails closed`, async () => {
    const h = checkoutHarness({ details: [order({ paymentDueAt })], nowMs });
    await h.mount();
    assert.equal(h.canOperateCheckout.value, false);
    assert.equal(h.canSubmitPayment.value, false);
    await h.saveCheckout();
    await h.confirmAndPay();
    assert.equal(h.calls.some((call) => call.type === "PATCH" || call.type === "POST"), false);
  });
}

test("valid credit-card payment awaits PATCH before POST and sends only receiver fields", async () => {
  const patch = deferred();
  const h = checkoutHarness({
    updateImplementation: () => patch.promise,
  });
  await h.mount();
  Object.assign(h.cardForm, {
    cardHolderName: "持卡人",
    cardNumber: "4111 1111 1111 1111",
    cardExpiry: "12/99",
    cardCvv: "123",
  });

  const payment = h.confirmAndPay();
  await Promise.resolve();
  assert.deepEqual(h.calls.map((call) => call.type), ["GET", "PATCH"]);

  patch.resolve({ data: { data: {} } });
  await payment;
  assert.deepEqual(h.calls.map((call) => call.type), ["GET", "PATCH", "POST"]);
  const payload = h.calls.find((call) => call.type === "PATCH").payload;
  assert.deepEqual(JSON.parse(JSON.stringify(payload)), {
    receiverName: "原收件人",
    receiverPhone: "0911000000",
    shippingAddress: "原地址",
  });
  assert.deepEqual(JSON.parse(JSON.stringify(h.routes.at(-1))), {
    name: "auction-order-detail",
    params: { orderNumber: "AO-123456" },
  });
});

test("invalid credit card performs neither PATCH nor payment", async () => {
  const h = checkoutHarness();
  await h.mount();
  Object.assign(h.cardForm, {
    cardHolderName: " ",
    cardNumber: "123",
    cardExpiry: "13/20",
    cardCvv: "1",
  });
  await h.confirmAndPay();
  assert.deepEqual(h.calls.map((call) => call.type), ["GET"]);
  assert.equal(h.cardErrors.cardNumber, "卡號格式不正確");
});

test("transfer validates name and five digits before the same PATCH then POST flow", async () => {
  const h = checkoutHarness({ details: [order({ paymentMethod: "轉帳" })] });
  await h.mount();
  h.transferForm.remitterName = " ";
  h.transferForm.accountLastFive = "123";
  await h.confirmAndPay();
  assert.deepEqual(h.calls.map((call) => call.type), ["GET"]);

  h.transferForm.remitterName = "匯款人";
  h.transferForm.accountLastFive = "12345";
  await h.confirmAndPay();
  assert.deepEqual(h.calls.map((call) => call.type), ["GET", "PATCH", "POST"]);
});

test("save-only validates shipping but neither validates nor submits payment fields", async () => {
  const h = checkoutHarness({ details: [order({ paymentMethod: "未知方式" })] });
  await h.mount();
  assert.equal(h.isSupportedPaymentMethod.value, false);
  assert.equal(h.canSubmitPayment.value, false);
  assert.equal(await h.saveCheckout(), true);
  assert.deepEqual(h.calls.map((call) => call.type), ["GET", "PATCH"]);
  assert.deepEqual(JSON.parse(JSON.stringify(h.cardErrors)), {
    cardHolderName: "",
    cardNumber: "",
    cardExpiry: "",
    cardCvv: "",
  });
});

test("PATCH rejection never calls pay", async () => {
  const h = checkoutHarness({
    updateImplementation: async () => {
      throw httpError(400, "收件資料錯誤");
    },
  });
  await h.mount();
  Object.assign(h.cardForm, {
    cardHolderName: "持卡人",
    cardNumber: "4111 1111 1111 1111",
    cardExpiry: "12/99",
    cardCvv: "123",
  });
  await h.confirmAndPay();
  assert.deepEqual(h.calls.map((call) => call.type), ["GET", "PATCH"]);
  assert.equal(h.operationError.value, "收件資料錯誤");
});

test("ambiguous payment failure reloads backend state without clearing local simulation inputs", async () => {
  const h = checkoutHarness({
    details: [order(), order()],
    payImplementation: async () => {
      throw httpError(undefined);
    },
  });
  await h.mount();
  Object.assign(h.cardForm, {
    cardHolderName: "持卡人",
    cardNumber: "4111 1111 1111 1111",
    cardExpiry: "12/99",
    cardCvv: "123",
  });
  await h.confirmAndPay();
  assert.deepEqual(h.calls.map((call) => call.type), ["GET", "PATCH", "POST", "GET"]);
  assert.equal(h.cardForm.cardHolderName, "持卡人");
  assert.equal(h.operationError.value, "付款處理失敗，請稍後再試。");
});

test("deadline crossing disables checkout and refreshes detail only once", async () => {
  const dueAt = "2026-09-15T20:00:20";
  const h = checkoutHarness({ details: [order({ paymentDueAt: dueAt }), order({ paymentDueAt: dueAt })] });
  await h.mount();
  h.setNow(Date.parse("2026-09-15T12:00:20Z"));
  await h.tick();
  await h.tick();
  assert.equal(h.canOperateCheckout.value, false);
  assert.deepEqual(h.calls.map((call) => call.type), ["GET", "GET"]);
});

test("checkout exposes only the selected payment form and has accessible controls", () => {
  assert.match(checkoutSource, /v-if="isCreditCard"/);
  assert.match(checkoutSource, /v-else-if="isTransfer"/);
  assert.match(checkoutSource, /目前付款方式無法處理/);
  assert.match(checkoutSource, /autocomplete="cc-name"/);
  assert.match(checkoutSource, /autocomplete="cc-number"/);
  assert.match(checkoutSource, /autocomplete="cc-exp"/);
  assert.match(checkoutSource, /autocomplete="cc-csc"/);
  assert.match(checkoutSource, /:aria-invalid=/);
  assert.match(checkoutSource, /Demo／模擬轉帳資訊/);
  assert.match(checkoutSource, /AUCTION_TRANSFER_ACCOUNT\.accountNumber/);
  assert.doesNotMatch(checkoutSource, /localStorage|sessionStorage|console\.log/);
});
