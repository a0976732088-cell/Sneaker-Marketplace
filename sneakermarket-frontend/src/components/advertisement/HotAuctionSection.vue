<script setup>
import { onBeforeUnmount, onMounted, ref } from "vue";
import { getAuctions } from "@/service/auctionService";
import AuctionCountdown from "@/components/auction/AuctionCountdown.vue";
import { formatTwd } from "@/utils/auctionMoney";
import { resolveAuctionImageUrl } from "@/utils/auctionImageUrl";

const auctions = ref([]);
const loading = ref(false);
const errorMessage = ref("");
const HOT_AUCTION_REFRESH_INTERVAL_MS = 5000;

let refreshTimerId = null;
let refreshInProgress = false;

async function loadHotAuctions({ silent = false } = {}) {
  if (refreshInProgress) {
    return;
  }

  refreshInProgress = true;

  if (!silent) {
    loading.value = true;
    errorMessage.value = "";
  }

  try {
    const response = await getAuctions({
      status: "ACTIVE",
      sort: ["bidCount,desc", "endAt,asc"],
      page: 0,
      size: 4,
    });

    auctions.value = response.data?.data?.items ?? [];
    errorMessage.value = "";

  } catch (error) {
    console.error("首頁熱門競標讀取失敗：", error);

    if (!silent) {
      auctions.value = [];
      errorMessage.value = "熱門競標暫時無法載入";
    }
  } finally {
    refreshInProgress = false;

    if (!silent) {
      loading.value = false;
    }
  }
}

onMounted(() => {
  loadHotAuctions();

  refreshTimerId = window.setInterval(() => {
    loadHotAuctions({ silent: true });
  }, HOT_AUCTION_REFRESH_INTERVAL_MS);
});

onBeforeUnmount(() => {
  if (refreshTimerId !== null) {
    window.clearInterval(refreshTimerId);
    refreshTimerId = null;
  }
});
</script>

<template>
  <section
    v-if="loading || errorMessage || auctions.length"
    class="home-auction-section"
    aria-labelledby="home-auction-title"
  >
    <header class="home-auction-header">
      <div class="hot-auction-heading">
        <span class="hot-auction-live">
          <i aria-hidden="true"></i>
          LIVE
        </span>
        <h2 id="home-auction-title">熱門競標</h2>
      </div>

      <RouterLink :to="{ name: 'auction' }" class="view-all-link">
        查看全部競標
      </RouterLink>
    </header>

    <p v-if="loading" class="auction-message">熱門競標載入中……</p>

    <p v-else-if="errorMessage" class="auction-message error">
      {{ errorMessage }}
    </p>

    <div v-else class="home-auction-grid">
      <RouterLink
        v-for="(auction, index) in auctions"
        :key="auction.auctionId"
        :to="{
          name: 'auction-detail',
          params: { id: auction.auctionId },
        }"
        class="home-auction-card"
      >
        <div class="home-auction-image">
          <img
            v-if="auction.imageUrl"
            :src="resolveAuctionImageUrl(auction.imageUrl)"
            :alt="auction.productName"
            loading="lazy"
          />

          <div v-else class="auction-image-placeholder">尚無圖片</div>

          <span class="live-badge">競標中</span>

          <span class="auction-rank">
            HOT {{ String(index + 1).padStart(2, "0") }}
          </span>
        </div>

        <div class="home-auction-content">
          <p class="auction-brand">
            {{ auction.brandName || "品牌未提供" }}
          </p>

          <h3>{{ auction.productName }}</h3>

          <div class="auction-price">
            <span>目前價格</span>
            <Transition name="auction-price-change" mode="out-in">
              <strong
                :key="`${auction.auctionId}-${auction.currentPrice}`"
                aria-live="polite"
              >
                {{ formatTwd(auction.currentPrice) }}
              </strong>
            </Transition>
          </div>

          <div class="auction-meta">
            <span>{{ auction.bidCount ?? 0 }} 次出價</span>

            <span class="auction-time">
              剩餘
              <AuctionCountdown
                :end-at="auction.endAt"
                :status="auction.status"
              />
            </span>
          </div>
          <div class="auction-card-action">
            <span>前往競標</span>
            <b aria-hidden="true">→</b>
          </div>
        </div>
      </RouterLink>
    </div>
  </section>
</template>

<style scoped src="./HotAuctionSection.css"></style>