<template>
  <div class="container py-4">
    <h1 class="mb-4">編輯競標</h1>

    <!-- 資料載入中 -->
    <div v-if="loading">載入中...</div>

    <!-- API / 表單錯誤 -->
    <div
      v-else-if="errorMessage"
      class="alert alert-danger global-error-alert"
      role="alert"
    >
      {{ errorMessage }}
    </div>

    <template v-else>
      <!-- 目前競標狀態提示 -->
      <div class="alert alert-info">
        目前狀態：{{ formatStatus(auctionStatus) }} ／ 出價次數：{{ bidCount }}
      </div>

      <!-- 不允許編輯 -->
      <div v-if="!editable" class="alert alert-warning">
        {{ editableMessage }}
      </div>

      <form novalidate @submit.prevent="submitUpdate">
        <p class="required-note">
          <span class="required-mark" aria-hidden="true">*</span>
          為必填欄位
        </p>

        <!-- 商品基本資料 -->
        <div class="card mb-4">
          <div class="card-header">商品基本資料</div>

          <div class="card-body">
            <div class="row g-3">
              <AuctionEditProductSection
                v-model:selected-model-option="selectedModelOption"
                :form="form"
                :brands="brands"
                :categories="categories"
                :editable="editable"
                :auction-status="auctionStatus"
                :available-model-options="availableModelOptions"
                :field-errors="fieldErrors"
                @brand-change="handleBrandChange"
                @model-option-change="handleModelOptionChange"
                @revalidate-field="revalidateField"
              />
              <AuctionEditDescriptionSection
                :form="form"
                :editable="editable"
                :field-errors="fieldErrors"
                :selected-description-option="selectedDescriptionOption"
                :selected-form-options="selectedFormOptions"
                :legacy-location-option="legacyLocationOption"
                @apply-description-template="applyDescriptionTemplate"
                @start-custom-description="startCustomDescription"
                @description-input="handleDescriptionInput"
                @form-option-change="handleFormOptionChange"
                @revalidate-field="revalidateField"
              />
            </div>
          </div>
        </div>

        <AuctionEditImageSection
          :image-urls="currentImageUrls"
          :product-name="form.productName"
        />

        <AuctionEditTransactionSection
          v-model:delivery-option="deliveryOption"
          :form="form"
          :editable="editable"
          :field-errors="fieldErrors"
          :selected-form-options="selectedFormOptions"
          :legacy-shipping-method-option="legacyShippingMethodOption"
          :legacy-payment-method-option="legacyPaymentMethodOption"
          @form-option-change="handleFormOptionChange"
          @revalidate-field="revalidateField"
        />

        <AuctionEditSettingsSection
          :form="form"
          :editable="editable"
          :auction-status="auctionStatus"
          :field-errors="fieldErrors"
          @revalidate-field="revalidateField"
        />

        <!-- 操作 -->
        <div class="d-flex gap-2">
          <button
            type="submit"
            class="btn btn-primary"
            :disabled="submitting || !editable"
          >
            {{ submitting ? "儲存中..." : "儲存修改" }}
          </button>

          <button
            type="button"
            class="btn btn-outline-secondary"
            :disabled="submitting"
            @click="goBackToDetail"
          >
            返回競標詳細頁
          </button>
        </div>
      </form>
    </template>
  </div>
</template>

<script setup>
import AuctionEditProductSection from "@/components/auction/edit/AuctionEditProductSection.vue";
import AuctionEditDescriptionSection from "@/components/auction/edit/AuctionEditDescriptionSection.vue";
import AuctionEditImageSection from "@/components/auction/edit/AuctionEditImageSection.vue";
import AuctionEditTransactionSection from "@/components/auction/edit/AuctionEditTransactionSection.vue";
import AuctionEditSettingsSection from "@/components/auction/edit/AuctionEditSettingsSection.vue";
import {
  AUCTION_AUTHENTICITY_NOTE_OPTIONS,
  AUCTION_DESCRIPTION_OPTIONS,
  AUCTION_DEFECT_NOTE_OPTIONS,
  AUCTION_MODEL_OPTIONS_BY_BRAND,
  AUCTION_PAYMENT_METHOD_OPTIONS,
  AUCTION_SHIPPING_METHOD_OPTIONS,
  AUCTION_SHOE_SIZES,
  AUCTION_WARRANTY_NOTE_OPTIONS,
  CUSTOM_OPTION_VALUE,
  DELIVERY_OPTION_VALUES,
  TAIWAN_CITIES,
} from "@/constants/auctionFormOptions";
import { parseAuctionTime } from "@/utils/auctionTime";
import {
  getAuctionOptions,
  requireAuctionSession,
  handleAuctionAccessError,
} from "@/service/auctionService";
import { computed, nextTick, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import { getAuctionDetail, updateAuction } from "@/service/auctionService";

// Router
const route = useRoute();
const router = useRouter();

// 資料載入狀態
const loading = ref(true);
const canManage = ref(false);

// 是否正在送出修改 Request
const submitting = ref(false);

// 頁面錯誤
const errorMessage = ref("");

// 目前競標狀態
const auctionStatus = ref("");

// 目前成功出價次數
const bidCount = ref(0);

// 目前瀏覽會員是否為此競標的賣家本人
const isOwner = ref(false);

/*
 * 保存後端原始 startAt。
 *
 * ACTIVE 競標的 startAt 不允許修改，
 * 因此送 PUT 時要使用後端原始值，
 * 不依賴 datetime-local 轉換後的字串。
 */
const originalStartAt = ref("");
const originalLocation = ref("");

const brands = ref([]);
const categories = ref([]);
const currentImageUrls = ref([]);

// 編輯表單
const form = reactive({
  brandId: "",
  categoryId: "",
  productName: "",
  model: "",
  size: "",
  condition: "",
  description: "",
  defectNote: "",
  authenticityNote: "",
  location: "",

  shippingMethod: "",
  shippingFee: 0,
  paymentMethod: "",
  allowMeetup: false,
  allowReturn: false,
  warrantyNote: "",

  startPrice: null,
  minIncrement: null,
  startAt: "",
  endAt: "",
});

const selectedModelOption = ref("");
const selectedDescriptionOption = ref("");

const selectedBrandName = computed(() => {
  const selectedBrand = brands.value.find(
    (brand) => String(brand.id) === String(form.brandId),
  );
  return selectedBrand?.name ?? "";
});

const availableModelOptions = computed(() => {
  const brandName = selectedBrandName.value.trim().toLowerCase();
  if (brandName.includes("jordan")) return AUCTION_MODEL_OPTIONS_BY_BRAND.Jordan;
  if (brandName.includes("nike")) return AUCTION_MODEL_OPTIONS_BY_BRAND.Nike;
  if (brandName.includes("adidas")) return AUCTION_MODEL_OPTIONS_BY_BRAND.adidas;
  if (brandName.replace(/[\s-]+/g, "").includes("newbalance")) {
    return AUCTION_MODEL_OPTIONS_BY_BRAND["New Balance"];
  }
  if (brandName.includes("vans")) return AUCTION_MODEL_OPTIONS_BY_BRAND.Vans;
  return [];
});

const legacyLocationOption = computed(() => {
  const value = originalLocation.value;

  return !isBlank(value) && !TAIWAN_CITIES.includes(value)
    ? value
    : "";
});

// 純 UI selector state；不屬於 AuctionUpdateRequest。
const selectedFormOptions = reactive({
  defectNote: "",
  authenticityNote: "",
  shippingMethod: "",
  paymentMethod: "",
  warrantyNote: "",
});

const formOptionPresets = Object.freeze({
  defectNote: AUCTION_DEFECT_NOTE_OPTIONS,
  authenticityNote: AUCTION_AUTHENTICITY_NOTE_OPTIONS,
  shippingMethod: AUCTION_SHIPPING_METHOD_OPTIONS,
  paymentMethod: AUCTION_PAYMENT_METHOD_OPTIONS,
  warrantyNote: AUCTION_WARRANTY_NOTE_OPTIONS,
});

const FIXED_FORM_OPTION_FIELDS = new Set([
  "shippingMethod",
  "paymentMethod",
]);

function getLegacyFormOption(fieldName) {
  const value = form[fieldName];
  return !isBlank(value) && !formOptionPresets[fieldName].includes(value)
    ? value
    : "";
}

const legacyShippingMethodOption = computed(() =>
  getLegacyFormOption("shippingMethod"),
);
const legacyPaymentMethodOption = computed(() =>
  getLegacyFormOption("paymentMethod"),
);

// 交付選項只控制既有 allowMeetup Boolean，不會新增 request 欄位。
const deliveryOption = computed({
  get() {
    return form.allowMeetup
      ? DELIVERY_OPTION_VALUES.SHIPPING_OR_MEETUP
      : DELIVERY_OPTION_VALUES.SHIPPING_ONLY;
  },
  set(value) {
    form.allowMeetup =
      value === DELIVERY_OPTION_VALUES.SHIPPING_OR_MEETUP;
  },
});

// 僅供前端欄位錯誤呈現，不會放進 update request。
const fieldErrors = reactive({
  brandId: "",
  categoryId: "",
  productName: "",
  size: "",
  condition: "",
  description: "",
  location: "",
  shippingMethod: "",
  shippingFee: "",
  paymentMethod: "",
  startPrice: "",
  minIncrement: "",
  startAt: "",
  endAt: "",
});

// 固定依畫面由上而下的順序決定第一個要捲動的錯誤。
const validationOrder = [
  "brandId",
  "categoryId",
  "productName",
  "size",
  "condition",
  "description",
  "location",
  "shippingMethod",
  "shippingFee",
  "paymentMethod",
  "startPrice",
  "minIncrement",
  "startAt",
  "endAt",
];

/**
 * 判斷目前畫面是否允許操作編輯表單。
 *
 * 前端只負責 UX；
 * 真正權限仍由後端 updateAuction()
 * 在 Transaction 中重新驗證。
 */
const editable = computed(() => {
  // 不是刊登者本人，不提供編輯操作
  if (!isOwner.value || !canManage.value) {
    return false;
  }

  // 已經有人成功出價，不可修改
  if (bidCount.value > 0) {
    return false;
  }

  // 目前只允許 SCHEDULED / ACTIVE 編輯
  return (
    auctionStatus.value === "SCHEDULED" || auctionStatus.value === "ACTIVE"
  );
});

/**
 * 不允許編輯時顯示的原因。
 */
const editableMessage = computed(() => {
  if (!isOwner.value) {
    return "你無權修改此競標";
  }

  if (bidCount.value > 0) {
    return "此競標已有出價紀錄，無法修改";
  }

  if (
    ["SOLD", "UNSOLD", "CANCELLED", "REMOVED"].includes(auctionStatus.value)
  ) {
    return "目前競標狀態不允許修改";
  }

  return "目前競標無法修改";
});

/**
 * 將後端 LocalDateTime 轉成 datetime-local 可使用的格式。
 *
 * 例如：
 * 2026-09-07T10:00:00
 * →
 * 2026-09-07T10:00
 */
function toDateTimeLocal(value) {
  if (!value) {
    return "";
  }

  return value.slice(0, 16);
}

function initializeModelOption(value) {
  form.model = value ?? "";

  if (isBlank(form.model)) {
    selectedModelOption.value = "";
    return;
  }

  selectedModelOption.value = availableModelOptions.value.includes(form.model)
    ? form.model
    : CUSTOM_OPTION_VALUE;
}

function handleBrandChange() {
  selectedModelOption.value = "";
  form.model = "";
  revalidateField("brandId");
}

function handleModelOptionChange() {
  form.model =
    selectedModelOption.value === CUSTOM_OPTION_VALUE
      ? ""
      : selectedModelOption.value;
}

function initializeDescriptionOption(value) {
  form.description = value ?? "";
  selectedDescriptionOption.value = AUCTION_DESCRIPTION_OPTIONS.includes(
    form.description,
  )
    ? form.description
    : isBlank(form.description)
      ? ""
      : CUSTOM_OPTION_VALUE;
}

function applyDescriptionTemplate(option) {
  if (!AUCTION_DESCRIPTION_OPTIONS.includes(option)) return;

  selectedDescriptionOption.value = option;
  form.description = option;
  revalidateField("description");
}

async function startCustomDescription() {
  selectedDescriptionOption.value = CUSTOM_OPTION_VALUE;
  await nextTick();

  if (typeof document !== "undefined") {
    document.getElementById?.("auction-edit-description")?.focus();
  }
}

function handleDescriptionInput() {
  if (selectedDescriptionOption.value !== form.description) {
    selectedDescriptionOption.value = CUSTOM_OPTION_VALUE;
  }

  revalidateField("description");
}

/**
 * 依既有後端字串初始化 UI selector。
 *
 * 可自訂欄位展開 Custom input；固定欄位則用動態 legacy option 保留舊值；
 * UI sentinel 永遠不會寫進 form。
 */
function initializeFormOption(fieldName, value) {
  if (value === CUSTOM_OPTION_VALUE) {
    selectedFormOptions[fieldName] = CUSTOM_OPTION_VALUE;
    form[fieldName] = "";
    return;
  }

  const formValue = value ?? "";
  form[fieldName] = formValue;

  if (isBlank(formValue)) {
    selectedFormOptions[fieldName] = "";
    return;
  }

  if (FIXED_FORM_OPTION_FIELDS.has(fieldName)) {
    selectedFormOptions[fieldName] = formValue;
    return;
  }

  selectedFormOptions[fieldName] = formOptionPresets[fieldName].includes(
    formValue,
  )
    ? formValue
    : CUSTOM_OPTION_VALUE;
}

/**
 * Select 只是 UI controller；form 永遠只保存 Backend 真正接收的字串。
 */
function handleFormOptionChange(fieldName) {
  const selectedValue = selectedFormOptions[fieldName];

  form[fieldName] =
    selectedValue === CUSTOM_OPTION_VALUE ? "" : selectedValue;
  revalidateField(fieldName);
}

/**
 * 防止保留給 UI controller 的 sentinel 被當成使用者內容送出。
 */
function clearReservedFormOptionValues() {
  for (const fieldName of Object.keys(selectedFormOptions)) {
    if (form[fieldName] === CUSTOM_OPTION_VALUE) {
      form[fieldName] = "";
    }
  }
}

/**
 * 將詳細資料填入編輯表單。
 */
function fillForm(auction) {
  form.brandId = auction.brandId;
  form.categoryId = auction.categoryId;
  form.productName = auction.productName;
  initializeModelOption(auction.model);
  form.size = auction.size;
  form.condition = auction.condition;
  initializeDescriptionOption(auction.description);
  initializeFormOption("defectNote", auction.defectNote);
  initializeFormOption("authenticityNote", auction.authenticityNote);
  originalLocation.value = auction.location ?? "";
  form.location = originalLocation.value;

  initializeFormOption("shippingMethod", auction.shippingMethod);
  form.shippingFee = auction.shippingFee;
  initializeFormOption("paymentMethod", auction.paymentMethod);
  form.allowMeetup = auction.allowMeetup === true;
  form.allowReturn = auction.allowReturn;
  initializeFormOption("warrantyNote", auction.warrantyNote);

  currentImageUrls.value = Array.isArray(auction.imageUrls)
    ? [...auction.imageUrls]
    : [];

  form.startPrice = auction.startPrice;
  form.minIncrement = auction.minIncrement;
  form.startAt = toDateTimeLocal(auction.startAt);
  form.endAt = toDateTimeLocal(auction.endAt);

  canManage.value = auction.canManage;
  auctionStatus.value =
    auction.hasStarted && auction.status === "SCHEDULED"
      ? "ACTIVE"
      : auction.status;
  bidCount.value = auction.bidCount;
  isOwner.value = auction.isOwner;

  // 保存未轉換過的後端原始開始時間
  originalStartAt.value = auction.startAt;
}

/**
 * 讀取目前競標資料。
 *
 * 非賣家本人不可進入競標編輯畫面，
 * 取得詳細資料確認 isOwner=false 後，
 * 直接導向共用 403 權限不足頁。
 */
async function loadAuction() {
  loading.value = true;
  errorMessage.value = "";

  try {
    const auctionId = route.params.id;

    await requireAuctionSession();
    const [response, optionsResponse] = await Promise.all([
      getAuctionDetail(auctionId),
      getAuctionOptions(),
    ]);
    brands.value = optionsResponse.data.data.brands;
    categories.value = optionsResponse.data.data.categories;

    const auction = response.data.data;

    /*
     * 編輯頁屬於賣家本人操作區。
     *
     * 非本人即使直接輸入 /auctions/{id}/edit，
     * 也不顯示編輯表單，而是導向權限不足頁。
     *
     * 後端 PUT API 仍會再次驗證 sellerId，
     * 這裡只是前端的頁面存取控制。
     */
    if (!auction.isOwner) {
      await router.replace({
        name: "forbidden",
      });

      return;
    }

    fillForm(auction);
  } catch (error) {
    if (await handleAuctionAccessError(error, router)) return;
    console.error("取得競標編輯資料失敗：", error);

    if (error.response?.status === 404) {
      errorMessage.value = "找不到指定的競標資料";
    } else {
      errorMessage.value = "無法取得競標資料";
    }
  } finally {
    loading.value = false;
  }
}

function isBlank(value) {
  return value === null || value === undefined || String(value).trim() === "";
}

function isIntegerAtLeast(value, minimum) {
  if (isBlank(value)) {
    return false;
  }

  const numberValue = Number(value);

  return (
    Number.isFinite(numberValue) &&
    Number.isInteger(numberValue) &&
    numberValue >= minimum
  );
}

function getEffectiveStartAtValue() {
  return auctionStatus.value === "ACTIVE"
    ? originalStartAt.value
    : form.startAt;
}

/**
 * 回傳單一欄位的前端輸入錯誤。
 *
 * 這些規則只重現既有 required / min / step 與時間檢查；
 * 後端仍是最終商業規則判定者。
 */
function getFieldError(fieldName) {
  switch (fieldName) {
    case "brandId":
      return isBlank(form.brandId) ? "請選擇品牌" : "";
    case "categoryId":
      return isBlank(form.categoryId) ? "請選擇分類" : "";
    case "productName":
      return isBlank(form.productName) ? "請輸入商品名稱" : "";
    case "size":
      return AUCTION_SHOE_SIZES.includes(form.size) ? "" : "請選擇尺寸";
    case "condition":
      return isBlank(form.condition) ? "請選擇商品狀況" : "";
    case "description":
      return isBlank(form.description) ? "請輸入商品介紹" : "";
    case "location":
      return isBlank(form.location) ||
        TAIWAN_CITIES.includes(form.location) ||
        form.location === legacyLocationOption.value
        ? ""
        : "請從台灣縣市選項中選擇商品所在地";
    case "shippingMethod":
      return isBlank(form.shippingMethod) ? "請選擇運送方式" : "";
    case "shippingFee":
      if (isBlank(form.shippingFee)) {
        return "請輸入運費";
      }

      return isIntegerAtLeast(form.shippingFee, 0)
        ? ""
        : "運費必須是 0 以上的整數";
    case "paymentMethod":
      return isBlank(form.paymentMethod) ? "請選擇付款方式" : "";
    case "startPrice":
      if (isBlank(form.startPrice)) {
        return "請輸入起標價";
      }

      return isIntegerAtLeast(form.startPrice, 1)
        ? ""
        : "起標價必須是 1 以上的整數";
    case "minIncrement":
      if (isBlank(form.minIncrement)) {
        return "請輸入最低加價金額";
      }

      return isIntegerAtLeast(form.minIncrement, 1)
        ? ""
        : "最低加價金額必須是 1 以上的整數";
    case "startAt": {
      const startAtValue = getEffectiveStartAtValue();

      if (isBlank(startAtValue)) {
        return "請設定競標開始時間";
      }

      return Number.isFinite(parseAuctionTime(startAtValue).getTime())
        ? ""
        : "請輸入有效的競標開始時間";
    }
    case "endAt": {
      if (isBlank(form.endAt)) {
        return "請設定競標結束時間";
      }

      const startAt = parseAuctionTime(getEffectiveStartAtValue());
      const endAt = parseAuctionTime(form.endAt);

      if (!Number.isFinite(endAt.getTime())) {
        return "請輸入有效的競標結束時間";
      }

      if (!Number.isFinite(startAt.getTime())) {
        return "";
      }

      if (endAt <= startAt) {
        return "競標結束時間必須晚於開始時間";
      }

      if (endAt <= new Date()) {
        return "競標結束時間必須晚於目前時間";
      }

      return "";
    }
    default:
      return "";
  }
}

/**
 * 已顯示錯誤的欄位在使用者修改後立即重新驗證。
 */
function revalidateField(fieldName) {
  if (fieldErrors[fieldName]) {
    fieldErrors[fieldName] = getFieldError(fieldName);
  }

  // 開始時間改變時，既有的時間先後錯誤也要同步重新判斷。
  if (fieldName === "startAt" && fieldErrors.endAt) {
    fieldErrors.endAt = getFieldError("endAt");
  }
}


/**
 * 一次顯示全部欄位錯誤，回傳表單是否有效。
 */
function validateForm() {
  clearReservedFormOptionValues();

  validationOrder.forEach((fieldName) => {
    fieldErrors[fieldName] = getFieldError(fieldName);
  });

  return validationOrder.every((fieldName) => !fieldErrors[fieldName]);
}

/**
 * 依畫面順序將第一個錯誤移到視窗中央，並聚焦可操作的控制項。
 */
async function focusFirstValidationError() {
  const firstFieldName = validationOrder.find(
    (fieldName) => fieldErrors[fieldName],
  );

  if (!firstFieldName) {
    return;
  }

  await nextTick();

  if (typeof document === "undefined") {
    return;
  }

  const fieldContainer = document.querySelector(
    `[data-validation-field="${firstFieldName}"]`,
  );

  if (!fieldContainer) {
    return;
  }

  fieldContainer.scrollIntoView({
    behavior: "smooth",
    block: "center",
  });

  const customInput = fieldContainer.querySelector("[data-custom-input]");
  const focusableField =
    customInput && !customInput.disabled
      ? customInput
      : fieldContainer.querySelector("input, select, textarea");

  if (
    focusableField &&
    !focusableField.disabled &&
    typeof focusableField.focus === "function"
  ) {
    focusableField.focus({
      preventScroll: true,
    });
  }
}

/**
 * 提交修改。
 */
async function submitUpdate() {
  errorMessage.value = "";

  if (!editable.value || submitting.value) {
    return;
  }

  if (!validateForm()) {
    await focusFirstValidationError();

    return;
  }

  submitting.value = true;

  try {
    const auctionId = route.params.id;

    /*
     * ACTIVE 的 startAt 必須使用原始後端值，
     * 確保前端不會意外改動已經發生的開始時間。
     */
    const request = {
      ...form,

      startAt:
        auctionStatus.value === "ACTIVE" ? originalStartAt.value : form.startAt,
    };

    await updateAuction(auctionId, request);

    // 修改成功後回詳細頁重新讀取最新資料
    await router.push({
      name: "auction-detail",
      params: {
        id: auctionId,
      },
    });
  } catch (error) {
    if (await handleAuctionAccessError(error, router)) return;
    console.error("修改競標失敗：", error);

    /*
     * 例如：
     * - 剛好有人先完成出價
     * - 非本人
     * - 狀態已改變
     * - 時間規則錯誤
     *
     * 都以後端最新判定為準。
     */
    errorMessage.value =
      error.response?.data?.message ?? "修改競標失敗，請稍後再試";
  } finally {
    submitting.value = false;
  }
}

/**
 * 返回目前競標詳細頁。
 */
function goBackToDetail() {
  router.push({
    name: "auction-detail",
    params: {
      id: route.params.id,
    },
  });
}

/**
 * 競標狀態中文顯示。
 */
function formatStatus(status) {
  const statusMap = {
    SCHEDULED: "尚未開始",
    ACTIVE: "競標中",
    SOLD: "已結標",
    UNSOLD: "流標",
    CANCELLED: "已取消",
    REMOVED: "廢標",
  };

  return statusMap[status] ?? status;
}

// 頁面載入後讀取目前競標
onMounted(() => {
  loadAuction();
});
</script>

<style scoped>
.required-note {
  margin: 0 0 1rem;
  color: #6c757d;
  font-size: 0.875rem;
}

.required-mark {
  margin-left: 0.2rem;
  color: #dc3545;
}

.global-error-alert {
  color: #dc3545;
  background: #f8d7da;
  border-color: rgba(220, 53, 69, 0.35);
}
</style>
