<template>
  <div class="auction-member-shell">
    <nav class="member-nav" aria-label="會員中心導覽">
      <RouterLink
        :to="{
          name: 'member',
        }"
      >
        個人檔案
      </RouterLink>

      <RouterLink
        :to="{
          name: 'coupons',
        }"
      >
        我的折價券
      </RouterLink>

      <RouterLink to="/member/orders"> 訂單 </RouterLink>

      <RouterLink
        :to="{
          name: 'auction-my-bids',
        }"
        class="router-link-active"
        aria-current="page"
      >
        競標
      </RouterLink>
    </nav>

    <main class="auction-member-page" :aria-label="title">
      <header class="auction-member-header">
        <h1>競標紀錄</h1>
        <p>管理你的出價、刊登商品與競標訂單</p>
      </header>

      <nav class="auction-member-tabs" aria-label="競標紀錄分類">
        <RouterLink
          :to="{ name: 'auction-my-bids' }"
          class="auction-member-tab"
          active-class="is-active"
        >
          出價紀錄
        </RouterLink>
        <RouterLink
          :to="{ name: 'auction-my-listings' }"
          class="auction-member-tab"
          active-class="is-active"
        >
          刊登紀錄
        </RouterLink>
        <RouterLink
          :to="{ name: 'auction-orders' }"
          class="auction-member-tab"
          active-class="is-active"
        >
          競標訂單
        </RouterLink>
      </nav>

      <slot name="view-switch" />

      <p v-if="loading" class="member-page-state" role="status">載入中…</p>

      <div v-else-if="errorMessage" class="member-page-state member-page-state--error" role="alert">
        <p>{{ errorMessage }}</p>
        <button type="button" class="state-action" @click="loadPage()">重試</button>
      </div>

      <template v-else>
        <section v-if="items.length === 0" class="member-empty-state">
          <div class="empty-state-mark" aria-hidden="true">—</div>
          <h2>{{ emptyTitle }}</h2>
          <p>{{ emptyMessage }}</p>
          <slot name="empty-action" />
        </section>

        <slot v-else :items="items" />

        <nav
          v-if="totalPages > 0 || currentPage > 0"
          class="member-pagination"
          aria-label="分頁"
        >
          <button
            type="button"
            class="pagination-button"
            :disabled="currentPage === 0"
            @click="loadPage(currentPage - 1)"
          >
            上一頁
          </button>

          <span class="pagination-summary">
            第 {{ currentPage + 1 }} 頁／共 {{ totalPages }} 頁，共 {{ totalElements }} 筆
          </span>

          <button
            type="button"
            class="pagination-button"
            :disabled="currentPage + 1 >= totalPages"
            @click="loadPage(currentPage + 1)"
          >
            下一頁
          </button>
        </nav>
      </template>
    </main>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from "vue";
import { useRouter } from "vue-router";

import { handleAuctionAccessError } from "@/service/auctionService";

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  emptyTitle: {
    type: String,
    default: "目前沒有紀錄",
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

const router = useRouter();

const items = ref([]);
const loading = ref(false);
const errorMessage = ref("");

const currentPage = ref(0);
const totalPages = ref(0);
const totalElements = ref(0);

let requestVersion = 0;

async function loadPage(page = currentPage.value) {
  if (loading.value || page < 0) {
    return;
  }

  const version = ++requestVersion;

  loading.value = true;
  errorMessage.value = "";
  items.value = [];

  try {
    const response = await props.fetchPage({
      page,
      size: 10,
    });

    if (version !== requestVersion) {
      return;
    }

    const data = response.data.data;

    items.value = data.items;
    currentPage.value = data.page;
    totalPages.value = data.totalPages;
    totalElements.value = data.totalElements;
  } catch (error) {
    if (version !== requestVersion) {
      return;
    }

    totalPages.value = 0;
    totalElements.value = 0;

    if (await handleAuctionAccessError(error, router)) {
      return;
    }

    errorMessage.value =
      error.response?.data?.message ?? props.errorMessageFallback;
  } finally {
    if (version === requestVersion) {
      loading.value = false;
    }
  }
}

onMounted(() => {
  loadPage();
});

onUnmounted(() => {
  requestVersion++;
});
</script>

<style scoped>
.auction-member-shell {
  min-height: 100vh;
  background: #f8f9fa;
  color: #111;
}

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

.member-nav a.router-link-active {
  text-decoration: underline;
  text-underline-offset: 6px;
}

.auction-member-page {
  width: min(calc(100% - 40px), 1100px);
  margin: 0 auto;
  padding: 42px 0 72px;
}

.auction-member-header h1 {
  margin: 0;
  font-size: clamp(1.8rem, 4vw, 2.35rem);
  font-weight: 700;
  letter-spacing: -0.035em;
}

.auction-member-header p {
  margin: 9px 0 0;
  color: #6c757d;
  font-size: 0.95rem;
}

.auction-member-tabs {
  display: flex;
  gap: 34px;
  margin: 30px 0 28px;
  border-bottom: 1px solid #dee2e6;
  overflow-x: auto;
  scrollbar-width: none;
}

.auction-member-tabs::-webkit-scrollbar {
  display: none;
}

.auction-member-tab {
  position: relative;
  flex: 0 0 auto;
  padding: 0 2px 13px;
  color: #6c757d;
  font-size: 0.95rem;
  font-weight: 650;
  text-decoration: none;
}

.auction-member-tab::after {
  position: absolute;
  right: 0;
  bottom: -1px;
  left: 0;
  height: 2px;
  background: #111;
  content: "";
  opacity: 0;
}

.auction-member-tab:hover,
.auction-member-tab:focus-visible,
.auction-member-tab.is-active {
  color: #111;
}

.auction-member-tab.is-active::after {
  opacity: 1;
}

.member-page-state,
.member-empty-state {
  margin: 0;
  padding: 38px;
  border: 1px solid #e9ecef;
  border-radius: 10px;
  background: #fff;
}

.member-page-state {
  color: #6c757d;
  text-align: center;
}

.member-page-state--error {
  color: #842029;
}

.member-page-state--error p {
  margin: 0 0 16px;
}

.state-action,
.pagination-button {
  min-height: 38px;
  padding: 8px 15px;
  border: 1px solid #111;
  border-radius: 6px;
  background: #fff;
  color: #111;
  font-weight: 650;
}

.state-action:hover,
.pagination-button:not(:disabled):hover {
  background: #111;
  color: #fff;
}

.member-empty-state {
  text-align: center;
}

.empty-state-mark {
  display: grid;
  width: 44px;
  height: 44px;
  margin: 0 auto 17px;
  place-items: center;
  border-radius: 50%;
  background: #f1f3f5;
  color: #6c757d;
}

.member-empty-state h2 {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 700;
}

.member-empty-state p {
  margin: 9px auto 0;
  color: #6c757d;
  font-size: 0.9rem;
}

.member-empty-state :deep(.empty-action) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  margin-top: 20px;
  padding: 9px 16px;
  border-radius: 6px;
  background: #111;
  color: #fff;
  font-size: 0.88rem;
  font-weight: 650;
  text-decoration: none;
}

.auction-member-page :deep(.auction-record-list) {
  display: grid;
  gap: 16px;
}

.member-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 28px;
}

.pagination-button:disabled {
  border-color: #dee2e6;
  color: #adb5bd;
  cursor: not-allowed;
}

.pagination-summary {
  color: #6c757d;
  font-size: 0.88rem;
}

@media (max-width: 768px) {
  .member-nav {
    justify-content: flex-start;
    overflow-x: auto;
  }
}

@media (max-width: 576px) {
  .auction-member-page {
    width: min(calc(100% - 32px), 1100px);
    padding: 32px 0 56px;
  }

  .auction-member-tabs {
    gap: 26px;
    margin-top: 25px;
  }

  .member-page-state,
  .member-empty-state {
    padding: 30px 20px;
  }

  .member-pagination {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .pagination-summary {
    grid-column: 1 / -1;
    grid-row: 1;
    text-align: center;
  }
}
</style>
