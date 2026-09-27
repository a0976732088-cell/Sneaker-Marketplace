<script setup>
import { ref, onMounted,onUnmounted, computed } from 'vue'
import httpClient from '@/service/httpClient'
import { useRouter } from 'vue-router'
import CheckoutSteps from '@/components/commerce/CheckoutSteps.vue'
import { getCurrentMember } from '@/service/authService'
import { refreshCartCount } from '@/service/cartState'
import { resolveProductImageUrl } from '@/utils/productImageUrl'

const loading = ref(true)
const errorMessage = ref('')
const cartItems = ref([])
const isLoggedIn = ref(false)
const memberId = ref(null)
const router = useRouter()
let redirectTimer = null

// 庫存提醒門檻：跟商品明細頁一致，庫存大於 0 且小於這個數字就提醒
const LOW_STOCK_THRESHOLD = 10

const totalPrice = computed(() => {
  let total = 0
  for (const item of cartItems.value) {
    if (item.selected) {
      total = total + item.price * item.quantity
    }
  }
  return total
})
const shippingFee = computed(() => {
  return totalPrice.value === 0 ? 0 : 100
})

const grandTotal = computed(() => {
  return totalPrice.value + shippingFee.value
})

// 金額格式：跟商品明細頁一致，加上千分位逗號（例如 NT$ 6,500）
function formatPrice(price) {
  return price != null ? 'NT$ ' + Number(price).toLocaleString('zh-TW') : '-'
}

function isLowStock(item) {
  return item.stock > 0 && item.stock < LOW_STOCK_THRESHOLD
}

function checkEmptyCartAndRedirect() {
  if (cartItems.value.length === 0) {
    clearTimeout(redirectTimer)
    redirectTimer = setTimeout(() => {
      router.push('/products')
    }, 3000)
  }
}

async function fetchCart() {
  try {
    loading.value = true
    const response = await httpClient.get('/cart', { params: { memberId: memberId.value } })
    cartItems.value = response.data
    checkEmptyCartAndRedirect()   // 改成呼叫共用函式
  } catch (error) {
    errorMessage.value = '無法載入購物車'
  } finally {
    loading.value = false
  }
}

async function toggleSelected(item) {
  const newSelected = !item.selected
  try {
    await httpClient.put(`/cart/items/${item.cartItemId}`, null, {
      params: { selected: newSelected }
    })
    item.selected = newSelected
  } catch (error) {
    alert('更新失敗')
  }
}

async function updateQuantity(item) {
  try {
    await httpClient.put(`/cart/items/${item.cartItemId}`, null, {
      params: { quantity: item.quantity }
    })
    await refreshCartCount() 
  } catch (error) {
    alert('更新失敗')
  }
}

async function deleteItem(item) {
  try {
    await httpClient.delete(`/cart/items/${item.cartItemId}`)
    cartItems.value = cartItems.value.filter(i => i.cartItemId !== item.cartItemId)
    await refreshCartCount()
    checkEmptyCartAndRedirect()
  } catch (error) {
    alert('刪除失敗')
  }
}

function increaseQuantity(item) {
  if (item.quantity >= item.stock) return
  item.quantity++
  updateQuantity(item)
}

function decreaseQuantity(item) {
  if (item.quantity === 1) {
    deleteItem(item)
    return
  }
  item.quantity--
  updateQuantity(item)
}

onMounted(async () => {
  try {
    const member = await getCurrentMember()
    isLoggedIn.value = true
    memberId.value = member.memberId
    await fetchCart()
  } catch (error) {
    isLoggedIn.value = false
    loading.value = false
  }
})

onUnmounted(() => {
  clearTimeout(redirectTimer)
})
</script>

<template>
<div v-if="loading" class="text-secondary mt-3">載入中...</div>

<div v-else-if="!isLoggedIn" class="text-center mt-5">
  <p class="mb-3">請先登入才能查看購物車</p>
  <button class="btn btn-dark" @click="router.push('/login')">前往登入</button>
</div>

<template v-else>
  <CheckoutSteps :current-step="1" />

  <div v-if="errorMessage" class="alert alert-danger mt-3">
    {{ errorMessage }}
  </div>

  <div v-else-if="cartItems.length === 0" class="text-secondary mt-3">
  購物車是空的，即將為您導回商城首頁...
  </div>

  <div v-else class="row mt-3">
    <div class="col-md-8">
      <div v-for="item in cartItems" :key="item.cartItemId" class="d-flex align-items-center gap-3 border-bottom py-3">
        <input type="checkbox" :checked="item.selected" @change="toggleSelected(item)" style="accent-color: gray; margin-top: -25px;">

        <div>
          <img :src="resolveProductImageUrl(item.imageUrl)" alt="" style="width: 120px; height: 120px; object-fit: cover;">
        </div>

        <div class="flex-grow-1 d-flex justify-content-between align-self-stretch">
          <div>
            <p class="mb-1">{{ item.productName }}</p>
            <p class="mb-0 text-secondary">{{ item.size }} / {{ item.color }}</p>
            <p v-if="isLowStock(item)" class="low-stock-text mt-4 mb-0">所剩庫存不多，欲購從速。</p>
          </div>

          <div class="d-flex flex-column justify-content-between align-items-end">
            <p class="mb-0">{{ formatPrice(item.price * item.quantity) }}</p>
            <div class="d-flex align-items-center gap-2 border rounded-pill px-3 py-1" style="width: fit-content;">
              <button
                class="btn btn-sm border-0 p-0 qty-btn"
                :aria-label="item.quantity === 1 ? '移除商品' : '減少數量'"
                @click="decreaseQuantity(item)"
              >
                <!-- 數量為 1 時顯示垃圾桶（SVG，不用 emoji），否則顯示減號 -->
                <svg v-if="item.quantity === 1" class="trash-icon" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M4 7h16M10 11v6M14 11v6M5 7l1 12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2l1-12M9 7V4h6v3" />
                </svg>
                <template v-else>−</template>
              </button>
              <span>{{ item.quantity }}</span>
              <button class="btn btn-sm border-0 p-0 qty-btn" aria-label="增加數量" @click="increaseQuantity(item)" :disabled="item.quantity >= item.stock">+</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="col-md-4">
      <div class="border rounded p-3">
        <h5>摘要</h5>
        <div class="d-flex justify-content-between mb-2">
          <span>小計</span>
          <span>{{ formatPrice(totalPrice) }}</span>
        </div>
        <div class="d-flex justify-content-between mb-3">
          <span>運費</span>
          <span>{{ formatPrice(shippingFee) }}</span>
        </div>
        <hr>
        <div class="d-flex justify-content-between mb-3">
          <span>總計</span>
          <span class="fw-bold">{{ formatPrice(grandTotal) }}</span>
        </div>
        <button class="btn btn-dark w-100" :disabled="totalPrice===0" @click="router.push('/checkout')">結帳</button>
      </div>
    </div>
  </div>
</template>
</template>

<style scoped>
/* 庫存提醒：跟商品明細頁同樣的紅色與粗細 */
.low-stock-text {
  color: #d9534f;
  font-weight: 600;
  font-size: 0.9rem;
}

.qty-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.2rem;
}

.trash-icon {
  width: 1.1rem;
  height: 1.1rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}
</style>