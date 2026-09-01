<template>
  <div class="container navbar-space">
    <!-- 1. 上方切換按鈕區 -->
    <div class="tab-buttons mb-4">
      <button
        :class="[
          'btn',
          activeTab === 'create' ? 'btn-primary' : 'btn-outline-primary',
        ]"
        @click="activeTab = 'create'"
      >
        新增活動
      </button>
      <button
        :class="[
          'btn',
          activeTab === 'search' ? 'btn-primary' : 'btn-outline-primary',
          'ms-2',
        ]"
        @click="activeTab = 'search'"
      >
        查詢活動
      </button>
    </div>

    <!-- 2. 新增表單 (當 activeTab 為 'create' 時顯示) -->
    <div v-if="activeTab === 'create'" class="card p-4">
      <h2>新增活動</h2>
      <div class="container py-4">
        <div class="row justify-content-center">
          <div class="col-lg-9 col-xl-8">
            <!-- 標題 -->

            <div class="mb-4">
              <h2 class="fw-bold mb-1">新增活動</h2>

              <p class="text-muted mb-0">建立新的行事曆活動資訊</p>
            </div>

            <!-- 表單卡片 -->

            <div class="card shadow-sm border-0">
              <div class="card-body p-4">
                <form @submit.prevent="submitForm">
                  <!-- 活動標題 -->

                  <div class="mb-3">
                    <label class="form-label">
                      活動標題

                      <span class="text-danger">*</span>
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

                  <!-- 活動類型 + 狀態 -->

                  <div class="row">
                    <div class="col-md-6 mb-3">
                      <label class="form-label">
                        活動類型

                        <span class="text-danger">*</span>
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
                        狀態

                        <span class="text-danger">*</span>
                      </label>

                      <select
                        v-model="form.status"
                        class="form-select"
                        required
                      >
                        <option value="ACTIVE">啟用</option>

                        <option value="INACTIVE">停用</option>

                        <option value="DRAFT">草稿</option>
                      </select>
                    </div>
                  </div>

                  <!-- 品牌 ID + 建立者 -->

                  <div class="row">
                    <div class="col-md-6 mb-3">
                      <label class="form-label"> Brand ID </label>

                      <input
                        v-model.number="form.brandId"
                        type="number"
                        class="form-control"
                        min="1"
                        placeholder="例如：1"
                      />
                    </div>

                    <div class="col-md-6 mb-3">
                      <label class="form-label"> Created By </label>

                      <input
                        v-model.number="form.createdBy"
                        type="number"
                        class="form-control"
                        min="1"
                        placeholder="例如：1001"
                      />
                    </div>
                  </div>

                  <!-- 開始時間 -->

                  <div class="row">
                    <div class="col-md-6 mb-3">
                      <label class="form-label">
                        開始時間

                        <span class="text-danger">*</span>
                      </label>

                      <input
                        v-model="form.startAt"
                        type="datetime-local"
                        class="form-control"
                        required
                      />
                    </div>

                    <div class="col-md-6 mb-3">
                      <label class="form-label"> 結束時間 </label>

                      <input
                        v-model="form.endAt"
                        type="datetime-local"
                        class="form-control"
                      />
                    </div>
                  </div>

                  <!-- 活動描述 -->

                  <div class="mb-3">
                    <label class="form-label"> 活動描述 </label>

                    <textarea
                      v-model.trim="form.description"
                      class="form-control"
                      rows="5"
                      placeholder="請輸入活動詳細內容..."
                    ></textarea>
                  </div>

                  <!-- Target URL -->

                  <div class="mb-3">
                    <label class="form-label"> 活動連結 </label>

                    <input
                      v-model.trim="form.targetUrl"
                      type="url"
                      class="form-control"
                      placeholder="https://example.com"
                      maxlength="500"
                    />

                    <div class="form-text">
                      使用者點擊活動時，可以導向此網址
                    </div>
                  </div>

                  <!-- Source -->

                  <div class="row">
                    <div class="col-md-6 mb-3">
                      <label class="form-label">
                        Source Type

                        <span class="text-danger">*</span>
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
                      <label class="form-label"> Source ID </label>

                      <input
                        v-model.number="form.sourceId"
                        type="number"
                        class="form-control"
                        min="1"
                        placeholder="例如：123"
                      />
                    </div>
                  </div>

                  <!-- 錯誤訊息 -->

                  <div v-if="errorMessage" class="alert alert-danger">
                    {{ errorMessage }}
                  </div>

                  <!-- 成功訊息 -->

                  <div v-if="successMessage" class="alert alert-success">
                    {{ successMessage }}
                  </div>

                  <hr class="my-4" />

                  <!-- Button -->

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

    <!-- 3. 查詢表格 (當 activeTab 為 'search' 時顯示) -->
    <div v-if="activeTab === 'search'" class="card p-4">
      <h2>查詢活動</h2>

      <!-- 查詢條件篩選區塊 -->
      <div class="card bg-light border-0 p-3 mb-4">
        <div class="row g-3">
          <!-- 標題模糊搜尋 -->
          <div class="col-md-3">
            <label class="form-label fw-bold">活動標題</label>
            <input
              v-model="searchForm.title"
              type="text"
              class="form-control"
              placeholder="搜尋標題關鍵字..."
            />
          </div>

          <!-- 活動類型下拉選單 -->
          <div class="col-md-3">
            <label class="form-label fw-bold">活動類型</label>
            <select v-model="searchForm.eventType" class="form-select">
              <option value="">全部類型</option>
              <option value="RELEASE">新品發售</option>
              <option value="SALE">優惠活動</option>
              <option value="EVENT">品牌活動</option>
              <option value="NOTICE">公告</option>
              <option value="OTHER">其他</option>
            </select>
          </div>

          <div class="col-md-3">
            <label class="form-label fw-bold">活動狀態</label>
            <select v-model="searchForm.eventStatus" class="form-select">
              <option value="">全部狀態</option>
              <option value="ACTIVE">啟用</option>
              <option value="INACTIVE">停用</option>
              <option value="DRAFT">草稿</option>
            </select>
          </div>

          <!-- 開始時間 -->
          <div class="col-md-3">
            <label class="form-label fw-bold">開始時間起</label>
            <input
              v-model="searchForm.startDate"
              type="datetime-local"
              class="form-control"
            />
          </div>

          <!-- 結束時間 -->
          <div class="col-md-3">
            <label class="form-label fw-bold">結束時間止</label>
            <input
              v-model="searchForm.endDate"
              type="datetime-local"
              class="form-control"
            />
          </div>
        </div>

        <!-- 按鈕區 -->
        <div class="d-flex justify-content-end gap-2 mt-3">
          <button
            type="button"
            class="btn btn-outline-secondary"
            @click="resetSearch"
          >
            重設
          </button>
          <button
            type="button"
            class="btn btn-primary px-4"
            @click="fetchEvents"
          >
            <i class="bi bi-search"></i> 查詢
          </button>
        </div>
      </div>

      <!-- 資料列表表格 -->
      <div class="table-responsive">
        <table class="table table-hover align-middle">
          <thead class="table-light">
            <tr>
              <th>ID</th>
              <th>活動標題</th>
              <th>類型</th>
              <th>狀態</th>
              <th>開始時間</th>
              <th>結束時間</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="event in eventList" :key="event.id">
              <td>{{ event.id }}</td>
              <td class="fw-bold">{{ event.title }}</td>
              <td>
                <span class="badge bg-info text-dark">{{
                  event.eventType
                }}</span>
              </td>
              <td>
                <span
                  :class="[
                    'badge',
                    event.status === 'ACTIVE' ? 'bg-success' : 'bg-secondary',
                  ]"
                >
                  {{ event.status }}
                </span>
              </td>
              <td>{{ event.startAt }}</td>
              <td>{{ event.endAt || "-" }}</td>
              <td>
                <button
                  class="btn btn-sm btn-outline-primary me-1"
                  @click="editEvent(event)"
                >
                  編輯
                </button>
                <button
                  class="btn btn-sm btn-outline-danger"
                  @click="deleteEvent(event.id)"
                >
                  刪除
                </button>
              </td>
            </tr>

            <!-- 無資料提示 -->
            <tr v-if="eventList.length === 0">
              <td colspan="7" class="text-center text-muted py-4">
                查無相關活動資料
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <table class="table table-striped mt-3">
      <thead>
        <tr>
          <th>ID</th>
          <th>活動標題</th>
          <th>類型</th>
          <th>開始時間</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <!-- 使用 v-for 渲染列表資料 -->
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import axios from "axios";

// 狀態定義
const loading = ref(false);
const errorMessage = ref("");
const successMessage = ref("");
const activeTab = ref("create" || "search");

// 表單預設值
const initialFormState = {
  title: "",
  eventType: "",
  status: "ACTIVE",
  brandId: null,
  createdBy: null,
  startAt: "",
  endAt: null,
  description: "",
  targetUrl: "",
  sourceType: "",
  sourceId: null,
};

const form = reactive({ ...initialFormState });

// 送出表單 (對應 POST /api/calendar/eventCreate)
const submitForm = async () => {
  loading.value = true;
  errorMessage.value = "";
  successMessage.value = "";

  // 整理發送到後端的 Payload
  const payload = {
    ...form,
    // 若為空字串，將其轉為 null，避免後端轉型問題
    brandId: form.brandId || null,
    createdBy: form.createdBy || null,
    sourceId: form.sourceId || null,
    endAt: form.endAt || null,
    targetUrl: form.targetUrl || null,
    description: form.description || null,
  };

  try {
    const response = await axios.post("/api/calendar/eventCreate", payload);

    if (response.status === 201) {
      successMessage.value = "活動新增成功！";
      resetForm();
    }
  } catch (error) {
    console.error("新增失敗：", error);
    errorMessage.value =
      error.response?.data?.message ||
      "新增活動失敗，請檢查輸入內容或伺服器狀態。";
  } finally {
    loading.value = false;
  }
};

// 重置表單
const resetForm = () => {
  Object.assign(form, initialFormState);
  errorMessage.value = "";
};

const searchForm = reactive({
  title: "",
  eventType: "",
  eventStatus: "",
  startDate: "",
  endDate: "",
});

// 查詢結果列表
const eventList = ref([]);

// 重設查詢條件
const resetSearch = () => {
  searchForm.title = "";
  searchForm.eventType = "";
  searchForm.startDate = "";
  searchForm.endDate = "";
  searchForm.eventStatus = "";
  fetchEvents();
};

// 發送查詢請求至 Spring Boot 後端
const fetchEvents = async () => {
  try {
    const response = await axios.get("/api/calendar/events", {
      params: {
        title: searchForm.title || undefined,
        eventType: searchForm.eventType || undefined,
        eventStatus: searchForm.eventStatus || undefined,
        startDate: searchForm.startDate || undefined,
        endDate: searchForm.endDate || undefined,
      },
    });
    eventList.value = response.data;
  } catch (error) {
    console.error("查詢失敗:", error);
  }
};
</script>
<style></style>
