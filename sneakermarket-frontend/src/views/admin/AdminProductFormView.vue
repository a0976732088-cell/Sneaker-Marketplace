<script setup>
import { onMounted, ref } from "vue";
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

const form = ref({
  categoryId: null,
  brandId: null,
  productName: "",
  description: "",
  variants: [],
  images: [],
});

const uploading = ref(false);
const submitting = ref(false);
const errorMessage = ref("");
const fileInput = ref(null);

function triggerFileInput() {
  fileInput.value?.click();
}

function addVariant() {
  form.value.variants.push({ variantId: null, size: "", color: "", price: null, stock: null });
}

function removeVariant(index) {
  form.value.variants.splice(index, 1);
}

async function handleImageUpload(event) {
  const file = event.target.files[0];

  if (!file) {
    return;
  }

  uploading.value = true;

  try {
    const response = await uploadProductImage(file);
    const imageUrl = response.data;

    form.value.images.push({
      imageUrl,
      isPrimary: form.value.images.length === 0,
    });
  } catch (error) {
    alert("圖片上傳失敗");
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

  form.value.categoryId = detail.categoryId;
  form.value.brandId = detail.brandId;
  form.value.productName = detail.productName;
  form.value.description = detail.description;

  form.value.variants = detail.variants.map((v) => ({
    variantId: v.variantId,
    size: v.size,
    color: v.color,
    price: v.price,
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

  if (form.value.variants.length === 0) {
    errorMessage.value = "至少要有一筆規格";
    return;
  }

  const payload = {
    categoryId: form.value.categoryId,
    brandId: form.value.brandId,
    productName: form.value.productName,
    description: form.value.description,
    variants: form.value.variants.map((v) => ({
      variantId: v.variantId,
      size: v.size,
      color: v.color,
      price: v.price,
      stock: v.stock,
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
  } else {
    addVariant();
  }
});
</script>

<template>
  <div class="container py-5">
    <h1 class="h2 mb-4">{{ isEditMode ? "編輯商品" : "新增商品" }}</h1>

    <div class="card shadow-sm">
      <div class="card-body">
        <div class="row g-3">
          <div class="col-md-6">
            <label class="form-label">分類 *</label>
            <select v-model="form.categoryId" class="form-select">
              <option :value="null" disabled>請選擇分類</option>
              <option :value="1">籃球鞋</option>
              <option :value="2">慢跑鞋</option>
              <option :value="3">休閒鞋</option>
              <option :value="4">滑板鞋</option>
            </select>
          </div>

          <div class="col-md-6">
            <label class="form-label">品牌 *</label>
            <select v-model="form.brandId" class="form-select" :disabled="loadingBrands">
              <option :value="null" disabled>請選擇品牌</option>
              <option v-for="brand in brands" :key="brand.brandId" :value="brand.brandId">
                {{ brand.brandName }}
              </option>
            </select>
          </div>

          <div class="col-12">
            <label class="form-label">商品名稱 *</label>
            <input v-model="form.productName" type="text" class="form-control" placeholder="例如：Air Jordan 1 High" />
          </div>

          <div class="col-12">
            <label class="form-label">商品介紹</label>
            <textarea v-model="form.description" class="form-control" rows="4"></textarea>
          </div>
        </div>
      </div>
    </div>

    <div class="card shadow-sm mt-4">
      <div class="card-body">
        <div class="d-flex justify-content-between align-items-center mb-3">
          <h5 class="mb-0">規格</h5>
          <button type="button" class="btn btn-sm btn-outline-dark" @click="addVariant">+ 新增規格</button>
        </div>

        <div v-if="form.variants.length === 0" class="text-secondary small">尚未新增任何規格，請至少新增一筆</div>

        <div v-for="(variant, index) in form.variants" :key="index" class="row g-2 align-items-end mb-2">
          <div class="col-6 col-md-2">
            <label class="form-label small">尺寸</label>
            <input v-model="variant.size" type="text" class="form-control form-control-sm" placeholder="US 9" />
          </div>

          <div class="col-6 col-md-2">
            <label class="form-label small">顏色</label>
            <input v-model="variant.color" type="text" class="form-control form-control-sm" placeholder="黑" />
          </div>

          <div class="col-6 col-md-3">
            <label class="form-label small">價格</label>
            <input v-model.number="variant.price" type="number" min="0" class="form-control form-control-sm" />
          </div>

          <div class="col-6 col-md-3">
            <label class="form-label small">庫存</label>
            <input v-model.number="variant.stock" type="number" min="0" class="form-control form-control-sm" />
          </div>

          <div class="col-12 col-md-2">
            <button type="button" class="btn btn-sm btn-outline-danger w-100" @click="removeVariant(index)">刪除</button>
          </div>
        </div>
      </div>
    </div>

    <div class="card shadow-sm mt-4">
      <div class="card-body">
        <div class="d-flex justify-content-between align-items-center mb-3">
          <h5 class="mb-0">圖片</h5>

          <div>
            <input ref="fileInput" type="file" accept="image/*" class="d-none" @change="handleImageUpload" />
            <button type="button" class="btn btn-sm btn-outline-dark" :disabled="uploading" @click="triggerFileInput">
              {{ uploading ? "上傳中..." : "+ 上傳圖片" }}
            </button>
          </div>
        </div>

        <div v-if="form.images.length === 0" class="text-secondary small">尚未上傳任何圖片</div>

        <div v-else class="row g-3">
          <div v-for="(image, index) in form.images" :key="image.imageUrl" class="col-6 col-md-3">
            <img :src="resolveProductImageUrl(image.imageUrl)" class="image-preview" />

            <div class="d-flex justify-content-between align-items-center mt-2">
              <button
                type="button"
                class="btn btn-sm"
                :class="image.isPrimary ? 'btn-dark' : 'btn-outline-dark'"
                @click="setPrimaryImage(index)"
              >
                {{ image.isPrimary ? "主圖" : "設為主圖" }}
              </button>

              <button type="button" class="btn btn-sm btn-outline-danger" @click="removeImage(index)">刪除</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="errorMessage" class="alert alert-danger mt-4">{{ errorMessage }}</div>

    <div class="d-flex justify-content-end gap-2 mt-4">
      <RouterLink :to="{ name: 'admin-products' }" class="btn btn-outline-secondary">取消</RouterLink>
      <button type="button" class="btn btn-dark" :disabled="submitting" @click="handleSubmit">
        {{ submitting ? "儲存中..." : (isEditMode ? "儲存修改" : "確認新增") }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.image-preview {
  width: 100%;
  height: 120px;
  object-fit: cover;
  border-radius: 0.4rem;
  border: 1px solid #dee2e6;
}
</style>