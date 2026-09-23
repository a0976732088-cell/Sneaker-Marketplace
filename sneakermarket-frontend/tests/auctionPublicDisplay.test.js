import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";
import { computed, ref } from "vue";

const listSource = await readFile(
  new URL("../src/views/auction/AuctionListView.vue", import.meta.url),
  "utf8",
);
const cardSource = await readFile(
  new URL("../src/components/auction/list/AuctionProductCard.vue", import.meta.url),
  "utf8",
);
const detailSource = await readFile(
  new URL("../src/views/auction/AuctionDetailView.vue", import.meta.url),
  "utf8",
);
const summarySource = await readFile(
  new URL("../src/components/auction/detail/AuctionSummaryPanel.vue", import.meta.url),
  "utf8",
);
const bidPanelSource = await readFile(
  new URL("../src/components/auction/detail/AuctionBidPanel.vue", import.meta.url),
  "utf8",
);
const sellerActionSource = await readFile(
  new URL("../src/components/auction/detail/AuctionSellerActionPanel.vue", import.meta.url),
  "utf8",
);
const bidHistorySource = await readFile(
  new URL("../src/components/auction/detail/AuctionBidHistory.vue", import.meta.url),
  "utf8",
);
const editSource = await readFile(
  new URL("../src/views/auction/AuctionEditView.vue", import.meta.url),
  "utf8",
);

function section(source, startMarker, endMarker) {
  const start = source.indexOf(startMarker);
  const end = source.indexOf(endMarker, start);
  assert.notEqual(start, -1, startMarker);
  assert.notEqual(end, -1, endMarker);
  return source.slice(start, end);
}

function scriptSetup(source) {
  return source.match(/<script setup>([\s\S]*?)<\/script>/)[1]
    .replace(/^import[\s\S]*?from\s+["'][^"']+["'];/gm, "");
}

function memberExpression(source, scopeName, fieldName) {
  const binding = scopeName + "." + fieldName;
  const start = source.indexOf("{{ " + binding);
  assert.notEqual(start, -1, binding + " binding");
  const end = source.indexOf("}}", start);
  assert.notEqual(end, -1, binding + " binding end");
  return source.slice(start + 2, end).trim();
}

function evaluate(expression, scopeName, value) {
  return Function(scopeName, "return (" + expression + ");")(value);
}

async function loadListFromApi(items) {
  const context = {
    ref,
    console,
    parseAuctionTime: (value) => new Date(value),
    formatAuctionTime: (value) => value,
    formatTwd: (value) => String(value),
    onMounted: () => {},
    onUnmounted: () => {},
    setInterval: () => 1,
    clearInterval: () => {},
    getAuctions: async () => ({
      data: {
        data: {
          items,
          page: 0,
          size: 5,
          totalElements: items.length,
          totalPages: items.length ? 1 : 0,
        },
      },
    }),
    getAuctionOptions: async () => ({
      data: { data: { brands: [], categories: [] } },
    }),
  };
  vm.runInNewContext(
    scriptSetup(listSource)
      + "\nglobalThis.subject = { auctions, loadAuctions };",
    context,
  );
  await context.subject.loadAuctions();
  return context.subject.auctions.value;
}

async function loadDetailFromApi(detail, bidItems = []) {
  const context = {
    computed,
    ref,
    console,
    parseAuctionTime: (value) => new Date(value),
    formatAuctionTime: (value) => value,
    formatTwd: (value) => String(value),
    useRoute: () => ({ params: { id: 12 } }),
    useRouter: () => ({}),
    onMounted: () => {},
    onUnmounted: () => {},
    setInterval: () => 1,
    clearInterval: () => {},
    handleAuctionAccessError: async () => false,
    cancelAuction: async () => {},
    getAuctionDetail: async () => ({ data: { data: detail } }),
    getAuctionStatus: async () => ({ data: { data: {} } }),
    getBidHistory: async () => ({ data: { data: bidItems } }),
    placeBid: async () => ({ data: { data: {} } }),
  };
  const runnableScript = scriptSetup(detailSource)
    .replace("import.meta.env.VITE_API_BASE_URL", "undefined");
  vm.runInNewContext(
    runnableScript
      + "\nglobalThis.subject = { auction, bids, canOwnerManageAuction, loadAuctionDetail };",
    context,
  );
  await context.subject.loadAuctionDetail();
  return context.subject;
}

const listCard = section(cardSource, "<template>", "</template>");
const detailInfo = section(summarySource, "<template>", "</template>");
const bidHistory = section(bidHistorySource, "<template>", "</template>");
const legacyBidField = ["masked", "Bidder"].join("");
const legacyFallback = ["匿名", "會員"].join("");

test("public and member auction views do not present DRAFT as a supported status", () => {
  const draftLabel = /DRAFT\s*:\s*["']草稿["']/;
  assert.doesNotMatch(listSource, draftLabel);
  assert.doesNotMatch(cardSource, draftLabel);
  assert.doesNotMatch(detailSource, draftLabel);
  for (const childSource of [summarySource, bidPanelSource, sellerActionSource, bidHistorySource]) {
    assert.doesNotMatch(childSource, draftLabel);
  }
  assert.doesNotMatch(editSource, draftLabel);
});

test("auction list binds the seller label to sellerName from its API response", async () => {
  const [auction] = await loadListFromApi([{ sellerName: "王小明" }]);
  assert.match(listSource, /<AuctionProductCard\b[^>]*:auction=["']auction["']/);
  assert.match(listCard, /賣家：/);
  const expression = memberExpression(listCard, "auction", "sellerName");
  assert.equal(evaluate(expression, "auction", auction), "王小明");
  assert.doesNotMatch(listCard, /auction\.sellerId/);
});

test("auction list uses the unified fallback for null and empty API sellerName", async () => {
  const auctions = await loadListFromApi([{ sellerName: null }, { sellerName: "" }]);
  const expression = memberExpression(listCard, "auction", "sellerName");
  assert.deepEqual(
    auctions.map((auction) => evaluate(expression, "auction", auction)),
    ["會員資料未提供", "會員資料未提供"],
  );
});

test("auction detail displays API sellerName and preserves owner controls", async () => {
  const subject = await loadDetailFromApi({
    sellerName: "陳小華",
    isOwner: true,
    canManage: true,
    bidCount: 0,
    status: "ACTIVE",
    nextMinimumBid: 5500,
    startAt: "2000-01-01T00:00:00+08:00",
    endAt: "2099-01-01T00:00:00+08:00",
  });
  assert.match(detailInfo, /賣家：/);
  const expression = memberExpression(detailInfo, "auction", "sellerName");
  assert.equal(evaluate(expression, "auction", subject.auction.value), "陳小華");
  assert.equal(subject.canOwnerManageAuction.value, true);
  assert.match(detailSource, /<AuctionSummaryPanel\b[\s\S]*?:auction="auction"/);
  assert.match(detailSource, /<AuctionSellerActionPanel\s+v-if="auction\.isOwner"/);
  assert.match(detailSource, /:can-manage="canOwnerManageAuction"/);
  assert.match(sellerActionSource, /v-if="canManage"/);
  assert.match(detailSource, /@submit="handlePlaceBid"/);
  assert.match(bidPanelSource, /v-if="auction\.isOwner"/);
  assert.match(bidPanelSource, /<form v-else @submit\.prevent="\$emit\(\x27submit\x27\)">/);
  assert.doesNotMatch(detailInfo, /auction\.sellerId/);
});

test("auction detail uses the unified fallback for null and empty API sellerName", async () => {
  const expression = memberExpression(detailInfo, "auction", "sellerName");
  for (const sellerName of [null, ""]) {
    const subject = await loadDetailFromApi({ sellerName, nextMinimumBid: 5500 });
    assert.equal(
      evaluate(expression, "auction", subject.auction.value),
      "會員資料未提供",
    );
  }
});

test("bid history rows bind each API item to bidderName", async () => {
  const subject = await loadDetailFromApi(
    { sellerName: "賣家", nextMinimumBid: 5500 },
    [{ bidderName: "王小明" }, { bidderName: "陳小華" }],
  );
  assert.match(bidHistory, /<th>出價者<\/th>/);
  const expression = memberExpression(bidHistory, "bid", "bidderName");
  assert.deepEqual(
    Array.from(subject.bids.value, (bid) => evaluate(expression, "bid", bid)),
    ["王小明", "陳小華"],
  );
  assert.equal(bidHistory.includes(legacyBidField), false);
});

test("bid history uses the unified fallback without the legacy anonymous label", async () => {
  const subject = await loadDetailFromApi(
    { sellerName: "賣家", nextMinimumBid: 5500 },
    [{ bidderName: null }, { bidderName: "" }],
  );
  const expression = memberExpression(bidHistory, "bid", "bidderName");
  assert.deepEqual(
    Array.from(subject.bids.value, (bid) => evaluate(expression, "bid", bid)),
    ["會員資料未提供", "會員資料未提供"],
  );
  assert.equal(bidHistory.includes(legacyFallback), false);
});
