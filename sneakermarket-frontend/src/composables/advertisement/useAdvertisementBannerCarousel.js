import { computed, onUnmounted, ref, watch } from "vue";

export function useAdvertisementBannerCarousel({
  ads,
  getBannerOrder,
  getStaticBanner,
}) {
  const bannerIndex = ref(0);
  let bannerTimer = null;

  const bannerAds = computed(() =>
    ads.value.filter(
      (ad) => ad.adPlacement === "BANNER" && ad.adStatus === "ACTIVE",
    ),
  );

  const displayedBannerAds = computed(() => {
    const bannerOrder = getBannerOrder();

    if (bannerOrder === null) {
      return bannerAds.value;
    }

    return bannerAds.value.filter(
      (ad) => Number(ad.sortOrder) === bannerOrder,
    );
  });

  const currentBanner = computed(
    () => displayedBannerAds.value[bannerIndex.value] || null,
  );

  function restartBannerTimer() {
    if (bannerTimer !== null) {
      window.clearTimeout(bannerTimer);
    }

    if (
      getStaticBanner() ||
      displayedBannerAds.value.length <= 1
    ) {
      return;
    }

    bannerTimer = window.setTimeout(() => {
      nextBanner();
    }, 6500);
  }

  function nextBanner() {
    if (displayedBannerAds.value.length === 0) return;

    bannerIndex.value =
      (bannerIndex.value + 1) % displayedBannerAds.value.length;

    restartBannerTimer();
  }

  function previousBanner() {
    if (displayedBannerAds.value.length === 0) return;

    bannerIndex.value =
      (bannerIndex.value - 1 + displayedBannerAds.value.length) %
      displayedBannerAds.value.length;

    restartBannerTimer();
  }

  function selectBanner(index) {
    bannerIndex.value = index;
    restartBannerTimer();
  }

  watch(displayedBannerAds, (nextBannerAds) => {
    if (bannerIndex.value >= nextBannerAds.length) {
      bannerIndex.value = 0;
    }
  });

  onUnmounted(() => {
    if (bannerTimer !== null) {
      window.clearTimeout(bannerTimer);
    }
  });

  return {
    bannerIndex,
    displayedBannerAds,
    currentBanner,
    restartBannerTimer,
    nextBanner,
    previousBanner,
    selectBanner,
  };
}