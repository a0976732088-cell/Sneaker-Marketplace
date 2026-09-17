<script setup>
import { ref, onMounted, computed } from 'vue'
import httpClient from '@/service/httpClient'
import { getCurrentMember } from '@/service/authService'
import { useRouter } from 'vue-router'
import CheckoutSteps from '@/components/commerce/CheckoutSteps.vue'

const router = useRouter()

// ===== 會員資料 =====
const memberEmail = ref('')
const memberInfo = ref({ name: '', phone: '', address: '' }) // 存起來給「帶入會員資料」用，不直接綁定表單
const useMemberInfo = ref(false)

// ===== 購物車資料（只用來顯示訂單摘要，不會送出） =====
const cartItems = ref([])
const cartLoading = ref(true)

// ===== 寄送資訊表單（預設空白，由使用者自己填或勾選帶入會員資料） =====
const receiverName = ref('')
const receiverPhone = ref('')
const shippingAddress = ref('')

// ===== 優惠券 =====
const coupons = ref([])
const couponId = ref('')

// ===== 信用卡表單（純前端視覺 + 格式驗證，不會送到後端） =====
const cardHolderName = ref('')
const cardNumber = ref('')
const cardExpiry = ref('')
const cardCvv = ref('')

const submitting = ref(false)
const paymentSuccess = ref(false)
const formError = ref('')
const completedOrderId = ref(null)

// ===== 訂單摘要計算（跟 CartView.vue 同一套邏輯，只算勾選的商品） =====
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

const grandTotal = computed(() => {
  return subtotal.value + shippingFee.value
})

async function loadMember() {
  try {
    const member = await getCurrentMember()
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

// ===== 「帶入會員資料」核取方塊：勾選就填入、取消就清空 =====
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
    const response = await httpClient.get('/cart', { params: { memberId: 1 } }) // 之後改成真正登入的會員 ID，跟 CartView.vue 一樣的既有 TODO
    cartItems.value = response.data
  } catch (error) {
    // 訂單摘要載入失敗不擋結帳，安靜失敗
  } finally {
    cartLoading.value = false
  }
}

async function loadCoupons() {
  try {
    const response = await httpClient.get('/coupons/me/available')
    coupons.value = response.data
  } catch (error) {
    // 優惠券查詢失敗不影響結帳本身，安靜失敗就好
  }
}

onMounted(() => {
  loadMember()
  loadCart()
  loadCoupons()
})

// ===== 信用卡欄位格式化 =====
function formatCardNumber(event) {
  const digitsOnly = event.target.value.replace(/\D/g, '').slice(0, 16)
  const groups = digitsOnly.match(/.{1,4}/g) || []
  cardNumber.value = groups.join(' ')
}

function formatExpiry(event) {
  const digitsOnly = event.target.value.replace(/\D/g, '').slice(0, 4)
  if (digitsOnly.length <= 2) {
    cardExpiry.value = digitsOnly
  } else {
    cardExpiry.value = digitsOnly.slice(0, 2) + '/' + digitsOnly.slice(2)
  }
}

function formatCvv(event) {
  cardCvv.value = event.target.value.replace(/\D/g, '').slice(0, 4)
}

// ===== 信用卡格式驗證（純前端，只是為了看起來逼真，不會真的刷卡） =====
function validateCard() {
  if (!cardHolderName.value.trim()) {
    return '請輸入持卡人姓名'
  }

  const rawCardNumber = cardNumber.value.replace(/\s/g, '')
  if (rawCardNumber.length < 13 || rawCardNumber.length > 16) {
    return '卡號格式不正確'
  }

  const expiryMatch = cardExpiry.value.match(/^(\d{2})\/(\d{2})$/)
  if (!expiryMatch) {
    return '到期日格式不正確，請輸入 MM/YY'
  }
  const month = Number(expiryMatch[1])
  const year = Number(expiryMatch[2]) + 2000
  if (month < 1 || month > 12) {
    return '到期月份不正確'
  }
  const now = new Date()
  const expiryDate = new Date(year, month) // 該月份結束後才算過期
  if (expiryDate < now) {
    return '這張卡已經過期了'
  }

  if (cardCvv.value.length < 3) {
    return '安全碼格式不正確'
  }

  return ''
}

async function checkout() {
  formError.value = ''

  const cardError = validateCard()
  if (cardError) {
    formError.value = cardError
    return
  }

  submitting.value = true
  try {
    const response = await httpClient.post('/orders', {
      receiverName: receiverName.value,
      receiverPhone: receiverPhone.value,
      shippingAddress: shippingAddress.value,
      couponId: couponId.value || null
    })
    completedOrderId.value = response.data.orderId
    paymentSuccess.value = true
  } catch (error) {
    alert(error.response?.data?.message || '結帳失敗，請稍後再試')
    submitting.value = false
  }
}
</script>

<template>
  <div v-if="paymentSuccess" class="text-center d-flex flex-column align-items-center justify-content-center" style="min-height: 70vh;">
    <CheckoutSteps :current-step="3" />

    <div class="mx-auto mb-3 rounded-circle border border-dark d-flex align-items-center justify-content-center" style="width:64px;height:64px; font-size:2rem;">
      ✓
    </div>
    <h2 class="fw-bold mb-2">感謝您的購買</h2>
    <p class="text-secondary mb-4">已收到您的訂單，請您檢視訂單，確認您的付款狀態</p>
    <div class="d-flex justify-content-center gap-2">
      <button class="btn btn-dark px-4" @click="router.push('/')">返回首頁</button>
      <button class="btn btn-outline-dark px-4" @click="router.push(`/orders/${completedOrderId}`)">查詢訂單</button>
    </div>
  </div>

  <div v-else-if="submitting" class="text-center py-5 text-secondary">
    信用卡付款中...
  </div>

  <div v-else>
    <CheckoutSteps :current-step="2" />
    <div class="row">
      <!-- 左側：寄送資訊 + 付款 -->
      <div class="col-lg-7">
        <h5 class="mb-4">寄送資訊</h5>
          
        <div class="mb-1">
          <label class="form-label">電子郵件</label>
          <input type="email" :value="memberEmail" class="form-control" disabled>
        </div>
        <p class="text-secondary small mb-3">結帳後會寄發確認電子郵件。</p>

        <div class="d-flex justify-content-between align-items-center mb-2">
        <p class="fw-semibold mb-2">輸入你的姓名和地址：</p>
        <div class="form-check mb-0">
            <input type="checkbox" class="form-check-input" id="useMemberInfo" v-model="useMemberInfo" @change="toggleUseMemberInfo">
            <label class="form-check-label" for="useMemberInfo">帶入會員資料</label>
          </div>
        </div>

        <div class="mb-3">
          <input type="text" v-model="receiverName" placeholder="收件人姓名" class="form-control">
        </div>

        <div class="mb-3">
          <input type="text" v-model="receiverPhone" placeholder="收件人電話" class="form-control">
        </div>

        <div class="mb-4">
          <input type="text" v-model="shippingAddress" placeholder="收件人地址" class="form-control">
        </div>

        <hr class="my-4">

        <h5 class="mb-3">付款</h5>

        <div class="mb-3">
          <label class="form-label">有促銷代碼嗎？</label>
          <select v-if="coupons.length > 0" v-model="couponId" class="form-select">
            <option value="">不使用優惠券</option>
            <option v-for="c in coupons" :key="c.couponId" :value="c.couponId">
              滿 NT$ {{ c.minSpend }} 折抵 NT$ {{ c.discountAmount }}
            </option>
          </select>
          <div v-else class="text-secondary small">目前沒有可使用的優惠券</div>
        </div>

        <div class="border rounded p-3 mb-3 bg-light">
          💳 信用卡或金融簽帳卡
        </div>

        <input type="text" v-model="cardHolderName" placeholder="持卡人姓名" class="form-control mb-2">
        <input type="text" :value="cardNumber" @input="formatCardNumber" placeholder="卡號" maxlength="19" class="form-control mb-2">
        <div class="d-flex gap-2 mb-2">
          <input type="text" :value="cardExpiry" @input="formatExpiry" placeholder="MM/YY" maxlength="5" class="form-control">
          <input type="text" :value="cardCvv" @input="formatCvv" placeholder="安全碼" maxlength="4" class="form-control">
        </div>

        <div v-if="formError" class="alert alert-danger py-2">
          {{ formError }}
        </div>

        <button class="btn btn-dark w-100 mt-3 rounded-pill" @click="checkout">下訂單</button>
      </div>

      <!-- 右側：訂單摘要 -->
      <div class="col-lg-5">
        <div class="border rounded p-3" style="position: sticky; top: 76px;">
          <h5 class="mb-3">訂單摘要</h5>

          <div v-if="cartLoading" class="text-secondary small">載入中...</div>

          <template v-else>
            <div v-for="item in selectedItems" :key="item.cartItemId" class="d-flex gap-2 mb-3">
              <img :src="item.imageUrl" alt="" style="width: 64px; height: 64px; object-fit: cover;">
              <div class="flex-grow-1">
                <p class="mb-0">{{ item.productName }}</p>
                <p class="mb-0 text-secondary small">{{ item.size }} / {{ item.color }}</p>
              </div>
              <p class="mb-0">NT$ {{ item.price * item.quantity }}</p>
            </div>

            <hr>

            <div class="d-flex justify-content-between mb-2">
              <span>小計</span>
              <span>NT$ {{ subtotal }}</span>
            </div>
            <div class="d-flex justify-content-between mb-2">
              <span>運費</span>
              <span>{{ shippingFee === 0 ? '免費' : `NT$ ${shippingFee}` }}</span>
            </div>
            <hr>
            <div class="d-flex justify-content-between">
              <span>總計</span>
              <span class="fw-bold">NT$ {{ grandTotal }}</span>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>