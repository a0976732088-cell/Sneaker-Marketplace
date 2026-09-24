<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import httpClient from '@/service/httpClient'
import { resolveProductImageUrl } from '@/utils/productImageUrl'

const router = useRouter()
const loading = ref(true)
const errorMessage = ref('')
const orders = ref([])

// ===== 篩選條件 =====
const activeTab = ref('ALL')
const dateRange = ref('ALL')
const keyword = ref('')

// 狀態分頁籤：一個分頁可以對應多個狀態值
const statusTabs = [
  { value: 'ALL', label: '全部', statuses: [] },
  { value: 'PENDING', label: '待付款', statuses: ['PENDING', 'PENDING_PAYMENT'] },
  { value: 'PAID', label: '已付款', statuses: ['PAID'] },
  { value: 'SHIPPED', label: '已出貨', statuses: ['SHIPPED'] },
  { value: 'COMPLETED', label: '已完成', statuses: ['COMPLETED'] },
  { value: 'CANCELLED', label: '已取消', statuses: ['CANCELLED'] }
]

// 新的在上面
const sortedOrders = computed(() => {
  return [...orders.value].sort(
    (a, b) => new Date(b.createdAt) - new Date(a.createdAt)
  )
})

// 套用三種篩選後的結果
const filteredOrders = computed(() => {
  const tab = statusTabs.find(t => t.value === activeTab.value)
  const text = keyword.value.trim().toLowerCase()

  return sortedOrders.value.filter(order => {
    // ① 狀態
    if (tab && tab.statuses.length > 0 && !tab.statuses.includes(order.status)) {
      return false
    }

    // ② 日期區間
    if (dateRange.value !== 'ALL') {
      const days = Number(dateRange.value)
      const limit = Date.now() - days * 24 * 60 * 60 * 1000
      if (new Date(order.createdAt).getTime() < limit) {
        return false
      }
    }

    // ③ 關鍵字（訂單編號或商品名稱）
    if (text) {
      const orderNumber = (order.orderNumber || '').toLowerCase()
      const productName = (order.firstProductName || '').toLowerCase()
      if (!orderNumber.includes(text) && !productName.includes(text)) {
        return false
      }
    }

    return true
  })
})

const isFiltering = computed(() => {
  return activeTab.value !== 'ALL' || dateRange.value !== 'ALL' || keyword.value.trim() !== ''
})

function clearFilters() {
  activeTab.value = 'ALL'
  dateRange.value = 'ALL'
  keyword.value = ''
}

// 狀態中文（對照文件 orders.status 定義，PENDING 為目前系統實際使用的值）
function statusLabel(status) {
  const labels = {
    PENDING: '待付款',
    PENDING_PAYMENT: '待付款',
    PAID: '已付款',
    PROCESSING: '處理中',
    SHIPPED: '已出貨',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }

  return labels[status] || status
}

function statusBadgeClass(status) {
  const classes = {
    PENDING: 'status-pending',
    PENDING_PAYMENT: 'status-pending',
    PAID: 'status-paid',
    PROCESSING: 'status-info',
    SHIPPED: 'status-info',
    COMPLETED: 'status-paid',
    CANCELLED: 'status-default'
  }

  return classes[status] || 'status-default'
}

function formatDateTime(dateString) {
  if (!dateString) return '—'

  const date = new Date(dateString)

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')

  return `${year}/${month}/${day} ${hour}:${minute}`
}

// 千分位
function formatPrice(value) {
  if (value === null || value === undefined || isNaN(Number(value))) {
    return '0'
  }
  return Number(value).toLocaleString('zh-TW')
}

function goToDetail(orderId) {
  router.push(`/orders/${orderId}`)
}

async function fetchOrders() {
  try {
    const response = await httpClient.get('/orders/me')
    orders.value = response.data
  } catch (error) {
    if (error.response?.status === 401) {
      router.push({
        name: 'login',
        query: { redirect: '/orders' }
      })
      return
    }

    errorMessage.value = '無法載入訂單資料'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchOrders()
})
</script>

<template>
  <div class="orders-page">

    <!-- 標題 -->
    <div class="orders-header">
      <div>
        <h5 class="orders-title">我的訂單</h5>
        <p class="orders-subtitle">
          查看您的訂單紀錄與付款狀態
        </p>
      </div>

      <span
        v-if="!loading && !errorMessage && orders.length > 0"
        class="order-count"
      >
        <template v-if="isFiltering">
          顯示 {{ filteredOrders.length }} 筆，共 {{ orders.length }} 筆訂單
        </template>
        <template v-else>
          共 {{ orders.length }} 筆訂單
        </template>
      </span>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="orders-state">
      <div class="spinner-border spinner-border-sm me-2" role="status"></div>
      載入中...
    </div>

    <!-- Error -->
    <div v-else-if="errorMessage" class="alert alert-danger">
      {{ errorMessage }}
    </div>

    <!-- Empty：完全沒有訂單 -->
    <div v-else-if="orders.length === 0" class="empty-orders">
      <div class="empty-icon">
        🛍️
      </div>

      <h6>目前還沒有任何訂單</h6>

      <p>
        完成購物後，您的訂單會顯示在這裡
      </p>
    </div>

    <template v-else>

      <!-- 篩選列 -->
      <div class="order-filters">

        <div class="status-tabs">
          <button
            v-for="tab in statusTabs"
            :key="tab.value"
            type="button"
            class="status-tab"
            :class="{ active: activeTab === tab.value }"
            @click="activeTab = tab.value"
          >
            {{ tab.label }}
          </button>
        </div>

        <div class="filter-row">
          <select v-model="dateRange" class="form-select filter-date">
            <option value="ALL">全部時間</option>
            <option value="30">近 30 天</option>
            <option value="90">近 3 個月</option>
          </select>

          <input
            v-model="keyword"
            type="text"
            class="form-control filter-keyword"
            placeholder="搜尋訂單編號或商品名稱"
          />
        </div>

      </div>

      <!-- Empty：有訂單但篩選後沒結果 -->
      <div v-if="filteredOrders.length === 0" class="empty-orders">
        <h6>沒有符合條件的訂單</h6>

        <p>
          試試調整狀態、日期或關鍵字
        </p>

        <button
          type="button"
          class="clear-button"
          @click="clearFilters"
        >
          清除篩選
        </button>
      </div>

      <!-- Order List -->
      <div v-else class="order-list">

        <div
          v-for="order in filteredOrders"
          :key="order.orderId"
          class="order-card"
          @click="goToDetail(order.orderId)"
        >

          <!-- 卡片標題列：日期＋狀態 -->
          <div class="order-card-header">
            <span class="order-date">
              {{ formatDateTime(order.createdAt) }}
            </span>

            <span
              class="status-badge"
              :class="statusBadgeClass(order.status)"
            >
              <span class="status-dot"></span>
              {{ statusLabel(order.status) }}
            </span>
          </div>

          <!-- 卡片內容：圖片＋商品＋金額 -->
          <div class="order-card-body">

            <div class="order-thumb">
              <img
                v-if="order.firstImageUrl"
                :src="resolveProductImageUrl(order.firstImageUrl)"
                :alt="order.firstProductName"
              />
              <div v-else class="no-image">
                無圖片
              </div>
            </div>

            <div class="order-main">
              <div class="product-name">
                {{ order.firstProductName || '商品資料已移除' }}
              </div>

              <div class="item-count">
                共 {{ order.itemCount }} 件商品
              </div>

              <div class="order-number">
                訂單編號 {{ order.orderNumber }}
              </div>
            </div>

            <div class="order-side">
              <div class="order-price">
                <span>NT$</span>
                {{ formatPrice(order.totalAmount) }}
              </div>

              <button
                type="button"
                class="detail-button"
                @click.stop="goToDetail(order.orderId)"
              >
                檢視訂單
              </button>
            </div>

          </div>

        </div>

      </div>

    </template>

  </div>
</template>

<style scoped>

.orders-page {
  width: 100%;
}

/* =========================
   Header
========================= */

.orders-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 20px;
}

.orders-title {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 700;
  color: #212529;
}

.orders-subtitle {
  margin: 6px 0 0;
  color: #8a8f98;
  font-size: 0.9rem;
}

.order-count {
  color: #8a8f98;
  font-size: 0.85rem;
}

/* =========================
   Filters
========================= */

.order-filters {
  margin-bottom: 20px;
}

.status-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.status-tab {
  padding: 6px 14px;

  border: 1px solid #dee2e6;
  border-radius: 999px;

  background: #ffffff;
  color: #495057;

  font-size: 0.85rem;

  transition:
    background 0.15s ease,
    border-color 0.15s ease,
    color 0.15s ease;
}

.status-tab:hover {
  border-color: #adb5bd;
}

.status-tab.active {
  background: #212529;
  border-color: #212529;
  color: #ffffff;
}

.filter-row {
  display: flex;
  gap: 10px;
}

.filter-date {
  width: 150px;
  flex-shrink: 0;
  font-size: 0.88rem;
}

.filter-keyword {
  flex: 1;
  font-size: 0.88rem;
}

/* =========================
   Order List
========================= */

.order-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* =========================
   Order Card
========================= */

.order-card {
  padding: 20px 22px;

  background: #f4f4f5;
  border: 1px solid #e9e9ec;
  border-radius: 10px;

  cursor: pointer;

  transition: border-color 0.15s ease;
}

.order-card:hover {
  border-color: #6c757d;
}

.order-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  margin-bottom: 16px;
}

.order-date {
  font-size: 1rem;
  font-weight: 500;
  color: #343a40;
  letter-spacing: 0.04em;
}

.order-card-body {
  display: flex;
  align-items: center;
  gap: 20px;
}

/* =========================
   Thumbnail
========================= */

.order-thumb {
  width: 112px;
  height: 112px;
  flex-shrink: 0;

  overflow: hidden;

  background: #ffffff;

  display: flex;
  align-items: center;
  justify-content: center;
}

.order-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  color: #999999;
  font-size: 0.75rem;
}

/* =========================
   Main Info
========================= */

.order-main {
  flex: 1;
  min-width: 0;
}

.product-name {
  font-size: 1.05rem;
  font-weight: 500;
  color: #212529;
  letter-spacing: 0.01em;

  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-count {
  margin-top: 4px;

  color: #6c757d;
  font-size: 0.85rem;
}

.order-number {
  margin-top: 14px;

  color: #adb5bd;
  font-size: 0.75rem;
  letter-spacing: 0.03em;

  word-break: break-all;
}

/* =========================
   Side：金額＋按鈕
========================= */

.order-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 14px;
  flex-shrink: 0;
}

.order-price {
  font-size: 1.15rem;
  font-weight: 500;
  color: #212529;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.order-price span {
  margin-right: 3px;

  font-size: 0.8rem;
  font-weight: 400;
  color: #6c757d;
}

/* =========================
   Status：小圓點＋文字
========================= */

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;

  font-size: 0.82rem;
  font-weight: 500;

  white-space: nowrap;
}

.status-dot {
  width: 6px;
  height: 6px;

  border-radius: 50%;
}

/* 已付款、已完成 */
.status-paid { color: #27834a; }
.status-paid .status-dot { background: #38a169; }

/* 待付款 */
.status-pending { color: #b7791f; }
.status-pending .status-dot { background: #d69e2e; }

/* 處理中、已出貨 */
.status-info { color: #2b6cb0; }
.status-info .status-dot { background: #3182ce; }

/* 已取消、其他 */
.status-default { color: #868e96; }
.status-default .status-dot { background: #adb5bd; }

/* =========================
   Buttons
========================= */

.detail-button {
  padding: 7px 18px;

  border: 1px solid #212529;
  border-radius: 999px;

  background: transparent;
  color: #212529;

  font-size: 0.82rem;

  transition:
    background 0.15s ease,
    color 0.15s ease;
}

.detail-button:hover {
  background: #212529;
  color: #ffffff;
}

.clear-button {
  margin-top: 14px;
  padding: 7px 18px;

  border: 1px solid #212529;
  border-radius: 999px;

  background: #ffffff;
  color: #212529;

  font-size: 0.85rem;

  transition:
    background 0.15s ease,
    color 0.15s ease;
}

.clear-button:hover {
  background: #212529;
  color: #ffffff;
}

/* =========================
   Loading
========================= */

.orders-state {
  display: flex;
  justify-content: center;
  align-items: center;

  min-height: 180px;

  color: #8a8f98;
  font-size: 0.9rem;
}

/* =========================
   Empty
========================= */

.empty-orders {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  min-height: 240px;

  border: 1px dashed #dee2e6;
  border-radius: 12px;

  background: #fafafa;

  text-align: center;
}

.empty-icon {
  margin-bottom: 14px;

  font-size: 2.2rem;
}

.empty-orders h6 {
  margin-bottom: 6px;

  font-weight: 600;
  color: #343a40;
}

.empty-orders p {
  margin: 0;

  color: #8a8f98;
  font-size: 0.85rem;
}

/* =========================
   Mobile
========================= */

@media (max-width: 768px) {

  .orders-header {
    align-items: flex-start;
    gap: 12px;
  }

  .order-count {
    white-space: nowrap;
  }

  .filter-row {
    flex-direction: column;
  }

  .filter-date {
    width: 100%;
  }

  .order-card {
    padding: 16px;
  }

  .order-card-body {
    flex-wrap: wrap;
  }

  .order-thumb {
    width: 84px;
    height: 84px;
  }

  .order-side {
    width: 100%;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;

    padding-top: 12px;
    border-top: 1px solid #f0f1f2;
  }

}

</style>