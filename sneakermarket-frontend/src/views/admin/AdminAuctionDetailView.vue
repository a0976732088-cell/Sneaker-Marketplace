<template>
  <main class="container py-4 admin-auction-detail">
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
      <div>
        <p class="text-uppercase text-secondary small mb-1">Admin moderation</p>
        <h1 class="h3 mb-1">競標管理詳情</h1>
        <p class="text-secondary mb-0">此頁可查看公開中、已下架及已刪除的競標紀錄。</p>
      </div>
      <RouterLink class="btn btn-outline-secondary" :to="{ name: 'admin-auctions' }">返回競標管理</RouterLink>
    </div>

    <div v-if="loading" class="card shadow-sm">
      <div class="card-body py-5 text-center text-secondary">載入中…</div>
    </div>
    <div v-else-if="errorMessage" class="alert alert-danger" role="alert">{{ errorMessage }}</div>

    <template v-else-if="detail">
      <section class="card shadow-sm mb-4">
        <div class="card-body">
          <div class="d-flex flex-wrap justify-content-between gap-3">
            <div>
              <div class="text-secondary small mb-1">競標 #{{ auction.auctionId }}</div>
              <h2 class="h4 mb-1">{{ auction.productName }}</h2>
              <p class="text-secondary mb-0">{{ auction.model || "無型號" }}</p>
            </div>
            <div class="d-flex align-items-start gap-2">
              <span class="badge text-bg-secondary fs-6">{{ formatStatus(auction.status) }}</span>
              <span class="badge fs-6" :class="managementBadge(auction.managementState)">
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
                <div v-for="(imageUrl, index) in visibleImages" :key="imageUrl" class="col-12 col-md-6">
                  <img
                    class="detail-image"
                    :src="resolveAuctionImageUrl(imageUrl)"
                    :alt="auction.productName + ' 圖片 ' + (index + 1)"
                    @error="markImageBroken(imageUrl)"
                  />
                </div>
              </div>
              <div v-else class="image-placeholder">目前沒有可顯示的商品圖片</div>
            </div>
          </section>
        </div>

        <div class="col-12 col-xl-5">
          <section class="card shadow-sm mb-4">
            <div class="card-header fw-semibold">競標資料</div>
            <dl class="row card-body detail-list mb-0">
              <dt class="col-5">賣家 ID</dt><dd class="col-7">#{{ auction.sellerId }}</dd>
              <dt class="col-5">目前價格</dt><dd class="col-7">{{ formatMoney(auction.currentPrice) }}</dd>
              <dt class="col-5">出價次數</dt><dd class="col-7">{{ auction.bidCount }}</dd>
              <dt class="col-5">開始時間</dt><dd class="col-7">{{ formatDateTime(auction.startAt) }}</dd>
              <dt class="col-5">結束時間</dt><dd class="col-7">{{ formatDateTime(auction.endAt) }}</dd>
              <dt class="col-5">建立時間</dt><dd class="col-7">{{ formatDateTime(auction.createdAt) }}</dd>
            </dl>
          </section>

          <section class="card shadow-sm">
            <div class="card-header fw-semibold">管理紀錄</div>
            <dl class="row card-body detail-list mb-0">
              <dt class="col-5">下架時間</dt><dd class="col-7">{{ formatDateTime(auction.removedAt) }}</dd>
              <dt class="col-5">下架管理員</dt><dd class="col-7">{{ formatMemberId(auction.removedByMemberId) }}</dd>
              <dt class="col-5">下架前狀態</dt><dd class="col-7">{{ formatStatus(auction.removedFromStatus) }}</dd>
              <dt class="col-5">下架原因</dt><dd class="col-7 reason-value">{{ auction.removeReason || "—" }}</dd>
              <dt class="col-5">刪除時間</dt><dd class="col-7">{{ formatDateTime(auction.deletedAt) }}</dd>
              <dt class="col-5">刪除管理員</dt><dd class="col-7">{{ formatMemberId(auction.deletedByMemberId) }}</dd>
              <dt class="col-5">刪除原因</dt><dd class="col-7 reason-value">{{ auction.deleteReason || "—" }}</dd>
            </dl>
          </section>
        </div>
      </div>

      <section class="card shadow-sm mt-4">
        <div class="card-header fw-semibold">商品說明</div>
        <dl class="row card-body detail-list mb-0">
          <dt class="col-md-3">尺寸</dt><dd class="col-md-9">{{ detail.size || "—" }}</dd>
          <dt class="col-md-3">商品狀況</dt><dd class="col-md-9">{{ formatCondition(detail.condition) }}</dd>
          <dt class="col-md-3">所在地</dt><dd class="col-md-9">{{ detail.location || "—" }}</dd>
          <dt class="col-md-3">商品介紹</dt><dd class="col-md-9 pre-wrap">{{ detail.description || "—" }}</dd>
          <dt class="col-md-3">瑕疵說明</dt><dd class="col-md-9 pre-wrap">{{ detail.defectNote || "—" }}</dd>
          <dt class="col-md-3">真偽／來源</dt><dd class="col-md-9 pre-wrap">{{ detail.authenticityNote || "—" }}</dd>
          <dt class="col-md-3">運送方式</dt><dd class="col-md-9">{{ detail.shippingMethod || "—" }}</dd>
          <dt class="col-md-3">運費</dt><dd class="col-md-9">{{ formatMoney(detail.shippingFee) }}</dd>
          <dt class="col-md-3">付款方式</dt><dd class="col-md-9">{{ detail.paymentMethod || "—" }}</dd>
          <dt class="col-md-3">面交／退貨</dt>
          <dd class="col-md-9">{{ detail.allowMeetup ? "可面交" : "不可面交" }}／{{ detail.allowReturn ? "可退貨" : "不可退貨" }}</dd>
          <dt class="col-md-3">保固說明</dt><dd class="col-md-9 pre-wrap">{{ detail.warrantyNote || "—" }}</dd>
        </dl>
      </section>
    </template>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { getAdminAuctionDetail } from "@/service/auctionAdminService";
import { resolveAuctionImageUrl } from "@/utils/auctionImageUrl";

const route = useRoute();
const router = useRouter();
const detail = ref(null);
const loading = ref(false);
const errorMessage = ref("");
const brokenImages = ref(new Set());

const auction = computed(() => detail.value?.auction || {});
const visibleImages = computed(() => (detail.value?.imageUrls || [])
  .filter((imageUrl) => !brokenImages.value.has(imageUrl)));

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
    errorMessage.value = error.response?.status === 404
      ? "找不到指定的競標資料"
      : "無法載入競標管理詳情";
  } finally {
    loading.value = false;
  }
}

function formatMoney(value) {
  const amount = Number(value);
  return Number.isFinite(amount) ? "NT$ " + Math.round(amount).toLocaleString("zh-TW") : "—";
}

function formatDateTime(value) {
  if (!value) return "—";
  const date = new Date(value + "+08:00");
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat("zh-TW", {
    timeZone: "Asia/Taipei", year: "numeric", month: "2-digit", day: "2-digit",
    hour: "2-digit", minute: "2-digit", hour12: false,
  }).format(date);
}

function formatMemberId(value) {
  return value ? "#" + value : "—";
}

function formatStatus(status) {
  if (!status) return "—";
  return ({ DRAFT: "草稿", SCHEDULED: "尚未開始", ACTIVE: "競標中", SOLD: "已成交", UNSOLD: "已流標", CANCELLED: "已取消", REMOVED: "廢標" })[status] || status;
}

function formatManagementState(state) {
  return ({ VISIBLE: "公開中", REMOVED: "已下架", DELETED: "已刪除" })[state] || state;
}

function managementBadge(state) {
  return ({ VISIBLE: "text-bg-success", REMOVED: "text-bg-warning", DELETED: "text-bg-danger" })[state] || "text-bg-secondary";
}

function formatCondition(condition) {
  return ({ NEW: "全新", LIKE_NEW: "近全新", USED: "二手" })[condition] || condition || "—";
}

onMounted(loadDetail);
</script>

<style scoped>
.admin-auction-detail { max-width: 1320px; }
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
.detail-list dd { margin-bottom: 0.8rem; }
.detail-list dd { overflow-wrap: anywhere; }
.reason-value,
.pre-wrap { white-space: pre-wrap; }
</style>
