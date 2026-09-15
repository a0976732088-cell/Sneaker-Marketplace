import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";
import { computed, reactive, ref } from "vue";
import { buildAdminAuctionTimeline } from "../src/utils/auctionAdminLifecycle.js";
import {
  formatAuctionOrderStatus,
  formatMemberAuctionStatus,
} from "../src/utils/auctionMemberStatus.js";
import {
  formatAuctionTime,
  parseAuctionTime,
} from "../src/utils/auctionTime.js";

const servicePath = new URL("../src/service/auctionAdminService.js", import.meta.url);
const viewPath = new URL("../src/views/admin/AdminAuctionListView.vue", import.meta.url);
const routerPath = new URL("../src/router/index.js", import.meta.url);
const homePath = new URL("../src/views/admin/AdminHomeView.vue", import.meta.url);
const detailPath = new URL("../src/views/admin/AdminAuctionDetailView.vue", import.meta.url);
const imageUrlPath = new URL("../src/utils/auctionImageUrl.js", import.meta.url);

const [service, view, router, home, detail, imageUrl] = await Promise.all([
  readFile(servicePath, "utf8"),
  readFile(viewPath, "utf8"),
  readFile(routerPath, "utf8"),
  readFile(homePath, "utf8"),
  readFile(detailPath, "utf8"),
  readFile(imageUrlPath, "utf8"),
]);

test("admin auction service reuses httpClient and implements list, detail and mutation APIs", () => {
  assert.match(service, /import httpClient from ["']\.\/httpClient["']/);
  assert.doesNotMatch(service, /localhost:8080/);
  for (const endpoint of [
    "/admin/auctions",
    "/admin/auctions/",
    "/remove",
    "/restore",
    "/delete",
    "/admin/auctions/batch/remove",
    "/admin/auctions/batch/restore",
    "/admin/auctions/batch/delete",
    "getAdminAuctionDetail",
  ]) assert.ok(service.includes(endpoint), endpoint);
  assert.match(service, /restoreAdminAuctionsBatch[\s\S]*?\{ auctionIds \}/);
});

test("admin auction route is guarded and AdminHome links to the management page", () => {
  assert.match(router, /path: ["']\/admin\/auctions["'][\s\S]*?requiresAdmin: true/);
  assert.match(home, /title: ["']競標系統["'][\s\S]*?name: ["']admin-auctions["']/);
  assert.match(home, /管理競標、下架／恢復顯示與批量刪除/);
  assert.doesNotMatch(home, /批次假刪除/);
});

test("admin views use shared status and time formatters and keep required SFC order", () => {
  assert.ok(view.indexOf("<template>") < view.indexOf("<script setup>"));
  for (const text of [
    "競標狀態",
    "訂單狀態",
    "管理狀態",
    "全選目前頁",
    "上一頁",
    "下一頁",
  ]) {
    assert.ok(view.includes(text), text);
  }
  assert.match(view, /formatAuctionOrderStatus/);
  assert.match(view, /formatAuctionTime/);
  assert.match(detail, /完整生命週期/);
  assert.match(detail, /formatAuctionTime/);
  assert.doesNotMatch(view, /new Date\(/);
  assert.doesNotMatch(detail, /new Date\(/);
  assert.doesNotMatch(view, /假刪除/);
});

test("admin status filter excludes reserved DRAFT while defensive handling remains", () => {
  const statusFilter = view.match(/const statuses = \[([\s\S]*?)\];/)?.[1] ?? "";
  assert.doesNotMatch(statusFilter, /["']DRAFT["']/);
  assert.match(view, /formatMemberAuctionStatus/);
  assert.equal(formatMemberAuctionStatus("DRAFT"), "草稿");
  assert.match(view, /const restorableStatuses = new Set\(\["DRAFT", "SCHEDULED", "ACTIVE"\]\)/);
});

test("single action buttons follow visible removed and deleted states", () => {
  assert.match(view, /v-if="isLegacyRemoved\(auction\)"[\s\S]*?>\s*補登下架資訊\s*<\/button\s*>/);
  assert.match(view, /v-else-if="!auction\.removedAt && !auction\.deletedAt"[\s\S]*?>\s*下架\s*<\/button\s*>/);
  assert.match(view, /v-if="canRestoreAuction\(auction\)"[\s\S]*?btn-success[\s\S]*?>\s*恢復顯示\s*<\/button\s*>/);
  assert.match(view, /v-if="auction\.removedAt && !auction\.deletedAt"[\s\S]*?btn-danger[\s\S]*?>\s*刪除\s*<\/button\s*>/);
  assert.match(view, /v-if="auction\.deletedAt"[\s\S]*?>\s*已刪除\s*<\/span\s*>/);
  assert.match(view, /\.action-button[\s\S]*?box-shadow/);
});

test("action dialog requires confirmation and reasons only for remove or delete", () => {
  assert.match(view, /<Teleport to="body">/);
  assert.match(view, /role="dialog"/);
  assert.match(view, /v-if="dialog\.action !== 'restore'"/);
  assert.match(view, /id="auction-action-reason"[\s\S]*?maxlength="500"/);
  assert.match(view, /v-model="dialog\.confirmed"/);
  assert.match(view, /:disabled="!dialogValid \|\| submitting"/);
  assert.match(view, /@submit\.prevent="confirmAction"/);
  assert.match(view, /dialog\.confirmed[\s\S]*?dialog\.action === "restore"[\s\S]*?dialog\.reason\.trim\(\)\.length > 0/);
  assert.doesNotMatch(view, /id="admin-action-reason"/);
});

test("batch controls appear only after entering batch mode", () => {
  assert.match(view, /v-if="!batchMode"[\s\S]*?@click="enterBatchMode"[\s\S]*?>\s*批量操作\s*<\/button\s*>/);
  assert.match(view, /<div(?=[^>]*\bv-else\b)[^>]*>[\s\S]*?openBatchAction\('remove'\)[\s\S]*?>\s*批量下架\s*<\/button\s*>/);
  assert.match(view, /openBatchAction\('restore'\)[\s\S]*?>\s*批量恢復顯示\s*<\/button\s*>/);
  assert.match(view, /openBatchAction\('delete'\)[\s\S]*?>\s*批量刪除\s*<\/button\s*>/);
  assert.match(view, /@click="exitBatchMode"[\s\S]*?>\s*結束批量操作\s*<\/button\s*>/);
  assert.match(view, /const restorableStatuses = new Set\(\["DRAFT", "SCHEDULED", "ACTIVE"\]\)/);
  assert.match(view, /const canBatchRestore = computed\([\s\S]*?every\(canRestoreAuction\)/);
  assert.match(view, /const canBatchDelete = computed\([\s\S]*?auction\.removedAt && !auction\.deletedAt/);
});

test("admin view prevents duplicate submissions and shows per-item batch results", () => {
  assert.ok(view.includes("submitting.value || loading.value"));
  assert.match(view, /if \(!dialogValid\.value \|\| submitting\.value\) return/);
  assert.match(view, /restoreAdminAuctionsBatch\(ids\)/);
  assert.match(view, /restoreAdminAuction\(ids\[0\]\)/);
  assert.match(view, /result\.items/);
  assert.match(view, /item\.success/);
});

test("admin list shows clear selectors, thumbnails, detail links and 15-row numbering", () => {
  assert.ok(view.includes('class="form-check-input batch-checkbox"'));
  assert.ok(view.includes("border: 2px solid #334155"));
  assert.match(view, /v-if="\s*auction\.imageUrl && !brokenImageIds\.has\(auction\.auctionId\)\s*"/);
  assert.ok(view.includes(':src="resolveAuctionImageUrl(auction.imageUrl)"'));
  assert.ok(view.includes("name: 'admin-auction-detail'"));
  assert.ok(view.includes('v-for="(auction, index) in items"'));
  assert.ok(view.includes("pagination.page * pagination.size + index + 1"));
  assert.ok(view.includes("size: 15"));
  assert.match(
    view,
    /auction\.sellerName\s*\|\|\s*"會員資料未提供"[\s\S]*?auction\.sellerId/,
  );
  assert.match(
    view,
    /v-if="auction\.orderStatus"[\s\S]*?formatAuctionOrderStatus\(auction\.orderStatus\)[\s\S]*?v-else[\s\S]*?—/,
  );
  assert.match(view, /batchMode\.value \? 10 : 9/);
});

test("admin list order-status filter uses every real AuctionOrderStatus", () => {
  assert.match(
    view,
    /id="admin-order-status"[\s\S]*?v-model="filters\.orderStatus"[\s\S]*?v-for="status in orderStatuses"[\s\S]*?formatAuctionOrderStatus\(status\)/,
  );
  const configured = view.match(/const orderStatuses = \[([\s\S]*?)\];/)?.[1] ?? "";
  for (const status of [
    "PENDING_PAYMENT",
    "PAID",
    "SHIPPED",
    "COMPLETED",
    "CANCELLED",
    "ABANDONED",
  ]) {
    assert.match(configured, new RegExp(`"${status}"`), status);
  }
  assert.doesNotMatch(configured, /FAILED|NOT_SHIPPED|DELIVERED/);
});

test("admin list keeps filters readable and scrolls a non-breaking table when zoomed", () => {
  assert.match(view, /class="col-12 col-xl-4"/);
  assert.equal((view.match(/class="col-12 col-md-4 col-xl-2"/g) || []).length, 3);
  assert.match(view, /class="col-12 col-xl-2 d-grid"/);
  assert.match(view, /table admin-auction-table table-hover/);
  assert.match(view, /\.admin-auction-table\s*{\s*min-width:\s*1380px/);
  assert.match(
    view,
    /\.admin-auction-table thead th[\s\S]*?white-space:\s*nowrap[\s\S]*?word-break:\s*keep-all/,
  );
  for (const column of [
    "price-column",
    "status-column",
    "time-column",
    "action-column",
  ]) {
    assert.match(
      view,
      new RegExp(`\\.${column}\\s*\\{[\\s\\S]*?white-space:\\s*nowrap`),
      column,
    );
  }
  assert.match(view, /class="d-flex flex-nowrap justify-content-end gap-2"/);
  assert.match(view, /\.auction-name-link\s*{[\s\S]*?overflow-wrap:\s*anywhere/);
});

test("admin detail remains available after moderation and resolves images against backend", () => {
  assert.match(router, /path: ["']\/admin\/auctions\/:id["'][\s\S]*?requiresAdmin: true/);
  assert.ok(detail.includes("getAdminAuctionDetail"));
  assert.ok(detail.includes("公開中、已下架及已刪除"));
  assert.ok(detail.includes("resolveAuctionImageUrl(imageUrl)"));
  assert.match(
    detail,
    /賣家[\s\S]*?auction\.sellerName\s*\|\|\s*"會員資料未提供"[\s\S]*?賣家 ID[\s\S]*?auction\.sellerId/,
  );
  assert.ok(imageUrl.includes('import httpClient from "@/service/httpClient"'));
  assert.ok(imageUrl.includes('replace(/\\/api\\/?$/, "")'));
  assert.ok(view.includes('@error="markThumbnailBroken(auction.auctionId)"'));
});

test("admin detail renders bid result order and lifecycle aggregate sections", () => {
  for (const text of [
    "出價紀錄",
    "競標結果",
    "競標訂單",
    "完整生命週期",
    "尚未產生競標結果",
    "尚未建立競標訂單",
    "付款期限",
    "棄標時間",
  ]) {
    assert.ok(detail.includes(text), text);
  }
  assert.match(detail, /v-for="\(bid, index\) in bids"/);
  assert.match(detail, /result\.winnerName/);
  assert.match(detail, /order\.buyerName/);
  assert.match(detail, /formatAuctionOrderStatus\(order\.orderStatus\)/);
  assert.match(detail, /order\.orderStatus === 'ABANDONED'/);
  assert.doesNotMatch(detail, /receiverPhone|shippingAddress|buyerEmail|sellerEmail/);
});

test("ACTIVE lifecycle shows persisted schedule only and keeps result and order absent", () => {
  const timeline = buildAdminAuctionTimeline({
    auction: {
      status: "ACTIVE",
      createdAt: "2026-09-15T10:00:00",
      startAt: "2026-09-15T18:00:00",
      endAt: "2026-09-15T20:00:00",
      updatedAt: "2026-09-15T18:00:01",
    },
    bids: [],
    result: null,
    order: null,
  });

  assert.deepEqual(
    timeline.map(({ type }) => type),
    ["AUCTION_CREATED", "AUCTION_STARTED", "AUCTION_ENDED"],
  );
  assert.equal(timeline[1].label, "排定開始");
  assert.equal(timeline[2].label, "競標截止");
  assert.ok(detail.includes("尚未產生競標結果"));
  assert.ok(detail.includes("尚未建立競標訂單"));
});

test("SOLD pending lifecycle includes complete bids result order and deadline", () => {
  const timeline = buildAdminAuctionTimeline({
    auction: {
      status: "SOLD",
      createdAt: "2026-09-15T10:00:00",
      startAt: "2026-09-15T18:00:00",
      endAt: "2026-09-15T20:00:00",
    },
    // API 以最新到最舊回傳，Timeline 仍必須按真正時間排序。
    bids: [
      {
        bidAmount: 8500,
        bidderName: "Buyer",
        createdAt: "2026-09-15T19:58:00",
      },
      {
        bidAmount: 8000,
        bidderName: "First bidder",
        createdAt: "2026-09-15T18:15:00",
      },
    ],
    result: {
      resultStatus: "SOLD",
      finalPrice: 8500,
      winnerName: "Buyer",
      endedAt: "2026-09-15T20:00:00",
      createdAt: "2026-09-15T20:00:00",
    },
    order: {
      orderNumber: "AO-001",
      orderStatus: "PENDING_PAYMENT",
      paymentStatus: "PENDING",
      shippingStatus: "NOT_SHIPPED",
      createdAt: "2026-09-15T20:00:00",
      paymentDueAt: "2026-09-16T20:00:00",
    },
  });

  assert.deepEqual(
    timeline.map(({ type }) => type),
    [
      "AUCTION_CREATED",
      "AUCTION_STARTED",
      "FIRST_BID",
      "LAST_BID",
      "AUCTION_ENDED",
      "RESULT_CREATED",
      "ORDER_CREATED",
      "PAYMENT_DUE",
    ],
  );
  assert.equal(timeline[2].amount, 8000);
  assert.equal(timeline[3].amount, 8500);
  assert.equal(formatAuctionOrderStatus("PENDING_PAYMENT"), "等待付款");
  assert.match(formatAuctionTime(timeline[7].occurredAt), /2026\/09\/16.*20:00/);

  const singleBidEvents = buildAdminAuctionTimeline({
    auction: {},
    bids: [
      {
        bidAmount: 8000,
        bidderName: "Only bidder",
        createdAt: "2026-09-15T18:15:00",
      },
    ],
  }).filter(({ type }) => type.endsWith("BID"));
  assert.deepEqual(singleBidEvents.map(({ type }) => type), ["FIRST_BID"]);

  const sameSecondBidEvents = buildAdminAuctionTimeline({
    auction: {},
    // API tie-break 是 createdAt DESC, bidId DESC，所以較新的 6500 在前。
    bids: [
      {
        bidAmount: 6500,
        bidderName: "Newer bidder",
        createdAt: "2026-09-15T18:15:00",
      },
      {
        bidAmount: 6000,
        bidderName: "Older bidder",
        createdAt: "2026-09-15T18:15:00",
      },
    ],
  }).filter(({ type }) => type.endsWith("BID"));
  assert.equal(sameSecondBidEvents[0].type, "FIRST_BID");
  assert.equal(sameSecondBidEvents[0].amount, 6000);
  assert.equal(sameSecondBidEvents[1].type, "LAST_BID");
  assert.equal(sameSecondBidEvents[1].amount, 6500);
});

test("SOLD paid lifecycle uses paidAt and never invents abandonment", () => {
  const timeline = buildAdminAuctionTimeline({
    auction: {
      status: "SOLD",
      createdAt: "2026-09-15T10:00:00",
      startAt: "2026-09-15T18:00:00",
      endAt: "2026-09-15T20:00:00",
    },
    result: {
      resultStatus: "SOLD",
      endedAt: "2026-09-15T20:00:00",
    },
    order: {
      orderNumber: "AO-002",
      orderStatus: "PAID",
      paymentStatus: "PAID",
      createdAt: "2026-09-15T20:00:00",
      paidAt: "2026-09-15T20:15:00",
      paymentDueAt: "2026-09-16T20:00:00",
      updatedAt: "2026-09-15T20:15:00",
    },
  });

  const types = timeline.map(({ type }) => type);
  assert.ok(types.includes("ORDER_PAID"));
  assert.ok(!types.includes("ORDER_ABANDONED"));
  assert.ok(types.indexOf("ORDER_PAID") < types.indexOf("PAYMENT_DUE"));
  assert.equal(
    timeline.find(({ type }) => type === "ORDER_PAID").occurredAt,
    "2026-09-15T20:15:00",
  );
});

test("SOLD abandoned keeps SOLD result and orders deadline before equal-time abandonment", () => {
  const timeline = buildAdminAuctionTimeline({
    auction: {
      status: "SOLD",
      createdAt: "2026-09-15T10:00:00",
      startAt: "2026-09-15T18:00:00",
      endAt: "2026-09-15T20:00:00",
    },
    result: {
      resultStatus: "SOLD",
      finalPrice: 8500,
      winnerName: "Buyer",
      endedAt: "2026-09-15T20:00:00",
    },
    order: {
      orderNumber: "AO-003",
      orderStatus: "ABANDONED",
      paymentStatus: "PENDING",
      shippingStatus: "NOT_SHIPPED",
      createdAt: "2026-09-15T20:00:00",
      paymentDueAt: "2026-09-16T20:00:00",
      abandonedAt: "2026-09-16T20:00:00",
    },
  });

  const types = timeline.map(({ type }) => type);
  assert.ok(types.indexOf("PAYMENT_DUE") < types.indexOf("ORDER_ABANDONED"));
  assert.ok(!types.includes("ORDER_PAID"));
  assert.equal(formatAuctionOrderStatus("ABANDONED"), "已棄標");
  assert.equal(formatAuctionOrderStatus("PENDING"), "等待付款");
  const resultEvent = timeline.find(({ type }) => type === "RESULT_CREATED");
  assert.equal(resultEvent.resultStatus, "SOLD");
  assert.equal(resultEvent.actorName, "Buyer");
  assert.equal(resultEvent.amount, 8500);
  assert.equal(
    timeline.find(({ type }) => type === "ORDER_ABANDONED").occurredAt,
    "2026-09-16T20:00:00",
  );
});

test("UNSOLD lifecycle has a flow result without a fabricated order", () => {
  const timeline = buildAdminAuctionTimeline({
    auction: {
      status: "UNSOLD",
      createdAt: "2026-09-15T10:00:00",
      startAt: "2026-09-15T18:00:00",
      endAt: "2026-09-15T20:00:00",
    },
    bids: [],
    result: {
      resultStatus: "UNSOLD",
      finalPrice: null,
      winnerName: null,
      endedAt: "2026-09-15T20:00:03",
    },
    order: null,
  });

  assert.equal(
    timeline.find(({ type }) => type === "RESULT_CREATED").label,
    "競標流標",
  );
  assert.ok(!timeline.some(({ type }) => type.startsWith("ORDER_")));
});

test("timeline omits missing timestamps and never treats updatedAt as a business event", async () => {
  assert.deepEqual(
    buildAdminAuctionTimeline({
      auction: { status: "ACTIVE", updatedAt: "2026-09-15T18:00:00" },
      bids: [{ bidAmount: 8000, bidderName: "Buyer", createdAt: null }],
      result: null,
      order: { orderStatus: "PAID", updatedAt: "2026-09-15T20:15:00" },
    }),
    [],
  );
  assert.doesNotMatch(
    await readFile(
      new URL("../src/utils/auctionAdminLifecycle.js", import.meta.url),
      "utf8",
    ),
    /\.updatedAt/,
  );
});

test("admin view handles backend 401 and 403 responses", () => {
  assert.match(view, /error\.response\?\.status === 401/);
  assert.match(view, /error\.response\?\.status === 403/);
  assert.match(view, /name: "forbidden"/);
});

function stripImports(script) {
  let remaining = script;
  while (remaining.includes("import ")) {
    const start = remaining.indexOf("import ");
    const end = remaining.indexOf(";", start);
    if (end < 0) throw new Error("Unterminated import");
    remaining = remaining.slice(0, start) + remaining.slice(end + 1);
  }
  return remaining;
}

const scriptStart = view.indexOf("<script setup>") + "<script setup>".length;
const adminViewScript = stripImports(view.slice(scriptStart, view.indexOf("</script>", scriptStart)));
function adminAuction(id, managementState = "VISIBLE", status = "ACTIVE") {
  return {
    auctionId: id,
    productName: "Auction " + id,
    managementState,
    status,
    removedAt: managementState === "VISIBLE" ? null : "2026-09-13T10:00:00",
    deletedAt: managementState === "DELETED" ? "2026-09-13T11:00:00" : null,
    endAt: "2099-09-13T12:00:00",
  };
}

function adminHarness(pages) {
  const batchCalls = [];
  const listCalls = [];
  const context = {
    computed,
    reactive,
    ref,
    onMounted: () => {},
    RouterLink: {},
    useRouter: () => ({ replace: async () => {} }),
    resolveAuctionImageUrl: (value) => value,
    formatAuctionOrderStatus,
    formatMemberAuctionStatus,
    formatAuctionTime,
    parseAuctionTime,
    getAdminAuctions: async (params) => {
      listCalls.push({ ...params });
      const { page } = params;
      const items = pages[page] || [];
      return { data: { data: { items, page, size: 15, totalElements: pages.flat().length, totalPages: pages.length } } };
    },
    removeAdminAuction: async () => ({}),
    restoreAdminAuction: async () => ({}),
    deleteAdminAuction: async () => ({}),
    removeAdminAuctionsBatch: async (ids) => {
      batchCalls.push([...ids]);
      return { data: { data: {
        totalCount: ids.length, successCount: ids.length, failureCount: 0,
        items: ids.map((auctionId) => ({ auctionId, success: true, message: "success" })),
      } } };
    },
    restoreAdminAuctionsBatch: async () => ({ data: { data: { totalCount: 0, successCount: 0, failureCount: 0, items: [] } } }),
    deleteAdminAuctionsBatch: async () => ({ data: { data: { totalCount: 0, successCount: 0, failureCount: 0, items: [] } } }),
  };
  vm.runInNewContext(adminViewScript + String.fromCharCode(10) + "globalThis.subject = { filters, orderStatuses, items, selectedIds, selectedAuctionMap, pagination, errorMessage, dialog, canBatchRemove, canBatchRestore, canBatchDelete, canRestoreAuction, formatStatus, loadAuctions, enterBatchMode, toggleSelection, toggleCurrentPage, openBatchAction, confirmAction };", context);
  return { ...context.subject, batchCalls, listCalls };
}

test("selected order status is sent through the existing admin list GET", async () => {
  const h = adminHarness([[adminAuction(1)]]);
  assert.deepEqual([...h.orderStatuses], [
    "PENDING_PAYMENT",
    "PAID",
    "SHIPPED",
    "COMPLETED",
    "CANCELLED",
    "ABANDONED",
  ]);
  h.filters.orderStatus = "ABANDONED";
  await h.loadAuctions(0);
  assert.equal(h.listCalls.at(-1).orderStatus, "ABANDONED");
});

test("batch selection remains across pages and current-page deselection preserves other pages", async () => {
  const h = adminHarness([
    [adminAuction(1), adminAuction(2)],
    [adminAuction(16), adminAuction(17)],
  ]);
  await h.loadAuctions(0);
  h.enterBatchMode();
  h.toggleSelection(h.items.value[0]);
  await h.loadAuctions(1);
  assert.deepEqual([...h.selectedIds.value], [1]);

  h.toggleCurrentPage({ target: { checked: true } });
  assert.deepEqual([...h.selectedIds.value], [1, 16, 17]);
  assert.equal(h.canBatchRemove.value, true);

  h.toggleCurrentPage({ target: { checked: false } });
  assert.deepEqual([...h.selectedIds.value], [1]);
});

test("batch action eligibility rejects terminal statuses retained from multiple pages", async () => {
  const h = adminHarness([
    [adminAuction(1, "REMOVED", "SOLD")],
    [adminAuction(16, "REMOVED", "UNSOLD")],
  ]);
  await h.loadAuctions(0);
  h.enterBatchMode();
  h.toggleSelection(h.items.value[0]);
  await h.loadAuctions(1);
  h.toggleSelection(h.items.value[0]);
  assert.equal(h.canBatchRemove.value, false);
  assert.equal(h.canBatchRestore.value, false);
  assert.equal(h.canBatchDelete.value, true);
});

test("restore is offered only for removed non-expired draft scheduled and active auctions", () => {
  const h = adminHarness([]);
  for (const status of ["DRAFT", "SCHEDULED", "ACTIVE"]) {
    assert.equal(h.canRestoreAuction(adminAuction(1, "REMOVED", status)), true, status);
  }
  for (const status of ["SOLD", "UNSOLD", "CANCELLED", "REMOVED"]) {
    assert.equal(h.canRestoreAuction(adminAuction(1, "REMOVED", status)), false, status);
  }
  const expired = adminAuction(1, "REMOVED", "ACTIVE");
  expired.endAt = "2000-01-01T00:00:00";
  assert.equal(h.canRestoreAuction(expired), false);
  assert.equal(h.canRestoreAuction(adminAuction(1, "DELETED", "ACTIVE")), false);
  assert.equal(h.formatStatus("REMOVED"), "廢標");
  assert.match(detail, /formatMemberAuctionStatus/);
});

test("batch action guard rejects mixed management states across pages", async () => {
  const h = adminHarness([
    [adminAuction(1, "VISIBLE")],
    [adminAuction(16, "REMOVED", "SOLD")],
  ]);
  await h.loadAuctions(0);
  h.enterBatchMode();
  h.toggleSelection(h.items.value[0]);
  await h.loadAuctions(1);
  h.toggleSelection(h.items.value[0]);
  assert.equal(h.canBatchRemove.value, false);
  assert.equal(h.canBatchRestore.value, false);
  assert.equal(h.canBatchDelete.value, false);
});

test("batch selection stops at 100 and reports the limit", () => {
  const h = adminHarness([]);
  h.enterBatchMode();
  for (let id = 1; id <= 101; id++) h.toggleSelection(adminAuction(id));
  assert.equal(h.selectedIds.value.length, 100);
  assert.equal(h.errorMessage.value, "批量操作最多可選取 100 筆");
});

test("successful batch processing clears cross-page selection", async () => {
  const h = adminHarness([[adminAuction(1), adminAuction(2)]]);
  await h.loadAuctions(0);
  h.enterBatchMode();
  h.toggleCurrentPage({ target: { checked: true } });
  h.openBatchAction("remove");
  h.dialog.reason = "批量下架測試";
  h.dialog.confirmed = true;
  await h.confirmAction();
  assert.deepEqual(h.batchCalls, [[1, 2]]);
  assert.equal(h.selectedIds.value.length, 0);
});
