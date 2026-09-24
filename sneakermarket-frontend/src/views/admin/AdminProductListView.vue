<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import { getAdminProducts } from "@/service/productAdminService";
import { getBrands } from "@/service/brandService";
import { resolveProductImageUrl } from "@/utils/productImageUrl";

const router = useRouter();

const products = ref([]);
const loading = ref(true);
const errorMessage = ref("");

const keyword = ref("");
const categoryId = ref(null);
const size = ref(null);
const brands = ref([]);
const loadingBrands = ref(true);
const brandId = ref(null);
const status = ref(null);
const maxStock = ref(null);

const currentPage = ref(1);
const pageSize = 10;

const totalPages = computed(() => Math.max(1, Math.ceil(products.value.length / pageSize)));

const pagedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return products.value.slice(start, start + pageSize);
});

async function loadBrands() {
  loadingBrands.value = true;
  try {
    const response = await getBrands();
    brands.value = response.data;
  } finally {
    loadingBrands.value = false;
  }
}

async function loadProducts() {
  loading.value = true;
  errorMessage.value = "";

  try {
    const response = await getAdminProducts({
      keyword: keyword.value || undefined,
      categoryId: categoryId.value || undefined,
      size: size.value || undefined,
      brandId: brandId.value || undefined,
      status: status.value || undefined,
      maxStock: maxStock.value || undefined,
    });
    products.value = response.data;
    currentPage.value = 1;
  } catch (error) {
    if (error.response?.status === 401) {
      await router.replace({ name: "login", query: { redirect: "/admin/products" } });
      return;
    }
    if (error.response?.status === 403) {
      await router.replace({ name: "forbidden" });
      return;
    }
    errorMessage.value = "取得商品資料失敗";
  } finally {
    loading.value = false;
  }
}

function goToPage(page) {
  if (page < 1 || page > totalPages.value) return;
  currentPage.value = page;
}

function formatPrice(price) {
  return price != null ? "NT$ " + price : "-";
}

function statusBadgeClass(status) {
  return {
    DRAFT: "bg-secondary",
    ON_SALE: "bg-success",
    OFF_SHELF: "bg-warning text-dark",
  }[status] || "bg-dark";
}

onMounted(() => {
  loadBrands();
  loadProducts();
});
</script>

<template>
  <div class="container py-5">
    <header class="d-flex justify-content-between align-items-center mb-4 admin-page-heading">
      <div>
        <p class="admin-page-eyebrow">SNEAKERMARKET ADMIN</p>
        <h1 class="h2 mb-1">商品管理</h1>
        <p class="text-secondary mb-0 admin-page-description">一般商城商品列表</p>
      </div>

      <RouterLink :to="{ name: 'admin-product-create' }" class="btn btn-dark">
        + 新增商品
      </RouterLink>
    </header>

    <div class="card shadow-sm mb-4">
      <div class="card-body">
        <div class="row g-2 align-items-end">
          <div class="col-12 col-md-4">
            <label class="form-label small text-secondary">商品名稱</label>
            <input
              v-model="keyword"
              type="text"
              class="form-control"
              placeholder="搜尋商品名稱"
              @keyup.enter="loadProducts"
            />
          </div>

          <div class="col-6 col-md-3">
            <label class="form-label small text-secondary">分類</label>
            <select v-model="categoryId" class="form-select">
              <option :value="null">全部分類</option>
              <option :value="1">籃球鞋</option>
              <option :value="2">慢跑鞋</option>
              <option :value="3">休閒鞋</option>
              <option :value="4">滑板鞋</option>
            </select>
          </div>

          <div class="col-6 col-md-3">
            <label class="form-label small text-secondary">尺寸</label>
            <select v-model="size" class="form-select">
              <option :value="null">全部尺寸</option>
              <option value="US 8">US 8</option>
              <option value="US 8.5">US 8.5</option>
              <option value="US 9">US 9</option>
              <option value="US 9.5">US 9.5</option>
              <option value="US 10">US 10</option>
              <option value="US 10.5">US 10.5</option>
              <option value="US 11">US 11</option>
            </select>
          </div>

          <div class="col-6 col-md-3">
            <label class="form-label small text-secondary">品牌</label>
            <select v-model="brandId" class="form-select" :disabled="loadingBrands">
              <option :value="null">全部品牌</option>
              <option v-for="brand in brands" :key="brand.brandId" :value="brand.brandId">
                {{ brand.brandName }}
              </option>
            </select>
          </div>

          <div class="col-6 col-md-3">
            <label class="form-label small text-secondary">狀態</label>
            <select v-model="status" class="form-select">
              <option :value="null">全部狀態</option>
              <option value="DRAFT">草稿</option>
              <option value="ON_SALE">販售中</option>
              <option value="OFF_SHELF">已下架</option>
            </select>
          </div>

          <div class="col-6 col-md-3">
            <label class="form-label small text-secondary">庫存小於</label>
            <input
              v-model.number="maxStock"
              type="number"
              min="0"
              class="form-control"
              placeholder="例如 5"
            />
          </div>

          <div class="col-12 col-md-2">
            <button type="button" class="btn btn-dark w-100" @click="loadProducts">搜尋</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="errorMessage" class="alert alert-danger">{{ errorMessage }}</div>

    <div v-if="loading" class="alert alert-secondary">商品資料載入中...</div>

    <div v-else-if="products.length === 0" class="alert alert-warning">目前沒有符合條件的商品</div>

    <template v-else>
      <div v-for="product in pagedProducts" :key="product.productId" class="card mb-3 shadow-sm">
        <div class="row g-0">
          <div class="col-4 col-md-2">
            <img
              v-if="product.imageUrl"
              :src="resolveProductImageUrl(product.imageUrl)"
              :alt="product.productName"
              class="product-thumb-h"
            />
            <div v-else class="product-thumb-h product-thumb-empty">尚無圖片</div>
          </div>

          <div class="col-8 col-md-10">
            <div class="card-body d-flex flex-wrap justify-content-between align-items-center h-100 gap-2">
              <div>
                <span class="badge mb-1" :class="statusBadgeClass(product.status)">{{ product.status }}</span>
                <h5 class="card-title mb-1">{{ product.productName }}</h5>
                <p class="text-secondary small mb-1">{{ product.brandName }}</p>
                <p class="fw-semibold mb-0">{{ formatPrice(product.price) }}</p>
                <p class="small mb-0" :class="product.totalStock > 0 ? 'text-secondary' : 'text-danger fw-semibold'">
                庫存：{{ product.totalStock }}<span v-if="product.totalStock === 0">（缺貨）</span>
                </p>
              </div>

              <div class="d-flex gap-2">        
                <RouterLink
                  :to="{ name: 'admin-product-edit', params: { id: product.productId } }"
                  class="btn btn-sm btn-outline-dark"
                >
                  編輯
                </RouterLink>
              </div>
            </div>
          </div>
        </div>
      </div>

      <nav v-if="totalPages > 1" class="mt-4">
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
</template>

<style scoped>
.product-thumb-h {
  width: 100%;
  height: 100%;
  min-height: 110px;
  object-fit: cover;
}

.product-thumb-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 110px;
  background-color: #f1f1f1;
  color: #999;
  font-size: 0.85rem;
}

.pagination .page-link {
  color: #212529;
}

.pagination .page-item.active .page-link {
  background-color: #212529;
  border-color: #212529;
}
</style>