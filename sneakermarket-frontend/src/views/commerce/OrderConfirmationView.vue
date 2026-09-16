<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import httpClient from '@/service/httpClient'

const route = useRoute()
const loading = ref(true)
const errorMessage = ref('')
const order = ref(null)

async function fetchOrder() {
  try {
    const response = await httpClient.get(`/orders/${route.params.id}`)
    order.value = response.data
  } catch (error) {
    errorMessage.value = '無法載入訂單資料'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchOrder()
})
</script>

<template>
  <h2>訂單完成</h2>

  <div v-if="loading" class="text-secondary mt-3">載入中...</div>

  <div v-else-if="errorMessage" class="alert alert-danger mt-3">
    {{ errorMessage }}
  </div>

  <div v-else class="mt-3">
    <p>訂單編號：{{ order.orderNumber }}</p>
    <p>訂單狀態：{{ order.status }}</p>
    <p>收件人：{{ order.receiverName }}（{{ order.receiverPhone }}）</p>
    <p>寄送地址：{{ order.shippingAddress }}</p>

    <hr>

    <div v-for="item in order.items" :key="item.orderItemId" class="d-flex justify-content-between border-bottom py-2">
      <span>{{ item.productName }}（{{ item.specification }}）× {{ item.quantity }}</span>
      <span>NT$ {{ item.lineTotal }}</span>
    </div>

    <hr>

    <p>小計：NT$ {{ order.subtotal }}</p>
    <p>運費：NT$ {{ order.shippingFee }}</p>
    <p v-if="order.discountAmount > 0" class="text-danger">優惠券折抵：−NT$ {{ order.discountAmount }}</p>
    <p class="fw-bold">總計：NT$ {{ order.totalAmount }}</p>
  </div>
</template>