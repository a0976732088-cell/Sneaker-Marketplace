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

            <div v-if="routeNotice" class="notice notice--warning" role="status">
              {{ routeNotice }}
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

              <div v-if="!isCvsShipping" class="form-field form-field--full">
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

              <div v-else class="cvs-store-picker form-field--full">
                <span class="cvs-store-picker__label">取貨超商</span>

                <div class="cvs-selection-card">
                  <p class="cvs-selection-card__label">目前取貨門市</p>
                  <template v-if="hasSelectedCvsStore">
                    <strong>{{ selectedCvsStoreLabel }} {{ cvsSelection.cvsStoreName }}</strong>
                    <span>門市代碼：{{ displayValue(cvsSelection.cvsStoreId) }}</span>
                    <span>地址：{{ displayValue(cvsSelection.cvsAddress) }}</span>
                    <span v-if="cvsSelection.cvsTelephone">
                      電話：{{ cvsSelection.cvsTelephone }}
                    </span>
                  </template>
                  <p v-else class="cvs-selection-card__empty">未選擇門市</p>
                  <div class="cvs-selection-card__actions">
                    <button
                      v-if="hasSelectedCvsStore"
                      type="button"
                      class="secondary-button"
                      @click="openStoreMap(cvsSelection)"
                    >
                      查看地圖
                    </button>
                    <button
                      type="button"
                      class="secondary-button"
                      @click="openCvsPicker"
                    >
                      {{ hasSelectedCvsStore ? "更換門市" : "選擇門市" }}
                    </button>
                  </div>
                </div>

                <div
                  v-if="isCvsPickerOpen"
                  class="cvs-picker-modal"
                  role="presentation"
                  @click.self="closeCvsPicker()"
                >
                  <section
                    class="cvs-picker-modal__dialog"
                    role="dialog"
                    aria-modal="true"
                    aria-labelledby="cvs-picker-title"
                  >
                    <header class="cvs-picker-modal__header">
                      <div>
                        <p class="eyebrow">CVS pickup</p>
                        <h2 id="cvs-picker-title">選擇取貨門市</h2>
                      </div>
                      <button
                        type="button"
                        class="cvs-picker-modal__close"
                        aria-label="關閉門市選擇"
                        :disabled="isSelectingCvsStore"
                        @click="closeCvsPicker()"
                      >
                        ×
                      </button>
                    </header>

                    <div class="cvs-picker-modal__body">
                <div class="cvs-store-options" role="group" aria-label="選擇超商品牌">
                  <button
                    v-for="option in AUCTION_CVS_STORE_OPTIONS"
                    :key="option.value"
                    type="button"
                    class="cvs-store-option"
                    :class="{ 'cvs-store-option--active': selectedCvsStoreType === option.value }"
                    :aria-pressed="selectedCvsStoreType === option.value"
                    :disabled="isLoadingCvsStores || isSelectingCvsStore"
                    @click="changeCvsStoreType(option.value)"
                  >
                    {{ option.label }}
                  </button>
                </div>

                <div class="cvs-store-search">
                  <div class="form-field">
                    <label for="auction-cvs-city">縣市</label>
                    <select id="auction-cvs-city" v-model="cvsStoreFilters.city">
                      <option value="">全部縣市</option>
                      <option v-for="city in TAIWAN_CITIES" :key="city" :value="city">
                        {{ city }}
                      </option>
                    </select>
                  </div>
                  <div class="form-field">
                    <label for="auction-cvs-keyword">搜尋門市</label>
                    <input
                      id="auction-cvs-keyword"
                      v-model="cvsStoreFilters.keyword"
                      type="search"
                      maxlength="80"
                      placeholder="門市名稱、店號或地址"
                      autocomplete="off"
                    />
                  </div>
                </div>

                <p v-if="isLoadingCvsStores" class="cvs-store-state" role="status">
                  門市資料載入中…
                </p>
                <div v-else-if="cvsStoreLoadError" class="notice notice--error" role="alert">
                  <p>{{ cvsStoreLoadError }}</p>
                  <button type="button" class="secondary-button" @click="reloadCvsStores">
                    重新載入門市
                  </button>
                </div>
                <p v-else-if="!hasCvsSearchCriteria" class="cvs-store-state">
                  請先選擇縣市或輸入門市關鍵字。
                </p>
                <p v-else-if="filteredCvsStores.length === 0" class="cvs-store-state">
                  查無符合條件的門市。
                </p>
                <template v-else>
                  <p v-if="hasTooManyCvsStores" class="notice notice--warning">
                    符合門市過多，請縮小搜尋範圍。
                  </p>
                  <p class="cvs-store-result-count">
                    顯示 {{ filteredCvsStores.length }} 筆門市（最多 100 筆）
                  </p>
                  <ul class="cvs-store-results" aria-label="超商門市搜尋結果">
                    <li v-for="store in filteredCvsStores" :key="`${store.storeType}-${store.storeId}`">
                      <div class="cvs-store-result__details">
                        <strong>{{ store.storeName }}</strong>
                        <span>門市代碼：{{ store.storeId }}</span>
                        <span>地址：{{ store.storeAddress }}</span>
                        <span v-if="store.storePhone">電話：{{ store.storePhone }}</span>
                      </div>
                      <div class="cvs-store-result__actions">
                        <button
                          type="button"
                          class="secondary-button"
                          @click="openStoreMap(store)"
                        >
                          查看地圖
                        </button>
                        <button
                          type="button"
                          class="secondary-button"
                          :disabled="isSelectingCvsStore || isCurrentCvsStore(store)"
                          @click="chooseCvsStore(store)"
                        >
                          {{ isCurrentCvsStore(store) ? "已選擇" : "選擇此門市" }}
                        </button>
                      </div>
                    </li>
                  </ul>
                </template>

                <p v-if="cvsStoreSelectionError" class="field-error" role="alert">
                  {{ cvsStoreSelectionError }}
                </p>
                    </div>

                    <footer class="cvs-picker-modal__footer">
                      <button
                        type="button"
                        class="secondary-button"
                        :disabled="isSelectingCvsStore"
                        @click="closeCvsPicker()"
                      >
                        取消
                      </button>
                    </footer>
                  </section>
                </div>
                <p v-if="!hasSelectedCvsStore" class="field-hint">
                  請先選擇超商取貨門市
                </p>
                <p v-if="shippingErrors.shippingAddress" class="field-error">
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

            <div v-else-if="isLinePay" class="line-pay-instructions">
              <strong>LINE Pay</strong>
              <p v-if="isWaitingForLinePay">
                等待 LINE Pay 完成付款…<br />請在 LINE Pay 付款視窗完成操作。
              </p>
              <p v-else>
                將開啟 LINE Pay 付款視窗。<br />付款完成後會自動前往競標訂單。
              </p>
            </div>

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
  AUCTION_CVS_RESULT_LIMIT,
  AUCTION_CVS_STORE_OPTIONS,
  AUCTION_CVS_STORE_TYPE,
} from "@/constants/auctionCvs";
import { TAIWAN_CITIES } from "@/constants/auctionFormOptions";
import {
  AUCTION_PAYMENT_METHOD,
  AUCTION_TRANSFER_ACCOUNT,
} from "@/constants/auctionPayment";
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

function displayValue(value) {
  return value === null || value === undefined || value === "" ? "—" : value;
}

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
.form-field input, .form-field select, .form-field textarea { width: 100%; min-width: 0; padding: 11px 12px; border: 1px solid #ced4da; border-radius: 6px; background: #fff; color: #111; }
.form-field input:focus, .form-field select:focus, .form-field textarea:focus { border-color: #111; outline: 2px solid rgba(17,17,17,.12); }
.form-field input:disabled, .form-field select:disabled, .form-field textarea:disabled, .form-field input[readonly] { background: #f8f9fa; color: #6c757d; }
.form-field [aria-invalid="true"] { border-color: #b42318; }
.field-hint, .field-error, .simulation-note { margin: 7px 0 0; font-size: .78rem; line-height: 1.5; }
.field-hint, .simulation-note { color: #6c757d; }
.field-error { color: #b42318; }
.cvs-store-picker { display: grid; gap: 14px; }
.cvs-store-picker__label { color: #343a40; font-size: .85rem; font-weight: 650; }
.cvs-store-options { display: flex; flex-wrap: wrap; gap: 8px; }
.cvs-store-option { min-height: 38px; padding: 8px 13px; border: 1px solid #ced4da; border-radius: 999px; background: #fff; color: #343a40; font-weight: 650; }
.cvs-store-option--active { border-color: #111; background: #111; color: #fff; }
.cvs-store-option:disabled { cursor: not-allowed; opacity: .6; }
.cvs-store-search { display: grid; grid-template-columns: minmax(150px, .45fr) minmax(0, 1fr); gap: 12px; }
.cvs-store-state, .cvs-store-result-count { margin: 0; color: #6c757d; font-size: .84rem; line-height: 1.5; }
.cvs-store-results { display: grid; gap: 10px; max-height: 430px; margin: 0; padding: 0; overflow-y: auto; list-style: none; }
.cvs-store-results li { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 14px; border: 1px solid #e9ecef; border-radius: 8px; }
.cvs-store-result__details { display: grid; min-width: 0; gap: 4px; }
.cvs-store-result__actions { display: flex; flex: 0 0 auto; flex-wrap: wrap; gap: 8px; }
.cvs-store-results strong, .cvs-store-results span { overflow-wrap: anywhere; }
.cvs-store-results span { color: #6c757d; font-size: .8rem; line-height: 1.45; }
.cvs-store-results .secondary-button { flex: 0 0 auto; }
.cvs-picker-modal .notice p { margin: 0 0 10px; }
.cvs-selection-card { display: grid; gap: 5px; padding: 15px; border: 1px solid #e9ecef; border-radius: 8px; background: #f8f9fa; }
.cvs-selection-card__label, .cvs-selection-card__empty { margin: 0; color: #6c757d; font-size: .8rem; }
.cvs-selection-card strong { overflow-wrap: anywhere; }
.cvs-selection-card span { color: #495057; font-size: .84rem; line-height: 1.45; overflow-wrap: anywhere; }
.cvs-selection-card__actions { display: flex; flex-wrap: wrap; gap: 8px; justify-self: start; margin-top: 8px; }
.cvs-picker-modal { position: fixed; z-index: 1100; inset: 0; display: grid; place-items: center; padding: 24px; background: rgba(0, 0, 0, .52); }
.cvs-picker-modal__dialog { display: flex; width: min(760px, 100%); max-height: min(820px, calc(100vh - 48px)); flex-direction: column; overflow: hidden; border-radius: 12px; background: #fff; box-shadow: 0 22px 70px rgba(0, 0, 0, .24); }
.cvs-picker-modal__header { display: flex; align-items: start; justify-content: space-between; gap: 24px; padding: 22px 24px; border-bottom: 1px solid #e9ecef; }
.cvs-picker-modal__header h2 { margin: 0; font-size: 1.25rem; }
.cvs-picker-modal__close { display: grid; width: 38px; height: 38px; padding: 0; place-items: center; border: 0; border-radius: 50%; background: transparent; color: #343a40; font-size: 1.7rem; line-height: 1; }
.cvs-picker-modal__close:hover { background: #f1f3f5; }
.cvs-picker-modal__close:disabled { cursor: not-allowed; opacity: .5; }
.cvs-picker-modal__body { display: grid; min-height: 0; gap: 16px; padding: 22px 24px; overflow-y: auto; }
.cvs-picker-modal__footer { display: flex; justify-content: flex-end; padding: 16px 24px; border-top: 1px solid #e9ecef; }
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
.line-pay-instructions { padding: 18px; border: 1px solid #e9ecef; border-radius: 8px; background: #f8f9fa; }
.line-pay-instructions p { margin: 8px 0 0; color: #495057; line-height: 1.6; }
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
  .cvs-store-search { grid-template-columns: 1fr; }
  .cvs-store-results li { align-items: stretch; flex-direction: column; }
  .cvs-picker-modal { padding: 12px; }
  .cvs-picker-modal__dialog { max-height: calc(100vh - 24px); }
  .cvs-picker-modal__header, .cvs-picker-modal__body { padding: 18px; }
  .cvs-picker-modal__footer { padding: 14px 18px; }
  .checkout-actions, .state-actions { align-items: stretch; flex-direction: column; }
  .primary-button, .secondary-button { width: 100%; }
  .summary-product { grid-template-columns: 82px minmax(0, 1fr); }
  .summary-image-wrap { width: 82px; }
}
</style>
