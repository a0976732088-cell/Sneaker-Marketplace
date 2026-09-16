import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";
import { computed, ref } from "vue";

const source = await readFile(
  new URL("../src/views/auction/AuctionDetailView.vue", import.meta.url),
  "utf8",
);
const script = source
  .match(/<script setup>([\s\S]*?)<\/script>/)[1]
  .replace(/^import[\s\S]*?from\s+["'][^"']+["'];/gm, "")
  .replace("import.meta.env.VITE_API_BASE_URL", "undefined");

const ACTIVE_DETAIL = {
  auctionId: 1,
  productName: "Test Auction",
  status: "ACTIVE",
  bidCount: 0,
  currentPrice: 5000,
  minIncrement: 500,
  nextMinimumBid: 5500,
  isOwner: false,
  canManage: false,
  startAt: "2000-01-01T00:00:00+08:00",
  endAt: "2099-01-01T00:00:00+08:00",
  updatedAt: "2026-09-10T12:00:00+08:00",
};

function createHarness({
  detail = ACTIVE_DETAIL,
  statusResponses = [],
  historyResponses = [],
  placeBidImplementation = null,
} = {}) {
  let mountedCallback;
  let unmountedCallback;
  let socketOptions;
  let socketMessageHandler;
  let subscribedAuctionId;
  let nextTimerId = 0;
  let statusRequests = 0;
  let historyRequests = 0;
  let socketActivations = 0;
  let socketDeactivations = 0;
  let socketUnsubscribes = 0;
  const timers = new Map();
  const clearedTimers = [];
  const routerDestinations = [];
  const queuedStatuses = [...statusResponses];
  const queuedHistories = [...historyResponses];

  const socketClient = {
    subscribeAuction(auctionId, { onMessage }) {
      subscribedAuctionId = auctionId;
      socketMessageHandler = onMessage;
      return () => {
        socketUnsubscribes++;
      };
    },
    activate() {
      socketActivations++;
    },
    async deactivate() {
      socketDeactivations++;
    },
  };

  const context = {
    computed,
    ref,
    console: {
      ...console,
      error() {},
    },
    parseAuctionTime: (value) => new Date(value),
    formatAuctionTime: (value) => value,
    formatTwd: (value) => String(value),
    useRoute: () => ({ params: { id: 1 } }),
    useRouter: () => ({
      replace: async (destination) => {
        routerDestinations.push(destination);
      },
    }),
    onMounted: (callback) => {
      mountedCallback = callback;
    },
    onUnmounted: (callback) => {
      unmountedCallback = callback;
    },
    setInterval: (callback, delay) => {
      const id = ++nextTimerId;
      timers.set(id, { callback, delay });
      return id;
    },
    clearInterval: (id) => {
      clearedTimers.push(id);
      timers.delete(id);
    },
    createAuctionSocketClient: (options) => {
      socketOptions = options;
      return socketClient;
    },
    handleAuctionAccessError: async () => false,
    cancelAuction: async () => {},
    getAuctionDetail: async () => ({
      data: { data: { ...detail } },
    }),
    getAuctionStatus: async () => {
      statusRequests++;
      let next = queuedStatuses.shift();

      if (next?.promise) {
        next = await next.promise;
      }

      if (next?.error) {
        throw next.error;
      }

      return {
        data: {
          data: {
            ...detail,
            ...(next || {}),
          },
        },
      };
    },
    getBidHistory: async () => {
      historyRequests++;
      const next = queuedHistories.shift();

      if (next?.error) {
        throw next.error;
      }

      return {
        data: {
          data:
            next?.items ||
            [{ bidderName: "A", bidAmount: 5500 + historyRequests }],
        },
      };
    },
    placeBid: (...args) =>
      placeBidImplementation
        ? placeBidImplementation(...args)
        : Promise.resolve({ data: { data: { ...detail } } }),
  };

  vm.runInNewContext(
    script +
      `
      globalThis.subject = {
        auction, bids, candidateBid, canPlaceBid, refreshAuctionStatus,
        startStatusPolling, stopStatusPolling, isOlderStatus, handlePlaceBid
      };
    `,
    context,
  );

  return {
    subject: context.subject,
    mount: () => mountedCallback(),
    unmount: () => unmountedCallback(),
    connect: () => socketOptions.onConnected(),
    disconnect: () => socketOptions.onDisconnected(),
    socketError: (error) => socketOptions.onError(error),
    message: (message) => socketMessageHandler(message),
    tickPolling: async () => {
      const [{ callback }] = timers.values();
      await callback();
    },
    timers,
    clearedTimers,
    statusRequests: () => statusRequests,
    historyRequests: () => historyRequests,
    subscribedAuctionId: () => subscribedAuctionId,
    socketActivations: () => socketActivations,
    socketDeactivations: () => socketDeactivations,
    socketUnsubscribes: () => socketUnsubscribes,
    routerDestinations,
  };
}

test("connected socket reconciles once through REST and stops fallback polling", async () => {
  const h = createHarness({
    statusResponses: [
      {
        currentPrice: 6000,
        nextMinimumBid: 6500,
        updatedAt: "2026-09-10T12:01:00+08:00",
      },
    ],
  });

  await h.mount();
  assert.equal(h.timers.size, 1);
  assert.equal([...h.timers.values()][0].delay, 12000);
  assert.equal(h.subscribedAuctionId(), 1);
  assert.equal(h.socketActivations(), 1);

  await h.connect();
  assert.equal(h.statusRequests(), 1);
  assert.equal(h.subject.auction.value.currentPrice, 6000);
  assert.equal(h.timers.size, 0);
});

test("failed reconciliation keeps polling active", async () => {
  const serverError = new Error("status unavailable");
  serverError.response = { status: 500 };
  const h = createHarness({
    statusResponses: [{ error: serverError }],
  });

  await h.mount();
  await h.connect();
  assert.equal(h.statusRequests(), 1);
  assert.equal(h.timers.size, 1);
});

test("disconnect starts fallback polling and reconnect reconciles before stopping it", async () => {
  const h = createHarness({
    statusResponses: [
      {
        currentPrice: 6000,
        nextMinimumBid: 6500,
        updatedAt: "2026-09-10T12:01:00+08:00",
      },
      {
        currentPrice: 6500,
        nextMinimumBid: 7000,
        updatedAt: "2026-09-10T12:02:00+08:00",
      },
    ],
  });

  await h.mount();
  await h.connect();
  assert.equal(h.timers.size, 0);

  h.disconnect();
  assert.equal(h.timers.size, 1);

  await h.connect();
  assert.equal(h.statusRequests(), 2);
  assert.equal(h.subject.auction.value.currentPrice, 6500);
  assert.equal(h.timers.size, 0);

  h.socketError(new Error("transport failed"));
  assert.equal(h.timers.size, 1);
});

test("reconnect waits for pre-subscribe polling then performs a new REST reconciliation", async () => {
  let resolveOldPolling;
  const oldPolling = new Promise((resolve) => {
    resolveOldPolling = resolve;
  });
  const h = createHarness({
    statusResponses: [
      {
        currentPrice: 5000,
        updatedAt: "2026-09-10T12:00:00+08:00",
      },
      { promise: oldPolling },
      {
        currentPrice: 6000,
        nextMinimumBid: 6500,
        updatedAt: "2026-09-10T12:02:00+08:00",
      },
    ],
  });

  await h.mount();
  await h.connect();
  h.disconnect();

  const pendingPolling = h.tickPolling();
  assert.equal(h.statusRequests(), 2);

  const reconnecting = h.connect();
  assert.equal(h.statusRequests(), 2);

  resolveOldPolling({
    currentPrice: 5500,
    nextMinimumBid: 6000,
    updatedAt: "2026-09-10T12:01:00+08:00",
  });
  await pendingPolling;
  await reconnecting;

  assert.equal(h.statusRequests(), 3);
  assert.equal(h.subject.auction.value.currentPrice, 6000);
  assert.equal(h.subject.candidateBid.value, 6500);
  assert.equal(h.timers.size, 0);
});

test("failed post-subscribe reconciliation does not let an old polling response stop fallback", async () => {
  let resolveOldPolling;
  const oldPolling = new Promise((resolve) => {
    resolveOldPolling = resolve;
  });
  const reconciliationError = new Error("reconciliation unavailable");
  reconciliationError.response = { status: 500 };
  const h = createHarness({
    statusResponses: [
      {
        currentPrice: 5000,
        updatedAt: "2026-09-10T12:00:00+08:00",
      },
      { promise: oldPolling },
      { error: reconciliationError },
    ],
  });

  await h.mount();
  await h.connect();
  h.disconnect();

  const pendingPolling = h.tickPolling();
  const reconnecting = h.connect();
  resolveOldPolling({
    currentPrice: 5500,
    nextMinimumBid: 6000,
    updatedAt: "2026-09-10T12:01:00+08:00",
  });

  await pendingPolling;
  await reconnecting;

  assert.equal(h.statusRequests(), 3);
  assert.equal(h.subject.auction.value.currentPrice, 5500);
  assert.equal(h.timers.size, 1);
});

test("BID_PLACED updates price and bid count then refreshes bid history", async () => {
  const h = createHarness();
  await h.mount();
  const initialHistoryRequests = h.historyRequests();

  await h.message({
    eventType: "BID_PLACED",
    data: {
      auctionId: 1,
      currentPrice: 6500,
      bidCount: 1,
      nextMinimumBid: 7000,
      status: "ACTIVE",
      endAt: ACTIVE_DETAIL.endAt,
      updatedAt: "2026-09-10T12:03:00+08:00",
    },
  });

  assert.equal(h.subject.auction.value.currentPrice, 6500);
  assert.equal(h.subject.auction.value.bidCount, 1);
  assert.equal(h.subject.candidateBid.value, 7000);
  assert.equal(h.historyRequests(), initialHistoryRequests + 1);
});

test("failed BID_PLACED history sync temporarily polls and stops after recovery", async () => {
  const historyError = new Error("history unavailable");
  const h = createHarness({
    statusResponses: [
      {
        bidCount: 0,
        updatedAt: "2026-09-10T12:00:00+08:00",
      },
      {
        currentPrice: 6500,
        bidCount: 1,
        nextMinimumBid: 7000,
        updatedAt: "2026-09-10T12:03:00+08:00",
      },
    ],
    historyResponses: [
      { items: [] },
      { error: historyError },
      { items: [{ bidderName: "B", bidAmount: 6500 }] },
    ],
  });
  await h.mount();
  await h.connect();
  assert.equal(h.timers.size, 0);

  await h.message({
    eventType: "BID_PLACED",
    data: {
      auctionId: 1,
      currentPrice: 6500,
      bidCount: 1,
      nextMinimumBid: 7000,
      status: "ACTIVE",
      endAt: ACTIVE_DETAIL.endAt,
      updatedAt: "2026-09-10T12:03:00+08:00",
    },
  });
  assert.equal(h.timers.size, 1);

  await h.tickPolling();
  assert.equal(h.historyRequests(), 3);
  assert.equal(h.timers.size, 0);
});

test("a late older bid POST response cannot overwrite a newer WebSocket bid", async () => {
  let resolveBid;
  const bidResponse = new Promise((resolve) => {
    resolveBid = resolve;
  });
  const h = createHarness({
    placeBidImplementation: () => bidResponse,
  });
  await h.mount();

  const submitting = h.subject.handlePlaceBid();

  await h.message({
    eventType: "BID_PLACED",
    data: {
      auctionId: 1,
      currentPrice: 6000,
      bidCount: 2,
      nextMinimumBid: 6500,
      status: "ACTIVE",
      endAt: ACTIVE_DETAIL.endAt,
      updatedAt: "2026-09-10T12:02:00+08:00",
    },
  });

  resolveBid({
    data: {
      data: {
        auctionId: 1,
        currentPrice: 5500,
        bidCount: 1,
        nextMinimumBid: 6000,
        status: "ACTIVE",
        endAt: ACTIVE_DETAIL.endAt,
        updatedAt: "2026-09-10T12:01:00+08:00",
      },
    },
  });
  await submitting;

  assert.equal(h.subject.auction.value.currentPrice, 6000);
  assert.equal(h.subject.auction.value.bidCount, 2);
  assert.equal(h.subject.candidateBid.value, 6500);
  assert.equal(
    h.subject.auction.value.updatedAt,
    "2026-09-10T12:02:00+08:00",
  );
});

test("older realtime snapshots cannot overwrite newer UI or trigger removal", async () => {
  const h = createHarness({
    detail: {
      ...ACTIVE_DETAIL,
      currentPrice: 7000,
      bidCount: 2,
      nextMinimumBid: 7500,
      updatedAt: "2026-09-10T12:05:00+08:00",
    },
  });
  await h.mount();

  await h.message({
    eventType: "BID_PLACED",
    data: {
      auctionId: 1,
      currentPrice: 6000,
      bidCount: 1,
      nextMinimumBid: 6500,
      status: "ACTIVE",
      endAt: ACTIVE_DETAIL.endAt,
      updatedAt: "2026-09-10T12:04:00+08:00",
    },
  });
  await h.message({
    eventType: "AUCTION_REMOVED",
    data: {
      auctionId: 1,
      currentPrice: 6000,
      bidCount: 1,
      nextMinimumBid: 6500,
      status: "REMOVED",
      endAt: ACTIVE_DETAIL.endAt,
      updatedAt: "2026-09-10T12:04:00+08:00",
    },
  });

  assert.equal(h.subject.auction.value.currentPrice, 7000);
  assert.equal(h.subject.auction.value.status, "ACTIVE");
  assert.equal(h.routerDestinations.length, 0);
});

test("AUCTION_STARTED changes scheduled auction to active", async () => {
  const h = createHarness({
    detail: {
      ...ACTIVE_DETAIL,
      status: "SCHEDULED",
    },
  });
  await h.mount();
  assert.equal(h.subject.canPlaceBid.value, false);

  await h.message({
    eventType: "AUCTION_STARTED",
    data: {
      auctionId: 1,
      currentPrice: 5000,
      bidCount: 0,
      nextMinimumBid: 5500,
      status: "ACTIVE",
      endAt: ACTIVE_DETAIL.endAt,
      updatedAt: "2026-09-10T12:01:00+08:00",
    },
  });

  assert.equal(h.subject.auction.value.status, "ACTIVE");
  assert.equal(h.subject.canPlaceBid.value, true);
});

for (const [eventType, status] of [
  ["AUCTION_SOLD", "SOLD"],
  ["AUCTION_UNSOLD", "UNSOLD"],
  ["AUCTION_CANCELLED", "CANCELLED"],
]) {
  test(`${eventType} applies terminal status and stops polling`, async () => {
    const h = createHarness();
    await h.mount();

    await h.message({
      eventType,
      data: {
        auctionId: 1,
        currentPrice: 5000,
        bidCount: 0,
        nextMinimumBid: 5500,
        status,
        endAt: "2026-09-10T12:10:00+08:00",
        updatedAt: "2026-09-10T12:10:00+08:00",
      },
    });

    assert.equal(h.subject.auction.value.status, status);
    assert.equal(h.subject.canPlaceBid.value, false);
    assert.equal(h.timers.size, 0);
  });
}

test("AUCTION_REMOVED cleans up and returns to the public auction list", async () => {
  const h = createHarness();
  await h.mount();
  const initialHistoryRequests = h.historyRequests();

  await h.message({
    eventType: "AUCTION_REMOVED",
    data: {
      auctionId: 1,
      currentPrice: 5000,
      bidCount: 1,
      nextMinimumBid: 5500,
      status: "ACTIVE",
      endAt: ACTIVE_DETAIL.endAt,
      updatedAt: "2026-09-10T12:11:00+08:00",
    },
  });

  assert.equal(h.timers.size, 0);
  assert.equal(h.socketUnsubscribes(), 1);
  assert.equal(h.socketDeactivations(), 1);
  assert.equal(h.historyRequests(), initialHistoryRequests);
  assert.equal(h.routerDestinations.length, 1);
  assert.equal(h.routerDestinations[0].name, "auction");
});

test("terminal detail keeps the socket subscription without polling and handles removal", async () => {
  const h = createHarness({
    detail: {
      ...ACTIVE_DETAIL,
      status: "SOLD",
      updatedAt: "2026-09-10T12:10:00+08:00",
    },
  });
  await h.mount();

  assert.equal(h.timers.size, 0);
  assert.equal(h.subscribedAuctionId(), 1);
  assert.equal(h.socketActivations(), 1);

  await h.message({
    eventType: "AUCTION_REMOVED",
    data: {
      auctionId: 1,
      currentPrice: 5000,
      bidCount: 0,
      nextMinimumBid: 5500,
      status: "REMOVED",
      endAt: ACTIVE_DETAIL.endAt,
      updatedAt: "2026-09-10T12:11:00+08:00",
    },
  });

  assert.equal(h.timers.size, 0);
  assert.equal(h.socketUnsubscribes(), 1);
  assert.equal(h.socketDeactivations(), 1);
  assert.equal(h.routerDestinations.length, 1);
  assert.equal(h.routerDestinations[0].name, "auction");
});

test("polling 404 performs the same removed-auction cleanup", async () => {
  const notFound = new Error("removed");
  notFound.response = { status: 404 };
  const h = createHarness({
    statusResponses: [{ error: notFound }],
  });
  await h.mount();

  await h.connect();

  assert.equal(h.timers.size, 0);
  assert.equal(h.socketUnsubscribes(), 1);
  assert.equal(h.socketDeactivations(), 1);
  assert.equal(h.routerDestinations.length, 1);
  assert.equal(h.routerDestinations[0].name, "auction");
});

test("unmount clears polling, unsubscribes and deactivates the socket", async () => {
  const h = createHarness();
  await h.mount();
  assert.equal(h.timers.size, 1);

  h.unmount();

  assert.equal(h.timers.size, 0);
  assert.equal(h.socketUnsubscribes(), 1);
  assert.equal(h.socketDeactivations(), 1);

  h.disconnect();
  assert.equal(h.timers.size, 0);
});
