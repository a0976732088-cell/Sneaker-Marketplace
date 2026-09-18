<template>
  <section class="auction-storefront">
    <!-- Hero -->
    <div class="auction-hero">
      <div class="container py-5">
        <span class="hero-icon" aria-hidden="true">👟</span>

        <div class="hero-content">
          <p class="hero-brand">SneakerMarket</p>
          <h1>競標商城</h1>
          <p class="hero-copy">
            熱門球鞋限時競價，<br class="d-sm-none" />出價競逐你的下一雙收藏
          </p>

          <form
            class="hero-search"
            role="search"
            @submit.prevent="searchAuctions"
          >
            <label class="visually-hidden" for="auction-keyword">
              搜尋競標商品
            </label>

            <input
              id="auction-keyword"
              v-model="keyword"
              type="search"
              class="form-control"
              placeholder="搜尋商品名稱或型號"
            />

            <button type="submit" class="btn btn-light" :disabled="loading">
              {{ loading ? "搜尋中" : "搜尋" }}
            </button>
          </form>
        </div>
      </div>
    </div>

    <!-- 競標商城廣告 -->
    <div class="container pt-4">
      <AdvertisementDisplay
        :placements="['BANNER', 'SMALL_BLOCK']"
        :banner-order="2"
        :static-banner="true"
        :small-block-order="2"
      />
    </div>

    <!-- 競標商品區 -->
    <div class="container py-4 auction-catalog">
      <header class="catalog-header">
        <div>
          <p class="catalog-eyebrow">限時競價</p>
          <h2>競標商品</h2>

          <p v-if="!loading" class="catalog-count">
            共 {{ totalElements }} 件商品
          </p>
        </div>

        <div class="catalog-tools">
          <button
            type="button"
            class="filter-visibility-button"
            :aria-expanded="showFilters"
            aria-controls="auction-filters"
            @click="showFilters = !showFilters"
          >
            {{ showFilters ? "隱藏篩選" : "顯示篩選" }}
          </button>

          <label class="visually-hidden" for="auction-sort"> 排序方式 </label>

          <select
            id="auction-sort"
            v-model="sort"
            class="form-select sort-select"
            :disabled="loading"
            @change="searchAuctions"
          >
            <option value="endAt,asc">即將結束</option>
            <option value="createdAt,desc">最新上架</option>
            <option value="currentPrice,asc">價格：低到高</option>
            <option value="currentPrice,desc">價格：高到低</option>
            <option value="bidCount,desc">出價次數：多到少</option>
          </select>
        </div>
      </header>

      <p v-if="optionsError" class="alert alert-warning py-2" role="status">
        {{ optionsError }}
      </p>

      <div class="catalog-layout" :class="{ 'filters-hidden': !showFilters }">
        <!-- 左側篩選 -->
        <aside
          v-if="showFilters"
          id="auction-filters"
          class="filter-sidebar"
          aria-label="競標商品篩選"
        >
          <form @submit.prevent="searchAuctions">
            <div class="filter-heading">
              <h3>篩選條件</h3>

              <button type="button" :disabled="loading" @click="clearFilters">
                清除
              </button>
            </div>

            <!-- 品牌 -->
            <div class="filter-group">
              <label for="auction-brand">品牌</label>

              <select id="auction-brand" v-model="brandId" class="form-select">
                <option value="">全部品牌</option>

                <option
                  v-for="brand in brands"
                  :key="brand.id"
                  :value="brand.id"
                >
                  {{ brand.name }}
                </option>
              </select>
            </div>

            <!-- 分類 -->
            <div class="filter-group">
              <label for="auction-category">分類</label>

              <select
                id="auction-category"
                v-model="categoryId"
                class="form-select"
              >
                <option value="">全部分類</option>

                <option
                  v-for="category in categories"
                  :key="category.id"
                  :value="category.id"
                >
                  {{ category.name }}
                </option>
              </select>
            </div>

            <!-- 狀態 -->
            <div class="filter-group">
              <label for="auction-status">競標狀態</label>

              <select id="auction-status" v-model="status" class="form-select">
                <option value="">全部狀態</option>
                <option value="ACTIVE">競標中</option>
                <option value="SCHEDULED">尚未開始</option>
                <option value="SOLD">已結標</option>
                <option value="UNSOLD">流標</option>
                <option value="CANCELLED">已取消</option>
              </select>
            </div>

            <!-- 商品狀況 -->
            <div class="filter-group">
              <label for="auction-condition">商品狀況</label>

              <select
                id="auction-condition"
                v-model="condition"
                class="form-select"
              >
                <option value="">全部狀況</option>
                <option value="NEW">全新</option>
                <option value="LIKE_NEW">近全新</option>
                <option value="USED">二手</option>
              </select>
            </div>

            <!-- 尺寸 -->
            <div class="filter-group">
              <label for="auction-size">尺寸</label>

              <select id="auction-size" v-model="shoeSize" class="form-select">
                <option value="">全部尺寸</option>
                <option value="US 8">US 8</option>
                <option value="US 8.5">US 8.5</option>
                <option value="US 9">US 9</option>
                <option value="US 9.5">US 9.5</option>
                <option value="US 10">US 10</option>
                <option value="US 10.5">US 10.5</option>
              </select>
            </div>

            <!-- 價格 -->
            <fieldset class="filter-group price-filter">
              <legend>目前價格</legend>

              <div class="price-inputs">
                <div>
                  <label class="visually-hidden" for="auction-min-price">
                    最低價格
                  </label>

                  <input
                    id="auction-min-price"
                    v-model="minPrice"
                    type="number"
                    min="0"
                    step="1"
                    class="form-control"
                    placeholder="最低價"
                  />
                </div>

                <span aria-hidden="true">—</span>

                <div>
                  <label class="visually-hidden" for="auction-max-price">
                    最高價格
                  </label>

                  <input
                    id="auction-max-price"
                    v-model="maxPrice"
                    type="number"
                    min="0"
                    step="1"
                    class="form-control"
                    placeholder="最高價"
                  />
                </div>
              </div>
            </fieldset>

            <button
              type="submit"
              class="apply-filter-button"
              :disabled="loading"
            >
              {{ loading ? "套用中..." : "套用篩選" }}
            </button>
          </form>
        </aside>

        <!-- 右側商品列表 -->
        <main class="auction-results" aria-live="polite">
          <!-- Loading -->
          <div v-if="loading" class="catalog-state" role="status">
            <span
              class="spinner-border spinner-border-sm"
              aria-hidden="true"
            ></span>

            正在載入競標商品
          </div>

          <!-- Error -->
          <div v-else-if="errorMessage" class="alert alert-danger">
            {{ errorMessage }}
          </div>

          <!-- Empty -->
          <div
            v-else-if="auctions.length === 0"
            class="catalog-state empty-state"
          >
            <strong>目前沒有符合條件的競標商品</strong>
            <span>請調整搜尋或篩選條件後再試一次。</span>
          </div>

          <!-- 商品 -->
          <template v-else>
            <div class="auction-grid">
              <RouterLink
                v-for="auction in auctions"
                :key="auction.auctionId"
                :to="{
                  name: 'auction-detail',
                  params: {
                    id: auction.auctionId,
                  },
                }"
                class="auction-product-link"
              >
                <article class="auction-product-card">
                  <!-- 商品圖片 -->
                  <div class="product-image-wrap">
                    <img
                      v-if="
                        auction.imageUrl &&
                        !failedAuctionImageUrls.has(auction.imageUrl)
                      "
                      :src="resolveAuctionImageUrl(auction.imageUrl)"
                      :alt="auction.productName"
                      class="product-image"
                      @error="markAuctionImageFailed(auction.imageUrl)"
                    />

                    <div
                      v-else
                      class="product-image product-image-empty"
                      aria-hidden="true"
                    >
                      <span class="placeholder-mark"> SM </span>

                      <span>尚無圖片</span>
                    </div>

                    <span
                      class="auction-status-badge"
                      :class="statusClass(auction.status)"
                    >
                      {{ formatStatus(auction.status) }}
                    </span>
                  </div>

                  <!-- 商品資訊 -->
                  <div class="product-card-body">
                    <p class="product-brand">
                      {{ auction.brandName ?? "品牌資料未提供" }}
                    </p>

                    <h3>
                      {{ auction.productName }}
                    </h3>

                    <p v-if="auction.model" class="product-model">
                      {{ auction.model }}
                    </p>

                    <p class="product-meta">
                      {{ auction.size }} ·
                      {{ formatCondition(auction.condition) }}
                    </p>

                    <!-- 價格 -->
                    <div class="price-block">
                      <span>目前價格</span>

                      <strong>
                        {{ formatTwd(auction.currentPrice) }}
                      </strong>
                    </div>

                    <!-- 競標資訊 -->
                    <div class="auction-card-stats">
                      <span> {{ auction.bidCount }} 次出價 </span>

                      <span>
                        {{ formatRemainingTime(auction) }}
                      </span>
                    </div>

                    <!-- 其他資訊 -->
                    <div class="auction-card-footer">
                      <span>
                        賣家：{{ auction.sellerName || "會員資料未提供" }}
                      </span>

                      <span>
                        結束
                        {{ formatDateTime(auction.endAt) }}
                      </span>
                    </div>
                  </div>
                </article>
              </RouterLink>
            </div>

            <!-- 分頁 -->
            <nav
              v-if="totalPages > 0"
              class="auction-pagination"
              aria-label="競標商品分頁"
            >
              <p>
                第
                <strong>
                  {{ currentPage + 1 }}
                </strong>
                /
                {{ totalPages }}
                頁，共
                {{ totalElements }}
                筆
              </p>

              <div class="pagination-actions">
                <button
                  type="button"
                  :disabled="loading || currentPage === 0"
                  @click="changePage(currentPage - 1)"
                >
                  上一頁
                </button>

                <span aria-current="page">
                  {{ currentPage + 1 }}
                </span>

                <button
                  type="button"
                  :disabled="loading || currentPage >= totalPages - 1"
                  @click="changePage(currentPage + 1)"
                >
                  下一頁
                </button>
              </div>
            </nav>
          </template>
        </main>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from "vue";

import AdvertisementDisplay from "@/components/advertisement/AdvertisementDisplay.vue";

import { getAuctions, getAuctionOptions } from "@/service/auctionService";

import { resolveAuctionImageUrl } from "@/utils/auctionImageUrl";

import { formatTwd } from "@/utils/auctionMoney";

import { parseAuctionTime, formatAuctionTime } from "@/utils/auctionTime";

// 競標列表資料
const auctions = ref([]);

const brands = ref([]);
const categories = ref([]);

const brandId = ref("");
const categoryId = ref("");

const optionsError = ref("");

const showFilters = ref(true);

const failedAuctionImageUrls = ref(new Set());

// 搜尋關鍵字
const keyword = ref("");

// 競標狀態篩選
const status = ref("");

// 商品狀況篩選
const condition = ref("");

// 球鞋尺寸篩選
const shoeSize = ref("");

// 競標列表排序方式
const sort = ref("endAt,asc");

// 是否正在讀取資料
const loading = ref(false);

// 錯誤訊息
const errorMessage = ref("");

// 目前頁碼
const currentPage = ref(0);

// 每頁資料筆數：配合桌面三欄商品牆，仍維持 Server-side pagination。
const pageSize = ref(9);

// 符合條件的總資料筆數
const totalElements = ref(0);

// 總頁數
const totalPages = ref(0);

// 最低價格篩選
const minPrice = ref("");

// 最高價格篩選
const maxPrice = ref("");

// 前端目前時間，用來更新競標剩餘時間
const currentTime = ref(Date.now());

let countdownTimer = null;

/**
 * 從後端取得競標列表。
 */
async function loadAuctions() {
  loading.value = true;
  errorMessage.value = "";

  try {
    const response = await getAuctions({
      keyword: keyword.value.trim() || undefined,

      brandId: brandId.value || undefined,

      categoryId: categoryId.value || undefined,

      status: status.value || undefined,

      condition: condition.value || undefined,

      shoeSize: shoeSize.value || undefined,

      minPrice: minPrice.value !== "" ? Number(minPrice.value) : undefined,

      maxPrice: maxPrice.value !== "" ? Number(maxPrice.value) : undefined,

      page: currentPage.value,

      size: pageSize.value,

      sort: sort.value,
    });

    const pageData = response.data.data;

    auctions.value = pageData.items;

    currentPage.value = pageData.page;

    pageSize.value = pageData.size;

    totalElements.value = pageData.totalElements;

    totalPages.value = pageData.totalPages;
  } catch (error) {
    console.error("取得競標列表失敗：", error);

    errorMessage.value = "無法取得競標列表";
  } finally {
    loading.value = false;
  }
}

/**
 * 將競標狀態轉成前端顯示文字。
 */
function formatStatus(status) {
  const statusMap = {
    SCHEDULED: "尚未開始",
    ACTIVE: "競標中",
    SOLD: "已結標",
    UNSOLD: "流標",
    CANCELLED: "已取消",
    REMOVED: "廢標",
  };

  return statusMap[status] ?? status;
}

/**
 * 回傳競標狀態 Badge class。
 */
function statusClass(status) {
  const classMap = {
    SCHEDULED: "status-scheduled",

    ACTIVE: "status-active",

    SOLD: "status-ended",

    UNSOLD: "status-ended",

    CANCELLED: "status-ended",

    REMOVED: "status-ended",
  };

  return classMap[status] ?? "status-default";
}

/**
 * 記錄圖片載入失敗 URL。
 */
function markAuctionImageFailed(imageUrl) {
  const failedUrls = new Set(failedAuctionImageUrls.value);

  failedUrls.add(imageUrl);

  failedAuctionImageUrls.value = failedUrls;
}

/**
 * 將商品狀況轉成前端顯示文字。
 */
function formatCondition(condition) {
  const conditionMap = {
    NEW: "全新",
    LIKE_NEW: "近全新",
    USED: "二手",
  };

  return conditionMap[condition] ?? condition;
}

/**
 * 將後端時間轉成前端顯示格式。
 */
const formatDateTime = formatAuctionTime;

/**
 * 計算競標剩餘時間。
 */
function formatRemainingTime(auction) {
  // 已進入終態的競標不再顯示倒數
  if (["SOLD", "UNSOLD", "CANCELLED", "REMOVED"].includes(auction.status)) {
    return "已結束";
  }

  if (!auction.endAt) {
    return "-";
  }

  const endTime = parseAuctionTime(auction.endAt).getTime();

  const diff = endTime - currentTime.value;

  // 前端倒數歸零不代表後端已完成結標
  if (diff <= 0) {
    return "已到截止時間";
  }

  const totalMinutes = Math.floor(diff / 1000 / 60);

  const days = Math.floor(totalMinutes / 1440);

  const hours = Math.floor((totalMinutes % 1440) / 60);

  const minutes = totalMinutes % 60;

  if (days > 0) {
    return `剩餘 ${days} 天 ${hours} 小時`;
  }

  if (hours > 0) {
    return `剩餘 ${hours} 小時 ${minutes} 分`;
  }

  return `剩餘 ${minutes} 分`;
}

/**
 * 執行關鍵字搜尋／篩選。
 */
async function searchAuctions() {
  // 搜尋條件改變時回到第一頁
  currentPage.value = 0;

  await loadAuctions();
}

/**
 * 清除競標列表搜尋與篩選條件。
 */
async function clearFilters() {
  keyword.value = "";

  brandId.value = "";
  categoryId.value = "";

  status.value = "";
  condition.value = "";

  shoeSize.value = "";

  minPrice.value = "";
  maxPrice.value = "";

  // 恢復預設排序
  sort.value = "endAt,asc";

  // 清除條件後回到第一頁
  currentPage.value = 0;

  await loadAuctions();
}

/**
 * 切換競標列表頁面。
 */
async function changePage(page) {
  // 不允許超出有效頁碼
  if (page < 0 || page >= totalPages.value) {
    return;
  }

  currentPage.value = page;

  await loadAuctions();
}

// 頁面載入完成後自動取得競標列表
onMounted(() => {
  loadAuctions();

  getAuctionOptions()
    .then((response) => {
      brands.value = response.data.data.brands;

      categories.value = response.data.data.categories;
    })
    .catch(() => {
      optionsError.value = "品牌與分類選項暫時無法載入，仍可使用其他搜尋條件。";
    });

  // 每分鐘更新一次列表上的剩餘時間
  countdownTimer = setInterval(() => {
    currentTime.value = Date.now();
  }, 60000);
});

// 頁面卸載時清除倒數計時器
onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer);
  }
});
</script>

<style scoped>
.auction-storefront {
  color: #212529;
}

.auction-hero {
  position: relative;
  overflow: hidden;
  background: #111;
  color: #fff;
}

.auction-hero .container {
  position: relative;
  min-height: 330px;
  display: flex;
  align-items: center;
}

.hero-content {
  position: relative;
  z-index: 1;
}

.hero-brand,
.catalog-eyebrow {
  margin: 0 0 0.55rem;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 0.72rem;
  font-weight: 700;
}

.hero-brand {
  color: rgba(255, 255, 255, 0.58);
}

.auction-hero h1 {
  margin: 0;
  font-size: clamp(2.35rem, 5vw, 4.15rem);
  line-height: 1.05;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.hero-copy {
  margin: 1rem 0 1.75rem;
  color: rgba(255, 255, 255, 0.68);
  font-size: clamp(1rem, 2vw, 1.15rem);
  line-height: 1.7;
}

.hero-icon {
  position: absolute;
  right: 1rem;
  top: 50%;
  transform: translateY(-50%) rotate(-14deg);

  font-size: clamp(8rem, 20vw, 15rem);

  line-height: 1;
  opacity: 0.08;
  pointer-events: none;

  font-family:
    "Apple Color Emoji", "Segoe UI Emoji", "Noto Color Emoji", sans-serif;
}

.hero-search {
  display: flex;
  width: min(100%, 470px);
  gap: 0.65rem;
}

.hero-search .form-control,
.hero-search .btn {
  min-height: 48px;
  border-radius: 0.35rem;
}

.hero-search .form-control {
  border-color: #fff;
}

.hero-search .btn {
  min-width: 96px;
  padding-inline: 1.35rem;
  color: #111;
  font-weight: 700;
}

.auction-catalog {
  padding-top: 2rem !important;
}

.catalog-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;

  margin-bottom: 1.5rem;
  padding-bottom: 1rem;

  border-bottom: 1px solid #dee2e6;
}

.catalog-eyebrow {
  color: #6c757d;
}

.catalog-header h2 {
  margin: 0;

  font-size: clamp(1.55rem, 3vw, 2rem);

  font-weight: 800;
  letter-spacing: -0.025em;
}

.catalog-count {
  margin: 0.35rem 0 0;
  color: #6c757d;
  font-size: 0.875rem;
}

.catalog-tools {
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.filter-visibility-button,
.filter-heading button {
  border: 0;
  background: transparent;
  color: #212529;
  font-weight: 600;
}

.filter-visibility-button {
  font-size: 0.875rem;
  white-space: nowrap;
}

.filter-visibility-button:hover,
.filter-heading button:hover {
  text-decoration: underline;
}

.sort-select {
  width: auto;
  min-width: 180px;
  border-color: #ced4da;
  font-size: 0.875rem;
}

.catalog-layout {
  display: grid;
  grid-template-columns:
    220px
    minmax(0, 1fr);

  gap: 2rem;
  align-items: start;
}

.catalog-layout.filters-hidden {
  grid-template-columns: minmax(0, 1fr);
}

.filter-sidebar {
  position: sticky;
  top: 1rem;
}

.filter-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;

  margin-bottom: 0.35rem;
}

.filter-heading h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 800;
}

.filter-heading button {
  padding: 0;
  color: #6c757d;
  font-size: 0.78rem;
}

.filter-group {
  margin: 0;
  padding: 1rem 0;

  border: 0;
  border-bottom: 1px solid #dee2e6;
}

.filter-group > label,
.filter-group legend {
  display: block;
  width: auto;

  margin: 0 0 0.5rem;

  float: none;

  color: #212529;
  font-size: 0.82rem;
  font-weight: 700;
}

.filter-group .form-select,
.filter-group .form-control {
  min-height: 40px;
  border-color: #dee2e6;
  border-radius: 0.25rem;
  font-size: 0.82rem;
}

.price-inputs {
  display: grid;

  grid-template-columns:
    minmax(0, 1fr)
    auto
    minmax(0, 1fr);

  align-items: center;
  gap: 0.35rem;
}

.price-inputs input {
  min-width: 0;
}

.price-inputs span {
  color: #adb5bd;
}

.apply-filter-button {
  width: 100%;
  min-height: 42px;

  margin-top: 1rem;

  border: 1px solid #111;

  border-radius: 2rem;

  background: #111;
  color: #fff;

  font-size: 0.875rem;
  font-weight: 700;
}

.apply-filter-button:hover:not(:disabled) {
  background: #2c2c2c;
}

.apply-filter-button:disabled {
  border-color: #adb5bd;
  background: #adb5bd;
}

.auction-results {
  min-width: 0;
}

.catalog-state {
  min-height: 220px;

  display: flex;
  align-items: center;
  justify-content: center;

  gap: 0.65rem;
  color: #6c757d;
}

.empty-state {
  flex-direction: column;
  padding: 2rem;

  border: 1px solid #e9ecef;

  background: #f8f9fa;
  text-align: center;
}

.empty-state strong {
  color: #212529;
}

.auction-grid {
  display: grid;

  grid-template-columns: repeat(3, minmax(0, 1fr));

  gap: 1.75rem 1.25rem;
}

.auction-product-link {
  min-width: 0;
  color: inherit;
  text-decoration: none;
}

.auction-product-card {
  height: 100%;
  background: #fff;
}

.product-image-wrap {
  position: relative;
  overflow: hidden;
  background: #f1f1f1;
}

.product-image {
  display: block;

  width: 100%;
  aspect-ratio: 1 / 1;

  object-fit: cover;

  transition: transform 0.3s ease;
}

.auction-product-link:hover .product-image {
  transform: scale(1.045);
}

.product-image-empty {
  display: flex;

  flex-direction: column;
  align-items: center;
  justify-content: center;

  gap: 0.4rem;

  color: #8b9197;
  font-size: 0.78rem;
}

.placeholder-mark {
  color: #c4c8cc;

  font-size: 1.5rem;
  font-weight: 800;

  letter-spacing: 0.08em;
}

.auction-status-badge {
  position: absolute;

  top: 0.7rem;
  left: 0.7rem;

  padding: 0.28rem 0.62rem;

  border-radius: 999px;

  font-size: 0.7rem;
  font-weight: 800;

  letter-spacing: 0.03em;
}

.status-active {
  background: #111;
  color: #fff;
}

.status-scheduled {
  background: #fff;
  color: #212529;
}

.status-ended,
.status-default {
  background: #e9ecef;
  color: #495057;
}

.product-card-body {
  padding: 0.9rem 0.15rem 0;
}

.product-brand {
  margin: 0 0 0.25rem;

  color: #6c757d;

  font-size: 0.75rem;
  font-weight: 700;

  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.product-card-body h3 {
  min-height: 2.7em;

  margin: 0;
  overflow: hidden;

  color: #111;

  font-size: 1rem;
  font-weight: 700;
  line-height: 1.35;

  display: -webkit-box;

  -webkit-box-orient: vertical;

  -webkit-line-clamp: 2;

  line-clamp: 2;
}

.product-model,
.product-meta {
  margin: 0.3rem 0 0;

  overflow: hidden;

  color: #6c757d;
  font-size: 0.78rem;

  text-overflow: ellipsis;

  white-space: nowrap;
}

.price-block {
  display: flex;
  flex-direction: column;

  gap: 0.1rem;

  margin-top: 0.9rem;
}

.price-block span {
  color: #6c757d;
  font-size: 0.72rem;
}

.price-block strong {
  color: #111;
  font-size: 1.22rem;
  font-weight: 800;
}

.auction-card-stats {
  display: flex;

  justify-content: space-between;

  gap: 0.5rem;

  margin-top: 0.8rem;
  padding-top: 0.7rem;

  border-top: 1px solid #e9ecef;

  color: #495057;

  font-size: 0.75rem;
  font-weight: 600;
}

.auction-card-footer {
  display: flex;
  flex-direction: column;

  gap: 0.2rem;

  margin-top: 0.65rem;

  color: #8a9096;
  font-size: 0.7rem;
}

.auction-pagination {
  display: flex;

  align-items: center;

  justify-content: space-between;

  gap: 1rem;

  margin-top: 3rem;
  padding-top: 1.25rem;

  border-top: 1px solid #dee2e6;
}

.auction-pagination p {
  margin: 0;

  color: #6c757d;
  font-size: 0.82rem;
}

.pagination-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.pagination-actions button,
.pagination-actions span {
  min-width: 40px;
  min-height: 40px;

  display: inline-flex;

  align-items: center;
  justify-content: center;

  border: 1px solid #dee2e6;

  background: #fff;
  color: #212529;

  font-size: 0.82rem;
}

.pagination-actions button {
  padding-inline: 0.85rem;
}

.pagination-actions button:hover:not(:disabled) {
  border-color: #111;
}

.pagination-actions button:disabled {
  color: #adb5bd;
  cursor: not-allowed;
}

.pagination-actions span {
  border-color: #111;
  background: #111;
  color: #fff;
  font-weight: 700;
}

@media (max-width: 1199.98px) {
  .catalog-layout {
    grid-template-columns:
      200px
      minmax(0, 1fr);

    gap: 1.5rem;
  }

  .auction-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767.98px) {
  .auction-hero .container {
    min-height: 300px;
  }

  .hero-icon {
    right: -2rem;
  }

  .catalog-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .catalog-tools {
    width: 100%;

    justify-content: space-between;
  }

  .sort-select {
    min-width: 0;

    width: min(100%, 205px);
  }

  .catalog-layout {
    grid-template-columns: minmax(0, 1fr);

    gap: 1.5rem;
  }

  .filter-sidebar {
    position: static;

    padding: 1rem;

    border: 1px solid #dee2e6;

    background: #f8f9fa;
  }

  .auction-grid {
    grid-template-columns: minmax(0, 1fr);

    gap: 2rem;
  }

  .auction-pagination {
    align-items: stretch;
    flex-direction: column;
  }

  .pagination-actions {
    justify-content: space-between;
  }

  .pagination-actions button {
    flex: 1;
  }
}

@media (max-width: 479.98px) {
  .hero-search {
    flex-direction: column;
  }

  .hero-search .btn {
    width: 100%;
  }
}
</style>
