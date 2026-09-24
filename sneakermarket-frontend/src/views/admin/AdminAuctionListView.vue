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

    <AdminAuctionFilterPanel
      :filters="filters"
      :statuses="statuses"
      :order-statuses="orderStatuses"
      :format-status="formatStatus"
      :loading="loading"
      :submitting="submitting"
      @submit="applyFilters"
    />

    <div v-if="errorMessage" class="alert alert-danger" role="alert">
      {{ errorMessage }}
    </div>
    <div v-if="successMessage" class="alert alert-success" role="status">
      {{ successMessage }}
    </div>

    <section class="card shadow-sm">
      <AdminAuctionBatchToolbar
        :batch-mode="batchMode"
        :selected-count="selectedIds.length"
        :all-current-page-selected="allCurrentPageSelected"
        :current-selectable-count="currentSelectableIds.length"
        :can-batch-remove="canBatchRemove"
        :can-batch-restore="canBatchRestore"
        :can-batch-delete="canBatchDelete"
        :loading="loading"
        :submitting="submitting"
        :batch-selection-text="batchSelectionText"
        :batch-selection-class="batchSelectionClass"
        @enter="enterBatchMode"
        @exit="exitBatchMode"
        @toggle-current-page="toggleCurrentPageFromButton"
        @remove="openBatchAction('remove')"
        @restore="openBatchAction('restore')"
        @delete="openBatchAction('delete')"
      />

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

        <AdminAuctionCard
          v-for="(auction, index) in items"
          :key="auction.auctionId"
          :auction="auction"
          :row-number="rowNumber(index)"
          :batch-mode="batchMode"
          :selected="selectedIds.includes(auction.auctionId)"
          :thumbnail-broken="brokenImageIds.has(auction.auctionId)"
          :loading="loading"
          :submitting="submitting"
          :can-restore="canRestoreAuction(auction)"
          :legacy-removed="isLegacyRemoved(auction)"
          :format-money="formatMoney"
          :format-status="formatStatus"
          :format-management-state="formatManagementState"
          :management-badge="managementBadge"
          @image-error="markThumbnailBroken(auction.auctionId)"
          @toggle-selection="toggleSelection(auction)"
          @remove="openSingleAction('remove', auction)"
          @restore="openSingleAction('restore', auction)"
          @delete="openSingleAction('delete', auction)"
        />


      </div>

      <AdminAuctionPagination
        :page="pagination.page"
        :total-pages="pagination.totalPages"
        :total-elements="pagination.totalElements"
        :loading="loading"
        :submitting="submitting"
        @change="changePage"
      />

    </section>
  </main>

  <AdminAuctionActionDialog
    :open="dialog.open"
    :action="dialog.action"
    v-model:reason="dialog.reason"
    v-model:confirmed="dialog.confirmed"
    :error="dialog.error"
    :title="dialogTitle"
    :target-text="dialogTargetText"
    :warning="dialogWarning"
    :action-label="dialogActionLabel"
    :alert-class="dialogAlertClass"
    :submit-class="dialogSubmitClass"
    :valid="dialogValid"
    :submitting="submitting"
    @close="closeActionDialog"
    @confirm="confirmAction"
  />


</template>

<script setup>
import AdminAuctionFilterPanel from "@/components/auction/admin/AdminAuctionFilterPanel.vue";
import AdminAuctionBatchToolbar from "@/components/auction/admin/AdminAuctionBatchToolbar.vue";
import AdminAuctionCard from "@/components/auction/admin/AdminAuctionCard.vue";
import AdminAuctionPagination from "@/components/auction/admin/AdminAuctionPagination.vue";
import AdminAuctionActionDialog from "@/components/auction/admin/AdminAuctionActionDialog.vue";

import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { formatMemberAuctionStatus } from "@/utils/auctionMemberStatus";
import { parseAuctionTime } from "@/utils/auctionTime";
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
@media (max-width: 576px) {
  .auction-list {
    padding: 12px;
  }
}
</style>
