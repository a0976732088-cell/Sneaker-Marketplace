<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";

import { getMyCoupons } from "@/service/couponService";

const router = useRouter();

const coupons = ref([]);
const loading = ref(true);
const errorMessage = ref("");

function getStatusText(status) {
  switch (status) {
    case "AVAILABLE":
      return "可使用";

    case "USED":
      return "已使用";

    case "EXPIRED":
      return "已過期";

    default:
      return status;
  }
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }

  return value
    .replace("T", " ")
    .substring(0, 16);
}

async function loadCoupons() {
  loading.value = true;
  errorMessage.value = "";

  try {
    const response = await getMyCoupons();

    coupons.value = response.data;
  } catch (error) {
    const status =
      error.status ||
      error.response?.status;

    if (status === 401) {
      await router.replace("/login");
      return;
    }

    errorMessage.value =
      error.response?.data?.message ||
      "無法載入折價券";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadCoupons();
});
</script>

<template>
  <div class="member-coupon-page">

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
         折價券內容
    ========================================== -->

    <main class="coupon-container">

      <div class="page-header">

        <div>
          <h1>我的折價券</h1>

          <p>
            新會員可獲得一張 100 元折價券，
            有效期限 30 天。
          </p>
        </div>

      </div>


      <!-- Loading -->

      <div
        v-if="loading"
        class="status-box"
      >
        折價券載入中...
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
        v-else-if="coupons.length === 0"
        class="empty-box"
      >
        目前沒有折價券。
      </div>


      <!-- =========================================
           Coupon List
      ========================================== -->

      <div
        v-else
        class="coupon-grid"
      >

        <div
          v-for="coupon in coupons"
          :key="coupon.couponId"
          class="coupon-card"
        >

          <!-- 上方 -->

          <div class="coupon-card-header">

            <h2>
              {{ coupon.couponName }}
            </h2>

            <span
              class="coupon-status"
              :class="{
                available:
                  coupon.status === 'AVAILABLE',

                used:
                  coupon.status === 'USED',

                expired:
                  coupon.status === 'EXPIRED'
              }"
            >
              {{ getStatusText(coupon.status) }}
            </span>

          </div>


          <!-- 金額 -->

          <div class="coupon-amount">
            NT$ {{ coupon.discountAmount }}
          </div>


          <!-- 詳細資料 -->

          <div class="coupon-info">

            <div class="coupon-info-row">

              <span class="label">
                使用條件：
              </span>

              <span>
                單筆商品小計需超過
                NT$ {{ coupon.minSpend }}
              </span>

            </div>


            <div class="coupon-info-row">

              <span class="label">
                發放時間：
              </span>

              <span>
                {{ formatDateTime(coupon.issuedAt) }}
              </span>

            </div>


            <div class="coupon-info-row">

              <span class="label">
                到期時間：
              </span>

              <span>
                {{ formatDateTime(coupon.expiresAt) }}
              </span>

            </div>

          </div>

        </div>

      </div>

    </main>

  </div>
</template>


<style scoped>

/* =========================================
   Page
========================================= */

.member-coupon-page {
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

.coupon-container {
  width: min(100% - 40px, 1100px);

  margin: 0 auto;

  padding: 45px 0 70px;
}


/* =========================================
   Header
========================================= */

.page-header {
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
   Coupon Grid
========================================= */

.coupon-grid {
  display: grid;

  grid-template-columns:
    repeat(
      auto-fit,
      minmax(320px, 1fr)
    );

  gap: 20px;
}


/* =========================================
   Coupon Card
========================================= */

.coupon-card {
  padding: 26px;

  background: #fff;

  border: 1px solid #ddd;

  border-radius: 10px;

  transition: 0.2s;

  box-shadow:
    0 2px 8px
    rgba(0, 0, 0, 0.04);
}


.coupon-card:hover {
  transform: translateY(-2px);

  box-shadow:
    0 6px 18px
    rgba(0, 0, 0, 0.08);
}


/* =========================================
   Coupon Header
========================================= */

.coupon-card-header {
  display: flex;

  justify-content: space-between;

  align-items: flex-start;

  gap: 20px;

  margin-bottom: 20px;
}


.coupon-card-header h2 {
  margin: 0;

  font-size: 20px;

  font-weight: 500;
}


/* =========================================
   Status
========================================= */

.coupon-status {
  flex-shrink: 0;

  padding: 6px 10px;

  border-radius: 6px;

  font-size: 13px;

  font-weight: 600;

  background: #eee;

  color: #555;
}


.coupon-status.available {
  background: #e8f5e9;

  color: #256029;
}


.coupon-status.used {
  background: #6c757d;

  color: #fff;
}


.coupon-status.expired {
  background: #fdecea;

  color: #b42318;
}


/* =========================================
   Amount
========================================= */

.coupon-amount {
  margin-bottom: 22px;

  font-size: 42px;

  line-height: 1;

  font-weight: 700;
}


/* =========================================
   Coupon Info
========================================= */

.coupon-info {
  display: flex;

  flex-direction: column;

  gap: 8px;

  font-size: 15px;
}


.coupon-info-row {
  display: flex;

  gap: 6px;

  line-height: 1.6;
}


.coupon-info-row .label {
  flex-shrink: 0;

  font-weight: 500;
}


/* =========================================
   RWD
========================================= */

@media (max-width: 768px) {

  .member-nav {
    justify-content: flex-start;

    overflow-x: auto;
  }


  .coupon-container {
    width: min(100% - 30px, 1100px);

    padding-top: 30px;
  }


  .coupon-grid {
    grid-template-columns: 1fr;
  }


  .coupon-card-header {
    gap: 12px;
  }


  .coupon-amount {
    font-size: 36px;
  }

}

</style>