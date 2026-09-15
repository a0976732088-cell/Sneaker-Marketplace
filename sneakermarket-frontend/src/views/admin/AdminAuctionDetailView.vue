<template>
  <main class="container py-4 admin-auction-detail">
    <div
      class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4"
    >
      <div>
        <p class="text-uppercase text-secondary small mb-1">Admin moderation</p>
        <h1 class="h3 mb-1">競標管理詳情</h1>
        <p class="text-secondary mb-0">
          此頁可唯讀查看公開中、已下架及已刪除競標的完整生命週期。
        </p>
      </div>
      <RouterLink
        class="btn btn-outline-secondary"
        :to="{ name: 'admin-auctions' }"
      >
        返回競標管理
      </RouterLink>
    </div>

    <div v-if="loading" class="card shadow-sm">
      <div class="card-body py-5 text-center text-secondary">載入中…</div>
    </div>
    <div v-else-if="errorMessage" class="alert alert-danger" role="alert">
      {{ errorMessage }}
    </div>

    <template v-else-if="detail">
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
                    @error="markImageBroken(imageUrl)"
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

      <section class="card shadow-sm mt-4">
        <div class="card-header fw-semibold">出價紀錄</div>
        <div v-if="bids.length" class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead class="table-light">
              <tr>
                <th>出價金額</th>
                <th>出價會員</th>
                <th>出價時間</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(bid, index) in bids"
                :key="`${bid.createdAt}-${bid.bidderName}-${bid.bidAmount}-${index}`"
              >
                <td class="fw-semibold">{{ formatMoney(bid.bidAmount) }}</td>
                <td>{{ bid.bidderName || "會員資料未提供" }}</td>
                <td>{{ formatTime(bid.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="card-body text-secondary">尚無出價紀錄</div>
      </section>

      <div class="row g-4 mt-0">
        <div class="col-12 col-xl-5">
          <section class="card shadow-sm h-100">
            <div class="card-header fw-semibold">競標結果</div>
            <div v-if="!result" class="card-body text-secondary">
              尚未產生競標結果
            </div>
            <dl v-else class="row card-body detail-list mb-0">
              <dt class="col-5">結果狀態</dt>
              <dd class="col-7">
                <span class="badge" :class="resultBadge(result.resultStatus)">
                  {{ formatAuctionResultStatus(result.resultStatus) }}
                </span>
              </dd>
              <dt class="col-5">成交價格</dt>
              <dd class="col-7">{{ formatMoney(result.finalPrice) }}</dd>
              <dt class="col-5">得標會員</dt>
              <dd class="col-7">{{ result.winnerName || "—" }}</dd>
              <dt class="col-5">結標時間</dt>
              <dd class="col-7">{{ formatTime(result.endedAt) }}</dd>
              <dt class="col-5">結果建立</dt>
              <dd class="col-7">{{ formatTime(result.createdAt) }}</dd>
              <dt class="col-5">結果原因</dt>
              <dd class="col-7 reason-value">{{ result.reason || "—" }}</dd>
            </dl>
          </section>
        </div>

        <div class="col-12 col-xl-7">
          <section class="card shadow-sm h-100">
            <div class="card-header fw-semibold">競標訂單</div>
            <div v-if="!order" class="card-body text-secondary">
              尚未建立競標訂單
            </div>
            <div v-else class="card-body">
              <div
                v-if="order.orderStatus === 'ABANDONED'"
                class="alert alert-warning py-2"
              >
                此訂單已棄標（買家逾期未付款），不能再進行付款。
              </div>
              <dl class="row detail-list mb-0">
                <dt class="col-5">訂單編號</dt>
                <dd class="col-7">{{ order.orderNumber }}</dd>
                <dt class="col-5">訂單狀態</dt>
                <dd class="col-7 fw-semibold">
                  {{ formatAuctionOrderStatus(order.orderStatus) }}
                </dd>
                <dt class="col-5">付款狀態</dt>
                <dd class="col-7">
                  {{ formatAuctionOrderStatus(order.paymentStatus) }}
                </dd>
                <dt class="col-5">物流狀態</dt>
                <dd class="col-7">
                  {{ formatAuctionOrderStatus(order.shippingStatus) }}
                </dd>
                <dt class="col-5">成交價格</dt>
                <dd class="col-7">{{ formatMoney(order.finalPrice) }}</dd>
                <dt class="col-5">買家</dt>
                <dd class="col-7">{{ order.buyerName || "會員資料未提供" }}</dd>
                <dt class="col-5">賣家</dt>
                <dd class="col-7">{{ order.sellerName || "會員資料未提供" }}</dd>
                <dt class="col-5">訂單建立</dt>
                <dd class="col-7">{{ formatTime(order.createdAt) }}</dd>
                <dt class="col-5">付款期限</dt>
                <dd class="col-7">{{ formatTime(order.paymentDueAt) }}</dd>
                <dt class="col-5">付款時間</dt>
                <dd class="col-7">{{ formatTime(order.paidAt) }}</dd>
                <dt class="col-5">棄標時間</dt>
                <dd class="col-7">{{ formatTime(order.abandonedAt) }}</dd>
                <dt class="col-5">出貨時間</dt>
                <dd class="col-7">{{ formatTime(order.shippedAt) }}</dd>
                <dt class="col-5">完成時間</dt>
                <dd class="col-7">{{ formatTime(order.completedAt) }}</dd>
                <dt class="col-5">最後更新</dt>
                <dd class="col-7">{{ formatTime(order.updatedAt) }}</dd>
              </dl>
            </div>
          </section>
        </div>
      </div>

      <section class="card shadow-sm mt-4">
        <div class="card-header fw-semibold">完整生命週期</div>
        <div v-if="timeline.length" class="card-body lifecycle-timeline">
          <article
            v-for="event in timeline"
            :key="`${event.type}-${event.occurredAt}`"
            class="timeline-event"
          >
            <span class="timeline-marker" aria-hidden="true"></span>
            <div>
              <h3 class="h6 mb-1">{{ event.label }}</h3>
              <time class="text-secondary small">{{ formatTime(event.occurredAt) }}</time>
              <div v-if="event.amount != null" class="small mt-1">
                {{ formatMoney(event.amount) }}
                <span v-if="event.actorName"> · {{ event.actorName }}</span>
              </div>
              <div v-else-if="event.orderNumber" class="small mt-1">
                {{ event.orderNumber }}
              </div>
            </div>
          </article>
        </div>
        <div v-else class="card-body text-secondary">目前沒有可顯示的生命週期事件</div>
      </section>

      <section class="card shadow-sm mt-4">
        <div class="card-header fw-semibold">商品說明</div>
        <dl class="row card-body detail-list mb-0">
          <dt class="col-md-3">所在地</dt>
          <dd class="col-md-9">{{ detail.location || "—" }}</dd>
          <dt class="col-md-3">商品介紹</dt>
          <dd class="col-md-9 pre-wrap">{{ detail.description || "—" }}</dd>
          <dt class="col-md-3">瑕疵說明</dt>
          <dd class="col-md-9 pre-wrap">{{ detail.defectNote || "—" }}</dd>
          <dt class="col-md-3">真偽／來源</dt>
          <dd class="col-md-9 pre-wrap">{{ detail.authenticityNote || "—" }}</dd>
          <dt class="col-md-3">運送方式</dt>
          <dd class="col-md-9">{{ detail.shippingMethod || "—" }}</dd>
          <dt class="col-md-3">運費</dt>
          <dd class="col-md-9">{{ formatMoney(detail.shippingFee) }}</dd>
          <dt class="col-md-3">付款方式</dt>
          <dd class="col-md-9">{{ detail.paymentMethod || "—" }}</dd>
          <dt class="col-md-3">面交／退貨</dt>
          <dd class="col-md-9">
            {{ detail.allowMeetup ? "可面交" : "不可面交" }}／{{
              detail.allowReturn ? "可退貨" : "不可退貨"
            }}
          </dd>
          <dt class="col-md-3">保固說明</dt>
          <dd class="col-md-9 pre-wrap">{{ detail.warrantyNote || "—" }}</dd>
        </dl>
      </section>

      <section class="card shadow-sm mt-4">
        <div class="card-header fw-semibold">管理紀錄</div>
        <dl class="row card-body detail-list mb-0">
          <dt class="col-md-3">下架時間</dt>
          <dd class="col-md-9">{{ formatTime(auction.removedAt) }}</dd>
          <dt class="col-md-3">下架管理員</dt>
          <dd class="col-md-9">{{ formatMemberId(auction.removedByMemberId) }}</dd>
          <dt class="col-md-3">下架前狀態</dt>
          <dd class="col-md-9">{{ formatStatus(auction.removedFromStatus) }}</dd>
          <dt class="col-md-3">下架原因</dt>
          <dd class="col-md-9 reason-value">{{ auction.removeReason || "—" }}</dd>
          <dt class="col-md-3">刪除時間</dt>
          <dd class="col-md-9">{{ formatTime(auction.deletedAt) }}</dd>
          <dt class="col-md-3">刪除管理員</dt>
          <dd class="col-md-9">{{ formatMemberId(auction.deletedByMemberId) }}</dd>
          <dt class="col-md-3">刪除原因</dt>
          <dd class="col-md-9 reason-value">{{ auction.deleteReason || "—" }}</dd>
        </dl>
      </section>
    </template>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { getAdminAuctionDetail } from "@/service/auctionAdminService";
import { buildAdminAuctionTimeline } from "@/utils/auctionAdminLifecycle";
import { resolveAuctionImageUrl } from "@/utils/auctionImageUrl";
import {
  formatAuctionOrderStatus,
  formatAuctionResultStatus,
  formatMemberAuctionStatus,
} from "@/utils/auctionMemberStatus";
import { formatAuctionTime } from "@/utils/auctionTime";

const route = useRoute();
const router = useRouter();
const detail = ref(null);
const loading = ref(false);
const errorMessage = ref("");
const brokenImages = ref(new Set());

const auction = computed(() => detail.value?.auction || {});
const bids = computed(() =>
  Array.isArray(detail.value?.bids) ? detail.value.bids : [],
);
const result = computed(() => detail.value?.result || null);
const order = computed(() => detail.value?.order || null);
const timeline = computed(() => buildAdminAuctionTimeline(detail.value));
const visibleImages = computed(() =>
  (detail.value?.imageUrls || []).filter(
    (imageUrl) => !brokenImages.value.has(imageUrl),
  ),
);

function markImageBroken(imageUrl) {
  brokenImages.value = new Set([...brokenImages.value, imageUrl]);
}

async function loadDetail() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const response = await getAdminAuctionDetail(route.params.id);
    detail.value = response.data.data;
  } catch (error) {
    if (error.response?.status === 401) {
      await router.replace({ name: "login", query: { redirect: route.fullPath } });
      return;
    }
    if (error.response?.status === 403) {
      await router.replace({ name: "forbidden" });
      return;
    }
    errorMessage.value =
      error.response?.status === 404
        ? "找不到指定的競標資料"
        : "無法載入競標管理詳情";
  } finally {
    loading.value = false;
  }
}

function formatMoney(value) {
  const amount = Number(value);
  return value !== null && value !== undefined && Number.isFinite(amount)
    ? "NT$ " + Math.round(amount).toLocaleString("zh-TW")
    : "—";
}

function formatTime(value) {
  return value ? formatAuctionTime(value) : "—";
}

function formatMemberId(value) {
  return value ? "#" + value : "—";
}

function formatStatus(status) {
  return status ? formatMemberAuctionStatus(status) : "—";
}

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

function resultBadge(status) {
  return (
    {
      SOLD: "text-bg-success",
      UNSOLD: "text-bg-secondary",
      CANCELLED: "text-bg-warning",
      REMOVED: "text-bg-danger",
    }[status] || "text-bg-secondary"
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

onMounted(loadDetail);
</script>

<style scoped>
.admin-auction-detail {
  max-width: 1320px;
}
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
.reason-value,
.pre-wrap {
  white-space: pre-wrap;
}
.lifecycle-timeline {
  padding-left: 2rem;
}
.timeline-event {
  position: relative;
  min-height: 4.5rem;
  padding: 0 0 1.25rem 1.75rem;
  border-left: 2px solid var(--bs-border-color);
}
.timeline-event:last-child {
  min-height: 0;
  padding-bottom: 0;
  border-left-color: transparent;
}
.timeline-marker {
  position: absolute;
  top: 0.15rem;
  left: -0.48rem;
  width: 0.85rem;
  height: 0.85rem;
  border: 2px solid #fff;
  border-radius: 50%;
  background: var(--bs-primary);
  box-shadow: 0 0 0 1px var(--bs-primary);
}
</style>
