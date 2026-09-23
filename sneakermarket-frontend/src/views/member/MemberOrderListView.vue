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
// 日期格式
// =====================================================
function formatDateTime(value) {
  if (!value) {
    return "-";
  }

  return value
    .replace("T", " ")
    .substring(0, 16);
}


// =====================================================
// 金額格式
// =====================================================
function formatMoney(value) {
  return Number(value || 0)
    .toLocaleString("zh-TW");
}


// =====================================================
// 商品名稱
// =====================================================
function getProductText(order) {
  if (!order.firstProductName) {
    return "商品資料";
  }

  if (!order.itemCount || order.itemCount <= 1) {
    return order.firstProductName;
  }

  return `${order.firstProductName} 等 ${order.itemCount} 件商品`;
}


// =====================================================
// 圖片網址處理
// =====================================================
function getProductImage(order) {
  if (!order.firstImageUrl) {
    return null;
  }

  // 已經是完整網址
  if (
    order.firstImageUrl.startsWith("http://") ||
    order.firstImageUrl.startsWith("https://")
  ) {
    return order.firstImageUrl;
  }

  // 本地靜態圖片，例如 /images/xxx.jpg
  if (order.firstImageUrl.startsWith("/")) {
    return order.firstImageUrl;
  }

  // 避免沒有 / 的情況
  return `/${order.firstImageUrl}`;
}


// =====================================================
// 圖片載入失敗
// =====================================================
function handleImageError(event) {
  event.target.style.display = "none";

  const placeholder =
    event.target.parentElement?.querySelector(
      ".image-placeholder"
    );

  if (placeholder) {
    placeholder.style.display = "flex";
  }
}


// =====================================================
// 取得會員訂單
// =====================================================
async function loadOrders() {
  loading.value = true;
  errorMessage.value = "";

  try {
    member.value =
      await getCurrentMember();

    const response =
      await getMyOrders(
        member.value.memberId
      );

    orders.value =
      response.data || [];
  } catch (error) {
    const status =
      error.status ||
      error.response?.status;

    if (status === 401) {
      await router.replace("/login");
      return;
    }

    console.error(
      "讀取訂單失敗：",
      error
    );

    errorMessage.value =
      error.response?.data?.message ||
      "無法載入訂單資料";
  } finally {
    loading.value = false;
  }
}


// =====================================================
// 點擊訂單
// =====================================================
function openOrder(orderId) {
  router.push(
    `/orders/${orderId}`
  );
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

      <RouterLink :to="{ name: 'member' }">
        個人檔案
      </RouterLink>

      <RouterLink :to="{ name: 'coupons' }">
        我的折價券
      </RouterLink>

      <RouterLink :to="{ name: 'member-orders' }">
        訂單
      </RouterLink>

      <RouterLink :to="{ name: 'auction-my-bids' }">
        競標
      </RouterLink>

    </nav>


    <!-- =========================================
         Content
    ========================================== -->

    <main class="order-container">

      <div class="page-header">

        <div>

          <h1>
            我的訂單
          </h1>

          <p>
            查看一般商城的購買紀錄
          </p>

        </div>

      </div>


      <!-- Loading -->

      <div
        v-if="loading"
        class="status-box"
      >
        訂單載入中...
      </div>


      <!-- Error -->

      <div
        v-else-if="errorMessage"
        class="error-box"
      >
        {{ errorMessage }}
      </div>


      <!-- Empty -->

      <div
        v-else-if="orders.length === 0"
        class="empty-box"
      >
        目前沒有一般商城訂單。
      </div>


      <!-- =====================================
           Order List
      ====================================== -->

      <div
        v-else
        class="order-list"
      >

        <button
          v-for="order in orders"
          :key="order.orderId"
          class="order-card"
          @click="openOrder(order.orderId)"
        >

          <!-- =================================
               商品圖片
          ================================== -->

          <div class="product-image-wrapper">

            <img
              v-if="getProductImage(order)"
              :src="getProductImage(order)"
              :alt="order.firstProductName || '商品圖片'"
              class="product-image"
              @error="handleImageError"
            />

            <div
              class="image-placeholder"
              :style="{
                display: getProductImage(order)
                  ? 'none'
                  : 'flex'
              }"
            >
              無圖片
            </div>

          </div>


          <!-- =================================
               左側資料
          ================================== -->

          <div class="order-main">

            <!-- 商品名稱 -->

            <div class="product-name">

              {{ getProductText(order) }}

            </div>


            <!-- 訂單編號 -->

            <div class="order-number">

              訂單編號：
              {{ order.orderNumber }}

            </div>


            <!-- 下單時間 -->

            <div class="order-date">

              下單時間：
              {{ formatDateTime(order.createdAt) }}

            </div>

          </div>


          <!-- =================================
               右側
          ================================== -->

          <div class="order-side">

            <div
              class="order-status"
              :class="{
                paid:
                  order.status === 'PAID',

                completed:
                  order.status === 'COMPLETED',

                cancelled:
                  order.status === 'CANCELLED'
              }"
            >
              {{ getStatusText(order.status) }}
            </div>


            <div class="order-total">

              NT$
              {{ formatMoney(order.totalAmount) }}

            </div>


            <div class="order-arrow">

              查看訂單 ›

            </div>

          </div>

        </button>

      </div>

    </main>

  </div>
</template>


<style scoped>

/* =========================================
   Page
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
   Loading / Error / Empty
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

  align-items: center;

  gap: 22px;

  padding: 20px 28px;

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

  box-shadow:
    0 4px 12px
    rgba(0, 0, 0, 0.06);
}


/* =========================================
   商品圖片
========================================= */

.product-image-wrapper {
  width: 105px;

  height: 105px;

  flex-shrink: 0;

  display: flex;

  justify-content: center;

  align-items: center;

  overflow: hidden;

  border-radius: 8px;

  border: 1px solid #e7e7e7;

  background: #f7f7f7;
}


.product-image {
  width: 100%;

  height: 100%;

  object-fit: cover;

  display: block;
}


.image-placeholder {
  width: 100%;

  height: 100%;

  justify-content: center;

  align-items: center;

  color: #aaa;

  font-size: 13px;

  background: #f4f4f4;
}


/* =========================================
   左側
========================================= */

.order-main {
  flex: 1;

  min-width: 0;
}


.product-name {
  margin-bottom: 10px;

  font-size: 20px;

  font-weight: 600;

  color: #111;

  overflow: hidden;

  white-space: nowrap;

  text-overflow: ellipsis;
}


.order-number {
  margin-bottom: 6px;

  font-size: 14px;

  color: #666;
}


.order-date {
  font-size: 14px;

  color: #888;
}


/* =========================================
   右側
========================================= */

.order-side {
  display: flex;

  align-items: center;

  gap: 28px;

  flex-shrink: 0;
}


.order-status {
  padding: 7px 12px;

  border-radius: 20px;

  background: #eee;

  color: #444;

  font-size: 14px;
}


.order-status.paid,
.order-status.completed {
  background: #e8f5e9;

  color: #256029;
}


.order-status.cancelled {
  background: #fdecea;

  color: #b42318;
}


.order-total {
  min-width: 110px;

  font-size: 20px;

  font-weight: 600;

  text-align: right;
}


.order-arrow {
  color: #777;

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


  .order-container {
    width: min(
      100% - 30px,
      1100px
    );

    padding-top: 30px;
  }


  .order-card {
    align-items: flex-start;

    flex-wrap: wrap;
  }


  .product-image-wrapper {
    width: 90px;

    height: 90px;
  }


  .order-main {
    width: calc(
      100% - 115px
    );
  }


  .product-name {
    white-space: normal;
  }


  .order-side {
    width: 100%;

    justify-content: flex-end;

    gap: 16px;

    margin-top: 8px;
  }


  .order-total {
    min-width: auto;
  }

}

</style>