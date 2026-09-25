import { computed, reactive, ref } from "vue";
import { uploadAdvertisementImage } from "@/service/advertisementService";

export function useAdvertisementForm(notify) {
  const IMAGE_UPLOAD_RULES = {
    BANNER: {
      label: "首頁橫幅",
      // 建議使用與一般商城相同的圖片尺寸
      width: 2014,
      height: 781,
      // 最低寬度仍維持 1200，高度依新比例換算
      minimumWidth: 1200,
      minimumHeight: 465,
      // 與一般商城 Hero Banner 相同比例
      ratio: 2014 / 781,
      ratioLabel: "2014:781（約 2.58:1）",
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
      height: 1200,
      minimumWidth: 300,
      minimumHeight: 600,
      ratio: 1 / 2,
      ratioLabel: "1:2",
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

  const isUploadingImage = ref(false);

  const form = reactive({
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

  const currentImageRule = computed(() => IMAGE_UPLOAD_RULES[form.adPlacement]);

  const timeError = computed(() => {
    if (!form.startAt || !form.endAt) return "";

    const startTime = new Date(form.startAt).getTime();
    const endTime = new Date(form.endAt).getTime();

    return startTime < endTime ? "" : "開始時間必須早於結束時間";
  });

  function validateAdvertisementTime() {
    if (!timeError.value) return true;

    notify(timeError.value, "error");
    return false;
  }

  function validateTargetSelection() {
    if (form.targetType === "CATEGORY" && !form.targetId) {
      notify("請選擇商品分類", "error");
      return false;
    }

    return true;
  }

  function handlePlacementChange() {
    form.sortOrder = 0;

    if (!form.imageUrl) return;

    form.imageUrl = "";

    notify("顯示版位已變更，請依照新版位規格重新上傳圖片", "error");
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
        // release RAM
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
    const input = event.target;
    const file = input.files?.[0];

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
      input.value = "";
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

  return {
    form,
    currentImageRule,
    isUploadingImage,
    handleImageUpload,
    validateAdvertisementTime,
    validateTargetSelection,
    handlePlacementChange,
    resetForm,
  };
}
