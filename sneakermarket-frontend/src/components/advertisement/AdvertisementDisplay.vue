<script setup>
import { useRouter } from "vue-router";
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useAdvertisementTracking } from "@/composables/advertisement/useAdvertisementTracking";
import AdvertisementCategoryCarousel from "./AdvertisementCategoryCarousel.vue";
import AdvertisementBanner from "./AdvertisementBanner.vue";
import AdvertisementPopup from "./AdvertisementPopup.vue";
import AdvertisementSmallBlock from "./AdvertisementSmallBlock.vue";
import { useAdvertisementPopup } from "@/composables/advertisement/useAdvertisementPopup";
import { usePublicAdvertisements } from "@/composables/advertisement/usePublicAdvertisements";
import { useAdvertisementBannerCarousel } from "@/composables/advertisement/useAdvertisementBannerCarousel";

const props = defineProps({
  placements: {
    type: Array,
    default: () => ["BANNER", "POPUP", "SMALL_BLOCK", "CATEGORY_CARD"],
  },
  bannerImageClickable: {
    type: Boolean,
    default: true,
  },
  // 指定目前頁面要顯示哪一組 BANNER
  bannerOrder: {
    type: Number,
    default: null,
  },
  // true 表示單張靜態 BANNER，不輪播也不能點擊
  staticBanner: {
    type: Boolean,
    default: false,
  },
  // 這個頁面要顯示哪一個排序的 SMALL_BLOCK
  smallBlockOrder: {
    type: Number,
    default: null,
  },
  showSmallBlockAction: {
    type: Boolean,
    default: false,
  },

  smallBlockActionLabel: {
    type: String,
    default: "立即報名",
  },
});
const emit = defineEmits(["select-category"]);

function hasPlacement(placement) {
  return props.placements.includes(placement);
}
const { ads, hotAuction, loading, errorMessage, loadAdvertisements } =
  usePublicAdvertisements(hasPlacement);
const { trackAdvertisementClick, disposeAdvertisementTracking } =
  useAdvertisementTracking();

const router = useRouter();

function goToAdvertisementTarget(ad) {
  if (!ad) return;

  if (ad.targetType === "HOT_AUCTION" && !hotAuction.value) {
    return;
  }

  // 放!ad後面避免空值上報
  trackAdvertisementClick(ad);

  if (ad.targetType === "CATEGORY") {
    if (ad.targetId) {
      emit("select-category", {
        categoryId: Number(ad.targetId),
        categoryName: ad.adName,
      });
    }

    return;
  }

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

  if (ad.targetType === "HOT_AUCTION") {
    router.push({
      name: "auction-detail",
      params: { id: hotAuction.value.auctionId },
    });
    return;
  }

  if (ad.targetType === "EVENT") {
    router.push({
      name: "calendar",
      query: ad.targetId ? { eventId: ad.targetId } : {},
    });
    return;
  }
}

const popupAds = computed(() =>
  ads.value.filter(
    (ad) => ad.adPlacement === "POPUP" && ad.adStatus === "ACTIVE",
  ),
);
const {
  bannerIndex,
  displayedBannerAds,
  currentBanner,
  restartBannerTimer,
  nextBanner,
  previousBanner,
  selectBanner,
} = useAdvertisementBannerCarousel({
  ads,
  getBannerOrder: () => props.bannerOrder,
  getStaticBanner: () => props.staticBanner,
});
const { isLoggedIn, currentPopup, loadLoginStatus } = useAdvertisementPopup({
  popupAds,
  hasPopupPlacement: () => hasPlacement("POPUP"),
});

const smallBlockAds = computed(() =>
  ads.value.filter(
    (ad) => ad.adPlacement === "SMALL_BLOCK" && ad.adStatus === "ACTIVE",
  ),
);

const categoryCardAds = computed(() =>
  ads.value.filter(
    (ad) => ad.adPlacement === "CATEGORY_CARD" && ad.adStatus === "ACTIVE",
  ),
);

const showPopup = ref(true);
const showSmallBlock = ref(true);

// 點擊目前顯示的 POPUP
function handlePopupClick() {
  if (!currentPopup.value) return;

  // 未登入者固定前往註冊頁
  if (!isLoggedIn.value) {
    trackAdvertisementClick(currentPopup.value);
    router.push({ name: "register" });
    return;
  }

  // 已登入者按照廣告設定前往指定商品
  goToAdvertisementTarget(currentPopup.value);
}

const currentSmallBlock = computed(() => {
  // 沒有指定排序時，維持原本行為：取得第一則廣告
  if (props.smallBlockOrder === null) {
    return smallBlockAds.value[0] || null;
  }

  // 找到 sortOrder 符合目前頁面的 SMALL_BLOCK
  return (
    smallBlockAds.value.find(
      (ad) => Number(ad.sortOrder) === props.smallBlockOrder,
    ) || null
  );
});

let advertisementRefreshTimer = null;

onMounted(async () => {
  // 廣告與 Session 狀態可以同時讀取
  await Promise.all([loadAdvertisements(), loadLoginStatus()]);
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
  disposeAdvertisementTracking();
});
</script>

<template>
  <section class="advertisement-preview">
    <div v-if="loading" class="text-center py-5">廣告讀取中……</div>

    <div v-else-if="errorMessage" class="alert alert-danger">
      {{ errorMessage }}
    </div>
    <template v-else>
      <AdvertisementBanner
        v-if="hasPlacement('BANNER') && currentBanner"
        :current-banner="currentBanner"
        :advertisements="displayedBannerAds"
        :banner-index="bannerIndex"
        :static-banner="staticBanner"
        :banner-image-clickable="bannerImageClickable"
        @select-target="goToAdvertisementTarget"
        @track-click="trackAdvertisementClick"
        @previous="previousBanner"
        @next="nextBanner"
        @select="selectBanner"
      />

      <p v-else-if="hasPlacement('BANNER')" class="empty-ad">
        目前沒有上架中的 BANNER 廣告
      </p>

      <slot name="after-banner"></slot>

      <AdvertisementCategoryCarousel
        v-if="hasPlacement('CATEGORY_CARD') && categoryCardAds.length"
        :advertisements="categoryCardAds"
        @select-ad="goToAdvertisementTarget"
      >
        <slot name="category-result"></slot>
      </AdvertisementCategoryCarousel>

      <AdvertisementSmallBlock
        v-if="
          hasPlacement('SMALL_BLOCK') &&
          showSmallBlock &&
          currentSmallBlock &&
          (currentSmallBlock.targetType !== 'HOT_AUCTION' || hotAuction)
        "
        :advertisement="currentSmallBlock"
        :show-action="showSmallBlockAction"
        :action-label="smallBlockActionLabel"
        @close="showSmallBlock = false"
        @action="goToAdvertisementTarget(currentSmallBlock)"
      />

      <AdvertisementPopup
        v-if="hasPlacement('POPUP') && showPopup && currentPopup"
        :popup="currentPopup"
        @close="showPopup = false"
        @click-ad="handlePopupClick"
      />
    </template>
  </section>
</template>

<style scoped>
/* 父元件只保留容器與 BANNER 無資料時的提示。 */
.advertisement-preview {
  position: relative;
}

.empty-ad {
  padding: 50px;
  color: #6c757d;
  background-color: #f8f9fa;
  text-align: center;
}
</style>
