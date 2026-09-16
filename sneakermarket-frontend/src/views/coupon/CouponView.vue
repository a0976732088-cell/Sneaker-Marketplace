<script setup>
import {
  onMounted,
  ref
} from "vue";

import {
  useRouter
} from "vue-router";

import {
  getMyCoupons
} from "@/service/couponService";


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

  try {

    const response =
      await getMyCoupons();

    coupons.value =
      response.data;


  } catch (error) {

    const status =
      error.status
      ||
      error.response?.status;


    if (status === 401) {

      await router.replace(
        "/login"
      );

      return;
    }


    errorMessage.value =
      error.response?.data?.message
      ||
      "無法載入折價券";


  } finally {

    loading.value = false;
  }
}


onMounted(
  loadCoupons
);
</script>


<template>

  <div class="container py-4">

    <div
      class="d-flex justify-content-between align-items-center mb-4"
    >

      <div>

        <h2 class="mb-1">
          我的折價券
        </h2>

        <p class="text-secondary mb-0">
          新會員可獲得一張 100 元折價券，
          有效期限 30 天。
        </p>

      </div>


      <RouterLink
        to="/member"
        class="btn btn-outline-dark"
      >
        返回會員中心
      </RouterLink>

    </div>


    <div
      v-if="loading"
      class="text-secondary"
    >
      載入中...
    </div>


    <div
      v-else-if="errorMessage"
      class="alert alert-danger"
    >
      {{ errorMessage }}
    </div>


    <div
      v-else-if="coupons.length === 0"
      class="alert alert-light border"
    >
      目前沒有折價券。
    </div>


    <div
      v-else
      class="row g-3"
    >

      <div
        v-for="coupon in coupons"
        :key="coupon.couponId"
        class="col-md-6 col-lg-4"
      >

        <div class="card h-100 shadow-sm">

          <div class="card-body">

            <div
              class="d-flex justify-content-between gap-2 mb-3"
            >

              <h5 class="mb-0">
                {{ coupon.couponName }}
              </h5>


              <span
                class="badge"
                :class="{
                  'text-bg-success':
                    coupon.status === 'AVAILABLE',

                  'text-bg-secondary':
                    coupon.status === 'USED',

                  'text-bg-danger':
                    coupon.status === 'EXPIRED'
                }"
              >
                {{ getStatusText(coupon.status) }}
              </span>

            </div>


            <div class="display-6 fw-bold mb-3">
              NT$ {{ coupon.discountAmount }}
            </div>


            <p class="mb-1">
              使用條件：
              單筆商品小計需超過
              NT$ {{ coupon.minSpend }}
            </p>


            <p class="mb-1">
              發放時間：
              {{ formatDateTime(coupon.issuedAt) }}
            </p>


            <p class="mb-0">
              到期時間：
              {{ formatDateTime(coupon.expiresAt) }}
            </p>

          </div>

        </div>

      </div>

    </div>

  </div>

</template>