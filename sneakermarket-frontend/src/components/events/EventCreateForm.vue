<!-- 新增活動表單 -->
<template>
  <div class="card p-4">
    <div class="container py-4">
      <div class="row justify-content-center">
        <div class="col-lg-9 col-xl-8">
          <div class="mb-4">
            <h2 class="fw-bold mb-1">新增活動</h2>
            <p class="text-muted mb-0">建立新的行事曆活動資訊</p>
          </div>

          <div class="card shadow-sm border-0">
            <div class="card-body p-4">
              <form @submit.prevent="submitForm">
                <div class="mb-3">
                  <label class="form-label">
                    活動標題 <span class="text-danger">*</span>
                  </label>
                  <input
                    v-model.trim="form.title"
                    type="text"
                    class="form-control"
                    placeholder="例如：Nike Air Jordan 新品發售"
                    maxlength="150"
                    required
                  />
                  <div class="form-text">最多 150 個字</div>
                </div>

                <div class="row">
                  <div class="col-md-6 mb-3">
                    <label class="form-label">
                      活動類型 <span class="text-danger">*</span>
                    </label>
                    <select
                      v-model="form.eventType"
                      class="form-select"
                      required
                    >
                      <option disabled value="">請選擇活動類型</option>
                      <option value="RELEASE">新品發售</option>
                      <option value="SALE">優惠活動</option>
                      <option value="EVENT">品牌活動</option>
                      <option value="NOTICE">公告</option>
                      <option value="OTHER">其他</option>
                    </select>
                  </div>

                  <div class="col-md-6 mb-3">
                    <label class="form-label">
                      狀態 <span class="text-danger">*</span>
                    </label>
                    <select v-model="form.status" class="form-select" required>
                      <option value="ACTIVE">啟用</option>
                      <option value="INACTIVE">停用</option>
                      <option value="DRAFT">草稿</option>
                    </select>
                  </div>
                </div>

                <div class="row">
                  <div class="col-md-6 mb-3">
                    <label class="form-label">Brand ID</label>
                    <input
                      v-model.number="form.brandId"
                      type="number"
                      class="form-control"
                      min="1"
                      placeholder="例如：1"
                    />
                  </div>

                  <div class="col-md-6 mb-3">
                    <label class="form-label">Created By</label>
                    <input
                      v-model.number="form.createdBy"
                      type="number"
                      class="form-control"
                      min="1"
                      placeholder="例如：1001"
                    />
                  </div>
                </div>

                <div class="row">
                  <div class="col-md-6 mb-3">
                    <label class="form-label">
                      開始時間 <span class="text-danger">*</span>
                    </label>
                    <input
                      v-model="form.startAt"
                      type="datetime-local"
                      class="form-control"
                      required
                    />
                  </div>

                  <div class="col-md-6 mb-3">
                    <label class="form-label">結束時間</label>
                    <input
                      v-model="form.endAt"
                      type="datetime-local"
                      class="form-control"
                    />
                  </div>
                </div>

                <div class="mb-3">
                  <label class="form-label">活動描述</label>
                  <textarea
                    v-model.trim="form.description"
                    class="form-control"
                    rows="5"
                    placeholder="請輸入活動詳細內容..."
                  ></textarea>
                </div>
                <div class="border rounded-3 p-3 mb-3">
                  <h5 class="fw-bold mb-3">活動地點</h5>

                  <div class="mb-3">
                    <label class="form-label">場地名稱</label>
                    <input
                      v-model.trim="form.venueName"
                      type="text"
                      class="form-control"
                      maxlength="150"
                      placeholder="例如：台北流行音樂中心"
                    />
                  </div>

                  <div class="mb-3">
                    <label class="form-label">活動地址</label>
                    <input
                      v-model.trim="form.address"
                      type="text"
                      class="form-control"
                      maxlength="300"
                      placeholder="例如：台北市南港區市民大道八段99號"
                    />
                  </div>

                  <LocationPickerMap
                    :venue-name="form.venueName"
                    :address="form.address"
                    v-model:latitude="form.latitude"
                    v-model:longitude="form.longitude"
                  />

                  <div class="form-text">
                    有完整地址時請按「依地址定位」；沒有詳細地址時，可直接點擊地圖標記。
                  </div>
                </div>

                <div class="mb-3">
                  <label class="form-label">活動圖片</label>
                  <input
                    type="file"
                    class="form-control"
                    accept="image/*"
                    @change="handleFileChange"
                  />
                  <div v-if="photoPreview" class="mt-2">
                    <img
                      :src="photoPreview"
                      style="max-height: 150px"
                      class="img-thumbnail"
                    />
                  </div>
                </div>

                <div class="mb-3">
                  <label class="form-label">活動連結</label>
                  <input
                    v-model.trim="form.targetUrl"
                    type="url"
                    class="form-control"
                    placeholder="https://example.com"
                    maxlength="500"
                  />
                  <div class="form-text">使用者點擊活動時，可以導向此網址</div>
                </div>

                <div class="row">
                  <div class="col-md-6 mb-3">
                    <label class="form-label">
                      Source Type <span class="text-danger">*</span>
                    </label>
                    <select
                      v-model="form.sourceType"
                      class="form-select"
                      required
                    >
                      <option disabled value="">請選擇來源</option>
                      <option value="MANUAL">手動建立</option>
                      <option value="PRODUCT">商品</option>
                      <option value="BRAND">品牌</option>
                      <option value="SYSTEM">系統</option>
                    </select>
                  </div>

                  <div class="col-md-6 mb-3">
                    <label class="form-label">Source ID</label>
                    <input
                      v-model.number="form.sourceId"
                      type="number"
                      class="form-control"
                      min="1"
                      placeholder="例如：123"
                    />
                  </div>
                </div>

                <div v-if="errorMessage" class="alert alert-danger">
                  {{ errorMessage }}
                </div>
                <div v-if="successMessage" class="alert alert-success">
                  {{ successMessage }}
                </div>

                <hr class="my-4" />

                <div class="d-flex justify-content-end gap-2">
                  <button
                    type="button"
                    class="btn btn-outline-secondary"
                    @click="resetForm"
                    :disabled="loading"
                  >
                    清除
                  </button>

                  <button
                    type="submit"
                    class="btn btn-primary px-4"
                    :disabled="loading"
                  >
                    <span
                      v-if="loading"
                      class="spinner-border spinner-border-sm me-2"
                    ></span>
                    {{ loading ? "新增中..." : "新增活動" }}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import { createEvent } from "@/service/eventService";
import LocationPickerMap from "@/components/events/LocationPickerMap.vue";

const emit = defineEmits(["success"]);
const loading = ref(false);
const errorMessage = ref("");
const successMessage = ref("");
const photoPreview = ref(null);

const initialFormState = {
  title: "",
  eventType: "",
  status: "ACTIVE",
  brandId: null,
  createdBy: null,
  startAt: "",
  endAt: null,
  description: "",
  venueName: "",
  address: "",
  latitude: null,
  longitude: null,
  targetUrl: "",
  sourceType: "",
  sourceId: null,
};

const form = reactive({ ...initialFormState });

const handleFileChange = (event) => {
  const file = event.target.files[0];
  if (!file) return;

  const reader = new FileReader();
  reader.readAsDataURL(file);
  reader.onload = () => {
    form.photo = reader.result.split(",")[1];
    photoPreview.value = reader.result;
  };
};

const normalizeOptionalNumber = (value) => {
  if (value === null || value === undefined || value === "") {
    return null;
  }

  const number = Number(value);

  return Number.isFinite(number) ? number : null;
};

const submitForm = async () => {
  errorMessage.value = "";
  successMessage.value = "";

  const latitude = normalizeOptionalNumber(form.latitude);

  const longitude = normalizeOptionalNumber(form.longitude);

  if ((latitude === null) !== (longitude === null)) {
    errorMessage.value = "緯度與經度必須同時填寫。";

    return;
  }

  if (latitude !== null && (latitude < -90 || latitude > 90)) {
    errorMessage.value = "緯度必須介於 -90 到 90。";

    return;
  }

  if (longitude !== null && (longitude < -180 || longitude > 180)) {
    errorMessage.value = "經度必須介於 -180 到 180。";

    return;
  }

  loading.value = true;

  const payload = {
    ...form,
    brandId: form.brandId || null,
    createdBy: form.createdBy || null,
    sourceId: form.sourceId || null,
    endAt: form.endAt || null,
    targetUrl: form.targetUrl || null,
    description: form.description || null,

    venueName: form.venueName || null,

    address: form.address || null,

    latitude,
    longitude,
  };

  try {
    const response = await createEvent(payload);
    if (response.status === 201) {
      successMessage.value = "活動新增成功！";
      resetForm();
      emit("success", response.data);
    }
  } catch (error) {
    console.error("新增失敗：", error);
    errorMessage.value =
      error.response?.data?.message || "新增活動失敗，請檢查輸入內容。";
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  Object.assign(form, initialFormState);
  photoPreview.value = null;
  errorMessage.value = "";
};
</script>
