<script setup>
import { ref } from "vue";
import AdvertisementDisplay from "@/views/advertisement/AdvertisementDisplay.vue";
import httpClient from "@/service/httpClient";
import { resolveProductImageUrl } from "@/utils/productImageUrl";

// 分類商品相關變數與 handleCategorySelect()
const selectedCategoryId = ref(null);
const selectedCategoryName = ref("");
const categoryProducts = ref([]);
const categoryProductsLoading = ref(false);
const categoryProductsError = ref("");

async function handleCategorySelect({ categoryId, categoryName }) {
  selectedCategoryId.value = categoryId;
  selectedCategoryName.value = categoryName;
  categoryProductsLoading.value = true;
  categoryProductsError.value = "";
  categoryProducts.value = [];

  try {
    const response = await httpClient.get("/products", {
      params: {
        categoryId,
      },
    });

    categoryProducts.value = response.data;
  } catch (error) {
    console.error("分類商品載入失敗", error);

    categoryProductsError.value = "分類商品載入失敗，請確認後端是否啟動。";
  } finally {
    categoryProductsLoading.value = false;
  }
}
</script>

<template>
  <AdvertisementDisplay @select-category="handleCategorySelect" />

  <!-- 分類商品區塊 -->
  <section v-if="selectedCategoryId" class="category-products-section">
    <header class="category-products-header">
      <div>
        <span>SHOP BY CATEGORY</span>
        <h2>{{ selectedCategoryName }}</h2>
      </div>

      <small> 共 {{ categoryProducts.length }} 件商品 </small>
    </header>

    <p v-if="categoryProductsLoading" class="category-products-message">
      商品載入中……
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

    <div v-else class="category-product-grid">
      <RouterLink
        v-for="product in categoryProducts"
        :key="product.productId"
        :to="{
          name: 'product-detail',
          params: {
            id: product.productId,
          },
        }"
        class="category-product-card"
      >
        <img
          v-if="product.imageUrl"
          :src="resolveProductImageUrl(product.imageUrl)"
          :alt="product.productName"
        />

        <div v-else class="category-product-image-placeholder">暫無圖片</div>

        <div class="category-product-info">
          <small>{{ product.brandName }}</small>
          <h3>{{ product.productName }}</h3>
        </div>
      </RouterLink>
    </div>
  </section>
</template>

<style scoped>
/* 分類商品相關樣式 */
.category-products-section {
  max-width: 1200px;
  margin: 48px auto 0;
}

.category-products-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 20px;
}

.category-products-header span {
  color: #777;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
}

.category-products-header h2 {
  margin: 5px 0 0;
  font-size: 30px;
  font-weight: 800;
}

.category-products-header > small {
  color: #777;
}

.category-product-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.category-product-card {
  overflow: hidden;
  color: inherit;
  background: #fff;
  text-decoration: none;
}

.category-product-card img,
.category-product-image-placeholder {
  display: block;
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #f1f1f1;
  object-fit: cover;
}

.category-product-image-placeholder {
  display: grid;
  place-items: center;
  color: #888;
}

.category-product-info {
  padding: 14px 2px;
}

.category-product-info small {
  color: #777;
}

.category-product-info h3 {
  margin: 5px 0 9px;
  font-size: 17px;
}

.category-products-message {
  padding: 28px;
  color: #666;
  background: #f7f7f7;
  text-align: center;
}

.category-products-message.error {
  color: #b42318;
  background: #fff1f0;
}

@media (max-width: 992px) {
  .category-product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 576px) {
  .category-product-grid {
    grid-template-columns: 1fr;
  }
}
</style>
