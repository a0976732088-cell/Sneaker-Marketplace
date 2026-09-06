<script setup>
import { ref, onMounted, computed } from 'vue'
import httpClient from '@/service/httpClient'

const loading = ref(true)
const errorMessage = ref('')
const cartItems = ref([])
const totalPrice = computed(() => {
  let total = 0
  for (const item of cartItems.value) {
    if (item.selected) {
      total = total + item.price * item.quantity
    }
  }
  return total
})

async function fetchCart() {
  try {
    loading.value = true
    const response = await httpClient.get('/cart', { params: { memberId: 1 } }) // 之後改成真正登入的會員 ID
    cartItems.value = response.data
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
  } catch (error) {
    alert('更新失敗')
  }
}

async function deleteItem(item) {
  try {
    await httpClient.delete(`/cart/items/${item.cartItemId}`)
    cartItems.value = cartItems.value.filter(i => i.cartItemId !== item.cartItemId)
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

onMounted(() => {
  fetchCart()
})
</script>

<template>
<h2>購物車</h2>

<div v-if="loading" class="text-secondary mt-3">載入中...</div>

<div v-else-if="errorMessage" class="alert alert-danger mt-3">
  {{ errorMessage }}
</div>

<div v-else-if="cartItems.length === 0" class="text-secondary mt-3">
  購物車是空的
</div>

<div v-else class="row mt-3">
  <div class="col-md-8">
    <div v-for="item in cartItems" :key="item.cartItemId" class="d-flex gap-3 border-bottom py-3">
      <input type="checkbox" :checked="item.selected" @change="toggleSelected(item)" style="accent-color: gray; margin-top: -50px;">

      <div>
        <img :src="item.imageUrl" alt="" style="width: 120px; height: 120px; object-fit: cover;">

        <div class="d-flex align-items-center gap-2 border rounded-pill px-3 py-1 mt-2" style="width: fit-content;">
          <button class="btn btn-sm border-0 p-0" @click="decreaseQuantity(item)">
            {{ item.quantity === 1 ? '🗑️' : '−' }}
          </button>
          <span>{{ item.quantity }}</span>
          <button class="btn btn-sm border-0 p-0" @click="increaseQuantity(item)" :disabled="item.quantity >= item.stock">+</button>
        </div>

        <p v-if="item.stock === 1" class="text-warning small mt-1 mb-0">所剩庫存不多，欲購從速。</p>
      </div>

      <div class="flex-grow-1 d-flex justify-content-between">
        <div>
          <p class="mb-1">{{ item.productName }}</p>
          <p class="mb-0 text-secondary">{{ item.size }} / {{ item.color }}</p>
        </div>
        <p class="mb-0">NT$ {{ item.price * item.quantity }}</p>
      </div>
    </div>
  </div>

  <div class="col-md-4">
    <div class="border rounded p-3">
      <h5>摘要</h5>
      <div class="d-flex justify-content-between mb-3">
        <span>合計</span>
        <span class="fw-bold">NT$ {{ totalPrice }}</span>
      </div>
      <button class="btn btn-dark w-100">結帳</button>
    </div>
  </div>
</div>
</template>