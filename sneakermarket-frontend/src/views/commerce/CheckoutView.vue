<script setup>
import { ref, onMounted, computed } from 'vue'
import httpClient from '@/service/httpClient'
import { getCurrentMember } from '@/service/authService'
import { useRouter } from 'vue-router'
import CheckoutSteps from '@/components/commerce/CheckoutSteps.vue'
import StoreMap from '@/views/commerce/StoreMap.vue'

const router = useRouter()

// ===== 會員資料 =====
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
const shippingMethod = ref('HOME_DELIVERY') // HOME_DELIVERY 或 CVS
const cvsStoreId = ref('')
const cvsStoreName = ref('')
const cvsStoreAddress = ref('')

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
const paymentSuccess = ref(false)
const formError = ref('')
const completedOrderId = ref(null)

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
    const response = await httpClient.get('/cart', { params: { memberId: 1 } })
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

onMounted(() => {
  loadMember()
  loadCart()
  loadCoupons()
})


async function checkout() {
  formError.value = ''

  // 選超商取貨卻還沒選門市，先擋下來
  if (shippingMethod.value === 'CVS' && !cvsStoreId.value) {
    formError.value = '請先選擇取貨門市'
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
      couponId: couponId.value || null
    })
    window.location.href = `http://localhost:8080/api/payment/checkout/${response.data.orderId}`
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

        <div class="mb-3">
          <label class="form-label d-block">取貨方式</label>
          <div class="btn-group" role="group">
            <input type="radio" class="btn-check" id="methodHome" value="HOME_DELIVERY" v-model="shippingMethod" autocomplete="off">
            <label class="btn btn-outline-dark" for="methodHome">宅配到府</label>

            <input type="radio" class="btn-check" id="methodCvs" value="CVS" v-model="shippingMethod" autocomplete="off">
            <label class="btn btn-outline-dark" for="methodCvs">7-11 超商取貨</label>
          </div>
        </div>

        <div v-if="shippingMethod === 'HOME_DELIVERY'" class="mb-4">
          <input type="text" v-model="shippingAddress" placeholder="收件人地址" class="form-control">
        </div>

        <!-- ========================================================
             選了 7-11 超商取貨才會進到這裡：
             還沒選門市 -> 顯示地圖讓使用者點
             已經選好門市 -> 收起地圖，顯示卡片
             ======================================================== -->
        <div v-else class="mb-4">
          <div v-if="cvsStoreName" class="border rounded p-3">
            <p class="mb-1 fw-semibold">取貨門市：{{ cvsStoreName }}</p>
            <p class="mb-2 text-secondary small">{{ cvsStoreAddress }}</p>
            <button type="button" class="btn btn-outline-dark btn-sm" @click="resetStoreSelection">更換門市</button>
          </div>
          <StoreMap v-else @store-selected="handleStoreSelected" />
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

        <div class="border rounded p-3 mb-3 bg-light text-secondary small">
          點選「下訂單」後，將跳轉至綠界金流輸入信用卡資訊
        </div>

        <div v-if="formError" class="alert alert-danger py-2">
          {{ formError }}
        </div>

        <button class="btn btn-dark w-100 mt-3 rounded-pill" @click="checkout">下訂單</button>
      </div>

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