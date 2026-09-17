<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import httpClient from '@/service/httpClient'
import { getCurrentMember } from '@/service/authService'

const route = useRoute()
const loading = ref(true)
const errorMessage = ref('')
const order = ref(null)
const memberInfo = ref({ name: '', email: '', phone: '' })

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

async function loadMember() {
  try {
    const member = await getCurrentMember()
    memberInfo.value = { name: member.name, email: member.email, phone: member.phone }
  } catch (error) {
    // 安靜失敗，不影響訂單明細顯示
  }
}

function formatDate(dateString) {
  if (!dateString) return ''
  const date = new Date(dateString)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}/${month}/${day}`
}

function paymentMethodLabel(method) {
  const labels = { MOCK_CREDIT_CARD: '信用卡' }
  return labels[method] || method
}

function statusLabel(status) {
  const labels = { PAID: '已付款', PENDING: '等待確認' }
  return labels[status] || status
}

onMounted(() => {
  fetchOrder()
  loadMember()
})
</script>

<template>
  <div v-if="loading" class="text-secondary mt-3">載入中...</div>

  <div v-else-if="errorMessage" class="alert alert-danger mt-3">
    {{ errorMessage }}
  </div>

  <div v-else class="border rounded overflow-hidden">
    <div class="bg-light text-center py-2 fw-bold">訂單明細</div>

    <div class="p-4">
      <div class="d-flex justify-content-between mb-3">
        <span>訂單編號：{{ order.orderNumber }}</span>
        <span class="text-secondary">訂購日：{{ formatDate(order.createdAt) }}</span>
      </div>

      <table class="table">
        <thead>
          <tr class="text-secondary small">
            <th>商品名稱</th>
            <th>規格</th>
            <th class="text-end">數量</th>
            <th class="text-end">價格</th>
            <th class="text-end">小計</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in order.items" :key="item.orderItemId">
            <td>
              <div class="d-flex align-items-center gap-2">
                <img v-if="item.imageUrl" :src="item.imageUrl" alt="" style="width: 48px; height: 48px; object-fit: cover;">
                <span>{{ item.productName }}</span>
              </div>
            </td>
            <td>{{ item.specification }}</td>
            <td class="text-end">{{ item.quantity }}</td>
            <td class="text-end">NT$ {{ item.unitPrice }}</td>
            <td class="text-end">NT$ {{ item.lineTotal }}</td>
          </tr>
        </tbody>
      </table>

      <div class="d-flex justify-content-end">
        <div style="min-width: 220px;">
          <div class="d-flex justify-content-between mb-1">
            <span>商品總計</span>
            <span>NT$ {{ order.subtotal }}</span>
          </div>
          <div class="d-flex justify-content-between mb-1">
            <span>運費</span>
            <span>NT$ {{ order.shippingFee }}</span>
          </div>
          <div v-if="order.discountAmount > 0" class="d-flex justify-content-between mb-1 text-danger">
            <span>優惠券折抵</span>
            <span>−NT$ {{ order.discountAmount }}</span>
          </div>
          <hr>
          <div class="d-flex justify-content-between fw-bold">
            <span>訂單總金額</span>
            <span>NT$ {{ order.totalAmount }}</span>
          </div>
        </div>
      </div>

      <hr class="my-4">

      <div class="row mb-4">
        <div class="col-md-6">
          <h6 class="fw-bold mb-2">會員資訊</h6>
          <p class="mb-0">{{ memberInfo.name }}</p>
          <p class="mb-0 text-secondary">{{ memberInfo.email }}</p>
          <p class="mb-0 text-secondary">{{ memberInfo.phone }}</p>
        </div>
        <div class="col-md-6">
          <h6 class="fw-bold mb-2">收件人資訊</h6>
          <p class="mb-0">{{ order.receiverName }}</p>
          <p class="mb-0 text-secondary">{{ order.receiverPhone }}</p>
          <p class="mb-0 text-secondary">{{ order.shippingAddress }}</p>
        </div>
      </div>

      <div class="row">
        <div class="col-md-6 mb-3">
          <h6 class="fw-bold mb-2">付款狀態</h6>
          <span class="badge bg-secondary">{{ statusLabel(order.status) }}</span>
        </div>
        <div class="col-md-6 mb-3">
          <h6 class="fw-bold mb-2">運送狀態</h6>
          <span class="badge bg-secondary">已出貨</span>
        </div>
        <div class="col-md-6">
          <h6 class="fw-bold mb-2">付款方式</h6>
          <span class="badge bg-secondary">{{ paymentMethodLabel(order.paymentMethod) }}</span>
        </div>
        <div class="col-md-6">
          <h6 class="fw-bold mb-2">運送方式</h6>
          <span class="badge bg-secondary">宅配</span>
        </div>
      </div>
    </div>
  </div>
</template>