<script setup>
import { useRouter } from "vue-router";
import { computed, onMounted, onUnmounted, ref } from "vue";
import {
  getPublicActiveAdvertisements,
  resolveAdvertisementImageUrl,
} from "@/service/advertisementService";

const props = defineProps({
  placements: {
    type: Array,
    default: () => ["BANNER", "POPUP", "SMALL_BLOCK"],
  },
});

function hasPlacement(placement) {
  return props.placements.includes(placement);
}

const ads = ref([]);
const router = useRouter();

function goToAdvertisementTarget(ad) {
  if (!ad) return;

  if (ad.targetType === "PRODUCT") {
    if (ad.targetId) {
      router.push({
        name: "product-detail",
        params: { id: ad.targetId },
      });
    } else {
      router.push({ name: "commerce" });
    }

    return;
  }

  if (ad.targetType === "AUCTION") {
    if (ad.targetId) {
      router.push({
        name: "auction-detail",
        params: { id: ad.targetId },
      });
    } else {
      router.push({ name: "auction" });
    }
  }
}
const loading = ref(false);
const errorMessage = ref("");

const bannerAds = computed(() =>
  ads.value.filter(
    (ad) => ad.adPlacement === "BANNER" && ad.adStatus === "ACTIVE",
  ),
);

const popupAds = computed(() =>
  ads.value.filter(
    (ad) => ad.adPlacement === "POPUP" && ad.adStatus === "ACTIVE",
  ),
);

const smallBlockAds = computed(() =>
  ads.value.filter(
    (ad) => ad.adPlacement === "SMALL_BLOCK" && ad.adStatus === "ACTIVE",
  ),
);
const bannerIndex = ref(0);
// 儲存自動播放計時器
let bannerTimer = null;

// 重新開始三秒倒數
function restartBannerTimer() {
  // 先取消上一個尚未結束的倒數
  if (bannerTimer !== null) {
    window.clearTimeout(bannerTimer);
  }

  // 只有一張或沒有 BANNER 時，不需要自動切換
  if (bannerAds.value.length <= 1) return;

  // 重新從三秒開始倒數
  bannerTimer = window.setTimeout(() => {
    nextBanner(); // 三秒後切換下一張
  }, 4500);
}
const showPopup = ref(true);
const showSmallBlock = ref(true);

const currentBanner = computed(() => {
  return bannerAds.value[bannerIndex.value] || null;
});

const currentPopup = computed(() => {
  return popupAds.value[0] || null;
});

const currentSmallBlock = computed(() => {
  return smallBlockAds.value[0] || null;
});

function nextBanner() {
  if (bannerAds.value.length === 0) return;

  // 切換到下一張，到最後一張會回到第一張
  bannerIndex.value = (bannerIndex.value + 1) % bannerAds.value.length;

  // 切換完成後，重新倒數三秒
  restartBannerTimer();
}

function previousBanner() {
  if (bannerAds.value.length === 0) return;

  // 切換到上一張，第一張再往前會回到最後一張
  bannerIndex.value =
    (bannerIndex.value - 1 + bannerAds.value.length) % bannerAds.value.length;

  // 使用者操作後，重新倒數三秒
  restartBannerTimer();
}

function selectBanner(index) {
  // 切換到使用者點選的圓點
  bannerIndex.value = index;

  // 使用者操作後，重新倒數三秒
  restartBannerTimer();
}

async function loadAdvertisements(silent = false) {
  if (!silent) {
    loading.value = true;
    errorMessage.value = "";
  }

  try {
    const result = await getPublicActiveAdvertisements();
    ads.value = result.data || [];
    errorMessage.value = "";
    if (bannerIndex.value >= bannerAds.value.length) {
      bannerIndex.value = 0;
    }
  } catch (error) {
    console.error(error);
    if (!silent) {
      errorMessage.value = "廣告讀取失敗";
    }
  } finally {
    if (!silent) {
      loading.value = false;
    }
  }
}
let advertisementRefreshTimer = null;
onMounted(async () => {
  await loadAdvertisements();
  restartBannerTimer();

  advertisementRefreshTimer = window.setInterval(async () => {
    await loadAdvertisements(true);
    restartBannerTimer();
  }, 60000);
});
onUnmounted(() => {
  if (bannerTimer !== null) {
    window.clearTimeout(bannerTimer);
  }

  if (advertisementRefreshTimer !== null) {
    window.clearInterval(advertisementRefreshTimer);
  }
});
</script>

<template>
  <section class="advertisement-preview">
    <div v-if="loading" class="text-center py-5">廣告讀取中……</div>

    <div v-else-if="errorMessage" class="alert alert-danger">
      {{ errorMessage }}
    </div>
    <template v-else>
      <section v-if="hasPlacement('BANNER') && currentBanner" class="banner-ad">
        <img
          :src="resolveAdvertisementImageUrl(currentBanner.imageUrl)"
          :alt="currentBanner.adName"
          @click="goToAdvertisementTarget(currentBanner)"
        />

        <button
          v-if="bannerAds.length > 1"
          type="button"
          class="banner-arrow left"
          @click="previousBanner"
        >
          ‹
        </button>

        <button
          v-if="bannerAds.length > 1"
          type="button"
          class="banner-arrow right"
          @click="nextBanner"
        >
          ›
        </button>

        <div v-if="bannerAds.length > 1" class="banner-dots">
          <button
            v-for="(ad, index) in bannerAds"
            :key="ad.adId"
            type="button"
            :class="{ active: index === bannerIndex }"
            @click="selectBanner(index)"
          ></button>
        </div>
      </section>

      <p v-else-if="hasPlacement('BANNER')" class="empty-ad">
        目前沒有上架中的 BANNER 廣告
      </p>

      <aside
        v-if="
          hasPlacement('SMALL_BLOCK') && showSmallBlock && currentSmallBlock
        "
        class="small-block-ad"
      >
        <button
          type="button"
          class="small-close"
          @click="showSmallBlock = false"
        >
          ×
        </button>

        <img
          :src="resolveAdvertisementImageUrl(currentSmallBlock.imageUrl)"
          :alt="currentSmallBlock.adName"
          @click="goToAdvertisementTarget(currentSmallBlock)"
        />
      </aside>

      <div
        v-if="hasPlacement('POPUP') && showPopup && currentPopup"
        class="popup-background"
      >
        <article class="popup-ad">
          <button type="button" class="popup-close" @click="showPopup = false">
            ×
          </button>

          <img
            :src="resolveAdvertisementImageUrl(currentPopup.imageUrl)"
            :alt="currentPopup.adName"
            @click="goToAdvertisementTarget(currentPopup)"
          />
        </article>
      </div>
    </template>
  </section>
</template>

<style scoped src="./AdvertisementPreviewView.css"></style>
