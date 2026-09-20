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
          <section class="checkout-panel" aria-labelledby="shipping-heading">
            <div class="panel-heading">
              <div>
                <p class="panel-step">01</p>
                <h2 id="shipping-heading">收件資訊</h2>
              </div>
              <button
                type="button"
                class="text-button"
                :disabled="!canOperateCheckout || isBusy || !hasMemberProfile"
                @click="fillFromMember"
              >
                帶入目前會員資料
              </button>
            </div>

            <div v-if="deadlineNotice" class="notice notice--warning" role="status">
              {{ deadlineNotice }}
            </div>

            <fieldset class="form-grid" :disabled="!canOperateCheckout || isBusy">
              <div class="form-field form-field--full">
                <label for="auction-checkout-email">Email</label>
                <input
                  id="auction-checkout-email"
                  type="email"
                  :value="memberEmail"
                  autocomplete="email"
                  readonly
                />
                <p class="field-hint">Email 僅供顯示，不會寫入競標訂單。</p>
              </div>

              <div class="form-field">
                <label for="auction-receiver-name">收件人</label>
                <input
                  id="auction-receiver-name"
                  v-model="shippingForm.receiverName"
                  type="text"
                  maxlength="50"
                  autocomplete="name"
                  :aria-invalid="Boolean(shippingErrors.receiverName)"
                  :aria-describedby="shippingErrors.receiverName ? 'auction-receiver-name-error' : undefined"
                  @input="shippingErrors.receiverName = ''"
                />
                <p
                  v-if="shippingErrors.receiverName"
                  id="auction-receiver-name-error"
                  class="field-error"
                >
                  {{ shippingErrors.receiverName }}
                </p>
              </div>

              <div class="form-field">
                <label for="auction-receiver-phone">電話</label>
                <input
                  id="auction-receiver-phone"
                  v-model="shippingForm.receiverPhone"
                  type="tel"
                  maxlength="20"
                  autocomplete="tel"
                  :aria-invalid="Boolean(shippingErrors.receiverPhone)"
                  :aria-describedby="shippingErrors.receiverPhone ? 'auction-receiver-phone-error' : undefined"
                  @input="shippingErrors.receiverPhone = ''"
                />
                <p
                  v-if="shippingErrors.receiverPhone"
                  id="auction-receiver-phone-error"
                  class="field-error"
                >
                  {{ shippingErrors.receiverPhone }}
                </p>
              </div>

              <div class="form-field form-field--full">
                <label for="auction-shipping-address">配送地址</label>
                <textarea
                  id="auction-shipping-address"
                  v-model="shippingForm.shippingAddress"
                  rows="3"
                  maxlength="300"
                  autocomplete="street-address"
                  :aria-invalid="Boolean(shippingErrors.shippingAddress)"
                  :aria-describedby="shippingErrors.shippingAddress ? 'auction-shipping-address-error' : undefined"
                  @input="shippingErrors.shippingAddress = ''"
                ></textarea>
                <p
                  v-if="shippingErrors.shippingAddress"
                  id="auction-shipping-address-error"
                  class="field-error"
                >
                  {{ shippingErrors.shippingAddress }}
                </p>
              </div>

              <div class="readonly-fact form-field--full">
                <span>配送方式</span>
                <strong>{{ displayValue(order.shippingMethod) }}</strong>
              </div>
            </fieldset>
          </section>

          <section class="checkout-panel" aria-labelledby="payment-heading">
            <div class="panel-heading">
              <div>
                <p class="panel-step">02</p>
                <h2 id="payment-heading">付款資訊</h2>
              </div>
              <span class="payment-method-label">{{ paymentMethodLabel }}</span>
            </div>

            <fieldset
              v-if="isCreditCard"
              class="form-grid"
              :disabled="!canOperateCheckout || isBusy"
            >
              <div class="form-field form-field--full">
                <label for="auction-card-holder">持卡人姓名</label>
                <input
                  id="auction-card-holder"
                  v-model="cardForm.cardHolderName"
                  type="text"
                  autocomplete="cc-name"
                  :aria-invalid="Boolean(cardErrors.cardHolderName)"
                  :aria-describedby="cardErrors.cardHolderName ? 'auction-card-holder-error' : undefined"
                  @input="cardErrors.cardHolderName = ''"
                />
                <p v-if="cardErrors.cardHolderName" id="auction-card-holder-error" class="field-error">
                  {{ cardErrors.cardHolderName }}
                </p>
              </div>

              <div class="form-field form-field--full">
                <label for="auction-card-number">信用卡號</label>
                <input
                  id="auction-card-number"
                  :value="cardForm.cardNumber"
                  type="text"
                  inputmode="numeric"
                  maxlength="19"
                  autocomplete="cc-number"
                  placeholder="0000 0000 0000 0000"
                  :aria-invalid="Boolean(cardErrors.cardNumber)"
                  :aria-describedby="cardErrors.cardNumber ? 'auction-card-number-error' : undefined"
                  @input="handleCardNumberInput"
                />
                <p v-if="cardErrors.cardNumber" id="auction-card-number-error" class="field-error">
                  {{ cardErrors.cardNumber }}
                </p>
              </div>

              <div class="form-field">
                <label for="auction-card-expiry">有效期限</label>
                <input
                  id="auction-card-expiry"
                  :value="cardForm.cardExpiry"
                  type="text"
                  inputmode="numeric"
                  maxlength="5"
                  autocomplete="cc-exp"
                  placeholder="MM/YY"
                  :aria-invalid="Boolean(cardErrors.cardExpiry)"
                  :aria-describedby="cardErrors.cardExpiry ? 'auction-card-expiry-error' : undefined"
                  @input="handleCardExpiryInput"
                />
                <p v-if="cardErrors.cardExpiry" id="auction-card-expiry-error" class="field-error">
                  {{ cardErrors.cardExpiry }}
                </p>
              </div>

              <div class="form-field">
                <label for="auction-card-cvv">安全碼</label>
                <input
                  id="auction-card-cvv"
                  :value="cardForm.cardCvv"
                  type="password"
                  inputmode="numeric"
                  maxlength="4"
                  autocomplete="cc-csc"
                  placeholder="CVV"
                  :aria-invalid="Boolean(cardErrors.cardCvv)"
                  :aria-describedby="cardErrors.cardCvv ? 'auction-card-cvv-error' : undefined"
                  @input="handleCardCvvInput"
                />
                <p v-if="cardErrors.cardCvv" id="auction-card-cvv-error" class="field-error">
                  {{ cardErrors.cardCvv }}
                </p>
              </div>

              <p class="simulation-note form-field--full">
                此為模擬付款，卡片資料只用於本頁格式驗證，不會傳送或儲存。
              </p>
            </fieldset>

            <fieldset
              v-else-if="isTransfer"
              class="form-grid"
              :disabled="!canOperateCheckout || isBusy"
            >
              <div class="transfer-instructions form-field--full">
                <p class="simulation-label">Demo／模擬轉帳資訊</p>
                <dl>
                  <div><dt>銀行名稱</dt><dd>{{ AUCTION_TRANSFER_ACCOUNT.bankName }}</dd></div>
                  <div><dt>銀行代碼</dt><dd>{{ AUCTION_TRANSFER_ACCOUNT.bankCode }}</dd></div>
                  <div><dt>銀行帳號</dt><dd>{{ AUCTION_TRANSFER_ACCOUNT.accountNumber }}</dd></div>
                  <div><dt>戶名</dt><dd>{{ AUCTION_TRANSFER_ACCOUNT.accountName }}</dd></div>
                  <div><dt>應付金額</dt><dd>{{ formatTwd(order.totalAmount) }}</dd></div>
                </dl>
              </div>

              <div class="form-field">
                <label for="auction-remitter-name">匯款人姓名</label>
                <input
                  id="auction-remitter-name"
                  v-model="transferForm.remitterName"
                  type="text"
                  autocomplete="off"
                  :aria-invalid="Boolean(transferErrors.remitterName)"
                  :aria-describedby="transferErrors.remitterName ? 'auction-remitter-name-error' : undefined"
                  @input="transferErrors.remitterName = ''"
                />
                <p v-if="transferErrors.remitterName" id="auction-remitter-name-error" class="field-error">
                  {{ transferErrors.remitterName }}
                </p>
              </div>

              <div class="form-field">
                <label for="auction-account-last-five">轉帳帳號末五碼</label>
                <input
                  id="auction-account-last-five"
                  :value="transferForm.accountLastFive"
                  type="text"
                  inputmode="numeric"
                  maxlength="5"
                  autocomplete="off"
                  placeholder="12345"
                  :aria-invalid="Boolean(transferErrors.accountLastFive)"
                  :aria-describedby="transferErrors.accountLastFive ? 'auction-account-last-five-error' : undefined"
                  @input="handleAccountLastFiveInput"
                />
                <p v-if="transferErrors.accountLastFive" id="auction-account-last-five-error" class="field-error">
                  {{ transferErrors.accountLastFive }}
                </p>
              </div>

              <p class="simulation-note form-field--full">
                此為模擬轉帳確認，匯款人與末五碼不會傳送或儲存。
              </p>
            </fieldset>

            <div v-else class="notice notice--error" role="alert">
              目前付款方式無法處理，請稍後再試。
            </div>
          </section>

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

        <aside class="order-summary" aria-labelledby="summary-heading">
          <h2 id="summary-heading">訂單摘要</h2>

          <div class="summary-product">
            <div class="summary-image-wrap">
              <img
                v-if="productImageUrl"
                :src="productImageUrl"
                :alt="`${order.productName || '競標商品'} 商品圖片`"
                class="summary-image"
                @error="imageLoadFailed = true"
              />
              <div v-else class="summary-image summary-image--fallback" role="img" aria-label="目前沒有商品圖片">
                暫無圖片
              </div>
            </div>
            <div>
              <p class="summary-brand">{{ order.brandName || "品牌資料未提供" }}</p>
              <h3>{{ order.productName || "未命名商品" }}</h3>
              <p class="summary-meta">{{ productMeta }}</p>
            </div>
          </div>

          <dl class="amount-list">
            <div><dt>成交價格</dt><dd>{{ formatTwd(order.finalPrice) }}</dd></div>
            <div><dt>運費</dt><dd>{{ formatTwd(order.shippingFee) }}</dd></div>
            <div class="amount-total"><dt>訂單總額</dt><dd>{{ formatTwd(order.totalAmount) }}</dd></div>
          </dl>

          <div class="deadline-card">
            <span>付款期限</span>
            <strong>{{ formatAuctionTime(order.paymentDueAt) }}</strong>
            <p v-if="canOperateCheckout">剩餘 {{ remainingPaymentTimeText }}</p>
            <p v-else>{{ deadlineNotice || "此訂單目前不可付款" }}</p>
          </div>

          <p class="summary-order-number">訂單 {{ order.orderNumber }}</p>
        </aside>
      </div>
    </template>
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";

import {
  AUCTION_PAYMENT_METHOD,
  AUCTION_TRANSFER_ACCOUNT,
} from "@/constants/auctionPayment";
import { getCurrentMember } from "@/service/authService";
import {
  getAuctionOrderDetail,
  handleAuctionAccessError,
  payAuctionOrder,
  updateAuctionOrderCheckout,
} from "@/service/auctionService";
import { resolveAuctionImageUrl } from "@/utils/auctionImageUrl";
import { formatTwd } from "@/utils/auctionMoney";
import { formatAuctionTime, parseAuctionTime } from "@/utils/auctionTime";
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
const isSaving = ref(false);
const isPaying = ref(false);
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

let requestVersion = 0;
let viewVersion = 0;
let countdownTimer = null;
let deadlineRefreshOrderNumber = null;

const isBuyer = computed(() => order.value?.viewerRole === "BUYER");
const isPendingPayment = computed(
  () => order.value?.paymentStatus === "PENDING" && order.value?.orderStatus === "PENDING_PAYMENT",
);
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
const isSupportedPaymentMethod = computed(() => isCreditCard.value || isTransfer.value);
const isBusy = computed(() => isSaving.value || isPaying.value);
const canSubmitPayment = computed(
  () => canOperateCheckout.value && isSupportedPaymentMethod.value && !isBusy.value,
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
const paymentMethodLabel = computed(() => {
  if (isCreditCard.value) return "信用卡";
  if (isTransfer.value) return "銀行轉帳";
  return order.value?.paymentMethod || "未知付款方式";
});
const paymentButtonText = computed(() => {
  if (isPaying.value) return "付款處理中…";
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

function displayValue(value) {
  return value === null || value === undefined || value === "" ? "—" : value;
}

function applyErrors(target, keys, errors) {
  for (const key of keys) target[key] = errors[key] ?? "";
  return keys.every((key) => !target[key]);
}

function validateShippingForm() {
  return applyErrors(
    shippingErrors,
    ["receiverName", "receiverPhone", "shippingAddress"],
    validateShippingFormValues(shippingForm),
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
  Object.assign(shippingForm, {
    receiverName: memberInfo.name ?? "",
    receiverPhone: memberInfo.phone ?? "",
    shippingAddress: memberInfo.address ?? "",
  });
  applyErrors(shippingErrors, ["receiverName", "receiverPhone", "shippingAddress"], {});
}

function checkoutPayload() {
  return {
    receiverName: shippingForm.receiverName.trim(),
    receiverPhone: shippingForm.receiverPhone.trim(),
    shippingAddress: shippingForm.shippingAddress.trim(),
  };
}

function isCurrentCheckout(orderNumber, activeViewVersion) {
  return activeViewVersion === viewVersion && String(route.params.orderNumber) === String(orderNumber);
}

function shouldUseDetailPage(detail) {
  return detail?.viewerRole !== "BUYER" || detail?.paymentStatus !== "PENDING" || detail?.orderStatus !== "PENDING_PAYMENT";
}

async function redirectToDetail(orderNumber) {
  redirecting.value = true;
  await router.replace({ name: "auction-order-detail", params: { orderNumber } });
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

function detailErrorMessage(error) {
  if (error.response?.status === 403) return "你沒有權限查看此訂單";
  if (error.response?.status === 404) return "找不到此競標訂單";
  return "訂單資料載入失敗，請稍後再試";
}

async function loadOrder() {
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
  order.value = null;
  memberEmail.value = "";
  Object.assign(memberInfo, { name: "", phone: "", address: "" });
  imageLoadFailed.value = false;
  resetCheckoutForms();

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

    await loadMember(orderNumber, activeViewVersion);
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

function validatePaymentForm() {
  if (isCreditCard.value) return validateCreditCardForm();
  if (isTransfer.value) return validateTransferForm();
  operationError.value = "目前付款方式無法處理，請稍後再試。";
  return false;
}

function paymentRequestErrorMessage(error) {
  const status = error.response?.status;
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
    await payAuctionOrder(orderNumber);
    if (!isCurrentCheckout(orderNumber, activeViewVersion)) return;
    await redirectToDetail(orderNumber);
  } catch (error) {
    if (!isCurrentCheckout(orderNumber, activeViewVersion)) return;
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
    isPaying.value = false;
  }
}

watch(
  () => route.params.orderNumber,
  (orderNumber, previousOrderNumber) => {
    if (orderNumber !== previousOrderNumber) loadOrder();
  },
);

onMounted(() => {
  startPaymentDeadlineTimer();
  return loadOrder();
});

onUnmounted(() => {
  stopPaymentDeadlineTimer();
  viewVersion++;
  requestVersion++;
});
</script>

<style scoped>
.auction-checkout-page { max-width: 1120px; margin: 0 auto; color: #111; }
.checkout-header { display: flex; align-items: end; justify-content: space-between; gap: 24px; margin-bottom: 28px; }
.eyebrow, .panel-step { margin: 0 0 6px; color: #6c757d; font-size: .75rem; font-weight: 700; letter-spacing: .12em; text-transform: uppercase; }
.checkout-header h1 { margin: 0; font-size: clamp(1.8rem, 4vw, 2.35rem); }
.back-link, .text-button { border: 0; background: transparent; color: #111; font-weight: 650; text-decoration: underline; text-underline-offset: 4px; }
.text-button:disabled { color: #adb5bd; cursor: not-allowed; }
.page-state, .checkout-panel, .order-summary { border: 1px solid #e9ecef; border-radius: 10px; background: #fff; }
.page-state { padding: 38px; color: #6c757d; text-align: center; }
.page-state h2 { margin: 0 0 8px; color: #111; font-size: 1.2rem; }
.page-state--error { color: #842029; }
.state-actions { display: flex; justify-content: center; gap: 10px; margin-top: 18px; }
.checkout-layout { display: grid; grid-template-columns: minmax(0, 1fr) minmax(300px, .72fr); align-items: start; gap: 24px; }
.checkout-forms { display: grid; gap: 18px; min-width: 0; }
.checkout-panel { padding: 26px; }
.panel-heading { display: flex; align-items: start; justify-content: space-between; gap: 20px; margin-bottom: 22px; }
.panel-heading h2, .order-summary h2 { margin: 0; font-size: 1.15rem; }
.payment-method-label { padding: 5px 10px; border-radius: 999px; background: #f1f3f5; color: #343a40; font-size: .8rem; font-weight: 700; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; min-width: 0; margin: 0; padding: 0; border: 0; }
.form-field { min-width: 0; }
.form-field--full { grid-column: 1 / -1; }
.form-field label { display: block; margin-bottom: 7px; color: #343a40; font-size: .85rem; font-weight: 650; }
.form-field input, .form-field textarea { width: 100%; min-width: 0; padding: 11px 12px; border: 1px solid #ced4da; border-radius: 6px; background: #fff; color: #111; }
.form-field input:focus, .form-field textarea:focus { border-color: #111; outline: 2px solid rgba(17,17,17,.12); }
.form-field input:disabled, .form-field textarea:disabled, .form-field input[readonly] { background: #f8f9fa; color: #6c757d; }
.form-field [aria-invalid="true"] { border-color: #b42318; }
.field-hint, .field-error, .simulation-note { margin: 7px 0 0; font-size: .78rem; line-height: 1.5; }
.field-hint, .simulation-note { color: #6c757d; }
.field-error { color: #b42318; }
.readonly-fact { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 14px; border-radius: 7px; background: #f8f9fa; color: #6c757d; }
.readonly-fact strong { color: #111; }
.notice { padding: 12px 14px; border-radius: 7px; font-size: .88rem; line-height: 1.55; }
.notice--warning { margin-bottom: 18px; background: #fff3cd; color: #765a00; }
.notice--error { background: #fbe9e7; color: #a12b21; }
.notice--success { background: #e7f4ec; color: #24613c; }
.transfer-instructions { padding: 18px; border: 1px solid #e9ecef; border-radius: 8px; background: #f8f9fa; }
.simulation-label { margin: 0 0 12px; font-weight: 700; }
.transfer-instructions dl { display: grid; gap: 9px; margin: 0; }
.transfer-instructions dl div { display: flex; justify-content: space-between; gap: 20px; }
.transfer-instructions dt { color: #6c757d; font-weight: 500; }
.transfer-instructions dd { margin: 0; font-weight: 650; text-align: right; overflow-wrap: anywhere; }
.checkout-actions { display: flex; justify-content: flex-end; gap: 10px; }
.primary-button, .secondary-button { display: inline-flex; align-items: center; justify-content: center; min-height: 42px; padding: 10px 17px; border: 1px solid #111; border-radius: 6px; font-weight: 700; text-decoration: none; }
.primary-button { background: #111; color: #fff; }
.secondary-button { background: #fff; color: #111; }
.primary-button:disabled, .secondary-button:disabled { border-color: #dee2e6; background: #f8f9fa; color: #adb5bd; cursor: not-allowed; }
.order-summary { position: sticky; top: 76px; padding: 24px; }
.summary-product { display: grid; grid-template-columns: 92px minmax(0, 1fr); gap: 15px; margin: 20px 0; }
.summary-image-wrap { width: 92px; aspect-ratio: 4 / 3; overflow: hidden; border-radius: 7px; background: #f1f3f5; }
.summary-image { width: 100%; height: 100%; object-fit: cover; }
.summary-image--fallback { display: grid; place-items: center; color: #868e96; font-size: .72rem; }
.summary-brand, .summary-meta { margin: 0; color: #6c757d; font-size: .8rem; }
.summary-product h3 { margin: 4px 0 7px; font-size: 1rem; line-height: 1.4; }
.amount-list { display: grid; gap: 11px; margin: 0; padding-top: 18px; border-top: 1px solid #e9ecef; }
.amount-list div { display: flex; justify-content: space-between; gap: 16px; }
.amount-list dt { color: #6c757d; font-weight: 500; }
.amount-list dd { margin: 0; font-weight: 650; }
.amount-list .amount-total { margin-top: 4px; padding-top: 14px; border-top: 1px solid #e9ecef; color: #111; font-size: 1.05rem; }
.deadline-card { margin-top: 20px; padding: 15px; border-radius: 8px; background: #f8f9fa; }
.deadline-card span { display: block; color: #6c757d; font-size: .78rem; }
.deadline-card strong { display: block; margin-top: 4px; }
.deadline-card p { margin: 7px 0 0; color: #6c757d; font-size: .78rem; line-height: 1.45; }
.summary-order-number { margin: 18px 0 0; color: #6c757d; font-size: .78rem; overflow-wrap: anywhere; }

@media (max-width: 991.98px) {
  .checkout-layout { grid-template-columns: 1fr; }
  .order-summary { position: static; grid-row: 1; }
}

@media (max-width: 575.98px) {
  .checkout-header { align-items: start; flex-direction: column; }
  .checkout-panel, .order-summary, .page-state { padding: 20px; }
  .panel-heading { flex-direction: column; gap: 10px; }
  .form-grid { grid-template-columns: 1fr; }
  .form-field--full { grid-column: auto; }
  .checkout-actions, .state-actions { align-items: stretch; flex-direction: column; }
  .primary-button, .secondary-button { width: 100%; }
  .summary-product { grid-template-columns: 82px minmax(0, 1fr); }
  .summary-image-wrap { width: 82px; }
}
</style>
