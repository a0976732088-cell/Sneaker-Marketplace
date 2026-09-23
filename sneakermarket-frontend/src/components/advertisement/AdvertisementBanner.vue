<script setup>
import { computed, onUnmounted } from "vue";
import { useAdvertisementTracking } from "@/composables/advertisement/useAdvertisementTracking";
import { resolveAdvertisementImageUrl } from "@/service/advertisementService";

const props = defineProps({
  currentBanner: {
    type: Object,
    required: true,
  },
  advertisements: {
    type: Array,
    required: true,
  },
  bannerIndex: {
    type: Number,
    required: true,
  },
  staticBanner: {
    type: Boolean,
    default: false,
  },
  bannerImageClickable: {
    type: Boolean,
    default: true,
  },
});

const emit = defineEmits([
  "select-target",
  "track-click",
  "previous",
  "next",
  "select",
]);

const { vViewableAd, disposeAdvertisementTracking } =
  useAdvertisementTracking();

const canRotate = computed(
  () => !props.staticBanner && props.advertisements.length > 1,
);

const isCommerceBanner = computed(() =>
  ["PRODUCT", "CATEGORY"].includes(props.currentBanner.targetType),
);

const isAuctionBanner = computed(() =>
  ["AUCTION", "HOT_AUCTION"].includes(props.currentBanner.targetType),
);

function handleBannerClick() {
  if (!props.bannerImageClickable || props.staticBanner) return;

  emit("select-target", props.currentBanner);
}

onUnmounted(() => {
  disposeAdvertisementTracking();
});
</script>

<template>
  <section v-viewable-ad="currentBanner" class="banner-ad">
    <Transition name="banner-fade">
      <img
        :key="currentBanner.adId"
        class="banner-image"
        :src="resolveAdvertisementImageUrl(currentBanner.imageUrl)"
        :alt="currentBanner.adName"
        :class="{
          'static-banner-image': staticBanner || !bannerImageClickable,
        }"
        @click="handleBannerClick"
      />
    </Transition>

    <Transition name="banner-fade">
      <div
        v-if="isCommerceBanner || isAuctionBanner"
        :key="`banner-actions-${currentBanner.adId}`"
        class="banner-actions"
      >
        <RouterLink
          v-if="isCommerceBanner"
          :to="{ name: 'commerce' }"
          class="banner-action banner-action-primary"
          @click.stop="emit('track-click', currentBanner)"
        >
          立即選購 <span aria-hidden="true">→</span>
        </RouterLink>

        <RouterLink
          v-if="isAuctionBanner"
          :to="{ name: 'auction' }"
          class="banner-action banner-action-auction-primary"
          @click.stop="emit('track-click', currentBanner)"
        >
          探索競標 <span aria-hidden="true">→</span>
        </RouterLink>
      </div>
    </Transition>

    <button
      v-if="canRotate"
      type="button"
      class="banner-arrow left"
      @click="emit('previous')"
    >
      ‹
    </button>

    <button
      v-if="canRotate"
      type="button"
      class="banner-arrow right"
      @click="emit('next')"
    >
      ›
    </button>

    <div v-if="canRotate" class="banner-dots">
      <button
        v-for="(ad, index) in advertisements"
        :key="ad.adId"
        type="button"
        :class="{ active: index === bannerIndex }"
        @click="emit('select', index)"
      ></button>
    </div>
  </section>
</template>
<style scoped src="./AdvertisementBanner.css"></style>
