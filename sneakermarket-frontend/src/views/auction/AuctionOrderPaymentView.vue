<template>
  <main class="auction-order-payment">
    <header class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
      <div>
        <p class="text-uppercase text-muted small fw-semibold mb-1">Auction order</p>
        <h1 class="h2 mb-0">競標訂單付款</h1>
      </div>
      <span v-if="order" class="badge rounded-pill fs-6 px-3 py-2" :class="headlineStatusClass">
        {{ headlineStatusText }}
      </span>
    </header>

    <div v-if="loading" class="card shadow-sm border-0">
      <div class="card-body py-5 text-center" role="status">
        <div class="spinner-border spinner-border-sm me-2" aria-hidden="true"></div>
        載入訂單資料中...
      </div>
    </div>

    <div v-else-if="errorMessage" class="alert alert-danger shadow-sm" role="alert">
      <h2 class="h5 alert-heading">無法顯示競標訂單</h2>
      <p class="mb-3">{{ errorMessage }}</p>
      <div class="d-flex flex-wrap gap-2">
        <button type="button" class="btn btn-outline-danger" @click="loadOrder">重新載入</button>
        <RouterLink class="btn btn-outline-secondary" :to="{ name: 'auction-orders' }">
          返回訂單
        </RouterLink>
      </div>
    </div>

    <template v-else-if="order">
      <section class="card shadow-sm border-0 mb-4" aria-labelledby="product-heading">
        <div class="card-body p-4">
          <div class="row g-4 align-items-center">
            <div class="col-12 col-md-5 col-lg-4">
              <div class="product-image-frame">
                <img
                  v-if="productImageUrl"
                  :src="productImageUrl"
                  :alt="(order.productName || '競標商品') + ' 商品圖片'"
                  class="product-image"
                  @error="imageLoadFailed = true"
                />
                <div v-else class="product-image-placeholder" role="img" aria-label="目前沒有商品圖片">
                  <span class="placeholder-mark" aria-hidden="true">SM</span>
                  <span>目前沒有商品圖片</span>
                </div>
              </div>
            </div>

            <div class="col-12 col-md-7 col-lg-8">
              <p class="text-muted mb-2">{{ order.brandName || "品牌資料未提供" }}</p>
              <h2 id="product-heading" class="h3 mb-4">{{ order.productName || "未命名商品" }}</h2>
              <dl class="row order-facts mb-0">
                <dt class="col-sm-4">型號</dt>
                <dd class="col-sm-8">{{ order.model || "型號資料未提供" }}</dd>
                <dt class="col-sm-4">尺寸</dt>
                <dd class="col-sm-8">{{ order.size || "—" }}</dd>
                <dt class="col-sm-4">訂單編號</dt>
                <dd class="col-sm-8 text-break">{{ order.orderNumber || "—" }}</dd>
                <dt class="col-sm-4">競標編號</dt>
                <dd class="col-sm-8">{{ order.auctionId ?? "—" }}</dd>
                <dt class="col-sm-4">建立時間</dt>
                <dd class="col-sm-8 mb-0">{{ formatAuctionTime(order.createdAt) }}</dd>
              </dl>
            </div>
          </div>
        </div>
      </section>

      <div class="row g-4">
        <div class="col-12 col-lg-7">
          <section class="card shadow-sm border-0 h-100" aria-labelledby="shipping-heading">
            <div class="card-header bg-white px-4 py-3">
              <h2 id="shipping-heading" class="h5 mb-0">配送資訊</h2>
            </div>
            <div class="card-body p-4">
              <dl class="row detail-list mb-0">
                <dt class="col-sm-4">配送方式</dt>
                <dd class="col-sm-8">{{ displayValue(order.shippingMethod) }}</dd>
                <dt class="col-sm-4">物流狀態</dt>
                <dd class="col-sm-8">{{ formatAuctionOrderStatus(order.shippingStatus) }}</dd>
                <dt class="col-sm-4">收件人</dt>
                <dd class="col-sm-8">{{ displayValue(order.receiverName) }}</dd>
                <dt class="col-sm-4">電話</dt>
                <dd class="col-sm-8">{{ displayValue(order.receiverPhone) }}</dd>
                <dt class="col-sm-4">配送地址</dt>
                <dd class="col-sm-8 mb-0 text-break">{{ displayValue(order.shippingAddress) }}</dd>
              </dl>
            </div>
          </section>
        </div>

        <div class="col-12 col-lg-5">
          <section class="card shadow-sm border-0 h-100 payment-summary" aria-labelledby="amount-heading">
            <div class="card-header bg-white px-4 py-3">
              <h2 id="amount-heading" class="h5 mb-0">付款金額</h2>
            </div>
            <div class="card-body p-4">
              <div class="amount-row">
                <span>競標成交價</span>
                <span>{{ formatTwd(order.finalPrice) }}</span>
              </div>
              <div class="amount-row">
                <span>運費</span>
                <span>{{ formatTwd(order.shippingFee) }}</span>
              </div>
              <hr />
              <div class="amount-row total-row">
                <strong>應付總額</strong>
                <strong>{{ formatTwd(order.totalAmount) }}</strong>
              </div>
            </div>
          </section>
        </div>
      </div>

      <section class="card shadow-sm border-0 my-4" aria-labelledby="payment-heading">
        <div class="card-header bg-white px-4 py-3">
          <h2 id="payment-heading" class="h5 mb-0">付款資訊</h2>
        </div>
        <div class="card-body p-4">
          <div v-if="paymentSuccessMessage" class="alert alert-success" role="status" aria-live="polite">
            {{ paymentSuccessMessage }}
          </div>
          <div v-if="paymentErrorMessage" class="alert alert-danger" role="alert" aria-live="assertive">
            {{ paymentErrorMessage }}
          </div>

          <dl class="row detail-list mb-0">
            <dt class="col-sm-4 col-lg-3">付款方式</dt>
            <dd class="col-sm-8 col-lg-9">{{ displayValue(order.paymentMethod) }}</dd>
            <dt class="col-sm-4 col-lg-3">付款狀態</dt>
            <dd class="col-sm-8 col-lg-9">{{ paymentStatusText }}</dd>
            <dt class="col-sm-4 col-lg-3">訂單狀態</dt>
            <dd class="col-sm-8 col-lg-9">{{ formatAuctionOrderStatus(order.orderStatus) }}</dd>
            <dt class="col-sm-4 col-lg-3">付款期限</dt>
            <dd class="col-sm-8 col-lg-9">{{ formatAuctionTime(order.paymentDueAt) }}</dd>
            <template v-if="isPendingPayment && hasPaymentDeadline && !isPaymentDeadlineReached">
              <dt class="col-sm-4 col-lg-3">剩餘付款時間</dt>
              <dd class="col-sm-8 col-lg-9">{{ remainingPaymentTimeText }}</dd>
            </template>
            <template v-if="order.paidAt">
              <dt class="col-sm-4 col-lg-3">付款時間</dt>
              <dd class="col-sm-8 col-lg-9">{{ formatAuctionTime(order.paidAt) }}</dd>
            </template>
            <template v-if="order.abandonedAt">
              <dt class="col-sm-4 col-lg-3">棄標時間</dt>
              <dd class="col-sm-8 col-lg-9 mb-0">{{ formatAuctionTime(order.abandonedAt) }}</dd>
            </template>
          </dl>

          <div v-if="isAbandoned" class="alert alert-danger mt-4 mb-0" role="status">
            已棄標（逾期未付款），此訂單已無法再完成付款。
          </div>
          <div
            v-else-if="isPendingPayment && isPaymentDeadlineReached"
            class="alert alert-danger mt-4 mb-0"
            role="status"
          >
            付款期限已到，系統正在更新訂單狀態。
          </div>
          <div v-else-if="isPendingPayment" class="alert alert-warning mt-4 mb-0" role="status">
            此訂單尚待付款；請確認訂單資料後完成付款。
          </div>
          <div v-else-if="isPaid" class="alert alert-success mt-4 mb-0" role="status">
            此訂單已完成付款。
          </div>
          <div v-else-if="isFailed" class="alert alert-danger mt-4 mb-0" role="status">
            付款失敗，目前尚未開放重新付款。
          </div>
          <div v-else class="alert alert-secondary mt-4 mb-0" role="status">
            此訂單目前不提供付款操作。
          </div>
        </div>
      </section>

      <div class="d-flex flex-wrap justify-content-end gap-2">
        <RouterLink class="btn btn-outline-secondary" :to="{ name: 'auction-orders' }">
          返回我的競標訂單
        </RouterLink>
        <button
          v-if="isPendingPayment"
          type="button"
          class="btn btn-dark"
          :disabled="paying || !canPay"
          @click="confirmAndPay"
        >
          {{ paying ? "付款處理中..." : isPaymentDeadlineReached ? "付款期限已到" : "立即付款" }}
        </button>
      </div>
    </template>
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  getAuctionOrderDetail,
  handleAuctionAccessError,
  payAuctionOrder,
} from "@/service/auctionService";
import { formatTwd } from "@/utils/auctionMoney";
import { resolveAuctionImageUrl } from "@/utils/auctionImageUrl";
import { formatAuctionOrderStatus } from "@/utils/auctionMemberStatus";
import { formatAuctionTime, parseAuctionTime } from "@/utils/auctionTime";

const route = useRoute();
const router = useRouter();
const order = ref(null);
const loading = ref(true);
const paying = ref(false);
const errorMessage = ref("");
const paymentErrorMessage = ref("");
const paymentSuccessMessage = ref("");
const imageLoadFailed = ref(false);
const currentTimeMs = ref(Date.now());
let requestVersion = 0;
let viewVersion = 0;
let countdownTimer = null;
let deadlineRefreshOrderNumber = null;

const isPendingPayment = computed(
  () =>
    order.value?.paymentStatus === "PENDING" &&
    order.value?.orderStatus === "PENDING_PAYMENT",
);
const isPaid = computed(() => order.value?.paymentStatus === "PAID");
const isFailed = computed(() => order.value?.paymentStatus === "FAILED");
const isAbandoned = computed(() => order.value?.orderStatus === "ABANDONED");
const paymentDeadlineMs = computed(() => {
  if (!order.value?.paymentDueAt) return null;
  const timestamp = parseAuctionTime(order.value.paymentDueAt).getTime();
  return Number.isFinite(timestamp) ? timestamp : null;
});
const hasPaymentDeadline = computed(() => paymentDeadlineMs.value !== null);
const isPaymentDeadlineReached = computed(
  () =>
    hasPaymentDeadline.value &&
    currentTimeMs.value >= paymentDeadlineMs.value,
);
const canPay = computed(
  () => isPendingPayment.value && !isPaymentDeadlineReached.value,
);
const remainingPaymentTimeText = computed(() => {
  if (!hasPaymentDeadline.value) return "—";
  const remainingMs = Math.max(0, paymentDeadlineMs.value - currentTimeMs.value);
  const totalSeconds = Math.ceil(remainingMs / 1000);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  return [hours, minutes, seconds]
    .map((value) => String(value).padStart(2, "0"))
    .join(":");
});
const paymentStatusText = computed(() =>
  formatAuctionOrderStatus(order.value?.paymentStatus),
);
const headlineStatusText = computed(() =>
  isAbandoned.value ? "已棄標（逾期未付款）" : paymentStatusText.value,
);
const headlineStatusClass = computed(() => {
  if (isAbandoned.value) return "text-bg-danger";
  if (isPaid.value) return "text-bg-success";
  if (isFailed.value) return "text-bg-danger";
  if (isPendingPayment.value) return "text-bg-warning";
  return "text-bg-secondary";
});
const productImageUrl = computed(() =>
  imageLoadFailed.value
    ? ""
    : resolveAuctionImageUrl(order.value?.imageUrl),
);

function displayValue(value) {
  return value === null || value === undefined || value === "" ? "—" : value;
}

function detailErrorMessage(error) {
  if (error.response?.status === 403) return "你沒有權限查看此訂單";
  if (error.response?.status === 404) return "找不到此競標訂單";
  return "訂單資料載入失敗，請稍後再試";
}

async function loadOrder() {
  const activeViewVersion = ++viewVersion;
  const version = ++requestVersion;
  const orderNumber = String(route.params.orderNumber || "");
  deadlineRefreshOrderNumber = null;
  currentTimeMs.value = Date.now();
  loading.value = true;
  errorMessage.value = "";
  paymentErrorMessage.value = "";
  paymentSuccessMessage.value = "";
  order.value = null;
  imageLoadFailed.value = false;

  try {
    const response = await getAuctionOrderDetail(orderNumber);
    if (
      version !== requestVersion ||
      activeViewVersion !== viewVersion ||
      String(route.params.orderNumber) !== orderNumber
    ) {
      return;
    }
    order.value = response.data.data;
    currentTimeMs.value = Date.now();
    await refreshExpiredOrderOnce();
  } catch (error) {
    if (
      activeViewVersion !== viewVersion ||
      String(route.params.orderNumber) !== orderNumber
    ) {
      return;
    }
    if (
      error.response?.status === 401 &&
      (await handleAuctionAccessError(error, router))
    ) {
      return;
    }
    errorMessage.value = detailErrorMessage(error);
  } finally {
    if (
      activeViewVersion === viewVersion &&
      String(route.params.orderNumber) === orderNumber
    ) {
      loading.value = false;
    }
  }
}

async function refreshOrderDetail(orderNumber) {
  const version = ++requestVersion;
  const response = await getAuctionOrderDetail(orderNumber);
  if (
    version !== requestVersion ||
    String(route.params.orderNumber) !== String(orderNumber)
  ) {
    return false;
  }
  order.value = response.data.data;
  currentTimeMs.value = Date.now();
  imageLoadFailed.value = false;
  return true;
}

async function refreshExpiredOrderOnce() {
  if (!isPendingPayment.value || !isPaymentDeadlineReached.value) return false;

  const orderNumber = String(
    order.value?.orderNumber || route.params.orderNumber || "",
  );
  if (!orderNumber || deadlineRefreshOrderNumber === orderNumber) return false;
  deadlineRefreshOrderNumber = orderNumber;

  try {
    return await refreshOrderDetail(orderNumber);
  } catch (error) {
    if (error.response?.status === 401) {
      await handleAuctionAccessError(error, router);
    }
    return false;
  }
}

async function tickPaymentDeadline() {
  currentTimeMs.value = Date.now();
  if (isPaymentDeadlineReached.value) {
    await refreshExpiredOrderOnce();
  }
}

function startPaymentDeadlineTimer() {
  if (countdownTimer !== null) clearInterval(countdownTimer);
  currentTimeMs.value = Date.now();
  countdownTimer = setInterval(tickPaymentDeadline, 1000);
}

function stopPaymentDeadlineTimer() {
  if (countdownTimer === null) return;
  clearInterval(countdownTimer);
  countdownTimer = null;
}

function paymentRequestErrorMessage(error) {
  const status = error.response?.status;
  if (status === 403) return "你沒有權限付款此訂單";
  if (status === 404) return "找不到此競標訂單";
  if (status === 409) {
    if (error.response?.data?.message?.includes("付款期限")) {
      return "競標訂單付款期限已過";
    }
    return "目前訂單狀態無法付款，請重新整理後確認訂單狀態。";
  }
  return "付款處理失敗，請稍後再試。";
}

function isCurrentPaymentView(orderNumber, activeViewVersion) {
  return (
    activeViewVersion === viewVersion &&
    String(route.params.orderNumber) === String(orderNumber)
  );
}

async function reconcilePaymentState(orderNumber) {
  try {
    const refreshed = await refreshOrderDetail(orderNumber);
    if (refreshed && isPaid.value) {
      paymentErrorMessage.value = "";
      paymentSuccessMessage.value = "付款完成";
      return true;
    }
  } catch (error) {
    if (error.response?.status === 401) {
      await handleAuctionAccessError(error, router);
    }
  }
  return false;
}

async function confirmAndPay() {
  currentTimeMs.value = Date.now();
  if (paying.value) return;
  if (!canPay.value) {
    await refreshExpiredOrderOnce();
    return;
  }

  const orderNumber = String(order.value.orderNumber || route.params.orderNumber);
  const activeViewVersion = viewVersion;
  const confirmed = window.confirm(
    [
      "確認付款",
      "",
      `訂單：${orderNumber}`,
      `應付總額：${formatTwd(order.value.totalAmount)}`,
      `付款方式：${displayValue(order.value.paymentMethod)}`,
      "",
      "確認要完成此筆競標訂單付款嗎？",
    ].join("\n"),
  );
  if (!confirmed) return;

  currentTimeMs.value = Date.now();
  if (!isCurrentPaymentView(orderNumber, activeViewVersion)) return;
  if (!canPay.value) {
    paymentErrorMessage.value = "競標訂單付款期限已過";
    await refreshExpiredOrderOnce();
    return;
  }

  paying.value = true;
  paymentErrorMessage.value = "";
  paymentSuccessMessage.value = "";

  try {
    await payAuctionOrder(orderNumber);
    if (!isCurrentPaymentView(orderNumber, activeViewVersion)) return;
    try {
      const refreshed = await refreshOrderDetail(orderNumber);
      if (!refreshed) return;
      if (isPaid.value) {
        paymentSuccessMessage.value = "付款完成";
      } else {
        paymentErrorMessage.value =
          "付款已送出，但尚未取得最新付款狀態，請重新載入確認。";
      }
    } catch (refreshError) {
      if (
        refreshError.response?.status === 401 &&
        (await handleAuctionAccessError(refreshError, router))
      ) {
        return;
      }
      paymentErrorMessage.value =
        "付款已送出，但無法取得最新訂單狀態，請重新載入確認。";
    }
  } catch (error) {
    if (!isCurrentPaymentView(orderNumber, activeViewVersion)) return;
    if (
      error.response?.status === 401 &&
      (await handleAuctionAccessError(error, router))
    ) {
      return;
    }

    paymentErrorMessage.value = paymentRequestErrorMessage(error);
    const status = error.response?.status;
    if (status === 409 || status === undefined || status >= 500) {
      await reconcilePaymentState(orderNumber);
    }
  } finally {
    paying.value = false;
  }
}

watch(
  () => route.params.orderNumber,
  (orderNumber, previousOrderNumber) => {
    if (orderNumber !== previousOrderNumber) {
      currentTimeMs.value = Date.now();
      loadOrder();
    }
  },
);

onMounted(() => {
  startPaymentDeadlineTimer();
  return loadOrder();
});
onUnmounted(() => {
  stopPaymentDeadlineTimer();
  viewVersion++;
  requestVersion++;
});
</script>

<style scoped>
.auction-order-payment {
  max-width: 1060px;
  margin: 0 auto;
}

.product-image-frame {
  aspect-ratio: 4 / 3;
  overflow: hidden;
  border: 1px solid #dee2e6;
  border-radius: 0.75rem;
  background: #f1f3f5;
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-image-placeholder {
  width: 100%;
  height: 100%;
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 0.75rem;
  color: #6c757d;
}

.placeholder-mark {
  display: grid;
  place-items: center;
  width: 3.25rem;
  height: 3.25rem;
  border: 1px solid #adb5bd;
  border-radius: 50%;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.order-facts dt,
.detail-list dt {
  color: #6c757d;
  font-weight: 500;
}

.order-facts dd,
.detail-list dd {
  font-weight: 500;
}

.amount-row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}

.total-row {
  margin-bottom: 0;
  color: #111;
  font-size: 1.15rem;
}

.payment-summary {
  border-top: 3px solid #212529 !important;
}

@media (max-width: 575.98px) {
  .order-facts dd:not(:last-child),
  .detail-list dd:not(:last-child) {
    margin-bottom: 1rem;
  }
}
</style>
