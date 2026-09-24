<template>
  <div class="row g-4 mt-0">
    <div class="col-12 col-xl-5">
      <section class="card shadow-sm h-100">
        <div class="card-header fw-semibold">競標結果</div>
        <div v-if="!result" class="card-body text-secondary">
          尚未產生競標結果
        </div>
        <dl v-else class="row card-body detail-list mb-0">
          <dt class="col-5">結果狀態</dt>
          <dd class="col-7">
            <span class="badge" :class="resultBadge(result.resultStatus)">
              {{ formatAuctionResultStatus(result.resultStatus) }}
            </span>
          </dd>
          <dt class="col-5">成交價格</dt>
          <dd class="col-7">{{ formatMoney(result.finalPrice) }}</dd>
          <dt class="col-5">得標會員</dt>
          <dd class="col-7">{{ result.winnerName || "—" }}</dd>
          <dt class="col-5">結標時間</dt>
          <dd class="col-7">{{ formatTime(result.endedAt) }}</dd>
          <dt class="col-5">結果建立</dt>
          <dd class="col-7">{{ formatTime(result.createdAt) }}</dd>
          <dt class="col-5">結果原因</dt>
          <dd class="col-7 reason-value">{{ result.reason || "—" }}</dd>
        </dl>
      </section>
    </div>

    <div class="col-12 col-xl-7">
      <section class="card shadow-sm h-100">
        <div class="card-header fw-semibold">競標訂單</div>
        <div v-if="!order" class="card-body text-secondary">
          尚未建立競標訂單
        </div>
        <div v-else class="card-body">
          <div
            v-if="order.orderStatus === 'ABANDONED'"
            class="alert alert-warning py-2"
          >
            此訂單已棄標（買家逾期未付款），不能再進行付款。
          </div>
          <dl class="row detail-list mb-0">
            <dt class="col-5">訂單編號</dt>
            <dd class="col-7">{{ order.orderNumber }}</dd>
            <dt class="col-5">訂單狀態</dt>
            <dd class="col-7 fw-semibold">
              {{ formatAuctionOrderStatus(order.orderStatus) }}
            </dd>
            <dt class="col-5">付款狀態</dt>
            <dd class="col-7">
              {{ formatAuctionOrderStatus(order.paymentStatus) }}
            </dd>
            <dt class="col-5">物流狀態</dt>
            <dd class="col-7">
              {{ formatAuctionOrderStatus(order.shippingStatus) }}
            </dd>
            <dt class="col-5">成交價格</dt>
            <dd class="col-7">{{ formatMoney(order.finalPrice) }}</dd>
            <dt class="col-5">買家</dt>
            <dd class="col-7">{{ order.buyerName || "會員資料未提供" }}</dd>
            <dt class="col-5">賣家</dt>
            <dd class="col-7">{{ order.sellerName || "會員資料未提供" }}</dd>
            <dt class="col-5">訂單建立</dt>
            <dd class="col-7">{{ formatTime(order.createdAt) }}</dd>
            <dt class="col-5">付款期限</dt>
            <dd class="col-7">{{ formatTime(order.paymentDueAt) }}</dd>
            <dt class="col-5">付款時間</dt>
            <dd class="col-7">{{ formatTime(order.paidAt) }}</dd>
            <dt class="col-5">棄標時間</dt>
            <dd class="col-7">{{ formatTime(order.abandonedAt) }}</dd>
            <dt class="col-5">出貨時間</dt>
            <dd class="col-7">{{ formatTime(order.shippedAt) }}</dd>
            <dt class="col-5">完成時間</dt>
            <dd class="col-7">{{ formatTime(order.completedAt) }}</dd>
            <dt class="col-5">最後更新</dt>
            <dd class="col-7">{{ formatTime(order.updatedAt) }}</dd>
          </dl>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import {
  formatAuctionOrderStatus,
  formatAuctionResultStatus,
} from "@/utils/auctionMemberStatus";

defineProps({
  result: { type: Object, default: null },
  order: { type: Object, default: null },
  formatMoney: { type: Function, required: true },
  formatTime: { type: Function, required: true },
});

function resultBadge(status) {
  return (
    {
      SOLD: "text-bg-success",
      UNSOLD: "text-bg-secondary",
      CANCELLED: "text-bg-warning",
      REMOVED: "text-bg-danger",
    }[status] || "text-bg-secondary"
  );
}
</script>

<style scoped>
.detail-list dt,
.detail-list dd {
  margin-bottom: 0.8rem;
}
.detail-list dd {
  overflow-wrap: anywhere;
}
.reason-value {
  white-space: pre-wrap;
}
</style>
