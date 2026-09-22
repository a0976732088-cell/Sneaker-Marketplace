<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import httpClient from '@/service/httpClient'
import { getCurrentMember } from '@/service/authService'
import CheckoutSteps from '@/components/commerce/CheckoutSteps.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const errorMessage = ref('')
const order = ref(null)

const memberInfo = ref({
  name: '',
  email: '',
  phone: ''
})

let isUnmounted = false
let currentPollSession = 0 // 用於防止跨路由變更時的輪詢競態條件

// 是否為綠界付款完成後剛導回來
const justPaid = computed(() => route.query.fromPayment === 'true')

// 千分位金額格式化
function formatPrice(value) {
  if (value === null || value === undefined || isNaN(Number(value))) {
    return '0'
  }
  return Number(value).toLocaleString('zh-TW')
}

// 日期時間格式化
function formatDateTime(dateString) {
  if (!dateString) return '—'
  const date = new Date(dateString)
  if (isNaN(date.getTime())) return '—'

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')

  return `${year}/${month}/${day} ${hour}:${minute}`
}

// 載入訂單
async function fetchOrder(autoFinishLoading = true) {
  errorMessage.value = ''

  try {
    const response = await httpClient.get(`/orders/${route.params.id}`)

    if (isUnmounted) return false

    order.value = response.data
    return true
  } catch (error) {
    if (!isUnmounted) {
      errorMessage.value = error?.response?.data?.message || '無法載入訂單資料'
    }
    return false
  } finally {
    if (!isUnmounted && autoFinishLoading) {
      loading.value = false
    }
  }
}

// 綠界付款完成後，等待後端 /notify 更新付款狀態
async function fetchOrderAfterPayment() {
  const maxAttempts = 5
  const sessionId = ++currentPollSession

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    // 若已卸載或開啟了新的輪詢會話，則中斷舊輪詢
    if (isUnmounted || sessionId !== currentPollSession) break

    const success = await fetchOrder(false)
    if (!success || isUnmounted || sessionId !== currentPollSession) break

    if (order.value?.status === 'PAID') break

    if (attempt < maxAttempts) {
      await new Promise(resolve => setTimeout(resolve, 1000))
    }
  }

  if (!isUnmounted && sessionId === currentPollSession) {
    loading.value = false
  }
}

// 載入會員資料
async function loadMember() {
  try {
    const member = await getCurrentMember()
    if (isUnmounted) return

    if (member) {
      memberInfo.value = {
        name: member.name || '',
        email: member.email || '',
        phone: member.phone || ''
      }
    }
  } catch (error) {
    // 會員資料載入失敗不影響訂單顯示
  }
}

// 付款方式標籤
function paymentMethodLabel(method) {
  if (!method) return '—'
  const labels = {
    MOCK_CREDIT_CARD: '信用卡',
    ECPAY_CREDIT_CARD: '信用卡',
    CREDIT_CARD: '信用卡',
    ATM: 'ATM 轉帳',
    LINE_PAY: 'LINE Pay',
    CVS_PAY: '超商付款'
  }
  return labels[method] || method
}

// 付款狀態標籤
function statusLabel(status) {
  if (!status) return '—'
  const labels = {
    PAID: '已付款',
    PENDING: '確認付款中',
    UNPAID: '未付款',
    FAILED: '付款失敗',
    CANCELLED: '已取消',
    REFUNDED: '已退款'
  }
  return labels[status] || status
}

// 配送方式標籤
function shippingMethodLabel(method) {
  if (!method) return '宅配到府'
  const labels = {
    HOME: '宅配到府',
    HOME_DELIVERY: '宅配到府',
    CVS: '7-ELEVEN 超商取貨'
  }
  return labels[method] || method
}

// 動態物流狀態顯示
function shippingStatusLabel(orderStatus) {
  if (!orderStatus) return '處理中'
  if (['CANCELLED', 'FAILED', 'REFUNDED'].includes(orderStatus)) {
    return '已取消'
  }
  if (orderStatus === 'PAID') {
    return '待出貨'
  }
  return '未完成付款'
}

// 初始化
function initData() {
  loading.value = true

  if (justPaid.value) {
    fetchOrderAfterPayment()
  } else {
    fetchOrder(true)
  }

  loadMember()
}

// 監聽訂單 ID 變更
watch(
  () => route.params.id,
  newId => {
    if (newId) {
      initData()
    }
  }
)

onMounted(() => {
  initData()
})

onUnmounted(() => {
  isUnmounted = true
  currentPollSession++
})
</script>

<template>
  <!-- 載入中 -->
  <div v-if="loading" class="loading-area">
    <div class="loading-spinner"></div>
    <p>正在載入訂單資料...</p>
  </div>

  <!-- 錯誤 -->
  <div v-else-if="errorMessage" class="container py-5">
    <div class="alert alert-danger text-center">
      {{ errorMessage }}
    </div>
  </div>

  <!-- 訂單內容 -->
  <main v-else-if="order" class="order-page">
    <div class="container order-container">

      <!-- 付款完成 -->
      <section v-if="justPaid" class="success-section">

        <div class="success-icon">
          ✓
        </div>

        <h1>感謝您的購買</h1>

        <p>
          已收到您的訂單，請檢視訂單，確認您的付款狀態
        </p>

        <div class="success-actions">
          <button
            type="button"
            class="btn btn-dark"
            @click="router.push('/')"
          >
            返回首頁
          </button>

          <button
            type="button"
            class="btn btn-outline-dark"
            @click="router.push('/orders')"
          >
            查詢所有訂單
          </button>
        </div>
      </section>

      <!-- 一般訂單頁 -->
      <section v-else class="normal-header">
        <h1>訂單明細</h1>

        <button
          type="button"
          class="btn btn-outline-dark"
          @click="router.push('/orders')"
        >
          返回訂單列表
        </button>
      </section>

      <!-- 訂單明細 -->
      <section class="order-card">

        <!-- 標題 -->
        <div class="order-card-title">
          訂單明細
        </div>

        <div class="order-card-body">

          <!-- 訂單基本資訊 -->
          <div class="order-header">
            <div class="order-number">
              訂單編號：
              <strong>{{ order.orderNumber }}</strong>
            </div>

            <div class="order-date">
              訂單日：{{ formatDateTime(order.createdAt) }}
            </div>
          </div>

          <!-- 商品表格 -->
          <div class="product-table">

            <div class="product-table-header">
              <div>商品名稱</div>
              <div>規格</div>
              <div>數量</div>
              <div>價格</div>
              <div>小計</div>
            </div>

            <div
              v-for="item in (order.items || [])"
              :key="item.orderItemId || item.id"
              class="product-row"
            >
              <!-- 商品名稱 -->
              <div class="product-name-cell">

                <div class="product-image">
                  <img
                    v-if="item.imageUrl"
                    :src="item.imageUrl"
                    :alt="item.productName"
                  />

                  <div v-else class="no-image">
                    無圖片
                  </div>
                </div>

                <span>
                  {{ item.productName }}
                </span>
              </div>

              <!-- 規格 -->
              <div class="product-spec">
                {{ item.specification || '—' }}
              </div>

              <!-- 數量 -->
              <div class="product-quantity">
                {{ item.quantity }}
              </div>

              <!-- 單價 -->
              <div class="product-unit-price">
                NT$ {{ formatPrice(item.unitPrice) }}
              </div>

              <!-- 小計 -->
              <div class="product-line-total">
                NT$ {{ formatPrice(item.lineTotal) }}
              </div>
            </div>

          </div>

          <!-- 金額 -->
          <div class="price-summary">

            <div class="price-row">
              <span>商品總計</span>
              <span>
                NT$ {{ formatPrice(order.subtotal) }}
              </span>
            </div>

            <div class="price-row">
              <span>運費</span>
              <span>
                NT$ {{ formatPrice(order.shippingFee) }}
              </span>
            </div>

            <div
              v-if="order.discountAmount > 0"
              class="price-row discount-row"
            >
              <span>優惠折扣</span>
              <span>
                - NT$ {{ formatPrice(order.discountAmount) }}
              </span>
            </div>

            <div class="price-divider"></div>

            <div class="price-row total-row">
              <strong>訂單總金額</strong>

              <strong>
                NT$ {{ formatPrice(order.totalAmount) }}
              </strong>
            </div>

          </div>

          <!-- 下方資訊 -->
          <div class="detail-grid">

            <!-- 左側 -->
            <div class="detail-column">

              <!-- 會員資訊 -->
              <div class="detail-block">
                <h3>會員資訊</h3>

                <div class="detail-text">
                  {{ memberInfo?.name || '—' }}
                </div>

                <div class="detail-text muted">
                  {{ memberInfo?.email || '—' }}
                </div>

                <div class="detail-text muted">
                  {{ memberInfo?.phone || '—' }}
                </div>
              </div>

              <!-- 付款狀態 -->
              <div class="detail-block">
                <h3>付款狀態</h3>

                <span class="status-badge">
                  <span
                    class="status-dot"
                    :class="{ paid: order.status === 'PAID' }"
                  ></span>

                  {{ statusLabel(order.status) }}
                </span>
              </div>

              <!-- 付款方式 -->
              <div class="detail-block">
                <h3>付款方式</h3>

                <span class="status-badge">
                  {{ paymentMethodLabel(order.paymentMethod) }}
                </span>
              </div>

              <!-- 付款時間 -->
              <div
                v-if="order.paidAt"
                class="detail-block"
              >
                <h3>付款時間</h3>

                <div class="detail-text muted">
                  {{ formatDateTime(order.paidAt) }}
                </div>
              </div>

            </div>

            <!-- 右側 -->
            <div class="detail-column">

              <!-- 收件人資訊 -->
              <div class="detail-block">
                <h3>收件人資訊</h3>

                <div class="detail-text">
                  {{ order.receiverName || '—' }}
                </div>

                <div class="detail-text muted">
                  {{ order.receiverPhone || '—' }}
                </div>

                <div class="detail-text muted">
                  {{ order.shippingAddress || '—' }}
                </div>
              </div>

              <!-- 運送狀態 -->
              <div class="detail-block">
                <h3>運送狀態</h3>

                <span class="status-badge">
                  <span class="status-dot"></span>
                  待出貨
                </span>
              </div>

              <!-- 運送方式 -->
              <div class="detail-block">
                <h3>運送方式</h3>

                <span class="status-badge">
                  {{ shippingMethodLabel(order.shippingMethod) }}
                </span>

                <!-- 超商門市 -->
                <div
                  v-if="
                    order.shippingMethod === 'CVS' &&
                    order.cvsStoreName
                  "
                  class="cvs-store-info"
                >
                  <div>
                    取貨門市：{{ order.cvsStoreName }}
                  </div>

                  <div v-if="order.cvsAddress">
                    {{ order.cvsAddress }}
                  </div>

                  <div
                    v-if="order.cvsStoreId"
                    class="cvs-store-id"
                  >
                    門市代號：{{ order.cvsStoreId }}
                  </div>
                </div>
              </div>

            </div>

          </div>

        </div>
      </section>

    </div>
  </main>
</template>

<style scoped>
/* =========================
   整體
========================= */

.order-page {
  min-height: 100vh;
  background: #ffffff;
  color: #222222;
}

.order-container {
  max-width: 1120px;
  margin: 0 auto;
  padding-top: 2rem;
  padding-bottom: 3rem;
}

/* =========================
   Loading
========================= */

.loading-area {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #777777;
  background: #ffffff;
}

.loading-spinner {
  width: 35px;
  height: 35px;
  border: 3px solid #e5e5e5;
  border-top-color: #222222;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* =========================
   付款完成
========================= */

.success-section {
  text-align: center;
  padding: 0.5rem 0 3rem;
}

.success-icon {
  width: 58px;
  height: 58px;
  margin: 0 auto 1rem;
  border: 1px solid #555555;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #222222;
  font-size: 1.55rem;
  font-weight: 600;
}

.success-section h1 {
  margin: 0;
  font-size: 1.8rem;
  font-weight: 700;
  color: #222222;
}

.success-section p {
  margin: 0.45rem 0 1.25rem;
  color: #888888;
  font-size: 0.9rem;
}

.success-actions {
  display: flex;
  justify-content: center;
  gap: 0.5rem;
}

/* =========================
   一般標題
========================= */

.normal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.normal-header h1 {
  margin: 0;
  font-size: 1.7rem;
  font-weight: 700;
  color: #222222;
}

/* =========================
   訂單卡片
========================= */

.order-card {
  border: 1px solid #e1e4e8;
  border-radius: 6px;
  background: #ffffff;
  overflow: hidden;
}

.order-card-title {
  padding: 0.7rem 1rem;
  text-align: center;
  background: #f8f9fa;
  border-bottom: 1px solid #e1e4e8;
  font-size: 0.95rem;
  font-weight: 700;
  color: #333333;
}

.order-card-body {
  padding: 1.4rem 1.25rem 1.2rem;
}

/* =========================
   訂單基本資訊
========================= */

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.15rem;
  font-size: 0.9rem;
}

.order-number {
  color: #444444;
}

.order-number strong {
  font-weight: 500;
  color: #222222;
}

.order-date {
  color: #777777;
}

/* =========================
   商品表格
========================= */

.product-table {
  width: 100%;
}

.product-table-header,
.product-row {
  display: grid;
  grid-template-columns:
    minmax(240px, 2fr)
    minmax(130px, 1.2fr)
    80px
    120px
    120px;
  align-items: center;
}

.product-table-header {
  padding: 0 0.35rem 0.55rem;
  border-bottom: 1px solid #dddddd;
  color: #333333;
  font-size: 0.78rem;
  font-weight: 700;
}

.product-table-header > div:nth-child(n + 3) {
  text-align: right;
}

.product-row {
  min-height: 55px;
  padding: 0.55rem 0.35rem;
  border-bottom: 1px solid #eeeeee;
  font-size: 0.82rem;
}

.product-name-cell {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  min-width: 0;
  color: #222222;
  font-weight: 500;
}

.product-image {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  overflow: hidden;
  background: #f4f4f4;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  color: #999999;
  font-size: 0.55rem;
}

.product-spec {
  color: #555555;
}

.product-quantity,
.product-unit-price,
.product-line-total {
  text-align: right;
  color: #333333;
}

.product-line-total {
  font-weight: 500;
}

/* =========================
   金額
========================= */

.price-summary {
  width: 300px;
  margin-left: auto;
  padding-top: 0.85rem;
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.3rem 0;
  font-size: 0.82rem;
  color: #444444;
}

.discount-row {
  color: #777777;
}

.price-divider {
  height: 1px;
  background: #dddddd;
  margin: 0.55rem 0;
}

.total-row {
  padding-top: 0.6rem;
  font-size: 0.92rem;
}

.total-row strong:last-child {
  font-size: 1rem;
  color: #222222;
}

/* =========================
   下方資訊
========================= */

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 3rem;
  margin-top: 1.25rem;
  padding-top: 1.15rem;
  border-top: 1px solid #dddddd;
}

.detail-column {
  min-width: 0;
}

.detail-block {
  margin-bottom: 1.05rem;
}

.detail-block:last-child {
  margin-bottom: 0;
}

.detail-block h3 {
  margin: 0 0 0.45rem;
  color: #222222;
  font-size: 0.84rem;
  font-weight: 700;
}

.detail-text {
  margin-bottom: 0.15rem;
  color: #333333;
  font-size: 0.8rem;
  line-height: 1.5;
}

.detail-text.muted {
  color: #777777;
}

/* =========================
   狀態 Badge
========================= */

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  background: #737a7f;
  color: #ffffff;
  font-size: 0.68rem;
  font-weight: 600;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #d9b45b;
}

.status-dot.paid {
  background: #72b58a;
}

/* =========================
   超商門市
========================= */

.cvs-store-info {
  margin-top: 0.45rem;
  color: #777777;
  font-size: 0.76rem;
  line-height: 1.5;
}

.cvs-store-id {
  color: #999999;
  font-size: 0.7rem;
}

/* =========================
   RWD
========================= */

@media (max-width: 768px) {
  .order-container {
    padding: 1.25rem 1rem 2.5rem;
  }

  .success-section {
    padding-bottom: 2rem;
  }

  .success-section h1 {
    font-size: 1.55rem;
  }

  .normal-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 0.8rem;
  }

  .order-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 0.35rem;
  }

  .product-table {
    overflow-x: auto;
  }

  .product-table-header,
  .product-row {
    min-width: 720px;
  }

  .price-summary {
    width: 100%;
    max-width: 300px;
  }

  .detail-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .detail-column + .detail-column {
    margin-top: 0.5rem;
  }
}

@media (max-width: 480px) {
  .success-actions {
    flex-direction: column;
    width: 100%;
  }

  .success-actions .btn {
    width: 100%;
  }

  .order-card-body {
    padding: 1rem;
  }

  .order-card-title {
    font-size: 0.9rem;
  }
}
</style>