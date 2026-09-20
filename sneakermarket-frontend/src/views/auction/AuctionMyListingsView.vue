<template>
  <AuctionMemberPage
    title="刊登紀錄"
    empty-title="尚無刊登紀錄"
    empty-message="建立你的第一筆競標商品"
    :fetch-page="getMyListings"
  >
    <template #empty-action>
      <RouterLink class="empty-action" :to="{ name: 'auction-create' }">建立競標商品</RouterLink>
    </template>

    <template #default="{ items }">
      <div class="auction-record-list">
        <AuctionRecordItem
          v-for="listing in items"
          :key="listing.auctionId"
          :image-url="listing.imageUrl"
          :image-alt="listing.productName"
          :status="formatMyListingStatus(listing)"
          :status-tone="getMyListingStatusTone(listing)"
        >
          <template #header>
            <p class="record-reference">
              競標 #{{ listing.auctionId }} · 刊登於 {{ formatAuctionTime(listing.createdAt) }}
            </p>
          </template>

          <template #product>
            <h2 class="record-product-title">{{ listing.productName }}</h2>
            <p class="record-product-meta">{{ formatProductMeta(listing) }}</p>
          </template>

          <div class="record-metrics">
            <div class="record-metric">
              <span class="record-metric__label">
                {{ listing.finalPrice != null ? "結標價格" : "目前價格" }}
              </span>
              <strong class="record-metric__value">
                {{ formatTwd(listing.finalPrice ?? listing.currentPrice) }}
              </strong>
            </div>
            <div class="record-metric">
              <span class="record-metric__label">出價</span>
              <strong class="record-metric__value">{{ listing.bidCount ?? 0 }} 次</strong>
            </div>
          </div>

          <p class="record-supporting-text">
            <template v-if="listing.status === 'SCHEDULED'">
              {{ formatAuctionTime(listing.startAt) }} 開始
            </template>
            <template v-else>{{ formatAuctionTime(listing.endAt) }} 結束</template>
          </p>

          <template #actions>
            <RouterLink
              v-if="listing.managementState === 'VISIBLE'"
              class="record-action"
              :to="{ name: 'auction-detail', params: { id: listing.auctionId } }"
            >
              查看競標 →
            </RouterLink>
            <p v-else class="record-action-notice">
              {{ formatMemberPublicDetailNotice(listing.managementState) }}
            </p>
          </template>
        </AuctionRecordItem>
      </div>
    </template>
  </AuctionMemberPage>
</template>

<script setup>
import AuctionMemberPage from "@/components/auction/AuctionMemberPage.vue";
import AuctionRecordItem from "@/components/auction/AuctionRecordItem.vue";
import { getMyListings } from "@/service/auctionService";
import {
  formatMemberPublicDetailNotice,
  formatMyListingStatus,
  getMyListingStatusTone,
} from "@/utils/auctionMemberStatus";
import { formatTwd } from "@/utils/auctionMoney";
import { formatAuctionTime } from "@/utils/auctionTime";

function formatProductMeta(item) {
  return [item.model, item.size].filter(Boolean).join(" · ") || "商品規格未提供";
}
</script>
