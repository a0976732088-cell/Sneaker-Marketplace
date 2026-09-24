<template>
  <main class="auction-checkout-page">
    <header class="checkout-header">
      <div>
        <p class="eyebrow">Auction checkout</p>
        <h1>競標訂單結帳</h1>
      </div>
      <RouterLink class="back-link" :to="{ name: 'auction-orders' }">
        返回競標訂單
      </RouterLink>
    </header>

    <p v-if="loading || redirecting" class="page-state" role="status">
      {{ redirecting ? "正在前往訂單明細…" : "載入訂單資料中…" }}
    </p>

    <section v-else-if="errorMessage" class="page-state page-state--error" role="alert">
      <h2>無法顯示競標訂單</h2>
      <p>{{ errorMessage }}</p>
      <div class="state-actions">
        <button type="button" class="secondary-button" @click="loadOrder">重新載入</button>
        <RouterLink class="secondary-button" :to="{ name: 'auction-orders' }">
          返回訂單
        </RouterLink>
      </div>
    </section>

    <template v-else-if="order">
      <div class="checkout-layout">
        <div class="checkout-forms">
          <AuctionCheckoutShippingSection
            :order="order"
            :member-email="memberEmail"
            :shipping-form="shippingForm"
            :shipping-errors="shippingErrors"
            :is-cvs-shipping="isCvsShipping"
            :can-operate-checkout="canOperateCheckout"
            :is-busy="isBusy"
            :has-member-profile="hasMemberProfile"
            :deadline-notice="deadlineNotice"
            :route-notice="routeNotice"
            :has-selected-cvs-store="hasSelectedCvsStore"
            :selected-cvs-store-label="selectedCvsStoreLabel"
            :cvs-selection="cvsSelection"
            @fill-from-member="fillFromMember"
            @open-cvs-picker="openCvsPicker"
            @open-store-map="openStoreMap"
          >
            <template #cvs-picker>
              <AuctionCvsStorePicker
                :open="isCvsPickerOpen"
                :selected-store-type="selectedCvsStoreType"
                :filters="cvsStoreFilters"
                :is-loading="isLoadingCvsStores"
                :is-selecting="isSelectingCvsStore"
                :load-error="cvsStoreLoadError"
                :selection-error="cvsStoreSelectionError"
                :has-search-criteria="hasCvsSearchCriteria"
                :filtered-stores="filteredCvsStores"
                :has-too-many-stores="hasTooManyCvsStores"
                :is-current-store="isCurrentCvsStore"
                @close="closeCvsPicker()"
                @change-store-type="changeCvsStoreType"
                @reload="reloadCvsStores"
                @select-store="chooseCvsStore"
                @open-map="openStoreMap"
              />
            </template>
          </AuctionCheckoutShippingSection>
          <AuctionCheckoutPaymentSection
            :order="order"
            :is-credit-card="isCreditCard"
            :is-transfer="isTransfer"
            :is-line-pay="isLinePay"
            :payment-method-label="paymentMethodLabel"
            :can-operate-checkout="canOperateCheckout"
            :is-busy="isBusy"
            :card-form="cardForm"
            :card-errors="cardErrors"
            :transfer-form="transferForm"
            :transfer-errors="transferErrors"
            :is-waiting-for-line-pay="isWaitingForLinePay"
            @card-number-input="handleCardNumberInput"
            @card-expiry-input="handleCardExpiryInput"
            @card-cvv-input="handleCardCvvInput"
            @account-last-five-input="handleAccountLastFiveInput"
          />
          <div v-if="saveMessage" class="notice notice--success" role="status" aria-live="polite">
            {{ saveMessage }}
          </div>
          <div v-if="operationError" class="notice notice--error" role="alert" aria-live="assertive">
            {{ operationError }}
          </div>

          <div class="checkout-actions">
            <button
              type="button"
              class="secondary-button"
              :disabled="!canOperateCheckout || isBusy"
              @click="saveCheckout"
            >
              {{ isSaving ? "儲存中…" : "儲存收件資料" }}
            </button>
            <button
              type="button"
              class="primary-button"
              :disabled="!canSubmitPayment"
              @click="confirmAndPay"
            >
              {{ paymentButtonText }}
            </button>
          </div>
        </div>

        <AuctionCheckoutOrderSummary
          :order="order"
          :product-image-url="productImageUrl"
          :product-meta="productMeta"
          :can-operate-checkout="canOperateCheckout"
          :remaining-payment-time-text="remainingPaymentTimeText"
          :deadline-notice="deadlineNotice"
          @image-error="imageLoadFailed = true"
        />
      </div>
    </template>
  </main>
</template>

<script setup>
import AuctionCheckoutShippingSection from "@/components/auction/checkout/AuctionCheckoutShippingSection.vue";
import AuctionCvsStorePicker from "@/components/auction/checkout/AuctionCvsStorePicker.vue";
import AuctionCheckoutPaymentSection from "@/components/auction/checkout/AuctionCheckoutPaymentSection.vue";
import AuctionCheckoutOrderSummary from "@/components/auction/checkout/AuctionCheckoutOrderSummary.vue";
import { computed, onMounted, onUnmounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";

import {
  AUCTION_CVS_RESULT_LIMIT,
  AUCTION_CVS_STORE_OPTIONS,
  AUCTION_CVS_STORE_TYPE,
} from "@/constants/auctionCvs";
import { AUCTION_PAYMENT_METHOD } from "@/constants/auctionPayment";
import { getCurrentMember } from "@/service/authService";
import {
  getAuctionCvsSelection,
  getAuctionCvsStores,
  getAuctionLinePayStatus,
  getAuctionOrderDetail,
  handleAuctionAccessError,
  payAuctionOrder,
  requestAuctionLinePay,
  selectAuctionCvsStore,
  updateAuctionOrderCheckout,
} from "@/service/auctionService";
import { resolveAuctionImageUrl } from "@/utils/auctionImageUrl";
import { parseAuctionTime } from "@/utils/auctionTime";
import {
  formatAccountLastFive,
  formatCardCvv,
  formatCardExpiry,
  formatCardNumber,
  validateCreditCardFormValues,
  validateShippingFormValues,
  validateTransferFormValues,
} from "@/utils/paymentForm";

const route = useRoute();
const router = useRouter();

const order = ref(null);
const loading = ref(true);
const redirecting = ref(false);
const errorMessage = ref("");
const operationError = ref("");
const saveMessage = ref("");
const routeNotice = ref("");
const isSaving = ref(false);
const isPaying = ref(false);
const isLoadingCvsStores = ref(false);
const isSelectingCvsStore = ref(false);
const isCvsPickerOpen = ref(false);
const isWaitingForLinePay = ref(false);
const linePayMerchantOrderId = ref("");
const imageLoadFailed = ref(false);
const currentTimeMs = ref(Date.now());

const memberEmail = ref("");
const memberInfo = reactive({ name: "", phone: "", address: "" });
const shippingForm = reactive({ receiverName: "", receiverPhone: "", shippingAddress: "" });
const shippingErrors = reactive({ receiverName: "", receiverPhone: "", shippingAddress: "" });
const cardForm = reactive({ cardHolderName: "", cardNumber: "", cardExpiry: "", cardCvv: "" });
const cardErrors = reactive({ cardHolderName: "", cardNumber: "", cardExpiry: "", cardCvv: "" });
const transferForm = reactive({ remitterName: "", accountLastFive: "" });
const transferErrors = reactive({ remitterName: "", accountLastFive: "" });
const selectedCvsStoreType = ref(AUCTION_CVS_STORE_TYPE.UNIMART);
const cvsStores = ref([]);
const cvsStoreFilters = reactive({ city: "", keyword: "" });
const cvsStoreLoadError = ref("");
const cvsStoreSelectionError = ref("");
const cvsSelection = reactive({
  selected: false,
  storeType: null,
  logisticsSubType: null,
  cvsStoreId: null,
  cvsStoreName: null,
  cvsAddress: null,
  cvsTelephone: null,
  cvsOutside: null,
  selectedAt: null,
});

let requestVersion = 0;
let viewVersion = 0;
let countdownTimer = null;
let deadlineRefreshOrderNumber = null;
let cvsStoreRequestVersion = 0;
let linePayPaymentWindow = null;
let linePayPollingVersion = 0;
let linePayPollTimer = null;
let resolveLinePayPollDelay = null;

const LINE_PAY_POLL_DELAY_MS = 1500;

const isBuyer = computed(() => order.value?.viewerRole === "BUYER");
const isPendingPayment = computed(
  () => order.value?.paymentStatus === "PENDING" && order.value?.orderStatus === "PENDING_PAYMENT",
);
const isCvsShipping = computed(() => order.value?.shippingMethod === "超商寄送");
const paymentDeadlineMs = computed(() => {
  if (!order.value?.paymentDueAt) return null;
  const timestamp = parseAuctionTime(order.value.paymentDueAt).getTime();
  return Number.isFinite(timestamp) ? timestamp : null;
});
const hasValidPaymentDeadline = computed(() => paymentDeadlineMs.value !== null);
const isPaymentDeadlineReached = computed(
  () => hasValidPaymentDeadline.value && currentTimeMs.value >= paymentDeadlineMs.value,
);
const canOperateCheckout = computed(
  () => isBuyer.value && isPendingPayment.value && hasValidPaymentDeadline.value && !isPaymentDeadlineReached.value,
);
const isCreditCard = computed(() => order.value?.paymentMethod === AUCTION_PAYMENT_METHOD.CREDIT_CARD);
const isTransfer = computed(() => order.value?.paymentMethod === AUCTION_PAYMENT_METHOD.TRANSFER);
const isLinePay = computed(() => order.value?.paymentMethod === AUCTION_PAYMENT_METHOD.LINE_PAY);
const isSupportedPaymentMethod = computed(
  () => isCreditCard.value || isTransfer.value || isLinePay.value,
);
const isBusy = computed(
  () =>
    isSaving.value ||
    isPaying.value ||
    isSelectingCvsStore.value ||
    isWaitingForLinePay.value,
);
const hasSelectedCvsStore = computed(() => Boolean(cvsSelection.selected));
const canSubmitPayment = computed(
  () =>
    canOperateCheckout.value &&
    isSupportedPaymentMethod.value &&
    (!isCvsShipping.value || hasSelectedCvsStore.value) &&
    !isBusy.value,
);
const hasMemberProfile = computed(
  () => Boolean(memberInfo.name || memberInfo.phone || memberInfo.address),
);
const productImageUrl = computed(() =>
  imageLoadFailed.value ? "" : resolveAuctionImageUrl(order.value?.imageUrl),
);
const productMeta = computed(
  () => [order.value?.model, order.value?.size].filter(Boolean).join(" · ") || "商品規格未提供",
);
const selectedCvsStoreLabel = computed(
  () =>
    AUCTION_CVS_STORE_OPTIONS.find((option) => option.value === cvsSelection.storeType)?.label ||
    "超商",
);
const normalizedCvsCity = computed(() => normalizeCvsSearchText(cvsStoreFilters.city));
const normalizedCvsKeyword = computed(() => normalizeCvsSearchText(cvsStoreFilters.keyword));
const hasCvsSearchCriteria = computed(
  () => Boolean(normalizedCvsCity.value || normalizedCvsKeyword.value),
);
const matchingCvsStores = computed(() => {
  if (!hasCvsSearchCriteria.value) return [];

  return cvsStores.value
    .filter((store) => {
      const address = normalizeCvsSearchText(store.storeAddress);
      if (normalizedCvsCity.value && !address.includes(normalizedCvsCity.value)) return false;
      if (!normalizedCvsKeyword.value) return true;

      return [store.storeId, store.storeName, store.storeAddress, store.storePhone]
        .some((value) => normalizeCvsSearchText(value).includes(normalizedCvsKeyword.value));
    });
});
const hasTooManyCvsStores = computed(
  () => matchingCvsStores.value.length > AUCTION_CVS_RESULT_LIMIT,
);
const filteredCvsStores = computed(() =>
  matchingCvsStores.value.slice(0, AUCTION_CVS_RESULT_LIMIT),
);
const paymentMethodLabel = computed(() => {
  if (isCreditCard.value) return "信用卡";
  if (isTransfer.value) return "銀行轉帳";
  if (isLinePay.value) return "LINE Pay";
  return order.value?.paymentMethod || "未知付款方式";
});
const paymentButtonText = computed(() => {
  if (isWaitingForLinePay.value) return "等待 LINE Pay 完成付款…";
  if (isPaying.value) return "付款處理中…";
  if (isLinePay.value) return "使用 LINE Pay 付款";
  return isTransfer.value ? "確認已完成轉帳" : "確認並付款";
});
const remainingPaymentTimeText = computed(() => {
  if (!hasValidPaymentDeadline.value) return "—";
  const remainingMs = Math.max(0, paymentDeadlineMs.value - currentTimeMs.value);
  const totalSeconds = Math.ceil(remainingMs / 1000);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  return [hours, minutes, seconds].map((value) => String(value).padStart(2, "0")).join(":");
});
const deadlineNotice = computed(() => {
  if (!isPendingPayment.value) return "此訂單目前不可修改或付款。";
  if (!hasValidPaymentDeadline.value) return "無法確認付款期限，暫時無法修改或付款。";
  if (isPaymentDeadlineReached.value) return "付款期限已到，系統正在更新訂單狀態。";
  return "";
});


function normalizeCvsSearchText(value) {
  return String(value ?? "")
    .normalize("NFKC")
    .trim()
    .replaceAll("台", "臺")
    .toLocaleLowerCase("zh-TW");
}

function buildStoreMapUrl(store) {
  const storeName =
    String(store?.storeName ?? "").trim() || String(store?.cvsStoreName ?? "").trim();
  const storeAddress =
    String(store?.storeAddress ?? "").trim() || String(store?.cvsAddress ?? "").trim();
  if (!storeName && !storeAddress) return "";

  const storeBrand = AUCTION_CVS_STORE_OPTIONS.find(
    (option) => option.value === store?.storeType,
  )?.label;
  const query = [storeBrand, storeName, storeAddress].filter(Boolean).join(" ");
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(query)}`;
}

function openStoreMap(store) {
  const url = buildStoreMapUrl(store);
  if (!url) return;

  const mapWindow = window.open(url, "_blank", "noopener,noreferrer");
  if (mapWindow) {
    try {
      mapWindow.opener = null;
    } catch {
      // The noopener window feature already prevents access to the originating checkout.
    }
  }
}

function applyErrors(target, keys, errors) {
  for (const key of keys) target[key] = errors[key] ?? "";
  return keys.every((key) => !target[key]);
}

function validateShippingForm() {
  if (isCvsShipping.value && hasSelectedCvsStore.value) {
    shippingForm.shippingAddress = cvsSelection.cvsAddress ?? shippingForm.shippingAddress;
  }
  const errors = validateShippingFormValues(shippingForm);
  if (isCvsShipping.value && !hasSelectedCvsStore.value) {
    errors.shippingAddress = "請先選擇超商取貨門市";
  }
  return applyErrors(
    shippingErrors,
    ["receiverName", "receiverPhone", "shippingAddress"],
    errors,
  );
}

function validateCreditCardForm() {
  return applyErrors(
    cardErrors,
    ["cardHolderName", "cardNumber", "cardExpiry", "cardCvv"],
    validateCreditCardFormValues(cardForm),
  );
}

function validateTransferForm() {
  return applyErrors(
    transferErrors,
    ["remitterName", "accountLastFive"],
    validateTransferFormValues(transferForm),
  );
}

function handleCardNumberInput(event) {
  cardForm.cardNumber = formatCardNumber(event.target.value);
  cardErrors.cardNumber = "";
}

function handleCardExpiryInput(event) {
  cardForm.cardExpiry = formatCardExpiry(event.target.value);
  cardErrors.cardExpiry = "";
}

function handleCardCvvInput(event) {
  cardForm.cardCvv = formatCardCvv(event.target.value);
  cardErrors.cardCvv = "";
}

function handleAccountLastFiveInput(event) {
  transferForm.accountLastFive = formatAccountLastFive(event.target.value);
  transferErrors.accountLastFive = "";
}

function resetCheckoutForms() {
  Object.assign(cardForm, { cardHolderName: "", cardNumber: "", cardExpiry: "", cardCvv: "" });
  Object.assign(transferForm, { remitterName: "", accountLastFive: "" });
  applyErrors(cardErrors, ["cardHolderName", "cardNumber", "cardExpiry", "cardCvv"], {});
  applyErrors(transferErrors, ["remitterName", "accountLastFive"], {});
}

function resetCvsSelection() {
  Object.assign(cvsSelection, {
    selected: false,
    storeType: null,
    logisticsSubType: null,
    cvsStoreId: null,
    cvsStoreName: null,
    cvsAddress: null,
    cvsTelephone: null,
    cvsOutside: null,
    selectedAt: null,
  });
  selectedCvsStoreType.value = AUCTION_CVS_STORE_TYPE.UNIMART;
}

function resetCvsStoreBrowser() {
  cvsStoreRequestVersion++;
  cvsStores.value = [];
  Object.assign(cvsStoreFilters, { city: "", keyword: "" });
  cvsStoreLoadError.value = "";
  cvsStoreSelectionError.value = "";
  isLoadingCvsStores.value = false;
  isSelectingCvsStore.value = false;
}

function applyCvsSelection(selection) {
  Object.assign(cvsSelection, {
    selected: Boolean(selection?.selected),
    storeType: selection?.storeType ?? null,
    logisticsSubType: selection?.logisticsSubType ?? null,
    cvsStoreId: selection?.cvsStoreId ?? null,
    cvsStoreName: selection?.cvsStoreName ?? null,
    cvsAddress: selection?.cvsAddress ?? null,
    cvsTelephone: selection?.cvsTelephone ?? null,
    cvsOutside: selection?.cvsOutside ?? null,
    selectedAt: selection?.selectedAt ?? null,
  });

  if (AUCTION_CVS_STORE_OPTIONS.some((option) => option.value === cvsSelection.storeType)) {
    selectedCvsStoreType.value = cvsSelection.storeType;
  }
  if (cvsSelection.selected && cvsSelection.cvsAddress) {
    shippingForm.shippingAddress = cvsSelection.cvsAddress;
    shippingErrors.shippingAddress = "";
  }
}

function initializeShippingForm(detail) {
  Object.assign(shippingForm, {
    receiverName: detail.receiverName ?? "",
    receiverPhone: detail.receiverPhone ?? "",
    shippingAddress: detail.shippingAddress ?? "",
  });
  applyErrors(shippingErrors, ["receiverName", "receiverPhone", "shippingAddress"], {});
}

function fillFromMember() {
  if (!canOperateCheckout.value || isBusy.value) return;
  shippingForm.receiverName = memberInfo.name ?? "";
  shippingForm.receiverPhone = memberInfo.phone ?? "";
  if (!isCvsShipping.value) shippingForm.shippingAddress = memberInfo.address ?? "";
  applyErrors(shippingErrors, ["receiverName", "receiverPhone", "shippingAddress"], {});
}

function checkoutPayload() {
  return {
    receiverName: shippingForm.receiverName.trim(),
    receiverPhone: shippingForm.receiverPhone.trim(),
    shippingAddress: String(
      isCvsShipping.value && hasSelectedCvsStore.value
        ? cvsSelection.cvsAddress ?? shippingForm.shippingAddress
        : shippingForm.shippingAddress,
    ).trim(),
  };
}

function isCurrentCheckout(orderNumber, activeViewVersion) {
  return activeViewVersion === viewVersion && String(route.params.orderNumber) === String(orderNumber);
}

function shouldUseDetailPage(detail) {
  return detail?.viewerRole !== "BUYER" || detail?.paymentStatus !== "PENDING" || detail?.orderStatus !== "PENDING_PAYMENT";
}

async function redirectToDetail(orderNumber) {
  stopLinePayPolling({ closeWindow: true });
  redirecting.value = true;
  await router.replace({ name: "auction-order-detail", params: { orderNumber } });
}

function closeLinePayPaymentWindow(paymentWindow = linePayPaymentWindow) {
  if (!paymentWindow) return;
  try {
    if (!paymentWindow.closed) paymentWindow.close();
  } catch {
    // 跨來源付款視窗若拒絕存取，仍可安全清除本地參照。
  }
  if (linePayPaymentWindow === paymentWindow) linePayPaymentWindow = null;
}

function stopLinePayPolling({ closeWindow = false } = {}) {
  linePayPollingVersion++;
  if (linePayPollTimer !== null) {
    clearTimeout(linePayPollTimer);
    linePayPollTimer = null;
  }
  if (resolveLinePayPollDelay) {
    const resolveDelay = resolveLinePayPollDelay;
    resolveLinePayPollDelay = null;
    resolveDelay(false);
  }
  isWaitingForLinePay.value = false;
  linePayMerchantOrderId.value = "";
  if (closeWindow) {
    closeLinePayPaymentWindow();
  } else if (linePayPaymentWindow?.closed) {
    linePayPaymentWindow = null;
  }
}

function waitForLinePayPollDelay(pollingVersion) {
  if (pollingVersion !== linePayPollingVersion) return Promise.resolve(false);

  return new Promise((resolve) => {
    resolveLinePayPollDelay = resolve;
    linePayPollTimer = setTimeout(() => {
      linePayPollTimer = null;
      resolveLinePayPollDelay = null;
      resolve(pollingVersion === linePayPollingVersion);
    }, LINE_PAY_POLL_DELAY_MS);
  });
}

function linePayStatusValue(response) {
  const payload = response?.data?.data ?? response?.data;
  return String(payload?.status ?? "").trim().toUpperCase();
}

async function pollLinePayStatus(
  orderNumber,
  merchantOrderId,
  activeViewVersion,
  pollingVersion,
) {
  while (
    pollingVersion === linePayPollingVersion &&
    isCurrentCheckout(orderNumber, activeViewVersion)
  ) {
    let response;
    try {
      response = await getAuctionLinePayStatus(orderNumber, merchantOrderId);
    } catch (error) {
      if (
        pollingVersion !== linePayPollingVersion ||
        !isCurrentCheckout(orderNumber, activeViewVersion)
      ) {
        return;
      }
      const status = error.response?.status;
      if (status === 401 || status === 403) {
        stopLinePayPolling({ closeWindow: true });
        await handleAuctionAccessError(error, router);
        return;
      }
      if (status >= 400 && status < 500) {
        stopLinePayPolling({ closeWindow: true });
        operationError.value = "LINE Pay 付款狀態查詢失敗，請稍後再試。";
        return;
      }

      // Provider timeout / 5xx 的結果不明確；保留同一 attempt，避免重複建立付款。
      if (!(await waitForLinePayPollDelay(pollingVersion))) return;
      continue;
    }

    if (
      pollingVersion !== linePayPollingVersion ||
      !isCurrentCheckout(orderNumber, activeViewVersion)
    ) {
      return;
    }

    const status = linePayStatusValue(response);
    if (status === "PAID") {
      stopLinePayPolling({ closeWindow: true });
      await redirectToDetail(orderNumber);
      return;
    }
    if (status === "CANCELLED") {
      stopLinePayPolling({ closeWindow: true });
      routeNotice.value = "LINE Pay 已取消，你仍可在付款期限內重新付款。";
      return;
    }
    if (status === "FAILED") {
      stopLinePayPolling({ closeWindow: true });
      operationError.value = "LINE Pay 付款失敗，請在付款期限內重新嘗試。";
      return;
    }
    if (status !== "WAITING") {
      stopLinePayPolling({ closeWindow: true });
      operationError.value = "LINE Pay 回傳了無法辨識的付款狀態，請稍後再試。";
      return;
    }

    if (linePayPaymentWindow?.closed) {
      stopLinePayPolling();
      routeNotice.value =
        "LINE Pay 付款視窗已關閉。若尚未完成付款，可重新發起付款。";
      return;
    }

    if (!(await waitForLinePayPollDelay(pollingVersion))) return;
  }
}

function startLinePayPolling(orderNumber, merchantOrderId, paymentWindow, activeViewVersion) {
  linePayPaymentWindow = paymentWindow;
  linePayMerchantOrderId.value = merchantOrderId;
  isWaitingForLinePay.value = true;
  const pollingVersion = ++linePayPollingVersion;
  void pollLinePayStatus(orderNumber, merchantOrderId, activeViewVersion, pollingVersion);
}

async function loadMember(orderNumber, activeViewVersion) {
  try {
    const member = await getCurrentMember();
    if (!isCurrentCheckout(orderNumber, activeViewVersion)) return;
    memberEmail.value = member.email ?? "";
    Object.assign(memberInfo, {
      name: member.name ?? "",
      phone: member.phone ?? "",
      address: member.address ?? "",
    });
  } catch {
    if (!isCurrentCheckout(orderNumber, activeViewVersion)) return;
    memberEmail.value = "";
    Object.assign(memberInfo, { name: "", phone: "", address: "" });
  }
}

async function loadCvsSelection(orderNumber, activeViewVersion) {
  resetCvsSelection();
  if (!isCvsShipping.value) return;

  const response = await getAuctionCvsSelection(orderNumber);
  if (!isCurrentCheckout(orderNumber, activeViewVersion)) return;
  applyCvsSelection(response?.data?.data ?? { selected: false });
}

async function openCvsPicker() {
  if (!canOperateCheckout.value || isBusy.value) return;

  resetCvsStoreBrowser();
  selectedCvsStoreType.value = AUCTION_CVS_STORE_OPTIONS.some(
    (option) => option.value === cvsSelection.storeType,
  )
    ? cvsSelection.storeType
    : AUCTION_CVS_STORE_TYPE.UNIMART;
  isCvsPickerOpen.value = true;

  const orderNumber = String(order.value?.orderNumber || route.params.orderNumber || "");
  if (!orderNumber) return;
  await loadCvsStores(orderNumber, viewVersion);
}

function closeCvsPicker(force = false) {
  if (isSelectingCvsStore.value && !force) return;
  isCvsPickerOpen.value = false;
  resetCvsStoreBrowser();
}

async function loadCvsStores(
  orderNumber,
  activeViewVersion,
  storeType = selectedCvsStoreType.value,
) {
  const storeRequestVersion = ++cvsStoreRequestVersion;
  cvsStores.value = [];
  cvsStoreLoadError.value = "";
  cvsStoreSelectionError.value = "";
  isLoadingCvsStores.value = true;

  try {
    const response = await getAuctionCvsStores(orderNumber, storeType);
    if (
      storeRequestVersion !== cvsStoreRequestVersion ||
      !isCurrentCheckout(orderNumber, activeViewVersion) ||
      !isCvsPickerOpen.value ||
      selectedCvsStoreType.value !== storeType
    ) {
      return;
    }

    const stores = response?.data?.data;
    if (!Array.isArray(stores)) throw new Error("Invalid CVS store response");
    cvsStores.value = stores
      .map((store) => ({
        storeType: String(store?.storeType ?? ""),
        storeId: String(store?.storeId ?? ""),
        storeName: String(store?.storeName ?? ""),
        storeAddress: String(store?.storeAddress ?? ""),
        storePhone: String(store?.storePhone ?? ""),
      }))
      .filter((store) => store.storeType === storeType && store.storeId !== "");
  } catch (error) {
    if (
      storeRequestVersion !== cvsStoreRequestVersion ||
      !isCurrentCheckout(orderNumber, activeViewVersion) ||
      !isCvsPickerOpen.value
    ) {
      return;
    }
    const status = error.response?.status;
    if ((status === 401 || status === 403) && (await handleAuctionAccessError(error, router))) {
      return;
    }
    cvsStoreLoadError.value = "門市資料暫時無法取得，請稍後再試。";
  } finally {
    if (
      storeRequestVersion === cvsStoreRequestVersion &&
      isCurrentCheckout(orderNumber, activeViewVersion) &&
      isCvsPickerOpen.value
    ) {
      isLoadingCvsStores.value = false;
    }
  }
}

async function changeCvsStoreType(storeType) {
  if (
    isLoadingCvsStores.value ||
    !AUCTION_CVS_STORE_OPTIONS.some((option) => option.value === storeType)
  ) {
    return;
  }
  selectedCvsStoreType.value = storeType;
  const orderNumber = String(order.value?.orderNumber || route.params.orderNumber || "");
  if (!orderNumber) return;
  await loadCvsStores(orderNumber, viewVersion, storeType);
}

async function reloadCvsStores() {
  const orderNumber = String(order.value?.orderNumber || route.params.orderNumber || "");
  if (!orderNumber) return;
  await loadCvsStores(orderNumber, viewVersion);
}

function isCurrentCvsStore(store) {
  return (
    hasSelectedCvsStore.value &&
    cvsSelection.storeType === store.storeType &&
    String(cvsSelection.cvsStoreId) === String(store.storeId)
  );
}

function detailErrorMessage(error) {
  if (error.response?.status === 403) return "你沒有權限查看此訂單";
  if (error.response?.status === 404) return "找不到此競標訂單";
  return "訂單資料載入失敗，請稍後再試";
}

async function loadOrder() {
  stopLinePayPolling({ closeWindow: true });
  const activeViewVersion = ++viewVersion;
  const version = ++requestVersion;
  const orderNumber = String(route.params.orderNumber || "");
  deadlineRefreshOrderNumber = null;
  currentTimeMs.value = Date.now();
  loading.value = true;
  redirecting.value = false;
  errorMessage.value = "";
  operationError.value = "";
  saveMessage.value = "";
  routeNotice.value = "";
  order.value = null;
  memberEmail.value = "";
  Object.assign(memberInfo, { name: "", phone: "", address: "" });
  imageLoadFailed.value = false;
  resetCheckoutForms();
  resetCvsSelection();
  resetCvsStoreBrowser();
  isCvsPickerOpen.value = false;

  try {
    const response = await getAuctionOrderDetail(orderNumber);
    if (version !== requestVersion || !isCurrentCheckout(orderNumber, activeViewVersion)) return;
    const detail = response.data.data;
    order.value = detail;
    initializeShippingForm(detail);
    currentTimeMs.value = Date.now();

    if (shouldUseDetailPage(detail)) {
      await redirectToDetail(orderNumber);
      return;
    }

    await Promise.all([
      loadMember(orderNumber, activeViewVersion),
      loadCvsSelection(orderNumber, activeViewVersion),
    ]);
    await refreshExpiredOrderOnce();
  } catch (error) {
    if (!isCurrentCheckout(orderNumber, activeViewVersion)) return;
    if (error.response?.status === 401 && (await handleAuctionAccessError(error, router))) return;
    if (error.response?.status === 403 && (await handleAuctionAccessError(error, router))) return;
    errorMessage.value = detailErrorMessage(error);
  } finally {
    if (isCurrentCheckout(orderNumber, activeViewVersion)) loading.value = false;
  }
}

async function refreshOrderDetail(orderNumber) {
  const version = ++requestVersion;
  const response = await getAuctionOrderDetail(orderNumber);
  if (version !== requestVersion || String(route.params.orderNumber) !== String(orderNumber)) return false;
  const detail = response.data.data;
  order.value = detail;
  imageLoadFailed.value = false;
  currentTimeMs.value = Date.now();
  if (shouldUseDetailPage(detail)) {
    await redirectToDetail(orderNumber);
    return false;
  }
  return true;
}

async function refreshExpiredOrderOnce() {
  if (!isPendingPayment.value || !isPaymentDeadlineReached.value) return false;
  const orderNumber = String(order.value?.orderNumber || route.params.orderNumber || "");
  if (!orderNumber || deadlineRefreshOrderNumber === orderNumber) return false;
  deadlineRefreshOrderNumber = orderNumber;
  try {
    return await refreshOrderDetail(orderNumber);
  } catch (error) {
    if (error.response?.status === 401 || error.response?.status === 403) {
      await handleAuctionAccessError(error, router);
    }
    return false;
  }
}

async function tickPaymentDeadline() {
  currentTimeMs.value = Date.now();
  if (isPaymentDeadlineReached.value) await refreshExpiredOrderOnce();
}

function startPaymentDeadlineTimer() {
  if (countdownTimer !== null) clearInterval(countdownTimer);
  currentTimeMs.value = Date.now();
  countdownTimer = setInterval(tickPaymentDeadline, 1000);
}

function stopPaymentDeadlineTimer() {
  if (countdownTimer === null) return;
  clearInterval(countdownTimer);
  countdownTimer = null;
}

function applyCheckoutUpdate(payload, response) {
  if (!order.value) return;
  const updated = response?.data?.data ?? {};
  order.value = {
    ...order.value,
    receiverName: updated.receiverName ?? payload.receiverName,
    receiverPhone: updated.receiverPhone ?? payload.receiverPhone,
    shippingAddress: updated.shippingAddress ?? payload.shippingAddress,
    updatedAt: updated.updatedAt ?? order.value.updatedAt,
  };
  Object.assign(shippingForm, payload);
}

function unavailableMessage() {
  return deadlineNotice.value || "此訂單目前不可修改或付款。";
}

async function handleCheckoutUpdateError(error, orderNumber) {
  const status = error.response?.status;
  if ((status === 401 || status === 403) && (await handleAuctionAccessError(error, router))) return;
  if (status === 400) {
    operationError.value = error.response?.data?.message || "收件資料格式不正確，請重新確認。";
    return;
  }
  if (status === 404) {
    operationError.value = "找不到此競標訂單";
    return;
  }
  if (status === 409) {
    operationError.value = "訂單目前已不可修改，正在重新取得最新狀態。";
    try {
      await refreshOrderDetail(orderNumber);
    } catch {
      // 保留原錯誤；使用者可重新載入。
    }
    return;
  }
  operationError.value = "收件資料儲存失敗，請稍後再試。";
}

async function saveCheckout() {
  currentTimeMs.value = Date.now();
  if (isBusy.value) return false;
  operationError.value = "";
  saveMessage.value = "";
  if (!canOperateCheckout.value) {
    operationError.value = unavailableMessage();
    await refreshExpiredOrderOnce();
    return false;
  }
  if (!validateShippingForm()) return false;

  const orderNumber = String(order.value.orderNumber || route.params.orderNumber);
  const activeViewVersion = viewVersion;
  const payload = checkoutPayload();
  isSaving.value = true;
  try {
    const response = await updateAuctionOrderCheckout(orderNumber, payload);
    if (!isCurrentCheckout(orderNumber, activeViewVersion)) return false;
    applyCheckoutUpdate(payload, response);
    saveMessage.value = "收件資料已儲存。";
    return true;
  } catch (error) {
    if (!isCurrentCheckout(orderNumber, activeViewVersion)) return false;
    await handleCheckoutUpdateError(error, orderNumber);
    return false;
  } finally {
    isSaving.value = false;
  }
}

async function chooseCvsStore(store) {
  currentTimeMs.value = Date.now();
  operationError.value = "";
  saveMessage.value = "";
  cvsStoreSelectionError.value = "";
  if (isBusy.value || !canOperateCheckout.value) {
    operationError.value = unavailableMessage();
    await refreshExpiredOrderOnce();
    return;
  }

  if (
    !store ||
    !AUCTION_CVS_STORE_OPTIONS.some((option) => option.value === store.storeType) ||
    !cvsStores.value.some(
      (candidate) =>
        candidate.storeType === store.storeType &&
        String(candidate.storeId) === String(store.storeId),
    )
  ) {
    cvsStoreSelectionError.value = "門市資料無效，請重新查詢後再試。";
    return;
  }

  const orderNumber = String(order.value.orderNumber || route.params.orderNumber);
  const activeViewVersion = viewVersion;
  isSelectingCvsStore.value = true;
  try {
    const response = await selectAuctionCvsStore(orderNumber, {
      storeType: store.storeType,
      storeId: store.storeId,
    });
    if (!isCurrentCheckout(orderNumber, activeViewVersion)) return;
    applyCvsSelection(response?.data?.data ?? { selected: false });
    saveMessage.value = "取貨門市已更新。";
    closeCvsPicker(true);
  } catch (error) {
    if (!isCurrentCheckout(orderNumber, activeViewVersion)) return;
    const status = error.response?.status;
    if ((status === 401 || status === 403) && (await handleAuctionAccessError(error, router))) return;
    if (status === 404) {
      cvsStoreSelectionError.value = "找不到此門市，請重新查詢。";
    } else if (status === 409) {
      cvsStoreSelectionError.value = "訂單目前已不可選店，正在重新取得最新狀態。";
      try {
        await refreshOrderDetail(orderNumber);
      } catch {
        // 保留選店錯誤；使用者可重新載入。
      }
    } else {
      cvsStoreSelectionError.value = "門市選擇失敗，請稍後再試。";
    }
  } finally {
    if (isCurrentCheckout(orderNumber, activeViewVersion)) {
      isSelectingCvsStore.value = false;
    }
  }
}

function validatePaymentForm() {
  if (isCreditCard.value) return validateCreditCardForm();
  if (isTransfer.value) return validateTransferForm();
  if (isLinePay.value) return true;
  operationError.value = "目前付款方式無法處理，請稍後再試。";
  return false;
}

function paymentRequestErrorMessage(error) {
  const status = error.response?.status;
  const providerMessage = error.response?.data?.message;
  if (isLinePay.value && typeof providerMessage === "string" && providerMessage.trim()) {
    return providerMessage;
  }
  if (status === 403) return "你沒有權限付款此訂單";
  if (status === 404) return "找不到此競標訂單";
  if (status === 409) return "目前訂單狀態無法付款，正在重新取得最新狀態。";
  return "付款處理失敗，請稍後再試。";
}

async function reconcilePaymentState(orderNumber) {
  try {
    return await refreshOrderDetail(orderNumber);
  } catch (error) {
    if (error.response?.status === 401 || error.response?.status === 403) {
      await handleAuctionAccessError(error, router);
    }
    return false;
  }
}

async function confirmAndPay() {
  currentTimeMs.value = Date.now();
  if (isBusy.value) return;
  operationError.value = "";
  saveMessage.value = "";
  if (!canOperateCheckout.value) {
    operationError.value = unavailableMessage();
    await refreshExpiredOrderOnce();
    return;
  }
  if (!validateShippingForm() || !validatePaymentForm()) return;

  let paymentWindow = null;
  let linePayWindowHandedOff = false;
  if (isLinePay.value) {
    stopLinePayPolling({ closeWindow: true });
    routeNotice.value = "";
    try {
      paymentWindow = window.open(
        "about:blank",
        "sneakermarket-line-pay",
        "popup,width=520,height=760",
      );
    } catch {
      paymentWindow = null;
    }
    if (!paymentWindow) {
      operationError.value =
        "瀏覽器阻擋了 LINE Pay 付款視窗，請允許彈出式視窗後再試。";
      return;
    }
    linePayPaymentWindow = paymentWindow;
  }

  const orderNumber = String(order.value.orderNumber || route.params.orderNumber);
  const activeViewVersion = viewVersion;
  const payload = checkoutPayload();
  let phase = "checkout";
  isPaying.value = true;

  try {
    const checkoutResponse = await updateAuctionOrderCheckout(orderNumber, payload);
    if (!isCurrentCheckout(orderNumber, activeViewVersion)) return;
    applyCheckoutUpdate(payload, checkoutResponse);

    currentTimeMs.value = Date.now();
    if (!canOperateCheckout.value) {
      operationError.value = unavailableMessage();
      await refreshExpiredOrderOnce();
      return;
    }

    phase = "payment";
    if (isLinePay.value) {
      const response = await requestAuctionLinePay(orderNumber);
      if (!isCurrentCheckout(orderNumber, activeViewVersion)) return;
      const paymentUrl = response?.data?.data?.paymentUrl ?? response?.data?.paymentUrl;
      const merchantOrderId =
        response?.data?.data?.merchantOrderId ?? response?.data?.merchantOrderId;
      if (typeof paymentUrl !== "string" || paymentUrl.trim() === "") {
        throw new Error("LINE Pay 未回傳付款網址");
      }
      if (typeof merchantOrderId !== "string" || merchantOrderId.trim() === "") {
        throw new Error("LINE Pay 未回傳付款識別碼");
      }
      paymentWindow.location.href = paymentUrl.trim();
      startLinePayPolling(
        orderNumber,
        merchantOrderId.trim(),
        paymentWindow,
        activeViewVersion,
      );
      linePayWindowHandedOff = true;
      return;
    }

    await payAuctionOrder(orderNumber);
    if (!isCurrentCheckout(orderNumber, activeViewVersion)) return;
    await redirectToDetail(orderNumber);
  } catch (error) {
    if (!isCurrentCheckout(orderNumber, activeViewVersion)) return;
    if (paymentWindow && !linePayWindowHandedOff) {
      closeLinePayPaymentWindow(paymentWindow);
      paymentWindow = null;
    }
    const status = error.response?.status;
    if ((status === 401 || status === 403) && (await handleAuctionAccessError(error, router))) return;
    if (phase === "checkout") {
      await handleCheckoutUpdateError(error, orderNumber);
      return;
    }
    operationError.value = paymentRequestErrorMessage(error);
    if (status === 409 || status === undefined || status >= 500) {
      await reconcilePaymentState(orderNumber);
    }
  } finally {
    if (paymentWindow && !linePayWindowHandedOff) {
      closeLinePayPaymentWindow(paymentWindow);
    }
    isPaying.value = false;
  }
}

watch(
  () => route.params.orderNumber,
  (orderNumber, previousOrderNumber) => {
    if (orderNumber !== previousOrderNumber) {
      stopLinePayPolling({ closeWindow: true });
      loadOrder();
    }
  },
);

onMounted(() => {
  startPaymentDeadlineTimer();
  return loadOrder();
});

onUnmounted(() => {
  stopPaymentDeadlineTimer();
  stopLinePayPolling({ closeWindow: true });
  viewVersion++;
  requestVersion++;
  cvsStoreRequestVersion++;
});
</script>

<style scoped>
.auction-checkout-page { max-width: 1120px; margin: 0 auto; color: #111; }
.checkout-header { display: flex; align-items: end; justify-content: space-between; gap: 24px; margin-bottom: 28px; }
.eyebrow { margin: 0 0 6px; color: #6c757d; font-size: .75rem; font-weight: 700; letter-spacing: .12em; text-transform: uppercase; }
.checkout-header h1 { margin: 0; font-size: clamp(1.8rem, 4vw, 2.35rem); }
.back-link { border: 0; background: transparent; color: #111; font-weight: 650; text-decoration: underline; text-underline-offset: 4px; }
.page-state { border: 1px solid #e9ecef; border-radius: 10px; background: #fff; }
.page-state { padding: 38px; color: #6c757d; text-align: center; }
.page-state h2 { margin: 0 0 8px; color: #111; font-size: 1.2rem; }
.page-state--error { color: #842029; }
.state-actions { display: flex; justify-content: center; gap: 10px; margin-top: 18px; }
.checkout-layout { display: grid; grid-template-columns: minmax(0, 1fr) minmax(300px, .72fr); align-items: start; gap: 24px; }
.checkout-forms { display: grid; gap: 18px; min-width: 0; }
.notice { padding: 12px 14px; border-radius: 7px; font-size: .88rem; line-height: 1.55; }
.notice--error { background: #fbe9e7; color: #a12b21; }
.notice--success { background: #e7f4ec; color: #24613c; }
.checkout-actions { display: flex; justify-content: flex-end; gap: 10px; }
.primary-button, .secondary-button { display: inline-flex; align-items: center; justify-content: center; min-height: 42px; padding: 10px 17px; border: 1px solid #111; border-radius: 6px; font-weight: 700; text-decoration: none; }
.primary-button { background: #111; color: #fff; }
.secondary-button { background: #fff; color: #111; }
.primary-button:disabled, .secondary-button:disabled { border-color: #dee2e6; background: #f8f9fa; color: #adb5bd; cursor: not-allowed; }

@media (max-width: 991.98px) {
  .checkout-layout { grid-template-columns: 1fr; }
}

@media (max-width: 575.98px) {
  .checkout-header { align-items: start; flex-direction: column; }
  .page-state { padding: 20px; }
  .checkout-actions, .state-actions { align-items: stretch; flex-direction: column; }
  .primary-button, .secondary-button { width: 100%; }
}
</style>
