<script setup>
// LINE Pay 付款完成或取消後，LINE Pay 會把使用者導回這一頁：
//   付款完成 → /linepay/confirm/:orderId?transactionId=...
//   按了取消 → /linepay/cancel/:orderId
// 這頁負責通知後端完成扣款（或取消訂單），再帶使用者去看訂單。
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import httpClient from '@/service/httpClient'

const route = useRoute()
const router = useRouter()

// processing：處理中 / paid：付款完成 / cancelled：已取消 / failed：失敗
const state = ref('processing')
const errorMessage = ref('')

const orderId = route.params.orderId

async function finishPayment() {
  // transactionId 是 19 位數，一定要保持字串，不能轉成 Number，否則尾數會跑掉
  const transactionId = route.query.transactionId
  if (!transactionId) {
    state.value = 'failed'
    errorMessage.value = '缺少 LINE Pay 交易編號，無法確認付款'
    return
  }

  try {
    await httpClient.post(`/orders/${orderId}/linepay/confirm`, { transactionId })
    state.value = 'paid'
    // 用 replace 取代目前這頁，使用者按上一頁就不會回到這裡重複確認
    setTimeout(() => router.replace(`/orders/${orderId}`), 1500)
  } catch (error) {
    state.value = 'failed'
    errorMessage.value = error.response?.data?.message || '付款確認失敗，請稍後再試'
  }
}

async function cancelPayment() {
  try {
    await httpClient.post(`/orders/${orderId}/linepay/cancel`)
  } catch (error) {
    // 取消失敗也一樣顯示已取消畫面，訂單狀態以「我的訂單」為準
  }
  state.value = 'cancelled'
}

onMounted(() => {
  if (route.name === 'linepay-cancel') {
    cancelPayment()
  } else {
    finishPayment()
  }
})
</script>

<template>
  <div class="text-center py-5" style="max-width: 480px; margin: 0 auto;">
    <template v-if="state === 'processing'">
      <div class="spinner-border text-secondary mb-3" role="status"></div>
      <p class="text-secondary">正在確認 LINE Pay 付款結果，請不要關閉頁面</p>
    </template>

    <template v-else-if="state === 'paid'">
      <h4 class="mb-2">付款完成</h4>
      <p class="text-secondary">即將前往訂單明細</p>
    </template>

    <template v-else-if="state === 'cancelled'">
      <h4 class="mb-2">已取消付款</h4>
      <p class="text-secondary mb-4">這筆訂單已取消，商品不會出貨。</p>
      <router-link to="/products" class="btn btn-dark rounded-pill px-4">繼續購物</router-link>
    </template>

    <template v-else>
      <h4 class="mb-2">付款沒有完成</h4>
      <p class="text-secondary mb-4">{{ errorMessage }}</p>
      <router-link to="/orders" class="btn btn-outline-dark rounded-pill px-4">查看我的訂單</router-link>
    </template>
  </div>
</template>