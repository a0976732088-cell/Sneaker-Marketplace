<template>
  <div class="container py-4">
    <h1 class="mb-4">建立競標</h1>

    <!-- API / 表單錯誤 -->
    <div v-if="errorMessage" class="alert alert-danger">
      {{ errorMessage }}
    </div>

    <p v-if="!ready && !errorMessage">正在確認登入與載入選項...</p>
    <form v-if="ready" @submit.prevent="submitAuction">
      <!-- 商品基本資料 -->
      <div class="card mb-4">
        <div class="card-header">商品基本資料</div>

        <div class="card-body">
          <div class="row g-3">
            <!-- 品牌 -->
            <div class="col-md-6">
              <label class="form-label"> 品牌 </label>

              <select
                v-model.number="form.brandId"
                class="form-select"
                required
              >
                <option disabled value="">請選擇品牌</option>

                <option
                  v-for="brand in brands"
                  :key="brand.id"
                  :value="brand.id"
                >
                  {{ brand.name }}
                </option>
              </select>
            </div>

            <!-- 分類 -->
            <div class="col-md-6">
              <label class="form-label"> 分類 </label>

              <select
                v-model.number="form.categoryId"
                class="form-select"
                required
              >
                <option disabled value="">請選擇分類</option>

                <option
                  v-for="category in categories"
                  :key="category.id"
                  :value="category.id"
                >
                  {{ category.name }}
                </option>
              </select>
            </div>

            <!-- 商品名稱 -->
            <div class="col-md-8">
              <label class="form-label"> 商品名稱 </label>

              <input
                v-model.trim="form.productName"
                type="text"
                maxlength="150"
                class="form-control"
                required
              />
            </div>

            <!-- 型號 -->
            <div class="col-md-4">
              <label class="form-label"> 型號／系列 </label>

              <input
                v-model.trim="form.model"
                type="text"
                maxlength="100"
                class="form-control"
              />
            </div>

            <!-- 尺寸 -->
            <div class="col-md-6">
              <label class="form-label"> 尺寸 </label>

              <input
                v-model.trim="form.size"
                type="text"
                maxlength="20"
                class="form-control"
                placeholder="例如：US 9"
                required
              />
            </div>

            <!-- 商品狀況 -->
            <div class="col-md-6">
              <label class="form-label"> 商品狀況 </label>

              <select v-model="form.condition" class="form-select" required>
                <option disabled value="">請選擇商品狀況</option>

                <option value="NEW">全新</option>

                <option value="LIKE_NEW">近全新</option>

                <option value="USED">二手</option>
              </select>
            </div>

            <!-- 商品介紹 -->
            <div class="col-12">
              <label class="form-label"> 商品介紹 </label>

              <textarea
                v-model.trim="form.description"
                class="form-control"
                rows="4"
                required
              ></textarea>
            </div>

            <!-- 瑕疵說明 -->
            <div class="col-md-6">
              <label class="form-label"> 瑕疵說明 </label>

              <textarea
                v-model.trim="form.defectNote"
                class="form-control"
                rows="3"
                maxlength="1000"
              ></textarea>
            </div>

            <!-- 真偽來源 -->
            <div class="col-md-6">
              <label class="form-label"> 真偽／來源說明 </label>

              <textarea
                v-model.trim="form.authenticityNote"
                class="form-control"
                rows="3"
                maxlength="1000"
              ></textarea>
            </div>

            <!-- 商品所在地 -->
            <div class="col-md-6">
              <label class="form-label"> 商品所在地 </label>

              <input
                v-model.trim="form.location"
                type="text"
                maxlength="100"
                class="form-control"
                placeholder="例如：台北市"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- 圖片 -->
      <div class="card mb-4">
        <div class="card-header">商品圖片</div>

        <div class="card-body">
          <label class="form-label"> 選擇圖片 </label>

          <input
            type="file"
            class="form-control"
            accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
            multiple
            @change="handleImageChange"
          />

          <div class="form-text">
            至少 1 張，最多 5 張；支援 JPG、JPEG、PNG、WEBP。
          </div>

          <div v-if="images.length > 0" class="mt-3">
            已選擇 {{ images.length }} 張圖片：

            <ul class="mb-0">
              <li
                v-for="(image, index) in images"
                :key="`${image.name}-${index}`"
              >
                {{ image.name }}
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 交易資訊 -->
      <div class="card mb-4">
        <div class="card-header">交易資訊</div>

        <div class="card-body">
          <div class="row g-3">
            <!-- 運送方式 -->
            <div class="col-md-6">
              <label class="form-label"> 運送方式 </label>

              <input
                v-model.trim="form.shippingMethod"
                type="text"
                maxlength="50"
                class="form-control"
                placeholder="例如：宅配"
                required
              />
            </div>

            <!-- 運費 -->
            <div class="col-md-6">
              <label class="form-label"> 運費 </label>

              <input
                v-model.number="form.shippingFee"
                type="number"
                min="0"
                step="0.01"
                class="form-control"
                required
              />
            </div>

            <!-- 付款方式 -->
            <div class="col-md-6">
              <label class="form-label"> 付款方式 </label>

              <input
                v-model.trim="form.paymentMethod"
                type="text"
                maxlength="50"
                class="form-control"
                placeholder="例如：轉帳"
                required
              />
            </div>

            <!-- 保固 -->
            <div class="col-md-6">
              <label class="form-label"> 保固／售後說明 </label>

              <input
                v-model.trim="form.warrantyNote"
                type="text"
                maxlength="500"
                class="form-control"
              />
            </div>

            <!-- 面交 -->
            <div class="col-md-3">
              <div class="form-check mt-2">
                <input
                  id="allowMeetup"
                  v-model="form.allowMeetup"
                  type="checkbox"
                  class="form-check-input"
                />

                <label for="allowMeetup" class="form-check-label">
                  允許面交
                </label>
              </div>
            </div>

            <!-- 退貨 -->
            <div class="col-md-3">
              <div class="form-check mt-2">
                <input
                  id="allowReturn"
                  v-model="form.allowReturn"
                  type="checkbox"
                  class="form-check-input"
                />

                <label for="allowReturn" class="form-check-label">
                  允許退貨
                </label>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 競標設定 -->
      <div class="card mb-4">
        <div class="card-header">競標設定</div>

        <div class="card-body">
          <div class="row g-3">
            <!-- 起標價 -->
            <div class="col-md-6">
              <label class="form-label"> 起標價 </label>

              <input
                v-model.number="form.startPrice"
                type="number"
                min="0.01"
                step="0.01"
                class="form-control"
                required
              />
            </div>

            <!-- 最低加價 -->
            <div class="col-md-6">
              <label class="form-label"> 最低加價金額 </label>

              <input
                v-model.number="form.minIncrement"
                type="number"
                min="0.01"
                step="0.01"
                class="form-control"
                required
              />
            </div>

            <!-- 開始時間 -->
            <div class="col-md-6">
              <label class="form-label"> 開始時間 </label>

              <input
                v-model="form.startAt"
                type="datetime-local" title="台北時間（UTC+8）"
                class="form-control"
                required
              />
            </div>

            <!-- 結束時間 -->
            <div class="col-md-6">
              <label class="form-label"> 結束時間 </label>

              <input
                v-model="form.endAt"
                type="datetime-local" title="台北時間（UTC+8）"
                class="form-control"
                required
              />
            </div>
          </div>
        </div>
      </div>

      <!-- 操作 -->
      <div class="d-flex gap-2">
        <button type="submit" class="btn btn-primary" :disabled="submitting">
          {{ submitting ? "建立中..." : "建立競標" }}
        </button>

        <button
          type="button"
          class="btn btn-outline-secondary"
          :disabled="submitting"
          @click="router.push('/auctions')"
        >
          返回競標列表
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { parseAuctionTime } from "@/utils/auctionTime";
import { getAuctionOptions, requireAuctionSession, handleAuctionAccessError } from "@/service/auctionService";
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";

import { createAuction } from "@/service/auctionService";

// Router
const router = useRouter();

// 是否正在送出建立競標 Request
const submitting = ref(false);
const ready = ref(false);

// 頁面錯誤訊息
const errorMessage = ref("");

// 使用者目前選擇的圖片
const images = ref([]);

const brands = ref([]);
const categories = ref([]);

// 建立競標表單
const form = reactive({
  brandId: "",
  categoryId: "",
  productName: "",
  model: "",
  size: "",
  condition: "",
  description: "",
  defectNote: "",
  authenticityNote: "",
  location: "",

  shippingMethod: "",
  shippingFee: 0,
  paymentMethod: "",
  allowMeetup: false,
  allowReturn: false,
  warrantyNote: "",

  startPrice: null,
  minIncrement: null,
  startAt: "",
  endAt: "",
});

/**
 * 使用者選擇商品圖片。
 */
function handleImageChange(event) {
  errorMessage.value = "";

  const selectedFiles = Array.from(event.target.files ?? []);

  if (selectedFiles.length > 5) {
    images.value = [];

    /*
     * 清掉 input，
     * 避免畫面看起來仍然保留 6 張以上的圖片。
     */
    event.target.value = "";

    errorMessage.value = "競標商品最多只能選擇 5 張圖片";

    return;
  }

  images.value = selectedFiles;
}

/**
 * 建立競標前的簡單前端檢查。
 *
 * 後端仍然是最終驗證者，
 * 前端檢查只用來改善操作體驗。
 */
function validateForm() {
  if (images.value.length === 0) {
    errorMessage.value = "請至少選擇一張商品圖片";

    return false;
  }

  if (!form.startAt || !form.endAt) {
    errorMessage.value = "請設定競標開始與結束時間";

    return false;
  }

  const startAt = parseAuctionTime(form.startAt);

  const endAt = parseAuctionTime(form.endAt);

  if (!Number.isFinite(startAt.getTime()) || !Number.isFinite(endAt.getTime()) || endAt <= startAt) {
    errorMessage.value = "競標結束時間必須晚於開始時間";

    return false;
  }

  if (endAt <= new Date()) {
    errorMessage.value = "競標結束時間必須晚於目前時間";
    return false;
  }
  return true;
}

/**
 * 提交建立競標表單。
 */
async function submitAuction() {
  if (!ready.value || submitting.value) return;
  errorMessage.value = "";

  if (!validateForm()) {
    return;
  }

  submitting.value = true;

  try {
    const response = await createAuction(
      {
        ...form,
      },
      images.value,
    );

    // 後端目前 ApiResponse.data 直接回新建立的 auctionId
    const auctionId = response.data.data;

    // 建立完成後直接進入既有競標詳細頁
    await router.push({
      name: "auction-detail",
      params: {
        id: auctionId,
      },
    });
  } catch (error) {
    if (await handleAuctionAccessError(error, router)) return;
    console.error("建立競標失敗：", error);

    /*
     * 優先顯示後端 GlobalExceptionHandler /
     * ApiResponse 回傳的錯誤訊息。
     */
    errorMessage.value =
      error.response?.data?.message ?? "建立競標失敗，請稍後再試";
  } finally {
    submitting.value = false;
  }
}
onMounted(async () => {
  try {
    await requireAuctionSession();
    const response = await getAuctionOptions();
    brands.value = response.data.data.brands;
    categories.value = response.data.data.categories;
    if (!brands.value.length || !categories.value.length) {
      errorMessage.value = "品牌或分類尚未建立，目前無法刊登競標。";
      return;
    }
    ready.value = true;
  } catch (error) {
    if (await handleAuctionAccessError(error, router)) return;
    errorMessage.value = "無法載入競標選項，請重新整理後再試。";
  }
});
</script>
