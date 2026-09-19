<script setup>
import { useRouter } from "vue-router";
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import {
  getPublicActiveAdvertisements,
  resolveAdvertisementImageUrl,
} from "@/service/advertisementService";
import { getCurrentMember } from "@/service/authService";
import { claimMemberPopup } from "./advertisementPopupMemory";
import { useAdvertisementTracking } from "@/composables/advertisement/useAdvertisementTracking";

const props = defineProps({
  placements: {
    type: Array,
    default: () => ["BANNER", "POPUP", "SMALL_BLOCK", "CATEGORY_CARD"],
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
});
const emit = defineEmits(["select-category"]);

function hasPlacement(placement) {
  return props.placements.includes(placement);
}

const ads = ref([]);

const { trackAdvertisementClick, vViewableAd, disposeAdvertisementTracking } =
  useAdvertisementTracking();

const router = useRouter();
// 是否已有會員登入
const isLoggedIn = ref(false);

// 是否已完成 Session 檢查
// 避免檢查完成前先閃出未登入者的 POPUP
const sessionChecked = ref(false);

async function loadLoginStatus() {
  // 沒有使用 POPUP 的頁面，不需要呼叫會員 Session API
  if (!hasPlacement("POPUP")) {
    sessionChecked.value = true;
    return;
  }

  try {
    // 成功取得會員資料，代表 Session 仍然有效
    await getCurrentMember();
    isLoggedIn.value = true;
  } catch (error) {
    // 401 代表目前沒有登入
    isLoggedIn.value = false;

    // 不是正常的 401 才輸出錯誤
    if (error.status !== 401) {
      console.error("會員登入狀態讀取失敗：", error);
    }
  } finally {
    // 不論有沒有登入，都表示檢查已完成
    sessionChecked.value = true;
  }
}

function goToAdvertisementTarget(ad) {
  if (!ad) return;

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

  if (ad.targetType === "EVENT") {
    router.push({
      name: "calendar",
      query: ad.targetId ? { eventId: ad.targetId } : {},
    });
    return;
  }
}

const loading = ref(false);
const errorMessage = ref("");

const bannerAds = computed(() =>
  ads.value.filter(
    (ad) => ad.adPlacement === "BANNER" && ad.adStatus === "ACTIVE",
  ),
);

// 根據目前頁面指定的 bannerOrder，取得這個頁面可以顯示的 BANNER
const displayedBannerAds = computed(() => {
  // 沒有指定 bannerOrder 時，維持原本行為：顯示全部 BANNER
  if (props.bannerOrder === null) {
    return bannerAds.value;
  }

  // 只保留 sortOrder 符合目前頁面的 BANNER
  return bannerAds.value.filter(
    (ad) => Number(ad.sortOrder) === props.bannerOrder,
  );
});

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

const categoryCardAds = computed(() =>
  ads.value.filter(
    (ad) => ad.adPlacement === "CATEGORY_CARD" && ad.adStatus === "ACTIVE",
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

  // 靜態 BANNER，或目前只有一張圖片時，不啟動自動輪播
  if (props.staticBanner || displayedBannerAds.value.length <= 1) return;

  // 重新從三秒開始倒數
  bannerTimer = window.setTimeout(() => {
    nextBanner(); // 三秒後切換下一張
  }, 4500);
}
const showPopup = ref(true);
const showSmallBlock = ref(true);
const allowPopupThisMount = ref(false);

const currentBanner = computed(() => {
  return displayedBannerAds.value[bannerIndex.value] || null;
});

watch(
  [sessionChecked, isLoggedIn, popupAds],
  ([checked, loggedIn, popups]) => {
    if (!checked) return;

    if (!loggedIn) {
      allowPopupThisMount.value = true;
      return;
    }

    if (allowPopupThisMount.value) return;

    const hasMemberPopup = popups.some((ad) => Number(ad.sortOrder) === 1);

    if (!hasMemberPopup) return;

    allowPopupThisMount.value = claimMemberPopup();
  },
  {
    immediate: true,
  },
);

const currentPopup = computed(() => {
  // Session 還沒確認完成時，先不要顯示 POPUP
  if (!sessionChecked.value || !allowPopupThisMount.value) return null;

  // 未登入使用 0，已登入使用 1
  const popupOrder = isLoggedIn.value ? 1 : 0;

  // 找到對應使用者狀態的 POPUP
  return (
    popupAds.value.find((ad) => Number(ad.sortOrder) === popupOrder) || null
  );
});
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

function nextBanner() {
  if (displayedBannerAds.value.length === 0) return;

  // 切換到下一張，到最後一張會回到第一張
  bannerIndex.value = (bannerIndex.value + 1) % displayedBannerAds.value.length;

  // 切換完成後，重新倒數三秒
  restartBannerTimer();
}

function previousBanner() {
  if (displayedBannerAds.value.length === 0) return;

  // 切換到上一張，第一張再往前會回到最後一張
  bannerIndex.value =
    (bannerIndex.value - 1 + displayedBannerAds.value.length) %
    displayedBannerAds.value.length;

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
    if (bannerIndex.value >= displayedBannerAds.value.length) {
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
      <section
        v-if="hasPlacement('BANNER') && currentBanner"
        v-viewable-ad="currentBanner"
        class="banner-ad"
      >
        <img
          :src="resolveAdvertisementImageUrl(currentBanner.imageUrl)"
          :alt="currentBanner.adName"
          :class="{ 'static-banner-image': staticBanner }"
          @click="!staticBanner && goToAdvertisementTarget(currentBanner)"
        />

        <button
          v-if="!staticBanner && displayedBannerAds.length > 1"
          type="button"
          class="banner-arrow left"
          @click="previousBanner"
        >
          ‹
        </button>

        <button
          v-if="!staticBanner && displayedBannerAds.length > 1"
          type="button"
          class="banner-arrow right"
          @click="nextBanner"
        >
          ›
        </button>

        <div
          v-if="!staticBanner && displayedBannerAds.length > 1"
          class="banner-dots"
        >
          <button
            v-for="(ad, index) in displayedBannerAds"
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

      <section
        v-if="hasPlacement('CATEGORY_CARD') && categoryCardAds.length"
        class="category-card-section"
      >
        <header class="category-card-header">
          <h2>推薦分類</h2>
        </header>

        <div class="category-card-track">
          <article
            v-for="ad in categoryCardAds"
            :key="ad.adId"
            v-viewable-ad="ad"
            class="category-card"
          >
            <button
              type="button"
              class="category-card-image-button"
              @click="goToAdvertisementTarget(ad)"
            >
              <img
                :src="resolveAdvertisementImageUrl(ad.imageUrl)"
                :alt="ad.adName"
              />
            </button>

            <div class="category-card-footer">
              <button
                type="button"
                class="category-card-title"
                @click="goToAdvertisementTarget(ad)"
              >
                <strong>{{ ad.adName }}</strong>
              </button>

              <!-- 廣告被元件實際選中顯示，而且同一次元件掛載只計一次 -->
              <RouterLink
                v-if="ad.targetType === 'CATEGORY' && ad.targetId"
                class="category-view-link"
                :to="{
                  name: 'commerce',
                  query: {
                    categoryId: ad.targetId,
                  },
                }"
                @click="trackAdvertisementClick(ad)"
              >
                查看相關
              </RouterLink>
            </div>
          </article>
        </div>
      </section>

      <aside
        v-if="
          hasPlacement('SMALL_BLOCK') && showSmallBlock && currentSmallBlock
        "
        v-viewable-ad="currentSmallBlock"
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
        <article v-viewable-ad="currentPopup" class="popup-ad">
          <button type="button" class="popup-close" @click="showPopup = false">
            ×
          </button>

          <img
            :src="resolveAdvertisementImageUrl(currentPopup.imageUrl)"
            :alt="currentPopup.adName"
            @click="handlePopupClick"
          />
        </article>
      </div>
    </template>
  </section>
</template>

<style scoped src="./AdvertisementDisplay.css"></style>
