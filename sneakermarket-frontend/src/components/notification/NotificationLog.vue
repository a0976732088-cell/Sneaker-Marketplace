<template>
  <div class="container-fluid py-3">
    <!-- 標題區域 -->
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h4 class="fw-bold mb-0">通知發送紀錄</h4>
    </div>

    <!-- 搜尋條件區塊 -->
    <div class="card border-0 shadow-sm mb-4">
      <div class="card-body">
        <div class="row g-3">
          <div class="col-md-4">
            <label class="form-label text-muted small fw-bold">通知標題</label>
            <input
              v-model="searchParams.title"
              type="text"
              class="form-control"
              placeholder="搜尋標題..."
              @keyup.enter="fetchLogs(1)"
            />
          </div>
          <div class="col-md-4">
            <label class="form-label text-muted small fw-bold"
              >發送對象類型</label
            >
            <input
              v-model="searchParams.sendType"
              type="text"
              class="form-control"
              placeholder="例如: ALL"
              @keyup.enter="fetchLogs(1)"
            />
          </div>
          <div class="col-md-4 d-flex align-items-end gap-2">
            <button class="btn btn-primary px-4" @click="fetchLogs(1)">
              搜尋
            </button>
            <button class="btn btn-outline-secondary" @click="resetSearch">
              重設
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 資料表格 -->
    <div class="table-responsive bg-white rounded-3 shadow-sm">
      <table class="table table-hover align-middle mb-0">
        <thead class="table-light">
          <tr>
            <th width="100">發送編號</th>
            <th>通知標題</th>
            <th width="120">對象類型</th>
            <th width="120">關聯類型</th>
            <th width="100">發送總數</th>
            <th width="180">發送時間</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in notificationLogList" :key="item.id">
            <td>#{{ item.id }}</td>
            <td class="fw-bold text-dark">{{ item.title }}</td>
            <td>
              <span class="badge bg-primary text-white">{{
                item.sendType
              }}</span>
            </td>
            <td>
              <span class="badge bg-secondary">{{
                item.targetType || "通用"
              }}</span>
            </td>
            <td>
              <span class="fw-semibold text-success"
                >{{ item.totalSent }} 人</span
              >
            </td>
            <td>{{ formatDate(item.createdAt) }}</td>
          </tr>
          <tr v-if="notificationLogList.length === 0">
            <td colspan="6" class="text-center py-4 text-muted">
              目前無相關發送紀錄
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分頁導覽 -->
    <nav v-if="totalPages > 1" class="d-flex justify-content-center mt-4">
      <ul class="pagination">
        <li :class="['page-item', currentPage === 1 ? 'disabled' : '']">
          <button class="page-link" @click="fetchLogs(currentPage - 1)">
            上一頁
          </button>
        </li>
        <li
          v-for="page in totalPages"
          :key="page"
          :class="['page-item', currentPage === page ? 'active' : '']"
        >
          <button class="page-link" @click="fetchLogs(page)">
            {{ page }}
          </button>
        </li>
        <li
          :class="['page-item', currentPage === totalPages ? 'disabled' : '']"
        >
          <button class="page-link" @click="fetchLogs(currentPage + 1)">
            下一頁
          </button>
        </li>
      </ul>
    </nav>
    <section class="card border-0 shadow-sm mt-4">
  <div class="card-body">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h5 class="mb-0">Email 失敗任務</h5>
      <button class="btn btn-outline-secondary btn-sm" @click="fetchEmailJobs(emailPage)">
        重新整理
      </button>
    </div>

    <p v-if="emailError" class="text-danger">{{ emailError }}</p>

    <div class="table-responsive">
      <table class="table table-hover align-middle">
        <thead>
          <tr>
            <th>任務 ID</th>
            <th>收件信箱</th>
            <th>失敗次數</th>
            <th>錯誤狀態</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="job in emailJobs" :key="job.id">
            <td>{{ job.id }}</td>
            <td>{{ job.recipientEmail }}</td>
            <td>{{ job.retryCount }}</td>
            <td>{{ job.lastError }}</td>
            <td>
              <button
                class="btn btn-outline-primary btn-sm"
                :disabled="retryingId === job.id"
                @click="retryEmail(job)"
              >
                手動重試
              </button>
            </td>
          </tr>
          <tr v-if="emailJobs.length === 0">
            <td colspan="5" class="text-center text-muted">目前沒有失敗任務</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="emailTotalPages > 1" class="d-flex gap-2 justify-content-end">
      <button
        class="btn btn-outline-secondary btn-sm"
        :disabled="emailPage === 0"
        @click="fetchEmailJobs(emailPage - 1)"
      >上一頁</button>
      <span>第 {{ emailPage + 1 }} / {{ emailTotalPages }} 頁</span>
      <button
        class="btn btn-outline-secondary btn-sm"
        :disabled="emailPage + 1 >= emailTotalPages"
        @click="fetchEmailJobs(emailPage + 1)"
      >下一頁</button>
    </div>
  </div>
</section>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import {
  getNotificationLogs,
  getEmailJobs,
  retryEmailJob,
} from "@/service/notificationService";

const currentPage = ref(1);
const totalPages = ref(1);
const notificationLogList = ref([]);

const emailJobs = ref([]);
const emailPage = ref(0);
const emailTotalPages = ref(0);
const emailError = ref("");
const retryingId = ref(null);

const fetchEmailJobs = async (page = 0) => {
  try {
    emailError.value = "";
    const res = await getEmailJobs("FAILED", page);
    emailJobs.value = res.data.content || [];
    emailPage.value = page;
    emailTotalPages.value = res.data.totalPages || 0;
  } catch (error) {
    emailError.value = "讀取 Email 失敗任務失敗";
    console.error(error);
  }
};

const retryEmail = async (job) => {
  if (!confirm(`確定重試寄給 ${job.recipientEmail} 的 Email？`)) return;

  try {
    retryingId.value = job.id;
    await retryEmailJob(job.id);
    await fetchEmailJobs(emailPage.value);
    alert("已重新加入寄送佇列");
  } catch (error) {
    alert(error.response?.data?.message || "重試失敗");
  } finally {
    retryingId.value = null;
  }
};

// 補上搜尋條件變數定義
const searchParams = ref({
  title: "",
  sendType: "",
});

onMounted(() => {
  fetchLogs(1);
  fetchEmailJobs(0);
});

// 🎯 2. 對接後台 Log 查詢 API (/api/notification/logs)
const fetchLogs = async (page = 1) => {
  try {
    const params = {
      pageNumber: page,
      pageSize: 10,
    };

    if (searchParams.value.title?.trim()) {
      params.title = searchParams.value.title.trim();
    }
    if (searchParams.value.sendType?.trim()) {
      params.sendType = searchParams.value.sendType.trim();
    }

    const res = await getNotificationLogs(params);
    notificationLogList.value = res.data.content || [];
    totalPages.value = res.data.totalPages || 1;
    currentPage.value = page;
  } catch (err) {
    console.error("查詢紀錄失敗", err);
  }
};

const resetSearch = () => {
  searchParams.value = {
    title: "",
    sendType: "",
  };
  fetchLogs(1);
};

// 時間格式化
const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  return new Date(dateStr).toLocaleString("zh-TW", { hour12: false });
};
</script>

<style scoped>
.table th {
  font-weight: 600;
  color: #64748b;
}
</style>
