import { computed, reactive, ref } from "vue";
import {
  createAdvertisement,
  deleteAdvertisement,
  getAdvertisementsByPage,
  updateAdvertisement,
} from "@/service/advertisementService";

export function useAdvertisementManagement() {
  const tabs = ["全部廣告", "進行中", "草稿", "已停用", "已結束"];
  const activeTab = ref("全部廣告");
  const keyword = ref("");
  const placement = ref("全部版位");
  const showCreate = ref(false);
  const editingAdId = ref(null);
  const isEditing = computed(() => editingAdId.value !== null);
  const toast = ref("");

  const ads = ref([]);
  const loadingAds = ref(false);
  const loadError = ref("");

  // 後端頁碼從 0 開始
  const currentPage = ref(0);
  // 後端回傳的總頁數
  const totalPages = ref(0);
  // 後端回傳的廣告總筆數
  const totalElements = ref(0);
  // 後端目前固定每頁顯示 5 筆
  const pageSize = ref(5);

  // Spring Data 的頁碼從 0 開始，畫面顯示時再加 1。
  const pageNumbers = computed(() =>
    Array.from({ length: totalPages.value }, (_, index) => index),
  );

  // 計算頁尾要顯示的資料範圍，例如「顯示 1–5，共 6 筆」。
  const firstItemNumber = computed(() =>
    totalElements.value === 0
      ? 0
      : currentPage.value * pageSize.value + 1,
  );
  const lastItemNumber = computed(() =>
    Math.min(
      (currentPage.value + 1) * pageSize.value,
      totalElements.value,
    ),
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
  };
  const statusLabels = {
    DRAFT: "草稿",
    ACTIVE: "進行中",
    INACTIVE: "已停用",
    EXPIRED: "已結束",
  };

  const filteredAds = computed(() =>
    ads.value.filter((ad) => {
      const term = keyword.value.trim().toLowerCase();
      return (
        (activeTab.value === "全部廣告" || ad.status === activeTab.value) &&
        (placement.value === "全部版位" || ad.place === placement.value) &&
        (!term || `${ad.name} ${ad.id} ${ad.desc}`.toLowerCase().includes(term))
      );
    }),
  );

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
    targetType: "URL",
    targetId: null,
    targetUrl: "",
    sortOrder: 0,
  });

  const timeError = computed(() => {
  // 尚未同時選擇開始與結束時間時，不顯示錯誤
  if (!form.startAt || !form.endAt) return "";

  const startTime = new Date(form.startAt).getTime();
  const endTime = new Date(form.endAt).getTime();

  return startTime < endTime
    ? ""
    : "開始時間必須早於結束時間";
});

function validateAdvertisementTime() {
  if (!timeError.value) return true;

  notify(timeError.value);
  return false;
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
      targetType: "URL",
      targetId: null,
      targetUrl: "",
      sortOrder: 0,
    });
  }

  function notify(message) {
    let timer = null;
    toast.value = message;

    if (timer) clearTimeout(timer);

    timer = window.setTimeout(() => {
      toast.value = "";
    }, 2400);
  }

  function toggle(ad) {
    ad.status = ad.status === "進行中" ? "草稿" : "進行中";
    notify(
      `「${ad.name}」已${ad.status === "進行中" ? "開始投放" : "暫停投放"}`,
    );
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
      status: statusLabels[ad.adStatus] || ad.adStatus,

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

      // 後端目前沒有成效統計欄位
      views: "—",
      clicks: "—",
      ctr: "—",
      color: "blue",
    };
  }

  function closeAdvertisementDrawer() {
    showCreate.value = false;
    editingAdId.value = null;
    resetForm();
  }

  function openCreateDrawer() {
    resetForm();
    editingAdId.value = null;
    showCreate.value = true;
  }

  function toDatePickerValue(value) {
    if (!value) return "";

    // 例如：2026-09-04T15:30:00 轉成 2026-09-04T15:30
    return value.slice(0, 16);
  }

  function openEditDrawer(ad) {
    editingAdId.value = ad.id;

    Object.assign(form, {
      createdBy: 1,
      adName: ad.name,
      imageUrl: ad.imageUrl || "",
      adDescription: ad.desc || "",
      adType: ad.adType,
      adPlacement: ad.adPlacement,
      startAt: toDatePickerValue(ad.startAt),
      endAt: toDatePickerValue(ad.endAt),
      targetType: ad.targetType || "URL",
      targetId: ad.targetId ?? null,
      targetUrl: ad.targetUrl || "",
      sortOrder: ad.sortOrder ?? 0,
    });

    showCreate.value = true;
  }

  async function createDraft() {
      if (!validateAdvertisementTime()) return;
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
      await loadAdvertisements(0);

      console.log("新增成功");
    } catch (error) {
      console.error("新增失敗");
      notify(error.response?.data?.message || "請確認後端與資料庫");
    }
  }

  async function loadAdvertisements(pageNumber = currentPage.value) {
    loadingAds.value = true;
    loadError.value = "";

    try {
      const result = await getAdvertisementsByPage(pageNumber);

      const pageData = result.data;

      ads.value = (pageData.content || []).map(toAdvertisementViewModel);
      currentPage.value = pageData.number;
      totalPages.value = pageData.totalPages;
      totalElements.value = pageData.totalElements;
      pageSize.value = pageData.size;
    } catch (error) {
      console.error("分頁查詢廣告失敗", error);
      loadError.value =
        error.response?.data?.message || "廣告列表載入失敗";
    } finally {
      loadingAds.value = false;
    }
  }

  async function goToPage(pageNumber) {
    // 避免超出第一頁、最後一頁，或載入期間重複送出請求。
    if (
      pageNumber < 0 ||
      pageNumber >= totalPages.value ||
      loadingAds.value
    ) {
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
      await loadAdvertisements(targetPage);
      notify(result.message || "廣告刪除成功");
    } catch (error) {
      console.error("刪除廣告失敗", error);
      notify("廣告刪除失敗，請確認後端與資料庫");
    }
  }

  async function updateAd() {
    // 沒有編輯 ID 時不送出修改
    if (editingAdId.value === null) return;

      if (!validateAdvertisementTime()) return;


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
      await loadAdvertisements();

      console.log("修改成功");
    } catch (error) {
      console.error("修改失敗", error);
      notify(error.response?.data?.message || "廣告修改失敗，請確認輸入資料");
    }
  }

  return {
    tabs,
    activeTab,
    keyword,
    placement,
    showCreate,
    toast,
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
    form,
    timeError,
    isEditing,
    notify,
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
