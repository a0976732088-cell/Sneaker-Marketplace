<template>
  <!-- 競標資訊 -->
  <div class="summary-heading">
    <span
      class="status-badge"
      :class="'status-' + auction.status.toLowerCase()"
    >
      {{ formatStatus(auction.status) }}
    </span>
    <h1>{{ auction.productName }}</h1>
    <p class="brand-category">
      {{ auction.brandName ?? "品牌資料未提供" }}
      <span aria-hidden="true">／</span>
      {{ auction.categoryName ?? "分類資料未提供" }}
    </p>
  </div>

  <dl class="product-meta-grid">
    <div>
      <dt>型號</dt>
      <dd>{{ auction.model || "-" }}</dd>
    </div>
    <div>
      <dt>尺寸</dt>
      <dd>{{ auction.size }}</dd>
    </div>
    <div>
      <dt>商品狀況</dt>
      <dd>{{ formatCondition(auction.condition) }}</dd>
    </div>
    <div>
      <dt>刊登會員</dt>
      <dd>賣家：{{ auction.sellerName || "會員資料未提供" }}</dd>
    </div>
  </dl>

  <div class="current-price-block">
    <span>目前價格</span>
    <strong>{{ formatTwd(auction.currentPrice) }}</strong>
    <small>起標價 {{ formatTwd(auction.startPrice) }}</small>
  </div>

  <dl class="auction-metrics">
    <div>
      <dt>出價次數</dt>
      <dd>{{ auction.bidCount }} 次</dd>
    </div>
    <div>
      <dt>最低加價</dt>
      <dd>{{ formatTwd(auction.minIncrement) }}</dd>
    </div>
    <div>
      <dt>下次最低出價</dt>
      <dd>{{ formatTwd(auction.nextMinimumBid) }}</dd>
    </div>
  </dl>

  <div class="auction-time-panel">
    <div>
      <span>開始時間</span>
      <strong>{{ formatDateTime(auction.startAt) }}</strong>
    </div>
    <div>
      <span>結束時間</span>
      <strong>{{ formatDateTime(auction.endAt) }}</strong>
    </div>
    <div class="remaining-row">
      <span>剩餘時間</span>
      <strong>
        <AuctionCountdown :end-at="auction.endAt" :status="auction.status" />
      </strong>
    </div>
  </div>
</template>

<script setup>
import AuctionCountdown from "@/components/auction/AuctionCountdown.vue";
import { formatTwd } from "@/utils/auctionMoney";
import { formatAuctionTime } from "@/utils/auctionTime";

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

function formatStatus(status) {
  const statusMap = {
    SCHEDULED: "尚未開始",
    ACTIVE: "競標中",
    SOLD: "已結標",
    UNSOLD: "流標",
    CANCELLED: "已取消",
    REMOVED: "廢標",
  };

  return statusMap[status] ?? status;
}

const formatDateTime = formatAuctionTime;
</script>

<style scoped>
.summary-heading {
  padding-bottom: 1.25rem;
  border-bottom: 1px solid #e9ecef;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  margin-bottom: 0.85rem;
  padding: 0.2rem 0.68rem;
  border-radius: 999px;
  font-size: 0.82rem;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.status-active {
  background: #111;
  color: #fff;
}

.status-scheduled {
  background: #f1f3f5;
  color: #343a40;
}

.status-sold {
  background: #e6f4ea;
  color: #1e7e34;
}

.status-unsold,
.status-cancelled,
.status-removed {
  background: #f1f1f1;
  color: #495057;
}

.summary-heading h1 {
  margin: 0;
  color: #111;
  font-size: clamp(1.75rem, 3.3vw, 2.6rem);
  font-weight: 800;
  line-height: 1.13;
  letter-spacing: -0.035em;
}

.brand-category {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin: 0.8rem 0 0;
  color: #495057;
  font-size: 1.05rem;
}

.product-meta-grid,
.auction-metrics {
  margin: 0;
}

.product-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  padding: 1.25rem 0;
  border-bottom: 1px solid #e9ecef;
}

.product-meta-grid div,
.auction-metrics div {
  min-width: 0;
}

.product-meta-grid dt,
.auction-metrics dt {
  margin-bottom: 0.25rem;
  color: #495057;
  font-size: 0.88rem;
  font-weight: 600;
}

.product-meta-grid dd,
.auction-metrics dd {
  margin: 0;
  color: #212529;
  overflow-wrap: anywhere;
}

.product-meta-grid dd {
  font-size: 1.05rem;
  font-weight: 600;
}

.current-price-block {
  display: flex;
  flex-direction: column;
  padding: 1.5rem 0 1.25rem;
}

.current-price-block > span {
  color: #495057;
  font-size: 0.95rem;
  font-weight: 600;
}

.current-price-block > strong {
  margin-top: 0.15rem;
  color: #111;
  font-size: clamp(2rem, 4vw, 2.8rem);
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: -0.035em;
}

.current-price-block > small {
  margin-top: 0.3rem;
  color: #495057;
  font-size: 0.95rem;
}

.auction-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border: 1px solid #e9ecef;
}

.auction-metrics div {
  padding: 0.85rem;
}

.auction-metrics div + div {
  border-left: 1px solid #e9ecef;
}

.auction-metrics dd {
  font-size: 1.05rem;
  font-weight: 700;
}

.auction-time-panel {
  margin-top: 1.25rem;
  border-top: 1px solid #e9ecef;
  border-bottom: 1px solid #e9ecef;
}

.auction-time-panel > div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.65rem 0;
}

.auction-time-panel > div + div {
  border-top: 1px solid #f1f3f5;
}

.auction-time-panel span {
  color: #495057;
  font-size: 0.95rem;
}

.auction-time-panel strong {
  color: #343a40;
  font-size: 1.05rem;
  text-align: right;
}

.auction-time-panel .remaining-row strong {
  color: #111;
  font-size: 1.15rem;
}

@media (max-width: 991.98px) {
  .auction-metrics {
    grid-template-columns: minmax(0, 1fr);
  }

  .auction-metrics div + div {
    border-top: 1px solid #e9ecef;
    border-left: 0;
  }
}

@media (max-width: 767.98px) {
  .summary-heading h1 {
    font-size: 1.8rem;
  }

  .auction-metrics {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .auction-metrics div + div {
    border-top: 0;
    border-left: 1px solid #e9ecef;
  }
}

@media (max-width: 479.98px) {
  .product-meta-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .auction-metrics {
    grid-template-columns: minmax(0, 1fr);
  }

  .auction-metrics div + div {
    border-top: 1px solid #e9ecef;
    border-left: 0;
  }

  .auction-time-panel > div {
    align-items: flex-start;
    flex-direction: column;
    gap: 0.2rem;
  }

  .auction-time-panel strong {
    text-align: left;
  }
}
</style>
