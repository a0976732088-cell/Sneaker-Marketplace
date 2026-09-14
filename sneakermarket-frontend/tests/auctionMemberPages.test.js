import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";
import { ref } from "vue";
import { formatMyBidOutcome, formatMemberAuctionStatus, formatMemberManagementState, formatMemberPublicDetailNotice, formatAuctionOrderStatus } from "../src/utils/auctionMemberStatus.js";

const source = await readFile(new URL("../src/components/auction/AuctionMemberPage.vue", import.meta.url), "utf8");
const script = source.match(/<script setup>([\s\S]*?)<\/script>/)[1]
  .replace(/^import[\s\S]*?from\s+["'][^"']+["'];/gm, "");
const serviceSource = (await readFile(new URL("../src/service/auctionService.js", import.meta.url), "utf8"))
  .replace(/^import[^\n]+\n/gm, "").replace(/^export /gm, "");
const serviceContext = { httpClient: { get: (url, config) => ({ url, config }) } };
vm.runInNewContext(serviceSource + "\nglobalThis.service = { getMyBids, getMyListings, getMyAuctionOrders, getMyAuctionSales, handleAuctionAccessError };", serviceContext);

function harness(fetchPage) {
  let unmount;
  const routes = [];
  const context = {
    ref, defineProps: () => ({ fetchPage, errorMessageFallback: "無法載入資料，請稍後重試" }),
    useRouter: () => ({ replace: async (route) => routes.push(route.name) }),
    onMounted: () => {},
    onUnmounted: (callback) => { unmount = callback; },
    handleAuctionAccessError: serviceContext.service.handleAuctionAccessError,
  };
  vm.runInNewContext(script + "\nglobalThis.subject = { items, loading, errorMessage, currentPage, totalPages, totalElements, loadPage };", context);
  return { ...context.subject, routes, unmount: () => unmount() };
}
function response(items = [], page = 0, totalElements = items.length, totalPages = items.length ? 1 : 0) {
  return { data: { data: { items, page, size: 10, totalElements, totalPages } } };
}

for (const [method, url] of [
  ["getMyBids", "/auctions/me/bids"],
  ["getMyListings", "/auctions/me/listings"],
  ["getMyAuctionOrders", "/auction-orders/me"],
  ["getMyAuctionSales", "/auction-orders/me/sales"],
]) {
  test(method + " uses existing client and strips every supplied member identity", () => {
    const request = serviceContext.service[method]({
      page: 2, size: 10, memberId: 99, bidderId: 99, sellerId: 99,
      buyerId: 99, winnerId: 99, highestBidderId: 99,
    });
    assert.equal(request.url, url);
    assert.deepEqual(JSON.parse(JSON.stringify(request.config.params)), { page: 2, size: 10 });
    assert.deepEqual(JSON.parse(JSON.stringify(serviceContext.service[method]().config.params)), { page: 0, size: 10 });
  });
}

test("loading prevents overlapping requests and successful pagination replaces the current page", async () => {
  let resolve;
  let calls = 0;
  const h = harness((params) => {
    calls++;
    assert.equal(params.page, 1);
    return new Promise((done) => { resolve = done; });
  });
  const pending = h.loadPage(1);
  assert.equal(h.loading.value, true);
  await h.loadPage(2);
  assert.equal(calls, 1);
  resolve(response([{ auctionId: 22 }], 1, 11, 2));
  await pending;
  assert.equal(h.loading.value, false);
  assert.equal(h.currentPage.value, 1);
  assert.equal(h.totalElements.value, 11);
  assert.equal(h.totalPages.value, 2);
  assert.equal(h.items.value[0].auctionId, 22);
});

test("HTTP 200 empty items is a normal empty state", async () => {
  const h = harness(async () => response());
  await h.loadPage();
  assert.equal(h.items.value.length, 0);
  assert.equal(h.errorMessage.value, "");
  assert.equal(h.totalPages.value, 0);
});

test("errors clear previous private rows and retry can recover", async () => {
  let fail = false;
  const h = harness(async () => {
    if (fail) throw { response: { status: 500, data: { message: "暫時無法查詢" } } };
    return response([{ auctionId: 1 }]);
  });
  await h.loadPage();
  fail = true;
  await h.loadPage();
  assert.equal(h.items.value.length, 0);
  assert.equal(h.errorMessage.value, "暫時無法查詢");
  fail = false;
  await h.loadPage();
  assert.equal(h.items.value.length, 1);
  assert.equal(h.errorMessage.value, "");
});

for (const [status, destination] of [[401, "login"], [403, "forbidden"]]) {
  test(status + " uses existing access handler and clears private rows", async () => {
    const h = harness(async () => { throw { response: { status } }; });
    h.items.value = [{ auctionId: 1 }];
    await h.loadPage();
    assert.deepEqual(h.routes, [destination]);
    assert.equal(h.items.value.length, 0);
    assert.equal(h.loading.value, false);
  });
}

test("late response after leaving page cannot restore private rows", async () => {
  let resolve;
  const h = harness(() => new Promise((done) => { resolve = done; }));
  const pending = h.loadPage();
  h.unmount();
  resolve(response([{ auctionId: 1 }]));
  await pending;
  assert.equal(h.items.value.length, 0);
});

test("new member page loads only the current session response without a shared cache", async () => {
  const memberA = harness(async () => response([{ auctionId: 1 }]));
  await memberA.loadPage();
  memberA.unmount();
  const memberB = harness(async () => response([{ auctionId: 2 }]));
  assert.equal(memberB.items.value.length, 0);
  await memberB.loadPage();
  assert.deepEqual(memberB.items.value.map((row) => row.auctionId), [2]);
});

test("bid outcomes distinguish leading, outbid, won and lost without depending on order", () => {
  assert.equal(formatMyBidOutcome({ status: "ACTIVE", managementState: "VISIBLE", leading: true }), "目前領先");
  assert.equal(formatMyBidOutcome({ status: "ACTIVE", managementState: "VISIBLE", leading: false }), "已被超價");
  assert.equal(formatMyBidOutcome({ status: "SOLD", resultStatus: "SOLD", won: true }), "已得標");
  assert.equal(formatMyBidOutcome({ status: "SOLD", resultStatus: "SOLD", won: false }), "未得標");
  for (const [status, label] of [["UNSOLD", "流標"], ["CANCELLED", "已取消"], ["REMOVED", "廢標"]]) {
    assert.equal(formatMyBidOutcome({ status }), label);
    assert.equal(formatMemberAuctionStatus(status), label);
  }
  assert.equal(formatMyBidOutcome({ status: "ACTIVE", managementState: "REMOVED", leading: true }), "已由平台下架");
  assert.equal(formatMyBidOutcome({ status: "REMOVED", resultStatus: "REMOVED", managementState: "REMOVED", leading: true, won: false }), "廢標");
  assert.equal(formatMyBidOutcome({ status: "ACTIVE", resultStatus: "REMOVED", managementState: "REMOVED", leading: true, won: true }), "廢標");
  assert.equal(formatMyBidOutcome({ status: "REMOVED", resultStatus: "REMOVED", managementState: "DELETED", leading: true, won: true }), "已由平台刪除");
  assert.equal(formatMyBidOutcome({ status: "ACTIVE", managementState: "DELETED", leading: true }), "已由平台刪除");
  assert.equal(formatMemberManagementState("REMOVED"), "已由平台下架");
  assert.equal(formatMemberManagementState("DELETED"), "已由平台刪除");
  assert.equal(formatMemberPublicDetailNotice("REMOVED"), "已由平台下架，公開詳細頁不可查看");
  assert.equal(formatMemberPublicDetailNotice("DELETED"), "已由平台刪除，公開詳細頁不可查看");
  assert.equal(formatMemberAuctionStatus(null), "尚未結標");
  assert.equal(formatAuctionOrderStatus("PENDING_PAYMENT"), "待付款");
  assert.equal(formatAuctionOrderStatus("NOT_SHIPPED"), "未出貨");
});

test("member views wire the shared page and use existing money/time formatters", async () => {
  for (const [file, api] of [["AuctionMyBidsView", "getMyBids"], ["AuctionMyListingsView", "getMyListings"]]) {
    const view = await readFile(new URL("../src/views/auction/" + file + ".vue", import.meta.url), "utf8");
    assert.ok(view.startsWith("<template>"));
    assert.ok(view.includes(':fetch-page="' + api + '"'));
    assert.ok(view.includes("formatTwd("));
    assert.ok(view.includes("formatAuctionTime("));
    assert.ok(view.includes("查看競標"));
  }

  const orderView = await readFile(new URL("../src/views/auction/AuctionOrderListView.vue", import.meta.url), "utf8");
  assert.ok(orderView.startsWith("<template>"));
  assert.ok(orderView.includes(':fetch-page="fetchActiveOrders"'));
  assert.ok(orderView.includes("getMyAuctionOrders(params)"));
  assert.ok(orderView.includes("getMyAuctionSales(params)"));
  assert.ok(orderView.includes("formatTwd("));
  assert.ok(orderView.includes("formatAuctionTime("));
  assert.ok(orderView.includes("查看競標"));
});
test("member bid and listing views link public detail only for visible auctions", async () => {
  for (const file of ["AuctionMyBidsView", "AuctionMyListingsView"]) {
    const view = await readFile(new URL("../src/views/auction/" + file + ".vue", import.meta.url), "utf8");
    const item = file === "AuctionMyBidsView" ? "bid" : "listing";
    assert.ok(view.includes("managementState === 'VISIBLE'"));
    assert.ok(view.includes("formatMemberPublicDetailNotice(" + item + ".managementState)"));
  }
});
