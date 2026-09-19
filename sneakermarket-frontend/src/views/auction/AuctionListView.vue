<template>
  <section class="auction-storefront">
    <!-- 競標商城廣告：位置與一般商城一致 -->
    <AdvertisementDisplay
      :placements="['BANNER', 'SMALL_BLOCK']"
      :banner-order="2"
      :static-banner="true"
      :small-block-order="2"
    />

    <!-- Hero -->
    <div class="auction-hero">
      <div class="container py-5">
        <span class="hero-icon" aria-hidden="true">👟</span>

        <div class="hero-content">
          <p class="hero-brand text-uppercase small text-white-50 mb-2 fw-semibold">
            SneakerMarket
          </p>
          <h1 class="display-5 fw-bold mb-2">競標商城</h1>
          <p class="hero-copy text-white-50 mb-4">
            熱門球鞋限時競價，出價競逐你的下一雙收藏
          </p>

          <form
            class="d-flex gap-2 hero-search"
            role="search"
            @submit.prevent="searchAuctions"
          >
            <label class="visually-hidden" for="auction-keyword">
              搜尋競標商品
            </label>

            <input
              id="auction-keyword"
              v-model="keyword"
              type="text"
              class="form-control"
              placeholder="搜尋商品名稱或型號"
            />

            <button
              type="submit"
              class="btn btn-light px-4 hero-search-btn"
              :disabled="loading"
            >
              {{ loading ? "搜尋中" : "搜尋" }}
            </button>
          </form>
        </div>
      </div>
    </div>

    <!-- 競標商品區 -->
    <div class="container py-4 auction-catalog">
      <header class="catalog-header">
        <h2 class="h4 fw-bold mb-0">
          競標商品
          <span v-if="!loading" class="catalog-count">
            ({{ totalElements }})
          </span>
        </h2>

        <div class="catalog-tools">
          <button
            type="button"
            class="btn btn-link btn-sm text-dark text-decoration-none filter-toggle-btn filter-visibility-button"
            :aria-expanded="showFilters"
            aria-controls="auction-filters"
            @click="showFilters = !showFilters"
          >
            {{ showFilters ? "« 隱藏篩選條件" : "» 顯示篩選條件" }}
          </button>

          <label class="visually-hidden" for="auction-sort"> 排序方式 </label>

          <select
            id="auction-sort"
            v-model="sort"
            class="form-select form-select-sm w-auto sort-select"
            :disabled="loading"
            @change="refreshAuctionsFromFirstPage"
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

      <div class="row g-4 catalog-layout">
        <!-- 左側篩選 -->
        <aside
          v-if="showFilters"
          id="auction-filters"
          class="col-12 col-md-2"
          aria-label="競標商品篩選"
        >
          <div class="filter-sidebar">
            <form @submit.prevent="applyPriceFilters">
              <!-- 分類 -->
              <div class="filter-block">
                <ul class="category-list">
                  <li>
                    <button
                      type="button"
                      class="category-item"
                      :class="{ active: selectedCategoryIds.length === 0 }"
                      :aria-pressed="selectedCategoryIds.length === 0"
                      @click="clearFacetSelection(selectedCategoryIds)"
                    >
                      全部分類
                    </button>
                  </li>

                  <li v-for="category in categories" :key="category.id">
                    <button
                      type="button"
                      class="category-item"
                      :class="{
                        active: selectedCategoryIds.includes(category.id),
                      }"
                      :aria-pressed="selectedCategoryIds.includes(category.id)"
                      @click="
                        toggleFacetSelection(selectedCategoryIds, category.id)
                      "
                    >
                      {{ category.name }}
                    </button>
                  </li>
                </ul>
              </div>

              <!-- 品牌 -->
              <details class="filter-group">
                <summary class="filter-toggle">
                  <span>
                    品牌
                    <span
                      v-if="selectedBrandIds.length"
                      class="filter-count"
                    >
                      {{ selectedBrandIds.length }}
                    </span>
                  </span>
                  <span class="filter-icon" aria-hidden="true"></span>
                </summary>

                <div class="filter-options">
                  <label
                    v-for="brand in brands"
                    :key="brand.id"
                    class="filter-option"
                  >
                    <input
                      type="checkbox"
                      :checked="selectedBrandIds.includes(brand.id)"
                      @change="
                        toggleFacetSelection(selectedBrandIds, brand.id)
                      "
                    />
                    <span>{{ brand.name }}</span>
                  </label>
                </div>
              </details>

              <!-- 狀態 -->
              <details class="filter-group">
                <summary class="filter-toggle">
                  <span>
                    競標狀態
                    <span
                      v-if="selectedStatuses.length"
                      class="filter-count"
                    >
                      {{ selectedStatuses.length }}
                    </span>
                  </span>
                  <span class="filter-icon" aria-hidden="true"></span>
                </summary>

                <div class="filter-options">
                  <label class="filter-option">
                    <input
                      type="checkbox"
                      :checked="selectedStatuses.includes('ACTIVE')"
                      @change="
                        toggleFacetSelection(selectedStatuses, 'ACTIVE')
                      "
                    />
                    <span>競標中</span>
                  </label>

                  <label class="filter-option">
                    <input
                      type="checkbox"
                      :checked="selectedStatuses.includes('SCHEDULED')"
                      @change="
                        toggleFacetSelection(selectedStatuses, 'SCHEDULED')
                      "
                    />
                    <span>尚未開始</span>
                  </label>

                  <label class="filter-option">
                    <input
                      type="checkbox"
                      :checked="selectedStatuses.includes('SOLD')"
                      @change="toggleFacetSelection(selectedStatuses, 'SOLD')"
                    />
                    <span>已結標</span>
                  </label>

                  <label class="filter-option">
                    <input
                      type="checkbox"
                      :checked="selectedStatuses.includes('UNSOLD')"
                      @change="
                        toggleFacetSelection(selectedStatuses, 'UNSOLD')
                      "
                    />
                    <span>流標</span>
                  </label>

                  <label class="filter-option">
                    <input
                      type="checkbox"
                      :checked="selectedStatuses.includes('CANCELLED')"
                      @change="
                        toggleFacetSelection(selectedStatuses, 'CANCELLED')
                      "
                    />
                    <span>已取消</span>
                  </label>
                </div>
              </details>

              <!-- 商品狀況 -->
              <details class="filter-group">
                <summary class="filter-toggle">
                  <span>
                    商品狀況
                    <span
                      v-if="selectedConditions.length"
                      class="filter-count"
                    >
                      {{ selectedConditions.length }}
                    </span>
                  </span>
                  <span class="filter-icon" aria-hidden="true"></span>
                </summary>

                <div class="filter-options">
                  <label class="filter-option">
                    <input
                      type="checkbox"
                      :checked="selectedConditions.includes('NEW')"
                      @change="
                        toggleFacetSelection(selectedConditions, 'NEW')
                      "
                    />
                    <span>全新</span>
                  </label>

                  <label class="filter-option">
                    <input
                      type="checkbox"
                      :checked="selectedConditions.includes('LIKE_NEW')"
                      @change="
                        toggleFacetSelection(selectedConditions, 'LIKE_NEW')
                      "
                    />
                    <span>近全新</span>
                  </label>

                  <label class="filter-option">
                    <input
                      type="checkbox"
                      :checked="selectedConditions.includes('USED')"
                      @change="
                        toggleFacetSelection(selectedConditions, 'USED')
                      "
                    />
                    <span>二手</span>
                  </label>
                </div>
              </details>

              <!-- 尺寸 -->
              <details class="filter-group">
                <summary class="filter-toggle">
                  <span>
                    尺寸
                    <span v-if="selectedShoeSizes.length" class="filter-count">
                      {{ selectedShoeSizes.length }}
                    </span>
                  </span>
                  <span class="filter-icon" aria-hidden="true"></span>
                </summary>

                <div class="filter-options">
                  <label class="filter-option">
                    <input
                      type="checkbox"
                      :checked="selectedShoeSizes.includes('US 8')"
                      @change="
                        toggleFacetSelection(selectedShoeSizes, 'US 8')
                      "
                    />
                    <span>US 8</span>
                  </label>

                  <label class="filter-option">
                    <input
                      type="checkbox"
                      :checked="selectedShoeSizes.includes('US 8.5')"
                      @change="
                        toggleFacetSelection(selectedShoeSizes, 'US 8.5')
                      "
                    />
                    <span>US 8.5</span>
                  </label>

                  <label class="filter-option">
                    <input
                      type="checkbox"
                      :checked="selectedShoeSizes.includes('US 9')"
                      @change="
                        toggleFacetSelection(selectedShoeSizes, 'US 9')
                      "
                    />
                    <span>US 9</span>
                  </label>

                  <label class="filter-option">
                    <input
                      type="checkbox"
                      :checked="selectedShoeSizes.includes('US 9.5')"
                      @change="
                        toggleFacetSelection(selectedShoeSizes, 'US 9.5')
                      "
                    />
                    <span>US 9.5</span>
                  </label>

                  <label class="filter-option">
                    <input
                      type="checkbox"
                      :checked="selectedShoeSizes.includes('US 10')"
                      @change="
                        toggleFacetSelection(selectedShoeSizes, 'US 10')
                      "
                    />
                    <span>US 10</span>
                  </label>

                  <label class="filter-option">
                    <input
                      type="checkbox"
                      :checked="selectedShoeSizes.includes('US 10.5')"
                      @change="
                        toggleFacetSelection(selectedShoeSizes, 'US 10.5')
                      "
                    />
                    <span>US 10.5</span>
                  </label>

                  <label class="filter-option">
                    <input
                      type="checkbox"
                      :checked="selectedShoeSizes.includes('US 11')"
                      @change="
                        toggleFacetSelection(selectedShoeSizes, 'US 11')
                      "
                    />
                    <span>US 11</span>
                  </label>
                </div>
              </details>

              <!-- 價格 -->
              <details class="filter-group price-filter">
                <summary class="filter-toggle">
                  <span>
                    目前價格
                    <span
                      v-if="appliedMinPrice !== '' || appliedMaxPrice !== ''"
                      class="filter-count"
                    >
                      1
                    </span>
                  </span>
                  <span class="filter-icon" aria-hidden="true"></span>
                </summary>

                <div class="filter-options">
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
                        class="form-control form-control-sm"
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
                        class="form-control form-control-sm"
                        placeholder="最高價"
                      />
                    </div>
                  </div>

                  <button
                    type="submit"
                    class="btn btn-dark btn-sm w-100 apply-filter-button"
                    :disabled="loading"
                  >
                    {{ loading ? "套用中..." : "套用金額篩選" }}
                  </button>
                </div>
              </details>

              <div class="filter-actions">
                <button
                  type="button"
                  class="btn btn-outline-dark btn-sm w-100 clear-filter-button"
                  :disabled="loading"
                  @click="clearFilters"
                >
                  清除全部篩選
                </button>
              </div>
            </form>
          </div>
        </aside>

        <!-- 右側商品列表 -->
        <main
          :class="showFilters ? 'col-12 col-md-10 auction-results' : 'col-12 auction-results'"
          aria-live="polite"
        >
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

const selectedBrandIds = ref([]);
const selectedCategoryIds = ref([]);

const optionsError = ref("");

const showFilters = ref(true);

const failedAuctionImageUrls = ref(new Set());

// 搜尋關鍵字
const keyword = ref("");
const appliedKeyword = ref("");

// 競標狀態篩選
const selectedStatuses = ref([]);

// 商品狀況篩選
const selectedConditions = ref([]);

// 球鞋尺寸篩選
const selectedShoeSizes = ref([]);

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
const appliedMinPrice = ref("");

// 最高價格篩選
const maxPrice = ref("");
const appliedMaxPrice = ref("");

// 前端目前時間，用來更新競標剩餘時間
const currentTime = ref(Date.now());

let countdownTimer = null;
let latestAuctionRequestId = 0;

/**
 * 從後端取得競標列表。
 */
async function loadAuctions() {
  const requestId = ++latestAuctionRequestId;

  loading.value = true;
  errorMessage.value = "";

  try {
    const response = await getAuctions({
      keyword: appliedKeyword.value || undefined,

      brandIds: selectedBrandIds.value.length
        ? [...selectedBrandIds.value]
        : undefined,

      categoryIds: selectedCategoryIds.value.length
        ? [...selectedCategoryIds.value]
        : undefined,

      statuses: selectedStatuses.value.length
        ? [...selectedStatuses.value]
        : undefined,

      conditions: selectedConditions.value.length
        ? [...selectedConditions.value]
        : undefined,

      shoeSizes: selectedShoeSizes.value.length
        ? [...selectedShoeSizes.value]
        : undefined,

      minPrice:
        appliedMinPrice.value !== ""
          ? Number(appliedMinPrice.value)
          : undefined,

      maxPrice:
        appliedMaxPrice.value !== ""
          ? Number(appliedMaxPrice.value)
          : undefined,

      page: currentPage.value,

      size: pageSize.value,

      sort: sort.value,
    });

    if (requestId !== latestAuctionRequestId) {
      return;
    }

    const pageData = response.data.data;

    auctions.value = pageData.items;

    currentPage.value = pageData.page;

    pageSize.value = pageData.size;

    totalElements.value = pageData.totalElements;

    totalPages.value = pageData.totalPages;
  } catch (error) {
    if (requestId !== latestAuctionRequestId) {
      return;
    }

    console.error("取得競標列表失敗：", error);

    errorMessage.value = "無法取得競標列表";
  } finally {
    if (requestId === latestAuctionRequestId) {
      loading.value = false;
    }
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
 * 由第一頁重新查詢目前已套用的條件。
 */
async function refreshAuctionsFromFirstPage() {
  currentPage.value = 0;

  await loadAuctions();
}

/**
 * 套用關鍵字搜尋；尚未送出的價格草稿不會在此被套用。
 */
async function searchAuctions() {
  appliedKeyword.value = keyword.value.trim();

  await refreshAuctionsFromFirstPage();
}

/**
 * 切換可複選的篩選條件，並立即重新查詢。
 */
async function toggleFacetSelection(selectedValues, value) {
  const selectedIndex = selectedValues.indexOf(value);

  if (selectedIndex >= 0) {
    selectedValues.splice(selectedIndex, 1);
  } else {
    selectedValues.push(value);
  }

  await refreshAuctionsFromFirstPage();
}

/**
 * 清空單一複選群組；目前只有「全部分類」會使用。
 */
async function clearFacetSelection(selectedValues) {
  if (selectedValues.length === 0) {
    return;
  }

  selectedValues.splice(0, selectedValues.length);

  await refreshAuctionsFromFirstPage();
}

/**
 * 價格是唯一需要明確按鈕才套用的左側篩選。
 */
async function applyPriceFilters() {
  appliedMinPrice.value = minPrice.value;
  appliedMaxPrice.value = maxPrice.value;

  await refreshAuctionsFromFirstPage();
}

/**
 * 清除競標列表搜尋與篩選條件。
 */
async function clearFilters() {
  keyword.value = "";
  appliedKeyword.value = "";

  selectedBrandIds.value = [];
  selectedCategoryIds.value = [];
  selectedStatuses.value = [];
  selectedConditions.value = [];
  selectedShoeSizes.value = [];

  minPrice.value = "";
  maxPrice.value = "";
  appliedMinPrice.value = "";
  appliedMaxPrice.value = "";

  // 恢復預設排序
  sort.value = "endAt,asc";

  await refreshAuctionsFromFirstPage();
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
  background: linear-gradient(135deg, #0a0a0a 0%, #232323 100%);
  color: #fff;
}

.auction-hero .container {
  position: relative;
}

.hero-content {
  position: relative;
  z-index: 1;
}

.hero-brand {
  letter-spacing: 0.15em;
}

.hero-copy {
  font-size: 1rem;
  line-height: 1.5;
}

.hero-icon {
  position: absolute;
  right: -1rem;
  top: 50%;
  transform: translateY(-50%) rotate(-15deg);
  font-size: 14rem;
  line-height: 1;
  opacity: 0.1;
  pointer-events: none;

  font-family:
    "Apple Color Emoji", "Segoe UI Emoji", "Noto Color Emoji", sans-serif;
}

.hero-search {
  width: 100%;
  max-width: 380px;
}

.hero-search input {
  min-width: 0;
  font-size: 0.95rem;
}

.hero-search-btn {
  flex-shrink: 0;
  white-space: nowrap;
}

.catalog-header {
  position: sticky;
  top: 56px;
  z-index: 20;

  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.5rem;

  padding-top: 0.5rem;
  padding-bottom: 0.5rem;
  background: #fff;
}

.catalog-header h2 {
  font-size: 1.5rem;
}

.catalog-count {
  color: inherit;
}

.catalog-tools {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.filter-visibility-button {
  white-space: nowrap;
}

.filter-toggle-btn {
  font-weight: 500;
}

.filter-visibility-button:hover {
  text-decoration: underline;
}

.sort-select {
  border: 0;
  background-color: transparent;
  box-shadow: none;
  font-weight: 500;
}

.sort-select:focus {
  outline: none;
  box-shadow: none;
}

.catalog-layout {
  align-items: flex-start;
}

.filter-sidebar {
  position: sticky;
  top: 103px;
  font-size: 0.82rem;
}

.filter-block {
  margin-bottom: 0.75rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #dee2e6;
}

.category-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.category-item {
  display: block;
  width: 100%;
  padding: 0.18rem 0;
  border: 0;
  background: none;
  color: #212529;
  cursor: pointer;
  font-size: 1.05rem;
  font-weight: 500;
  line-height: 1.7;
  text-align: left;
}

.category-item.active {
  font-weight: 700;
  text-decoration: underline;
}

.filter-group {
  margin: 0;
  padding: 0 0 0.5rem;
  border-bottom: 1px solid #dee2e6;
  margin-bottom: 0.5rem;
}

.filter-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 0;
  border: 0;
  background: none;
  color: #212529;
  cursor: pointer;
  font-size: 0.85rem;
  font-weight: 600;
  list-style: none;
}

.filter-toggle::-webkit-details-marker {
  display: none;
}

.filter-toggle::marker {
  content: "";
}

.filter-icon {
  color: #6c757d;
}

.filter-icon::before {
  content: "+";
}

.filter-group[open] .filter-icon::before {
  content: "−";
}

.filter-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1rem;
  height: 1rem;
  padding: 0 0.25rem;
  margin-left: 0.35rem;
  border-radius: 50%;
  background: #111;
  color: #fff;
  font-size: 0.6rem;
  font-weight: 600;
}

.filter-options {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  margin-top: 0.4rem;
}

.filter-option {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  cursor: pointer;
  font-size: 0.82rem;
}

.filter-option input[type="checkbox"] {
  flex: 0 0 auto;
  width: 0.85rem;
  height: 0.85rem;
  accent-color: #111;
}

.filter-group .form-control {
  min-height: 31px;
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

.filter-actions {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.apply-filter-button {
  min-height: 31px;
  border: 1px solid #111;
  border-radius: 0.25rem;
  background: #111;
  color: #fff;
  font-size: 0.82rem;
  font-weight: 700;
}

.apply-filter-button:hover:not(:disabled) {
  background: #2c2c2c;
}

.apply-filter-button:disabled {
  border-color: #adb5bd;
  background: #adb5bd;
}

.clear-filter-button {
  border-radius: 0.25rem;
  font-size: 0.82rem;
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

  gap: 1.5rem;
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
  transform: scale(1.06);
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
  padding: 1rem 0.25rem 0;
}

.product-brand {
  margin: 0 0 0.25rem;

  color: #6c757d;

  font-size: 0.875rem;
  font-weight: 400;
}

.product-card-body h3 {
  min-height: 2.7em;

  margin: 0;
  overflow: hidden;

  color: #111;

  font-size: 1rem;
  font-weight: 500;
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
  font-size: 1rem;
  font-weight: 600;
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
  .auction-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 768px) and (max-width: 991.98px) {
  .price-inputs {
    grid-template-columns: minmax(0, 1fr);
  }

  .price-inputs > span {
    display: none;
  }
}

@media (max-width: 767.98px) {
  .filter-sidebar {
    position: static;
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
</style>
