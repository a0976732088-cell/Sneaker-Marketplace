<template>
  <AuctionMemberPage
    :key="activeTab"
    title="我的競標訂單"
    :empty-message="activeEmptyMessage"
    :error-message-fallback="activeErrorMessage"
    :fetch-page="fetchActiveOrders"
  >
    <template #view-switch>
      <div class="btn-group mb-4" role="tablist" aria-label="競標訂單角色切換">
        <button
          class="btn"
          :class="isBuyerOrders ? 'btn-primary' : 'btn-outline-primary'"
          type="button"
          role="tab"
          :aria-selected="isBuyerOrders"
          @click="selectTab('buyer')"
        >
          我買到的
        </button>
        <button
          class="btn"
          :class="isSellerOrders ? 'btn-primary' : 'btn-outline-primary'"
          type="button"
          role="tab"
          :aria-selected="isSellerOrders"
          @click="selectTab('seller')"
        >
          我賣出的
        </button>
      </div>
    </template>

    <template #default="{ items }">
      <div v-if="isBuyerOrders" class="row g-3">
        <div v-for="order in items" :key="order.orderNumber" class="col-12 col-lg-6">
          <article class="card h-100">
            <div class="card-body">
              <h2 class="h5">{{ order.productName }}</h2>
              <p class="text-muted text-break">訂單編號：{{ order.orderNumber }}</p>
              <dl class="row mb-0">
                <dt class="col-5">成交價格</dt><dd class="col-7">{{ formatTwd(order.finalPrice) }}</dd>
                <dt class="col-5">付款方式</dt><dd class="col-7">{{ order.paymentMethod }}</dd>
                <dt class="col-5">付款狀態</dt><dd class="col-7">{{ formatAuctionOrderStatus(order.paymentStatus) }}</dd>
                <dt class="col-5">物流狀態</dt><dd class="col-7">{{ formatAuctionOrderStatus(order.shippingStatus) }}</dd>
                <dt class="col-5">訂單狀態</dt><dd class="col-7">{{ formatAuctionOrderStatus(order.orderStatus) }}</dd>
                <dt class="col-5">建立時間</dt><dd class="col-7">{{ formatAuctionTime(order.createdAt) }}</dd>
              </dl>
              <div class="d-flex flex-wrap gap-2 mt-3">
                <RouterLink class="btn btn-outline-primary" :to="{ name: 'auction-detail', params: { id: order.auctionId } }">
                  查看競標
                </RouterLink>
                <RouterLink
                  v-if="order.paymentStatus === 'PENDING' && order.orderStatus === 'PENDING_PAYMENT'"
                  class="btn btn-primary"
                  :to="{ name: 'auction-order-payment', params: { orderNumber: order.orderNumber } }"
                >
                  前往付款
                </RouterLink>
                <RouterLink
                  v-else
                  class="btn btn-outline-secondary"
                  :to="{ name: 'auction-order-payment', params: { orderNumber: order.orderNumber } }"
                >
                  查看訂單
                </RouterLink>
              </div>
            </div>
          </article>
        </div>
      </div>

      <div v-else class="row g-3">
        <div v-for="order in items" :key="order.orderNumber" class="col-12 col-lg-6">
          <article class="card h-100">
            <div class="card-body">
              <div class="d-flex flex-wrap justify-content-between gap-2">
                <h2 class="h5">{{ order.productName }}</h2>
                <span class="badge align-self-start" :class="sellerPaymentBadgeClass(order)">
                  {{ sellerPaymentText(order) }}
                </span>
              </div>
              <p class="text-muted text-break">訂單編號：{{ order.orderNumber }}</p>
              <dl class="row mb-0">
                <dt class="col-5">競標編號</dt><dd class="col-7">{{ order.auctionId }}</dd>
                <dt class="col-5">成交價格</dt><dd class="col-7">{{ formatTwd(order.finalPrice) }}</dd>
                <dt class="col-5">付款方式</dt><dd class="col-7">{{ order.paymentMethod }}</dd>
                <dt class="col-5">付款狀態</dt><dd class="col-7">{{ sellerPaymentText(order) }}</dd>
                <dt class="col-5">訂單狀態</dt><dd class="col-7">{{ formatAuctionOrderStatus(order.orderStatus) }}</dd>
                <dt class="col-5">建立時間</dt><dd class="col-7">{{ formatAuctionTime(order.createdAt) }}</dd>
              </dl>
            </div>
          </article>
        </div>
      </div>
    </template>
  </AuctionMemberPage>
</template>

<script setup>
import { computed, ref } from "vue";
import AuctionMemberPage from "@/components/auction/AuctionMemberPage.vue";
import { getMyAuctionOrders, getMyAuctionSales } from "@/service/auctionService";
import { formatTwd } from "@/utils/auctionMoney";
import { formatAuctionTime } from "@/utils/auctionTime";
import { formatAuctionOrderStatus } from "@/utils/auctionMemberStatus";

const activeTab = ref("buyer");
const isBuyerOrders = computed(() => activeTab.value === "buyer");
const isSellerOrders = computed(() => activeTab.value === "seller");
const activeEmptyMessage = computed(() =>
  isSellerOrders.value ? "目前沒有賣出的競標訂單" : "目前沒有競標訂單",
);
const activeErrorMessage = computed(() =>
  isSellerOrders.value
    ? "賣出訂單載入失敗，請稍後再試。"
    : "無法載入資料，請稍後重試",
);

function selectTab(tab) {
  if (tab !== "buyer" && tab !== "seller") return;
  activeTab.value = tab;
}

function fetchActiveOrders(params) {
  return isSellerOrders.value ? getMyAuctionSales(params) : getMyAuctionOrders(params);
}

function sellerPaymentText(order) {
  if (order.paymentStatus === "PAID") return "買家已付款";
  if (order.paymentStatus === "FAILED") return "付款失敗";
  if (order.paymentStatus === "PENDING" && order.orderStatus === "PENDING_PAYMENT") {
    return "等待買家付款";
  }
  if (order.paymentStatus === "PENDING") return "未付款";
  return formatAuctionOrderStatus(order.paymentStatus);
}

function sellerPaymentBadgeClass(order) {
  if (order.paymentStatus === "PAID") return "text-bg-success";
  if (order.paymentStatus === "FAILED" || order.orderStatus === "CANCELLED") {
    return "text-bg-danger";
  }
  if (order.paymentStatus === "PENDING" && order.orderStatus === "PENDING_PAYMENT") {
    return "text-bg-warning";
  }
  return "text-bg-secondary";
}
</script>
