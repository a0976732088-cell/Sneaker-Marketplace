<template>
  <section class="card shadow-sm mb-4">
    <div class="card-body">
      <div class="d-flex flex-wrap justify-content-between gap-3">
        <div>
          <div class="text-secondary small mb-1">
            競標 #{{ auction.auctionId }}
          </div>
          <h2 class="h4 mb-1">{{ auction.productName }}</h2>
          <p class="text-secondary mb-0">{{ auction.model || "無型號" }}</p>
        </div>
        <div class="d-flex flex-wrap align-items-start gap-2">
          <span class="badge text-bg-secondary fs-6">
            {{ formatStatus(auction.status) }}
          </span>
          <span
            v-if="order"
            class="badge fs-6"
            :class="orderBadge(order.orderStatus)"
          >
            {{ formatAuctionOrderStatus(order.orderStatus) }}
          </span>
          <span
            class="badge fs-6"
            :class="managementBadge(auction.managementState)"
          >
            {{ formatManagementState(auction.managementState) }}
          </span>
        </div>
      </div>
    </div>
  </section>

  <div class="row g-4">
    <div class="col-12 col-xl-7">
      <section class="card shadow-sm h-100">
        <div class="card-header fw-semibold">商品圖片</div>
        <div class="card-body">
          <div v-if="visibleImages.length" class="row g-3">
            <div
              v-for="(imageUrl, index) in visibleImages"
              :key="imageUrl"
              class="col-12 col-md-6"
            >
              <img
                class="detail-image"
                :src="resolveAuctionImageUrl(imageUrl)"
                :alt="auction.productName + ' 圖片 ' + (index + 1)"
                @error="emit('image-error', imageUrl)"
              />
            </div>
          </div>
          <div v-else class="image-placeholder">
            目前沒有可顯示的商品圖片
          </div>
        </div>
      </section>
    </div>

    <div class="col-12 col-xl-5">
      <section class="card shadow-sm mb-4">
        <div class="card-header fw-semibold">競標基本資料</div>
        <dl class="row card-body detail-list mb-0">
          <dt class="col-5">品牌</dt>
          <dd class="col-7">{{ detail.brandName || "—" }}</dd>
          <dt class="col-5">型號</dt>
          <dd class="col-7">{{ auction.model || "—" }}</dd>
          <dt class="col-5">尺寸</dt>
          <dd class="col-7">{{ detail.size || "—" }}</dd>
          <dt class="col-5">商品狀況</dt>
          <dd class="col-7">{{ formatCondition(detail.condition) }}</dd>
          <dt class="col-5">賣家</dt>
          <dd class="col-7">
            {{ auction.sellerName || "會員資料未提供" }}
          </dd>
          <dt class="col-5">賣家 ID</dt>
          <dd class="col-7">#{{ auction.sellerId }}</dd>
        </dl>
      </section>

      <section class="card shadow-sm">
        <div class="card-header fw-semibold">價格與時間</div>
        <dl class="row card-body detail-list mb-0">
          <dt class="col-5">起標價格</dt>
          <dd class="col-7">{{ formatMoney(detail.startPrice) }}</dd>
          <dt class="col-5">目前價格</dt>
          <dd class="col-7">{{ formatMoney(auction.currentPrice) }}</dd>
          <dt class="col-5">最小加價</dt>
          <dd class="col-7">{{ formatMoney(detail.minIncrement) }}</dd>
          <dt class="col-5">出價次數</dt>
          <dd class="col-7">{{ auction.bidCount }}</dd>
          <dt class="col-5">建立時間</dt>
          <dd class="col-7">{{ formatTime(auction.createdAt) }}</dd>
          <dt class="col-5">開始時間</dt>
          <dd class="col-7">{{ formatTime(auction.startAt) }}</dd>
          <dt class="col-5">結束時間</dt>
          <dd class="col-7">{{ formatTime(auction.endAt) }}</dd>
          <dt class="col-5">更新時間</dt>
          <dd class="col-7">{{ formatTime(detail.updatedAt) }}</dd>
        </dl>
      </section>
    </div>
  </div>
</template>

<script setup>
import { resolveAuctionImageUrl } from "@/utils/auctionImageUrl";
import { formatAuctionOrderStatus } from "@/utils/auctionMemberStatus";

defineProps({
  auction: { type: Object, required: true },
  detail: { type: Object, required: true },
  order: { type: Object, default: null },
  visibleImages: { type: Array, required: true },
  formatStatus: { type: Function, required: true },
  formatMoney: { type: Function, required: true },
  formatTime: { type: Function, required: true },
});

const emit = defineEmits(["image-error"]);

function formatManagementState(state) {
  return (
    { VISIBLE: "公開中", REMOVED: "已下架", DELETED: "已刪除" }[state] ||
    state
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

function orderBadge(status) {
  return (
    {
      PENDING_PAYMENT: "text-bg-warning",
      PAID: "text-bg-primary",
      SHIPPED: "text-bg-info",
      COMPLETED: "text-bg-success",
      CANCELLED: "text-bg-secondary",
      ABANDONED: "text-bg-danger",
    }[status] || "text-bg-secondary"
  );
}

function formatCondition(condition) {
  return (
    { NEW: "全新", LIKE_NEW: "近全新", USED: "二手" }[condition] ||
    condition ||
    "—"
  );
}
</script>

<style scoped>
.detail-image {
  width: 100%;
  aspect-ratio: 4 / 3;
  border: 1px solid var(--bs-border-color);
  border-radius: 0.6rem;
  object-fit: contain;
  background: #f8f9fa;
}
.image-placeholder {
  display: grid;
  min-height: 16rem;
  place-items: center;
  border: 1px dashed var(--bs-border-color);
  border-radius: 0.6rem;
  color: #6c757d;
  background: #f8f9fa;
}
.detail-list dt,
.detail-list dd {
  margin-bottom: 0.8rem;
}
.detail-list dd {
  overflow-wrap: anywhere;
}
</style>
