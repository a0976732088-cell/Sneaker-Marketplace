<script>
// ===== 頁面快取 =====
// 放在 <script setup> 外面：離開頁面時不會被清掉，返回一般商城時可以先直接顯示上次的資料
let cachedBrands = null;
const cachedProducts = new Map(); // key：篩選條件（JSON 字串），value：商品列表
// 離開頁面時的畫面狀態（篩選、排序、頁數、捲動位置），從商品明細返回時用來還原
let savedState = null;
</script>

<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount, onUnmounted } from "vue";
import httpClient from "@/service/httpClient";
import { getBrands } from "@/service/brandService";
import { useRoute } from "vue-router"; // zwei導向分類
import { resolveProductImageUrl } from "@/utils/productImageUrl";
// 引入廣告顯示元件
import AdvertisementDisplay from "@/components/advertisement/AdvertisementDisplay.vue";
// Banner 圖片放在前端本地，打包時會一起帶走，不依賴網路或後端
import heroBanner from "@/assets/images/hero-banner.jpg";

const route = useRoute(); //zwei導向分類

const products = ref([]);
const loading = ref(true);
const errorMessage = ref("");

const keyword = ref("");
const categoryId = ref(null);
const size = ref(null);
const brandId = ref(null);

const brands = ref([]);
const loadingBrands = ref(true);

// Banner 快速篩選只顯示這 5 個品牌，其餘品牌由左側篩選欄選擇
const HERO_BRAND_NAMES = ["Nike", "Jordan", "adidas", "New Balance", "Vans"];
const heroBrands = computed(() =>
  brands.value.filter((b) => HERO_BRAND_NAMES.includes(b.brandName)),
);

const openBrand = ref(false);
const openSize = ref(false);
const showFilters = ref(true);
const sortBy = ref("");
const currentPage = ref(1);
const pageSize = 9;

// 商品列表區塊，Banner 搜尋或點品牌標籤後捲動到這裡
const listSection = ref(null);

// 導覽列高度：標題列和側欄固定（sticky）時要停在導覽列下方
// 導覽列是共用元件，高度可能因 Logo、字型、螢幕而不同，所以直接量，不寫死
const navHeight = ref(64);

function measureNavHeight() {
  const navBar = document.querySelector('a[href="/cart"]')?.closest("header, nav");
  if (navBar) {
    navHeight.value = Math.round(navBar.getBoundingClientRect().height);
  }
}

const categories = [
  { id: 1, name: "籃球鞋" },
  { id: 2, name: "慢跑鞋" },
  { id: 3, name: "休閒鞋" },
  { id: 4, name: "滑板鞋" },
];

const sizeOptions = [
  "US 7",
  "US 7.5",
  "US 8",
  "US 8.5",
  "US 9",
  "US 9.5",
  "US 10",
  "US 10.5",
  "US 11",
  "US 11.5",
  "US 12",
];

const currentCategoryLabel = computed(() => {
  const found = categories.find((c) => c.id === categoryId.value);
  return found ? found.name : "全部商品";
});

const totalPages = computed(() =>
  Math.max(1, Math.ceil(products.value.length / pageSize)),
);

const sortedProducts = computed(() => {
  const list = [...products.value];
  if (sortBy.value === "price_asc") {
    list.sort((a, b) => a.price - b.price);
  } else if (sortBy.value === "price_desc") {
    list.sort((a, b) => b.price - a.price);
  } else if (sortBy.value === "newest") {
    list.sort((a, b) => b.productId - a.productId);
  }
  return list;
});

const pagedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return sortedProducts.value.slice(start, start + pageSize);
});

// resetPage：一般篩選要回到第 1 頁；從明細返回還原畫面時不要重設
async function fetchProducts({ resetPage = true } = {}) {
  const params = {
    keyword: keyword.value || undefined,
    categoryId: categoryId.value || undefined,
    size: size.value || undefined,
    brandId: brandId.value || undefined,
  };
  const cacheKey = JSON.stringify(params);
  const cached = cachedProducts.get(cacheKey);

  errorMessage.value = "";

  // 同樣的篩選條件之前查過 → 先直接顯示，不出現「載入中」；背景照樣重新抓最新資料
  if (cached) {
    products.value = cached;
    loading.value = false;
  } else {
    loading.value = true;
  }
  if (resetPage) {
    currentPage.value = 1;
  }

  try {
    const response = await httpClient.get("/products", { params });
    products.value = response.data;
    cachedProducts.set(cacheKey, response.data);
    // 背景更新後商品數量可能變少，頁數超過總頁數就退回最後一頁
    if (currentPage.value > totalPages.value) {
      currentPage.value = totalPages.value;
    }
  } catch (error) {
    // 有快取可顯示時就不跳錯誤，避免畫面上有商品卻同時顯示錯誤訊息
    if (!cached) {
      errorMessage.value = "商品資料載入失敗，請確認後端是否啟動";
    }
  } finally {
    loading.value = false;
  }
}

async function loadBrands() {
  // 品牌幾乎不會變動，抓過一次就直接沿用
  if (cachedBrands) {
    brands.value = cachedBrands;
    loadingBrands.value = false;
    return;
  }

  loadingBrands.value = true;
  try {
    const response = await getBrands();
    brands.value = response.data;
    cachedBrands = response.data;
  } finally {
    loadingBrands.value = false;
  }
}

function scrollToList() {
  listSection.value?.scrollIntoView({ behavior: "smooth", block: "start" });
}

function toggleCategory(id) {
  categoryId.value = categoryId.value === id ? null : id;
  fetchProducts();
}

function toggleBrand(id) {
  brandId.value = brandId.value === id ? null : id;
  fetchProducts();
}

function toggleSize(value) {
  size.value = size.value === value ? null : value;
  fetchProducts();
}

function clearFilters() {
  categoryId.value = null;
  brandId.value = null;
  size.value = null;
  fetchProducts();
}

// 點標題「全部商品」：清掉所有篩選和搜尋關鍵字，回到全部商品
function resetAll() {
  keyword.value = "";
  clearFilters();
}

// Banner 搜尋框：搜尋後捲動到商品列表
function searchFromHero() {
  fetchProducts();
  scrollToList();
}

// Banner 品牌標籤：套用品牌篩選、展開側欄的品牌區塊，並捲動到商品列表
function pickBrandFromHero(id) {
  toggleBrand(id);
  openBrand.value = true;
  showFilters.value = true;
  scrollToList();
}

function goToPage(page) {
  if (page < 1 || page > totalPages.value) return;
  currentPage.value = page;
  scrollToList();
}

function formatPrice(price) {
  return price != null ? "NT$ " + Number(price).toLocaleString("zh-TW") : "-";
}

// 判斷是不是從商品明細頁「返回」回來的（瀏覽器上一頁、或明細頁用 router.back() 的返回按鈕）
// Vue Router 會在 history.state 記錄這一頁的下一頁（forward）；是返回回來時，forward 就是剛剛的明細頁
// 只看 forward、不看 back：從明細頁點導覽列「一般商城」是新的一頁（forward 為空），要從第 1 頁開始
function isReturningFromDetail() {
  const forward = window.history.state?.forward;
  return typeof forward === "string" && /^\/products\/\d+/.test(forward);
}

// 已經在商品列表時再點導覽列「一般商城」：網址一樣，Vue Router 不會重新載入這一頁，
// 畫面會停在原本的篩選和頁數。導覽列是共用元件不方便改，所以在這裡攔截點擊，自己回到全部商品第 1 頁
function handleShopLinkClick(event) {
  const link = event.target.closest("a[href]");
  if (!link || route.path !== "/products") return;

  const url = new URL(link.href, window.location.origin);
  if (url.pathname === "/products" && !url.search) {
    sortBy.value = "";
    resetAll();
    window.scrollTo({ top: 0, behavior: "smooth" });
  }
}

// 還原離開前的捲動位置：等商品畫出來後再捲
async function restoreScroll(scrollY, fetching) {
  if (loading.value) {
    await fetching; // 沒有快取時要等 API 回來，商品才會畫出來
  }
  await nextTick();
  requestAnimationFrame(() => window.scrollTo(0, scrollY));
}

onMounted(() => {
  loadBrands();
  measureNavHeight();
  window.addEventListener("resize", measureNavHeight);
  document.addEventListener("click", handleShopLinkClick);

  if (savedState && isReturningFromDetail()) {
    // 從商品明細返回：還原離開前的篩選、排序、頁數和捲動位置
    const s = savedState;
    keyword.value = s.keyword;
    categoryId.value = s.categoryId;
    size.value = s.size;
    brandId.value = s.brandId;
    sortBy.value = s.sortBy;
    showFilters.value = s.showFilters;
    openBrand.value = s.openBrand;
    openSize.value = s.openSize;
    currentPage.value = s.currentPage;

    const fetching = fetchProducts({ resetPage: false });
    restoreScroll(s.scrollY, fetching);
  } else {
    // 一般進入（導覽列、首頁分類連結等）：照原本的方式，從第 1 頁開始
    // zwei導向分類，取得id後再執行fetchProducts
    categoryId.value = Number(route.query.categoryId) || null;
    fetchProducts();
  }
});

// 離開頁面前記住目前的畫面狀態，返回時才能還原
onBeforeUnmount(() => {
  savedState = {
    keyword: keyword.value,
    categoryId: categoryId.value,
    size: size.value,
    brandId: brandId.value,
    sortBy: sortBy.value,
    showFilters: showFilters.value,
    openBrand: openBrand.value,
    openSize: openSize.value,
    currentPage: currentPage.value,
    scrollY: window.scrollY,
  };
});

onUnmounted(() => {
  window.removeEventListener("resize", measureNavHeight);
  document.removeEventListener("click", handleShopLinkClick);
});
</script>

<template>
  <section class="kicks-shop" :style="{ '--nav-h': navHeight + 'px' }">
    <!-- 一般商城顯示專用的靜態、 BANNER 與 SMALL_BLOCK -->
    <!-- 移除了:banner-order="1" :static-banner="true" -->
    <AdvertisementDisplay
      :placements="['SMALL_BLOCK']"
      :small-block-order="1"
    />

    <!-- ========== Banner ========== -->
    <div class="container pt-4">
      <div class="hero">
        <img
          :src="heroBanner"
          alt="KICKS - Find your next pair"
          class="hero-img"
        />

        <div class="hero-actions">
          <form class="hero-search" @submit.prevent="searchFromHero">
            <div class="hero-search-field">
              <svg class="icon" viewBox="0 0 24 24" aria-hidden="true">
                <circle cx="11" cy="11" r="7" />
                <path d="m20 20-3.5-3.5" />
              </svg>
              <input
                v-model="keyword"
                type="text"
                placeholder="搜尋商品名稱..."
              />
            </div>
            <button type="submit" class="hero-search-btn">搜尋</button>
          </form>

          <div v-if="!loadingBrands" class="hero-brands">
            <button
              v-for="brand in heroBrands"
              :key="brand.brandId"
              type="button"
              class="hero-brand-chip"
              :class="{ active: brandId === brand.brandId }"
              @click="pickBrandFromHero(brand.brandId)"
            >
              {{ brand.brandName }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== 商品列表 ========== -->
    <div ref="listSection" class="container py-4 list-section">
      <div
        class="d-flex justify-content-between align-items-center flex-wrap gap-2 list-header"
      >
        <h2 class="h4 fw-bold mb-0">
          <button
            type="button"
            class="title-reset-btn"
            title="查看全部商品"
            @click="resetAll"
          >
            {{ currentCategoryLabel }}</button
          ><span v-if="categoryId && !loading"> ({{ products.length }})</span>
        </h2>
        <div class="d-flex align-items-center gap-3">
          <button
            type="button"
            class="btn btn-link btn-sm text-dark text-decoration-none filter-toggle-btn"
            @click="showFilters = !showFilters"
          >
            {{ showFilters ? "« 隱藏篩選條件" : "» 顯示篩選條件" }}
          </button>
          <select
            v-model="sortBy"
            class="form-select form-select-sm w-auto sort-select"
            @change="currentPage = 1"
          >
            <option value="">排序依據</option>
            <option value="newest">最新</option>
            <option value="price_desc">價格：由高到低</option>
            <option value="price_asc">價格：由低到高</option>
          </select>
        </div>
      </div>

      <div class="row g-4">
        <!-- ========== 左側篩選欄 ========== -->
        <aside v-if="showFilters" class="col-12 col-md-2">
          <div class="filter-sidebar">
            <div class="filter-block">
              <ul class="category-list">
                <li v-for="cat in categories" :key="cat.id">
                  <button
                    type="button"
                    class="category-item"
                    :class="{ active: categoryId === cat.id }"
                    @click="toggleCategory(cat.id)"
                  >
                    {{ cat.name }}
                  </button>
                </li>
              </ul>
            </div>

            <div class="filter-group">
              <button
                type="button"
                class="filter-toggle"
                @click="openBrand = !openBrand"
              >
                <span>
                  品牌
                  <span v-if="brandId" class="filter-count">1</span>
                </span>
                <span class="filter-icon">{{ openBrand ? "−" : "+" }}</span>
              </button>
              <div v-show="openBrand" class="filter-options">
                <div v-if="loadingBrands" class="text-secondary small">
                  載入中...
                </div>
                <label
                  v-for="brand in brands"
                  v-else
                  :key="brand.brandId"
                  class="filter-option"
                >
                  <input
                    type="checkbox"
                    :checked="brandId === brand.brandId"
                    @change="toggleBrand(brand.brandId)"
                  />
                  {{ brand.brandName }}
                </label>
              </div>
            </div>

            <div class="filter-group">
              <button
                type="button"
                class="filter-toggle"
                @click="openSize = !openSize"
              >
                <span>
                  尺寸
                  <span v-if="size" class="filter-count">1</span>
                </span>
                <span class="filter-icon">{{ openSize ? "−" : "+" }}</span>
              </button>
              <div v-show="openSize" class="filter-options">
                <label v-for="s in sizeOptions" :key="s" class="filter-option">
                  <input
                    type="checkbox"
                    :checked="size === s"
                    @change="toggleSize(s)"
                  />
                  {{ s }}
                </label>
              </div>
            </div>

            <button
              type="button"
              class="clear-btn"
              @click="clearFilters"
            >
              清除篩選
            </button>
          </div>
        </aside>

        <!-- ========== 商品卡片（維持原樣） ========== -->
        <div :class="showFilters ? 'col-12 col-md-10' : 'col-12'">
          <div v-if="loading" class="text-secondary">載入中...</div>

          <div v-else-if="errorMessage" class="alert alert-danger">
            {{ errorMessage }}
          </div>

          <div v-else-if="products.length === 0" class="alert alert-warning">
            目前沒有符合條件的商品
          </div>

          <template v-else>
            <div class="row row-cols-1 row-cols-sm-2 row-cols-lg-3 g-4">
              <div v-for="p in pagedProducts" :key="p.productId" class="col">
                <router-link
                  :to="`/products/${p.productId}`"
                  class="text-decoration-none text-dark"
                >
                  <div class="card h-100 product-card border-0">
                    <div class="product-card-img-wrap">
                      <img
                        v-if="p.imageUrl"
                        :src="resolveProductImageUrl(p.imageUrl)"
                        :alt="p.productName"
                        class="product-card-img"
                        loading="lazy"
                        decoding="async"
                      />
                      <div
                        v-else
                        class="product-card-img product-card-img-empty"
                      >
                        尚無圖片
                      </div>
                      <span v-if="p.totalStock === 0" class="stock-badge"
                        >缺貨</span
                      >
                    </div>
                    <div class="card-body px-1">
                      <p class="text-secondary small mb-1">{{ p.brandName }}</p>
                      <h6 class="mb-1 product-name">{{ p.productName }}</h6>
                      <p class="fw-semibold mb-0">{{ formatPrice(p.price) }}</p>
                    </div>
                  </div>
                </router-link>
              </div>
            </div>

            <!-- ========== 分頁 ========== -->
            <nav v-if="totalPages > 1" class="kicks-pagination" aria-label="商品分頁">
              <button
                type="button"
                class="page-btn page-btn-text"
                :disabled="currentPage === 1"
                @click="goToPage(currentPage - 1)"
              >
                <svg class="icon" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="m15 18-6-6 6-6" />
                </svg>
                上一頁
              </button>
              <button
                v-for="page in totalPages"
                :key="page"
                type="button"
                class="page-btn"
                :class="{ active: page === currentPage }"
                @click="goToPage(page)"
              >
                {{ page }}
              </button>
              <button
                type="button"
                class="page-btn page-btn-text"
                :disabled="currentPage === totalPages"
                @click="goToPage(currentPage + 1)"
              >
                下一頁
                <svg class="icon" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="m9 18 6-6-6-6" />
                </svg>
              </button>
            </nav>
          </template>
        </div>
      </div>

      <p class="text-secondary mt-5 mb-0">主要負責：商城系統負責人</p>
    </div>
  </section>
</template>

<!-- 樣式獨立放在 assets/css，只作用在這一頁（scoped），不會影響其他組員的頁面 -->
<style scoped src="../../assets/css/commerceHome.css"></style>