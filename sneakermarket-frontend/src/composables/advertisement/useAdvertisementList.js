// 管理廣告列表、篩選與分頁
import { computed, ref, watch } from "vue";
// 呼叫service
import {
  getAdvertisementsByPage,
  getAdvertisementStats,
} from "@/service/advertisementService";
// 轉換資料
import { toAdvertisementViewModel } from "@/utils/advertisementViewModel";

// 向後端取得摘要-保存摘要-提供給畫面使用
export function useAdvertisementList() {
  const tabs = ["全部廣告", "已排程", "進行中", "草稿", "暫停／取消", "已結束"];

  const activeTab = ref("全部廣告");
  const keyword = ref("");
  const placement = ref("全部版位");

  const ads = ref([]);
  const loadingAds = ref(false);
  const loadError = ref("");

  const currentPage = ref(0);
  const totalPages = ref(0);
  const totalElements = ref(0);
  const pageSize = ref(5);

  const placementApiValues = {
    橫幅: "BANNER",
    彈窗: "POPUP",
    側欄: "SMALL_BLOCK",
    分類: "CATEGORY_CARD",
  };

  const tabStatusValues = {
    草稿: "DRAFT",
    "暫停／取消": "INACTIVE",
    已結束: "EXPIRED",
  };

  const tabDeliveryStatusValues = {
    已排程: "SCHEDULED",
    進行中: "RUNNING",
  };

  const selectedAdStatus = computed(
    () => tabStatusValues[activeTab.value] || null,
  );

  const selectedDeliveryStatus = computed(
    () => tabDeliveryStatusValues[activeTab.value] || null,
  );

  const selectedAdPlacement = computed(
    () => placementApiValues[placement.value] || null,
  );

  const filteredAds = computed(() => ads.value);

  const pageNumbers = computed(() =>
    Array.from({ length: totalPages.value }, (_, index) => index),
  );

  const firstItemNumber = computed(() =>
    totalElements.value === 0 ? 0 : currentPage.value * pageSize.value + 1,
  );

  const lastItemNumber = computed(() =>
    Math.min((currentPage.value + 1) * pageSize.value, totalElements.value),
  );

  async function loadAdvertisements(
    pageNumber = currentPage.value,
    silent = false,
  ) {
    if (!silent) {
      loadingAds.value = true;
      loadError.value = "";
    }

    try {
      const result = await getAdvertisementsByPage(
        pageNumber,
        selectedAdStatus.value,
        selectedDeliveryStatus.value,
        selectedAdPlacement.value,
        keyword.value.trim() || null,
      );

      const pageData = result.data;

      const previousAds = new Map(ads.value.map((ad) => [ad.id, ad]));

      const refreshedAds = (pageData.content || []).map(
        toAdvertisementViewModel,
      );

      let advertisementStats = [];

      if (refreshedAds.length > 0) {
        const statsResult = await getAdvertisementStats(
          refreshedAds.map((ad) => ad.id),
        );

        advertisementStats = statsResult.data || [];
      }

      const statsByAdId = new Map(
        advertisementStats.map((stats) => [Number(stats.adId), stats]),
      );

      ads.value = refreshedAds.map((ad) => {
        const previousAd = previousAds.get(ad.id);

        const shouldKeepImageState =
          previousAd && previousAd.imageUrl === ad.imageUrl;

        const stats = statsByAdId.get(Number(ad.id));

        return {
          ...ad,
          views: stats?.views ?? 0,
          clicks: stats?.clicks ?? 0,
          ctr: `${Number(stats?.ctr ?? 0).toFixed(2)}%`,
          imageLoadFailed: shouldKeepImageState
            ? previousAd.imageLoadFailed
            : false,
        };
      });

      currentPage.value = pageData.number;
      totalPages.value = pageData.totalPages;
      totalElements.value = pageData.totalElements;
      pageSize.value = pageData.size;
    } catch (error) {
      console.error("分頁查詢廣告失敗", error);

      if (!silent) {
        loadError.value = error.response?.data?.message || "廣告列表載入失敗";
      }
    } finally {
      if (!silent) {
        loadingAds.value = false;
      }
    }
  }

  async function goToPage(pageNumber) {
    if (pageNumber < 0 || pageNumber >= totalPages.value || loadingAds.value) {
      return;
    }

    await loadAdvertisements(pageNumber);
  }

  watch(activeTab, () => {
    loadAdvertisements(0);
  });

  watch(placement, () => {
    loadAdvertisements(0);
  });

  watch(keyword, (_, __, onCleanup) => {
    const timer = window.setTimeout(() => {
      loadAdvertisements(0, true);
    }, 300);

    onCleanup(() => {
      window.clearTimeout(timer);
    });
  });

  return {
    tabs,
    activeTab,
    keyword,
    placement,
    ads,
    loadingAds,
    loadError,
    currentPage,
    totalPages,
    totalElements,
    pageSize,
    pageNumbers,
    firstItemNumber,
    lastItemNumber,
    filteredAds,
    loadAdvertisements,
    goToPage,
  };
}
