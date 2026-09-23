<template>
  <section class="auction-storefront">
    <!-- 競標商城廣告：位置與一般商城一致 -->
    <!-- <AdvertisementDisplay
      :placements="['BANNER', 'SMALL_BLOCK']"
      :banner-order="2"
      :static-banner="true"
      :small-block-order="2"
    /> -->

    <!-- Hero -->
    <AuctionStorefrontHero
      v-model="keyword"
      :loading="loading"
      @search="searchAuctions"
    />
    <!-- 競標商品區 -->
    <AuctionFilterSidebar
      :brands="brands"
      :categories="categories"
      :selected-brand-ids="selectedBrandIds"
      :selected-category-ids="selectedCategoryIds"
      :selected-statuses="selectedStatuses"
      :selected-conditions="selectedConditions"
      :selected-shoe-sizes="selectedShoeSizes"
      v-model:min-price="minPrice"
      v-model:max-price="maxPrice"
      :applied-min-price="appliedMinPrice"
      :applied-max-price="appliedMaxPrice"
      v-model:sort="sort"
      :show-filters="showFilters"
      :loading="loading"
      :total-elements="totalElements"
      :options-error="optionsError"
      @toggle-filters="showFilters = !showFilters"
      @sort-change="refreshAuctionsFromFirstPage"
      @toggle-facet="toggleFacetSelection"
      @clear-facet="clearFacetSelection"
      @apply-price="applyPriceFilters"
      @clear-filters="clearFilters"
    >
        <!-- 右側商品列表 -->
        <main
          :class="
            showFilters
              ? 'col-12 col-md-10 auction-results'
              : 'col-12 auction-results'
          "
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
                <AuctionProductCard
                  :auction="auction"
                  :current-time="currentTime"
                  :image-failed="failedAuctionImageUrls.has(auction.imageUrl)"
                  @image-error="markAuctionImageFailed"
                />
              </RouterLink>
            </div>

            <!-- 分頁 -->
            <AuctionPagination
              :current-page="currentPage"
              :total-pages="totalPages"
              :total-elements="totalElements"
              :loading="loading"
              @change="changePage"
            />
          </template>
        </main>
    </AuctionFilterSidebar>
  </section>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from "vue";

import AuctionStorefrontHero from "@/components/auction/list/AuctionStorefrontHero.vue";
import AuctionFilterSidebar from "@/components/auction/list/AuctionFilterSidebar.vue";
import AuctionProductCard from "@/components/auction/list/AuctionProductCard.vue";
import AuctionPagination from "@/components/auction/list/AuctionPagination.vue";

import AdvertisementDisplay from "@/components/advertisement/AdvertisementDisplay.vue";

import { getAuctions, getAuctionOptions } from "@/service/auctionService";

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
 * 記錄圖片載入失敗 URL。
 */
function markAuctionImageFailed(imageUrl) {
  const failedUrls = new Set(failedAuctionImageUrls.value);

  failedUrls.add(imageUrl);

  failedAuctionImageUrls.value = failedUrls;
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

.auction-results {
  min-width: 0;
}

.catalog-state {
  min-height: 220px;

  display: flex;
  align-items: center;
  justify-content: center;

  gap: 0.65rem;
  color: #495057;
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

.auction-product-link:hover :deep(.product-image) {
  transform: scale(1.06);
}

@media (max-width: 1199.98px) {
  .auction-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767.98px) {
  .auction-grid {
    grid-template-columns: minmax(0, 1fr);

    gap: 2rem;
  }
}
</style>