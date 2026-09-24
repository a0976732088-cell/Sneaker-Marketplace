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
const filterPanelPath = new URL("../src/components/auction/admin/AdminAuctionFilterPanel.vue", import.meta.url);
const batchToolbarPath = new URL("../src/components/auction/admin/AdminAuctionBatchToolbar.vue", import.meta.url);
const cardPath = new URL("../src/components/auction/admin/AdminAuctionCard.vue", import.meta.url);
const paginationPath = new URL("../src/components/auction/admin/AdminAuctionPagination.vue", import.meta.url);
const actionDialogPath = new URL("../src/components/auction/admin/AdminAuctionActionDialog.vue", import.meta.url);
const routerPath = new URL("../src/router/index.js", import.meta.url);
const sidebarPath = new URL("../src/components/admin/AdminSidebar.vue", import.meta.url);
const detailPath = new URL("../src/views/admin/AdminAuctionDetailView.vue", import.meta.url);
const imageUrlPath = new URL("../src/utils/auctionImageUrl.js", import.meta.url);

const [service, view, filterPanel, batchToolbar, card, paginationUi, actionDialog, router, sidebar, detail, imageUrl] = await Promise.all([
  readFile(servicePath, "utf8"),
  readFile(viewPath, "utf8"),
  readFile(filterPanelPath, "utf8"),
  readFile(batchToolbarPath, "utf8"),
  readFile(cardPath, "utf8"),
  readFile(paginationPath, "utf8"),
  readFile(actionDialogPath, "utf8"),
  readFile(routerPath, "utf8"),
  readFile(sidebarPath, "utf8"),
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

test("admin auction route is guarded and the current sidebar links to it", () => {
  assert.match(
    router,
    /path: ["']\/admin["'][\s\S]*?meta: \{[\s\S]*?requiresAdmin: true[\s\S]*?children: \[/,
  );
  assert.match(
    router,
    /path: ["']auctions["'][\s\S]*?name: ["']admin-auctions["']/,
  );
  assert.match(sidebar, /name: ["']admin-auctions["'][\s\S]*?>\s*<span>\s*競標管理\s*<\/span>/);
});

test("admin views use shared status and time formatters and keep required SFC order", () => {
  assert.ok(view.indexOf("<template>") < view.indexOf("<script setup>"));
  for (const text of ["競標狀態", "訂單狀態", "管理狀態"]) {
    assert.ok(filterPanel.includes(text), text);
  }
  assert.match(batchToolbar, /全選本頁/);
  assert.match(paginationUi, /上一頁[\s\S]*?下一頁/);
  assert.match(filterPanel, /formatAuctionOrderStatus/);
  assert.match(card, /formatAuctionOrderStatus/);
  assert.match(card, /formatAuctionTime/);
  assert.match(detail, /完整生命週期/);
  assert.match(detail, /formatAuctionTime/);
  assert.doesNotMatch([view, filterPanel, batchToolbar, card, paginationUi, actionDialog].join("\n"), /new Date\(/);
  assert.doesNotMatch(detail, /new Date\(/);
  assert.doesNotMatch([view, filterPanel, batchToolbar, card, paginationUi, actionDialog].join("\n"), /假刪除/);
});

test("admin status filter excludes reserved DRAFT while defensive handling remains", () => {
  const statusFilter = view.match(/const statuses = \[([\s\S]*?)\];/)?.[1] ?? "";
  assert.doesNotMatch(statusFilter, /["']DRAFT["']/);
  assert.match(view, /formatMemberAuctionStatus/);
  assert.equal(formatMemberAuctionStatus("DRAFT"), "草稿");
  assert.match(view, /const restorableStatuses = new Set\(\["DRAFT", "SCHEDULED", "ACTIVE"\]\)/);
});

test("single action buttons follow visible removed and deleted states", () => {
  assert.match(card, /v-if="legacyRemoved"[\s\S]*?>\s*補登下架資訊\s*<\/button\s*>/);
  assert.match(card, /v-else-if="!auction\.removedAt && !auction\.deletedAt"[\s\S]*?>\s*下架\s*<\/button\s*>/);
  assert.match(card, /v-if="canRestore"[\s\S]*?btn-success[\s\S]*?>\s*恢復顯示\s*<\/button\s*>/);
  assert.match(card, /v-if="auction\.removedAt && !auction\.deletedAt"[\s\S]*?btn-danger[\s\S]*?>\s*刪除\s*<\/button\s*>/);
  assert.match(card, /v-if="auction\.deletedAt"[\s\S]*?>\s*已刪除\s*<\/span\s*>/);
  assert.match(card, /\.action-button[\s\S]*?box-shadow/);
});

test("action dialog requires confirmation and reasons only for remove or delete", () => {
  assert.match(actionDialog, /<Teleport to="body">/);
  assert.match(actionDialog, /role="dialog"/);
  assert.match(actionDialog, /v-if="action !== 'restore'"/);
  assert.match(actionDialog, /id="auction-action-reason"[\s\S]*?maxlength="500"/);
  assert.match(actionDialog, /v-model="reasonModel"/);
  assert.match(actionDialog, /v-model="confirmedModel"/);
  assert.match(actionDialog, /emit\("update:reason", value\)/);
  assert.match(actionDialog, /emit\("update:confirmed", value\)/);
  assert.match(actionDialog, /:disabled="!valid \|\| submitting"/);
  assert.match(actionDialog, /@submit\.prevent="emit\('confirm'\)"/);
  assert.match(view, /dialog\.confirmed[\s\S]*?dialog\.action === "restore"[\s\S]*?dialog\.reason\.trim\(\)\.length > 0/);
  assert.doesNotMatch(actionDialog, /id="admin-action-reason"/);
});

test("batch controls appear only after entering batch mode", () => {
  assert.match(batchToolbar, /v-if="!batchMode"[\s\S]*?@click="emit\('enter'\)"[\s\S]*?>\s*批量操作\s*<\/button\s*>/);
  assert.match(batchToolbar, /<div(?=[^>]*\bv-else\b)[^>]*>[\s\S]*?emit\('remove'\)[\s\S]*?>\s*批量下架\s*<\/button\s*>/);
  assert.match(batchToolbar, /emit\('restore'\)[\s\S]*?>\s*批量恢復顯示\s*<\/button\s*>/);
  assert.match(batchToolbar, /emit\('delete'\)[\s\S]*?>\s*批量刪除\s*<\/button\s*>/);
  assert.match(batchToolbar, /@click="emit\('exit'\)"[\s\S]*?>\s*結束批量操作\s*<\/button\s*>/);
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
  assert.ok(card.includes('class="form-check-input batch-checkbox"'));
  assert.ok(card.includes("border: 1.5px solid #98a2b3"));
  assert.match(card, /v-if="auction\.imageUrl && !thumbnailBroken"/);
  assert.ok(card.includes(':src="resolveAuctionImageUrl(auction.imageUrl)"'));
  assert.ok(card.includes("name: 'admin-auction-detail'"));
  assert.ok(view.includes('v-for="(auction, index) in items"'));
  assert.ok(view.includes("size: 15"));
  assert.match(view, /function rowNumber\(index\)[\s\S]*?pagination\.page \* pagination\.size \+ index \+ 1/);
  assert.match(card, /第 \{\{ rowNumber \}\} 筆 · 競標 #\{\{ auction\.auctionId \}\}/);
  assert.match(
    card,
    /auction\.sellerName\s*\|\|\s*"會員資料未提供"[\s\S]*?auction\.sellerId/,
  );
  assert.match(
    card,
    /<span class="auction-status-item">\s*<span>訂單狀態<\/span>[\s\S]*?auction\.orderStatus \? formatAuctionOrderStatus\(auction\.orderStatus\) : "-"/,
  );
  assert.match(card, /role="listitem"/);
});

test("admin list order-status filter exposes supported auction order states", () => {
  assert.match(
    filterPanel,
    /id="admin-order-status"[\s\S]*?v-model="filters\.orderStatus"[\s\S]*?v-for="status in orderStatuses"[\s\S]*?formatAuctionOrderStatus\(status\)/,
  );
  const configured =
    view.match(/^[ \t]*const orderStatuses = \[([\s\S]*?)\];/m)?.[1] ?? "";
  for (const status of [
    "PENDING_PAYMENT",
    "PAID",
    "ABANDONED",
  ]) {
    assert.match(configured, new RegExp(`"${status}"`), status);
  }
  for (const unsupportedStatus of ["SHIPPED", "COMPLETED", "CANCELLED"]) {
    assert.doesNotMatch(
      configured,
      new RegExp(`"${unsupportedStatus}"`),
      unsupportedStatus,
    );
  }
  assert.doesNotMatch(configured, /FAILED|NOT_SHIPPED|DELIVERED/);
});

test("admin list uses compact responsive cards without a wide table", () => {
  assert.match(filterPanel, /class="col-12 col-xl-4"/);
  assert.equal((filterPanel.match(/class="col-12 col-md-4 col-xl-2"/g) || []).length, 3);
  assert.match(filterPanel, /class="col-12 col-xl-2 d-grid"/);
  assert.match(view, /class="auction-list" role="list"/);
  assert.match(card, /class="auction-card"/);
  assert.match(card, /\.auction-card-head\s*\{[^}]*display:\s*flex/);
  assert.match(card, /\.auction-card-meta\s*\{[^}]*display:\s*grid/);
  assert.match(card, /auction-row-actions auction-action-buttons/);
  for (const label of ["賣家", "目前價格／出價", "時間"]) {
    assert.ok(card.includes('class="auction-field-label">' + label), label);
  }
  assert.match(card, /\.auction-name-link\s*\{[^}]*overflow-wrap:\s*anywhere/);
  assert.doesNotMatch([view, filterPanel, batchToolbar, card, paginationUi, actionDialog].join("\n"), /min-width:\s*1380px/);
});

test("admin auction parent wires five UI components to existing state and actions", () => {
  function tag(name) {
    const openingTag = view.match(new RegExp("<" + name + "\\b[\\s\\S]*?/>"))?.[0];
    assert.ok(openingTag, name + " is mounted");
    return openingTag;
  }

  const filter = tag("AdminAuctionFilterPanel");
  for (const binding of [
    ':filters="filters"',
    ':statuses="statuses"',
    ':order-statuses="orderStatuses"',
    '@submit="applyFilters"',
  ]) assert.ok(filter.includes(binding), binding);

  const toolbar = tag("AdminAuctionBatchToolbar");
  for (const binding of [
    ':batch-mode="batchMode"',
    ':selected-count="selectedIds.length"',
    ':all-current-page-selected="allCurrentPageSelected"',
    ':current-selectable-count="currentSelectableIds.length"',
    ':can-batch-remove="canBatchRemove"',
    ':can-batch-restore="canBatchRestore"',
    ':can-batch-delete="canBatchDelete"',
    '@enter="enterBatchMode"',
    '@exit="exitBatchMode"',
    '@toggle-current-page="toggleCurrentPageFromButton"',
    '@remove="openBatchAction(\'remove\')"',
    '@restore="openBatchAction(\'restore\')"',
    '@delete="openBatchAction(\'delete\')"',
  ]) assert.ok(toolbar.includes(binding), binding);

  const auctionCard = tag("AdminAuctionCard");
  for (const binding of [
    'v-for="(auction, index) in items"',
    ':auction="auction"',
    ':row-number="rowNumber(index)"',
    ':batch-mode="batchMode"',
    ':selected="selectedIds.includes(auction.auctionId)"',
    ':thumbnail-broken="brokenImageIds.has(auction.auctionId)"',
    ':can-restore="canRestoreAuction(auction)"',
    ':legacy-removed="isLegacyRemoved(auction)"',
    '@image-error="markThumbnailBroken(auction.auctionId)"',
    '@toggle-selection="toggleSelection(auction)"',
    '@remove="openSingleAction(\'remove\', auction)"',
    '@restore="openSingleAction(\'restore\', auction)"',
    '@delete="openSingleAction(\'delete\', auction)"',
  ]) assert.ok(auctionCard.includes(binding), binding);

  const pagination = tag("AdminAuctionPagination");
  for (const binding of [
    ':page="pagination.page"',
    ':total-pages="pagination.totalPages"',
    ':total-elements="pagination.totalElements"',
    '@change="changePage"',
  ]) assert.ok(pagination.includes(binding), binding);
  assert.match(paginationUi, /@click="emit\('change', page - 1\)"/);
  assert.match(paginationUi, /@click="emit\('change', page \+ 1\)"/);

  const dialogComponent = tag("AdminAuctionActionDialog");
  for (const binding of [
    ':open="dialog.open"',
    ':action="dialog.action"',
    'v-model:reason="dialog.reason"',
    'v-model:confirmed="dialog.confirmed"',
    ':error="dialog.error"',
    ':title="dialogTitle"',
    ':target-text="dialogTargetText"',
    ':warning="dialogWarning"',
    ':action-label="dialogActionLabel"',
    ':alert-class="dialogAlertClass"',
    ':submit-class="dialogSubmitClass"',
    ':valid="dialogValid"',
    '@close="closeActionDialog"',
    '@confirm="confirmAction"',
  ]) assert.ok(dialogComponent.includes(binding), binding);
});

test("admin detail remains available after moderation and resolves images against backend", () => {
  assert.match(
    router,
    /path: ["']auctions\/:id["'][\s\S]*?name: ["']admin-auction-detail["']/,
  );
  assert.ok(detail.includes("getAdminAuctionDetail"));
  assert.ok(detail.includes("公開中、已下架及已刪除"));
  assert.ok(detail.includes("resolveAuctionImageUrl(imageUrl)"));
  assert.match(
    detail,
    /賣家[\s\S]*?auction\.sellerName\s*\|\|\s*"會員資料未提供"[\s\S]*?賣家 ID[\s\S]*?auction\.sellerId/,
  );
  assert.ok(imageUrl.includes('import httpClient from "@/service/httpClient"'));
  assert.ok(imageUrl.includes('replace(/\\/api\\/?$/, "")'));
  assert.ok(card.includes('@error="emit(\'image-error\')"'));
  assert.ok(view.includes('@image-error="markThumbnailBroken(auction.auctionId)"'));
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
  assert.equal(timeline[2].label, "競標排定截止");
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
  vm.runInNewContext(adminViewScript + String.fromCharCode(10) + "globalThis.subject = { filters, orderStatuses, items, selectedIds, selectedAuctionMap, pagination, errorMessage, dialog, canBatchRemove, canBatchRestore, canBatchDelete, canRestoreAuction, formatStatus, loadAuctions, enterBatchMode, toggleSelection, toggleCurrentPage, toggleCurrentPageFromButton, openBatchAction, confirmAction };", context);
  return { ...context.subject, batchCalls, listCalls };
}

test("selected order status is sent through the existing admin list GET", async () => {
  const h = adminHarness([[adminAuction(1)]]);
  assert.deepEqual([...h.orderStatuses], [
    "PENDING_PAYMENT",
    "PAID",
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

test("batch toolbar selects eligible auctions on this page and toggles only this page", async () => {
  const h = adminHarness([
    [adminAuction(1), adminAuction(2), adminAuction(3, "DELETED")],
    [adminAuction(16)],
  ]);
  await h.loadAuctions(0);
  h.enterBatchMode();
  h.toggleCurrentPageFromButton();
  assert.deepEqual([...h.selectedIds.value], [1, 2]);

  await h.loadAuctions(1);
  h.toggleCurrentPageFromButton();
  assert.deepEqual([...h.selectedIds.value], [1, 2, 16]);
  h.toggleCurrentPageFromButton();
  assert.deepEqual([...h.selectedIds.value], [1, 2]);
});

test("batch toolbar keeps the 100-item limit without partial current-page selection", async () => {
  const h = adminHarness([[adminAuction(101), adminAuction(102)]]);
  await h.loadAuctions(0);
  h.enterBatchMode();
  for (let id = 1; id <= 99; id++) h.toggleSelection(adminAuction(id));
  h.toggleCurrentPageFromButton();
  assert.equal(h.selectedIds.value.length, 99);
  assert.equal(h.errorMessage.value, "批量操作最多可選取 100 筆");
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
