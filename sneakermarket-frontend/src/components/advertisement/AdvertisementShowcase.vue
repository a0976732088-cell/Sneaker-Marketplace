<script setup>
import { ref, onMounted } from "vue";
import AdvertisementDisplay from "@/components/advertisement/AdvertisementDisplay.vue";
import httpClient from "@/service/httpClient";
import { resolveProductImageUrl } from "@/utils/productImageUrl";
import HotAuctionSection from "@/components/advertisement/HotAuctionSection.vue";

// 首頁預設顯示籃球推薦，切換分類後隨機顯示該分類 4 筆商品
const selectedCategoryId = ref(null);
const selectedCategoryName = ref("");
const categoryProducts = ref([]);
const categoryProductsLoading = ref(false);
const categoryProductsError = ref("");

// 平台全部商品中最新的 5 筆
const latestProducts = ref([]);
const latestProductsLoading = ref(false);
const latestProductsError = ref("");

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

    latestProducts.value = sortNewestProducts(response.data, 10);
  } catch (error) {
    console.error("最新商品載入失敗：", error);
    latestProducts.value = [];
    latestProductsError.value = "最新商品暫時無法載入";
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

function formatPrice(price) {
  if (price === null || price === undefined) {
    return "價格未提供";
  }

  return `NT$ ${Number(price).toLocaleString("zh-TW")}`;
}

onMounted(() => {
  fetchLatestProducts();
  fetchCategoryProducts(1, "籃球");
});
</script>

<template>
  <AdvertisementDisplay
    :banner-order="0"
    :small-block-order="0"
    @select-category="handleCategorySelect"
  >
    <template #after-banner>
      <HotAuctionSection />
    </template>

    <template #category-result>
      <!-- 點擊分類廣告後才顯示的分類最新商品 -->
      <section
        v-if="selectedCategoryId"
        class="category-products-section category-products-embedded"
      >
        <header class="category-products-header">
          <div>
            <h2>{{ selectedCategoryName }}推薦</h2>
          </div>

          <RouterLink
            :to="{
              name: 'commerce',
              query: { categoryId: selectedCategoryId },
            }"
            class="product-section-link"
          >
            查看分類
          </RouterLink>
        </header>

        <p
          v-if="categoryProductsLoading && categoryProducts.length === 0"
          class="category-products-message"
        >
          分類商品載入中……
        </p>

        <p
          v-else-if="categoryProductsError"
          class="category-products-message error"
        >
          {{ categoryProductsError }}
        </p>

        <p
          v-else-if="categoryProducts.length === 0"
          class="category-products-message"
        >
          這個分類目前沒有上架中的商品。
        </p>

        <Transition v-else name="category-products" mode="out-in">
          <div
            :key="selectedCategoryId"
            class="category-product-grid four-columns"
          >
            <RouterLink
              v-for="product in categoryProducts"
              :key="product.productId"
              :to="{
                name: 'product-detail',
                params: { id: product.productId },
              }"
              class="category-product-card"
            >
              <div class="category-product-image-wrap">
                <img
                  v-if="product.imageUrl"
                  :src="resolveProductImageUrl(product.imageUrl)"
                  :alt="product.productName"
                  loading="lazy"
                />

                <div v-else class="category-product-image-placeholder">
                  暫無圖片
                </div>
                <small class="category-product-brand">{{
                  product.brandName || "品牌未提供"
                }}</small>
              </div>
              <div class="category-product-info">
                <h3>{{ product.productName }}</h3>

                <div class="category-product-meta">
                  <strong>{{ formatPrice(product.price) }}</strong>
                </div>
              </div>
            </RouterLink>
          </div>
        </Transition>
      </section>
    </template>
  </AdvertisementDisplay>

  <!-- 不受分類點擊影響，固定顯示平台最新 10 筆商品 -->
  <section class="category-products-section latest-products-section">
    <header class="category-products-header">
      <div>
        <h2>最新推出</h2>
      </div>

      <RouterLink :to="{ name: 'commerce' }" class="product-section-link">
        查看全部商品
      </RouterLink>
    </header>

    <p v-if="latestProductsLoading" class="category-products-message">
      最新商品載入中……
    </p>

    <p v-else-if="latestProductsError" class="category-products-message error">
      {{ latestProductsError }}
    </p>

    <p
      v-else-if="latestProducts.length === 0"
      class="category-products-message"
    >
      目前沒有上架中的商品。
    </p>

    <div v-else class="category-product-grid five-columns">
      <RouterLink
        v-for="product in latestProducts"
        :key="product.productId"
        :to="{
          name: 'product-detail',
          params: { id: product.productId },
        }"
        class="category-product-card"
      >
        <div class="category-product-image-wrap">
          <img
            v-if="product.imageUrl"
            :src="resolveProductImageUrl(product.imageUrl)"
            :alt="product.productName"
            loading="lazy"
          />

          <div v-else class="category-product-image-placeholder">暫無圖片</div>
          <small class="category-product-brand">
            {{ product.brandName || "品牌未提供" }}
          </small>
        </div>
        <div class="category-product-info">
          <h3>{{ product.productName }}</h3>

          <div class="category-product-meta">
            <strong>{{ formatPrice(product.price) }}</strong>
          </div>
        </div>
      </RouterLink>
    </div>
  </section>
</template>

<style scoped src="./AdvertisementShowcase.css"></style>
