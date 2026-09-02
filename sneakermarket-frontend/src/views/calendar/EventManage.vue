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
        @click="
          activeTab = 'search';
          fetchEvents(1);
        "
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

                  <div class="mb-3">
                    <label class="form-label">活動圖片</label>
                    <input
                      type="file"
                      class="form-control"
                      accept="image/*"
                      @change="handleFileChange($event, 'create')"
                    />
                    <!-- 圖片預覽 -->
                    <div v-if="photoPreview.create" class="mt-2">
                      <img
                        :src="photoPreview.create"
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
                    <div class="form-text">
                      使用者點擊活動時，可以導向此網址
                    </div>
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

    <!-- 3. 查詢表格 (當 activeTab 為 'search' 時顯示) -->
    <div v-if="activeTab === 'search'" class="card p-4">
      <h2>查詢活動</h2>

      <!-- 完整查詢條件篩選區塊（保留你原本所有的欄位） -->
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

          <!-- 活動狀態 -->
          <div class="col-md-3">
            <label class="form-label fw-bold">活動狀態</label>
            <select v-model="searchForm.status" class="form-select">
              <option value="">全部狀態</option>
              <option value="ACTIVE">啟用</option>
              <option value="INACTIVE">停用</option>
              <option value="DRAFT">草稿</option>
            </select>
          </div>

          <!-- 開始時間起 -->
          <div class="col-md-3">
            <label class="form-label fw-bold">開始時間起</label>
            <input
              v-model="searchForm.startDate"
              type="datetime-local"
              class="form-control"
            />
          </div>

          <!-- 結束時間止 -->
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
            @click="fetchEvents(1)"
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
            <tr v-for="event in eventList" :key="event.eventId">
              <td>{{ event.eventId }}</td>
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
              <td>{{ formatDate(event.startAt) }}</td>
              <td>{{ formatDate(event.endAt) }}</td>
              <td>
                <button
                  class="btn btn-sm btn-outline-info me-1"
                  @click="openDetailModal(event)"
                >
                  查看
                </button>
                <button
                  class="btn btn-sm btn-outline-primary me-1"
                  @click="openEditModal(event)"
                >
                  編輯
                </button>
                <button
                  class="btn btn-sm btn-outline-danger"
                  @click="deleteEvent(event.eventId)"
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

      <!-- 4. Bootstrap 分頁頁碼區塊 -->
      <nav v-if="totalPages > 1" class="d-flex justify-content-center mt-3">
        <ul class="pagination">
          <li :class="['page-item', currentPage === 1 ? 'disabled' : '']">
            <button class="page-link" @click="fetchEvents(currentPage - 1)">
              上一頁
            </button>
          </li>
          <li
            v-for="page in totalPages"
            :key="page"
            :class="['page-item', currentPage === page ? 'active' : '']"
          >
            <button class="page-link" @click="fetchEvents(page)">
              {{ page }}
            </button>
          </li>
          <li
            :class="['page-item', currentPage === totalPages ? 'disabled' : '']"
          >
            <button class="page-link" @click="fetchEvents(currentPage + 1)">
              下一頁
            </button>
          </li>
        </ul>
      </nav>
    </div>

    <!-- 5. 查看詳細資料彈窗 (Modal) -->
    <div
      v-if="showDetailModal"
      class="modal fade show d-block"
      tabindex="-1"
      style="background-color: rgba(0, 0, 0, 0.5); z-index: 1050"
      @click.self="closeDetailModal"
    >
      <div class="modal-dialog modal-dialog-centered modal-lg">
        <div
          class="modal-content shadow-lg border-0 rounded-4 position-relative"
        >
          <!-- 右上角關閉叉叉 (已優化點擊範圍與觸發事件) -->
          <button
            type="button"
            class="btn-close position-absolute top-0 end-0 m-3"
            style="z-index: 1060; cursor: pointer"
            aria-label="Close"
            @click="closeDetailModal"
          ></button>

          <div class="modal-body text-center pt-4 px-4 pb-4">
            <p class="text-uppercase text-muted fw-bold tracking-wider mb-1">
              {{ selectedEvent?.eventType || "EVENT DETAILS" }}
            </p>
            <h2 class="fw-bold mb-4">{{ selectedEvent?.title }}</h2>

            <div
              class="event-image-container mb-4 overflow-hidden rounded-3 shadow-sm"
            >
              <img
                :src="`http://localhost:8080/api/calendar/event/${selectedEvent?.eventId}/photo?t=${Date.now()}`"
                class="img-fluid w-100 object-fit-cover"
                style="max-height: 350px"
                alt="Event Image"
                @error="(e) => (e.target.src = 'https://picsum.photos/800/400')"
              />
            </div>

            <div class="text-start bg-light p-3 rounded-3">
              <!-- <p class="mb-2">
                <strong>ID：</strong> {{ selectedEvent?.eventId }}
              </p> -->
              <p class="mb-2">
                <strong>狀態：</strong> {{ selectedEvent?.status }}
              </p>
              <p class="mb-2">
                <strong>開始時間：</strong>
                {{ formatDate(selectedEvent?.startAt) }}
              </p>
              <p class="mb-2">
                <strong>結束時間：</strong>
                {{ formatDate(selectedEvent?.endAt) }}
              </p>
              <p class="mb-2">
                <strong>描述：</strong>
                {{ selectedEvent?.description || "無詳細描述" }}
              </p>
              <p v-if="selectedEvent?.targetUrl" class="mb-0">
                <strong>連結：</strong>
                <a
                  :href="selectedEvent.targetUrl"
                  target="_blank"
                  rel="noopener"
                  >{{ selectedEvent.targetUrl }}</a
                >
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 6. 編輯活動彈窗 (Modal) -->
    <div
      v-if="showEditModal"
      class="modal fade show d-block"
      tabindex="-1"
      style="background-color: rgba(0, 0, 0, 0.5)"
      @click.self="closeEditModal"
    >
      <div class="modal-dialog modal-dialog-centered modal-lg">
        <div class="modal-content shadow-lg border-0 rounded-4">
          <div class="modal-header">
            <h5 class="modal-title fw-bold">
              編輯活動 (ID: {{ editForm.eventId }})
            </h5>
            <button
              type="button"
              class="btn-close"
              @click="closeEditModal"
            ></button>
          </div>
          <div class="modal-body p-4">
            <form @submit.prevent="submitUpdateForm">
              <div class="mb-3">
                <label class="form-label"
                  >活動標題 <span class="text-danger">*</span></label
                >
                <input
                  v-model.trim="editForm.title"
                  type="text"
                  class="form-control"
                  required
                />
              </div>

              <div class="row">
                <div class="col-md-6 mb-3">
                  <label class="form-label"
                    >活動類型 <span class="text-danger">*</span></label
                  >
                  <select
                    v-model="editForm.eventType"
                    class="form-select"
                    required
                  >
                    <option value="RELEASE">新品發售</option>
                    <option value="SALE">優惠活動</option>
                    <option value="EVENT">品牌活動</option>
                    <option value="NOTICE">公告</option>
                    <option value="OTHER">其他</option>
                  </select>
                </div>

                <div class="col-md-6 mb-3">
                  <label class="form-label"
                    >狀態 <span class="text-danger">*</span></label
                  >
                  <select
                    v-model="editForm.status"
                    class="form-select"
                    required
                  >
                    <option value="ACTIVE">啟用</option>
                    <option value="INACTIVE">停用</option>
                    <option value="DRAFT">草稿</option>
                  </select>
                </div>
              </div>

              <div class="row">
                <div class="col-md-6 mb-3">
                  <label class="form-label"
                    >開始時間 <span class="text-danger">*</span></label
                  >
                  <input
                    v-model="editForm.startAt"
                    type="datetime-local"
                    class="form-control"
                    required
                  />
                </div>

                <div class="col-md-6 mb-3">
                  <label class="form-label">結束時間</label>
                  <input
                    v-model="editForm.endAt"
                    type="datetime-local"
                    class="form-control"
                  />
                </div>
              </div>

              <div class="mb-3">
                <label class="form-label">活動描述</label>
                <textarea
                  v-model.trim="editForm.description"
                  class="form-control"
                  rows="4"
                ></textarea>
              </div>

              <div class="mb-3">
                <label class="form-label">修改圖片</label>
                <input
                  type="file"
                  class="form-control"
                  accept="image/*"
                  @change="handleFileChange($event, 'edit')"
                />
                <!-- 圖片預覽 (若有新選擇看預覽，沒有則看原本的 API 圖片) -->
                <div class="mt-2">
                  <img
                    :src="
                      photoPreview.edit ||
                      `/api/calendar/event/${editForm.eventId}/photo`
                    "
                    style="max-height: 150px"
                    class="img-thumbnail"
                    @error="(e) => (e.target.style.display = 'none')"
                  />
                </div>
              </div>

              <div class="mb-3">
                <label class="form-label">活動連結</label>
                <input
                  v-model.trim="editForm.targetUrl"
                  type="url"
                  class="form-control"
                />
              </div>

              <div class="d-flex justify-content-end gap-2 mt-4">
                <button
                  type="button"
                  class="btn btn-outline-secondary"
                  @click="closeEditModal"
                >
                  取消
                </button>
                <button
                  type="submit"
                  class="btn btn-primary"
                  :disabled="loading"
                >
                  儲存修改
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from "vue";
import axios from "axios";

// 狀態定義
const loading = ref(false);
const errorMessage = ref("");
const successMessage = ref("");
const activeTab = ref(localStorage.getItem("event_active_tab") || "search");

// 新增表單預設值
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

// 查詢條件
const searchForm = reactive({
  title: "",
  eventType: "",
  status: "",
  startDate: "",
  endDate: "",
});

const eventList = ref([]);
const currentPage = ref(1);
const totalPages = ref(1);

// Modal 控制狀態
const showDetailModal = ref(false);
const showEditModal = ref(false);
const selectedEvent = ref(null);

// 編輯表單
const editForm = reactive({
  eventId: null,
  title: "",
  eventType: "",
  status: "",
  brandId: null,
  createdBy: null,
  startAt: "",
  endAt: "",
  description: "",
  targetUrl: "",
  sourceType: "",
  sourceId: null,
});

// 新增事件提交
const submitForm = async () => {
  loading.value = true;
  errorMessage.value = "";
  successMessage.value = "";

  const payload = {
    ...form,
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
      error.response?.data?.message || "新增活動失敗，請檢查輸入內容。";
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  Object.assign(form, initialFormState);
  photoPreview.create = null;
  errorMessage.value = "";
};

// 重設查詢
const resetSearch = () => {
  searchForm.title = "";
  searchForm.eventType = "";
  searchForm.status = "";
  searchForm.startDate = "";
  searchForm.endDate = "";
  fetchEvents(1);
};

// 修正後的 查詢列表 API 對接
const fetchEvents = async (page = 1) => {
  console.log("開始發送查詢請求，頁碼：", page); // 調試日誌
  try {
    // 嚴格過濾掉未填寫的空參數，避免傳遞空字串或 null 導致 API 異常
    const params = {
      pageNumber: page,
    };

    if (searchForm.title && searchForm.title.trim()) {
      params.title = searchForm.title.trim();
    }
    if (searchForm.eventType) {
      params.eventType = searchForm.eventType;
    }
    if (searchForm.status) {
      params.status = searchForm.status; // 後端接收名稱為 status
    }
    if (searchForm.startDate) {
      params.startAt =
        searchForm.startDate.length === 16
          ? `${searchForm.startDate}:00`
          : searchForm.startDate;
    }
    if (searchForm.endDate) {
      params.endAt =
        searchForm.endDate.length === 16
          ? `${searchForm.endDate}:00`
          : searchForm.endDate;
    }
    console.log("👉 送出給後端的參數：", params);

    const response = await axios.get("/api/calendar/eventPage", { params });

    console.log("後端回傳資料：", response.data); // 調試日誌
    eventList.value = response.data.content || [];
    totalPages.value = response.data.totalPages || 1;
    currentPage.value = page;
  } catch (error) {
    console.error("查詢失敗，詳細 Error:", error);
  }
};

// 查看 Modal 操作
const openDetailModal = (event) => {
  selectedEvent.value = event;
  showDetailModal.value = true;
};

const closeDetailModal = () => {
  showDetailModal.value = false;
  selectedEvent.value = null;
};

// 切換至編輯 Modal
const switchToEditModal = () => {
  if (selectedEvent.value) {
    const eventToEdit = selectedEvent.value;
    closeDetailModal();
    openEditModal(eventToEdit);
  }
};

// 開啟編輯 Modal 並填入既有資料
const openEditModal = (event) => {
  photoPreview.edit = null; // 清空舊的編輯預覽
  Object.assign(editForm, {
    ...event,
    photo: null, // 先設 null，有重新選檔才覆蓋
    startAt: formatForDateTimeInput(event.startAt),
    endAt: formatForDateTimeInput(event.endAt),
  });
  showEditModal.value = true;
};

const closeEditModal = () => {
  showEditModal.value = false;
};

// 送出編輯
const submitUpdateForm = async () => {
  loading.value = true;
  try {
    await axios.put(`/api/calendar/eventUpdate/${editForm.eventId}`, editForm);
    alert("更新成功！");
    closeEditModal();
    fetchEvents(currentPage.value);
  } catch (error) {
    console.error("更新失敗", error);
    alert("更新失敗，請檢查伺服器狀態。");
  } finally {
    loading.value = false;
  }
};

// 刪除資料
const deleteEvent = async (eventId) => {
  if (!confirm(`確定要刪除 ID: ${eventId} 的活動嗎？`)) return;

  try {
    await axios.delete("/api/calendar/eventDelete", {
      params: { eventId },
    });
    alert("刪除成功！");
    fetchEvents(currentPage.value);
  } catch (error) {
    console.error("刪除失敗", error);
    alert("刪除失敗！");
  }
};

// 時間轉字串工具
const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  return new Date(dateStr).toLocaleString("zh-TW", { hour12: false });
};

// 格式化為 <input type="datetime-local"> 吃的格式
const formatForDateTimeInput = (dateStr) => {
  if (!dateStr) return "";
  const d = new Date(dateStr);
  const pad = (num) => String(num).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(
    d.getHours(),
  )}:${pad(d.getMinutes())}`;
};

// 監聽 activeTab 變更，存入 localStorage
watch(activeTab, (newTab) => {
  localStorage.setItem("event_active_tab", newTab);
});

onMounted(() => {
  if (activeTab.value === "search") {
    fetchEvents(1);
  }
});

// 1. 新增預覽圖狀態
const photoPreview = reactive({
  create: null,
  edit: null,
});

// 2. 處理檔案讀取並轉為 Base64 (傳給後端的 byte[])
const handleFileChange = (event, mode) => {
  const file = event.target.files[0];
  if (!file) return;

  const reader = new FileReader();
  reader.readAsDataURL(file); // 轉為 Data URL
  reader.onload = () => {
    // reader.result 格式為 "data:image/jpeg;base64,/9j/4AAQSkZJRg..."
    // 需要把前綴 "data:image/xxx;base64," 擦掉，純留 Base64 內容給後端 byte[]
    const base64String = reader.result.split(",")[1];

    if (mode === "create") {
      form.photo = base64String;
      photoPreview.create = reader.result; // 用原本的 Data URL 做 preview
    } else {
      editForm.photo = base64String;
      photoPreview.edit = reader.result;
    }
  };
};
</script>
