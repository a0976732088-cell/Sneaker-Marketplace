<script setup>
import HomeProductCard from "@/components/advertisement/HomeProductCard.vue";

defineProps({
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
  <section class="category-products-section latest-products-section">
    <header class="category-products-header">
      <div>
        <h2>最新推出</h2>
      </div>

      <RouterLink
        :to="{ name: 'commerce' }"
        class="product-section-link"
      >
        查看全部商品
      </RouterLink>
    </header>

    <p v-if="loading" class="category-products-message">
      最新商品載入中……
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
      目前沒有上架中的商品。
    </p>

    <div v-else class="category-product-grid five-columns">
      <HomeProductCard
        v-for="product in products"
        :key="product.productId"
        :product="product"
        variant="latest"
        show-new-badge
      />
    </div>
  </section>
</template>

<style scoped>
.category-products-section {
  max-width: 1200px;
  margin: 48px auto 0;
}

.latest-products-section {
  padding-bottom: 64px;
}

.category-products-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 24px;
}

.category-products-header h2 {
  margin: 0;
  color: #16181d;
  font-size: 30px;
  font-weight: 800;
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
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 20px;
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