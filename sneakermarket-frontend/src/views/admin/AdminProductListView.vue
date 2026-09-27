<script setup>
import { computed, nextTick, onMounted, ref, watch } from "vue";
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

// 點上方「庫存偏低」「缺貨」卡片的快速篩選：null / "low" / "out"
const stockFilter = ref(null);
// 點卡片後要捲動到的位置（狀態分頁那一列）
const listSection = ref(null);

const currentPage = ref(1);
const pageSize = 10;

// 低於等於這個數字就標示為低庫存（跟後台首頁「庫存 5 件以下」一致）
const LOW_STOCK_THRESHOLD = 5;

// 跟新增／編輯頁的尺寸格一致：US 7 ～ US 12
const SIZE_OPTIONS = Array.from({ length: 11 }, (_, i) => `US ${7 + i * 0.5}`);

const STATUS_TABS = [
  { value: null, label: "全部" },
  { value: "ON_SALE", label: "販售中" },
  { value: "DRAFT", label: "草稿" },
  { value: "OFF_SHELF", label: "已下架" },
];

const STATUS_LABELS = {
  ON_SALE: "販售中",
  DRAFT: "草稿",
  OFF_SHELF: "已下架",
};

/* ---------- 列表 ---------- */

const totalPages = computed(() => Math.max(1, Math.ceil(products.value.length / pageSize)));

const pagedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return products.value.slice(start, start + pageSize);
});

// 庫存條以目前列表中最大庫存為 100%
const maxStockInList = computed(() =>
  Math.max(1, ...products.value.map((p) => p.totalStock ?? 0))
);

const lowStockCount = computed(
  () => products.value.filter((p) => (p.totalStock ?? 0) <= LOW_STOCK_THRESHOLD).length
);

/* ---------- 上方數據帶：固定抓全部商品，不受篩選影響 ---------- */

const allProducts = ref([]);
const summaryLoading = ref(true);

async function loadSummary() {
  summaryLoading.value = true;
  try {
    const response = await getAdminProducts({});
    allProducts.value = response.data;
  } catch {
    // 錯誤訊息交給列表那邊顯示，這裡不重複處理
  } finally {
    summaryLoading.value = false;
  }
}

const summary = computed(() => {
  const list = allProducts.value;
  const total = list.length;
  const count = (s) => list.filter((p) => p.status === s).length;
  const totalStock = list.reduce((sum, p) => sum + (p.totalStock ?? 0), 0);

  return {
    total,
    onSale: count("ON_SALE"),
    draft: count("DRAFT"),
    offShelf: count("OFF_SHELF"),
    totalStock,
    avgStock: total ? Math.round(totalStock / total) : 0,
    low: list.filter((p) => p.totalStock > 0 && p.totalStock <= LOW_STOCK_THRESHOLD).length,
    out: list.filter((p) => !p.totalStock).length,
  };
});

/* ---------- 品牌佔比環圈圖 ---------- */

// 配色取自經典球鞋配色：Chicago 紅、Royal 藍、Pine 綠、Shattered Backboard 橘、Court 紫、Cement 灰
const BRAND_COLORS = ["#c8102e", "#2f5d9e", "#1f6b4f", "#e08a1e", "#6b4fa0", "#a7a29a"];
const DONUT_R = 52;
const DONUT_C = 2 * Math.PI * DONUT_R;
const DONUT_GAP = 2; // 每段之間留一點縫

const hoveredBrand = ref(null);
const donutDrawn = ref(false);

const brandStats = computed(() => {
  const counts = {};
  const stocks = {};
  allProducts.value.forEach((p) => {
    const name = p.brandName || "未分類";
    counts[name] = (counts[name] || 0) + 1;
    stocks[name] = (stocks[name] || 0) + (p.totalStock ?? 0);
  });
  const totalStockAll = Object.values(stocks).reduce((a, b) => a + b, 0);

  const total = allProducts.value.length;
  const sorted = Object.entries(counts).sort((a, b) => b[1] - a[1]);

  // 前 5 名單獨顯示，其餘合併成「其他」
  const top = sorted.slice(0, 5).map(([name, c]) => [name, c, stocks[name]]);
  const rest = sorted.slice(5);
  if (rest.length > 0) {
    top.push([
      "其他",
      rest.reduce((sum, [, c]) => sum + c, 0),
      rest.reduce((sum, [name]) => sum + stocks[name], 0),
    ]);
  }

  let offset = 0;
  return top.map(([name, count, stock], i) => {
    const length = total ? (count / total) * DONUT_C : 0;
    const item = {
      name,
      count,
      pct: total ? Math.round((count / total) * 100) : 0,
      color: BRAND_COLORS[i],
      length: Math.max(length - (top.length > 1 ? DONUT_GAP : 0), 0),
      offset,
      stock,
      stockPct: totalStockAll ? (stock / totalStockAll) * 100 : 0,
    };
    offset += length;
    return item;
  });
});

const donutCenter = computed(() => {
  const hit = brandStats.value.find((b) => b.name === hoveredBrand.value);
  return hit
    ? { value: hit.pct + "%", label: hit.name }
    : { value: summary.value.total, label: "款商品" };
});

// 資料載入完成後才播放畫圈動畫
watch(summaryLoading, async (isLoading) => {
  if (!isLoading) {
    await nextTick();
    requestAnimationFrame(() => (donutDrawn.value = true));
  }
});

/* ---------- 載入 ---------- */

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
    // 後端只支援「庫存小於 X」，1～5 雙這種範圍由前端再篩一次
    products.value = response.data.filter((p) => {
      if (stockFilter.value === "low") return p.totalStock > 0 && p.totalStock <= LOW_STOCK_THRESHOLD;
      if (stockFilter.value === "out") return !p.totalStock;
      return true;
    });
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
    errorMessage.value = "無法取得商品資料，請確認後端是否啟動後重新整理頁面。";
  } finally {
    loading.value = false;
  }
}

function selectStatus(value) {
  if (status.value === value) return;
  status.value = value;
  loadProducts();
}

function resetFilters() {
  keyword.value = "";
  categoryId.value = null;
  size.value = null;
  brandId.value = null;
  status.value = null;
  maxStock.value = null;
  stockFilter.value = null;
  loadProducts();
}

// 點上方指標卡片：清掉其他篩選，只看庫存偏低或缺貨的商品；再點一次同一張就取消
function pickStockFilter(type) {
  const next = stockFilter.value === type ? null : type;
  keyword.value = "";
  categoryId.value = null;
  size.value = null;
  brandId.value = null;
  status.value = null;
  maxStock.value = null;
  stockFilter.value = next;
  loadProducts();
  nextTick(() => listSection.value?.scrollIntoView({ behavior: "smooth", block: "start" }));
}

function goToPage(page) {
  if (page < 1 || page > totalPages.value) return;
  currentPage.value = page;
}

/* ---------- 顯示用 ---------- */

function formatPrice(price) {
  return price != null ? Number(price).toLocaleString("zh-TW") : "-";
}

function formatId(id) {
  return "#" + String(id).padStart(4, "0");
}

function stockLevel(stock) {
  if (!stock) return "out";
  if (stock <= LOW_STOCK_THRESHOLD) return "low";
  return "ok";
}

function stockPercent(stock) {
  return Math.min(100, Math.round(((stock ?? 0) / maxStockInList.value) * 100));
}

onMounted(() => {
  loadBrands();
  loadProducts();
  loadSummary();
});
</script>

<template>
  <div class="container py-5 product-admin">
    <header class="d-flex justify-content-between align-items-end mb-4 admin-page-heading">
      <div>
        <p class="admin-page-eyebrow">酷客 KICKS ADMIN</p>
        <h1 class="h2 mb-1">商品管理</h1>
        <p class="text-secondary mb-0 admin-page-description">一般商城商品列表</p>
      </div>

      <RouterLink :to="{ name: 'admin-product-create' }" class="btn btn-dark px-4">
        新增商品
      </RouterLink>
    </header>

    <!-- ================= 數據帶 ================= -->
    <section class="stats-strip mb-4" aria-label="商品概況">
      <!-- 商品總數與品牌佔比 -->
      <div class="stat stat-wide">
        <p class="stat-label">商品總數與品牌佔比</p>

        <div class="brand-mix">
          <div class="donut">
            <svg viewBox="0 0 120 120" role="img" aria-label="各品牌商品數量佔比">
              <circle cx="60" cy="60" :r="DONUT_R" fill="none" stroke="#f1efeb" stroke-width="14" />
              <circle
                v-for="b in brandStats"
                :key="b.name"
                class="donut-seg"
                :class="{ faded: hoveredBrand && hoveredBrand !== b.name }"
                cx="60"
                cy="60"
                :r="DONUT_R"
                fill="none"
                :stroke="b.color"
                stroke-width="14"
                :stroke-dasharray="`${donutDrawn ? b.length : 0} ${DONUT_C}`"
                :stroke-dashoffset="-b.offset"
                transform="rotate(-90 60 60)"
              />
            </svg>
            <div class="donut-center" aria-live="polite">
              <span class="donut-value">{{ summaryLoading ? "—" : donutCenter.value }}</span>
              <span class="donut-label">{{ donutCenter.label }}</span>
            </div>
          </div>

          <ul class="brand-list">
            <li
              v-for="b in brandStats"
              :key="b.name"
              class="brand-row"
              :class="{ active: hoveredBrand === b.name }"
              tabindex="0"
              @mouseenter="hoveredBrand = b.name"
              @mouseleave="hoveredBrand = null"
              @focus="hoveredBrand = b.name"
              @blur="hoveredBrand = null"
            >
              <i class="brand-swatch" :style="{ background: b.color }"></i>
              <span class="brand-name">{{ b.name }}</span>
              <span class="brand-count">{{ b.count }} 款</span>
              <span class="brand-pct">{{ b.pct }}%</span>
            </li>
          </ul>
        </div>

        <p class="stat-note mb-0 status-line">
          <span class="dot dot-sale"></span>販售中 {{ summary.onSale }}
          <span class="dot dot-draft"></span>草稿 {{ summary.draft }}
          <span class="dot dot-off"></span>已下架 {{ summary.offShelf }}
        </p>
      </div>

      <!-- 右側：總庫存（含品牌庫存條）＋ 兩個小指標 -->
      <div class="stat-side">
        <div class="stat stat-stock">
          <div class="d-flex justify-content-between align-items-baseline">
            <p class="stat-label">總庫存</p>
            <p class="stat-note mb-0">平均每款 {{ summary.avgStock }} 雙</p>
          </div>
          <p class="stat-value">
            {{ summaryLoading ? "—" : summary.totalStock.toLocaleString("zh-TW") }}<span class="stat-unit">雙</span>
          </p>

          <div class="stock-mix" role="img" aria-label="各品牌庫存佔比">
            <span
              v-for="b in brandStats"
              :key="b.name"
              class="stock-mix-seg"
              :class="{ faded: hoveredBrand && hoveredBrand !== b.name }"
              :style="{ width: (donutDrawn ? b.stockPct : 0) + '%', background: b.color }"
              :title="`${b.name}：${b.stock.toLocaleString('zh-TW')} 雙`"
            ></span>
          </div>
          <p class="stat-note mb-0 stock-mix-caption">
            <template v-if="hoveredBrand">
              {{ hoveredBrand }} 庫存
              <strong>{{ (brandStats.find((b) => b.name === hoveredBrand)?.stock ?? 0).toLocaleString("zh-TW") }}</strong> 雙
            </template>
            <template v-else>依品牌分布，滑過左側品牌可查看</template>
          </p>
        </div>

        <div
          class="stat stat-mini clickable"
          :class="{ selected: stockFilter === 'low' }"
          role="button"
          tabindex="0"
          title="點擊查看庫存偏低的商品"
          @click="pickStockFilter('low')"
          @keydown.enter="pickStockFilter('low')"
        >
          <p class="stat-label">庫存偏低</p>
          <div class="mini-row">
            <p class="stat-value mb-0" :class="{ alert: summary.low > 0 }">
              {{ summaryLoading ? "—" : summary.low }}<span class="stat-unit">款</span>
            </p>
            <span class="health" :class="summary.low > 0 ? 'bad' : 'good'">
              {{ summary.low > 0 ? "需補貨" : "充足" }}
            </span>
          </div>
          <p class="stat-note mb-0">剩 {{ LOW_STOCK_THRESHOLD }} 雙以下</p>
        </div>

        <div
          class="stat stat-mini clickable"
          :class="{ selected: stockFilter === 'out' }"
          role="button"
          tabindex="0"
          title="點擊查看缺貨的商品"
          @click="pickStockFilter('out')"
          @keydown.enter="pickStockFilter('out')"
        >
          <p class="stat-label">缺貨</p>
          <div class="mini-row">
            <p class="stat-value mb-0" :class="{ alert: summary.out > 0 }">
              {{ summaryLoading ? "—" : summary.out }}<span class="stat-unit">款</span>
            </p>
            <span class="health" :class="summary.out > 0 ? 'bad' : 'good'">
              {{ summary.out > 0 ? "缺貨中" : "全部有貨" }}
            </span>
          </div>
          <p class="stat-note mb-0">前台無法下單</p>
        </div>
      </div>
    </section>

    <!-- ================= 篩選 ================= -->
    <section class="filter-panel mb-4">
      <div class="row g-3 align-items-end">
        <div class="col-12 col-md-6 col-lg-3">
          <label class="filter-label" for="f-keyword">商品名稱</label>
          <input
            id="f-keyword"
            v-model="keyword"
            type="text"
            class="form-control"
            placeholder="例如：Jordan 1"
            @keyup.enter="loadProducts"
          />
        </div>

        <div class="col-6 col-md-3 col-lg-2">
          <label class="filter-label" for="f-category">分類</label>
          <select id="f-category" v-model="categoryId" class="form-select">
            <option :value="null">全部分類</option>
            <option :value="1">籃球鞋</option>
            <option :value="2">慢跑鞋</option>
            <option :value="3">休閒鞋</option>
            <option :value="4">滑板鞋</option>
          </select>
        </div>

        <div class="col-6 col-md-3 col-lg-2">
          <label class="filter-label" for="f-brand">品牌</label>
          <select id="f-brand" v-model="brandId" class="form-select" :disabled="loadingBrands">
            <option :value="null">全部品牌</option>
            <option v-for="brand in brands" :key="brand.brandId" :value="brand.brandId">
              {{ brand.brandName }}
            </option>
          </select>
        </div>

        <div class="col-6 col-md-4 col-lg-2">
          <label class="filter-label" for="f-size">尺寸</label>
          <select id="f-size" v-model="size" class="form-select">
            <option :value="null">全部尺寸</option>
            <option v-for="s in SIZE_OPTIONS" :key="s" :value="s">{{ s }}</option>
          </select>
        </div>

        <div class="col-6 col-md-2 col-lg-1">
          <label class="filter-label" for="f-stock">庫存小於</label>
          <input
            id="f-stock"
            v-model.number="maxStock"
            type="number"
            min="0"
            class="form-control"
            placeholder="5"
          />
        </div>

        <div class="col-12 col-md-6 col-lg-2 d-flex gap-2">
          <button type="button" class="btn btn-dark flex-fill" @click="loadProducts">搜尋</button>
          <button type="button" class="btn btn-outline-secondary" @click="resetFilters">重設</button>
        </div>
      </div>
    </section>

    <!-- ================= 狀態分頁 + 結果數 ================= -->
    <div ref="listSection" class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
      <nav class="status-tabs" aria-label="依狀態篩選">
        <button
          v-for="tab in STATUS_TABS"
          :key="tab.label"
          type="button"
          class="status-tab"
          :class="{ active: status === tab.value }"
          :aria-pressed="status === tab.value"
          @click="selectStatus(tab.value)"
        >
          {{ tab.label }}
        </button>
      </nav>

      <p v-if="!loading" class="result-count mb-0">
        共 <strong>{{ products.length }}</strong> 件商品
        <span v-if="lowStockCount > 0" class="result-low">，其中 {{ lowStockCount }} 件庫存偏低</span>
        <span v-if="stockFilter" class="stock-filter-chip">
          只顯示：{{ stockFilter === "low" ? "庫存偏低" : "缺貨" }}
          <button type="button" aria-label="取消篩選" @click="pickStockFilter(stockFilter)">✕</button>
        </span>
      </p>
    </div>

    <div v-if="errorMessage" class="alert alert-danger">{{ errorMessage }}</div>

    <div v-if="loading" class="inventory-empty">商品資料載入中</div>

    <div v-else-if="products.length === 0" class="inventory-empty">
      <p class="mb-3">沒有符合條件的商品，試著放寬篩選條件。</p>
      <button type="button" class="btn btn-outline-dark btn-sm" @click="resetFilters">清除篩選</button>
    </div>

    <!-- ================= 商品表格 ================= -->
    <template v-else>
      <div class="inventory">
        <div class="table-responsive">
          <table class="table inventory-table mb-0">
            <thead>
              <tr>
                <th scope="col">商品</th>
                <th scope="col">狀態</th>
                <th scope="col" class="text-end">售價（NT$）</th>
                <th scope="col" class="col-stock">庫存</th>
                <th scope="col" class="text-end"><span class="visually-hidden">操作</span></th>
              </tr>
            </thead>

            <tbody>
              <tr v-for="product in pagedProducts" :key="product.productId">
                <td>
                  <div class="d-flex align-items-center gap-3">
                    <div class="thumb">
                      <img
                        v-if="product.imageUrl"
                        :src="resolveProductImageUrl(product.imageUrl)"
                        :alt="product.productName"
                        loading="lazy"
                      />
                      <span v-else class="thumb-empty">無圖片</span>
                    </div>
                    <div class="min-w-0">
                      <RouterLink
                        :to="{ name: 'admin-product-edit', params: { id: product.productId } }"
                        class="product-name"
                      >
                        {{ product.productName }}
                      </RouterLink>
                      <p class="product-meta mb-0">
                        <span class="product-brand">{{ product.brandName }}</span>
                        <span class="product-id">{{ formatId(product.productId) }}</span>
                      </p>
                    </div>
                  </div>
                </td>

                <td>
                  <span class="status-pill" :class="`is-${product.status?.toLowerCase()}`">
                    {{ STATUS_LABELS[product.status] ?? product.status }}
                  </span>
                </td>

                <td class="text-end price">{{ formatPrice(product.price) }}</td>

                <td class="col-stock">
                  <div class="stock" :class="`stock-${stockLevel(product.totalStock)}`">
                    <span class="stock-num">{{ product.totalStock ?? 0 }}</span>
                    <div class="stock-track" aria-hidden="true">
                      <div class="stock-bar" :style="{ width: stockPercent(product.totalStock) + '%' }"></div>
                    </div>
                    <span v-if="stockLevel(product.totalStock) === 'out'" class="stock-flag">缺貨</span>
                    <span v-else-if="stockLevel(product.totalStock) === 'low'" class="stock-flag">偏低</span>
                  </div>
                </td>

                <td class="text-end text-nowrap">
                  <a
                    :href="`/products/${product.productId}`"
                    target="_blank"
                    rel="noopener"
                    class="btn btn-link btn-sm text-secondary"
                  >
                    前台檢視
                  </a>
                  <RouterLink
                    :to="{ name: 'admin-product-edit', params: { id: product.productId } }"
                    class="btn btn-sm btn-outline-dark"
                  >
                    編輯
                  </RouterLink>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <nav v-if="totalPages > 1" class="mt-4" aria-label="分頁">
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

<!-- 樣式獨立放在 assets/css，只作用在這一頁（scoped），不會影響其他組員的頁面 -->
<style scoped src="../../assets/css/adminProductList.css"></style>