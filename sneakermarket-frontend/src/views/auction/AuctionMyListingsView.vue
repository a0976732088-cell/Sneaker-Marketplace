<template>
  <AuctionMemberPage title="我的刊登" empty-message="目前沒有刊登紀錄" :fetch-page="getMyListings">
    <template #default="{ items }">
      <div class="row g-3">
        <div v-for="listing in items" :key="listing.auctionId" class="col-12 col-lg-6">
          <article class="card h-100">
            <div class="card-body">
              <h2 class="h5">{{ listing.productName }}</h2>
              <p class="text-muted">{{ listing.model }} · {{ listing.size }}</p>
              <dl class="row mb-0">
                <dt class="col-5">目前價格</dt><dd class="col-7">{{ formatTwd(listing.currentPrice) }}</dd>
                <dt v-if="listing.finalPrice != null" class="col-5">成交價格</dt><dd v-if="listing.finalPrice != null" class="col-7">{{ formatTwd(listing.finalPrice) }}</dd>
                <dt class="col-5">出價次數</dt><dd class="col-7">{{ listing.bidCount }}</dd>
                <dt class="col-5">競標狀態</dt><dd class="col-7">{{ formatMemberAuctionStatus(listing.status) }}</dd>
                <dt class="col-5">結標結果</dt><dd class="col-7">{{ formatMemberAuctionStatus(listing.resultStatus) }}</dd>
                <dt class="col-5">開始時間</dt><dd class="col-7">{{ formatAuctionTime(listing.startAt) }}</dd>
                <dt class="col-5">結束時間</dt><dd class="col-7">{{ formatAuctionTime(listing.endAt) }}</dd>
              </dl>
              <RouterLink class="btn btn-outline-primary" :to="{ name: 'auction-detail', params: { id: listing.auctionId } }">查看競標</RouterLink>
            </div>
          </article>
        </div>
      </div>
    </template>
  </AuctionMemberPage>
</template>

<script setup>
import AuctionMemberPage from "@/components/auction/AuctionMemberPage.vue";
import { getMyListings } from "@/service/auctionService";
import { formatTwd } from "@/utils/auctionMoney";
import { formatAuctionTime } from "@/utils/auctionTime";
import { formatMemberAuctionStatus } from "@/utils/auctionMemberStatus";
</script>
