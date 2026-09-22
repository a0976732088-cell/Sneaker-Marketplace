import { computed, ref } from "vue";
import {
  createAdvertisement,
  deleteAdvertisement,
  updateAdvertisement,
  updateAdvertisementStatus,
} from "@/service/advertisementService";
import { useAdvertisementDashboard } from "@/composables/advertisement/useAdvertisementDashboard";
import { useAdvertisementList } from "@/composables/advertisement/useAdvertisementList";
import { useAdvertisementForm } from "@/composables/advertisement/useAdvertisementForm";

export function useAdvertisementManagement() {
  const showCreate = ref(false);
  const editingAdId = ref(null);
  const minimumStartAt = ref(null);
  const isStartAtLocked = ref(false);
  const isEditing = computed(() => editingAdId.value !== null);
  const toast = ref("");
  const toastType = ref("success");
  let toastTimer = null;

  const { dashboardSummary, loadDashboardSummary } =
    useAdvertisementDashboard();

  const {
    tabs,
    activeTab,
    keyword,
    placement,
    ads,
    loadingAds,
    loadError,
    currentPage, // 後端頁碼從 0 開始
    totalPages, // 後端回傳的總頁數
    totalElements, // 後端回傳的廣告總筆數
    pageNumbers, // Spring Data 的頁碼從 0 開始，畫面顯示時再加 1。
    firstItemNumber, // 計算頁尾要顯示的資料範圍，例如「顯示 1–5，共 6 筆」。
    lastItemNumber,
    filteredAds,
    loadAdvertisements,
    goToPage,
  } = useAdvertisementList();

  function getCurrentMinute() {
    const now = new Date();
    now.setSeconds(0, 0);
    return now;
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

  const {
    form,
    currentImageRule,
    isUploadingImage,
    handleImageUpload,
    validateAdvertisementTime,
    validateTargetSelection,
    handlePlacementChange,
    resetForm,
  } = useAdvertisementForm(notify);

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
    loadAdvertisements,
    goToPage,
    toggle,
    closeAdvertisementDrawer,
    openCreateDrawer,
    openEditDrawer,
    createDraft,
    handleDeleteAdvertisement,
    updateAd,
  };
}
