<script setup>
import { ref, computed, onMounted } from 'vue'
import httpClient from '@/service/httpClient'
import { getBrands } from '@/service/brandService'
import { useRoute } from 'vue-router' // zwei導向分類

const route = useRoute(); //zwei導向分類

const products = ref([])
const loading = ref(true)
const errorMessage = ref('')

const keyword = ref('')
const categoryId = ref(null)
const size = ref(null)
const brandId = ref(null)

const brands = ref([])
const loadingBrands = ref(true)

const openBrand = ref(false)
const openSize = ref(false)
const showFilters = ref(true)
const sortBy = ref('')
const currentPage = ref(1)
const pageSize = 9

const categories = [
  { id: 1, name: '籃球鞋' },
  { id: 2, name: '慢跑鞋' },
  { id: 3, name: '休閒鞋' },
  { id: 4, name: '滑板鞋' },
]

const sizeOptions = ['US 8', 'US 8.5', 'US 9', 'US 9.5', 'US 10', 'US 10.5', 'US 11']

const currentCategoryLabel = computed(() => {
  const found = categories.find(c => c.id === categoryId.value)
  return found ? found.name : '全部商品'
})

const totalPages = computed(() => Math.max(1, Math.ceil(products.value.length / pageSize)))

const sortedProducts = computed(() => {
  const list = [...products.value]
  if (sortBy.value === 'price_asc') {
    list.sort((a, b) => a.price - b.price)
  } else if (sortBy.value === 'price_desc') {
    list.sort((a, b) => b.price - a.price)
  } else if (sortBy.value === 'newest') {
    list.sort((a, b) => b.productId - a.productId)
  }
  return list
})

const pagedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return sortedProducts.value.slice(start, start + pageSize)
})

async function fetchProducts() {
  loading.value = true
  try {
    const response = await httpClient.get('/products', {
      params: {
        keyword: keyword.value || undefined,
        categoryId: categoryId.value || undefined,
        size: size.value || undefined,
        brandId: brandId.value || undefined,
      }
    })
    products.value = response.data
    currentPage.value = 1
  } catch (error) {
    errorMessage.value = '商品資料載入失敗，請確認後端是否啟動'
  } finally {
    loading.value = false
  }
}

async function loadBrands() {
  loadingBrands.value = true
  try {
    const response = await getBrands()
    brands.value = response.data
  } finally {
    loadingBrands.value = false
  }
}

function toggleCategory(id) {
  categoryId.value = categoryId.value === id ? null : id
  fetchProducts()
}

function toggleBrand(id) {
  brandId.value = brandId.value === id ? null : id
  fetchProducts()
}

function toggleSize(value) {
  size.value = size.value === value ? null : value
  fetchProducts()
}

function clearFilters() {
  categoryId.value = null
  brandId.value = null
  size.value = null
  fetchProducts()
}

function goToPage(page) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function formatPrice(price) {
  return price != null ? 'NT$ ' + Number(price).toLocaleString('zh-TW') : '-'
}

onMounted(() => {
  // zwei導向分類，取得id後再執行fetchProducts
  categoryId.value = Number(route.query.categoryId) || null 
  loadBrands()
  fetchProducts()
})
</script>

<template>
  <section>
    <div class="shop-hero">
      <div class="container py-5">
        <span class="hero-icon" aria-hidden="true">👟</span>
        <div class="hero-content">
          <p class="text-uppercase small text-white-50 mb-2 fw-semibold" style="letter-spacing: 0.15em;">SneakerMarket</p>
          <h1 class="display-5 fw-bold mb-2">一般商城</h1>
          <p class="text-white-50 mb-4">嚴選球鞋，找到你的下一雙戰靴</p>

          <form class="d-flex gap-2 hero-search" @submit.prevent="fetchProducts">
            <input
              v-model="keyword"
              type="text"
              class="form-control"
              placeholder="搜尋商品名稱"
            >
            <button type="submit" class="btn btn-light px-4 hero-search-btn">搜尋</button>
          </form>
        </div>
      </div>
    </div>

    <div class="container py-4">
      <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 list-header">
        <h2 class="h4 fw-bold mb-0">
          {{ currentCategoryLabel }}<span v-if="categoryId && !loading"> ({{ products.length }})</span>
        </h2>
        <div class="d-flex align-items-center gap-3">
          <button
            type="button"
            class="btn btn-link btn-sm text-dark text-decoration-none filter-toggle-btn"
            @click="showFilters = !showFilters"
            >
            {{ showFilters ? '« 隱藏篩選條件' : '» 顯示篩選條件' }}
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
              <button type="button" class="filter-toggle" @click="openBrand = !openBrand">
                <span>
                  品牌
                  <span v-if="brandId" class="filter-count">1</span>
                </span>
                <span class="filter-icon">{{ openBrand ? '−' : '+' }}</span>
              </button>
              <div v-show="openBrand" class="filter-options">
                <div v-if="loadingBrands" class="text-secondary small">載入中...</div>
                <label v-for="brand in brands" v-else :key="brand.brandId" class="filter-option">
                  <input
                    type="checkbox"
                    :checked="brandId === brand.brandId"
                    @change="toggleBrand(brand.brandId)"
                  >
                  {{ brand.brandName }}
                </label>
              </div>
            </div>

            <div class="filter-group">
              <button type="button" class="filter-toggle" @click="openSize = !openSize">
                <span>
                  尺寸
                  <span v-if="size" class="filter-count">1</span>
                </span>
                <span class="filter-icon">{{ openSize ? '−' : '+' }}</span>
              </button>
              <div v-show="openSize" class="filter-options">
                <label v-for="s in sizeOptions" :key="s" class="filter-option">
                  <input
                    type="checkbox"
                    :checked="size === s"
                    @change="toggleSize(s)"
                  >
                  {{ s }}
                </label>
              </div>
            </div>

            <button
              type="button"
              class="btn btn-outline-dark btn-sm w-100"
              @click="clearFilters"
            >
              清除篩選
            </button>
          </div>
        </aside>

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
                <router-link :to="`/products/${p.productId}`" class="text-decoration-none text-dark">
                  <div class="card h-100 product-card border-0">
                    <div class="product-card-img-wrap">
                      <img
                        v-if="p.imageUrl"
                        :src="p.imageUrl"
                        :alt="p.productName"
                        class="product-card-img"
                      >
                      <div v-else class="product-card-img product-card-img-empty">尚無圖片</div>
                      <span v-if="p.totalStock === 0" class="stock-badge">缺貨</span>
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

            <nav v-if="totalPages > 1" class="mt-5">
              <ul class="pagination justify-content-center">
                <li class="page-item" :class="{ disabled: currentPage === 1 }">
                  <button class="page-link" @click="goToPage(currentPage - 1)">上一頁</button>
                </li>
                <li
                  v-for="page in totalPages"
                  :key="page"
                  class="page-item"
                  :class="{ active: page === currentPage }"
                >
                  <button class="page-link" @click="goToPage(page)">{{ page }}</button>
                </li>
                <li class="page-item" :class="{ disabled: currentPage === totalPages }">
                  <button class="page-link" @click="goToPage(currentPage + 1)">下一頁</button>
                </li>
              </ul>
            </nav>
          </template>
        </div>
      </div>

      <p class="text-secondary mt-5 mb-0">主要負責：商城系統負責人</p>
    </div>
  </section>
</template>

<style scoped>
.shop-hero {
  background: linear-gradient(135deg, #0a0a0a 0%, #232323 100%);
  color: #fff;
  position: relative;
  overflow: hidden;
}

.hero-content {
  position: relative;
  z-index: 1;
}

.hero-icon {
  position: absolute;
  right: -1rem;
  top: 50%;
  transform: translateY(-50%) rotate(-15deg);
  font-size: 14rem;
  line-height: 1;
  opacity: 0.1;
  pointer-events: none;
  font-family: "Apple Color Emoji", "Segoe UI Emoji", "Noto Color Emoji", sans-serif;
}

.hero-search {
  max-width: 380px;
}

.hero-search input {
  font-size: 0.95rem;
}

.hero-search-btn {
  white-space: nowrap;
  flex-shrink: 0;
}

.list-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background-color: #F8F9FA;
  padding-top: 0.5rem;
  padding-bottom: 0.5rem;
}

.sort-select {
  border: none;
  background-color: transparent;
  box-shadow: none;
  font-weight: 500;
}

.filter-toggle-btn {
  font-weight: 500;
}

.sort-select:focus {
  outline: none;
  box-shadow: none;
}

.filter-sidebar {
  position: sticky;
  top: 4.5rem;
  font-size: 0.82rem;
}

.filter-block {
  margin-bottom: 0.75rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #dee2e6;
}

.category-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.category-item {
  display: block;
  width: 100%;
  text-align: left;
  background: none;
  border: none;
  padding: 0.18rem 0;
  cursor: pointer;
  color: #212529;
  font-size: 1.05rem;
  font-weight: 500;
  line-height: 1.7;
}

.category-item.active {
  font-weight: 700;
  text-decoration: underline;
}

.filter-group {
  border-bottom: 1px solid #dee2e6;
  padding-bottom: 0.5rem;
  margin-bottom: 0.5rem;
}

.filter-toggle {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  background: none;
  border: none;
  padding: 0;
  font-weight: 600;
  font-size: 0.85rem;
  cursor: pointer;
}

.filter-icon {
  color: #6c757d;
}

.filter-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1rem;
  height: 1rem;
  margin-left: 0.35rem;
  border-radius: 50%;
  background-color: #111111;
  color: #fff;
  font-size: 0.6rem;
  font-weight: 600;
}

.filter-options {
  margin-top: 0.4rem;
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.filter-option {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  cursor: pointer;
  font-size: 0.82rem;
}

.filter-option input[type="checkbox"] {
  accent-color: #111111;
  width: 0.85rem;
  height: 0.85rem;
}

.product-card {
  transition: transform 0.2s ease;
}

.product-card:hover {
  transform: translateY(-4px);
}

.product-card-img-wrap {
  position: relative;
  overflow: hidden;
  border-radius: 0;
  background-color: #f1f1f1;
}

.product-card-img {
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  display: block;
  border-radius: 0;
  transition: transform 0.3s ease;
}

.product-card:hover .product-card-img {
  transform: scale(1.06);
}

.product-card-img-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 0.85rem;
  aspect-ratio: 1 / 1;
}

.stock-badge {
  position: absolute;
  top: 0.5rem;
  left: 0.5rem;
  background-color: #111111;
  color: #fff;
  font-size: 0.7rem;
  padding: 0.2rem 0.5rem;
  border-radius: 0.25rem;
}

.product-name {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.pagination .page-link {
  color: #212529;
}

.pagination .page-item.active .page-link {
  background-color: #212529;
  border-color: #212529;
}
</style>