<template>
  <main class="container py-4">
    <!-- ==================================================
         頁面標題
    =================================================== -->
    <h1 class="mb-3">
      {{ title }}
    </h1>

    <!-- ==================================================
         我的競標導覽

         原本有：

         競標商城
         我的出價
         我的刊登
         我的競標訂單

         現在移除「競標商城」，
         只保留會員自己的競標功能。
    =================================================== -->
    <nav class="d-flex flex-wrap gap-2 mb-4" aria-label="我的競標導覽">
      <!-- 我的出價 -->
      <RouterLink class="btn btn-outline-primary" to="/auctions/me/bids">
        我的出價
      </RouterLink>

      <!-- 我的刊登 -->
      <RouterLink class="btn btn-outline-primary" to="/auctions/me/listings">
        我的刊登
      </RouterLink>

      <!-- 我的競標訂單 -->
      <RouterLink class="btn btn-outline-primary" to="/auction-orders">
        我的競標訂單
      </RouterLink>
    </nav>

    <!-- ==================================================
         子頁面額外切換區

         AuctionOrderListView 會使用這個 slot
         顯示：
         我買到的 / 我賣出的
    =================================================== -->
    <slot name="view-switch" />

    <!-- ==================================================
         Loading
    =================================================== -->
    <p v-if="loading" role="status">載入中…</p>

    <!-- ==================================================
         Error
    =================================================== -->
    <div v-else-if="errorMessage" class="alert alert-danger" role="alert">
      {{ errorMessage }}

      <button class="btn btn-sm btn-outline-danger ms-2" @click="loadPage()">
        重試
      </button>
    </div>

    <!-- ==================================================
         資料載入完成
    =================================================== -->
    <template v-else>
      <!-- 無資料 -->
      <p v-if="items.length === 0" class="alert alert-info">
        {{ emptyMessage }}
      </p>

      <!-- =================================================
           實際內容

           AuctionMyBidsView
           AuctionMyListingsView
           AuctionOrderListView

           會透過這個 slot 放入自己的內容。
      ================================================== -->
      <slot v-else :items="items" />

      <!-- =================================================
           分頁
      ================================================== -->
      <nav
        v-if="totalPages > 0 || currentPage > 0"
        class="d-flex align-items-center flex-wrap gap-3 mt-4"
        aria-label="分頁"
      >
        <!-- 上一頁 -->
        <button
          class="btn btn-outline-secondary"
          :disabled="currentPage === 0"
          @click="loadPage(currentPage - 1)"
        >
          上一頁
        </button>

        <!-- 頁數 -->
        <span>
          第
          {{ currentPage + 1 }}
          頁／共
          {{ totalPages }}
          頁，共
          {{ totalElements }}
          筆
        </span>

        <!-- 下一頁 -->
        <button
          class="btn btn-outline-secondary"
          :disabled="currentPage + 1 >= totalPages"
          @click="loadPage(currentPage + 1)"
        >
          下一頁
        </button>
      </nav>
    </template>
  </main>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from "vue";

import { useRouter } from "vue-router";

import { handleAuctionAccessError } from "@/service/auctionService";

// =====================================================
// Props
//
// 不同競標會員頁面會傳入：
//
// title
// emptyMessage
// fetchPage
// errorMessageFallback
// =====================================================
const props = defineProps({
  title: {
    type: String,
    required: true,
  },

  emptyMessage: {
    type: String,
    required: true,
  },

  fetchPage: {
    type: Function,
    required: true,
  },

  errorMessageFallback: {
    type: String,
    default: "無法載入資料，請稍後重試",
  },
});

// =====================================================
// Router
// =====================================================
const router = useRouter();

// =====================================================
// Data
// =====================================================
const items = ref([]);

const loading = ref(false);

const errorMessage = ref("");

// =====================================================
// Pagination
// =====================================================
const currentPage = ref(0);

const totalPages = ref(0);

const totalElements = ref(0);

// =====================================================
// 防止舊 API Response 覆蓋新資料
// =====================================================
let requestVersion = 0;

// =====================================================
// 載入會員競標資料
// =====================================================
async function loadPage(page = currentPage.value) {
  /*
   * 已經 loading
   * 或 page < 0
   * 就不繼續執行。
   */
  if (loading.value || page < 0) {
    return;
  }

  const version = ++requestVersion;

  loading.value = true;

  errorMessage.value = "";

  items.value = [];

  try {
    /*
     * fetchPage 由子頁面傳進來。
     *
     * 例如：
     *
     * 我的出價
     * getMyBids()
     *
     * 我的刊登
     * getMyListings()
     *
     * 競標訂單
     * getMyAuctionOrders()
     */
    const response = await props.fetchPage({
      page,

      size: 10,
    });

    /*
     * 如果這不是最新的 Request，
     * 就不要更新畫面。
     */
    if (version !== requestVersion) {
      return;
    }

    const data = response.data.data;

    items.value = data.items;

    currentPage.value = data.page;

    totalPages.value = data.totalPages;

    totalElements.value = data.totalElements;
  } catch (error) {
    /*
     * 如果已經有新的 Request，
     * 不處理舊 Error。
     */
    if (version !== requestVersion) {
      return;
    }

    totalPages.value = 0;

    totalElements.value = 0;

    /*
     * 例如：
     *
     * 401 未登入
     * 403 無權限
     *
     * 交給競標共用錯誤處理。
     */
    if (await handleAuctionAccessError(error, router)) {
      return;
    }

    errorMessage.value =
      error.response?.data?.message ?? props.errorMessageFallback;
  } finally {
    /*
     * 只有最新 Request
     * 才能結束 loading。
     */
    if (version === requestVersion) {
      loading.value = false;
    }
  }
}

// =====================================================
// 頁面載入
// =====================================================
onMounted(() => {
  loadPage();
});

// =====================================================
// 頁面離開
//
// 讓舊 Request 失效。
// =====================================================
onUnmounted(() => {
  requestVersion++;
});
</script>
