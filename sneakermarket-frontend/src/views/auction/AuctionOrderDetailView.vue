<template>
  <main class="auction-order-detail">
    <header class="detail-header">
      <div>
        <p class="eyebrow">{{ isSeller ? "Auction sale" : "Auction order" }}</p>
        <h1>{{ pageTitle }}</h1>
        <p v-if="order" class="order-reference">
          訂單 {{ order.orderNumber }} · {{ formatAuctionTime(order.createdAt) }}
        </p>
      </div>
      <span
        v-if="order"
        class="status-badge"
        :class="`status-badge--${statusTone}`"
      >
        {{ statusText }}
      </span>
    </header>

    <p v-if="loading" class="page-state" role="status">載入訂單資料中…</p>

    <section v-else-if="errorMessage" class="page-state page-state--error" role="alert">
      <h2>無法顯示競標訂單</h2>
      <p>{{ errorMessage }}</p>
      <div class="state-actions">
        <button type="button" class="secondary-button" @click="loadOrder">重新載入</button>
        <RouterLink class="secondary-button" :to="{ name: 'auction-orders' }">
          返回訂單
        </RouterLink>
      </div>
    </section>

    <template v-else-if="order">
      <section class="detail-panel product-panel" aria-labelledby="product-heading">
        <div class="product-image-wrap">
          <img
            v-if="productImageUrl"
            :src="productImageUrl"
            :alt="`${order.productName || '競標商品'} 商品圖片`"
            class="product-image"
            @error="imageLoadFailed = true"
          />
          <div v-else class="product-image product-image--fallback" role="img" aria-label="目前沒有商品圖片">
            暫無圖片
          </div>
        </div>

        <div class="product-copy">
          <p>{{ order.brandName || "品牌資料未提供" }}</p>
          <h2 id="product-heading">{{ order.productName || "未命名商品" }}</h2>
          <p>{{ productMeta }}</p>
          <div class="product-price">
            <span>成交價格</span>
            <strong>{{ formatTwd(order.finalPrice) }}</strong>
          </div>
        </div>
      </section>

      <div class="detail-grid">
        <section class="detail-panel" aria-labelledby="amount-heading">
          <h2 id="amount-heading">金額明細</h2>
          <dl class="fact-list amount-list">
            <div><dt>成交價格</dt><dd>{{ formatTwd(order.finalPrice) }}</dd></div>
            <div><dt>運費</dt><dd>{{ formatTwd(order.shippingFee) }}</dd></div>
            <div class="total-row"><dt>訂單總額</dt><dd>{{ formatTwd(order.totalAmount) }}</dd></div>
          </dl>
        </section>

        <section class="detail-panel" aria-labelledby="payment-heading">
          <h2 id="payment-heading">付款資訊</h2>
          <dl class="fact-list">
            <div><dt>付款方式</dt><dd>{{ paymentMethodLabel }}</dd></div>
            <div><dt>付款狀態</dt><dd>{{ statusText }}</dd></div>
            <div><dt>付款期限</dt><dd>{{ formatAuctionTime(order.paymentDueAt) }}</dd></div>
            <div v-if="order.paidAt"><dt>付款時間</dt><dd>{{ formatAuctionTime(order.paidAt) }}</dd></div>
            <div v-if="order.abandonedAt"><dt>棄標時間</dt><dd>{{ formatAuctionTime(order.abandonedAt) }}</dd></div>
          </dl>
        </section>
      </div>

      <section class="detail-panel" aria-labelledby="receiver-heading">
        <div class="section-heading">
          <h2 id="receiver-heading">收件資訊</h2>
          <span>{{ displayValue(order.shippingMethod) }}</span>
        </div>

        <dl v-if="receiverInfoVisible" class="fact-list receiver-list">
          <div><dt>收件人</dt><dd>{{ displayValue(order.receiverName) }}</dd></div>
          <div><dt>電話</dt><dd>{{ displayValue(order.receiverPhone) }}</dd></div>
          <div><dt>配送地址</dt><dd>{{ displayValue(order.shippingAddress) }}</dd></div>
          <div><dt>配送方式</dt><dd>{{ displayValue(order.shippingMethod) }}</dd></div>
        </dl>

        <div v-else class="privacy-notice" role="status">
          {{ receiverPrivacyMessage }}
        </div>
      </section>

      <div v-if="buyerAbandoned" class="order-notice order-notice--danger" role="status">
        此訂單已因逾期未付款而棄標，無法再次付款。
      </div>

      <footer class="detail-actions">
        <RouterLink class="primary-link" :to="{ name: 'auction-orders' }">
          返回競標訂單
        </RouterLink>
      </footer>
    </template>
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";

import { AUCTION_PAYMENT_METHOD } from "@/constants/auctionPayment";
import {
  getAuctionOrderDetail,
  handleAuctionAccessError,
} from "@/service/auctionService";
import { resolveAuctionImageUrl } from "@/utils/auctionImageUrl";
import {
  formatBuyerAuctionOrderStatus,
  formatSellerAuctionOrderStatus,
  getBuyerAuctionOrderStatusTone,
  getSellerAuctionOrderStatusTone,
} from "@/utils/auctionMemberStatus";
import { formatTwd } from "@/utils/auctionMoney";
import { formatAuctionTime } from "@/utils/auctionTime";

const route = useRoute();
const router = useRouter();
const order = ref(null);
const loading = ref(true);
const errorMessage = ref("");
const imageLoadFailed = ref(false);
let requestVersion = 0;
let viewVersion = 0;

const isBuyer = computed(() => order.value?.viewerRole === "BUYER");
const isSeller = computed(() => order.value?.viewerRole === "SELLER");
const pageTitle = computed(() => (isSeller.value ? "賣出訂單明細" : "競標訂單明細"));
const receiverInfoVisible = computed(() => order.value?.receiverInfoVisible === true);
const statusText = computed(() =>
  isSeller.value
    ? formatSellerAuctionOrderStatus(order.value)
    : formatBuyerAuctionOrderStatus(order.value),
);
const statusTone = computed(() =>
  isSeller.value
    ? getSellerAuctionOrderStatusTone(order.value)
    : getBuyerAuctionOrderStatusTone(order.value),
);
const productImageUrl = computed(() =>
  imageLoadFailed.value ? "" : resolveAuctionImageUrl(order.value?.imageUrl),
);
const productMeta = computed(
  () => [order.value?.model, order.value?.size].filter(Boolean).join(" · ") || "商品規格未提供",
);
const paymentMethodLabel = computed(() => {
  if (order.value?.paymentMethod === AUCTION_PAYMENT_METHOD.TRANSFER) return "銀行轉帳";
  return order.value?.paymentMethod || "—";
});
const buyerAbandoned = computed(
  () => isBuyer.value && order.value?.orderStatus === "ABANDONED",
);
const sellerPending = computed(
  () => isSeller.value && order.value?.orderStatus === "PENDING_PAYMENT",
);
const sellerAbandoned = computed(
  () => isSeller.value && order.value?.orderStatus === "ABANDONED",
);
const receiverPrivacyMessage = computed(() => {
  if (sellerAbandoned.value) return "訂單已棄標，無需進行出貨處理。";
  if (sellerPending.value) return "買家完成付款後將顯示收件資訊。";
  if (isSeller.value) return "目前不提供收件資訊。";
  return "收件資訊目前無法顯示。";
});

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
  loading.value = true;
  errorMessage.value = "";
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
  } catch (error) {
    if (activeViewVersion !== viewVersion || String(route.params.orderNumber) !== orderNumber) return;
    if (
      (error.response?.status === 401 || error.response?.status === 403) &&
      (await handleAuctionAccessError(error, router))
    ) {
      return;
    }
    errorMessage.value = detailErrorMessage(error);
  } finally {
    if (activeViewVersion === viewVersion && String(route.params.orderNumber) === orderNumber) {
      loading.value = false;
    }
  }
}

watch(
  () => route.params.orderNumber,
  (orderNumber, previousOrderNumber) => {
    if (orderNumber !== previousOrderNumber) loadOrder();
  },
);

onMounted(() => loadOrder());
onUnmounted(() => {
  viewVersion++;
  requestVersion++;
});
</script>

<style scoped>
.auction-order-detail { max-width: 1000px; margin: 0 auto; color: #111; }
.detail-header { display: flex; align-items: start; justify-content: space-between; gap: 24px; margin-bottom: 26px; }
.eyebrow { margin: 0 0 6px; color: #6c757d; font-size: .75rem; font-weight: 700; letter-spacing: .12em; text-transform: uppercase; }
.detail-header h1 { margin: 0; font-size: clamp(1.8rem, 4vw, 2.35rem); }
.order-reference { margin: 9px 0 0; color: #6c757d; font-size: .88rem; overflow-wrap: anywhere; }
.status-badge { flex: 0 0 auto; padding: 6px 11px; border-radius: 999px; font-size: .8rem; font-weight: 700; }
.status-badge--neutral { background: #e9ecef; color: #343a40; }
.status-badge--positive { background: #e7f4ec; color: #24613c; }
.status-badge--warning { background: #fff3cd; color: #765a00; }
.status-badge--danger { background: #fbe9e7; color: #a12b21; }
.page-state, .detail-panel { border: 1px solid #e9ecef; border-radius: 10px; background: #fff; }
.page-state { padding: 38px; color: #6c757d; text-align: center; }
.page-state h2 { margin: 0 0 8px; color: #111; font-size: 1.2rem; }
.page-state--error { color: #842029; }
.state-actions { display: flex; justify-content: center; gap: 10px; margin-top: 18px; }
.product-panel { display: grid; grid-template-columns: 190px minmax(0, 1fr); align-items: center; gap: 28px; padding: 26px; }
.product-image-wrap { width: 190px; aspect-ratio: 4 / 3; overflow: hidden; border-radius: 8px; background: #f1f3f5; }
.product-image { width: 100%; height: 100%; object-fit: cover; }
.product-image--fallback { display: grid; place-items: center; color: #868e96; font-size: .8rem; }
.product-copy p { margin: 0; color: #6c757d; font-size: .88rem; }
.product-copy h2 { margin: 5px 0 8px; font-size: 1.35rem; }
.product-price { display: flex; align-items: baseline; gap: 14px; margin-top: 22px; }
.product-price span { color: #6c757d; font-size: .8rem; }
.product-price strong { font-size: 1.15rem; }
.detail-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; margin: 18px 0; }
.detail-panel { min-width: 0; padding: 24px; }
.detail-panel h2 { margin: 0 0 18px; font-size: 1.08rem; }
.fact-list { display: grid; gap: 13px; margin: 0; }
.fact-list div { display: flex; justify-content: space-between; gap: 22px; min-width: 0; }
.fact-list dt { color: #6c757d; font-weight: 500; }
.fact-list dd { min-width: 0; margin: 0; font-weight: 600; text-align: right; overflow-wrap: anywhere; }
.amount-list .total-row { margin-top: 2px; padding-top: 14px; border-top: 1px solid #e9ecef; color: #111; font-size: 1.05rem; }
.section-heading { display: flex; align-items: center; justify-content: space-between; gap: 18px; }
.section-heading span { color: #6c757d; font-size: .82rem; }
.receiver-list { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.receiver-list div:nth-child(3) { grid-column: 1 / -1; }
.privacy-notice, .order-notice { padding: 15px; border-radius: 8px; background: #f8f9fa; color: #6c757d; line-height: 1.55; }
.order-notice { margin-top: 18px; }
.order-notice--danger { background: #fbe9e7; color: #a12b21; }
.detail-actions { display: flex; justify-content: flex-end; margin-top: 24px; }
.primary-link, .secondary-button { display: inline-flex; align-items: center; justify-content: center; min-height: 42px; padding: 10px 17px; border: 1px solid #111; border-radius: 6px; font-weight: 700; text-decoration: none; }
.primary-link { background: #111; color: #fff; }
.secondary-button { background: #fff; color: #111; }

@media (max-width: 767.98px) {
  .product-panel { grid-template-columns: 130px minmax(0, 1fr); gap: 20px; }
  .product-image-wrap { width: 130px; }
  .detail-grid { grid-template-columns: 1fr; }
  .receiver-list { grid-template-columns: 1fr; }
  .receiver-list div:nth-child(3) { grid-column: auto; }
}

@media (max-width: 575.98px) {
  .detail-header { flex-direction: column; }
  .product-panel { grid-template-columns: 92px minmax(0, 1fr); gap: 16px; }
  .product-image-wrap { width: 92px; }
  .detail-panel, .page-state { padding: 20px; }
  .product-price { align-items: start; flex-direction: column; gap: 4px; }
  .fact-list div { align-items: start; flex-direction: column; gap: 3px; }
  .fact-list dd { text-align: left; }
  .state-actions, .detail-actions { align-items: stretch; flex-direction: column; }
  .primary-link, .secondary-button { width: 100%; }
}
</style>
