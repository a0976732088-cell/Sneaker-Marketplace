<template>
  <AuctionMemberPage
    :key="activeTab"
    title="競標訂單"
    :empty-title="activeEmptyTitle"
    :empty-message="activeEmptyMessage"
    :error-message-fallback="activeErrorMessage"
    :fetch-page="fetchActiveOrders"
  >
    <template #view-switch>
      <div class="order-role-tabs" role="tablist" aria-label="競標訂單角色切換">
        <button
          type="button"
          role="tab"
          class="order-role-tab"
          :class="{ 'is-active': isBuyerOrders }"
          :aria-selected="isBuyerOrders"
          @click="selectTab('buyer')"
        >
          我買到的
        </button>
        <button
          type="button"
          role="tab"
          class="order-role-tab"
          :class="{ 'is-active': isSellerOrders }"
          :aria-selected="isSellerOrders"
          @click="selectTab('seller')"
        >
          我賣出的
        </button>
      </div>
    </template>

    <template #default="{ items }">
      <div v-if="isBuyerOrders" class="auction-record-list">
        <AuctionRecordItem
          v-for="order in items"
          :key="order.orderNumber"
          :image-url="order.imageUrl"
          :image-alt="order.productName"
          :status="formatBuyerAuctionOrderStatus(order)"
          :status-tone="getBuyerAuctionOrderStatusTone(order)"
        >
          <template #header>
            <p class="record-reference">
              訂單 {{ order.orderNumber }} · {{ formatAuctionTime(order.createdAt) }}
            </p>
          </template>

          <template #product>
            <h2 class="record-product-title">{{ order.productName }}</h2>
            <p class="record-product-meta">{{ formatProductMeta(order) }}</p>
          </template>

          <div class="record-metrics">
            <div class="record-metric">
              <span class="record-metric__label">成交金額</span>
              <strong class="record-metric__value">{{ formatTwd(order.finalPrice) }}</strong>
            </div>
          </div>

          <p
            v-if="canPayAuctionOrder(order) && order.paymentDueAt"
            class="record-supporting-text payment-deadline"
          >
            請於 {{ formatAuctionTime(order.paymentDueAt) }} 前完成付款
          </p>

          <template #actions>
            <RouterLink
              v-if="canViewPublicAuction(order)"
              class="record-action"
              :to="{ name: 'auction-detail', params: { id: order.auctionId } }"
            >
              查看競標
            </RouterLink>
            <p v-else class="record-action-notice">
              {{ formatMemberPublicDetailNotice(order.managementState) }}
            </p>
            <RouterLink
              v-if="canPayAuctionOrder(order)"
              class="record-action record-action--primary"
              :to="{
                name: 'auction-order-checkout',
                params: { orderNumber: order.orderNumber },
              }"
            >
              立即付款
            </RouterLink>
            <RouterLink
              v-else
              class="record-action"
              :to="{
                name: 'auction-order-detail',
                params: { orderNumber: order.orderNumber },
              }"
            >
              查看訂單
            </RouterLink>
          </template>
        </AuctionRecordItem>
      </div>

      <div v-else class="auction-record-list">
        <AuctionRecordItem
          v-for="order in items"
          :key="order.orderNumber"
          :image-url="order.imageUrl"
          :image-alt="order.productName"
          :status="formatSellerAuctionOrderStatus(order)"
          :status-tone="getSellerAuctionOrderStatusTone(order)"
        >
          <template #header>
            <p class="record-reference">
              訂單 {{ order.orderNumber }} · {{ formatAuctionTime(order.createdAt) }}
            </p>
          </template>

          <template #product>
            <h2 class="record-product-title">{{ order.productName }}</h2>
            <p class="record-product-meta">{{ formatProductMeta(order) }}</p>
          </template>

          <div class="record-metrics">
            <div class="record-metric">
              <span class="record-metric__label">成交金額</span>
              <strong class="record-metric__value">{{ formatTwd(order.finalPrice) }}</strong>
            </div>
          </div>

          <template #actions>
            <RouterLink
              class="record-action"
              :to="{
                name: 'auction-order-detail',
                params: { orderNumber: order.orderNumber },
              }"
            >
              查看訂單
            </RouterLink>
          </template>
        </AuctionRecordItem>
      </div>
    </template>
  </AuctionMemberPage>
</template>

<script setup>
import { computed, ref } from "vue";

import AuctionMemberPage from "@/components/auction/AuctionMemberPage.vue";
import AuctionRecordItem from "@/components/auction/AuctionRecordItem.vue";
import { getMyAuctionOrders, getMyAuctionSales } from "@/service/auctionService";
import { canPayAuctionOrder, formatBuyerAuctionOrderStatus, formatMemberPublicDetailNotice, formatSellerAuctionOrderStatus, getBuyerAuctionOrderStatusTone, getSellerAuctionOrderStatusTone } from "@/utils/auctionMemberStatus";
import { formatTwd } from "@/utils/auctionMoney";
import { formatAuctionTime } from "@/utils/auctionTime";

const activeTab = ref("buyer");
const isBuyerOrders = computed(() => activeTab.value === "buyer");
const isSellerOrders = computed(() => activeTab.value === "seller");
const activeEmptyTitle = computed(() =>
  isSellerOrders.value ? "目前沒有賣出的競標訂單" : "目前沒有競標訂單",
);
const activeEmptyMessage = computed(() =>
  isSellerOrders.value
    ? "競標商品成交後，賣出訂單會顯示在這裡"
    : "得標後的訂單會顯示在這裡",
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

function canViewPublicAuction(order) {
  return order.managementState === "VISIBLE";
}

function formatProductMeta(item) {
  return [item.model, item.size].filter(Boolean).join(" · ") || "商品規格未提供";
}
</script>

<style scoped>
.order-role-tabs {
  display: flex;
  gap: 26px;
  margin: -4px 0 22px;
}

.order-role-tab {
  padding: 0 0 7px;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: #6c757d;
  font-size: 0.88rem;
  font-weight: 650;
  cursor: pointer;
}

.order-role-tab:hover,
.order-role-tab:focus-visible,
.order-role-tab.is-active {
  border-bottom-color: #111;
  color: #111;
}

.payment-deadline {
  color: #8a5b00 !important;
  font-weight: 600;
}

@media (max-width: 576px) {
  .order-role-tabs {
    gap: 22px;
  }
}
</style>
