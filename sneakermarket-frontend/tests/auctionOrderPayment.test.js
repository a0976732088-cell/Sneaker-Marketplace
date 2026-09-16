import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";
import { computed, ref } from "vue";
import { formatTwd } from "../src/utils/auctionMoney.js";
import { formatAuctionTime, parseAuctionTime } from "../src/utils/auctionTime.js";
import { formatAuctionOrderStatus } from "../src/utils/auctionMemberStatus.js";

const routerSource = await readFile(
  new URL("../src/router/index.js", import.meta.url),
  "utf8",
);
const serviceSource = await readFile(
  new URL("../src/service/auctionService.js", import.meta.url),
  "utf8",
);
const paymentViewSource = await readFile(
  new URL("../src/views/auction/AuctionOrderPaymentView.vue", import.meta.url),
  "utf8",
);
const orderListSource = await readFile(
  new URL("../src/views/auction/AuctionOrderListView.vue", import.meta.url),
  "utf8",
);
const DEFAULT_NOW_MS = Date.parse("2026-09-15T12:00:18Z");
const DEFAULT_PAYMENT_DUE_AT = "2026-09-16T20:00:00";

function evaluateAuctionService() {
  const requests = [];
  const source = serviceSource
    .replace(/^import[^\n]+\n/gm, "")
    .replace(/^export /gm, "");
  const context = {
    httpClient: {
      get: (...args) => {
        const request = { method: "GET", url: args[0], config: args[1], args };
        requests.push(request);
        return request;
      },
      post: (...args) => {
        const request = { method: "POST", url: args[0], body: args[1], args };
        requests.push(request);
        return request;
      },
      put: () => {},
      delete: () => {},
    },
  };
  vm.runInNewContext(
    source +
      "\nglobalThis.subject = { getAuctionOrderDetail, payAuctionOrder };",
    context,
  );
  return { ...context.subject, requests };
}

function detail(overrides = {}) {
  return {
    orderNumber: "AO-123456",
    auctionId: 7,
    productName: "Jordan 1 Retro High",
    brandId: 9,
    brandName: "Nike",
    model: "Retro High",
    size: "US 9",
    imageUrl: "/uploads/auctions/jordan.jpg",
    finalPrice: 6800,
    shippingFee: 100,
    totalAmount: 6900,
    shippingMethod: "宅配",
    paymentMethod: "銀行轉帳",
    paymentStatus: "PENDING",
    shippingStatus: "NOT_SHIPPED",
    orderStatus: "PENDING_PAYMENT",
    receiverName: "王小明",
    receiverPhone: "0912345678",
    shippingAddress: "台北市測試路 1 號",
    createdAt: "2026-09-14T10:00:00",
    paymentDueAt: DEFAULT_PAYMENT_DUE_AT,
    paidAt: null,
    abandonedAt: null,
    updatedAt: "2026-09-14T10:00:00",
    ...overrides,
  };
}

function paidDetail(overrides = {}) {
  return detail({
    paymentStatus: "PAID",
    orderStatus: "PAID",
    paidAt: "2026-09-14T11:30:00",
    updatedAt: "2026-09-14T11:30:00",
    ...overrides,
  });
}

function httpError(status) {
  const error = new Error(status ? `HTTP ${status}` : "Network error");
  if (status !== undefined) error.response = { status };
  return error;
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

function paymentViewHarness({
  detailOutcomes = [detail()],
  payImplementation = async () => ({ data: { data: {} } }),
  confirmResult = true,
  confirmImplementation,
  nowMs = DEFAULT_NOW_MS,
} = {}) {
  const script = paymentViewSource
    .match(/<script setup>([\s\S]*?)<\/script>/)[1]
    .replace(/^import[\s\S]*?from\s+["'][^"']+["'];\r?\n/gm, "");

  let mounted;
  let unmounted;
  let detailIndex = 0;
  let currentNowMs = nowMs;
  let intervalCallback;
  let nextTimerId = 1;
  const requestedOrderNumbers = [];
  const paidOrderNumbers = [];
  const replacedRoutes = [];
  const confirmationMessages = [];
  const intervalDelays = [];
  const clearedTimerIds = [];
  const route = {
    params: { orderNumber: "AO-123456" },
    fullPath: "/auction-orders/AO-123456/payment",
  };
  const router = {
    replace: async (target) => {
      replacedRoutes.push(target);
    },
  };
  const context = {
    computed,
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
    getAuctionOrderDetail: async (orderNumber) => {
      requestedOrderNumbers.push(orderNumber);
      const outcome = detailOutcomes[
        Math.min(detailIndex, detailOutcomes.length - 1)
      ];
      detailIndex += 1;
      const resolvedOutcome = await outcome;
      if (resolvedOutcome instanceof Error) throw resolvedOutcome;
      return { data: { data: resolvedOutcome } };
    },
    payAuctionOrder: async (orderNumber) => {
      paidOrderNumbers.push(orderNumber);
      return payImplementation(orderNumber);
    },
    handleAuctionAccessError: async (accessError, targetRouter) => {
      if (accessError.response?.status !== 401) return false;
      await targetRouter.replace({ name: "login" });
      return true;
    },
    formatTwd,
    formatAuctionTime,
    parseAuctionTime,
    formatAuctionOrderStatus,
    resolveAuctionImageUrl: (url) => (url ? "resolved:" + url : ""),
    Date: {
      now: () => currentNowMs,
    },
    setInterval: (callback, delay) => {
      intervalCallback = callback;
      intervalDelays.push(delay);
      return nextTimerId++;
    },
    clearInterval: (timerId) => {
      clearedTimerIds.push(timerId);
    },
    window: {
      confirm: (message) => {
        confirmationMessages.push(message);
        if (confirmImplementation) {
          return confirmImplementation(message, (value) => {
            currentNowMs = value;
          });
        }
        return confirmResult;
      },
    },
  };

  vm.runInNewContext(
    script +
      "\nglobalThis.subject = { order, loading, paying, errorMessage, " +
      "paymentErrorMessage, paymentSuccessMessage, imageLoadFailed, " +
      "currentTimeMs, isPendingPayment, isPaid, isFailed, isAbandoned, " +
      "paymentDeadlineMs, hasPaymentDeadline, isPaymentDeadlineReached, canPay, " +
      "remainingPaymentTimeText, paymentStatusText, headlineStatusText, headlineStatusClass, " +
      "productImageUrl, displayValue, detailErrorMessage, paymentRequestErrorMessage, " +
      "loadOrder, refreshOrderDetail, refreshExpiredOrderOnce, tickPaymentDeadline, " +
      "startPaymentDeadlineTimer, stopPaymentDeadlineTimer, isCurrentPaymentView, " +
      "reconcilePaymentState, confirmAndPay };",
    context,
  );

  return {
    ...context.subject,
    mount: () => mounted(),
    unmount: () => unmounted(),
    requestedOrderNumbers,
    paidOrderNumbers,
    replacedRoutes,
    confirmationMessages,
    intervalDelays,
    clearedTimerIds,
    setNow: (value) => {
      currentNowMs = value;
    },
    setRouteOrderNumber: (orderNumber) => {
      route.params.orderNumber = orderNumber;
      route.fullPath = `/auction-orders/${orderNumber}/payment`;
    },
    tick: async () => intervalCallback?.(),
  };
}

test("payment route uses the existing auction payment view", () => {
  assert.match(routerSource, /path:\s*"\/auction-orders\/:orderNumber\/payment"/);
  assert.match(routerSource, /name:\s*"auction-order-payment"/);
  assert.match(
    routerSource,
    /import\("@\/views\/auction\/AuctionOrderPaymentView\.vue"\)/,
  );
});

test("auction service GETs detail and POSTs payment through the shared client with no body", () => {
  assert.match(serviceSource, /import httpClient from ["']\.\/httpClient["']/);
  const service = evaluateAuctionService();

  const detailRequest = service.getAuctionOrderDetail("AO/測試");
  assert.equal(detailRequest.method, "GET");
  assert.equal(detailRequest.url, "/auction-orders/AO%2F%E6%B8%AC%E8%A9%A6");

  const paymentRequest = service.payAuctionOrder("AO/測試");
  assert.equal(paymentRequest.method, "POST");
  assert.equal(
    paymentRequest.url,
    "/auction-orders/AO%2F%E6%B8%AC%E8%A9%A6/pay",
  );
  assert.equal(paymentRequest.args.length, 1);
  assert.equal(paymentRequest.body, undefined);
  assert.doesNotMatch(
    serviceSource.match(/function payAuctionOrder[\s\S]*?\n}/)[0],
    /buyerId|memberId|paymentMethod|finalPrice|shippingFee|totalAmount/,
  );
});

test("payment page loads route detail and keeps API amounts authoritative", async () => {
  const h = paymentViewHarness();
  await h.mount();

  assert.deepEqual(h.requestedOrderNumbers, ["AO-123456"]);
  assert.equal(h.loading.value, false);
  assert.equal(h.errorMessage.value, "");
  assert.equal(h.order.value.productName, "Jordan 1 Retro High");
  assert.equal(formatTwd(h.order.value.finalPrice), "NT$ 6,800");
  assert.equal(formatTwd(h.order.value.shippingFee), "NT$ 100");
  assert.equal(formatTwd(h.order.value.totalAmount), "NT$ 6,900");
  assert.match(paymentViewSource, /formatTwd\(order\.totalAmount\)/);
  assert.doesNotMatch(paymentViewSource, /finalPrice\s*\+\s*order\.shippingFee/);
});

test("pending order enables payment UI while paid order exposes no payment action", async () => {
  const h = paymentViewHarness();
  await h.mount();

  assert.equal(h.isPendingPayment.value, true);
  assert.equal(h.canPay.value, true);
  assert.equal(h.paying.value, false);
  assert.match(paymentViewSource, /v-if="isPendingPayment"/);
  assert.match(paymentViewSource, /:disabled="paying \|\| !canPay"/);
  assert.match(paymentViewSource, /@click="confirmAndPay"/);
  assert.doesNotMatch(paymentViewSource, /payment-coming-soon|付款功能準備中/);

  h.order.value = paidDetail();
  assert.equal(h.isPendingPayment.value, false);
  assert.equal(h.isPaid.value, true);
  assert.equal(h.canPay.value, false);
  assert.equal(h.paymentStatusText.value, "已付款");
});

test("future deadline uses Taipei time, shows a deterministic countdown and permits payment", async () => {
  const h = paymentViewHarness();
  await h.mount();

  assert.match(formatAuctionTime(h.order.value.paymentDueAt), /2026\/09\/16.*20:00/);
  assert.equal(h.hasPaymentDeadline.value, true);
  assert.equal(h.isPaymentDeadlineReached.value, false);
  assert.equal(h.remainingPaymentTimeText.value, "23:59:42");
  assert.equal(h.canPay.value, true);
  assert.match(paymentViewSource, />付款期限</);
  assert.match(paymentViewSource, />剩餘付款時間</);
});

for (const [label, nowMs] of [
  ["exactly at", Date.parse("2026-09-16T12:00:00Z")],
  ["after", Date.parse("2026-09-16T12:00:01Z")],
]) {
  test(label + " the deadline disables payment without sending a request", async () => {
    const h = paymentViewHarness({ nowMs });
    await h.mount();

    assert.equal(h.isPendingPayment.value, true);
    assert.equal(h.isPaymentDeadlineReached.value, true);
    assert.equal(h.canPay.value, false);
    await h.confirmAndPay();
    assert.deepEqual(h.confirmationMessages, []);
    assert.deepEqual(h.paidOrderNumbers, []);
    assert.match(paymentViewSource, /付款期限已到，系統正在更新訂單狀態。/);
  });
}

test("deadline crossing refreshes order detail once without polling", async () => {
  const dueAt = "2026-09-15T20:00:20";
  const pendingAtDeadline = detail({ paymentDueAt: dueAt });
  const h = paymentViewHarness({
    detailOutcomes: [pendingAtDeadline, pendingAtDeadline],
  });
  await h.mount();
  assert.deepEqual(h.requestedOrderNumbers, ["AO-123456"]);

  h.setNow(Date.parse("2026-09-15T12:00:20Z"));
  await h.tick();
  assert.equal(h.isPaymentDeadlineReached.value, true);
  assert.equal(h.canPay.value, false);
  assert.deepEqual(h.requestedOrderNumbers, ["AO-123456", "AO-123456"]);

  h.setNow(Date.parse("2026-09-15T12:00:30Z"));
  await h.tick();
  await h.tick();
  assert.deepEqual(h.requestedOrderNumbers, ["AO-123456", "AO-123456"]);
});

test("an initially overdue pending order refreshes once and then stops", async () => {
  const overdue = detail({ paymentDueAt: "2026-09-15T20:00:00" });
  const h = paymentViewHarness({
    detailOutcomes: [overdue, overdue],
    nowMs: Date.parse("2026-09-15T12:00:05Z"),
  });

  await h.mount();

  assert.equal(h.loading.value, false);
  assert.equal(h.isPaymentDeadlineReached.value, true);
  assert.equal(h.canPay.value, false);
  assert.deepEqual(h.requestedOrderNumbers, ["AO-123456", "AO-123456"]);

  await h.tick();
  await h.tick();
  assert.deepEqual(h.requestedOrderNumbers, ["AO-123456", "AO-123456"]);
});

test("crossing the deadline inside confirmation blocks POST and performs one expiry sync", async () => {
  const dueAt = "2026-09-15T20:00:20";
  const pendingAtDeadline = detail({ paymentDueAt: dueAt });
  const h = paymentViewHarness({
    detailOutcomes: [pendingAtDeadline, pendingAtDeadline],
    confirmImplementation: (_message, setNow) => {
      setNow(Date.parse("2026-09-15T12:00:20Z"));
      return true;
    },
  });
  await h.mount();

  await h.confirmAndPay();

  assert.equal(h.confirmationMessages.length, 1);
  assert.deepEqual(h.paidOrderNumbers, []);
  assert.equal(h.paymentErrorMessage.value, "競標訂單付款期限已過");
  assert.deepEqual(h.requestedOrderNumbers, ["AO-123456", "AO-123456"]);

  await h.tick();
  assert.deepEqual(h.requestedOrderNumbers, ["AO-123456", "AO-123456"]);
});

test("abandoned order shows deadline and abandoned time with no payment action", async () => {
  const h = paymentViewHarness({
    detailOutcomes: [
      detail({
        orderStatus: "ABANDONED",
        paymentStatus: "PENDING",
        abandonedAt: "2026-09-16T20:00:05",
      }),
    ],
    nowMs: Date.parse("2026-09-16T12:00:05Z"),
  });
  await h.mount();

  assert.equal(h.isAbandoned.value, true);
  assert.equal(h.isPendingPayment.value, false);
  assert.equal(h.canPay.value, false);
  assert.equal(h.paymentStatusText.value, "等待付款");
  assert.equal(h.headlineStatusText.value, "已棄標（逾期未付款）");
  assert.equal(h.headlineStatusClass.value, "text-bg-danger");
  assert.match(formatAuctionTime(h.order.value.abandonedAt), /2026\/09\/16.*20:00/);
  assert.match(paymentViewSource, /v-if="order\.abandonedAt"/);
  assert.match(paymentViewSource, /已棄標（逾期未付款），此訂單已無法再完成付款。/);
});

test("legacy order with null deadline remains payable and countdown-safe", async () => {
  const h = paymentViewHarness({
    detailOutcomes: [detail({ paymentDueAt: null })],
    nowMs: Date.parse("2030-01-01T00:00:00Z"),
  });
  await assert.doesNotReject(h.mount);

  assert.equal(h.hasPaymentDeadline.value, false);
  assert.equal(h.isPaymentDeadlineReached.value, false);
  assert.equal(h.remainingPaymentTimeText.value, "—");
  assert.equal(h.canPay.value, true);
  await assert.doesNotReject(h.tick);
  assert.deepEqual(h.requestedOrderNumbers, ["AO-123456"]);
});

test("payment deadline timer is one-second local UX state and is cleared on unmount", async () => {
  const h = paymentViewHarness();
  await h.mount();

  assert.deepEqual(h.intervalDelays, [1000]);
  assert.deepEqual(h.clearedTimerIds, []);
  h.unmount();
  assert.deepEqual(h.clearedTimerIds, [1]);
});

test("confirmed payment POSTs once then reloads detail and renders PAID", async () => {
  const h = paymentViewHarness({ detailOutcomes: [detail(), paidDetail()] });
  await h.mount();
  await h.confirmAndPay();

  assert.deepEqual(h.paidOrderNumbers, ["AO-123456"]);
  assert.deepEqual(h.requestedOrderNumbers, ["AO-123456", "AO-123456"]);
  assert.match(h.confirmationMessages[0], /確認付款/);
  assert.match(h.confirmationMessages[0], /訂單：AO-123456/);
  assert.match(h.confirmationMessages[0], /應付總額：NT\$ 6,900/);
  assert.match(h.confirmationMessages[0], /付款方式：銀行轉帳/);
  assert.equal(h.order.value.paymentStatus, "PAID");
  assert.equal(h.order.value.paidAt, "2026-09-14T11:30:00");
  assert.equal(h.paymentSuccessMessage.value, "付款完成");
  assert.equal(h.paymentErrorMessage.value, "");
  assert.equal(h.isPendingPayment.value, false);
  assert.doesNotMatch(paymentViewSource, /order\.value\.paymentStatus\s*=(?!=)/);
});

test("cancelled confirmation never sends payment", async () => {
  const h = paymentViewHarness({ confirmResult: false });
  await h.mount();
  await h.confirmAndPay();

  assert.equal(h.confirmationMessages.length, 1);
  assert.deepEqual(h.paidOrderNumbers, []);
  assert.deepEqual(h.requestedOrderNumbers, ["AO-123456"]);
});

test("paying state disables repeat submission until the first request settles", async () => {
  const pendingPayment = deferred();
  const h = paymentViewHarness({
    detailOutcomes: [detail(), paidDetail()],
    payImplementation: () => pendingPayment.promise,
  });
  await h.mount();

  const first = h.confirmAndPay();
  await Promise.resolve();
  assert.equal(h.paying.value, true);
  assert.deepEqual(h.paidOrderNumbers, ["AO-123456"]);
  await h.confirmAndPay();
  assert.deepEqual(h.paidOrderNumbers, ["AO-123456"]);
  assert.equal(h.confirmationMessages.length, 1);
  assert.match(
    paymentViewSource,
    /paying \? "付款處理中\.\.\." : isPaymentDeadlineReached \? "付款期限已到" : "立即付款"/,
  );

  pendingPayment.resolve({ data: { data: {} } });
  await first;
  assert.equal(h.paying.value, false);
  assert.equal(h.order.value.paymentStatus, "PAID");
});

test("a payment settling after route change cannot refresh or overwrite the next order", async () => {
  const pendingPayment = deferred();
  const nextOrderDetail = deferred();
  const h = paymentViewHarness({
    detailOutcomes: [
      detail(),
      nextOrderDetail.promise,
      paidDetail(),
    ],
    payImplementation: () => pendingPayment.promise,
  });
  await h.mount();

  const payment = h.confirmAndPay();
  await Promise.resolve();
  h.setRouteOrderNumber("AO-654321");
  const nextLoad = h.loadOrder();
  await Promise.resolve();

  assert.deepEqual(h.requestedOrderNumbers, ["AO-123456", "AO-654321"]);
  assert.equal(h.loading.value, true);

  pendingPayment.resolve({ data: { data: {} } });
  await payment;

  assert.deepEqual(h.requestedOrderNumbers, ["AO-123456", "AO-654321"]);
  assert.equal(h.paymentSuccessMessage.value, "");
  assert.equal(h.paymentErrorMessage.value, "");
  assert.equal(h.loading.value, true);

  nextOrderDetail.resolve(detail({ orderNumber: "AO-654321" }));
  await nextLoad;

  assert.equal(h.loading.value, false);
  assert.equal(h.order.value.orderNumber, "AO-654321");
});

test("a payment failure settling after unmount cannot reconcile or write stale state", async () => {
  const pendingPayment = deferred();
  const h = paymentViewHarness({
    detailOutcomes: [detail(), detail({ orderStatus: "CANCELLED" })],
    payImplementation: () => pendingPayment.promise,
  });
  await h.mount();

  const payment = h.confirmAndPay();
  await Promise.resolve();
  h.unmount();
  pendingPayment.reject(httpError(409));
  await payment;

  assert.deepEqual(h.requestedOrderNumbers, ["AO-123456"]);
  assert.equal(h.paymentSuccessMessage.value, "");
  assert.equal(h.paymentErrorMessage.value, "");
  assert.deepEqual(h.replacedRoutes, []);
  assert.equal(h.paying.value, false);
});

test("409 shows a useful message and reloads backend detail", async () => {
  const h = paymentViewHarness({
    detailOutcomes: [detail(), detail({ orderStatus: "CANCELLED" })],
    payImplementation: async () => {
      throw httpError(409);
    },
  });
  await h.mount();
  await h.confirmAndPay();

  assert.equal(
    h.paymentErrorMessage.value,
    "目前訂單狀態無法付款，請重新整理後確認訂單狀態。",
  );
  assert.deepEqual(h.requestedOrderNumbers, ["AO-123456", "AO-123456"]);
  assert.equal(h.order.value.orderStatus, "CANCELLED");
  assert.equal(h.order.value.paymentStatus, "PENDING");
});

test("payment 401 reuses existing login navigation", async () => {
  const h = paymentViewHarness({
    payImplementation: async () => {
      throw httpError(401);
    },
  });
  await h.mount();
  await h.confirmAndPay();

  assert.deepEqual(
    JSON.parse(JSON.stringify(h.replacedRoutes)),
    [{ name: "login" }],
  );
  assert.equal(h.paymentErrorMessage.value, "");
});

for (const [status, message] of [
  [403, "你沒有權限付款此訂單"],
  [404, "找不到此競標訂單"],
]) {
  test(`payment ${status} renders the expected message`, async () => {
    const h = paymentViewHarness({
      payImplementation: async () => {
        throw httpError(status);
      },
    });
    await h.mount();
    await h.confirmAndPay();

    assert.equal(h.paymentErrorMessage.value, message);
    assert.deepEqual(h.requestedOrderNumbers, ["AO-123456"]);
    assert.equal(h.order.value.paymentStatus, "PENDING");
  });
}

for (const status of [undefined, 500]) {
  test(`${status ?? "network"} payment error reloads detail without setting FAILED`, async () => {
    const h = paymentViewHarness({
      detailOutcomes: [detail(), detail()],
      payImplementation: async () => {
        throw httpError(status);
      },
    });
    await h.mount();
    await h.confirmAndPay();

    assert.equal(h.paymentErrorMessage.value, "付款處理失敗，請稍後再試。");
    assert.deepEqual(h.requestedOrderNumbers, ["AO-123456", "AO-123456"]);
    assert.equal(h.order.value.paymentStatus, "PENDING");
    assert.notEqual(h.order.value.paymentStatus, "FAILED");
  });
}

test("network error reconciles to paid when backend detail shows committed payment", async () => {
  const h = paymentViewHarness({
    detailOutcomes: [detail(), paidDetail()],
    payImplementation: async () => {
      throw httpError(undefined);
    },
  });
  await h.mount();
  await h.confirmAndPay();

  assert.deepEqual(h.requestedOrderNumbers, ["AO-123456", "AO-123456"]);
  assert.equal(h.order.value.paymentStatus, "PAID");
  assert.equal(h.paymentSuccessMessage.value, "付款完成");
  assert.equal(h.paymentErrorMessage.value, "");
  assert.equal(h.isPendingPayment.value, false);
});

test("nullable model, brand, image and paidAt remain valid display states", async () => {
  const h = paymentViewHarness({
    detailOutcomes: [
      detail({ model: null, brandName: null, imageUrl: null, paidAt: null }),
    ],
  });
  await assert.doesNotReject(h.mount);

  assert.equal(h.order.value.model, null);
  assert.equal(h.order.value.brandName, null);
  assert.equal(h.productImageUrl.value, "");
  assert.equal(formatAuctionTime(h.order.value.paidAt), "-");
  assert.equal(h.displayValue(null), "—");
  assert.match(paymentViewSource, /品牌資料未提供/);
  assert.match(paymentViewSource, /型號資料未提供/);
  assert.match(paymentViewSource, /目前沒有商品圖片/);
  assert.match(paymentViewSource, /v-if="order\.paidAt"/);
});

for (const [status, message] of [
  [403, "你沒有權限查看此訂單"],
  [404, "找不到此競標訂單"],
  [500, "訂單資料載入失敗，請稍後再試"],
]) {
  test(`initial detail ${status} renders the payment page error state`, async () => {
    const h = paymentViewHarness({ detailOutcomes: [httpError(status)] });
    await h.mount();
    assert.equal(h.order.value, null);
    assert.equal(h.loading.value, false);
    assert.equal(h.errorMessage.value, message);
    assert.deepEqual(h.replacedRoutes, []);
  });
}

test("initial detail 401 uses existing auction login handling", async () => {
  const h = paymentViewHarness({ detailOutcomes: [httpError(401)] });
  await h.mount();
  assert.equal(h.order.value, null);
  assert.equal(h.errorMessage.value, "");
  assert.deepEqual(
    JSON.parse(JSON.stringify(h.replacedRoutes)),
    [{ name: "login" }],
  );
});

test("order list keeps pending payment and other-state detail links", () => {
  assert.match(
    orderListSource,
    /paymentStatus === 'PENDING' && order\.orderStatus === 'PENDING_PAYMENT'/,
  );
  assert.match(orderListSource, /name: 'auction-order-payment'/);
  assert.match(orderListSource, /orderNumber: order\.orderNumber/);
  assert.match(orderListSource, />\s*前往付款\s*</);
  assert.match(orderListSource, />\s*查看訂單\s*</);
});
