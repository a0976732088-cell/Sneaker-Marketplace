<template>
  <aside class="order-summary" aria-labelledby="summary-heading">
    <h2 id="summary-heading">訂單摘要</h2>

    <div class="summary-product">
      <div class="summary-image-wrap">
        <img
          v-if="productImageUrl"
          :src="productImageUrl"
          :alt="`${order.productName || '競標商品'} 商品圖片`"
          class="summary-image"
          @error="emit('image-error')"
        />
        <div v-else class="summary-image summary-image--fallback" role="img" aria-label="目前沒有商品圖片">
          暫無圖片
        </div>
      </div>
      <div>
        <p class="summary-brand">{{ order.brandName || "品牌資料未提供" }}</p>
        <h3>{{ order.productName || "未命名商品" }}</h3>
        <p class="summary-meta">{{ productMeta }}</p>
      </div>
    </div>

    <dl class="amount-list">
      <div><dt>成交價格</dt><dd>{{ formatTwd(order.finalPrice) }}</dd></div>
      <div><dt>運費</dt><dd>{{ formatTwd(order.shippingFee) }}</dd></div>
      <div class="amount-total"><dt>訂單總額</dt><dd>{{ formatTwd(order.totalAmount) }}</dd></div>
    </dl>

    <div class="deadline-card">
      <span>付款期限</span>
      <strong>{{ formatAuctionTime(order.paymentDueAt) }}</strong>
      <p v-if="canOperateCheckout">剩餘 {{ remainingPaymentTimeText }}</p>
      <p v-else>{{ deadlineNotice || "此訂單目前不可付款" }}</p>
    </div>

    <p class="summary-order-number">訂單 {{ order.orderNumber }}</p>
  </aside>
</template>

<script setup>
import { formatTwd } from "@/utils/auctionMoney";
import { formatAuctionTime } from "@/utils/auctionTime";

defineProps({
  order: { type: Object, required: true },
  productImageUrl: { type: String, required: true },
  productMeta: { type: String, required: true },
  canOperateCheckout: { type: Boolean, required: true },
  remainingPaymentTimeText: { type: String, required: true },
  deadlineNotice: { type: String, required: true },
});

const emit = defineEmits(["image-error"]);
</script>

<style scoped>
.order-summary { position: sticky; top: 76px; padding: 24px; border: 1px solid #e9ecef; border-radius: 10px; background: #fff; }
.order-summary h2 { margin: 0; font-size: 1.15rem; }
.summary-product { display: grid; grid-template-columns: 92px minmax(0, 1fr); gap: 15px; margin: 20px 0; }
.summary-image-wrap { width: 92px; aspect-ratio: 4 / 3; overflow: hidden; border-radius: 7px; background: #f1f3f5; }
.summary-image { width: 100%; height: 100%; object-fit: cover; }
.summary-image--fallback { display: grid; place-items: center; color: #868e96; font-size: .72rem; }
.summary-brand, .summary-meta { margin: 0; color: #6c757d; font-size: .8rem; }
.summary-product h3 { margin: 4px 0 7px; font-size: 1rem; line-height: 1.4; }
.amount-list { display: grid; gap: 11px; margin: 0; padding-top: 18px; border-top: 1px solid #e9ecef; }
.amount-list div { display: flex; justify-content: space-between; gap: 16px; }
.amount-list dt { color: #6c757d; font-weight: 500; }
.amount-list dd { margin: 0; font-weight: 650; }
.amount-list .amount-total { margin-top: 4px; padding-top: 14px; border-top: 1px solid #e9ecef; color: #111; font-size: 1.05rem; }
.deadline-card { margin-top: 20px; padding: 15px; border-radius: 8px; background: #f8f9fa; }
.deadline-card span { display: block; color: #6c757d; font-size: .78rem; }
.deadline-card strong { display: block; margin-top: 4px; }
.deadline-card p { margin: 7px 0 0; color: #6c757d; font-size: .78rem; line-height: 1.45; }
.summary-order-number { margin: 18px 0 0; color: #6c757d; font-size: .78rem; overflow-wrap: anywhere; }

@media (max-width: 991.98px) {
  .order-summary { position: static; grid-row: 1; }
}

@media (max-width: 575.98px) {
  .order-summary { padding: 20px; }
  .summary-product { grid-template-columns: 82px minmax(0, 1fr); }
  .summary-image-wrap { width: 82px; }
}
</style>
