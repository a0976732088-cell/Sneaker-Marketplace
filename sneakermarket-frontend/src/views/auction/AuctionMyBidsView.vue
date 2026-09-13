<template>
  <AuctionMemberPage title="我的出價" empty-message="目前沒有出價紀錄" :fetch-page="getMyBids">
    <template #default="{ items }">
      <div class="row g-3">
        <div v-for="bid in items" :key="bid.auctionId" class="col-12 col-lg-6">
          <article class="card h-100">
            <div class="card-body">
              <h2 class="h5">{{ bid.productName }}</h2>
              <p class="text-muted">{{ bid.model }} · {{ bid.size }}</p>
              <p><span class="badge text-bg-primary">{{ formatMyBidOutcome(bid) }}</span></p>
              <dl class="row mb-0">
                <dt class="col-5">我的最高出價</dt><dd class="col-7">{{ formatTwd(bid.myHighestBid) }}</dd>
                <dt class="col-5">目前價格</dt><dd class="col-7">{{ formatTwd(bid.currentPrice) }}</dd>
                <dt class="col-5">出價次數</dt><dd class="col-7">{{ bid.bidCount }}</dd>
                <dt class="col-5">競標狀態</dt><dd class="col-7">{{ formatMemberAuctionStatus(bid.status) }}</dd>
                <dt class="col-5">管理狀態</dt><dd class="col-7">{{ formatMemberManagementState(bid.managementState) }}</dd>
                <dt class="col-5">結標結果</dt><dd class="col-7">{{ formatMemberAuctionStatus(bid.resultStatus) }}</dd>
                <dt v-if="bid.finalPrice != null" class="col-5">成交價格</dt><dd v-if="bid.finalPrice != null" class="col-7">{{ formatTwd(bid.finalPrice) }}</dd>
                <dt class="col-5">最近出價</dt><dd class="col-7">{{ formatAuctionTime(bid.lastBidAt) }}</dd>
                <dt class="col-5">結束時間</dt><dd class="col-7">{{ formatAuctionTime(bid.endAt) }}</dd>
              </dl>
              <RouterLink
                v-if="bid.managementState === 'VISIBLE'"
                class="btn btn-outline-primary"
                :to="{ name: 'auction-detail', params: { id: bid.auctionId } }"
              >查看競標</RouterLink>
              <p v-else class="alert alert-secondary py-2 mb-0 small">
                {{ formatMemberPublicDetailNotice(bid.managementState) }}
              </p>
            </div>
          </article>
        </div>
      </div>
    </template>
  </AuctionMemberPage>
</template>

<script setup>
import AuctionMemberPage from "@/components/auction/AuctionMemberPage.vue";
import { getMyBids } from "@/service/auctionService";
import { formatTwd } from "@/utils/auctionMoney";
import { formatAuctionTime } from "@/utils/auctionTime";
import { formatMemberAuctionStatus, formatMemberManagementState, formatMemberPublicDetailNotice, formatMyBidOutcome } from "@/utils/auctionMemberStatus";
</script>
