<template>
  <main class="container py-4 admin-auctions">
    <header class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4 admin-page-heading">
      <div>
        <p class="text-uppercase text-secondary small mb-1 admin-page-eyebrow">SNEAKERMARKET ADMIN</p>
        <h1 class="h3 mb-1">競標管理</h1>
        <p class="text-secondary mb-0 admin-page-description">
          下架可恢復公開顯示；刪除僅能在競標下架後執行。
        </p>
      </div>
    </header>

    <section class="card shadow-sm mb-4">
      <div class="card-body">
        <form class="row g-3 align-items-end" @submit.prevent="applyFilters">
          <div class="col-12 col-xl-4">
            <label
              class="form-label filter-field-label"
              for="admin-auction-keyword"
              >商品名稱或型號</label
            >
            <input
              id="admin-auction-keyword"
              v-model.trim="filters.keyword"
              class="form-control"
              type="search"
            />
          </div>
          <div class="col-12 col-md-4 col-xl-2">
            <label
              class="form-label filter-field-label"
              for="admin-auction-status"
              >競標狀態</label
            >
            <select
              id="admin-auction-status"
              v-model="filters.status"
              class="form-select"
            >
              <option value="">全部</option>
              <option v-for="status in statuses" :key="status" :value="status">
                {{ formatStatus(status) }}
              </option>
            </select>
          </div>
          <div class="col-12 col-md-4 col-xl-2">
            <label
              class="form-label filter-field-label"
              for="admin-order-status"
              >訂單狀態</label
            >
            <select
              id="admin-order-status"
              v-model="filters.orderStatus"
              class="form-select"
            >
              <option value="">全部</option>
              <option
                v-for="status in orderStatuses"
                :key="status"
                :value="status"
              >
                {{ formatAuctionOrderStatus(status) }}
              </option>
            </select>
          </div>
          <div class="col-12 col-md-4 col-xl-2">
            <label
              class="form-label filter-field-label"
              for="admin-management-state"
              >管理狀態</label
            >
            <select
              id="admin-management-state"
              v-model="filters.managementState"
              class="form-select"
            >
              <option value="">全部</option>
              <option value="VISIBLE">公開中</option>
              <option value="REMOVED">已下架</option>
              <option value="DELETED">已刪除</option>
            </select>
          </div>
          <div class="col-12 col-xl-2 d-grid">
            <button
              class="btn btn-dark"
              type="submit"
              :disabled="loading || submitting"
            >
              {{ loading ? "查詢中…" : "查詢" }}
            </button>
          </div>
        </form>
      </div>
    </section>

    <div v-if="errorMessage" class="alert alert-danger" role="alert">
      {{ errorMessage }}
    </div>
    <div v-if="successMessage" class="alert alert-success" role="status">
      {{ successMessage }}
    </div>

    <section class="card shadow-sm">
      <div
        class="card-body border-bottom d-flex flex-wrap justify-content-between align-items-center gap-3"
      >
        <div>
          <h2 class="h5 mb-1">競標清單</h2>
          <p v-if="!batchMode" class="text-secondary small mb-0">
            所有操作都會先確認；下架與刪除另外要求填寫原因。
          </p>
          <div v-else>
            <p class="small mb-1" :class="batchSelectionClass">
              {{ batchSelectionText }}
            </p>
            <p class="text-secondary small mb-0">
              請選擇相同管理狀態的競標，避免混合下架、恢復與刪除。
            </p>
          </div>
        </div>

        <button
          v-if="!batchMode"
          class="btn btn-primary px-4"
          type="button"
          :disabled="loading || submitting"
          @click="enterBatchMode"
        >
          批量操作
        </button>

        <div v-else class="d-flex flex-wrap align-items-center gap-2 auction-batch-actions auction-action-buttons">
          <span class="badge text-bg-primary fs-6 auction-batch-count"
            >已選 {{ selectedIds.length }} 筆（可跨頁）</span
          >
          <button
            class="btn btn-outline-secondary auction-select-page"
            type="button"
            :aria-pressed="allCurrentPageSelected"
            :disabled="loading || submitting || currentSelectableIds.length === 0"
            @click="toggleCurrentPageFromButton"
          >
            {{ allCurrentPageSelected ? "取消本頁全選" : "全選本頁" }}
          </button>
          <button
            class="btn btn-warning fw-semibold"
            type="button"
            :disabled="loading || submitting || !canBatchRemove"
            @click="openBatchAction('remove')"
          >
            批量下架
          </button>
          <button
            class="btn btn-success"
            type="button"
            :disabled="loading || submitting || !canBatchRestore"
            @click="openBatchAction('restore')"
          >
            批量恢復顯示
          </button>
          <button
            class="btn btn-danger"
            type="button"
            :disabled="loading || submitting || !canBatchDelete"
            @click="openBatchAction('delete')"
          >
            批量刪除
          </button>
          <button
            class="btn btn-outline-secondary"
            type="button"
            :disabled="submitting"
            @click="exitBatchMode"
          >
            結束批量操作
          </button>
        </div>
      </div>

      <div v-if="batchItems.length" class="border-bottom p-3 bg-light">
        <strong>本次處理結果</strong>
        <ul class="mb-0 mt-2">
          <li
            v-for="item in batchItems"
            :key="item.auctionId"
            :class="item.success ? 'text-success' : 'text-danger'"
          >
            #{{ item.auctionId }}：{{ item.message }}
          </li>
        </ul>
      </div>

      <div class="auction-list" role="list" aria-label="競標清單">
        <div v-if="loading && items.length === 0" class="auction-list-message text-secondary">
          載入中…
        </div>
        <div v-else-if="items.length === 0" class="auction-list-message text-secondary">
          沒有符合條件的競標。
        </div>

        <article
          v-for="(auction, index) in items"
          :key="auction.auctionId"
          class="auction-card"
          :class="{ 'is-batch': batchMode, 'is-selected': batchMode && selectedIds.includes(auction.auctionId) }"
          role="listitem"
        >
          <div class="auction-card-head">
            <div class="auction-product">
              <img
                v-if="
                  auction.imageUrl && !brokenImageIds.has(auction.auctionId)
                "
                class="auction-thumbnail"
                :src="resolveAuctionImageUrl(auction.imageUrl)"
                :alt="(auction.productName || '競標商品') + '縮圖'"
                loading="lazy"
                @error="markThumbnailBroken(auction.auctionId)"
              />
              <div
                v-else
                class="auction-thumbnail thumbnail-placeholder"
                aria-label="沒有商品圖片"
              >
                無圖片
              </div>
              <div class="product-info">
                <div class="auction-product-id">
                  第 {{ rowNumber(index) }} 筆 · 競標 #{{ auction.auctionId }}
                </div>
                <RouterLink
                  class="auction-name-link"
                  :to="{
                    name: 'admin-auction-detail',
                    params: { id: auction.auctionId },
                  }"
                >
                  {{ auction.productName || "商品資料缺失" }}
                </RouterLink>
                <div class="small text-secondary">{{ auction.model || "無型號" }}</div>
              </div>
            </div>

            <div class="auction-card-controls">
              <div class="auction-statuses">
                <span class="auction-status-item">
                  <span>競標狀態</span>
                  <span class="badge text-bg-secondary">{{
                    formatStatus(auction.status)
                  }}</span>
                </span>
                <span class="auction-status-item">
                  <span>管理狀態</span>
                  <span
                    class="badge"
                    :class="managementBadge(auction.managementState)"
                  >
                    {{ formatManagementState(auction.managementState) }}
                  </span>
                </span>
                <span class="auction-status-item">
                  <span>訂單狀態</span>
                  <span class="badge text-bg-light border text-dark">
                    {{ auction.orderStatus ? formatAuctionOrderStatus(auction.orderStatus) : "-" }}
                  </span>
                </span>
              </div>
              <input
                v-if="batchMode"
                class="form-check-input batch-checkbox"
                type="checkbox"
                :aria-label="'選取競標 ' + auction.auctionId"
                :checked="selectedIds.includes(auction.auctionId)"
                :disabled="loading || submitting || Boolean(auction.deletedAt)"
                @change="toggleSelection(auction)"
              />
            </div>
          </div>

          <div class="auction-card-meta">
            <div class="auction-meta-item">
              <span class="auction-field-label">目前價格／出價</span>
              <strong>{{ formatMoney(auction.currentPrice) }}</strong>
              <span class="small text-secondary">{{ auction.bidCount }} 次出價</span>
            </div>
            <div class="auction-meta-item">
              <span class="auction-field-label">賣家</span>
              <span>{{ auction.sellerName || "會員資料未提供" }}</span>
              <span class="small text-secondary">ID #{{ auction.sellerId }}</span>
            </div>
            <div class="auction-meta-item auction-meta-time">
              <span class="auction-field-label">時間</span>
              <span>建立 {{ formatAuctionTime(auction.createdAt) }}</span>
              <span v-if="auction.removedAt">
                下架 {{ formatAuctionTime(auction.removedAt) }}
              </span>
              <span v-if="auction.deletedAt">
                刪除 {{ formatAuctionTime(auction.deletedAt) }}
              </span>
            </div>
            <div
              v-if="!batchMode"
              class="d-flex flex-wrap justify-content-end gap-2 auction-row-actions auction-action-buttons auction-meta-actions"
            >
              <button
                v-if="isLegacyRemoved(auction)"
                class="btn btn-sm btn-warning fw-semibold action-button"
                type="button"
                :disabled="loading || submitting"
                @click="openSingleAction('remove', auction)"
              >
                補登下架資訊
              </button>
              <button
                v-else-if="!auction.removedAt && !auction.deletedAt"
                class="btn btn-sm btn-warning fw-semibold action-button"
                type="button"
                :disabled="loading || submitting"
                @click="openSingleAction('remove', auction)"
              >
                下架
              </button>
              <button
                v-if="canRestoreAuction(auction)"
                class="btn btn-sm btn-success action-button"
                type="button"
                :disabled="loading || submitting"
                @click="openSingleAction('restore', auction)"
              >
                恢復顯示
              </button>
              <button
                v-if="auction.removedAt && !auction.deletedAt"
                class="btn btn-sm btn-danger action-button"
                type="button"
                :disabled="loading || submitting"
                @click="openSingleAction('delete', auction)"
              >
                刪除
              </button>
              <span v-if="auction.deletedAt" class="small text-secondary">
                已刪除
              </span>
            </div>
          </div>

          <div
            v-if="auction.removeReason || auction.deleteReason"
            class="auction-card-reasons"
          >
            <div
              v-if="auction.removeReason"
              class="reason-text"
              :title="auction.removeReason"
            >
              下架：{{ auction.removeReason }}
            </div>
            <div
              v-if="auction.deleteReason"
              class="reason-text"
              :title="auction.deleteReason"
            >
              刪除：{{ auction.deleteReason }}
            </div>
          </div>
        </article>
      </div>

      <div
        class="card-body d-flex flex-wrap justify-content-between align-items-center gap-3 border-top"
      >
        <span class="text-secondary"
          >共 {{ pagination.totalElements }} 筆，第 {{ pagination.page + 1 }} /
          {{ Math.max(pagination.totalPages, 1) }} 頁</span
        >
        <div class="btn-group" role="group" aria-label="競標管理分頁">
          <button
            class="btn btn-outline-secondary"
            type="button"
            :disabled="loading || submitting || pagination.page === 0"
            @click="changePage(pagination.page - 1)"
          >
            上一頁
          </button>
          <button
            class="btn btn-outline-secondary"
            type="button"
            :disabled="
              loading ||
              submitting ||
              pagination.page + 1 >= pagination.totalPages
            "
            @click="changePage(pagination.page + 1)"
          >
            下一頁
          </button>
        </div>
      </div>
    </section>
  </main>

  <Teleport to="body">
    <div
      v-if="dialog.open"
      class="action-dialog-overlay"
      role="presentation"
      @click.self="closeActionDialog"
      @keydown.esc="closeActionDialog"
    >
      <section
        class="card shadow-lg action-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="auction-action-dialog-title"
      >
        <form @submit.prevent="confirmAction">
          <div
            class="card-header d-flex justify-content-between align-items-start gap-3"
          >
            <div>
              <p class="text-uppercase text-secondary small mb-1">操作確認</p>
              <h2 id="auction-action-dialog-title" class="h5 mb-0">
                {{ dialogTitle }}
              </h2>
            </div>
            <button
              class="btn-close"
              type="button"
              aria-label="關閉"
              :disabled="submitting"
              @click="closeActionDialog"
            />
          </div>

          <div class="card-body">
            <div class="alert mb-3" :class="dialogAlertClass">
              {{ dialogWarning }}
            </div>
            <p class="fw-semibold mb-3">{{ dialogTargetText }}</p>

            <div v-if="dialog.action !== 'restore'">
              <label class="form-label" for="auction-action-reason"
                >{{ dialogActionLabel }}原因</label
              >
              <textarea
                id="auction-action-reason"
                v-model="dialog.reason"
                class="form-control"
                rows="4"
                maxlength="500"
                autofocus
                :placeholder="`請輸入${dialogActionLabel}原因（必填，最多 500 字）`"
              />
              <div class="d-flex justify-content-between form-text mb-3">
                <span
                  v-if="dialog.reason.trim().length === 0"
                  class="text-danger"
                  >原因不可空白</span
                >
                <span v-else>請填寫可供後續查核的具體原因</span>
                <span>{{ dialog.reason.length }}/500</span>
              </div>
            </div>

            <div class="form-check confirmation-check">
              <input
                id="auction-action-confirmed"
                v-model="dialog.confirmed"
                class="form-check-input"
                type="checkbox"
              />
              <label class="form-check-label" for="auction-action-confirmed">
                我已核對競標資料，確認執行{{ dialogActionLabel }}
              </label>
            </div>

            <div
              v-if="dialog.error"
              class="alert alert-danger mt-3 mb-0"
              role="alert"
            >
              {{ dialog.error }}
            </div>
          </div>

          <div class="card-footer d-flex justify-content-end gap-2">
            <button
              class="btn btn-outline-secondary"
              type="button"
              :disabled="submitting"
              @click="closeActionDialog"
            >
              取消
            </button>
            <button
              class="btn"
              :class="dialogSubmitClass"
              type="submit"
              :disabled="!dialogValid || submitting"
            >
              {{ submitting ? "處理中…" : `確認${dialogActionLabel}` }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import { resolveAuctionImageUrl } from "@/utils/auctionImageUrl";
import {
  formatAuctionOrderStatus,
  formatMemberAuctionStatus,
} from "@/utils/auctionMemberStatus";
import { formatAuctionTime, parseAuctionTime } from "@/utils/auctionTime";
import {
  deleteAdminAuction,
  deleteAdminAuctionsBatch,
  getAdminAuctions,
  removeAdminAuction,
  removeAdminAuctionsBatch,
  restoreAdminAuction,
  restoreAdminAuctionsBatch,
} from "@/service/auctionAdminService";

const router = useRouter();
const statuses = [
  "SCHEDULED",
  "ACTIVE",
  "SOLD",
  "UNSOLD",
  "CANCELLED",
  "REMOVED",
];
// const orderStatuses = [
//   "PENDING_PAYMENT",
//   "PAID",
//   "SHIPPED",
//   "COMPLETED",
//   "CANCELLED",
//   "ABANDONED",
// ];
const orderStatuses = ["PENDING_PAYMENT", "PAID", "ABANDONED"];
const restorableStatuses = new Set(["DRAFT", "SCHEDULED", "ACTIVE"]);
const filters = reactive({
  keyword: "",
  status: "",
  orderStatus: "",
  managementState: "",
});
const items = ref([]);
const MAX_BATCH_SELECTION = 100;
const selectedAuctionMap = ref(new Map());
const selectedIds = computed(() => [...selectedAuctionMap.value.keys()]);
const batchMode = ref(false);
const loading = ref(false);
const submitting = ref(false);
const errorMessage = ref("");
const successMessage = ref("");
const batchItems = ref([]);
const brokenImageIds = ref(new Set());
const pagination = reactive({
  page: 0,
  size: 15,
  totalElements: 0,
  totalPages: 0,
});
const dialog = reactive({
  open: false,
  action: "remove",
  mode: "single",
  ids: [],
  targetName: "",
  reason: "",
  confirmed: false,
  error: "",
});
let requestSequence = 0;

const currentSelectableAuctions = computed(() =>
  items.value.filter((auction) => !auction.deletedAt),
);
const currentSelectableIds = computed(() =>
  currentSelectableAuctions.value.map((auction) => auction.auctionId),
);
const selectedAuctions = computed(() => [...selectedAuctionMap.value.values()]);
const allCurrentPageSelected = computed(
  () =>
    currentSelectableIds.value.length > 0 &&
    currentSelectableIds.value.every((id) => selectedAuctionMap.value.has(id)),
);
const canBatchRemove = computed(
  () =>
    selectedAuctions.value.length > 0 &&
    (selectedAuctions.value.every(
      (auction) =>
        auction.managementState === "VISIBLE" &&
        !auction.removedAt &&
        !auction.deletedAt,
    ) ||
      selectedAuctions.value.every(isLegacyRemoved)),
);
const canBatchRestore = computed(
  () =>
    selectedAuctions.value.length > 0 &&
    selectedAuctions.value.every(canRestoreAuction),
);
const canBatchDelete = computed(
  () =>
    selectedAuctions.value.length > 0 &&
    selectedAuctions.value.every(
      (auction) => auction.removedAt && !auction.deletedAt,
    ),
);
const batchSelectionText = computed(() => {
  if (selectedIds.value.length === 0) return "請勾選要處理的競標";
  if (canBatchRemove.value)
    return `已選 ${selectedIds.value.length} 筆，可進行批量下架`;
  if (canBatchRestore.value && canBatchDelete.value) {
    return `已選 ${selectedIds.value.length} 筆，可進行批量恢復顯示或刪除`;
  }
  if (canBatchDelete.value)
    return `已選 ${selectedIds.value.length} 筆，可進行批量刪除`;
  return "選取項目包含不同管理狀態，請重新選擇";
});
const batchSelectionClass = computed(() =>
  selectedIds.value.length > 0 &&
  !canBatchRemove.value &&
  !canBatchRestore.value &&
  !canBatchDelete.value
    ? "text-danger"
    : "text-primary",
);
const dialogActionLabel = computed(
  () =>
    ({
      remove: "下架",
      restore: "恢復顯示",
      delete: "刪除",
    })[dialog.action],
);
const dialogTitle = computed(
  () =>
    `${dialog.mode === "batch" ? "批量" : "單筆"}${dialogActionLabel.value}確認`,
);
const dialogTargetText = computed(() =>
  dialog.mode === "batch"
    ? `即將${dialogActionLabel.value} ${dialog.ids.length} 筆競標`
    : `競標 #${dialog.ids[0]} ${dialog.targetName}`,
);
const dialogWarning = computed(
  () =>
    ({
      remove:
        "下架後會立即從公開商城隱藏，並停止出價與賣家操作；若持續下架至截止時間，系統會自動判定廢標。",
      restore:
        "恢復後會重新出現在公開商城；進行中競標會依原開始與截止時間恢復狀態。",
      delete: "刪除後目前沒有還原功能，請確認選取的競標皆已完成下架。",
    })[dialog.action],
);
const dialogAlertClass = computed(
  () =>
    ({
      remove: "alert-warning",
      restore: "alert-success",
      delete: "alert-danger",
    })[dialog.action],
);
const dialogSubmitClass = computed(
  () =>
    ({
      remove: "btn-warning fw-semibold",
      restore: "btn-success",
      delete: "btn-danger",
    })[dialog.action],
);
const dialogValid = computed(
  () =>
    dialog.confirmed &&
    (dialog.action === "restore" ||
      (dialog.reason.trim().length > 0 && dialog.reason.trim().length <= 500)),
);

async function loadAuctions(page = pagination.page) {
  const sequence = ++requestSequence;
  loading.value = true;
  errorMessage.value = "";
  try {
    const response = await getAdminAuctions({
      ...filters,
      page,
      size: pagination.size,
      sort: "createdAt,desc",
    });
    if (sequence !== requestSequence) return;
    const data = response.data.data;
    items.value = data.items;
    refreshSelectedSnapshots(data.items);
    pagination.page = data.page;
    pagination.size = data.size;
    pagination.totalElements = data.totalElements;
    pagination.totalPages = data.totalPages;
  } catch (error) {
    if (sequence !== requestSequence) return;
    if (await handleAccessError(error)) return;
    errorMessage.value = apiError(error, "無法載入競標管理列表");
  } finally {
    if (sequence === requestSequence) loading.value = false;
  }
}

function applyFilters() {
  exitBatchMode();
  loadAuctions(0);
}

function changePage(page) {
  if (page < 0 || page >= pagination.totalPages) return;
  loadAuctions(page);
}

function rowNumber(index) {
  return pagination.page * pagination.size + index + 1;
}

function markThumbnailBroken(auctionId) {
  brokenImageIds.value = new Set([...brokenImageIds.value, auctionId]);
}

function enterBatchMode() {
  clearActionMessages();
  clearSelection();
  batchMode.value = true;
}

function exitBatchMode() {
  if (submitting.value) return;
  clearSelection();
  batchMode.value = false;
}

function selectionSnapshot(auction) {
  return {
    auctionId: auction.auctionId,
    managementState: auction.managementState,
    status: auction.status,
    removedAt: auction.removedAt,
    deletedAt: auction.deletedAt,
    endAt: auction.endAt,
  };
}

function refreshSelectedSnapshots(pageItems) {
  if (selectedAuctionMap.value.size === 0) return;
  const next = new Map(selectedAuctionMap.value);
  for (const auction of pageItems) {
    if (next.has(auction.auctionId))
      next.set(auction.auctionId, selectionSnapshot(auction));
  }
  selectedAuctionMap.value = next;
}

function clearSelection() {
  selectedAuctionMap.value = new Map();
}

function toggleSelection(auction) {
  if (auction.deletedAt) return;
  const next = new Map(selectedAuctionMap.value);
  if (next.has(auction.auctionId)) {
    next.delete(auction.auctionId);
  } else {
    if (next.size >= MAX_BATCH_SELECTION) {
      errorMessage.value = "批量操作最多可選取 100 筆";
      return;
    }
    next.set(auction.auctionId, selectionSnapshot(auction));
  }
  errorMessage.value = "";
  selectedAuctionMap.value = next;
}

function toggleCurrentPage(event) {
  const next = new Map(selectedAuctionMap.value);
  if (event.target.checked) {
    const additions = currentSelectableAuctions.value.filter(
      (auction) => !next.has(auction.auctionId),
    );
    if (next.size + additions.length > MAX_BATCH_SELECTION) {
      errorMessage.value = "批量操作最多可選取 100 筆";
      event.target.checked = allCurrentPageSelected.value;
      return;
    }
    for (const auction of additions)
      next.set(auction.auctionId, selectionSnapshot(auction));
  } else {
    for (const auctionId of currentSelectableIds.value) next.delete(auctionId);
  }
  errorMessage.value = "";
  selectedAuctionMap.value = next;
}

function toggleCurrentPageFromButton() {
  toggleCurrentPage({ target: { checked: !allCurrentPageSelected.value } });
}

function canRestoreAuction(auction) {
  if (
    !auction.removedAt ||
    auction.deletedAt ||
    !restorableStatuses.has(auction.status)
  )
    return false;
  if (auction.status === "DRAFT") return true;
  if (!auction.endAt) return false;
  const endAt = parseAuctionTime(auction.endAt).getTime();
  return !Number.isNaN(endAt) && Date.now() < endAt;
}

function isLegacyRemoved(auction) {
  return (
    auction.status === "REMOVED" && !auction.removedAt && !auction.deletedAt
  );
}
function openSingleAction(action, auction) {
  if (submitting.value || loading.value) return;
  if (action === "delete" && (!auction.removedAt || auction.deletedAt)) return;
  if (action === "restore" && !canRestoreAuction(auction)) return;
  if (action === "remove" && (auction.removedAt || auction.deletedAt)) return;
  openActionDialog(
    action,
    "single",
    [auction.auctionId],
    auction.productName || "",
  );
}

function openBatchAction(action) {
  if (!batchMode.value || submitting.value || loading.value) return;
  const allowed = {
    remove: canBatchRemove.value,
    restore: canBatchRestore.value,
    delete: canBatchDelete.value,
  }[action];
  if (!allowed) {
    errorMessage.value = {
      remove: "批量下架只能選擇尚未下架的競標",
      restore: "批量恢復只能選擇可恢復且尚未刪除的已下架競標",
      delete: "批量刪除只能選擇已下架且尚未刪除的競標",
    }[action];
    return;
  }
  openActionDialog(action, "batch", [...selectedIds.value], "");
}

function openActionDialog(action, mode, ids, targetName) {
  clearActionMessages();
  dialog.action = action;
  dialog.mode = mode;
  dialog.ids = ids;
  dialog.targetName = targetName;
  dialog.reason = "";
  dialog.confirmed = false;
  dialog.error = "";
  dialog.open = true;
}

function closeActionDialog() {
  if (submitting.value) return;
  resetDialog();
}

function resetDialog() {
  dialog.open = false;
  dialog.ids = [];
  dialog.targetName = "";
  dialog.reason = "";
  dialog.confirmed = false;
  dialog.error = "";
}

async function confirmAction() {
  if (!dialogValid.value || submitting.value) return;
  submitting.value = true;
  dialog.error = "";
  const action = dialog.action;
  const mode = dialog.mode;
  const ids = [...dialog.ids];
  const reason = dialog.reason.trim();
  try {
    if (mode === "batch") {
      let response;
      if (action === "remove")
        response = await removeAdminAuctionsBatch(ids, reason);
      else if (action === "restore")
        response = await restoreAdminAuctionsBatch(ids);
      else response = await deleteAdminAuctionsBatch(ids, reason);
      const result = response.data.data;
      batchItems.value = result.items;
      clearSelection();
      successMessage.value = `處理 ${result.totalCount} 筆：成功 ${result.successCount}，失敗 ${result.failureCount}`;
    } else {
      if (action === "remove") await removeAdminAuction(ids[0], reason);
      else if (action === "restore") await restoreAdminAuction(ids[0]);
      else await deleteAdminAuction(ids[0], reason);
      successMessage.value = `競標 #${ids[0]}${dialogActionLabel.value}成功`;
    }
    resetDialog();
    await loadAuctions(pagination.page);
  } catch (error) {
    if (await handleAccessError(error)) {
      resetDialog();
      return;
    }
    dialog.error = apiError(error, `${dialogActionLabel.value}操作失敗`);
  } finally {
    submitting.value = false;
  }
}

function clearActionMessages() {
  errorMessage.value = "";
  successMessage.value = "";
  batchItems.value = [];
}

async function handleAccessError(error) {
  if (error.response?.status === 401) {
    await router.replace({
      name: "login",
      query: { redirect: "/admin/auctions" },
    });
    return true;
  }
  if (error.response?.status === 403) {
    await router.replace({ name: "forbidden" });
    return true;
  }
  return false;
}

function apiError(error, fallback) {
  return error.response?.data?.message || fallback;
}

function formatMoney(value) {
  const amount = Number(value);
  return Number.isFinite(amount)
    ? `NT$ ${Math.round(amount).toLocaleString("zh-TW")}`
    : "—";
}

function formatStatus(status) {
  return status ? formatMemberAuctionStatus(status) : "—";
}

function formatManagementState(state) {
  return (
    { VISIBLE: "公開中", REMOVED: "已下架", DELETED: "已刪除" }[state] || state
  );
}

function managementBadge(state) {
  return (
    {
      VISIBLE: "text-bg-success",
      REMOVED: "text-bg-warning",
      DELETED: "text-bg-danger",
    }[state] || "text-bg-secondary"
  );
}

onMounted(() => loadAuctions(0));
</script>

<style scoped>
.admin-auctions {
  max-width: 1500px;
}
.filter-field-label {
  white-space: nowrap;
  word-break: keep-all;
}
/* Compact auction cards keep statuses and controls with their product. */
.auction-list {
  display: grid;
  gap: 12px;
  padding: 16px;
  background: #f8f9fa;
  container-type: inline-size;
}
.auction-list-message {
  padding: 36px 20px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  text-align: center;
}
.auction-card {
  position: relative;
  min-width: 0;
  padding: 20px 22px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}
.auction-card:hover {
  border-color: #cfd4dc;
}
.auction-card.is-selected {
  border-color: #98a2b3;
  background: #fcfcfd;
}
.auction-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}
.auction-product {
  display: flex;
  flex: 1;
  align-items: flex-start;
  gap: 16px;
  min-width: 0;
}
.auction-card.is-batch .auction-product {
  padding-right: 36px;
}
.auction-card-controls {
  display: flex;
  flex: 0 1 auto;
  align-items: flex-start;
  justify-content: flex-end;
  max-width: 75%;
  min-width: 0;
}
.auction-card.is-batch .auction-card-controls {
  padding-right: 36px;
}
.auction-card.is-batch .batch-checkbox {
  position: absolute;
  top: 22px;
  right: 22px;
}
.auction-statuses {
  display: grid;
  gap: 8px;
  justify-items: start;
}
.auction-status-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #475467;
  font-size: 0.875rem;
  font-weight: 600;
  white-space: nowrap;
}
.auction-status-item .badge {
  padding: 0.4em 0.65em;
  font-size: 0.875rem;
  font-weight: 600;
}
.auction-card-meta {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) minmax(0, 1.5fr) auto;
  gap: 18px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}
.auction-card.is-batch .auction-card-meta {
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) minmax(0, 1.5fr);
}
.auction-meta-actions {
  align-self: center;
  justify-self: end;
}
.auction-meta-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  overflow-wrap: anywhere;
}
.auction-meta-item strong {
  color: #111;
  font-size: 1.05rem;
  font-weight: 700;
}
.auction-field-label {
  display: block;
  margin-bottom: 3px;
  color: #667085;
  font-size: 0.72rem;
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: 0.03em;
}
.auction-card-reasons {
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: 8px;
  color: #667085;
  background: #f8f9fa;
  font-size: 0.82rem;
  line-height: 1.5;
}
@container (max-width: 680px) {
  .auction-card-meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .auction-meta-time {
    grid-column: 1;
  }
  .auction-card.is-batch .auction-meta-time {
    grid-column: 1 / -1;
  }
  .auction-meta-actions {
    grid-column: 2;
  }
}
@container (max-width: 520px) {
  .auction-card-head {
    flex-wrap: wrap;
    gap: 14px;
  }
  .auction-card-controls {
    width: 100%;
    max-width: none;
  }
}
@media (max-width: 576px) {
  .auction-list {
    padding: 12px;
  }
  .auction-card {
    padding: 16px;
  }
  .auction-product {
    gap: 12px;
  }
  .auction-card.is-batch .batch-checkbox {
    top: 18px;
    right: 16px;
  }
}
.batch-checkbox {
  width: 1.25rem;
  height: 1.25rem;
  margin: 0;
  vertical-align: middle;
  cursor: pointer;
  background-color: #fff;
  border: 1.5px solid #98a2b3;
  border-radius: 5px;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.08);
  transition: border-color 150ms ease, background-color 150ms ease, box-shadow 150ms ease;
}
.batch-checkbox:hover:not(:disabled) {
  border-color: #344054;
  box-shadow: 0 0 0 3px #f2f4f7;
}
.batch-checkbox:checked {
  background-color: #111;
  border-color: #111;
  box-shadow: none;
}
.batch-checkbox:checked:hover:not(:disabled) {
  background-color: #303030;
  border-color: #303030;
}
.batch-checkbox:focus-visible {
  outline: 2px solid #111;
  outline-offset: 2px;
  box-shadow: none;
}
.batch-checkbox:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}
/* Batch controls follow the site's neutral admin action style. */
.auction-batch-actions .auction-batch-count {
  display: inline-flex;
  align-items: center;
  min-height: 40px;
  padding: 0 12px;
  color: #344054 !important;
  background: #f2f4f7 !important;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 0.8rem !important;
  font-weight: 600;
  white-space: nowrap;
}
.auction-action-buttons .btn {
  min-height: 40px;
  padding: 9px 14px;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  box-shadow: none;
  transition: background-color 150ms ease, border-color 150ms ease, color 150ms ease;
}
.auction-action-buttons .btn-warning,
.auction-action-buttons .btn-outline-secondary {
  color: #344054;
  background: #fff;
  border-color: #cfd4dc;
}
.auction-action-buttons .btn-warning:hover:not(:disabled),
.auction-action-buttons .btn-outline-secondary:hover:not(:disabled) {
  color: #111;
  background: #f2f4f7;
  border-color: #98a2b3;
}
.auction-action-buttons .btn-success {
  color: #fff;
  background: #111;
  border-color: #111;
}
.auction-action-buttons .btn-success:hover:not(:disabled) {
  color: #fff;
  background: #303030;
  border-color: #303030;
}
.auction-action-buttons .btn-danger {
  color: #b42318;
  background: #fff;
  border-color: #f2c1c5;
}
.auction-action-buttons .btn-danger:hover:not(:disabled) {
  color: #912018;
  background: #fef3f2;
  border-color: #e59a9f;
}
.auction-batch-actions .auction-select-page {
  color: #111;
  background: #f2f4f7;
  border-color: #d0d5dd;
}
.auction-batch-actions .auction-select-page:hover:not(:disabled) {
  color: #111;
  background: #e9ecef;
  border-color: #98a2b3;
}
.auction-action-buttons .btn:focus-visible {
  outline: 2px solid #111;
  outline-offset: 2px;
  box-shadow: none;
}
.auction-action-buttons .btn:disabled {
  color: #98a2b3;
  background: #f8f9fa;
  border-color: #e5e7eb;
  opacity: 0.6;
  box-shadow: none;
}
.product-info {
  min-width: 0;
}
.auction-product-id {
  margin-bottom: 2px;
  color: #667085;
  font-size: 0.78rem;
  font-weight: 600;
}
.auction-thumbnail {
  width: 4rem;
  height: 4rem;
  flex: 0 0 4rem;
  border: 1px solid var(--bs-border-color);
  border-radius: 0.5rem;
  object-fit: cover;
  background: #fff;
}
.thumbnail-placeholder {
  display: grid;
  place-items: center;
  color: #6c757d;
  font-size: 0.75rem;
  background: #f8f9fa;
}
.auction-name-link {
  display: block;
  overflow-wrap: anywhere;
  word-break: normal;
  color: #111;
  font-size: 1.05rem;
  font-weight: 700;
  line-height: 1.35;
  text-decoration: none;
}
.auction-name-link:hover,
.auction-name-link:focus {
  text-decoration: underline;
}
.reason-text {
  max-width: 100%;
  overflow-wrap: anywhere;
}
.action-button {
  min-width: 4.5rem;
  white-space: nowrap;
  box-shadow: none;
}
.action-dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 1080;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow-y: auto;
  padding: 1.25rem;
  background: rgba(17, 24, 39, 0.62);
}
.action-dialog {
  width: min(100%, 36rem);
}
.confirmation-check {
  padding: 0.9rem 0.9rem 0.9rem 2.4rem;
  border: 1px solid var(--bs-border-color);
  border-radius: 0.5rem;
  background: var(--bs-light);
}
textarea {
  resize: vertical;
}
</style>
