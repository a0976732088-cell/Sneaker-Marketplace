<script setup>
import { ref, onBeforeUnmount, onMounted } from "vue";
import AdvertisementDisplay from "@/components/advertisement/AdvertisementDisplay.vue";
import httpClient from "@/service/httpClient";
import { resolveProductImageUrl } from "@/utils/productImageUrl";
import HotAuctionSection from "@/components/advertisement/HotAuctionSection.vue";
import FeaturedProductsSection from "@/components/advertisement/FeaturedProductsSection.vue";
import LatestProductsSection from "@/components/advertisement/LatestProductsSection.vue";
import CategoryProductsSection from "@/components/advertisement/CategoryProductsSection.vue";

// 首頁預設顯示籃球推薦，切換分類後隨機顯示該分類 4 筆商品
const selectedCategoryId = ref(null);
const selectedCategoryName = ref("");
const categoryProducts = ref([]);
const categoryProductsLoading = ref(false);
const categoryProductsError = ref("");

// 平台全部商品中最新的 10 筆
const latestProducts = ref([]);
const latestProductsLoading = ref(false);
const latestProductsError = ref("");

// 保存 API 回傳的完整商品
const allProducts = ref([]);
// 讓 Vue 知道要執行整組切換動畫
const featuredRotationKey = ref(0);
// 切換間隔
const FEATURED_ROTATION_INTERVAL_MS = 6000;
// 保存計時器，方便停止
let featuredRotationTimer = null;
let featuredRotationInProgress = false;

// 圖片預載方法
function preloadProductImages(products) {
  const imageTasks = products.map((product) => {
    if (!product.imageUrl) {
      return Promise.resolve();
    }

    return new Promise((resolve) => {
      const image = new Image();

      image.onload = resolve;
      image.onerror = resolve;
      image.src = resolveProductImageUrl(product.imageUrl);
    });
  });

  return Promise.all(imageTasks);
}

async function rotateFeaturedProducts() {
  if (featuredRotationInProgress || allProducts.value.length <= 3) {
    return;
  }

  featuredRotationInProgress = true;

  try {
    const currentProductIds = new Set(
      featuredProducts.value.map((product) => product.productId),
    );

    const newCandidates = allProducts.value.filter(
      (product) => !currentProductIds.has(product.productId),
    );

    const nextProducts = pickRandomProducts(newCandidates, 3);

    if (nextProducts.length < 3) {
      const remainingCount = 3 - nextProducts.length;

      nextProducts.push(
        ...pickRandomProducts(featuredProducts.value, remainingCount),
      );
    }

    const shuffledNextProducts = pickRandomProducts(nextProducts, 3);

    // 圖片全部預載完成後，才開始切換動畫
    await preloadProductImages(shuffledNextProducts);

    featuredProducts.value = shuffledNextProducts;
    featuredRotationKey.value += 1;
  } finally {
    featuredRotationInProgress = false;
  }
}
function stopFeaturedRotation() {
  if (featuredRotationTimer !== null) {
    window.clearInterval(featuredRotationTimer);
    featuredRotationTimer = null;
  }
}

function startFeaturedRotation() {
  stopFeaturedRotation();

  if (allProducts.value.length <= 3) {
    return;
  }

  featuredRotationTimer = window.setInterval(() => {
    void rotateFeaturedProducts();
  }, FEATURED_ROTATION_INTERVAL_MS);
}

const featuredProducts = ref([]);
const favoriteProductIds = ref(new Set());

function toggleFavoriteProduct(productId) {
  const nextFavoriteIds = new Set(favoriteProductIds.value);

  if (nextFavoriteIds.has(productId)) {
    nextFavoriteIds.delete(productId);
  } else {
    nextFavoriteIds.add(productId);
  }

  favoriteProductIds.value = nextFavoriteIds;
}

function sortNewestProducts(products, limit) {
  return [...products]
    .sort((a, b) => Number(b.productId) - Number(a.productId))
    .slice(0, limit);
}

async function fetchCategoryProducts(categoryId, categoryName) {
  if (
    categoryId === selectedCategoryId.value &&
    categoryProducts.value.length > 0
  ) {
    return;
  }

  const isFirstSelection = selectedCategoryId.value === null;

  if (isFirstSelection) {
    selectedCategoryId.value = categoryId;
    selectedCategoryName.value = categoryName;
  }

  categoryProductsLoading.value = true;
  categoryProductsError.value = "";

  try {
    const response = await httpClient.get("/products", {
      params: { categoryId },
    });

    const nextProducts = pickRandomProducts(response.data, 4);

    selectedCategoryId.value = categoryId;
    selectedCategoryName.value = categoryName;
    categoryProducts.value = nextProducts;
  } catch (error) {
    console.error("分類商品載入失敗：", error);
    categoryProductsError.value = "分類商品暫時無法載入";
  } finally {
    categoryProductsLoading.value = false;
  }
}

async function fetchLatestProducts() {
  latestProductsLoading.value = true;
  latestProductsError.value = "";

  try {
    const response = await httpClient.get("/products");
    const products = Array.isArray(response.data) ? response.data : [];

    allProducts.value = products;

    // 精選鞋款：從全部商品隨機挑選 3 筆
    featuredProducts.value = pickRandomProducts(products, 3);

    // 最新推出：繼續顯示最新的 10 筆
    latestProducts.value = sortNewestProducts(products, 10);
    startFeaturedRotation();
  } catch (error) {
    console.error("首頁商品載入失敗：", error);

    allProducts.value = [];
    featuredProducts.value = [];
    latestProducts.value = [];
    latestProductsError.value = "商品暫時無法載入";
    stopFeaturedRotation();
  } finally {
    latestProductsLoading.value = false;
  }
}

function handleCategorySelect({ categoryId, categoryName }) {
  fetchCategoryProducts(categoryId, categoryName);
}

function pickRandomProducts(products, limit) {
  const shuffledProducts = [...products];

  for (let index = shuffledProducts.length - 1; index > 0; index--) {
    const randomIndex = Math.floor(Math.random() * (index + 1));

    [shuffledProducts[index], shuffledProducts[randomIndex]] = [
      shuffledProducts[randomIndex],
      shuffledProducts[index],
    ];
  }

  return shuffledProducts.slice(0, limit);
}

onMounted(() => {
  fetchLatestProducts();
  fetchCategoryProducts(1, "籃球");
});
onBeforeUnmount(() => {
  stopFeaturedRotation();
});
</script>

<template>
  <AdvertisementDisplay
    :banner-order="0"
    :small-block-order="0"
    :banner-image-clickable="false"
    :show-small-block-action="true"
    small-block-action-label="立即報名"
    @select-category="handleCategorySelect"
  >
    <template #after-banner>
      <section class="home-market-overview">
        <FeaturedProductsSection
          :products="featuredProducts"
          :loading="latestProductsLoading"
          :error-message="latestProductsError"
          :rotation-key="featuredRotationKey"
          :favorite-product-ids="favoriteProductIds"
          @pause-rotation="stopFeaturedRotation"
          @resume-rotation="startFeaturedRotation"
          @toggle-favorite="toggleFavoriteProduct"
        />

        <HotAuctionSection
          :limit="2"
          heading="即將結標"
          :sort="['endAt,asc']"
          compact
        />
      </section>
    </template>

    <template #category-result>
      <!-- 點擊分類廣告後才顯示的分類最新商品 -->
      <CategoryProductsSection
        :category-id="selectedCategoryId"
        :category-name="selectedCategoryName"
        :products="categoryProducts"
        :loading="categoryProductsLoading"
        :error-message="categoryProductsError"
      />
    </template>
  </AdvertisementDisplay>

  <!-- 不受分類點擊影響，固定顯示平台最新 10 筆商品 -->
  <LatestProductsSection
    :products="latestProducts"
    :loading="latestProductsLoading"
    :error-message="latestProductsError"
  />
</template>

<style scoped>
.home-market-overview {
  display: grid;
  grid-template-columns: minmax(0, 1.59fr) minmax(360px, 1fr);
  gap: 28px;
  max-width: 1200px;
  margin: 38px auto 0;
}

@media (max-width: 992px) {
  .home-market-overview {
    grid-template-columns: 1fr;
  }
}
</style>
