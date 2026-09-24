<template>
  <article class="auction-product-card">
    <!-- 商品圖片 -->
    <div class="product-image-wrap">
      <img
        v-if="auction.imageUrl && !imageFailed"
        :src="resolveAuctionImageUrl(auction.imageUrl)"
        :alt="auction.productName"
        class="product-image"
        @error="$emit('image-error', auction.imageUrl)"
      />

      <div
        v-else
        class="product-image product-image-empty"
        aria-hidden="true"
      >
        <span class="placeholder-mark"> SM </span>

        <span>尚無圖片</span>
      </div>

      <span
        class="auction-status-badge"
        :class="statusClass(auction.status)"
      >
        {{ formatStatus(auction.status) }}
      </span>
    </div>

    <!-- 商品資訊 -->
    <div class="product-card-body">
      <p class="product-brand">
        {{ auction.brandName ?? "品牌資料未提供" }}
      </p>

      <h3>
        {{ auction.productName }}
      </h3>

      <p v-if="auction.model" class="product-model">
        {{ auction.model }}
      </p>

      <p class="product-meta">
        {{ auction.size }} ·
        {{ formatCondition(auction.condition) }}
      </p>

      <!-- 價格 -->
      <div class="price-block">
        <span>目前價格</span>

        <strong>
          {{ formatTwd(auction.currentPrice) }}
        </strong>
      </div>

      <!-- 競標資訊 -->
      <div class="auction-card-stats">
        <span> {{ auction.bidCount }} 次出價 </span>

        <span>
          {{ formatRemainingTime(auction) }}
        </span>
      </div>

      <!-- 其他資訊 -->
      <div class="auction-card-footer">
        <span>
          賣家：{{ auction.sellerName || "會員資料未提供" }}
        </span>

        <span>
          結束
          {{ formatDateTime(auction.endAt) }}
        </span>
      </div>
    </div>
  </article>
</template>

<script setup>
import { resolveAuctionImageUrl } from "@/utils/auctionImageUrl";
import { formatTwd } from "@/utils/auctionMoney";
import { parseAuctionTime, formatAuctionTime } from "@/utils/auctionTime";

const props = defineProps({
  auction: { type: Object, required: true },
  currentTime: { type: Number, required: true },
  imageFailed: { type: Boolean, default: false },
});

defineEmits(["image-error"]);
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

/**
 * 回傳競標狀態 Badge class。
 */
function statusClass(status) {
  const classMap = {
    SCHEDULED: "status-scheduled",

    ACTIVE: "status-active",

    SOLD: "status-ended",

    UNSOLD: "status-ended",

    CANCELLED: "status-ended",

    REMOVED: "status-ended",
  };

  return classMap[status] ?? "status-default";
}

function formatCondition(condition) {
  const conditionMap = {
    NEW: "全新",
    LIKE_NEW: "近全新",
    USED: "二手",
  };

  return conditionMap[condition] ?? condition;
}

/**
 * 將後端時間轉成前端顯示格式。
 */
const formatDateTime = formatAuctionTime;

/**
 * 計算競標剩餘時間。
 */
function formatRemainingTime(auction) {
  // 已進入終態的競標不再顯示倒數
  if (["SOLD", "UNSOLD", "CANCELLED", "REMOVED"].includes(auction.status)) {
    return "已結束";
  }

  if (!auction.endAt) {
    return "-";
  }

  const endTime = parseAuctionTime(auction.endAt).getTime();

  const diff = endTime - props.currentTime;

  // 前端倒數歸零不代表後端已完成結標
  if (diff <= 0) {
    return "已到截止時間";
  }

  const totalMinutes = Math.floor(diff / 1000 / 60);

  const days = Math.floor(totalMinutes / 1440);

  const hours = Math.floor((totalMinutes % 1440) / 60);

  const minutes = totalMinutes % 60;

  if (days > 0) {
    return `剩餘 ${days} 天 ${hours} 小時`;
  }

  if (hours > 0) {
    return `剩餘 ${hours} 小時 ${minutes} 分`;
  }

  return `剩餘 ${minutes} 分`;
}
</script>

<style scoped>
.auction-product-card {
  height: 100%;
  background: #fff;
}

.product-image-wrap {
  position: relative;
  overflow: hidden;
  background: #f1f1f1;
}

.product-image {
  display: block;

  width: 100%;
  aspect-ratio: 1 / 1;

  object-fit: cover;

  transition: transform 0.3s ease;
}

.product-image-empty {
  display: flex;

  flex-direction: column;
  align-items: center;
  justify-content: center;

  gap: 0.4rem;
  color: #495057;
  font-size: 0.9rem;
}

.placeholder-mark {
  color: #5d6670;

  font-size: 1.5rem;
  font-weight: 800;

  letter-spacing: 0.08em;
}

.auction-status-badge {
  position: absolute;

  top: 0.7rem;
  left: 0.7rem;

  padding: 0.28rem 0.62rem;

  border-radius: 999px;

  font-size: 0.85rem;
  font-weight: 800;

  letter-spacing: 0.03em;
}

.status-active {
  background: #111;
  color: #fff;
}

.status-scheduled {
  background: #fff;
  color: #212529;
}

.status-ended,
.status-default {
  background: #e9ecef;
  color: #495057;
}

.product-card-body {
  padding: 1rem 0.25rem 0;
}

.product-brand {
  margin: 0 0 0.25rem;
  color: #495057;

  font-size: 0.95rem;
  font-weight: 400;
}

.product-card-body h3 {
  min-height: 2.7em;

  margin: 0;
  overflow: hidden;

  color: #111;

  font-size: 1.1rem;
  font-weight: 500;
  line-height: 1.35;

  display: -webkit-box;

  -webkit-box-orient: vertical;

  -webkit-line-clamp: 2;

  line-clamp: 2;
}

.product-model,
.product-meta {
  margin: 0.3rem 0 0;

  overflow: hidden;
  color: #495057;
  font-size: 0.95rem;

  text-overflow: ellipsis;

  white-space: nowrap;
}

.price-block {
  display: flex;
  flex-direction: column;

  gap: 0.1rem;

  margin-top: 0.9rem;
}

.price-block span {
  color: #495057;
  font-size: 0.9rem;
}

.price-block strong {
  color: #111;
  font-size: 1.1rem;
  font-weight: 600;
}

.auction-card-stats {
  display: flex;

  justify-content: space-between;

  gap: 0.5rem;

  margin-top: 0.8rem;
  padding-top: 0.7rem;

  border-top: 1px solid #e9ecef;

  color: #495057;

  font-size: 0.9rem;
  font-weight: 600;
}

.auction-card-footer {
  display: flex;
  flex-direction: column;

  gap: 0.2rem;

  margin-top: 0.65rem;
  color: #495057;
  font-size: 0.9rem;
}
</style>
