<script setup>
import { getCurrentMember } from '@/service/authService'
import { useRoute } from 'vue-router'
import { ref, onMounted, computed } from 'vue'
import httpClient from '@/service/httpClient'
import { useRouter } from 'vue-router'


const route = useRoute()
const product = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const selectedVariantId = ref(null)
const router = useRouter()
const shippingInfoOpen = ref(false)
const showCartPopup = ref(false)
const addedItem = ref(null)

onMounted(async () => {
  try {
    const response = await httpClient.get(`/products/${route.params.id}`)
    product.value = response.data
  } catch (error) {
    errorMessage.value = '商品資料載入失敗，請確認後端是否啟動'
  } finally {
    loading.value = false
  }
})

const selectedVariant = computed(() => {
  for (const v of product.value.variants) {
    if (v.variantId === selectedVariantId.value) {
      return v
    }
  }
  return null
})

const displayPrice = computed(() => {
  if (selectedVariant.value) return selectedVariant.value.price
  if (product.value && product.value.variants.length > 0) return product.value.variants[0].price
  return null
})

const lowStockWarning = computed(() => {
  return !!(selectedVariant.value && selectedVariant.value.stock > 0 && selectedVariant.value.stock < 10)
})

function formatPrice(price) {
  return price != null ? 'NT$ ' + Number(price).toLocaleString('zh-TW') : '-'
}

async function addToCart() {
  try {
    await getCurrentMember()
  } catch (error) {
    router.push('/login')
    return
  }

  try {
    await httpClient.post('/cart/items', {
      variantId: selectedVariant.value.variantId,
      quantity: 1,
    })

    addedItem.value = {
      image: product.value.images && product.value.images.length > 0 ? product.value.images[0] : null,
      productName: product.value.productName,
      categoryName: product.value.categoryName,
      size: selectedVariant.value.size,
      price: selectedVariant.value.price,
    }
    showCartPopup.value = true
  } catch (error) {
    alert('加入購物車失敗')
  }
}

function closeCartPopup() {
  showCartPopup.value = false
}

function goToCart() {
  showCartPopup.value = false
  router.push('/cart')
}

function goToCheckout() {
  showCartPopup.value = false
  router.push('/checkout')
}

</script>
<template>
  <section class="detail-container py-5">
    <div v-if="loading" class="text-secondary mt-3">載入中...</div>

    <div v-else-if="errorMessage" class="alert alert-danger mt-3">
      {{ errorMessage }}
    </div>

    <div v-else class="row g-5 align-items-start">
      <div class="col-12 col-md-5">
        <img
          v-if="product.images && product.images.length > 0"
          :src="product.images[0]"
          :alt="product.productName"
          class="detail-img"
        >
        <div v-else class="detail-img detail-img-empty">尚無圖片</div>
      </div>

      <div class="col-12 col-md-7">
        <span class="status-badge mb-2" :class="product.status === 'ON_SALE' ? 'status-on' : 'status-off'">
          {{ product.status === 'ON_SALE' ? '現貨' : product.status }}
        </span>

        <h1 class="product-title">{{ product.productName }}</h1>
        <p class="text-secondary mb-3">{{ product.brandName }}</p>
        <p class="text-secondary mb-3">{{ product.categoryName }}</p>

        <p class="fs-4 fw-semibold mb-3">{{ formatPrice(displayPrice) }}</p>
        <p v-if="lowStockWarning" class="low-stock-text mb-3">⏱ 所剩庫存不多，欲購從速。</p>

        <hr class="my-4">

        <h2 class="h6 fw-bold mb-2">選取尺寸</h2>
        <div class="size-grid mb-3">
          <button
            v-for="v in product.variants"
            :key="v.variantId"
            type="button"
            class="size-btn"
            :class="{ active: selectedVariantId === v.variantId }"
            :disabled="v.stock === 0"
            @click="selectedVariantId = v.variantId"
          >
            {{ v.size }}
          </button>
        </div>

        <p v-if="!selectedVariant" class="text-secondary small mb-3">請先選擇尺寸</p>

        <button
          class="btn-add-to-cart"
          :disabled="!selectedVariant || selectedVariant.stock === 0"
          @click="addToCart"
        >
          加入購物車
        </button>

        <div class="product-info-section">
          <p class="text-secondary mb-2">顏色：{{ product.variants[0]?.color }}</p>
          <p v-if="product.description" class="text-secondary mb-0">{{ product.description }}</p>
          <p v-else class="text-secondary fst-italic mb-0">尚無商品介紹</p>

          <div class="shipping-toggle">
            <button type="button" class="shipping-toggle-header" @click="shippingInfoOpen = !shippingInfoOpen">
              <span>寄送及退貨</span>
              <span class="chevron" :class="{ open: shippingInfoOpen }">⌄</span>
            </button>
            <div v-if="shippingInfoOpen" class="shipping-toggle-body">
              <p class="mb-2">台北市：</p>
              <ul class="mb-3">
                <li>標準運送商品在 2-5 個工作天內送達</li>
              </ul>
              <p class="mb-2">其他國家/地區：</p>
              <ul class="mb-3">
                <li>標準運送商品在 3-6 個工作天內送達</li>
              </ul>
              <p class="mb-0">訂單處理和出貨時間為星期一至星期五 (國定假日除外)</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>

  <div v-if="showCartPopup" class="cart-popup">
    <div class="cart-popup-header">
      <span class="cart-popup-check">✓</span>
      <span class="cart-popup-title">已加入購物車</span>
      <button type="button" class="cart-popup-close" @click="closeCartPopup">✕</button>
    </div>

    <div class="cart-popup-body" v-if="addedItem">
      <img v-if="addedItem.image" :src="addedItem.image" :alt="addedItem.productName" class="cart-popup-img">
      <div v-else class="cart-popup-img cart-popup-img-empty"></div>

      <div class="cart-popup-info">
        <p class="cart-popup-name">{{ addedItem.productName }}</p>
        <p class="cart-popup-meta">{{ addedItem.categoryName }}</p>
        <p class="cart-popup-meta">尺寸 {{ addedItem.size }}</p>
        <p class="cart-popup-price">{{ formatPrice(addedItem.price) }}</p>
      </div>
    </div>

    <div class="cart-popup-actions">
      <button type="button" class="btn-view-cart" @click="goToCart">查看購物車</button>
      <button type="button" class="btn-checkout" @click="goToCheckout">結帳</button>
    </div>
  </div>
</template>

<style scoped>
.detail-container {
  max-width: 960px;
  margin: 0 auto;
  padding-left: 1rem;
  padding-right: 1rem;
}

.detail-img {
  width: 100%;
  max-width: 400px;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  background-color: #f1f1f1;
  border-radius: 0.5rem;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.detail-img-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  width: 100%;
  max-width: 400px;
  aspect-ratio: 1 / 1;
  background-color: #f1f1f1;
  border-radius: 0.5rem;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.product-title {
  font-size: 1.6rem;
  font-weight: 700;
  margin-bottom: 0.5rem;
}

.status-badge {
  display: inline-block;
  padding: 0.15rem 0.6rem;
  border-radius: 1rem;
  font-size: 0.75rem;
  font-weight: 600;
}

.status-on {
  background-color: #e6f4ea;
  color: #1e7e34;
}

.status-off {
  background-color: #f1f1f1;
  color: #6c757d;
}

.size-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.5rem;
}

.size-btn {
  padding: 0.4rem 0;
  border: 1px solid #ced4da;
  background-color: #fff;
  border-radius: 0.375rem;
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.15s ease;
}

.size-btn:hover:not(:disabled) {
  border-color: #111111;
}

.size-btn.active {
  background-color: #111111;
  color: #fff;
  border-color: #111111;
  font-weight: 600;
}

.size-btn:disabled {
  color: #adb5bd;
  background-color: #f8f9fa;
  text-decoration: line-through;
  cursor: not-allowed;
}

.btn-add-to-cart {
  width: auto;
  min-width: 220px;
  padding: 0.6rem 2rem;
  background-color: #111111;
  color: #fff;
  border: none;
  border-radius: 2rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-add-to-cart:disabled {
  background-color: #adb5bd;
  cursor: not-allowed;
}

.low-stock-text {
  color: #d9534f;
  font-weight: 600;
  font-size: 0.9rem;
}

.product-info-section {
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e9ecef;
}

.shipping-toggle {
  margin-top: 1.5rem;
  border-top: 1px solid #e9ecef;
}

.shipping-toggle-header {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: none;
  border: none;
  padding: 1rem 0;
  font-weight: 600;
  font-size: 0.95rem;
  cursor: pointer;
  color: #111111;
}

.chevron {
  font-size: 1.2rem;
  transition: transform 0.2s ease;
}

.chevron.open {
  transform: rotate(180deg);
}

.shipping-toggle-body {
  padding-bottom: 1rem;
  font-size: 0.9rem;
  color: #6c757d;
}

.shipping-toggle-body ul {
  padding-left: 1.2rem;
}

.cart-popup {
  position: fixed;
  top: 80px;
  right: 24px;
  width: 360px;
  max-width: calc(100vw - 32px);
  background-color: #fff;
  border-radius: 0.75rem;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
  padding: 1.25rem;
  z-index: 1000;
}

.cart-popup-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.cart-popup-check {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: #1e7e34;
  color: #fff;
  font-size: 0.8rem;
  flex-shrink: 0;
}

.cart-popup-title {
  font-weight: 600;
  flex: 1;
}

.cart-popup-close {
  background: none;
  border: none;
  font-size: 1rem;
  color: #6c757d;
  cursor: pointer;
}

.cart-popup-body {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.25rem;
}

.cart-popup-img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 0.5rem;
  background-color: #f1f1f1;
  flex-shrink: 0;
}

.cart-popup-info {
  flex: 1;
}

.cart-popup-name {
  font-weight: 600;
  margin-bottom: 0.25rem;
}

.cart-popup-meta {
  color: #6c757d;
  font-size: 0.85rem;
  margin-bottom: 0.15rem;
}

.cart-popup-price {
  font-weight: 600;
  margin-top: 0.25rem;
}

.cart-popup-actions {
  display: flex;
  gap: 0.75rem;
}

.btn-view-cart {
  flex: 1;
  padding: 0.6rem 0;
  border: 1px solid #111111;
  background-color: #fff;
  color: #111111;
  border-radius: 2rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-checkout {
  flex: 1;
  padding: 0.6rem 0;
  border: none;
  background-color: #111111;
  color: #fff;
  border-radius: 2rem;
  font-weight: 600;
  cursor: pointer;
}
</style>