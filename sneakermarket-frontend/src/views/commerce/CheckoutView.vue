<script setup>
import { ref, onMounted, computed } from 'vue'
import httpClient from '@/service/httpClient'
import { getCurrentMember } from '@/service/authService'
import { useRouter } from 'vue-router'
import CheckoutSteps from '@/components/commerce/CheckoutSteps.vue'
import StoreMap from '@/views/commerce/StoreMap.vue'
import { resolveProductImageUrl } from '@/utils/productImageUrl'

const router = useRouter()

// ===== 會員資料 =====
const memberId = ref(null)
const memberEmail = ref('')
const memberInfo = ref({ name: '', phone: '', address: '' })
const useMemberInfo = ref(false)

// ===== 購物車資料 =====
const cartItems = ref([])
const cartLoading = ref(true)

// ===== 寄送資訊表單 =====
const receiverName = ref('')
const receiverPhone = ref('')
const shippingAddress = ref('')

// ===== 取貨方式 =====
const shippingMethod = ref('') // 預設不選，HOME_DELIVERY 或 CVS
const cvsStoreId = ref('')
const cvsStoreName = ref('')
const cvsStoreAddress = ref('')

// ===== 付款方式 =====
const paymentMethod = ref('') // 預設不選，ECPAY_CREDIT_CARD 或 LINE_PAY

// 地圖選好門市後，把資料寫回同一組變數
function handleStoreSelected(store) {
  cvsStoreId.value = store.storeId
  cvsStoreName.value = store.storeName
  cvsStoreAddress.value = store.address
}

// 按「更換門市」：清空目前選擇，畫面會自動切回顯示地圖
function resetStoreSelection() {
  cvsStoreId.value = ''
  cvsStoreName.value = ''
  cvsStoreAddress.value = ''
}

// ===== 優惠券 =====
const coupons = ref([])
const couponId = ref('')

const submitting = ref(false)
const formError = ref('')

const selectedItems = computed(() => cartItems.value.filter(item => item.selected))

const subtotal = computed(() => {
  let total = 0
  for (const item of selectedItems.value) {
    total += item.price * item.quantity
  }
  return total
})

const shippingFee = computed(() => {
  return subtotal.value === 0 ? 0 : 100
})

// 選到的優惠券；有達到滿額門檻才折抵（門檻以商品小計計算，不含運費）
const selectedCoupon = computed(() => {
  return coupons.value.find(c => String(c.couponId) === String(couponId.value)) || null
})

const couponNotReached = computed(() => {
  return !!selectedCoupon.value && subtotal.value < Number(selectedCoupon.value.minSpend)
})

const discountAmount = computed(() => {
  if (!selectedCoupon.value || couponNotReached.value) return 0
  return Number(selectedCoupon.value.discountAmount)
})

// 對應規格書 6.4 節：商品小計 + 運費 − 折抵金額 = 最終應付金額
const grandTotal = computed(() => {
  return subtotal.value + shippingFee.value - discountAmount.value
})

// 金額格式：跟商品明細頁、購物車一致，加上千分位逗號（例如 NT$ 6,500）
function formatPrice(price) {
  return price != null ? 'NT$ ' + Number(price).toLocaleString('zh-TW') : '-'
}

async function loadMember() {
  try {
    const member = await getCurrentMember()
    memberId.value = member.memberId
    memberEmail.value = member.email
    memberInfo.value = {
      name: member.name,
      phone: member.phone,
      address: member.address
    }
  } catch (error) {
    // 理論上能進到結帳頁代表已經登入，這裡失敗就不特別處理
  }
}

function toggleUseMemberInfo() {
  if (useMemberInfo.value) {
    receiverName.value = memberInfo.value.name
    receiverPhone.value = memberInfo.value.phone
    shippingAddress.value = memberInfo.value.address
  } else {
    receiverName.value = ''
    receiverPhone.value = ''
    shippingAddress.value = ''
  }
}

async function loadCart() {
  try {
    cartLoading.value = true
    const response = await httpClient.get('/cart', { params: { memberId: memberId.value } })
    cartItems.value = response.data
  } catch (error) {
    // 安靜失敗
  } finally {
    cartLoading.value = false
  }
}

async function loadCoupons() {
  try {
    const response = await httpClient.get('/coupons/me/available')
    coupons.value = response.data
  } catch (error) {
    // 安靜失敗
  }
}

onMounted(async () => {
  await loadMember()
  loadCart()
  loadCoupons()
})

async function checkout() {
  formError.value = ''
  if (!shippingMethod.value) {
    formError.value = '請選擇取貨方式'
    return
  }
  if (shippingMethod.value === 'CVS' && !cvsStoreId.value) {
    formError.value = '請先選擇取貨門市'
    return
  }
  if (!paymentMethod.value) {
    formError.value = '請選擇付款方式'
    return
  }
  submitting.value = true
  try {
    const response = await httpClient.post('/orders', {
      receiverName: receiverName.value,
      receiverPhone: receiverPhone.value,
      shippingAddress: shippingMethod.value === 'HOME_DELIVERY' ? shippingAddress.value : cvsStoreAddress.value,
      shippingMethod: shippingMethod.value,
      cvsStoreId: cvsStoreId.value || null,
      cvsStoreName: cvsStoreName.value || null,
      cvsAddress: cvsStoreAddress.value || null,
      couponId: couponId.value || null,
      paymentMethod: paymentMethod.value
    })

    const orderId = response.data.orderId

    if (paymentMethod.value === 'LINE_PAY') {
      const payResponse = await httpClient.post(`/orders/${orderId}/linepay/request`)
      window.location.href = payResponse.data.paymentUrl
    } else {
      window.location.href = `http://localhost:8080/api/payment/checkout/${orderId}`
    }
  } catch (error) {
    alert(error.response?.data?.message || '結帳失敗，請稍後再試')
    submitting.value = false
  }
}
</script>

<template>
  <div v-if="submitting" class="text-center py-5 text-secondary">
    {{ paymentMethod === 'LINE_PAY' ? '正在前往 LINE Pay...' : '信用卡付款中...' }}
  </div>

  <div v-else>
    <CheckoutSteps :current-step="2" />
    <div class="row">
      <div class="col-lg-7">
        <!-- ===== 寄送資訊 ===== -->
        <h2 class="section-title">寄送資訊</h2>

        <div class="field">
          <label class="field-label">電子郵件</label>
          <input type="email" :value="memberEmail" class="form-control" disabled>
          <p class="field-hint">結帳後會寄發確認電子郵件。</p>
        </div>

        <div class="field">
          <div class="d-flex justify-content-between align-items-center">
            <label class="field-label">收件人資料</label>
            <div class="form-check mb-2">
              <input type="checkbox" class="form-check-input" id="useMemberInfo" v-model="useMemberInfo" @change="toggleUseMemberInfo">
              <label class="form-check-label small" for="useMemberInfo">帶入會員資料</label>
            </div>
          </div>
          <input type="text" v-model="receiverName" placeholder="收件人姓名" class="form-control mb-2">
          <input type="text" v-model="receiverPhone" placeholder="收件人電話" class="form-control">
        </div>

        <div class="field">
          <label class="field-label">取貨方式</label>

          <div class="pay-options">
            <!-- 宅配到府 -->
            <div class="pay-option" :class="{ active: shippingMethod === 'HOME_DELIVERY' }">
              <label class="pay-option-head">
                <input type="radio" class="form-check-input mt-0" value="HOME_DELIVERY" v-model="shippingMethod">
                <svg class="option-icon" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M3 6h11v9H3zM14 9h4l3 3v3h-7z" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round" />
                  <circle cx="7" cy="17" r="1.8" fill="none" stroke="currentColor" stroke-width="1.6" />
                  <circle cx="17" cy="17" r="1.8" fill="none" stroke="currentColor" stroke-width="1.6" />
                </svg>
                <span class="pay-option-name">宅配到府</span>
                <span class="option-fee">NT$ 100</span>
              </label>
              <div v-if="shippingMethod === 'HOME_DELIVERY'" class="option-body">
                <input type="text" v-model="shippingAddress" placeholder="收件人地址" class="form-control">
              </div>
            </div>

            <!-- 7-11 超商取貨 -->
            <div class="pay-option" :class="{ active: shippingMethod === 'CVS' }">
              <label class="pay-option-head">
                <input type="radio" class="form-check-input mt-0" value="CVS" v-model="shippingMethod">
                <img src="/images/shipping-logos/7-eleven.png" alt="7-ELEVEN" class="option-logo">
                <span class="pay-option-name">7-11 超商取貨</span>
                <span class="option-fee">NT$ 100</span>
              </label>
              <div v-if="shippingMethod === 'CVS'" class="option-body">
                <div v-if="cvsStoreName" class="store-selected">
                  <div>
                    <p class="mb-1 fw-semibold">{{ cvsStoreName }}</p>
                    <p class="mb-0 text-secondary small">{{ cvsStoreAddress }}</p>
                  </div>
                  <button type="button" class="btn btn-outline-dark btn-sm" @click="resetStoreSelection">更換門市</button>
                </div>
                <StoreMap v-else @store-selected="handleStoreSelected" />
              </div>
            </div>
          </div>
        </div>

        <hr class="section-divider">

        <!-- ===== 優惠券 ===== -->
        <h2 class="section-title">優惠券</h2>

        <div class="field">
          <select v-if="coupons.length > 0" v-model="couponId" class="form-select">
            <option value="">不使用優惠券</option>
            <option v-for="c in coupons" :key="c.couponId" :value="c.couponId">
              滿 {{ formatPrice(c.minSpend) }} 折抵 {{ formatPrice(c.discountAmount) }}
            </option>
          </select>
          <select v-else class="form-select" disabled>
            <option>目前沒有可使用的優惠券</option>
          </select>
          <p v-if="couponNotReached" class="field-hint text-danger">
            尚未達到此優惠券的滿額門檻，本次不會折抵
          </p>
        </div>

        <hr class="section-divider">

        <!-- ===== 付款 ===== -->
        <h2 class="section-title">付款</h2>

        <div class="field">
          <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
            <label class="field-label mb-0">付款方式</label>
            <div class="payment-logo-strip" aria-label="支援的付款方式">
              <img src="/images/payment-logos/01_ECPay.png" alt="ECPay" title="ECPay">
              <img src="/images/payment-logos/02_Apple_Pay.svg" alt="Apple Pay" title="Apple Pay">
              <img src="/images/payment-logos/03_iPASS_MONEY.png" alt="iPASS MONEY" title="iPASS MONEY">
              <img src="/images/payment-logos/04_JKOPAY.png" alt="街口支付" title="街口支付">
              <img src="/images/payment-logos/05_LINE_Pay.png" alt="LINE Pay" title="LINE Pay">
            </div>
          </div>

          <div class="pay-options">
            <label class="pay-option" :class="{ active: paymentMethod === 'ECPAY_CREDIT_CARD' }">
              <div class="pay-option-head">
                <input type="radio" class="form-check-input mt-0" value="ECPAY_CREDIT_CARD" v-model="paymentMethod">
                <span class="pay-option-name">信用卡</span>
                <div class="pay-option-logos">
                  <img src="/images/payment-logos/ECPay_VISA.png" alt="VISA" class="card-brand-logo">
                  <img src="/images/payment-logos/ECPay_mastercard.svg" alt="Mastercard" class="card-brand-logo">
                  <img src="/images/payment-logos/ECPay_JCB.png" alt="JCB" class="card-brand-logo">
                </div>
              </div>
              <p v-if="paymentMethod === 'ECPAY_CREDIT_CARD'" class="pay-option-desc">
                點選「下訂單」後，將跳轉至綠界輸入卡號
              </p>
            </label>

            <label class="pay-option" :class="{ active: paymentMethod === 'LINE_PAY' }">
              <div class="pay-option-head">
                <input type="radio" class="form-check-input mt-0" value="LINE_PAY" v-model="paymentMethod">
                <span class="pay-option-name">LINE Pay</span>
                <div class="pay-option-logos">
                  <img src="/images/payment-logos/05_LINE_Pay.png" alt="LINE Pay" class="card-brand-logo">
                </div>
              </div>
              <p v-if="paymentMethod === 'LINE_PAY'" class="pay-option-desc">
                點選「下訂單」後，將跳轉至 LINE Pay 完成付款
              </p>
            </label>
          </div>
        </div>

        <div v-if="formError" class="alert alert-danger py-2">
          {{ formError }}
        </div>

        <!-- ===== 送出 ===== -->
        <div class="checkout-footer">
          <span class="total-label">總計</span>
          <span class="total-amount">{{ formatPrice(grandTotal) }}</span>
          <button class="btn btn-dark rounded-pill px-5 py-2" @click="checkout">下訂單</button>
        </div>
      </div>

      <div class="col-lg-5">
        <div class="border rounded p-3" style="position: sticky; top: 76px;">
          <h2 class="section-title">訂單摘要</h2>

          <div v-if="cartLoading" class="text-secondary small">載入中...</div>

          <template v-else>
            <div v-for="item in selectedItems" :key="item.cartItemId" class="d-flex gap-2 mb-3">
              <img :src="resolveProductImageUrl(item.imageUrl)" alt="" style="width: 120px; height: 120px; object-fit: cover;">
              <div class="flex-grow-1">
                <p class="mb-0">{{ item.productName }}</p>
                <p class="mb-0 text-secondary small">{{ item.size }} / {{ item.color }}</p>
              </div>
              <p class="mb-0 text-nowrap">{{ formatPrice(item.price * item.quantity) }}</p>
            </div>

            <hr>

            <div class="d-flex justify-content-between mb-2">
              <span>小計</span>
              <span>{{ formatPrice(subtotal) }}</span>
            </div>
            <div class="d-flex justify-content-between mb-2">
              <span>運費</span>
              <span>{{ shippingFee === 0 ? '免費' : formatPrice(shippingFee) }}</span>
            </div>
            <div v-if="discountAmount > 0" class="d-flex justify-content-between mb-2">
              <span>優惠券折抵</span>
              <span class="text-danger">- {{ formatPrice(discountAmount) }}</span>
            </div>
            <hr>
            <div class="d-flex justify-content-between">
              <span>總計</span>
              <span class="fw-bold">{{ formatPrice(grandTotal) }}</span>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ===== 字級與間距：三層就好 ===== */
/* 第一層：區塊標題（寄送資訊 / 付款 / 訂單摘要） */
.section-title {
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 1.25rem;
}

/* 第二層：欄位標題（電子郵件 / 取貨方式 / 付款方式…） */
.field-label {
  display: block;
  font-size: 0.95rem;
  font-weight: 500;
  color: #212529;
  margin-bottom: 0.5rem;
}

/* 第三層：輔助說明文字 */
.field-hint {
  font-size: 0.85rem;
  color: #6c757d;
  margin: 0.4rem 0 0;
}

/* 每個欄位群組之間統一距離 */
.field {
  margin-bottom: 1.75rem;
}

.section-divider {
  margin: 2.25rem 0;
  opacity: 0.15;
}


/* ===== 取貨方式選項 ===== */
.option-icon {
  width: 22px;
  height: 22px;
  color: #212529;
  flex-shrink: 0;
}

.option-logo {
  width: 22px;
  height: 22px;
  object-fit: contain;
  flex-shrink: 0;
}

.option-fee {
  margin-left: auto;
  font-size: 0.9rem;
  color: #6c757d;
}

.option-body {
  margin: 0.9rem 0 0 1.75rem;
}

.store-selected {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1rem;
  background: #fff;
  border: 1px solid #dee2e6;
  border-radius: 6px;
}

/* ===== 付款方式 logo ===== */
.payment-logo-strip {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.payment-logo-strip img {
  width: 56px;
  height: 22px;
  object-fit: contain;
  display: block;
  flex-shrink: 0;
}

.card-brand-logo {
  width: 48px;
  height: 30px;
  padding: 4px 6px;
  box-sizing: border-box;
  background: #fff;
  border: 1px solid #dee2e6;
  border-radius: 6px;
  object-fit: contain;
  display: block;
  flex-shrink: 0;
}


/* ===== 付款選項（上下排列） ===== */
.pay-options {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.pay-option {
  display: block;
  padding: 0.9rem 1rem;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.15s, background-color 0.15s;
}

.pay-option:hover {
  border-color: #adb5bd;
}

.pay-option.active {
  border-color: #212529;
  background: #f8f9fa;
}

.pay-option-head {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.pay-option-name {
  font-weight: 500;
}

.pay-option-logos {
  margin-left: auto;
  display: flex;
  gap: 6px;
}

.pay-option-desc {
  margin: 0.6rem 0 0 1.75rem;
  font-size: 0.85rem;
  color: #6c757d;
}

/* 選取的圓點改成黑色，配合整體黑白灰 */
.pay-option .form-check-input:checked {
  background-color: #212529;
  border-color: #212529;
}

/* ===== 送出列 ===== */
.checkout-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 1rem;
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
}

.total-label {
  font-size: 0.95rem;
  color: #6c757d;
}

.total-amount {
  font-size: 1.35rem;
  font-weight: 700;
}

@media (max-width: 576px) {
  .payment-logo-strip {
    justify-content: flex-start;
    gap: 8px;
  }

  .payment-logo-strip img {
    width: 48px;
    height: 20px;
  }

  .checkout-footer {
    flex-wrap: wrap;
  }

  .checkout-footer .btn {
    width: 100%;
  }
}

.pay-option-head {
  cursor: pointer;
}

div.pay-option {
  cursor: default;
}
</style>