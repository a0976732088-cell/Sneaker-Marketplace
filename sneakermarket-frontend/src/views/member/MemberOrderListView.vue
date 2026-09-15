<script setup>
import { onMounted, ref } from "vue";

import { useRouter } from "vue-router";

import { getCurrentMember } from "@/service/authService";

import { getMyOrders } from "@/service/orderService";

const router = useRouter();

const member = ref(null);

const orders = ref([]);

const loading = ref(true);

const errorMessage = ref("");

// =====================================================
// 訂單狀態中文
// =====================================================
function getStatusText(status) {
  switch (status) {
    case "PAID":
      return "已付款";

    case "PENDING":
      return "待付款";

    case "COMPLETED":
      return "已完成";

    case "CANCELLED":
      return "已取消";

    default:
      return status;
  }
}

// =====================================================
// 日期格式化
// =====================================================
function formatDateTime(value) {
  if (!value) {
    return "-";
  }

  return value.replace("T", " ").substring(0, 16);
}

// =====================================================
// 金額格式
// =====================================================
function formatMoney(value) {
  return Number(value || 0).toLocaleString("zh-TW");
}

// =====================================================
// 取得會員訂單
// =====================================================
async function loadOrders() {
  loading.value = true;

  errorMessage.value = "";

  try {
    // =============================================
    // 先取得目前登入會員
    // =============================================
    member.value = await getCurrentMember();

    // =============================================
    // 再用 memberId 查訂單
    // =============================================
    const response = await getMyOrders(member.value.memberId);

    orders.value = response.data;
  } catch (error) {
    const status = error.status || error.response?.status;

    if (status === 401) {
      await router.replace("/login");

      return;
    }

    console.error("讀取訂單失敗：", error);

    errorMessage.value = error.response?.data?.message || "無法載入訂單資料";
  } finally {
    loading.value = false;
  }
}

// =====================================================
// 點擊某張訂單
// =====================================================
function openOrder(orderId) {
  router.push(`/orders/${orderId}`);
}

onMounted(() => {
  loadOrders();
});
</script>

<template>
  <div class="member-order-page">
    <!-- =========================================
         Member 導覽列
    ========================================== -->

    <nav class="member-nav">
      <router-link to="/member"> 個人檔案 </router-link>

      <router-link to="/coupons"> 我的折價券 </router-link>

      <router-link to="/member/orders"> 訂單 </router-link>

      <RouterLink :to="{ name: 'auction-my-bids' }"> 競標 </RouterLink>
    </nav>

    <!-- =========================================
         Content
    ========================================== -->

    <main class="order-container">
      <div class="page-header">
        <div>
          <h1>我的訂單</h1>

          <p>查看一般商城的購買紀錄</p>
        </div>
      </div>

      <!-- Loading -->

      <div v-if="loading" class="status-box">訂單載入中...</div>

      <!-- Error -->

      <div v-else-if="errorMessage" class="error-box">
        {{ errorMessage }}
      </div>

      <!-- 沒有訂單 -->

      <div v-else-if="orders.length === 0" class="empty-box">
        目前沒有一般商城訂單。
      </div>

      <!-- =====================================
           Order List
      ====================================== -->

      <div v-else class="order-list">
        <button
          v-for="order in orders"
          :key="order.orderId"
          class="order-card"
          @click="openOrder(order.orderId)"
        >
          <!-- 左邊 -->

          <div class="order-main">
            <div class="order-number">
              {{ order.orderNumber }}
            </div>

            <div class="order-date">
              下單時間：

              {{ formatDateTime(order.createdAt) }}
            </div>
          </div>

          <!-- 右邊 -->

          <div class="order-side">
            <div
              class="order-status"
              :class="{
                paid: order.status === 'PAID',

                completed: order.status === 'COMPLETED',

                cancelled: order.status === 'CANCELLED',
              }"
            >
              {{ getStatusText(order.status) }}
            </div>

            <div class="order-total">
              NT$

              {{ formatMoney(order.totalAmount) }}
            </div>

            <div class="order-arrow">查看訂單 ›</div>
          </div>
        </button>
      </div>
    </main>
  </div>
</template>

<style scoped>
/* =========================================
   整頁
========================================= */

.member-order-page {
  min-height: 100vh;

  background: #f8f9fa;

  color: #111;
}

/* =========================================
   Member Nav
========================================= */

.member-nav {
  display: flex;

  justify-content: center;

  gap: 35px;

  padding: 30px 20px;

  background: #fff;

  border-bottom: 1px solid #eee;
}

.member-nav a {
  color: #111;

  text-decoration: none;

  font-weight: 600;
}

.member-nav a:hover {
  color: #666;
}

.member-nav .router-link-active {
  text-decoration: underline;

  text-underline-offset: 6px;
}

/* =========================================
   Container
========================================= */

.order-container {
  width: min(100% - 40px, 1100px);

  margin: 0 auto;

  padding: 45px 0 70px;
}

.page-header {
  display: flex;

  justify-content: space-between;

  align-items: center;

  margin-bottom: 30px;
}

.page-header h1 {
  margin: 0;

  font-size: 30px;

  font-weight: 500;
}

.page-header p {
  margin: 8px 0 0;

  color: #777;
}

/* =========================================
   Loading / Empty / Error
========================================= */

.status-box,
.empty-box,
.error-box {
  padding: 30px;

  background: #fff;

  border: 1px solid #e5e5e5;

  border-radius: 8px;
}

.error-box {
  color: #b42318;

  background: #fdecea;
}

/* =========================================
   Order List
========================================= */

.order-list {
  display: flex;

  flex-direction: column;

  gap: 14px;
}

.order-card {
  width: 100%;

  display: flex;

  justify-content: space-between;

  align-items: center;

  gap: 24px;

  padding: 24px 28px;

  border: 1px solid #e5e5e5;

  border-radius: 8px;

  background: #fff;

  text-align: left;

  cursor: pointer;

  transition: 0.2s;
}

.order-card:hover {
  border-color: #999;

  transform: translateY(-1px);

  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

/* =========================================
   Left
========================================= */

.order-main {
  min-width: 0;
}

.order-number {
  font-size: 18px;

  font-weight: 600;

  margin-bottom: 8px;
}

.order-date {
  color: #777;

  font-size: 14px;
}

/* =========================================
   Right
========================================= */

.order-side {
  display: flex;

  align-items: center;

  gap: 22px;
}

.order-status {
  padding: 6px 10px;

  border-radius: 999px;

  background: #eee;

  font-size: 13px;
}

.order-status.paid {
  background: #e8f5e9;

  color: #256029;
}

.order-status.completed {
  background: #e3f2fd;

  color: #1565c0;
}

.order-status.cancelled {
  background: #fdecea;

  color: #b42318;
}

.order-total {
  min-width: 110px;

  text-align: right;

  font-size: 18px;

  font-weight: 600;
}

.order-arrow {
  color: #666;

  white-space: nowrap;
}

/* =========================================
   RWD
========================================= */

@media (max-width: 768px) {
  .member-nav {
    justify-content: flex-start;

    overflow-x: auto;
  }

  .order-card {
    flex-direction: column;

    align-items: flex-start;
  }

  .order-side {
    width: 100%;

    justify-content: space-between;

    flex-wrap: wrap;
  }

  .order-total {
    text-align: left;
  }
}
</style>
