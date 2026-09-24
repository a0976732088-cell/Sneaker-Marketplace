import { ref } from "vue";
import {
  getPublicActiveAdvertisements,
} from "@/service/advertisementService";
import { getAuctions } from "@/service/auctionService";

export function usePublicAdvertisements(hasPlacement) {
  const ads = ref([]);
  const hotAuction = ref(null);
  const loading = ref(false);
  const errorMessage = ref("");

  async function loadHotAuction() {
    try {
      const response = await getAuctions({
        status: "ACTIVE",
        sort: "bidCount,desc",
        page: 0,
        size: 1,
      });

      hotAuction.value = response.data?.data?.items?.[0] ?? null;
    } catch (error) {
      console.error("熱門競標讀取失敗：", error);
      hotAuction.value = null;
    }
  }

  async function loadAdvertisements(silent = false) {
    if (!silent) {
      loading.value = true;
      errorMessage.value = "";
    }

    try {
      const result = await getPublicActiveAdvertisements();
      ads.value = result.data || [];

      const hasHotAuctionAd = ads.value.some(
        (ad) =>
          ad.targetType === "HOT_AUCTION" &&
          hasPlacement(ad.adPlacement),
      );

      if (hasHotAuctionAd) {
        await loadHotAuction();
      } else {
        hotAuction.value = null;
      }

      errorMessage.value = "";
    } catch (error) {
      console.error("廣告讀取失敗：", error);

      if (!silent) {
        errorMessage.value = "廣告讀取失敗";
      }
    } finally {
      if (!silent) {
        loading.value = false;
      }
    }
  }

  return {
    ads,
    hotAuction,
    loading,
    errorMessage,
    loadAdvertisements,
  };
}