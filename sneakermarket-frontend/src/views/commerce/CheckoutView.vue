<script setup>
import { ref, onMounted } from 'vue'
import httpClient from '@/service/httpClient'

import { useRouter } from 'vue-router'
const router = useRouter()

const receiverName = ref('')
const receiverPhone = ref('')
const shippingAddress = ref('')
const submitting = ref(false)
const paymentSuccess = ref(false)
const coupons = ref([])
const couponId = ref('')

async function loadCoupons() {
  try {
    const response = await httpClient.get('/coupons/me/available')
    coupons.value = response.data
  } catch (error) {
    // 優惠券查詢失敗不影響結帳本身，安靜失敗就好
  }
}

onMounted(() => {
  loadCoupons()
})

async function checkout() {
  submitting.value = true
  try {
    const response = await httpClient.post('/orders', {
      receiverName: receiverName.value,
      receiverPhone: receiverPhone.value,
      shippingAddress: shippingAddress.value,
      couponId: couponId.value || null
    })
    paymentSuccess.value = true
    setTimeout(() => {
      router.push(`/orders/${response.data.orderId}`)
    }, 3000)
    } catch (error) {
    alert(error.response?.data?.message || '結帳失敗，請稍後再試')
    submitting.value = false
    }
}
</script>

<template>
  <h2>結帳</h2>

  <div class="mt-3" style="max-width: 400px;">
    <div v-if="paymentSuccess">
    ✅ 付款成功
    </div>
    <div v-else-if="submitting">
    信用卡付款中...
    </div>
    <div v-else>
    <input type="text" v-model="receiverName" placeholder="收件人姓名" class="form-control mb-2">
    <input type="text" v-model="receiverPhone" placeholder="收件人電話" class="form-control mb-2">
    <input type="text" v-model="shippingAddress" placeholder="收件人地址" class="form-control mb-3">
      <select v-if="coupons.length > 0" v-model="couponId" class="form-select mb-3">
        <option value="">不使用優惠券</option>
        <option v-for="c in coupons" :key="c.couponId" :value="c.couponId">
          滿 NT$ {{ c.minSpend }} 折抵 NT$ {{ c.discountAmount }}
        </option>
      </select>
    <button class="btn btn-dark w-100" @click="checkout">送出訂單</button>
    </div>

  </div>
</template>