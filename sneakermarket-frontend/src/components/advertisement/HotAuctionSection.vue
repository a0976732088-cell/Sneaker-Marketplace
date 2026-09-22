<script setup>
import { onBeforeUnmount, onMounted, ref } from "vue";
import { getAuctions } from "@/service/auctionService";
import HomeAuctionCountdown from "@/components/advertisement/HomeAuctionCountdown.vue";
import { resolveAuctionImageUrl } from "@/utils/auctionImageUrl";

const props = defineProps({
  limit: {
    type: Number,
    default: 4,
  },
  heading: {
    type: String,
    default: "熱門競標",
  },
  sort: {
    type: Array,
    default: () => ["bidCount,desc", "endAt,asc"],
  },
  compact: {
    type: Boolean,
    default: false,
  },
});

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
      sort: props.sort,
      page: 0,
      size: props.limit,
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
    :class="{ compact }"
    aria-labelledby="home-auction-title"
  >
    <header class="home-auction-header">
      <div class="hot-auction-heading">
        <h2 id="home-auction-title">{{ heading }}</h2>
      </div>

      <RouterLink :to="{ name: 'auction' }" class="view-all-link">
        查看全部
      </RouterLink>
    </header>

    <p v-if="loading" class="auction-message">熱門競標載入中……</p>

    <p v-else-if="errorMessage" class="auction-message error">
      {{ errorMessage }}
    </p>

    <div v-else class="home-auction-grid">
      <RouterLink
        v-for="auction in auctions"
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
        </div>

        <div class="home-auction-content">
          <h3>{{ auction.productName }}</h3>

          <p class="auction-brand">
            {{ auction.brandName || "品牌未提供" }}
          </p>

          <div class="auction-card-summary">
            <HomeAuctionCountdown
              :end-at="auction.endAt"
              :status="auction.status"
            />
          </div>
        </div>
      </RouterLink>
    </div>
  </section>
</template>

<style scoped src="./HotAuctionSection.css"></style>
