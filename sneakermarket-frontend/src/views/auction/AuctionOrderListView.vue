<template>
  <AuctionMemberPage title="我的競標訂單" empty-message="目前沒有競標訂單" :fetch-page="getMyAuctionOrders">
    <template #default="{ items }">
      <div class="row g-3">
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
              <RouterLink class="btn btn-outline-primary" :to="{ name: 'auction-detail', params: { id: order.auctionId } }">查看競標</RouterLink>
            </div>
          </article>
        </div>
      </div>
    </template>
  </AuctionMemberPage>
</template>

<script setup>
import AuctionMemberPage from "@/components/auction/AuctionMemberPage.vue";
import { getMyAuctionOrders } from "@/service/auctionService";
import { formatTwd } from "@/utils/auctionMoney";
import { formatAuctionTime } from "@/utils/auctionTime";
import { formatAuctionOrderStatus } from "@/utils/auctionMemberStatus";
</script>
