<template>
  <AuctionMemberPage
    title="出價紀錄"
    empty-title="尚無出價紀錄"
    empty-message="去競標商城看看目前正在競標的商品"
    :fetch-page="getMyBids"
  >
    <template #empty-action>
      <RouterLink class="empty-action" :to="{ name: 'auction' }">前往競標商城</RouterLink>
    </template>

    <template #default="{ items }">
      <div class="auction-record-list">
        <AuctionRecordItem
          v-for="bid in items"
          :key="bid.auctionId"
          :image-url="bid.imageUrl"
          :image-alt="bid.productName"
          :status="formatMyBidOutcome(bid)"
          :status-tone="getMyBidStatusTone(bid)"
        >
          <template #header>
            <p class="record-reference">
              競標 #{{ bid.auctionId }} · 最近出價 {{ formatAuctionTime(bid.lastBidAt) }}
            </p>
          </template>

          <template #product>
            <h2 class="record-product-title">{{ bid.productName }}</h2>
            <p class="record-product-meta">{{ formatProductMeta(bid) }}</p>
          </template>

          <div class="record-metrics">
            <div class="record-metric">
              <span class="record-metric__label">我的最高出價</span>
              <strong class="record-metric__value">{{ formatTwd(bid.myHighestBid) }}</strong>
            </div>
            <div class="record-metric">
              <span class="record-metric__label">
                {{ bid.finalPrice != null ? "結標價格" : "目前價格" }}
              </span>
              <strong class="record-metric__value">
                {{ formatTwd(bid.finalPrice ?? bid.currentPrice) }}
              </strong>
            </div>
          </div>

          <p class="record-supporting-text">
            {{ bid.bidCount ?? 0 }} 次出價 · {{ formatAuctionTime(bid.endAt) }} 結束
          </p>

          <template #actions>
            <RouterLink
              v-if="bid.managementState === 'VISIBLE'"
              class="record-action"
              :to="{ name: 'auction-detail', params: { id: bid.auctionId } }"
            >
              查看競標 →
            </RouterLink>
            <p
              v-else
              class="record-action-notice record-action-notice--single-line"
            >
              {{ formatMemberPublicDetailNotice(bid.managementState) }}
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
import { getMyBids } from "@/service/auctionService";
import {
  formatMemberPublicDetailNotice,
  formatMyBidOutcome,
  getMyBidStatusTone,
} from "@/utils/auctionMemberStatus";
import { formatTwd } from "@/utils/auctionMoney";
import { formatAuctionTime } from "@/utils/auctionTime";

function formatProductMeta(item) {
  return [item.model, item.size].filter(Boolean).join(" · ") || "商品規格未提供";
}
</script>
