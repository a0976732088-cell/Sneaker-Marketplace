<script setup>
import { ref } from 'vue'
import httpClient from '@/service/httpClient'

import { useRouter } from 'vue-router'
const router = useRouter()

const receiverName = ref('')
const receiverPhone = ref('')
const shippingAddress = ref('')
const submitting = ref(false)
const paymentSuccess = ref(false)

async function checkout() {
  submitting.value = true
  try {
    const response = await httpClient.post('/orders', {
      memberId: 1,
      receiverName: receiverName.value,
      receiverPhone: receiverPhone.value,
      shippingAddress: shippingAddress.value
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

    <button class="btn btn-dark w-100" @click="checkout">送出訂單</button>
    </div>

  </div>
</template>