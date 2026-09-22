<script setup>
import HomeProductCard from "@/components/advertisement/HomeProductCard.vue";

defineProps({
  categoryId: {
    type: [Number, String],
    default: null,
  },

  categoryName: {
    type: String,
    default: "",
  },

  products: {
    type: Array,
    default: () => [],
  },

  loading: {
    type: Boolean,
    default: false,
  },

  errorMessage: {
    type: String,
    default: "",
  },
});
</script>

<template>
  <section
    v-if="categoryId"
    class="category-products-section category-products-embedded"
  >
    <header class="category-products-header">
      <div>
        <h2>{{ categoryName }}推薦</h2>
      </div>

      <RouterLink
        :to="{
          name: 'commerce',
          query: { categoryId },
        }"
        class="product-section-link"
      >
        查看分類
      </RouterLink>
    </header>

    <p
      v-if="loading && products.length === 0"
      class="category-products-message"
    >
      分類商品載入中……
    </p>

    <p
      v-else-if="errorMessage"
      class="category-products-message error"
    >
      {{ errorMessage }}
    </p>

    <p
      v-else-if="products.length === 0"
      class="category-products-message"
    >
      這個分類目前沒有上架中的商品。
    </p>

    <Transition v-else name="category-products" mode="out-in">
      <div
        :key="categoryId"
        class="category-product-grid four-columns"
      >
        <HomeProductCard
          v-for="product in products"
          :key="product.productId"
          :product="product"
          variant="recommended"
        />
      </div>
    </Transition>
  </section>
</template>

<style scoped>
.category-products-section {
  max-width: 1200px;
  margin: 48px auto 0;
}

.category-products-section.category-products-embedded {
  max-width: none;
  margin: 0;
}

.category-products-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 24px;
}

.category-products-embedded .category-products-header {
  align-items: center;
  margin-bottom: 16px;
}

.category-products-header h2 {
  margin: 0;
  color: #16181d;
  font-size: 30px;
  font-weight: 800;
}

.category-products-embedded .category-products-header h2 {
  font-size: 24px;
}

.product-section-link {
  flex-shrink: 0;
  padding: 9px 15px;
  border: 1px solid #d8dde5;
  border-radius: 999px;
  color: #303846;
  background: #fff;
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
  transition:
    color 0.2s ease,
    border-color 0.2s ease,
    background-color 0.2s ease;
}

.product-section-link:hover {
  border-color: #16181d;
  color: #fff;
  background: #16181d;
}

.category-product-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.category-products-embedded .category-product-grid {
  gap: 16px;
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

.category-products-enter-active,
.category-products-leave-active {
  transition:
    opacity 0.24s ease,
    transform 0.24s ease;
}

.category-products-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

.category-products-leave-to {
  opacity: 0;
  transform: translateY(-6px);
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