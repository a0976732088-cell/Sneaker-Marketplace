import { computed, reactive, ref, watch } from "vue";
import {
  createAdvertisement,
  deleteAdvertisement,
  getAdvertisementsByPage,
  getAdvertisementStats,
  getAdvertisementDashboardSummary,
  updateAdvertisement,
  updateAdvertisementStatus,
  uploadAdvertisementImage,
} from "@/service/advertisementService";

export function useAdvertisementManagement() {
  const tabs = ["全部廣告", "已排程", "進行中", "草稿", "暫停／取消", "已結束"];
  const activeTab = ref("全部廣告");
  const keyword = ref("");
  const placement = ref("全部版位");
  const showCreate = ref(false);
  const editingAdId = ref(null);
  const minimumStartAt = ref(null);
  const isStartAtLocked = ref(false);
  const isEditing = computed(() => editingAdId.value !== null);
  const toast = ref("");
  const toastType = ref("success");
  let toastTimer = null;
  const isUploadingImage = ref(false);

  const ads = ref([]);
  const loadingAds = ref(false);
  const loadError = ref("");

  // 四個值屬同一份摘要資料
  const dashboardSummary = reactive({
    activeAdvertisements: 0,
    views: 0,
    clicks: 0,
    ctr: 0,
  });

  // 後端頁碼從 0 開始
  const currentPage = ref(0);
  // 後端回傳的總頁數
  const totalPages = ref(0);
  // 後端回傳的廣告總筆數
  const totalElements = ref(0);
  // 後端目前固定每頁顯示 5 筆
  const pageSize = ref(5);

  const IMAGE_UPLOAD_RULES = {
    BANNER: {
      label: "首頁橫幅",
      width: 2000,
      height: 600,
      minimumWidth: 1200,
      minimumHeight: 360,
      ratio: 10 / 3,
      ratioLabel: "10:3",
    },

    POPUP: {
      label: "首頁彈窗",
      width: 1080,
      height: 1350,
      minimumWidth: 600,
      minimumHeight: 750,
      ratio: 4 / 5,
      ratioLabel: "4:5",
    },

    SMALL_BLOCK: {
      label: "商品頁側欄",
      width: 600,
      height: 600,
      minimumWidth: 300,
      minimumHeight: 300,
      ratio: 1,
      ratioLabel: "1:1",
    },

    CATEGORY_CARD: {
      label: "分類卡片",
      width: 1080,
      height: 1350,
      minimumWidth: 600,
      minimumHeight: 750,
      ratio: 4 / 5,
      ratioLabel: "4:5",
    },
  };

  // Spring Data 的頁碼從 0 開始，畫面顯示時再加 1。
  const pageNumbers = computed(() =>
    Array.from({ length: totalPages.value }, (_, index) => index),
  );

  // 計算頁尾要顯示的資料範圍，例如「顯示 1–5，共 6 筆」。
  const firstItemNumber = computed(() =>
    totalElements.value === 0 ? 0 : currentPage.value * pageSize.value + 1,
  );
  const lastItemNumber = computed(() =>
    Math.min((currentPage.value + 1) * pageSize.value, totalElements.value),
  );

  const typeLabels = {
    HOT_PRODUCT: "熱門商品",
    NEW_PRODUCT: "新品推薦",
    AUCTION: "競標活動",
    DISCOUNT: "折扣優惠",
  };
  const placementLabels = {
    BANNER: "首頁橫幅",
    POPUP: "首頁彈窗",
    SMALL_BLOCK: "商品頁側欄",
    CATEGORY_CARD: "分類卡片",
  };
  const placementApiValues = {
    首頁橫幅: "BANNER",
    首頁彈窗: "POPUP",
    商品頁側欄: "SMALL_BLOCK",
    分類卡片: "CATEGORY_CARD",
  };
  function resolveDisplayStatus(ad) {
    if (ad.adStatus === "DRAFT") return "草稿";
    if (ad.adStatus === "INACTIVE") {
      const now = Date.now();
      const startAt = new Date(ad.startAt).getTime();

      return now < startAt ? "已取消排程" : "已暫停";
    }
    if (ad.adStatus === "EXPIRED") return "已結束";

    if (ad.adStatus === "ACTIVE") {
      const now = Date.now();
      const startAt = new Date(ad.startAt).getTime();
      const endAt = new Date(ad.endAt).getTime();

      if (now < startAt) return "已排程";
      if (now >= endAt) return "已結束";

      return "進行中";
    }

    return ad.adStatus;
  }

  // 將畫面頁籤轉成後端 AdvertisementStatus；全部廣告不帶狀態參數。
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

  const form = reactive({
    // 未來登入功能完成後應由登入者資料取得
    createdBy: 1,
    adName: "",
    imageUrl: "",
    adDescription: "",
    adType: "NEW_PRODUCT",
    adPlacement: "BANNER",
    startAt: "",
    endAt: "",
    targetType: "PRODUCT",
    targetId: null,
    targetUrl: "",
    sortOrder: 0,
  });

  const currentImageRule = computed(() => {
    return IMAGE_UPLOAD_RULES[form.adPlacement];
  });

  const timeError = computed(() => {
    // 尚未同時選擇開始與結束時間時，不顯示錯誤
    if (!form.startAt || !form.endAt) return "";

    const startTime = new Date(form.startAt).getTime();
    const endTime = new Date(form.endAt).getTime();

    return startTime < endTime ? "" : "開始時間必須早於結束時間";
  });

  function getCurrentMinute() {
    const now = new Date();
    now.setSeconds(0, 0);
    return now;
  }

  function validateAdvertisementTime() {
    if (!timeError.value) return true;

    notify(timeError.value, "error");
    return false;
  }

  function handlePlacementChange() {
    form.sortOrder = 0;
    if (!form.imageUrl) return;

    form.imageUrl = "";

    notify("顯示版位已變更，請依照新版位規格重新上傳圖片", "error");
  }

  function validateTargetSelection() {
    if (form.targetType === "CATEGORY" && !form.targetId) {
      notify("請選擇商品分類", "error");
      return false;
    }

    return true;
  }

  function readImageDimensions(file) {
    return new Promise((resolve, reject) => {
      const image = new Image();
      const imageUrl = URL.createObjectURL(file);

      image.onload = () => {
        resolve({
          width: image.naturalWidth,
          height: image.naturalHeight,
        });

        URL.revokeObjectURL(imageUrl);
      };

      image.onerror = () => {
        URL.revokeObjectURL(imageUrl);
        reject(new Error("無法讀取圖片內容"));
      };

      image.src = imageUrl;
    });
  }

  async function handleImageUpload(event) {
    const file = event.target.files?.[0];

    if (!file) return;

    isUploadingImage.value = true;

    try {
      const dimensions = await readImageDimensions(file);

      const rule = currentImageRule.value;

      if (
        dimensions.width < rule.minimumWidth ||
        dimensions.height < rule.minimumHeight
      ) {
        notify(
          `${rule.label}圖片至少需要 ${rule.minimumWidth} × ${rule.minimumHeight} px，目前圖片為 ${dimensions.width} × ${dimensions.height} px`,
          "error",
        );

        return;
      }

      const actualRatio = dimensions.width / dimensions.height;

      const ratioDifference = Math.abs(actualRatio - rule.ratio) / rule.ratio;

      if (ratioDifference > 0.02) {
        notify(
          `${rule.label}圖片比例需要接近 ${rule.ratioLabel}，目前圖片為 ${dimensions.width} × ${dimensions.height} px`,
          "error",
        );

        return;
      }
      const result = await uploadAdvertisementImage(file, form.adPlacement);

      form.imageUrl = result.data;
      notify("圖片上傳成功");
    } catch (error) {
      console.error("圖片上傳失敗", error);

      notify(
        error.response?.data?.message || "圖片上傳失敗，請確認圖片格式與大小",
        "error",
      );
    } finally {
      isUploadingImage.value = false;
      event.target.value = "";
    }
  }

  function resetForm() {
    Object.assign(form, {
      createdBy: 1,
      adName: "",
      imageUrl: "",
      adDescription: "",
      adType: "NEW_PRODUCT",
      adPlacement: "BANNER",
      startAt: "",
      endAt: "",
      targetType: "PRODUCT",
      targetId: null,
      targetUrl: "",
      sortOrder: 0,
    });
  }

  function notify(message, type = "success") {
    toast.value = message;
    toastType.value = type;

    if (toastTimer !== null) clearTimeout(toastTimer);

    toastTimer = window.setTimeout(() => {
      toast.value = "";
      toastTimer = null;
    }, 2400);
  }

  async function toggle(ad) {
    // 畫面顯示「進行中」時，按下按鈕就改成停用；
    // 草稿或已停用時，按下按鈕就改成上架。
    const nextStatus = ad.adStatus === "ACTIVE" ? "INACTIVE" : "ACTIVE";

    const actionName = nextStatus === "ACTIVE" ? "發布/排程" : "停用";

    try {
      // 1. 先讓後端真正更新資料庫狀態。
      const result = await updateAdvertisementStatus(ad.id, nextStatus);

      // 2. 若目前頁面只有最後一筆資料，且不是第一頁，
      //    狀態改變後這一頁可能會變空，因此回前一頁。
      const targetPage =
        ads.value.length === 1 && currentPage.value > 0
          ? currentPage.value - 1
          : currentPage.value;

      // 3. 重新向後端查詢「目前頁籤」對應的資料。
      //    例如現在在草稿頁籤，後端會再查 DRAFT；
      //    剛上架的資料已變 ACTIVE，因此不會再回傳。
      await loadAdvertisements(targetPage, true);

      // 4. 顯示成功訊息。

      notify(`廣告${actionName}成功`);
    } catch (error) {
      console.error(`廣告${actionName}失敗`, error);

      notify(
        error.response?.data?.message ||
          `廣告${actionName}失敗，請確認後端與資料庫`,
        "error",
      );
    }
  }

  function formatDateTime(value) {
    if (!value) return "尚未設定";

    return new Intl.DateTimeFormat("zh-TW", {
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    }).format(new Date(value));
  }

  function toAdvertisementViewModel(ad) {
    return {
      id: ad.adId,
      name: ad.adName,
      desc: ad.adDescription || "",
      imageUrl: ad.imageUrl,
      // 圖片載入失敗時切換為「尚無圖片」提示
      imageLoadFailed: false,

      type: typeLabels[ad.adType] || ad.adType,
      place: placementLabels[ad.adPlacement] || ad.adPlacement,
      status: resolveDisplayStatus(ad),
      adStatus: ad.adStatus,
      adType: ad.adType,
      adPlacement: ad.adPlacement,
      startAt: ad.startAt,
      endAt: ad.endAt,
      targetType: ad.targetType,
      targetId: ad.targetId,
      targetUrl: ad.targetUrl,
      sortOrder: ad.sortOrder,

      period:
        ad.startAt || ad.endAt
          ? `${formatDateTime(ad.startAt)} — ${formatDateTime(ad.endAt)}`
          : "尚未設定",

      // 預設為0
      views: 0,
      clicks: 0,
      ctr: "0.00%",
    };
  }

  function closeAdvertisementDrawer() {
    showCreate.value = false;
    editingAdId.value = null;
    isStartAtLocked.value = false;
    resetForm();
  }

  function openCreateDrawer() {
    toast.value = "";
    resetForm();
    editingAdId.value = null;
    isStartAtLocked.value = false;
    minimumStartAt.value = getCurrentMinute();
    showCreate.value = true;
  }

  function toDatePickerValue(value) {
    if (!value) return "";

    // 例如：2026-09-04T15:30:00 轉成 2026-09-04T15:30
    return value.slice(0, 16);
  }

  function openEditDrawer(ad) {
    toast.value = "";
    editingAdId.value = ad.id;
    minimumStartAt.value = getCurrentMinute();

    const originalStartAt = ad.startAt ? new Date(ad.startAt).getTime() : null;

    isStartAtLocked.value =
      ad.adStatus === "INACTIVE" &&
      originalStartAt !== null &&
      originalStartAt <= Date.now();

    Object.assign(form, {
      createdBy: 1,
      adName: ad.name,
      imageUrl: ad.imageUrl || "",
      adDescription: ad.desc || "",
      adType: ad.adType,
      adPlacement: ad.adPlacement,
      startAt: toDatePickerValue(ad.startAt),
      endAt: toDatePickerValue(ad.endAt),
      targetType: ad.targetType || "PRODUCT",
      targetId: ad.targetId ?? null,
      targetUrl: ad.targetUrl || "",
      sortOrder: ad.sortOrder ?? 0,
    });

    showCreate.value = true;
  }

  async function createDraft() {
    if (!validateAdvertisementTime()) return;
    if (!validateTargetSelection()) return;
    const payload = {
      createdBy: form.createdBy,
      adName: form.adName,
      imageUrl: form.imageUrl,
      adDescription: form.adDescription || null,
      adType: form.adType,
      adPlacement: form.adPlacement,
      startAt: form.startAt || null,
      endAt: form.endAt || null,
      targetType: form.targetType,
      targetId: form.targetType === "URL" ? null : form.targetId || null,
      targetUrl: form.targetType === "URL" ? form.targetUrl || null : null,
      sortOrder: form.sortOrder,
    };

    try {
      const result = await createAdvertisement(payload);

      closeAdvertisementDrawer();
      notify(result.message);
      // 新增資料會依 createdAt 排在最前面，因此回到第一頁。
      await loadAdvertisements(0, true);

      console.log("新增成功");
    } catch (error) {
      console.error("新增失敗");
      notify(error.response?.data?.message || "請確認後端與資料庫", "error");
    }
  }

  async function loadAdvertisements(
    pageNumber = currentPage.value,
    silent = false,
  ) {
    // 一般查詢才顯示載入畫面；背景查詢不顯示
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

      // 記住重新查詢前的廣告畫面狀態
      const previousAds = new Map(ads.value.map((ad) => [ad.id, ad]));

      // 將後端最新資料轉成前端格式
      const refreshedAds = (pageData.content || []).map(
        toAdvertisementViewModel,
      );

      let advertisementStats = [];

      if (refreshedAds.length > 0) {
        const statsResult = await getAdvertisementStats(
          // 目前廣告轉為ID陣列，查詢統計後轉Map
          refreshedAds.map((ad) => ad.id),
        );

        advertisementStats = statsResult.data || [];
      }

      const statsByAdId = new Map(
        advertisementStats.map((stats) => [Number(stats.adId), stats]),
      );

      ads.value = refreshedAds.map((ad) => {
        const previousAd = previousAds.get(ad.id);

        // 圖片網址沒改變時，保留之前的載入失敗結果
        const shouldKeepImageState =
          previousAd && previousAd.imageUrl === ad.imageUrl;

        const stats = statsByAdId.get(Number(ad.id));

        // 統計合併進廣告資料
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

      // 背景更新失敗時保留原本畫面，不顯示整頁錯誤
      if (!silent) {
        loadError.value = error.response?.data?.message || "廣告列表載入失敗";
      }
    } finally {
      if (!silent) {
        loadingAds.value = false;
      }
    }
  }

  async function loadDashboardSummary() {
    try {
      const result = await getAdvertisementDashboardSummary();
      const summary = result.data || {};

      dashboardSummary.activeAdvertisements = summary.activeAdvertisements ?? 0;
      dashboardSummary.views = summary.views ?? 0;
      dashboardSummary.clicks = summary.clicks ?? 0;
      dashboardSummary.ctr = summary.ctr ?? 0;
    } catch (error) {
      console.error("廣告摘要載入失敗", error);
    }
  }

  // 切換頁籤時回到第一頁，並交由後端依狀態篩選後再分頁。
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

  async function goToPage(pageNumber) {
    // 避免超出第一頁、最後一頁，或載入期間重複送出請求。
    if (pageNumber < 0 || pageNumber >= totalPages.value || loadingAds.value) {
      return;
    }

    await loadAdvertisements(pageNumber);
  }

  async function handleDeleteAdvertisement(ad) {
    // 防止不小心按到刪除
    const confirmed = window.confirm(`確定要刪除廣告「${ad.name}」嗎？`);

    if (!confirmed) return;

    try {
      const result = await deleteAdvertisement(ad.id);

      // 若刪除目前頁面最後一筆，回到上一頁，避免停在空白頁面。
      const targetPage =
        ads.value.length === 1 && currentPage.value > 0
          ? currentPage.value - 1
          : currentPage.value;
      await loadAdvertisements(targetPage, true);
      notify(result.message || "廣告刪除成功");
    } catch (error) {
      console.error("刪除廣告失敗", error);
      notify("廣告刪除失敗，請確認後端與資料庫", "error");
    }
  }

  async function updateAd() {
    // 沒有編輯 ID 時不送出修改
    if (editingAdId.value === null) return;
    if (!validateAdvertisementTime()) return;
    if (!validateTargetSelection()) return;

    const payload = {
      adName: form.adName,
      imageUrl: form.imageUrl,
      adDescription: form.adDescription || null,
      adType: form.adType,
      adPlacement: form.adPlacement,
      startAt: form.startAt || null,
      endAt: form.endAt || null,
      targetType: form.targetType,
      targetId: form.targetType === "URL" ? null : form.targetId || null,
      targetUrl: form.targetType === "URL" ? form.targetUrl || null : null,
      sortOrder: form.sortOrder,
    };

    try {
      const result = await updateAdvertisement(editingAdId.value, payload);

      closeAdvertisementDrawer();
      notify(result.message || "廣告修改成功");
      await loadAdvertisements(currentPage.value, true);

      console.log("修改成功");
    } catch (error) {
      console.error("修改失敗", error);
      notify(
        error.response?.data?.message || "廣告修改失敗，請確認輸入資料",
        "error",
      );
    }
  }

  return {
    tabs,
    activeTab,
    keyword,
    placement,
    showCreate,
    toast,
    toastType,
    loadingAds,
    loadError,
    currentPage,
    totalPages,
    totalElements,
    pageNumbers,
    firstItemNumber,
    lastItemNumber,
    filteredAds,
    form,
    currentImageRule,
    isEditing,
    isUploadingImage,
    minimumStartAt,
    isStartAtLocked,
    dashboardSummary,
    loadDashboardSummary,
    handlePlacementChange,
    handleImageUpload,
    toggle,
    closeAdvertisementDrawer,
    openCreateDrawer,
    openEditDrawer,
    createDraft,
    loadAdvertisements,
    goToPage,
    handleDeleteAdvertisement,
    updateAd,
  };
}
