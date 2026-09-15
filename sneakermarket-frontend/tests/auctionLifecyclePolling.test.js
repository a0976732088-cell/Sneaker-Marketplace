import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";
import { computed, ref } from "vue";

// 執行現有 SFC script setup，僅替換路由、HTTP、生命週期掛鉤與計時器。
const source = await readFile(
  new URL("../src/views/auction/AuctionDetailView.vue", import.meta.url),
  "utf8",
);
const script = source
  .match(/<script setup>([\s\S]*?)<\/script>/)[1]
  .replace(/^import[\s\S]*?from\s+["'][^"']+["'];/gm, "");

function harness(statusResponse) {
  let interval;
  let cleared = 0;
  let historyRequests = 0;
  const context = {
    computed,
    ref,
    console,
    parseAuctionTime: (value) => new Date(value),
    formatAuctionTime: (value) => value,
    formatTwd: (value) => String(value),
    useRoute: () => ({ params: { id: 1 } }),
    useRouter: () => ({}),
    onMounted: () => {},
    onUnmounted: () => {},
    setInterval: (callback) => {
      interval = callback;
      return 42;
    },
    clearInterval: (id) => {
      assert.equal(id, 42);
      cleared++;
    },
    getAuctionStatus: async () => ({ data: { data: statusResponse } }),
    getBidHistory: async () => {
      historyRequests++;
      return { data: { data: [{ bidAmount: 5500 }] } };
    },
  };
  // import.meta 只用於圖片網址，測試中仍提供與原程式相同預設值。
  vm.runInNewContext(
    script.replace("import.meta.env.VITE_API_BASE_URL", "undefined") +
      `
    globalThis.subject = {
      auction, bids, canPlaceBid, refreshAuctionStatus,
      startStatusPolling, formatStatus, isOlderStatus
    };
  `,
    context,
  );
  const subject = context.subject;
  subject.auction.value = {
    auctionId: 1,
    status: "ACTIVE",
    bidCount: 0,
    currentPrice: 5000,
    nextMinimumBid: 5500,
    isOwner: false,
    startAt: "2000-01-01T00:00:00+08:00",
    endAt: "2099-01-01T00:00:00+08:00",
    updatedAt: "2026-09-10T11:59:59+08:00",
  };
  return {
    subject,
    tick: () => interval(),
    cleared: () => cleared,
    historyRequests: () => historyRequests,
  };
}

for (const [status, label, bidCount] of [
  ["SOLD", "已結標", 1],
  ["UNSOLD", "流標", 0],
  ["REMOVED", "廢標", 1],
]) {
  test(`existing polling displays ${status}, disables bidding and stops timer`, async () => {
    const h = harness({
      auctionId: 1,
      status,
      bidCount,
      currentPrice: status === "SOLD" ? 5500 : 5000,
      nextMinimumBid: 6000,
      endAt: "2026-09-10T12:00:00+08:00",
      updatedAt: "2026-09-10T12:00:00+08:00",
    });
    assert.equal(h.subject.canPlaceBid.value, true);
    h.subject.startStatusPolling();
    await h.tick();
    assert.equal(h.subject.auction.value.status, status);
    assert.equal(h.subject.formatStatus(status), label);
    assert.equal(h.subject.canPlaceBid.value, false);
    assert.equal(h.cleared(), 1);
    assert.equal(h.historyRequests(), bidCount > 0 ? 1 : 0);
    assert.equal(
      h.subject.isOlderStatus({
        status: "ACTIVE",
        bidCount,
        updatedAt: "2026-09-10T12:00:00+08:00",
      }),
      true,
    );
  });
}

test("existing polling enables bidding when scheduled auction becomes active", async () => {
  const h = harness({
    auctionId: 1,
    status: "ACTIVE",
    bidCount: 0,
    currentPrice: 5000,
    nextMinimumBid: 5500,
    endAt: "2099-01-01T00:00:00+08:00",
    updatedAt: "2026-09-10T12:00:00+08:00",
  });
  h.subject.auction.value.status = "SCHEDULED";
  assert.equal(h.subject.canPlaceBid.value, false);
  h.subject.startStatusPolling();
  await h.tick();
  assert.equal(h.subject.canPlaceBid.value, true);
  assert.equal(h.cleared(), 0);
});
