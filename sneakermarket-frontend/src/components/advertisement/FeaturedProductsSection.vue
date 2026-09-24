<script setup>
import { resolveProductImageUrl } from "@/utils/productImageUrl";

const props = defineProps({
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

  rotationKey: {
    type: Number,
    default: 0,
  },

  favoriteProductIds: {
    type: Set,
    default: () => new Set(),
  },
});

const emit = defineEmits([
  "pause-rotation",
  "resume-rotation",
  "toggle-favorite",
]);

function isFavoriteProduct(productId) {
  return props.favoriteProductIds.has(productId);
}

function formatPrice(price) {
  if (price === null || price === undefined) {
    return "價格未提供";
  }

  return `NT$ ${Number(price).toLocaleString("zh-TW")}`;
}
</script>

<template>
  <div class="home-featured-column">
    <header class="home-overview-header">
      <h2>精選鞋款</h2>

      <RouterLink
        :to="{ name: 'commerce' }"
        class="home-overview-link"
      >
        查看全部
      </RouterLink>
    </header>

    <p v-if="loading" class="home-overview-message">
      精選鞋款載入中……
    </p>

    <p
      v-else-if="errorMessage"
      class="home-overview-message error"
    >
      {{ errorMessage }}
    </p>

    <p
      v-else-if="products.length === 0"
      class="home-overview-message"
    >
      目前沒有上架中的商品。
    </p>

    <Transition v-else name="featured-swap" mode="out-in">
      <div
        :key="rotationKey"
        class="featured-product-grid"
        @mouseenter="emit('pause-rotation')"
        @mouseleave="emit('resume-rotation')"
        @focusin="emit('pause-rotation')"
        @focusout="emit('resume-rotation')"
      >
        <article
          v-for="product in products"
          :key="product.productId"
          class="featured-product-card"
        >
          <RouterLink
            :to="{
              name: 'product-detail',
              params: { id: product.productId },
            }"
            class="featured-product-link"
          >
            <div class="featured-product-image">
              <img
                v-if="product.imageUrl"
                :src="resolveProductImageUrl(product.imageUrl)"
                :alt="product.productName"
                loading="eager"
              />

              <div v-else class="featured-image-placeholder">
                暫無圖片
              </div>
            </div>

            <div class="featured-product-content">
              <h3>{{ product.productName }}</h3>

              <small>
                {{ product.brandName || "品牌未提供" }}
              </small>

              <strong>{{ formatPrice(product.price) }}</strong>
            </div>
          </RouterLink>

          <!-- <button
            type="button"
            class="favorite-button"
            :class="{ active: isFavoriteProduct(product.productId) }"
            :aria-label="
              isFavoriteProduct(product.productId)
                ? `取消收藏 ${product.productName}`
                : `收藏 ${product.productName}`
            "
            :aria-pressed="isFavoriteProduct(product.productId)"
            @click="emit('toggle-favorite', product.productId)"
          >
            <span aria-hidden="true">
              {{ isFavoriteProduct(product.productId) ? "♥" : "♡" }}
            </span>
          </button> -->
        </article>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.home-featured-column {
  min-width: 0;
  padding-right: 28px;
  border-right: 1px solid #e6e6e6;
}

.home-overview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
}

.home-overview-header h2 {
  margin: 0;
  color: #151515;
  font-size: 27px;
  font-weight: 900;
}

.home-overview-link {
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

.home-overview-link:hover {
  border-color: #16181d;
  color: #fff;
  background: #16181d;
}

.featured-product-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.featured-product-card {
  position: relative;
  min-width: 0;
  overflow: hidden;
  border: 1px solid #e2e4e7;
  border-radius: 10px;
  color: inherit;
  background: #fff;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.featured-product-card:hover {
  color: inherit;
  transform: translateY(-3px);
  box-shadow: 0 10px 24px rgb(0 0 0 / 9%);
}

.featured-product-link {
  display: block;
  color: inherit;
  text-decoration: none;
}

.featured-product-link:hover {
  color: inherit;
}

.featured-product-link:focus-visible {
  outline: 3px solid rgb(229 72 77 / 28%);
  outline-offset: -3px;
}

.featured-product-image {
  position: relative;
  overflow: hidden;
  aspect-ratio: 4 / 3;
  background: #f5f5f5;
}

.featured-product-image img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.25s ease;
}

.featured-product-card:hover .featured-product-image img {
  transform: scale(1.035);
}

.featured-image-placeholder {
  display: grid;
  width: 100%;
  height: 100%;
  place-items: center;
  color: #8a8f96;
  font-size: 13px;
}

.favorite-button {
  position: absolute;
  z-index: 3;
  top: 9px;
  right: 9px;
  display: grid;
  width: 30px;
  height: 30px;
  padding: 0;
  place-items: center;
  border: 1px solid #dfe2e6;
  border-radius: 50%;
  color: #555;
  background: rgb(255 255 255 / 90%);
  font-size: 18px;
  cursor: pointer;
  transition:
    color 0.18s ease,
    background-color 0.18s ease,
    border-color 0.18s ease,
    transform 0.18s ease;
}

.favorite-button:hover {
  color: #e5484d;
  background: #fff;
  transform: scale(1.08);
}

.favorite-button.active {
  border-color: rgb(229 72 77 / 25%);
  color: #e5484d;
  background: #fff2f2;
}

.favorite-button:focus-visible {
  outline: 3px solid rgb(229 72 77 / 35%);
  outline-offset: 2px;
}

.featured-product-content {
  height: 104px;
  min-height: 104px;
  padding: 10px 12px;
}

.featured-product-content h3 {
  overflow: hidden;
  margin: 0;
  color: #202124;
  font-size: 18px;
  font-weight: 750;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.featured-product-content small {
  display: block;
  margin-top: 3px;
  color: #8a8f96;
  font-size: 14px;
}

.featured-product-content strong {
  display: block;
  margin-top: 9px;
  color: #171717;
  font-size: 15px;
}

.home-overview-message {
  display: grid;
  min-height: 220px;
  margin: 0;
  place-items: center;
  border-radius: 10px;
  color: #69717d;
  background: #f6f7f8;
}

.home-overview-message.error {
  color: #b42318;
  background: #fff1f0;
}

.featured-swap-enter-active {
  transition:
    opacity 0.38s ease,
    transform 0.38s cubic-bezier(0.22, 1, 0.36, 1);
}

.featured-swap-leave-active {
  transition:
    opacity 0.24s ease,
    transform 0.24s cubic-bezier(0.4, 0, 1, 1);
}

.featured-swap-enter-from {
  opacity: 0;
  transform: translateY(18px) scale(0.96);
}

.featured-swap-leave-to {
  opacity: 0;
  transform: translateY(-14px) scale(0.96);
}

@media (max-width: 992px) {
  .home-featured-column {
    padding-right: 0;
    padding-bottom: 28px;
    border-right: 0;
    border-bottom: 1px solid #e6e6e6;
  }
}

@media (prefers-reduced-motion: reduce) {
  .featured-swap-enter-active,
  .featured-swap-leave-active {
    transition: none;
  }
}
</style>