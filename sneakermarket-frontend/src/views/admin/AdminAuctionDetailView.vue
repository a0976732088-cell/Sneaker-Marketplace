<template>
  <main class="container py-4 admin-auction-detail">
    <header class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4 admin-page-heading">
      <div>
        <p class="text-uppercase text-secondary small mb-1 admin-page-eyebrow">酷客 Kicks ADMIN</p>
        <h1 class="h3 mb-1">競標管理詳情</h1>
        <p class="text-secondary mb-0 admin-page-description">
          此頁可唯讀查看公開中、已下架及已刪除競標的完整生命週期。
        </p>
      </div>
      <RouterLink
        class="btn btn-outline-secondary"
        :to="{ name: 'admin-auctions' }"
      >
        返回競標管理
      </RouterLink>
    </header>

    <div v-if="loading" class="card shadow-sm">
      <div class="card-body py-5 text-center text-secondary">載入中…</div>
    </div>
    <div v-else-if="errorMessage" class="alert alert-danger" role="alert">
      {{ errorMessage }}
    </div>

    <template v-else-if="detail">
      <AdminAuctionOverviewPanel
        :auction="auction"
        :detail="detail"
        :order="order"
        :visible-images="visibleImages"
        :format-status="formatStatus"
        :format-money="formatMoney"
        :format-time="formatTime"
        @image-error="markImageBroken"
      />

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

      <AdminAuctionResultAndOrderPanel
        :result="result"
        :order="order"
        :format-money="formatMoney"
        :format-time="formatTime"
      />

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
import AdminAuctionOverviewPanel from "@/components/auction/admin/AdminAuctionOverviewPanel.vue";
import AdminAuctionResultAndOrderPanel from "@/components/auction/admin/AdminAuctionResultAndOrderPanel.vue";
import { getAdminAuctionDetail } from "@/service/auctionAdminService";
import { buildAdminAuctionTimeline } from "@/utils/auctionAdminLifecycle";
import { formatMemberAuctionStatus } from "@/utils/auctionMemberStatus";
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

onMounted(loadDetail);
</script>

<style scoped>
.admin-auction-detail {
  max-width: 1320px;
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
