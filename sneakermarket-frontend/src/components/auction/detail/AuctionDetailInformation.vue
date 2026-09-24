<script setup>
import { formatTwd } from "@/utils/auctionMoney";

defineProps({
  auction: {
    type: Object,
    required: true,
  },
  formatCondition: {
    type: Function,
    required: true,
  },
});

/**
 * 將 Boolean 轉成較容易閱讀的中文。
 */
function formatBoolean(value) {
  return value ? "是" : "否";
}

/**
 * 將既有運送方式與面交 Boolean 組合成清楚的公開交付說明。
 */
function formatDeliveryMethod(shippingMethod, allowMeetup) {
  const method =
    typeof shippingMethod === "string" && shippingMethod.trim()
      ? shippingMethod.trim()
      : "-";

  return allowMeetup ? `${method}／可協調面交` : method;
}
</script>

<template>
  <div class="detail-information">
    <section class="information-section" aria-labelledby="product-info-title">
      <header>
        <p>Product</p>
        <h2 id="product-info-title">商品資訊</h2>
      </header>

      <dl class="information-list">
        <div class="full-row">
          <dt>商品描述</dt>
          <dd>{{ auction.description || "-" }}</dd>
        </div>
        <div>
          <dt>商品狀況</dt>
          <dd>{{ formatCondition(auction.condition) }}</dd>
        </div>
        <div>
          <dt>商品所在地</dt>
          <dd>{{ auction.location || "-" }}</dd>
        </div>
        <div class="full-row">
          <dt>瑕疵說明</dt>
          <dd>{{ auction.defectNote || "無" }}</dd>
        </div>
        <div class="full-row">
          <dt>真偽／來源說明</dt>
          <dd>{{ auction.authenticityNote || "-" }}</dd>
        </div>
      </dl>
    </section>

    <!-- 交易資訊 -->
    <section class="information-section" aria-labelledby="transaction-info-title">
      <header>
        <p>Transaction</p>
        <h2 id="transaction-info-title">交易資訊</h2>
      </header>

      <dl class="information-list">
        <div>
          <dt>交付方式</dt>
          <dd>
            {{
              formatDeliveryMethod(
                auction.shippingMethod,
                auction.allowMeetup,
              )
            }}
          </dd>
        </div>
        <div>
          <dt>運費</dt>
          <dd>{{ formatTwd(auction.shippingFee) }}</dd>
        </div>
        <div>
          <dt>付款方式</dt>
          <dd>{{ auction.paymentMethod }}</dd>
        </div>
        <div>
          <dt>允許退貨</dt>
          <dd>{{ formatBoolean(auction.allowReturn) }}</dd>
        </div>
        <div class="full-row">
          <dt>保固／售後</dt>
          <dd>{{ auction.warrantyNote || "-" }}</dd>
        </div>
      </dl>
    </section>
  </div>
</template>

<style scoped>
.detail-information {
  display: flex;
  flex-direction: column;
  gap: 2rem;
  max-width: 920px;
}

.information-section + .information-section {
  padding-top: 1.5rem;
  border-top: 1px solid #dee2e6;
}

.information-section header {
  margin-bottom: 0.4rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #dee2e6;
}

.information-section header p {
  margin: 0 0 0.25rem;
  color: #495057;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-size: 0.66rem;
  font-weight: 700;
}

.information-section h2 {
  margin: 0;
  color: #111;
  font-size: 1.35rem;
  font-weight: 800;
}

.information-list {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  margin: 0;
}

.information-list div {
  display: grid;
  grid-template-columns: minmax(0, 34%) minmax(0, 1fr);
  align-items: start;
  gap: clamp(0.75rem, 2vw, 1.25rem);
  min-width: 0;
  padding: 0.85rem 0;
}

.information-list dt {
  margin: 0;
  color: #212529;
  font-size: 1.05rem;
  font-weight: 700;
  line-height: 1.7;
}

.information-list dd {
  margin: 0;
  color: #212529;
  overflow-wrap: anywhere;
  font-size: 1.05rem;
  line-height: 1.7;
  white-space: pre-line;
}

@media (max-width: 575.98px) {
  .information-list div {
    grid-template-columns: minmax(0, 1fr);
    gap: 0.2rem;
    padding: 0.75rem 0;
  }
}
</style>
