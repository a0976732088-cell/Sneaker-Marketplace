<template>
  <section class="auction-create-page">
    <header class="create-hero">
      <div class="create-hero-inner">
        <div class="create-hero-content">
          <p class="create-eyebrow">酷客 Kicks</p>
          <h1>建立競標</h1>
          <p class="create-hero-copy">
            填寫商品與競標資訊，開始你的球鞋競價。
          </p>
        </div>

        <div class="create-hero-icon" aria-hidden="true">👟</div>
      </div>
    </header>

    <div class="create-content">

      <!-- API / 表單錯誤 -->
      <div v-if="errorMessage" class="create-alert" role="alert">
        {{ errorMessage }}
      </div>

      <p v-if="!ready && !errorMessage" class="create-loading" role="status">
        正在確認登入與載入選項...
      </p>
      <form
        v-if="ready"
        class="create-form-shell"
        novalidate
        @submit.prevent="submitAuction"
      >
        <!-- 商品基本資料 -->
        <section class="create-section">
          <header class="section-heading">
            <p class="section-eyebrow">STEP 01</p>
            <h2>商品基本資料</h2>
            <p>填寫品牌、尺寸與商品狀況，讓買家快速掌握商品資訊。</p>
          </header>

          <p class="required-note">
            <span class="required-mark" aria-hidden="true">*</span>
            為必填欄位
          </p>

          <div class="create-field-grid">
            <AuctionCreateProductSection
              v-model:selected-model-option="selectedModelOption"
              :form="form"
              :brands="brands"
              :categories="categories"
              :field-errors="fieldErrors"
              :available-model-options="availableModelOptions"
              :set-field-element="setFieldElement"
              @brand-change="handleBrandChange"
              @model-option-change="handleModelOptionChange"
              @revalidate-field="revalidateErroredField"
            />
            <AuctionCreateDescriptionSection
              :form="form"
              :field-errors="fieldErrors"
              :selected-description-option="selectedDescriptionOption"
              :selected-form-options="selectedFormOptions"
              :set-field-element="setFieldElement"
              @apply-description-template="applyDescriptionTemplate"
              @start-custom-description="startCustomDescription"
              @description-input="handleDescriptionInput"
              @form-option-change="handleFormOptionChange"
              @revalidate-field="revalidateErroredField"
            />
          </div>
        </section>

        <AuctionCreateImageSection
          :images="images"
          :field-errors="fieldErrors"
          :set-field-element="setFieldElement"
          @change="handleImageChange"
        />

        <AuctionCreateTransactionSection
          v-model:delivery-option="deliveryOption"
          :form="form"
          :selected-form-options="selectedFormOptions"
          :field-errors="fieldErrors"
          :set-field-element="setFieldElement"
          @form-option-change="handleFormOptionChange"
          @revalidate-field="revalidateErroredField"
        />

        <AuctionCreateSettingsSection
          :form="form"
          :field-errors="fieldErrors"
          :set-field-element="setFieldElement"
          :revalidate-errored-field="revalidateErroredField"
          :revalidate-time-fields="revalidateTimeFields"
        />

        <!-- 操作 -->
        <div class="create-actions">
          <button
            type="submit"
            class="create-submit-button"
            :disabled="submitting"
          >
            {{ submitting ? "建立中..." : "建立競標" }}
          </button>

          <button
            type="button"
            class="create-back-button"
            :disabled="submitting"
            @click="router.push('/auctions')"
          >
            返回競標列表
          </button>
        </div>
      </form>
    </div>
  </section>
</template>

<script setup>
import AuctionCreateProductSection from "@/components/auction/create/AuctionCreateProductSection.vue";
import AuctionCreateDescriptionSection from "@/components/auction/create/AuctionCreateDescriptionSection.vue";
import AuctionCreateImageSection from "@/components/auction/create/AuctionCreateImageSection.vue";
import AuctionCreateTransactionSection from "@/components/auction/create/AuctionCreateTransactionSection.vue";
import AuctionCreateSettingsSection from "@/components/auction/create/AuctionCreateSettingsSection.vue";
import {
  AUCTION_DESCRIPTION_OPTIONS,
  AUCTION_MODEL_OPTIONS_BY_BRAND,
  AUCTION_SHOE_SIZES,
  CUSTOM_OPTION_VALUE,
  DELIVERY_OPTION_VALUES,
  MAX_AUCTION_IMAGE_COUNT,
  TAIWAN_CITIES,
  normalizeTaiwanCity,
} from "@/constants/auctionFormOptions";
import { parseAuctionTime } from "@/utils/auctionTime";
import { getAuctionOptions, requireAuctionSession, handleAuctionAccessError } from "@/service/auctionService";
import { computed, nextTick, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";

import { createAuction } from "@/service/auctionService";

// Router
const router = useRouter();

// 是否正在送出建立競標 Request
const submitting = ref(false);
const ready = ref(false);

// 頁面錯誤訊息
const errorMessage = ref("");

// 使用者目前選擇的圖片
const images = ref([]);

const brands = ref([]);
const categories = ref([]);

const selectedModelOption = ref("");
const selectedDescriptionOption = ref("");

// 建立競標表單
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

// Select 僅控制 UI；實際送出的值仍寫入原本 form 欄位。
const selectedFormOptions = reactive({
  defectNote: "",
  authenticityNote: "",
  shippingMethod: "",
  paymentMethod: "",
  warrantyNote: "",
});

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

function handleFormOptionChange(field) {
  if (!Object.prototype.hasOwnProperty.call(selectedFormOptions, field)) return;

  const selectedValue = selectedFormOptions[field];
  form[field] = selectedValue === CUSTOM_OPTION_VALUE ? "" : selectedValue;
  revalidateErroredField(field);
}

function handleBrandChange() {
  selectedModelOption.value = "";
  form.model = "";
  revalidateErroredField("brandId");
}

function handleModelOptionChange() {
  form.model =
    selectedModelOption.value === CUSTOM_OPTION_VALUE
      ? ""
      : selectedModelOption.value;
}

function applyDescriptionTemplate(option) {
  if (!AUCTION_DESCRIPTION_OPTIONS.includes(option)) return;

  selectedDescriptionOption.value = option;
  form.description = option;
  revalidateErroredField("description");
}

async function startCustomDescription() {
  selectedDescriptionOption.value = CUSTOM_OPTION_VALUE;
  await nextTick();
  fieldElements.get("description")?.focus();
}

function handleDescriptionInput() {
  if (selectedDescriptionOption.value !== form.description) {
    selectedDescriptionOption.value = CUSTOM_OPTION_VALUE;
  }
  revalidateErroredField("description");
}

/**
 * 防止保留給 UI controller 的 sentinel 被當成使用者內容送出。
 */
function clearReservedFormOptionValues() {
  for (const field of Object.keys(selectedFormOptions)) {
    if (form[field] === CUSTOM_OPTION_VALUE) {
      form[field] = "";
    }
  }
}

const validationOrder = Object.freeze([
  "brandId",
  "categoryId",
  "productName",
  "size",
  "condition",
  "description",
  "location",
  "images",
  "shippingMethod",
  "shippingFee",
  "paymentMethod",
  "startPrice",
  "minIncrement",
  "startAt",
  "endAt",
]);

const fieldErrors = reactive({
  brandId: "",
  categoryId: "",
  productName: "",
  size: "",
  condition: "",
  description: "",
  location: "",
  images: "",
  shippingMethod: "",
  shippingFee: "",
  paymentMethod: "",
  startPrice: "",
  minIncrement: "",
  startAt: "",
  endAt: "",
});

const fieldElements = new Map();

function setFieldElement(field) {
  return (element) => {
    if (element) {
      fieldElements.set(field, element);
      return;
    }

    fieldElements.delete(field);
  };
}

function isBlank(value) {
  return (
    value === null ||
    value === undefined ||
    (typeof value === "string" && value.trim() === "")
  );
}

function integerAmountError(
  value,
  { invalidMessage, minimum, minimumMessage, integerMessage },
) {
  if (isBlank(value) || !Number.isFinite(Number(value))) {
    return invalidMessage;
  }

  const amount = Number(value);

  if (amount < minimum) {
    return minimumMessage;
  }

  if (!Number.isInteger(amount)) {
    return integerMessage;
  }

  return "";
}

function getTimeErrors() {
  const timeErrors = {
    startAt: "",
    endAt: "",
  };

  if (isBlank(form.startAt)) {
    timeErrors.startAt = "請設定競標開始時間";
  }

  if (isBlank(form.endAt)) {
    timeErrors.endAt = "請設定競標結束時間";
  }

  if (timeErrors.startAt || timeErrors.endAt) {
    return timeErrors;
  }

  const startAt = parseAuctionTime(form.startAt);
  const endAt = parseAuctionTime(form.endAt);

  if (
    !Number.isFinite(startAt.getTime()) ||
    !Number.isFinite(endAt.getTime()) ||
    endAt <= startAt
  ) {
    timeErrors.endAt = "競標結束時間必須晚於開始時間";
    return timeErrors;
  }

  if (endAt <= new Date()) {
    timeErrors.endAt = "競標結束時間必須晚於目前時間";
  }

  return timeErrors;
}

function getFieldError(field) {
  switch (field) {
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
      return isBlank(form.location) || TAIWAN_CITIES.includes(form.location)
        ? ""
        : "請從台灣縣市選項中選擇商品所在地";
    case "images":
      if (images.value.length === 0) return "請至少選擇一張商品圖片";
      if (images.value.length > MAX_AUCTION_IMAGE_COUNT) {
        return `競標商品最多只能選擇 ${MAX_AUCTION_IMAGE_COUNT} 張圖片`;
      }
      return "";
    case "shippingMethod":
      return isBlank(form.shippingMethod) ? "請選擇運送方式" : "";
    case "shippingFee":
      return integerAmountError(form.shippingFee, {
        invalidMessage: "請輸入有效的運費",
        minimum: 0,
        minimumMessage: "運費不可小於 0",
        integerMessage: "運費必須為整數",
      });
    case "paymentMethod":
      return isBlank(form.paymentMethod) ? "請選擇付款方式" : "";
    case "startPrice":
      return integerAmountError(form.startPrice, {
        invalidMessage: "請輸入有效的起標價",
        minimum: 1,
        minimumMessage: "起標價必須大於 0",
        integerMessage: "起標價必須為整數",
      });
    case "minIncrement":
      return integerAmountError(form.minIncrement, {
        invalidMessage: "請輸入有效的最低加價金額",
        minimum: 1,
        minimumMessage: "最低加價金額必須大於 0",
        integerMessage: "最低加價金額必須為整數",
      });
    default:
      return "";
  }
}

function revalidateErroredField(field) {
  if (!fieldErrors[field]) return;
  fieldErrors[field] = getFieldError(field);
}

function normalizeLocation() {
  form.location = normalizeTaiwanCity(form.location);
  revalidateErroredField("location");
}

function revalidateTimeFields() {
  if (!fieldErrors.startAt && !fieldErrors.endAt) return;

  const timeErrors = getTimeErrors();
  fieldErrors.startAt = timeErrors.startAt;
  fieldErrors.endAt = timeErrors.endAt;
}

async function focusField(field) {
  await nextTick();

  const element = fieldElements.get(field);
  if (!element) return;

  element.scrollIntoView({
    behavior: "smooth",
    block: "center",
  });
  element.focus({
    preventScroll: true,
  });
}

async function focusFirstError() {
  const firstErrorField = validationOrder.find((field) => fieldErrors[field]);
  if (!firstErrorField) return;

  await focusField(firstErrorField);
}

/**
 * 使用者選擇商品圖片。
 */
function handleImageChange(event) {
  errorMessage.value = "";

  const selectedFiles = Array.from(event.target.files ?? []);

  if (selectedFiles.length > MAX_AUCTION_IMAGE_COUNT) {
    images.value = [];

    /*
     * 清掉 input，
     * 避免畫面看起來仍然保留超過上限的圖片。
    */
    event.target.value = "";

    fieldErrors.images = `競標商品最多只能選擇 ${MAX_AUCTION_IMAGE_COUNT} 張圖片`;
    void focusField("images");

    return;
  }

  images.value = selectedFiles;
  revalidateErroredField("images");
}

/**
 * 建立競標前的簡單前端檢查。
 *
 * 後端仍然是最終驗證者，
 * 前端檢查只用來改善操作體驗。
 */
function validateForm() {
  clearReservedFormOptionValues();
  normalizeLocation();

  for (const field of validationOrder) {
    if (field === "startAt" || field === "endAt") continue;
    fieldErrors[field] = getFieldError(field);
  }

  const timeErrors = getTimeErrors();
  fieldErrors.startAt = timeErrors.startAt;
  fieldErrors.endAt = timeErrors.endAt;

  return !validationOrder.some((field) => fieldErrors[field]);
}

/**
 * 提交建立競標表單。
 */
async function submitAuction() {
  if (!ready.value || submitting.value) return;
  errorMessage.value = "";

  if (!validateForm()) {
    await focusFirstError();
    return;
  }

  submitting.value = true;

  try {
    const response = await createAuction(
      {
        ...form,
      },
      images.value,
    );

    // 後端目前 ApiResponse.data 直接回新建立的 auctionId
    const auctionId = response.data.data;

    // 建立完成後直接進入既有競標詳細頁
    await router.push({
      name: "auction-detail",
      params: {
        id: auctionId,
      },
    });
  } catch (error) {
    if (await handleAuctionAccessError(error, router)) return;
    console.error("建立競標失敗：", error);

    /*
     * 優先顯示後端 GlobalExceptionHandler /
     * ApiResponse 回傳的錯誤訊息。
     */
    errorMessage.value =
      error.response?.data?.message ?? "建立競標失敗，請稍後再試";
  } finally {
    submitting.value = false;
  }
}
onMounted(async () => {
  try {
    await requireAuctionSession();
    const response = await getAuctionOptions();
    brands.value = response.data.data.brands;
    categories.value = response.data.data.categories;
    if (!brands.value.length || !categories.value.length) {
      errorMessage.value = "品牌或分類尚未建立，目前無法刊登競標。";
      return;
    }
    ready.value = true;
  } catch (error) {
    if (await handleAuctionAccessError(error, router)) return;
    errorMessage.value = "無法載入競標選項，請重新整理後再試。";
  }
});
</script>

<style scoped>
.auction-create-page,
.auction-create-page * {
  box-sizing: border-box;
}

.auction-create-page {
  min-height: 100vh;
  overflow-x: hidden;
  color: #212529;
  background: #fff;
}

.create-hero {
  position: relative;
  display: flex;
  min-height: 224px;
  overflow: hidden;
  color: #fff;
  background: #111;
  border-bottom: 1px solid #212529;
}

.create-hero-inner {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  max-width: 1180px;
  min-width: 0;
  margin: 0 auto;
  padding: 3rem 1.5rem;
}

.create-hero-content {
  position: relative;
  z-index: 1;
  max-width: 680px;
}

.create-eyebrow,
.section-eyebrow {
  margin: 0 0 0.65rem;
  font-size: 0.75rem;
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.create-eyebrow {
  color: #dee2e6;
}

.create-hero h1 {
  margin: 0;
  font-size: clamp(2.25rem, 5vw, 3.5rem);
  font-weight: 800;
  line-height: 1.05;
  letter-spacing: -0.04em;
}

.create-hero-copy {
  max-width: 560px;
  margin: 1rem 0 0;
  color: #dee2e6;
  font-size: 1rem;
  line-height: 1.75;
}

.create-hero-icon {
  position: absolute;
  right: 1rem;
  bottom: -2.65rem;
  color: #fff;
  font-size: clamp(7.5rem, 17vw, 11rem);
  line-height: 1;
  opacity: 0.1;
  transform: rotate(-12deg);
  user-select: none;
  pointer-events: none;
}

.create-content {
  width: 100%;
  max-width: 1180px;
  min-width: 0;
  margin: 0 auto;
  padding: 3.25rem 1.5rem 4rem;
}

.create-alert,
.create-loading {
  width: 100%;
  margin: 0 0 2rem;
  padding: 1rem 1.15rem;
  border: 1px solid #dee2e6;
  border-radius: 0.4rem;
  background: #f8f9fa;
  color: #212529;
  line-height: 1.6;
}

.create-alert {
  color: #842029;
  font-weight: 700;
  background: #f8d7da;
  border-color: #f1aeb5;
  border-left: 4px solid #dc3545;
}

.create-loading {
  color: #6c757d;
}

.create-form-shell {
  width: 100%;
  min-width: 0;
}

.required-note {
  margin: 0 0 1.5rem;
  color: #6c757d;
  font-size: 0.82rem;
  line-height: 1.5;
}

.required-mark {
  margin-left: 0.2rem;
  color: #dc3545;
}

.create-form-shell > .create-section:first-child {
  margin: 0;
  padding: 0 0 3rem;
  border: 0;
}

.section-heading {
  max-width: 650px;
  margin-bottom: 2rem;
}

.section-eyebrow {
  color: #8a9096;
}

.section-heading h2 {
  margin: 0;
  color: #111;
  font-size: clamp(1.55rem, 3vw, 1.9rem);
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.section-heading > p:last-child {
  margin: 0.65rem 0 0;
  color: #6c757d;
  line-height: 1.7;
}

.create-field-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 1.4rem 1.5rem;
  width: 100%;
  min-width: 0;
}

.create-field-grid > [class^="col-"],
.create-field-grid > [class*=" col-"] {
  width: auto;
  max-width: none;
  min-width: 0;
  padding: 0;
}

.create-field-grid > .col-md-8 {
  grid-column: span 8;
}

.create-field-grid > .col-md-6 {
  grid-column: span 6;
}

.create-field-grid > .col-md-4 {
  grid-column: span 4;
}

.create-field-grid > .col-12 {
  grid-column: 1 / -1;
}

.create-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.75rem;
  padding-top: 2rem;
  border-top: 1px solid #e9ecef;
}

.create-submit-button,
.create-back-button {
  min-height: 48px;
  padding: 0.75rem 1.6rem;
  font: inherit;
  font-weight: 700;
  line-height: 1.2;
  border: 1px solid #111;
  border-radius: 999px;
  cursor: pointer;
  transition:
    color 0.16s ease,
    background-color 0.16s ease,
    border-color 0.16s ease;
}

.create-submit-button {
  color: #fff;
  background: #111;
}

.create-submit-button:hover:not(:disabled) {
  background: #212529;
  border-color: #212529;
}

.create-back-button {
  color: #212529;
  background: #fff;
  border-color: #dee2e6;
}

.create-back-button:hover:not(:disabled) {
  background: #f8f9fa;
  border-color: #8a9096;
}

.create-submit-button:focus-visible,
.create-back-button:focus-visible {
  outline: 2px solid #111;
  outline-offset: 3px;
}

.create-submit-button:disabled,
.create-back-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

@media (max-width: 767.98px) {
  .create-hero {
    min-height: 190px;
  }

  .create-hero-inner {
    padding: 2.5rem 1.25rem;
  }

  .create-hero-copy {
    max-width: 80%;
  }

  .create-hero-icon {
    right: -1.5rem;
    bottom: -1.75rem;
    font-size: 8rem;
  }

  .create-content {
    padding: 2.5rem 1.25rem 3rem;
  }

  .create-form-shell > .create-section:first-child {
    padding-bottom: 2.5rem;
  }

  .create-field-grid > .col-md-8,
  .create-field-grid > .col-md-6,
  .create-field-grid > .col-md-4,
  .create-field-grid > .col-12 {
    grid-column: 1 / -1;
  }

  .create-actions {
    flex-direction: column;
  }

  .create-submit-button,
  .create-back-button {
    width: 100%;
  }
}

@media (max-width: 479.98px) {
  .create-hero {
    min-height: 176px;
  }

  .create-hero-inner,
  .create-content {
    padding-right: 1rem;
    padding-left: 1rem;
  }

  .create-hero h1 {
    font-size: 2.2rem;
  }

  .create-hero-copy {
    max-width: 88%;
    font-size: 0.94rem;
  }

  .section-heading {
    margin-bottom: 1.5rem;
  }

  .create-field-grid {
    gap: 1.2rem;
  }

}
</style>
