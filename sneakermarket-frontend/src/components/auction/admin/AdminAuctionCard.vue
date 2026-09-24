<template>
  <article
    class="auction-card"
    :class="{ 'is-batch': batchMode, 'is-selected': batchMode && selected }"
    role="listitem"
  >
    <div class="auction-card-head">
      <div class="auction-product">
        <img
          v-if="auction.imageUrl && !thumbnailBroken"
          class="auction-thumbnail"
          :src="resolveAuctionImageUrl(auction.imageUrl)"
          :alt="(auction.productName || '競標商品') + '縮圖'"
          loading="lazy"
          @error="emit('image-error')"
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
            第 {{ rowNumber }} 筆 · 競標 #{{ auction.auctionId }}
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
            <span class="badge text-bg-secondary">{{ formatStatus(auction.status) }}</span>
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
          :checked="selected"
          :disabled="loading || submitting || Boolean(auction.deletedAt)"
          @change="emit('toggle-selection')"
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
          v-if="legacyRemoved"
          class="btn btn-sm btn-warning fw-semibold action-button"
          type="button"
          :disabled="loading || submitting"
          @click="emit('remove')"
        >
          補登下架資訊
        </button>
        <button
          v-else-if="!auction.removedAt && !auction.deletedAt"
          class="btn btn-sm btn-warning fw-semibold action-button"
          type="button"
          :disabled="loading || submitting"
          @click="emit('remove')"
        >
          下架
        </button>
        <button
          v-if="canRestore"
          class="btn btn-sm btn-success action-button"
          type="button"
          :disabled="loading || submitting"
          @click="emit('restore')"
        >
          恢復顯示
        </button>
        <button
          v-if="auction.removedAt && !auction.deletedAt"
          class="btn btn-sm btn-danger action-button"
          type="button"
          :disabled="loading || submitting"
          @click="emit('delete')"
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
</template>

<script setup>
import { RouterLink } from "vue-router";
import { resolveAuctionImageUrl } from "@/utils/auctionImageUrl";
import { formatAuctionOrderStatus } from "@/utils/auctionMemberStatus";
import { formatAuctionTime } from "@/utils/auctionTime";

defineProps({
  auction: { type: Object, required: true },
  rowNumber: { type: Number, required: true },
  batchMode: { type: Boolean, required: true },
  selected: { type: Boolean, required: true },
  thumbnailBroken: { type: Boolean, required: true },
  loading: { type: Boolean, required: true },
  submitting: { type: Boolean, required: true },
  canRestore: { type: Boolean, required: true },
  legacyRemoved: { type: Boolean, required: true },
  formatMoney: { type: Function, required: true },
  formatStatus: { type: Function, required: true },
  formatManagementState: { type: Function, required: true },
  managementBadge: { type: Function, required: true },
});

const emit = defineEmits([
  "image-error",
  "toggle-selection",
  "remove",
  "restore",
  "delete",
]);
</script>

<style scoped>
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
</style>
