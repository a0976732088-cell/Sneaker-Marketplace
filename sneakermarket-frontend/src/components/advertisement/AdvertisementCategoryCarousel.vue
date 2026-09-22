<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { resolveAdvertisementImageUrl } from "@/service/advertisementService";
import { useAdvertisementTracking } from "@/composables/advertisement/useAdvertisementTracking";

const props = defineProps({
  advertisements: {
    type: Array,
    default: () => [],
  },
});

const emit = defineEmits(["select-ad"]);

const categoryTrack = ref(null);

const { vViewableAd, disposeAdvertisementTracking } =
  useAdvertisementTracking();

const loopingAdvertisements = computed(() => {
  const originalAds = props.advertisements;

  if (originalAds.length <= 2) {
    return originalAds.map((ad, index) => ({
      ...ad,
      loopKey: `original-${ad.adId}-${index}`,
    }));
  }

  const firstGroup = originalAds.map((ad, index) => ({
    ...ad,
    loopKey: `original-${ad.adId}-${index}`,
  }));

  const secondGroup = originalAds.map((ad, index) => ({
    ...ad,
    loopKey: `clone-${ad.adId}-${index}`,
  }));

  return [...firstGroup, ...secondGroup];
});

let categoryLoopResetTimer = null;
let categoryAutoScrollTimer = null;

let categoryManualNavigationLocked = false;
let categoryManualUnlockTimer = null;

function getCategoryScrollStep(track) {
  const firstCard = track.querySelector(".category-card");

  if (!firstCard) return 0;

  const gap = Number.parseFloat(window.getComputedStyle(track).columnGap) || 0;

  return firstCard.getBoundingClientRect().width + gap;
}

function setCategoryScrollInstantly(track, left) {
  const previousScrollBehavior = track.style.scrollBehavior;

  track.style.scrollBehavior = "auto";
  track.scrollLeft = left;
  track.style.scrollBehavior = previousScrollBehavior;
}

function clearCategoryLoopResetTimer() {
  if (categoryLoopResetTimer !== null) {
    window.clearTimeout(categoryLoopResetTimer);
    categoryLoopResetTimer = null;
  }
}

function scheduleCategoryLoopReset(track, loopWidth) {
  clearCategoryLoopResetTimer();

  categoryLoopResetTimer = window.setTimeout(() => {
    if (track === categoryTrack.value) {
      setCategoryScrollInstantly(
        track,
        track.scrollLeft - loopWidth,
      );
    }

    categoryLoopResetTimer = null;
  }, 700);
}

function scrollCategoryCards(direction) {
  clearCategoryLoopResetTimer();
  const track = categoryTrack.value;

  if (!track) return;

  const step = getCategoryScrollStep(track);
  const originalCount = props.advertisements.length;

  if (step === 0 || originalCount === 0) return;

  const canLoop = originalCount > 2;
  const loopWidth = step * originalCount;

  if (canLoop && direction < 0 && track.scrollLeft <= 2) {
    setCategoryScrollInstantly(track, loopWidth);
  }

  const targetLeft = track.scrollLeft + direction * step;

  track.scrollTo({
    left: targetLeft,
    behavior: "smooth",
  });

  if (canLoop && direction > 0 && targetLeft >= loopWidth - 2) {
    scheduleCategoryLoopReset(track, loopWidth);
  }
}

function stopCategoryAutoScroll() {
  if (categoryAutoScrollTimer !== null) {
    window.clearInterval(categoryAutoScrollTimer);
    categoryAutoScrollTimer = null;
  }
}

function startCategoryAutoScroll() {
  stopCategoryAutoScroll();

  if (props.advertisements.length <= 2) return;

  categoryAutoScrollTimer = window.setInterval(() => {
    scrollCategoryCards(1);
  }, 4000);
}

function navigateCategoryCards(direction) {
  if (categoryManualNavigationLocked) return;

  categoryManualNavigationLocked = true;

  scrollCategoryCards(direction);
  startCategoryAutoScroll();

  categoryManualUnlockTimer = window.setTimeout(() => {
    categoryManualNavigationLocked = false;
    categoryManualUnlockTimer = null;
  }, 750);
}

function selectAdvertisement(ad) {
  emit("select-ad", ad);
}

watch(
  () => props.advertisements.length,
  () => {
    if (categoryTrack.value) {
      setCategoryScrollInstantly(categoryTrack.value, 0);
    }

    startCategoryAutoScroll();
  },
  {
    flush: "post",
  },
);

onMounted(() => {
  startCategoryAutoScroll();
});

onUnmounted(() => {
  stopCategoryAutoScroll();
  clearCategoryLoopResetTimer();
   if (categoryManualUnlockTimer !== null) {
    window.clearTimeout(categoryManualUnlockTimer);
  }
  disposeAdvertisementTracking();
});
</script>
<template>
  <section class="category-card-section">
    <header class="category-card-header">
      <h2>一般商城</h2>

      <div class="category-card-nav">
        <button
          type="button"
          aria-label="查看上一個分類"
          @click="navigateCategoryCards(-1)"
        >
          ←
        </button>

        <button
          type="button"
          aria-label="查看下一個分類"
          @click="navigateCategoryCards(1)"
        >
          →
        </button>
      </div>
    </header>

    <div
      ref="categoryTrack"
      class="category-card-track"
      @mouseenter="stopCategoryAutoScroll"
      @mouseleave="startCategoryAutoScroll"
      @focusin="stopCategoryAutoScroll"
      @focusout="startCategoryAutoScroll"
    >
      <article
        v-for="ad in loopingAdvertisements"
        :key="ad.loopKey"
        v-viewable-ad="ad"
        class="category-card"
      >
        <button
          type="button"
          class="category-ad-button"
          @click="selectAdvertisement(ad)"
        >
          <img
            :src="resolveAdvertisementImageUrl(ad.imageUrl)"
            :alt="ad.adName"
          />

          <div class="category-ad-overlay">
            <div class="category-ad-copy">
              <strong>{{ ad.adName }}</strong>

              <small v-if="ad.adDescription">
                {{ ad.adDescription }}
              </small>

              <span class="category-ad-action">
                探索系列
                <b>→</b>
              </span>
            </div>
          </div>
        </button>
      </article>
    </div>

    <div class="category-result-area">
      <slot></slot>
    </div>
  </section>
</template>

<style scoped src="./AdvertisementCategoryCarousel.css"></style>