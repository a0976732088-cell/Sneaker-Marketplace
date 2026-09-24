<template>
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
      @click="emit('enter')"
    >
      批量操作
    </button>

    <div v-else class="d-flex flex-wrap align-items-center gap-2 auction-batch-actions auction-action-buttons">
      <span class="badge text-bg-primary fs-6 auction-batch-count"
        >已選 {{ selectedCount }} 筆（可跨頁）</span
      >
      <button
        class="btn btn-outline-secondary auction-select-page"
        type="button"
        :aria-pressed="allCurrentPageSelected"
        :disabled="loading || submitting || currentSelectableCount === 0"
        @click="emit('toggle-current-page')"
      >
        {{ allCurrentPageSelected ? "取消本頁全選" : "全選本頁" }}
      </button>
      <button
        class="btn btn-warning fw-semibold"
        type="button"
        :disabled="loading || submitting || !canBatchRemove"
        @click="emit('remove')"
      >
        批量下架
      </button>
      <button
        class="btn btn-success"
        type="button"
        :disabled="loading || submitting || !canBatchRestore"
        @click="emit('restore')"
      >
        批量恢復顯示
      </button>
      <button
        class="btn btn-danger"
        type="button"
        :disabled="loading || submitting || !canBatchDelete"
        @click="emit('delete')"
      >
        批量刪除
      </button>
      <button
        class="btn btn-outline-secondary"
        type="button"
        :disabled="submitting"
        @click="emit('exit')"
      >
        結束批量操作
      </button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  batchMode: { type: Boolean, required: true },
  selectedCount: { type: Number, required: true },
  allCurrentPageSelected: { type: Boolean, required: true },
  currentSelectableCount: { type: Number, required: true },
  canBatchRemove: { type: Boolean, required: true },
  canBatchRestore: { type: Boolean, required: true },
  canBatchDelete: { type: Boolean, required: true },
  loading: { type: Boolean, required: true },
  submitting: { type: Boolean, required: true },
  batchSelectionText: { type: String, required: true },
  batchSelectionClass: { type: String, required: true },
});

const emit = defineEmits([
  "enter",
  "exit",
  "toggle-current-page",
  "remove",
  "restore",
  "delete",
]);
</script>

<style scoped>
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
</style>
