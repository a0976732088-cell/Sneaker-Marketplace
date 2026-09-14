<template>
  <main class="container py-4">
    <h1 class="mb-3">{{ title }}</h1>
    <nav class="d-flex flex-wrap gap-2 mb-4" aria-label="我的競標導覽">
      <RouterLink class="btn btn-outline-secondary" to="/auctions">競標商城</RouterLink>
      <RouterLink class="btn btn-outline-primary" to="/auctions/me/bids">我的出價</RouterLink>
      <RouterLink class="btn btn-outline-primary" to="/auctions/me/listings">我的刊登</RouterLink>
      <RouterLink class="btn btn-outline-primary" to="/auction-orders">我的競標訂單</RouterLink>
    </nav>

    <slot name="view-switch" />

    <p v-if="loading" role="status">載入中…</p>
    <div v-else-if="errorMessage" class="alert alert-danger" role="alert">
      {{ errorMessage }}
      <button class="btn btn-sm btn-outline-danger ms-2" @click="loadPage()">重試</button>
    </div>
    <template v-else>
      <p v-if="items.length === 0" class="alert alert-info">{{ emptyMessage }}</p>
      <slot v-else :items="items" />
      <nav v-if="totalPages > 0 || currentPage > 0" class="d-flex align-items-center flex-wrap gap-3 mt-4" aria-label="分頁">
        <button class="btn btn-outline-secondary" :disabled="currentPage === 0" @click="loadPage(currentPage - 1)">上一頁</button>
        <span>第 {{ currentPage + 1 }} 頁／共 {{ totalPages }} 頁，共 {{ totalElements }} 筆</span>
        <button class="btn btn-outline-secondary" :disabled="currentPage + 1 >= totalPages" @click="loadPage(currentPage + 1)">下一頁</button>
      </nav>
    </template>
  </main>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from "vue";
import { useRouter } from "vue-router";
import { handleAuctionAccessError } from "@/service/auctionService";

const props = defineProps({
  title: { type: String, required: true },
  emptyMessage: { type: String, required: true },
  fetchPage: { type: Function, required: true },
  errorMessageFallback: { type: String, default: "無法載入資料，請稍後重試" },
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
  if (loading.value || page < 0) return;
  const version = ++requestVersion;
  loading.value = true;
  errorMessage.value = "";
  items.value = [];
  try {
    const response = await props.fetchPage({ page, size: 10 });
    if (version !== requestVersion) return;
    const data = response.data.data;
    items.value = data.items;
    currentPage.value = data.page;
    totalPages.value = data.totalPages;
    totalElements.value = data.totalElements;
  } catch (error) {
    if (version !== requestVersion) return;
    totalPages.value = 0;
    totalElements.value = 0;
    if (await handleAuctionAccessError(error, router)) return;
    errorMessage.value = error.response?.data?.message ?? props.errorMessageFallback;
  } finally {
    if (version === requestVersion) loading.value = false;
  }
}

onMounted(() => loadPage());
onUnmounted(() => { requestVersion++; });
</script>
