<script setup>
import { resolveProductImageUrl } from "@/utils/productImageUrl";

defineProps({
  product: {
    type: Object,
    required: true,
  },

  variant: {
    type: String,
    default: "recommended",
    validator: (value) => ["recommended", "latest"].includes(value),
  },

  showNewBadge: {
    type: Boolean,
    default: false,
  },
});

function formatPrice(price) {
  if (price === null || price === undefined) {
    return "價格未提供";
  }

  return `NT$ ${Number(price).toLocaleString("zh-TW")}`;
}
</script>

<template>
  <RouterLink
    :to="{
      name: 'product-detail',
      params: { id: product.productId },
    }"
    class="home-product-card"
    :class="`home-product-card--${variant}`"
  >
    <div class="home-product-image">
      <img
        v-if="product.imageUrl"
        :src="resolveProductImageUrl(product.imageUrl)"
        :alt="product.productName"
        loading="lazy"
      />

      <div v-else class="home-product-placeholder">
        暫無圖片
      </div>

      <span v-if="showNewBadge" class="home-product-badge">
        NEW
      </span>
    </div>

    <div class="home-product-info">
      <h3>{{ product.productName }}</h3>

      <small>
        {{ product.brandName || "品牌未提供" }}
      </small>

      <div class="home-product-meta">
        <strong>{{ formatPrice(product.price) }}</strong>
      </div>
    </div>
  </RouterLink>
</template>

<style scoped>
.home-product-card {
  min-width: 0;
  overflow: hidden;
  border: 1px solid #e7e9ed;
  border-radius: 14px;
  color: inherit;
  background: #fff;
  text-decoration: none;
  box-shadow: 0 6px 20px rgb(28 35 48 / 6%);
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.home-product-card:hover {
  border-color: #cfd5de;
  color: inherit;
  transform: translateY(-4px);
  box-shadow: 0 14px 30px rgb(28 35 48 / 12%);
}

.home-product-card:focus-visible {
  outline: 3px solid rgb(229 72 77 / 28%);
  outline-offset: 3px;
}

.home-product-image {
  position: relative;
  overflow: hidden;
}

.home-product-image img,
.home-product-placeholder {
  display: block;
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #f1f1f1;
  object-fit: cover;
}

.home-product-image img {
  transition: transform 0.3s ease;
}

.home-product-card:hover .home-product-image img {
  transform: scale(1.035);
}

.home-product-placeholder {
  display: grid;
  place-items: center;
  color: #87909d;
  font-size: 13px;
}

.home-product-info {
  padding: 15px;
}

.home-product-info small {
  display: block;
  margin-bottom: 5px;
  color: #8992a0;
  font-size: 12px;
  font-weight: 700;
}

.home-product-info h3 {
  display: -webkit-box;
  min-height: 44px;
  margin: 0;
  overflow: hidden;
  color: #1c2027;
  font-size: 16px;
  line-height: 1.4;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
}

.home-product-meta {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #eee8e2;
}

.home-product-meta strong {
  color: #000;
  font-size: 14px;
  font-weight: 800;
}

/* 分類推薦商品原本的樣式 */
.home-product-card--recommended {
  display: block;
  min-height: 0;
  border-color: #dedede;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 4px 14px rgb(0 0 0 / 5%);
}

.home-product-card--recommended:hover {
  border-color: #111;
  transform: translateY(-3px);
  box-shadow: 0 10px 22px rgb(0 0 0 / 10%);
}

.home-product-card--recommended .home-product-image img,
.home-product-card--recommended .home-product-placeholder {
  width: 100%;
  height: auto;
  min-height: 0;
  aspect-ratio: 4 / 3;
  object-fit: cover;
}

.home-product-card--recommended .home-product-info {
  padding: 14px;
  border-top: 1px solid #e5e5e5;
  background: #fff;
  box-shadow: none;
}

.home-product-card--recommended .home-product-info h3 {
  min-height: 38px;
  color: #111;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.35;
}

/* 最新推出原本的樣式 */
.home-product-card--latest {
  border-color: #e8e3dc;
}

.home-product-card--latest .home-product-info {
  padding: 10px 12px 12px;
  background: #fff;
}

.home-product-card--latest .home-product-info h3 {
  min-height: 0;
  font-size: 14px;
  line-height: 1.35;
  -webkit-line-clamp: 1;
  line-clamp: 1;
}

.home-product-card--latest .home-product-meta {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #e5e5e5;
}

.home-product-badge {
  position: absolute;
  z-index: 1;
  top: 12px;
  left: 12px;
  padding: 5px 8px;
  border-radius: 999px;
  color: #fff;
  background: rgb(25 24 22 / 78%);
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.1em;
  backdrop-filter: blur(5px);
}
</style>