<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { getBrands } from "@/service/brandService";
import {
  uploadProductImage,
  getAdminProductDetail,
  createProduct,
  updateProduct,
} from "@/service/productAdminService";
import { resolveProductImageUrl } from "@/utils/productImageUrl";

const route = useRoute();
const router = useRouter();
const isEditMode = !!route.params.id;

const brands = ref([]);
const loadingBrands = ref(true);

const CATEGORIES = [
  { id: 1, name: "籃球鞋" },
  { id: 2, name: "慢跑鞋" },
  { id: 3, name: "休閒鞋" },
  { id: 4, name: "滑板鞋" },
];

const STATUS_OPTIONS = [
  { value: "DRAFT", label: "草稿", hint: "前台看不到" },
  { value: "ON_SALE", label: "販售中", hint: "前台可購買" },
  { value: "OFF_SHELF", label: "已下架", hint: "前台看不到" },
];

// 尺寸格：US 7 ～ US 12，每半號一格
const SIZE_GRID = Array.from({ length: 11 }, (_, i) => `US ${7 + i * 0.5}`);

// 售價、配色整個商品共用一組，送出時再套用到每一筆規格
const form = ref({
  categoryId: null,
  brandId: null,
  productName: "",
  description: "",
  status: "DRAFT",
  price: null,
  color: "",
  variants: [], // { variantId, size, stock }
  images: [], // { imageUrl, isPrimary }
});

const customSize = ref("");
const uploading = ref(false);
const submitting = ref(false);
const errorMessage = ref("");
const fileInput = ref(null);

/* ---------- 尺寸 ---------- */

function sizeValue(size) {
  const n = parseFloat(String(size).replace(/[^\d.]/g, ""));
  return Number.isNaN(n) ? 999 : n;
}

const sortedVariants = computed(() =>
  [...form.value.variants].sort((a, b) => sizeValue(a.size) - sizeValue(b.size))
);

const totalStock = computed(() =>
  form.value.variants.reduce((sum, v) => sum + (Number(v.stock) || 0), 0)
);

function findVariant(size) {
  return form.value.variants.find((v) => v.size === size);
}

function addSize(size) {
  const trimmed = size.trim();
  if (!trimmed || findVariant(trimmed)) return;
  form.value.variants.push({ variantId: null, size: trimmed, stock: null });
}

function removeVariant(variant) {
  if ((Number(variant.stock) || 0) > 0 && !window.confirm(`移除 ${variant.size}？這個尺寸的庫存會一起刪除。`)) {
    return;
  }
  const index = form.value.variants.indexOf(variant);
  if (index !== -1) form.value.variants.splice(index, 1);
}

function toggleSize(size) {
  const existing = findVariant(size);
  if (existing) {
    removeVariant(existing);
  } else {
    addSize(size);
  }
}

function addCustomSize() {
  const raw = customSize.value.trim();
  if (!raw) return;
  // 只打數字時自動補上 US
  addSize(/^\d+(\.\d+)?$/.test(raw) ? `US ${raw}` : raw);
  customSize.value = "";
}

// 不在尺寸格裡的尺寸（例如 US 13），接在尺寸格後面
const extraSizes = computed(() =>
  sortedVariants.value.filter((v) => !SIZE_GRID.includes(v.size))
);

/* ---------- 圖片 ---------- */

const primaryImage = computed(() => form.value.images.find((img) => img.isPrimary) ?? null);

function triggerFileInput() {
  fileInput.value?.click();
}

async function handleImageUpload(event) {
  const file = event.target.files[0];
  if (!file) return;

  uploading.value = true;
  try {
    const response = await uploadProductImage(file);
    form.value.images.push({
      imageUrl: response.data,
      isPrimary: form.value.images.length === 0, // 第一張自動當主圖
    });
  } catch (error) {
    errorMessage.value = "圖片上傳失敗，請確認檔案格式後再試一次";
  } finally {
    uploading.value = false;
    event.target.value = "";
  }
}

function removeImage(index) {
  const wasPrimary = form.value.images[index].isPrimary;
  form.value.images.splice(index, 1);
  if (wasPrimary && form.value.images.length > 0) {
    form.value.images[0].isPrimary = true;
  }
}

function setPrimaryImage(index) {
  form.value.images.forEach((img, i) => {
    img.isPrimary = i === index;
  });
}

/* ---------- 前台預覽用 ---------- */

const previewBrand = computed(
  () => brands.value.find((b) => b.brandId === form.value.brandId)?.brandName ?? "品牌"
);

const previewCategory = computed(
  () => CATEGORIES.find((c) => c.id === form.value.categoryId)?.name ?? ""
);

const previewPrice = computed(() =>
  form.value.price != null && form.value.price !== ""
    ? "NT$ " + Number(form.value.price).toLocaleString("zh-TW")
    : "NT$ —"
);

const previewNotice = computed(() => {
  if (form.value.status === "DRAFT") return "草稿，前台不會顯示";
  if (form.value.status === "OFF_SHELF") return "已下架，前台不會顯示";
  if (totalStock.value === 0) return "目前全部缺貨";
  return "";
});

/* ---------- 載入 / 送出 ---------- */

async function loadBrands() {
  loadingBrands.value = true;
  try {
    const response = await getBrands();
    brands.value = response.data;
  } finally {
    loadingBrands.value = false;
  }
}

async function loadExistingProduct() {
  const response = await getAdminProductDetail(route.params.id);
  const detail = response.data;
  const first = detail.variants[0];

  form.value.categoryId = detail.categoryId;
  form.value.brandId = detail.brandId;
  form.value.productName = detail.productName;
  form.value.description = detail.description;
  form.value.status = detail.status;
  form.value.price = first?.price ?? null;
  form.value.color = first?.color ?? "";

  form.value.variants = detail.variants.map((v) => ({
    variantId: v.variantId,
    size: v.size,
    stock: v.stock,
  }));

  form.value.images = detail.images.map((imageUrl, index) => ({
    imageUrl,
    isPrimary: index === 0,
  }));
}

async function handleSubmit() {
  errorMessage.value = "";

  if (!form.value.categoryId || !form.value.brandId || !form.value.productName.trim()) {
    errorMessage.value = "請把分類、品牌、商品名稱填寫完整";
    return;
  }

  if (form.value.price == null || form.value.price === "" || form.value.price <= 0) {
    errorMessage.value = "請填寫售價";
    return;
  }

  if (form.value.variants.length === 0) {
    errorMessage.value = "至少要選一個尺寸";
    return;
  }

  const payload = {
    categoryId: form.value.categoryId,
    brandId: form.value.brandId,
    productName: form.value.productName,
    description: form.value.description,
    status: form.value.status,
    variants: sortedVariants.value.map((v) => ({
      variantId: v.variantId,
      size: v.size,
      color: form.value.color,
      price: form.value.price,
      stock: Number(v.stock) || 0,
    })),
    images: form.value.images.map((img, index) => ({
      imageUrl: img.imageUrl,
      sortOrder: index,
      isPrimary: img.isPrimary,
    })),
  };

  submitting.value = true;

  try {
    if (isEditMode) {
      await updateProduct(route.params.id, payload);
    } else {
      await createProduct(payload);
    }
    router.push({ name: "admin-products" });
  } catch (error) {
    errorMessage.value = "儲存失敗，請確認欄位內容是否正確";
  } finally {
    submitting.value = false;
  }
}

onMounted(async () => {
  await loadBrands();
  if (isEditMode) {
    await loadExistingProduct();
  }
});
</script>

<template>
  <div class="container py-5 product-form">
    <header class="d-flex justify-content-between align-items-end mb-4 admin-page-heading">
      <div>
        <p class="admin-page-eyebrow">酷客 KICKS ADMIN</p>
        <h1 class="h2 mb-1">{{ isEditMode ? "編輯商品" : "新增商品" }}</h1>
        <p class="text-secondary mb-0 admin-page-description">
          {{ isEditMode ? form.productName || "載入中" : "填寫商品資料、尺寸與庫存" }}
        </p>
      </div>

      <RouterLink :to="{ name: 'admin-products' }" class="back-link">返回商品列表</RouterLink>
    </header>

    <div class="row g-4">
      <!-- ================= 左欄 ================= -->
      <div class="col-12 col-lg-8">
        <!-- 基本資料 -->
        <section class="panel mb-4">
          <h2 class="panel-title">基本資料</h2>

          <div class="row g-3">
            <div class="col-12">
              <label class="field-label" for="p-name">商品名稱 <span class="req">*</span></label>
              <input
                id="p-name"
                v-model="form.productName"
                type="text"
                class="form-control form-control-lg name-input"
                placeholder="例如：Air Jordan 1 Retro High OG &quot;Chicago&quot;"
              />
            </div>

            <div class="col-6">
              <label class="field-label" for="p-category">分類 <span class="req">*</span></label>
              <select id="p-category" v-model="form.categoryId" class="form-select">
                <option :value="null" disabled>請選擇分類</option>
                <option v-for="c in CATEGORIES" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
            </div>

            <div class="col-6">
              <label class="field-label" for="p-brand">品牌 <span class="req">*</span></label>
              <select id="p-brand" v-model="form.brandId" class="form-select" :disabled="loadingBrands">
                <option :value="null" disabled>請選擇品牌</option>
                <option v-for="brand in brands" :key="brand.brandId" :value="brand.brandId">
                  {{ brand.brandName }}
                </option>
              </select>
            </div>

            <div class="col-6">
              <label class="field-label" for="p-price">售價 <span class="req">*</span></label>
              <div class="input-group">
                <span class="input-group-text">NT$</span>
                <input
                  id="p-price"
                  v-model.number="form.price"
                  type="number"
                  min="0"
                  class="form-control price-input"
                  placeholder="6500"
                />
              </div>
            </div>

            <div class="col-6">
              <label class="field-label" for="p-color">配色</label>
              <input
                id="p-color"
                v-model="form.color"
                type="text"
                class="form-control"
                placeholder="例如：白／紅"
              />
            </div>

            <div class="col-12">
              <label class="field-label" for="p-desc">商品介紹</label>
              <textarea
                id="p-desc"
                v-model="form.description"
                class="form-control"
                rows="5"
                placeholder="配色、材質、發售背景等，會顯示在前台商品頁"
              ></textarea>
            </div>
          </div>
        </section>

        <!-- 尺寸與庫存：尺寸和庫存上下對齊在同一格 -->
        <section class="panel">
          <div class="d-flex justify-content-between align-items-baseline mb-3">
            <h2 class="panel-title mb-0">尺寸與庫存</h2>
            <span class="panel-aside">點尺寸開啟販售，再填庫存</span>
          </div>

          <div class="size-board">
            <div
              v-for="size in SIZE_GRID"
              :key="size"
              class="size-cell"
              :class="{ on: findVariant(size), empty: findVariant(size) && !findVariant(size).stock }"
            >
              <button
                type="button"
                class="size-toggle"
                :aria-pressed="!!findVariant(size)"
                :aria-label="findVariant(size) ? `停售 ${size}` : `開啟 ${size}`"
                @click="toggleSize(size)"
              >
                <span class="size-us">US</span>
                <span class="size-num">{{ size.replace("US ", "") }}</span>
              </button>

              <div class="size-stock">
                <template v-if="findVariant(size)">
                  <input
                    v-model.number="findVariant(size).stock"
                    type="number"
                    min="0"
                    placeholder="0"
                    :aria-label="`${size} 庫存`"
                  />
                  <span class="size-unit">雙</span>
                </template>
                <span v-else class="size-off">未販售</span>
              </div>
            </div>

            <!-- 其他尺寸（例如 US 13） -->
            <div v-for="v in extraSizes" :key="v.size" class="size-cell on" :class="{ empty: !v.stock }">
              <button
                type="button"
                class="size-toggle"
                aria-pressed="true"
                :aria-label="`停售 ${v.size}`"
                @click="removeVariant(v)"
              >
                <span class="size-us">{{ /^US /.test(v.size) ? "US" : "" }}</span>
                <span class="size-num">{{ v.size.replace("US ", "") }}</span>
              </button>
              <div class="size-stock">
                <input
                  v-model.number="v.stock"
                  type="number"
                  min="0"
                  placeholder="0"
                  :aria-label="`${v.size} 庫存`"
                />
                <span class="size-unit">雙</span>
              </div>
            </div>

            <!-- 新增其他尺寸 -->
            <div class="size-cell add">
              <label class="size-toggle add-label" for="custom-size">
                <span class="size-us">其他</span>
                <span class="size-num">+</span>
              </label>
              <div class="size-stock">
                <input
                  id="custom-size"
                  v-model="customSize"
                  type="text"
                  placeholder="13"
                  aria-label="其他尺寸，輸入後按 Enter"
                  @keyup.enter="addCustomSize"
                  @blur="addCustomSize"
                />
              </div>
            </div>
          </div>

          <div class="size-summary">
            <span>
              已開啟 <strong>{{ form.variants.length }}</strong> 個尺寸，總庫存
              <strong>{{ totalStock }}</strong> 雙
            </span>
            <span class="legend">
              <i class="legend-box on"></i>販售中
              <i class="legend-box empty"></i>未填庫存
            </span>
          </div>
        </section>
      </div>

      <!-- ================= 右欄 ================= -->
      <div class="col-12 col-lg-4">
        <!-- 前台預覽（圖片也在這裡上傳） -->
        <section class="panel mb-4">
          <div class="d-flex justify-content-between align-items-baseline">
            <h2 class="panel-title">前台預覽</h2>
            <span class="panel-aside">商城列表中的樣子</span>
          </div>

          <article class="preview-card" :class="{ dimmed: form.status !== 'ON_SALE' }">
            <button
              v-if="!primaryImage"
              type="button"
              class="preview-photo"
              :disabled="uploading"
              aria-label="上傳商品圖片"
              @click="triggerFileInput"
            >
              <span class="preview-upload">
                <strong>{{ uploading ? "上傳中" : "上傳商品圖片" }}</strong>
                <small>淺灰底、正方形、單隻側面</small>
              </span>
            </button>

            <div v-else class="preview-photo">
              <img :src="resolveProductImageUrl(primaryImage.imageUrl)" alt="主圖預覽" />
            </div>

            <div class="preview-body">
              <p class="preview-brand mb-1">
                {{ previewBrand }}<template v-if="previewCategory">　{{ previewCategory }}</template>
              </p>
              <h3 class="preview-name">{{ form.productName || "商品名稱" }}</h3>
              <p class="preview-price mb-0">{{ previewPrice }}</p>
            </div>
          </article>

          <div v-if="form.images.length > 0" class="thumb-grid">
            <div
              v-for="(image, index) in form.images"
              :key="image.imageUrl"
              class="thumb-item"
              :class="{ primary: image.isPrimary }"
            >
              <button
                type="button"
                class="thumb-select"
                :aria-label="image.isPrimary ? '目前主圖' : `將第 ${index + 1} 張設為主圖`"
                :title="image.isPrimary ? '目前主圖' : '設為主圖'"
                @click="setPrimaryImage(index)"
              >
                <img :src="resolveProductImageUrl(image.imageUrl)" alt="" />
              </button>
              <span v-if="image.isPrimary" class="thumb-badge">主圖</span>
              <button
                type="button"
                class="thumb-remove"
                :aria-label="`刪除第 ${index + 1} 張圖片`"
                @click="removeImage(index)"
              >
                ×
              </button>
            </div>

            <button type="button" class="thumb-upload" :disabled="uploading" @click="triggerFileInput">
              {{ uploading ? "上傳中" : "+ 圖片" }}
            </button>
          </div>

          <input ref="fileInput" type="file" accept="image/*" class="d-none" @change="handleImageUpload" />

          <p v-if="previewNotice" class="preview-notice mb-0">{{ previewNotice }}</p>
        </section>

        <!-- 上架狀態 -->
        <section class="panel">
          <h2 class="panel-title">上架狀態</h2>

          <div class="status-options" role="radiogroup" aria-label="上架狀態">
            <label
              v-for="opt in STATUS_OPTIONS"
              :key="opt.value"
              class="status-option"
              :class="[`is-${opt.value.toLowerCase()}`, { active: form.status === opt.value }]"
            >
              <input v-model="form.status" type="radio" name="status" :value="opt.value" class="visually-hidden" />
              <span class="status-dot"></span>
              <span class="status-text">{{ opt.label }}</span>
              <span class="status-hint">{{ opt.hint }}</span>
            </label>
          </div>
        </section>
      </div>
    </div>

    <!-- 底部操作列 -->
    <div class="action-bar">
      <p v-if="errorMessage" class="action-error mb-0" role="alert">{{ errorMessage }}</p>
      <div class="ms-auto d-flex gap-2">
        <RouterLink :to="{ name: 'admin-products' }" class="btn btn-outline-secondary">取消</RouterLink>
        <button type="button" class="btn btn-dark px-4" :disabled="submitting" @click="handleSubmit">
          {{ submitting ? "儲存中" : (isEditMode ? "儲存修改" : "新增商品") }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ---------- tokens（跟商品列表頁同一組） ---------- */
.product-form {
  --ink: #212529;
  --muted: #7a7a7a;
  --faint: #b5b5b5;
  --line: #e7e5e1;
  --soft: #faf9f7;
  --photo: #f1f1f1;
  --sale: #1f7a4d;
  --draft: #8c8c8c;
  --off: #b7791f;
  --danger: #c0392b;
  --warn-bg: #fdf3f1;
  --warn-line: #efcfc8;
}

.back-link {
  color: var(--muted);
  font-size: 0.875rem;
  text-decoration: none;
}

.back-link:hover {
  color: var(--ink);
  text-decoration: underline;
  text-underline-offset: 3px;
}

/* ---------- panels ---------- */
.panel {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 1.5rem;
}

.panel-title {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 1rem;
  color: var(--ink);
}

.panel-aside {
  font-size: 0.78rem;
  color: var(--muted);
}

.field-label {
  display: block;
  margin-bottom: 0.35rem;
  font-size: 0.8rem;
  color: var(--muted);
}

.req {
  color: var(--danger);
}

.name-input,
.price-input {
  font-weight: 600;
}

.name-input::placeholder,
.price-input::placeholder {
  font-weight: 400;
  color: var(--faint);
}

.price-input {
  font-variant-numeric: tabular-nums;
}

/* ---------- size board：尺寸與庫存同一格 ---------- */
.size-board {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 0.5rem;
}

.size-cell {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--line);
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
  transition: border-color 0.12s;
}

.size-cell:hover {
  border-color: #c9c6c0;
}

.size-toggle {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 0.2rem;
  width: 100%;
  height: 42px;
  padding: 0;
  margin: 0;
  border: 0;
  background: var(--soft);
  color: var(--ink);
  cursor: pointer;
  line-height: 42px;
  transition: background-color 0.12s, color 0.12s;
}

.size-toggle:focus-visible {
  outline: 2px solid var(--ink);
  outline-offset: -2px;
}

.size-us {
  font-size: 0.65rem;
  color: var(--muted);
}

.size-num {
  font-size: 1.05rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.size-stock {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.2rem;
  height: 40px;
  padding: 0 0.5rem;
  border-top: 1px solid var(--line);
}

.size-stock input {
  width: 100%;
  min-width: 0;
  border: 0;
  padding: 0;
  text-align: right;
  font-size: 1rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--ink);
  background: transparent;
}

.size-stock input:focus {
  outline: none;
}

.size-stock input::placeholder {
  color: #d0d0d0;
}

.size-unit {
  flex: 0 0 auto;
  font-size: 0.72rem;
  color: var(--muted);
}

.size-off {
  font-size: 0.72rem;
  color: var(--faint);
}

/* 開啟販售 */
.size-cell.on {
  border-color: var(--ink);
}

.size-cell.on .size-toggle {
  background: var(--ink);
  color: #fff;
}

.size-cell.on .size-us {
  color: rgba(255, 255, 255, 0.6);
}

.size-cell.on:focus-within {
  box-shadow: 0 0 0 2px rgba(33, 37, 41, 0.15);
}

/* 開啟了但庫存還沒填 */
.size-cell.on.empty .size-stock {
  background: var(--warn-bg);
}

/* 新增其他尺寸 */
.size-cell.add {
  border-style: dashed;
}

.size-cell.add .size-toggle {
  background: transparent;
  cursor: text;
}

.size-cell.add .size-num {
  font-weight: 500;
  color: var(--muted);
}

.size-cell.add .size-stock input {
  text-align: center;
  font-weight: 500;
}

.size-summary {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
  margin-top: 1rem;
  padding-top: 0.9rem;
  border-top: 1px solid var(--line);
  font-size: 0.85rem;
  color: var(--muted);
}

.size-summary strong {
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}

.legend {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.75rem;
}

.legend-box {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 3px;
  margin-left: 0.5rem;
}

.legend-box.on {
  background: var(--ink);
}

.legend-box.empty {
  background: var(--warn-bg);
  border: 1px solid var(--warn-line);
}

@media (max-width: 767.98px) {
  .size-board {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

/* ---------- preview card ---------- */
.preview-card {
  border: 1px solid var(--line);
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
  transition: opacity 0.15s;
}

.preview-card.dimmed {
  opacity: 0.55;
}

.preview-photo {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  aspect-ratio: 1 / 1;
  padding: 0;
  border: 0;
  background: var(--photo);
}

button.preview-photo {
  cursor: pointer;
}

.preview-photo img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 8%;
}

.preview-photo:focus-visible {
  outline: 2px solid var(--ink);
  outline-offset: -2px;
}

.preview-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.3rem;
  padding: 1.25rem 1.5rem;
  border: 1px dashed #c9c6c0;
  border-radius: 8px;
  color: var(--ink);
  font-size: 0.85rem;
}

.preview-upload small {
  font-size: 0.72rem;
  color: var(--muted);
}

.preview-photo:hover .preview-upload {
  border-color: var(--ink);
}

.preview-body {
  padding: 0.9rem 1rem 1rem;
}

.preview-brand {
  font-size: 0.75rem;
  color: var(--muted);
}

.preview-name {
  font-size: 0.95rem;
  font-weight: 600;
  line-height: 1.4;
  margin-bottom: 0.4rem;
  color: var(--ink);
  word-break: break-word;
}

.preview-price {
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.preview-notice {
  margin-top: 0.6rem;
  font-size: 0.78rem;
  color: var(--off);
}

/* ---------- thumbnails ---------- */
.thumb-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 0.4rem;
  margin-top: 0.6rem;
}

.thumb-item {
  position: relative;
}

.thumb-select {
  display: block;
  width: 100%;
  aspect-ratio: 1 / 1;
  padding: 0;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--photo);
  overflow: hidden;
}

.thumb-select img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 6%;
}

.thumb-item.primary .thumb-select {
  border-color: var(--ink);
  box-shadow: inset 0 0 0 1px var(--ink);
}

.thumb-badge {
  position: absolute;
  left: 3px;
  bottom: 3px;
  padding: 0 0.25rem;
  font-size: 0.6rem;
  color: #fff;
  background: var(--ink);
  border-radius: 3px;
}

.thumb-remove {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  font-size: 0.8rem;
  line-height: 1;
  color: #fff;
  background: var(--ink);
  border: 2px solid #fff;
  border-radius: 50%;
  opacity: 0;
}

.thumb-item:hover .thumb-remove,
.thumb-remove:focus-visible {
  opacity: 1;
}

@media (hover: none) {
  .thumb-remove {
    opacity: 1;
  }
}

.thumb-upload {
  aspect-ratio: 1 / 1;
  border: 1px dashed #c9c6c0;
  border-radius: 6px;
  background: #fff;
  font-size: 0.7rem;
  color: var(--muted);
}

.thumb-upload:hover:not(:disabled) {
  border-color: var(--ink);
  color: var(--ink);
}

.thumb-select:focus-visible,
.thumb-remove:focus-visible,
.thumb-upload:focus-visible {
  outline: 2px solid var(--ink);
  outline-offset: 2px;
}

/* ---------- status ---------- */
.status-options {
  display: grid;
  gap: 0.5rem;
}

.status-option {
  display: grid;
  grid-template-columns: 10px 1fr auto;
  align-items: center;
  gap: 0.6rem;
  padding: 0.7rem 0.9rem;
  border: 1px solid var(--line);
  border-radius: 8px;
  cursor: pointer;
}

.status-option:hover {
  border-color: #c9c6c0;
}

.status-option.active {
  border-color: var(--ink);
  background: var(--soft);
}

.status-option:has(input:focus-visible) {
  outline: 2px solid var(--ink);
  outline-offset: 2px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--draft);
}

.status-option.is-on_sale .status-dot { background: var(--sale); }
.status-option.is-off_shelf .status-dot { background: var(--off); }

.status-text {
  font-size: 0.9rem;
  font-weight: 500;
}

.status-hint {
  font-size: 0.75rem;
  color: var(--muted);
}

/* ---------- action bar ---------- */
.action-bar {
  position: sticky;
  bottom: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 1.5rem;
  padding: 0.75rem 1.25rem;
  padding-bottom: calc(0.75rem + env(safe-area-inset-bottom, 0px));
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 12px;
  box-shadow: 0 -6px 20px rgba(33, 37, 41, 0.08);
}

.action-error {
  font-size: 0.85rem;
  color: var(--danger);
}

@media (prefers-reduced-motion: reduce) {
  .size-cell,
  .size-toggle,
  .preview-card {
    transition: none;
  }
}
</style>