import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";
import { computed, reactive, ref } from "vue";

import {
  AUCTION_CVS_RESULT_LIMIT,
  AUCTION_CVS_STORE_OPTIONS,
  AUCTION_CVS_STORE_TYPE,
} from "../src/constants/auctionCvs.js";
import { TAIWAN_CITIES } from "../src/constants/auctionFormOptions.js";
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
const [shippingSource, cvsPickerSource, paymentSource, summarySource] = await Promise.all(
  [
    "../src/components/auction/checkout/AuctionCheckoutShippingSection.vue",
    "../src/components/auction/checkout/AuctionCvsStorePicker.vue",
    "../src/components/auction/checkout/AuctionCheckoutPaymentSection.vue",
    "../src/components/auction/checkout/AuctionCheckoutOrderSummary.vue",
  ].map((path) => readFile(new URL(path, import.meta.url), "utf8")),
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

function selectedCvs(overrides = {}) {
  return {
    selected: true,
    storeType: "FAMI",
    logisticsSubType: "FAMIC2C",
    cvsStoreId: "001234",
    cvsStoreName: "測試門市",
    cvsAddress: "臺北市中正區測試路 1 號",
    cvsTelephone: "02-12345678",
    cvsOutside: "0",
    selectedAt: "2026-09-15T12:00:00",
    ...overrides,
  };
}

function cvsStore(overrides = {}) {
  return {
    storeType: "UNIMART",
    storeId: "000001",
    storeName: "台北測試門市",
    storeAddress: "台北市中正區測試路 1 號",
    storePhone: "02-23456789",
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
  linePayImplementation = async () => ({
    data: {
      data: {
        paymentUrl: "https://sandbox-web-pay.line.me/payment/test",
        merchantOrderId: "ALP-ATTEMPT-001",
      },
    },
  }),
  linePayStatusImplementation = async () => ({ data: { data: { status: "PAID" } } }),
  popupBlocked = false,
  cvsSelectionResponse = { selected: false },
  cvsImplementation = async () => ({ data: { data: cvsSelectionResponse } }),
  cvsStoresImplementation = async (_orderNumber, storeType) => ({
    data: { data: [cvsStore({ storeType })] },
  }),
  cvsSelectImplementation = async (_orderNumber, payload) => ({
    data: {
      data: selectedCvs({
        storeType: payload.storeType,
        cvsStoreId: payload.storeId,
      }),
    },
  }),
  query = {},
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
  let routeWatcher;
  let nextTimeoutId = 1;
  let currentNow = nowMs;
  let detailIndex = 0;
  const calls = [];
  const routes = [];
  const timeoutCallbacks = new Map();
  const route = { params: { orderNumber: "AO-123456" }, query: { ...query } };
  const router = {
    replace: async (target) => {
      routes.push(target);
    },
  };

  const context = {
    computed,
    reactive,
    ref,
    watch: (_source, callback) => {
      routeWatcher = callback;
    },
    onMounted: (callback) => {
      mounted = callback;
    },
    onUnmounted: (callback) => {
      unmounted = callback;
    },
    useRoute: () => route,
    useRouter: () => router,
    AUCTION_CVS_RESULT_LIMIT,
    AUCTION_CVS_STORE_OPTIONS,
    AUCTION_CVS_STORE_TYPE,
    TAIWAN_CITIES,
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
    getAuctionCvsSelection: async (orderNumber) => {
      calls.push({ type: "CVS_GET", orderNumber });
      return cvsImplementation(orderNumber);
    },
    getAuctionCvsStores: async (orderNumber, storeType) => {
      calls.push({ type: "CVS_STORES", orderNumber, storeType });
      return cvsStoresImplementation(orderNumber, storeType);
    },
    selectAuctionCvsStore: async (orderNumber, payload) => {
      calls.push({ type: "CVS_SELECT", orderNumber, payload: { ...payload } });
      return cvsSelectImplementation(orderNumber, payload);
    },
    requestAuctionLinePay: async (orderNumber) => {
      calls.push({ type: "LINE_PAY", orderNumber });
      return linePayImplementation(orderNumber);
    },
    getAuctionLinePayStatus: async (orderNumber, merchantOrderId) => {
      calls.push({ type: "LINE_PAY_STATUS", orderNumber, merchantOrderId });
      return linePayStatusImplementation(orderNumber, merchantOrderId);
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
    setTimeout: (callback, delay) => {
      const timerId = nextTimeoutId++;
      timeoutCallbacks.set(timerId, { callback, delay });
      return timerId;
    },
    clearTimeout: (timerId) => {
      timeoutCallbacks.delete(timerId);
    },
    window: {
      open: (...args) => {
        calls.push({ type: "POPUP_OPEN", args });
        return popupBlocked ? null : popupWindow;
      },
    },
  };
  const popupWindow = {
    closed: false,
    opener: { source: "checkout" },
    location: { href: "about:blank" },
    close() {
      this.closed = true;
      calls.push({ type: "POPUP_CLOSE" });
    },
  };

  vm.runInNewContext(
    checkoutScript +
      "\nglobalThis.subject = { order, loading, redirecting, errorMessage, operationError, saveMessage, routeNotice, " +
      "isSaving, isPaying, currentTimeMs, memberEmail, memberInfo, shippingForm, shippingErrors, cardForm, " +
      "cardErrors, transferForm, transferErrors, selectedCvsStoreType, cvsSelection, isBuyer, " +
      "cvsStores, cvsStoreFilters, cvsStoreLoadError, cvsStoreSelectionError, isLoadingCvsStores, " +
      "isSelectingCvsStore, isCvsPickerOpen, isWaitingForLinePay, linePayMerchantOrderId, " +
      "isPendingPayment, isCvsShipping, paymentDeadlineMs, hasValidPaymentDeadline, " +
      "isPaymentDeadlineReached, canOperateCheckout, isCreditCard, isTransfer, isLinePay, " +
      "isSupportedPaymentMethod, isBusy, hasSelectedCvsStore, canSubmitPayment, " +
      "selectedCvsStoreLabel, normalizedCvsCity, normalizedCvsKeyword, hasCvsSearchCriteria, " +
      "matchingCvsStores, hasTooManyCvsStores, filteredCvsStores, paymentMethodLabel, " +
      "paymentButtonText, remainingPaymentTimeText, " +
      "deadlineNotice, validateShippingForm, validateCreditCardForm, validateTransferForm, " +
      "validatePaymentForm, fillFromMember, checkoutPayload, loadOrder, loadCvsSelection, " +
      "openCvsPicker, closeCvsPicker, loadCvsStores, changeCvsStoreType, reloadCvsStores, " +
      "isCurrentCvsStore, buildStoreMapUrl, openStoreMap, chooseCvsStore, " +
      "refreshExpiredOrderOnce, tickPaymentDeadline, saveCheckout, confirmAndPay, " +
      "pollLinePayStatus, stopLinePayPolling };",
    context,
  );

  return {
    ...context.subject,
    calls,
    routes,
    popupWindow,
    mount: () => mounted(),
    unmount: () => unmounted(),
    tick: () => intervalCallback?.(),
    flush: () => new Promise((resolve) => setImmediate(resolve)),
    runNextTimeout: async () => {
      const next = timeoutCallbacks.entries().next();
      if (next.done) return false;
      const [timerId, timer] = next.value;
      timeoutCallbacks.delete(timerId);
      timer.callback();
      await new Promise((resolve) => setImmediate(resolve));
      return true;
    },
    pendingTimeouts: () => [...timeoutCallbacks.values()].map((timer) => timer.delay),
    changeOrderNumber: (orderNumber) => {
      const previousOrderNumber = route.params.orderNumber;
      route.params.orderNumber = orderNumber;
      routeWatcher?.(orderNumber, previousOrderNumber);
    },
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

test("LINE Pay opens its popup before PATCH, then requests and starts status polling", async () => {
  const patch = deferred();
  const h = checkoutHarness({
    details: [order({ paymentMethod: "LINE Pay" })],
    updateImplementation: () => patch.promise,
    linePayStatusImplementation: async () => ({ data: { data: { status: "WAITING" } } }),
    payImplementation: async () => {
      throw new Error("legacy pay must not be called");
    },
  });
  await h.mount();

  assert.equal(h.isLinePay.value, true);
  assert.equal(h.isCreditCard.value, false);
  assert.equal(h.isTransfer.value, false);
  assert.equal(h.isSupportedPaymentMethod.value, true);
  assert.equal(h.paymentMethodLabel.value, "LINE Pay");
  assert.equal(h.paymentButtonText.value, "使用 LINE Pay 付款");
  assert.equal(h.validatePaymentForm(), true);

  const payment = h.confirmAndPay();
  assert.deepEqual(h.calls.map((call) => call.type), ["GET", "POPUP_OPEN", "PATCH"]);
  assert.deepEqual(h.calls.find((call) => call.type === "POPUP_OPEN").args, [
    "about:blank",
    "sneakermarket-line-pay",
    "popup,width=520,height=760",
  ]);

  patch.resolve({ data: { data: {} } });
  await payment;
  await h.flush();

  assert.deepEqual(h.calls.map((call) => call.type), [
    "GET",
    "POPUP_OPEN",
    "PATCH",
    "LINE_PAY",
    "LINE_PAY_STATUS",
  ]);
  assert.equal(h.calls.some((call) => call.type === "POST"), false);
  assert.equal(h.popupWindow.location.href, "https://sandbox-web-pay.line.me/payment/test");
  assert.equal(h.linePayMerchantOrderId.value, "ALP-ATTEMPT-001");
  assert.equal(h.isWaitingForLinePay.value, true);
  assert.deepEqual(h.pendingTimeouts(), [1500]);
  assert.deepEqual(h.routes, []);
});

test("blocked LINE Pay popup creates neither checkout update nor payment attempt", async () => {
  const h = checkoutHarness({
    details: [order({ paymentMethod: "LINE Pay" })],
    popupBlocked: true,
  });
  await h.mount();

  await h.confirmAndPay();

  assert.deepEqual(h.calls.map((call) => call.type), ["GET", "POPUP_OPEN"]);
  assert.equal(
    h.operationError.value,
    "瀏覽器阻擋了 LINE Pay 付款視窗，請允許彈出式視窗後再試。",
  );
});

test("LINE Pay request failure closes the blank popup and never starts status polling", async () => {
  const h = checkoutHarness({
    details: [order({ paymentMethod: "LINE Pay" }), order({ paymentMethod: "LINE Pay" })],
    linePayImplementation: async () => {
      throw httpError(502);
    },
  });
  await h.mount();

  await h.confirmAndPay();

  assert.equal(h.popupWindow.closed, true);
  assert.equal(h.calls.some((call) => call.type === "LINE_PAY_STATUS"), false);
  assert.equal(h.isWaitingForLinePay.value, false);
  assert.equal(h.operationError.value, "付款處理失敗，請稍後再試。");
});

test("unselected CVS blocks LINE Pay before popup, checkout update and payment request", async () => {
  const h = checkoutHarness({
    details: [order({ paymentMethod: "LINE Pay", shippingMethod: "超商寄送" })],
  });
  await h.mount();

  await h.confirmAndPay();

  assert.equal(h.canSubmitPayment.value, false);
  assert.equal(h.shippingErrors.shippingAddress, "請先選擇超商取貨門市");
  assert.equal(
    h.calls.some((call) => ["POPUP_OPEN", "PATCH", "LINE_PAY"].includes(call.type)),
    false,
  );
});

test("LINE Pay WAITING polls sequentially and PAID closes popup then routes to detail", async () => {
  const statuses = ["WAITING", "PAID"];
  const h = checkoutHarness({
    details: [order({ paymentMethod: "LINE Pay" })],
    linePayStatusImplementation: async () => ({
      data: { data: { status: statuses.shift() } },
    }),
  });
  await h.mount();
  await h.confirmAndPay();
  await h.flush();

  assert.equal(h.calls.filter((call) => call.type === "LINE_PAY_STATUS").length, 1);
  assert.deepEqual(h.pendingTimeouts(), [1500]);
  assert.equal(h.popupWindow.closed, false);

  await h.runNextTimeout();

  assert.equal(h.calls.filter((call) => call.type === "LINE_PAY_STATUS").length, 2);
  assert.equal(h.popupWindow.closed, true);
  assert.equal(h.isWaitingForLinePay.value, false);
  assert.deepEqual(JSON.parse(JSON.stringify(h.routes.at(-1))), {
    name: "auction-order-detail",
    params: { orderNumber: "AO-123456" },
  });
});

test("transient LINE Pay status failure keeps the same attempt and later PAID completes", async () => {
  let statusCalls = 0;
  const h = checkoutHarness({
    details: [order({ paymentMethod: "LINE Pay" })],
    linePayStatusImplementation: async () => {
      statusCalls++;
      if (statusCalls === 1) throw httpError(502);
      return { data: { data: { status: "PAID" } } };
    },
  });
  await h.mount();
  await h.confirmAndPay();
  await h.flush();

  assert.equal(statusCalls, 1);
  assert.equal(h.isWaitingForLinePay.value, true);
  assert.equal(h.linePayMerchantOrderId.value, "ALP-ATTEMPT-001");
  assert.equal(h.popupWindow.closed, false);
  assert.deepEqual(h.pendingTimeouts(), [1500]);
  assert.deepEqual(h.routes, []);

  await h.runNextTimeout();

  assert.equal(statusCalls, 2);
  assert.equal(h.popupWindow.closed, true);
  assert.equal(h.isWaitingForLinePay.value, false);
  assert.deepEqual(JSON.parse(JSON.stringify(h.routes.at(-1))), {
    name: "auction-order-detail",
    params: { orderNumber: "AO-123456" },
  });
});

for (const [status, expectedMessage, messageTarget] of [
  ["CANCELLED", "LINE Pay 已取消，你仍可在付款期限內重新付款。", "routeNotice"],
  ["FAILED", "LINE Pay 付款失敗，請在付款期限內重新嘗試。", "operationError"],
]) {
  test(`LINE Pay ${status} stops polling, closes popup and keeps checkout pending`, async () => {
    const h = checkoutHarness({
      details: [order({ paymentMethod: "LINE Pay" })],
      linePayStatusImplementation: async () => ({ data: { data: { status } } }),
    });
    await h.mount();
    await h.confirmAndPay();
    await h.flush();

    assert.equal(h[messageTarget].value, expectedMessage);
    assert.equal(h.popupWindow.closed, true);
    assert.equal(h.isWaitingForLinePay.value, false);
    assert.equal(h.order.value.paymentStatus, "PENDING");
    assert.equal(h.order.value.orderStatus, "PENDING_PAYMENT");
    assert.deepEqual(h.routes, []);
    assert.deepEqual(h.pendingTimeouts(), []);
  });
}

test("manually closed LINE Pay popup performs one final status check before stopping on WAITING", async () => {
  const h = checkoutHarness({
    details: [order({ paymentMethod: "LINE Pay" })],
    linePayStatusImplementation: async () => ({ data: { data: { status: "WAITING" } } }),
  });
  await h.mount();
  await h.confirmAndPay();
  await h.flush();
  assert.equal(h.calls.filter((call) => call.type === "LINE_PAY_STATUS").length, 1);

  h.popupWindow.closed = true;
  await h.runNextTimeout();

  assert.equal(h.calls.filter((call) => call.type === "LINE_PAY_STATUS").length, 2);
  assert.equal(h.isWaitingForLinePay.value, false);
  assert.equal(
    h.routeNotice.value,
    "LINE Pay 付款視窗已關閉。若尚未完成付款，可重新發起付款。",
  );
  assert.deepEqual(h.pendingTimeouts(), []);
});

test("unmount cancels LINE Pay polling and closes its popup", async () => {
  const h = checkoutHarness({
    details: [order({ paymentMethod: "LINE Pay" })],
    linePayStatusImplementation: async () => ({ data: { data: { status: "WAITING" } } }),
  });
  await h.mount();
  await h.confirmAndPay();
  await h.flush();
  assert.deepEqual(h.pendingTimeouts(), [1500]);

  h.unmount();

  assert.deepEqual(h.pendingTimeouts(), []);
  assert.equal(h.popupWindow.closed, true);
  assert.equal(await h.runNextTimeout(), false);
  assert.equal(h.calls.filter((call) => call.type === "LINE_PAY_STATUS").length, 1);
});

test("route order change cancels LINE Pay polling and closes its popup", async () => {
  const h = checkoutHarness({
    details: [order({ paymentMethod: "LINE Pay" })],
    linePayStatusImplementation: async () => ({ data: { data: { status: "WAITING" } } }),
  });
  await h.mount();
  await h.confirmAndPay();
  await h.flush();
  assert.deepEqual(h.pendingTimeouts(), [1500]);

  h.changeOrderNumber("AO-654321");

  assert.deepEqual(h.pendingTimeouts(), []);
  assert.equal(h.popupWindow.closed, true);
  assert.equal(h.isWaitingForLinePay.value, false);
});

for (const shippingMethod of ["宅配", "郵寄"]) {
  test(`${shippingMethod} keeps the original address flow and never queries CVS selection`, async () => {
    const h = checkoutHarness({ details: [order({ shippingMethod })] });
    await h.mount();

    assert.equal(h.isCvsShipping.value, false);
    assert.equal(h.canSubmitPayment.value, true);
    assert.equal(h.calls.some((call) => call.type === "CVS_GET"), false);
    h.fillFromMember();
    assert.equal(h.shippingForm.shippingAddress, "會員地址");
  });
}

test("CVS StoreList is lazy and loading appears only after opening the picker", async () => {
  const stores = deferred();
  const h = checkoutHarness({
    details: [order({ shippingMethod: "超商寄送" })],
    cvsStoresImplementation: () => stores.promise,
  });
  await h.mount();
  assert.deepEqual(h.calls.map((call) => call.type), ["GET", "CVS_GET"]);
  assert.equal(h.loading.value, false);
  assert.equal(h.isCvsPickerOpen.value, false);
  assert.equal(h.isLoadingCvsStores.value, false);

  const opening = h.openCvsPicker();
  await h.flush();
  assert.equal(h.isCvsPickerOpen.value, true);
  assert.equal(h.isLoadingCvsStores.value, true);
  assert.equal(h.order.value.orderNumber, "AO-123456");
  assert.deepEqual(h.calls.map((call) => call.type), ["GET", "CVS_GET", "CVS_STORES"]);
  assert.equal(h.calls.at(-1).storeType, "UNIMART");

  stores.resolve({ data: { data: [cvsStore()] } });
  await opening;
  assert.equal(h.isLoadingCvsStores.value, false);
  assert.equal(h.cvsStores.value.length, 1);
});

test("CVS map URL uses brand, name and address for both StoreList and saved selection DTOs", () => {
  const h = checkoutHarness();
  const listStoreUrl = h.buildStoreMapUrl(
    cvsStore({
      storeName: "博正門市",
      storeAddress: "高雄市左營區博愛二路 1 號",
    }),
  );
  assert.equal(
    listStoreUrl,
    `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(
      "7-ELEVEN 博正門市 高雄市左營區博愛二路 1 號",
    )}`,
  );

  const savedSelectionUrl = h.buildStoreMapUrl(selectedCvs());
  assert.equal(
    savedSelectionUrl,
    `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(
      "全家 測試門市 臺北市中正區測試路 1 號",
    )}`,
  );

  const blankPrimaryFieldsUrl = h.buildStoreMapUrl({
    storeType: "HILIFE",
    storeName: " ",
    storeAddress: "",
    cvsStoreName: "備援門市",
    cvsAddress: "臺南市中西區測試路 2 號",
  });
  assert.equal(
    blankPrimaryFieldsUrl,
    `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(
      "萊爾富 備援門市 臺南市中西區測試路 2 號",
    )}`,
  );
  assert.equal(h.buildStoreMapUrl({}), "");
});

test("opening a StoreList result map keeps the picker open without selecting the store", async () => {
  const h = checkoutHarness({
    details: [order({ shippingMethod: "超商寄送" })],
  });
  await h.mount();
  await h.openCvsPicker();
  const store = h.cvsStores.value[0];

  h.openStoreMap(store);

  const mapCall = h.calls.find(
    (call) => call.type === "POPUP_OPEN" && call.args[0].includes("google.com/maps/search"),
  );
  assert.ok(mapCall);
  assert.deepEqual(mapCall.args.slice(1), ["_blank", "noopener,noreferrer"]);
  assert.match(decodeURIComponent(mapCall.args[0]), /7-ELEVEN 台北測試門市 台北市中正區測試路 1 號/);
  assert.equal(h.popupWindow.opener, null);
  assert.equal(h.isCvsPickerOpen.value, true);
  assert.equal(h.hasSelectedCvsStore.value, false);
  assert.equal(h.calls.some((call) => call.type === "CVS_SELECT"), false);
});

test("opening the saved CVS map does not mutate the current selection", async () => {
  const h = checkoutHarness({
    details: [order({ shippingMethod: "超商寄送" })],
    cvsSelectionResponse: selectedCvs(),
  });
  await h.mount();
  const selectionBefore = JSON.parse(JSON.stringify(h.cvsSelection));

  h.openStoreMap(h.cvsSelection);

  const mapCall = h.calls.find(
    (call) => call.type === "POPUP_OPEN" && call.args[0].includes("google.com/maps/search"),
  );
  assert.ok(mapCall);
  assert.match(decodeURIComponent(mapCall.args[0]), /全家 測試門市 臺北市中正區測試路 1 號/);
  assert.deepEqual(JSON.parse(JSON.stringify(h.cvsSelection)), selectionBefore);
  assert.equal(h.isCvsPickerOpen.value, false);
  assert.equal(h.calls.some((call) => call.type === "CVS_SELECT"), false);
});

test("unselected CVS disables payment and preserves its address on member fill", async () => {
  const h = checkoutHarness({
    details: [
      order({
        shippingMethod: "超商寄送",
        shippingAddress: "尚未覆寫的門市地址",
      }),
    ],
  });
  await h.mount();
  await h.flush();

  assert.deepEqual(h.calls.map((call) => call.type), ["GET", "CVS_GET"]);
  assert.equal(h.isCvsPickerOpen.value, false);
  assert.equal(h.isCvsShipping.value, true);
  assert.equal(h.hasSelectedCvsStore.value, false);
  assert.equal(h.canSubmitPayment.value, false);
  assert.equal(h.hasCvsSearchCriteria.value, false);
  assert.equal(h.filteredCvsStores.value.length, 0);

  h.fillFromMember();
  assert.equal(h.shippingForm.receiverName, "會員姓名");
  assert.equal(h.shippingForm.receiverPhone, "0922000000");
  assert.equal(h.shippingForm.shippingAddress, "尚未覆寫的門市地址");

  await h.confirmAndPay();
  assert.equal(h.shippingErrors.shippingAddress, "請先選擇超商取貨門市");
  assert.equal(h.calls.some((call) => call.type === "PATCH" || call.type === "POST"), false);
});

test("CVS stores stay hidden without criteria, normalize 台/臺 and cap memory results at 100", async () => {
  const stores = Array.from({ length: 105 }, (_, index) =>
    cvsStore({
      storeId: String(index).padStart(6, "0"),
      storeName: `台北測試門市 ${index}`,
      storeAddress: `台北市中正區測試路 ${index} 號`,
    }),
  );
  stores.push(
    cvsStore({
      storeId: "999999",
      storeName: "高雄門市",
      storeAddress: "高雄市前鎮區測試路 1 號",
    }),
  );
  const h = checkoutHarness({
    details: [order({ shippingMethod: "超商寄送" })],
    cvsStoresImplementation: async () => ({ data: { data: stores } }),
  });
  await h.mount();
  await h.openCvsPicker();

  assert.equal(h.cvsStores.value.length, 106);
  assert.equal(h.hasCvsSearchCriteria.value, false);
  assert.equal(h.filteredCvsStores.value.length, 0);

  h.cvsStoreFilters.city = "臺北市";
  assert.equal(h.normalizedCvsCity.value, "臺北市");
  assert.equal(h.matchingCvsStores.value.length, 105);
  assert.equal(h.hasTooManyCvsStores.value, true);
  assert.equal(h.filteredCvsStores.value.length, 100);
  assert.equal(h.filteredCvsStores.value.every((store) => store.storeAddress.startsWith("台北市")), true);

  h.cvsStoreFilters.city = "";
  h.cvsStoreFilters.keyword = "臺北測試門市 104";
  assert.deepEqual(h.filteredCvsStores.value.map((store) => store.storeId), ["000104"]);
});

test("changing CVS brand reloads only that allowlisted type without clearing the current selection", async () => {
  const h = checkoutHarness({
    details: [order({ shippingMethod: "超商寄送" })],
    cvsSelectionResponse: selectedCvs(),
  });
  await h.mount();

  assert.equal(h.selectedCvsStoreType.value, "FAMI");
  assert.equal(h.cvsSelection.cvsStoreId, "001234");
  assert.equal(h.calls.some((call) => call.type === "CVS_STORES"), false);

  await h.openCvsPicker();
  assert.equal(h.calls.find((call) => call.type === "CVS_STORES").storeType, "FAMI");

  h.cvsStoreFilters.city = "臺北市";
  await h.changeCvsStoreType("HILIFE");
  assert.equal(h.selectedCvsStoreType.value, "HILIFE");
  assert.equal(h.cvsSelection.storeType, "FAMI");
  assert.equal(h.cvsSelection.cvsStoreId, "001234");
  assert.equal(h.cvsStoreFilters.city, "臺北市");
  assert.deepEqual(
    h.calls.filter((call) => call.type === "CVS_STORES").map((call) => call.storeType),
    ["FAMI", "HILIFE"],
  );
});

test("closing the CVS picker preserves the existing selection and clears only picker state", async () => {
  const h = checkoutHarness({
    details: [order({ shippingMethod: "超商寄送" })],
    cvsSelectionResponse: selectedCvs(),
  });
  await h.mount();
  await h.openCvsPicker();
  h.cvsStoreFilters.city = "臺北市";
  h.cvsStoreFilters.keyword = "測試";

  h.closeCvsPicker();

  assert.equal(h.isCvsPickerOpen.value, false);
  assert.equal(h.cvsSelection.cvsStoreId, "001234");
  assert.equal(h.cvsSelection.cvsStoreName, "測試門市");
  assert.equal(h.shippingForm.shippingAddress, "臺北市中正區測試路 1 號");
  assert.equal(h.cvsStoreFilters.city, "");
  assert.equal(h.cvsStoreFilters.keyword, "");
  assert.equal(h.calls.some((call) => call.type === "CVS_SELECT"), false);
});

test("choosing an in-memory CVS store PUTs only type and id then updates the checkout selection", async () => {
  const h = checkoutHarness({
    details: [order({ shippingMethod: "超商寄送", shippingAddress: "舊地址" })],
  });
  await h.mount();
  await h.openCvsPicker();
  const store = h.cvsStores.value[0];

  await h.chooseCvsStore(store);

  const request = h.calls.find((call) => call.type === "CVS_SELECT");
  assert.deepEqual(JSON.parse(JSON.stringify(request.payload)), {
    storeType: "UNIMART",
    storeId: "000001",
  });
  assert.equal(h.hasSelectedCvsStore.value, true);
  assert.equal(h.cvsSelection.cvsStoreId, "000001");
  assert.equal(h.shippingForm.shippingAddress, "臺北市中正區測試路 1 號");
  assert.equal(h.canSubmitPayment.value, true);
  assert.equal(h.saveMessage.value, "取貨門市已更新。");
  assert.equal(h.isCurrentCvsStore(store), true);
  assert.equal(h.isCvsPickerOpen.value, false);
});

test("CVS list failures are isolated, visible and retryable", async () => {
  let shouldFail = true;
  const h = checkoutHarness({
    details: [order({ shippingMethod: "超商寄送" })],
    cvsStoresImplementation: async (_orderNumber, storeType) => {
      if (shouldFail) throw httpError(502);
      return { data: { data: [cvsStore({ storeType })] } };
    },
  });
  await h.mount();
  await h.openCvsPicker();

  assert.equal(h.cvsStoreLoadError.value, "門市資料暫時無法取得，請稍後再試。");
  assert.equal(h.cvsStores.value.length, 0);
  assert.equal(h.errorMessage.value, "");

  shouldFail = false;
  await h.reloadCvsStores();
  assert.equal(h.cvsStoreLoadError.value, "");
  assert.equal(h.cvsStores.value.length, 1);
});

test("failed CVS PUT preserves the previously selected store", async () => {
  const h = checkoutHarness({
    details: [order({ shippingMethod: "超商寄送" })],
    cvsSelectionResponse: selectedCvs(),
    cvsSelectImplementation: async () => {
      throw httpError(502);
    },
  });
  await h.mount();
  await h.openCvsPicker();
  const replacement = h.cvsStores.value[0];
  await h.chooseCvsStore(replacement);

  assert.equal(h.cvsSelection.cvsStoreId, "001234");
  assert.equal(h.cvsSelection.cvsStoreName, "測試門市");
  assert.equal(h.cvsStoreSelectionError.value, "門市選擇失敗，請稍後再試。");
  assert.equal(h.isCvsPickerOpen.value, true);
});

test("selected CVS displays the saved store and sends its address through the original credit-card flow", async () => {
  const h = checkoutHarness({
    details: [order({ shippingMethod: "超商寄送", shippingAddress: "舊地址" })],
    cvsSelectionResponse: selectedCvs(),
  });
  await h.mount();
  await h.flush();

  assert.equal(h.hasSelectedCvsStore.value, true);
  assert.equal(h.selectedCvsStoreType.value, "FAMI");
  assert.equal(h.selectedCvsStoreLabel.value, "全家");
  assert.equal(h.cvsSelection.cvsStoreId, "001234");
  assert.equal(h.cvsSelection.cvsStoreName, "測試門市");
  assert.equal(h.shippingForm.shippingAddress, "臺北市中正區測試路 1 號");
  assert.equal(h.canSubmitPayment.value, true);

  h.fillFromMember();
  assert.equal(h.shippingForm.shippingAddress, "臺北市中正區測試路 1 號");
  Object.assign(h.cardForm, {
    cardHolderName: "持卡人",
    cardNumber: "4111 1111 1111 1111",
    cardExpiry: "12/99",
    cardCvv: "123",
  });
  await h.confirmAndPay();

  assert.deepEqual(h.calls.map((call) => call.type), [
    "GET",
    "CVS_GET",
    "PATCH",
    "POST",
  ]);
  assert.equal(
    h.calls.find((call) => call.type === "PATCH").payload.shippingAddress,
    "臺北市中正區測試路 1 號",
  );
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
  assert.match(paymentSource, /v-if="isCreditCard"/);
  assert.match(paymentSource, /v-else-if="isTransfer"/);
  assert.match(paymentSource, /v-else-if="isLinePay"/);
  assert.match(paymentSource, /將開啟 LINE Pay 付款視窗/);
  assert.match(paymentSource, /等待 LINE Pay 完成付款/);
  assert.match(checkoutSource, /使用 LINE Pay 付款/);
  assert.match(paymentSource, /目前付款方式無法處理/);
  assert.match(paymentSource, /autocomplete="cc-name"/);
  assert.match(paymentSource, /autocomplete="cc-number"/);
  assert.match(paymentSource, /autocomplete="cc-exp"/);
  assert.match(paymentSource, /autocomplete="cc-csc"/);
  assert.match(paymentSource, /:aria-invalid=/);
  assert.match(paymentSource, /Demo／模擬轉帳資訊/);
  assert.match(paymentSource, /AUCTION_TRANSFER_ACCOUNT\.accountNumber/);
  assert.match(shippingSource, /v-if="!isCvsShipping"/);
  assert.match(cvsPickerSource, /AUCTION_CVS_STORE_OPTIONS/);
  assert.match(shippingSource, /目前取貨門市/);
  assert.match(shippingSource, /未選擇門市/);
  assert.match(shippingSource, /hasSelectedCvsStore \? "更換門市" : "選擇門市"/);
  assert.match(shippingSource, /門市代碼：/);
  assert.match(cvsPickerSource, /門市代碼：/);
  assert.equal((shippingSource + cvsPickerSource).match(/查看地圖/g)?.length, 2);
  assert.match(
    shippingSource,
    /v-if="hasSelectedCvsStore"[\s\S]*?@click="emit\('open-store-map', cvsSelection\)"/,
  );
  assert.match(cvsPickerSource, /@click="emit\('open-map', store\)"/);
  assert.match(cvsPickerSource, /v-if="open"/);
  assert.match(cvsPickerSource, /role="dialog"/);
  assert.match(cvsPickerSource, /aria-modal="true"/);
  assert.match(cvsPickerSource, /關閉門市選擇/);
  assert.match(cvsPickerSource, />\s*取消\s*</);
  assert.match(cvsPickerSource, /TAIWAN_CITIES/);
  assert.match(cvsPickerSource, /選擇超商品牌/);
  assert.match(cvsPickerSource, /搜尋門市/);
  assert.match(cvsPickerSource, /門市名稱、店號或地址/);
  assert.match(cvsPickerSource, /請先選擇縣市或輸入門市關鍵字/);
  assert.match(cvsPickerSource, /符合門市過多，請縮小搜尋範圍。/);
  assert.match(cvsPickerSource, /最多 100 筆/);
  assert.match(cvsPickerSource, /重新載入門市/);
  assert.match(cvsPickerSource, /選擇此門市/);
  assert.match(shippingSource, /請先選擇超商取貨門市/);
  assert.match(summarySource, /訂單摘要/);
  assert.match(summarySource, /formatTwd\(order\.totalAmount\)/);
  assert.match(summarySource, /remainingPaymentTimeText/);
  assert.match(summarySource, /emit\('image-error'\)/);
  assert.match(checkoutSource, /window\.open\(/);
  assert.match(checkoutSource, /paymentWindow\.location\.href = paymentUrl\.trim\(\)/);
  const allCheckoutSources = [checkoutSource, shippingSource, cvsPickerSource, paymentSource, summarySource].join("\n");
  assert.doesNotMatch(allCheckoutSources, /window\.location\.assign|query\?\.linePay/);
  assert.doesNotMatch(
    allCheckoutSources,
    /\/cvs\/store-map|localStorage|sessionStorage|console\.log/,
  );
});

test("checkout wires all four UI components to Parent state and handlers", () => {
  function openingTag(componentName) {
    const match = checkoutSource.match(new RegExp("<" + componentName + "\\b([^>]*)>"));
    assert.ok(match, componentName + " must be mounted");
    return match[1];
  }

  function expectBindings(componentName, bindings) {
    const tag = openingTag(componentName);
    for (const binding of bindings) {
      assert.ok(tag.includes(binding), componentName + " missing " + binding);
    }
  }

  expectBindings("AuctionCheckoutShippingSection", [
    ':order="order"',
    ':shipping-form="shippingForm"',
    ':shipping-errors="shippingErrors"',
    ':is-cvs-shipping="isCvsShipping"',
    ':can-operate-checkout="canOperateCheckout"',
    ':is-busy="isBusy"',
    ':deadline-notice="deadlineNotice"',
    ':route-notice="routeNotice"',
    ':has-selected-cvs-store="hasSelectedCvsStore"',
    ':cvs-selection="cvsSelection"',
    '@fill-from-member="fillFromMember"',
    '@open-cvs-picker="openCvsPicker"',
    '@open-store-map="openStoreMap"',
  ]);
  assert.match(checkoutSource, /<AuctionCheckoutShippingSection[\s\S]*?<template #cvs-picker>[\s\S]*?<AuctionCvsStorePicker/);

  expectBindings("AuctionCvsStorePicker", [
    ':open="isCvsPickerOpen"',
    ':selected-store-type="selectedCvsStoreType"',
    ':filters="cvsStoreFilters"',
    ':is-loading="isLoadingCvsStores"',
    ':is-selecting="isSelectingCvsStore"',
    ':load-error="cvsStoreLoadError"',
    ':selection-error="cvsStoreSelectionError"',
    ':has-search-criteria="hasCvsSearchCriteria"',
    ':filtered-stores="filteredCvsStores"',
    ':has-too-many-stores="hasTooManyCvsStores"',
    ':is-current-store="isCurrentCvsStore"',
    '@close="closeCvsPicker()"',
    '@change-store-type="changeCvsStoreType"',
    '@reload="reloadCvsStores"',
    '@select-store="chooseCvsStore"',
    '@open-map="openStoreMap"',
  ]);

  expectBindings("AuctionCheckoutPaymentSection", [
    ':order="order"',
    ':is-credit-card="isCreditCard"',
    ':is-transfer="isTransfer"',
    ':is-line-pay="isLinePay"',
    ':can-operate-checkout="canOperateCheckout"',
    ':is-busy="isBusy"',
    ':card-form="cardForm"',
    ':card-errors="cardErrors"',
    ':transfer-form="transferForm"',
    ':transfer-errors="transferErrors"',
    ':is-waiting-for-line-pay="isWaitingForLinePay"',
    '@card-number-input="handleCardNumberInput"',
    '@card-expiry-input="handleCardExpiryInput"',
    '@card-cvv-input="handleCardCvvInput"',
    '@account-last-five-input="handleAccountLastFiveInput"',
  ]);

  expectBindings("AuctionCheckoutOrderSummary", [
    ':order="order"',
    ':product-image-url="productImageUrl"',
    ':product-meta="productMeta"',
    ':can-operate-checkout="canOperateCheckout"',
    ':remaining-payment-time-text="remainingPaymentTimeText"',
    ':deadline-notice="deadlineNotice"',
    '@image-error="imageLoadFailed = true"',
  ]);
});
