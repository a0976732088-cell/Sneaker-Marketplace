<script setup>
import { ref, onMounted,onUnmounted, computed } from 'vue'
import httpClient from '@/service/httpClient'
import { useRouter } from 'vue-router'
import CheckoutSteps from '@/components/commerce/CheckoutSteps.vue'
import { getCurrentMember } from '@/service/authService'
import { refreshCartCount } from '@/service/cartState'

const loading = ref(true)
const errorMessage = ref('')
const cartItems = ref([])
const isLoggedIn = ref(false)
const memberId = ref(null)
const router = useRouter()
let redirectTimer = null

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
          <img :src="item.imageUrl" alt="" style="width: 120px; height: 120px; object-fit: cover;">
          <p v-if="item.stock === 1" class="text-warning small mt-1 mb-0">所剩庫存不多，欲購從速。</p>
        </div>

        <div class="flex-grow-1 d-flex justify-content-between align-self-stretch">
          <div>
            <p class="mb-1">{{ item.productName }}</p>
            <p class="mb-0 text-secondary">{{ item.size }} / {{ item.color }}</p>
          </div>

          <div class="d-flex flex-column justify-content-between align-items-end">
            <p class="mb-0">NT$ {{ item.price * item.quantity }}</p>
            <div class="d-flex align-items-center gap-2 border rounded-pill px-3 py-1" style="width: fit-content;">
              <button class="btn btn-sm border-0 p-0" @click="decreaseQuantity(item)">
                {{ item.quantity === 1 ? '🗑️' : '−' }}
              </button>
              <span>{{ item.quantity }}</span>
              <button class="btn btn-sm border-0 p-0" @click="increaseQuantity(item)" :disabled="item.quantity >= item.stock">+</button>
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
          <span>NT$ {{ totalPrice }}</span>
        </div>
        <div class="d-flex justify-content-between mb-3">
          <span>運費</span>
          <span>NT$ {{ shippingFee }}</span>
        </div>
        <hr>
        <div class="d-flex justify-content-between mb-3">
          <span>總計</span>
          <span class="fw-bold">NT$ {{ grandTotal }}</span>
        </div>
        <button class="btn btn-dark w-100" :disabled="totalPrice===0" @click="router.push('/checkout')">結帳</button>
      </div>
    </div>
  </div>
</template>
</template>