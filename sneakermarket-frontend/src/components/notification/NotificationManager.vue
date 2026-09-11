<!-- components/notification/NotificationManager.vue -->
<template>
  <div class="card shadow-sm border-0 rounded-3 p-4 bg-white">
    <!-- 標題區域 -->
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h4 class="fw-bold mb-0">通知管理系統</h4>
    </div>

    <!-- 頂部搜尋與篩選區域 -->
    <div class="row g-3 mb-4">
      <div class="col-md-3">
        <label class="form-label small text-muted">通知標題</label>
        <input
          v-model="searchParams.title"
          type="text"
          class="form-control"
          placeholder="搜尋標題關鍵字..."
        />
      </div>

      <div class="col-md-3">
        <label class="form-label small text-muted">通知類型</label>
        <select v-model="searchParams.notificationType" class="form-select">
          <option value="">全部類型</option>
          <option value="EVENT_REMINDER">活動提醒 (EVENT_REMINDER)</option>
          <option value="SYSTEM_NOTICE">系統公告 (SYSTEM_NOTICE)</option>
          <option value="ORDER_UPDATE">訂單更新 (ORDER_UPDATE)</option>
          <option value="PROMOTION">促銷優惠 (PROMOTION)</option>
        </select>
      </div>

      <div class="col-md-3">
        <label class="form-label small text-muted">開始時間起</label>
        <input
          v-model="searchParams.startDate"
          type="datetime-local"
          class="form-control"
        />
      </div>

      <div class="col-md-3">
        <label class="form-label small text-muted">結束時間止</label>
        <input
          v-model="searchParams.endDate"
          type="datetime-local"
          class="form-control"
        />
      </div>

      <div class="col-12 text-end">
        <button class="btn btn-outline-secondary me-2" @click="handleReset">
          重設
        </button>
        <button class="btn btn-primary px-4" @click="fetchNotifications(1)">
          查詢
        </button>
      </div>
    </div>

    <!-- 資料表格 -->
    <div class="table-responsive">
      <table class="table table-hover align-middle">
        <thead class="table-light">
          <tr>
            <th width="60">ID</th>
            <th>通知標題</th>
            <th width="150">類型</th>
            <th width="120">對象類型</th>
            <th width="180">建立時間</th>
            <th width="260" class="text-center">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in notificationLogList" :key="item.notificationId">
            <td>{{ item.notificationId }}</td>
            <td class="fw-bold text-dark">{{ item.title }}</td>
            <td>
              <span class="badge bg-info text-dark">{{
                item.notificationType
              }}</span>
            </td>
            <td>
              <span class="badge bg-secondary">{{
                item.targetType || "通用"
              }}</span>
            </td>
            <td>{{ formatDate(item.createdAt) }}</td>
            <td class="text-center">
              <div class="btn-group btn-group-sm">
                <!-- 補回「查看」按鈕 -->
                <button
                  class="btn btn-outline-info"
                  @click="openDetailModal(item)"
                >
                  查看
                </button>
                <button
                  class="btn btn-outline-primary"
                  @click="openEditModal(item)"
                >
                  編輯
                </button>
                <button
                  class="btn btn-outline-success"
                  @click="handleSendNotice(item)"
                >
                  發送
                </button>
                <button
                  class="btn btn-outline-danger"
                  @click="handleDelete(item.notificationId)"
                >
                  刪除
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="notificationLogList.length === 0">
            <td colspan="6" class="text-center py-4 text-muted">目前查無通知資料</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分頁導覽 -->
    <nav v-if="totalPages > 1" class="d-flex justify-content-center mt-3">
      <ul class="pagination">
        <li :class="['page-item', currentPage === 1 ? 'disabled' : '']">
          <button
            class="page-link"
            @click="fetchNotifications(currentPage - 1)"
          >
            上一頁
          </button>
        </li>
        <li
          v-for="page in totalPages"
          :key="page"
          :class="['page-item', currentPage === page ? 'active' : '']"
        >
          <button class="page-link" @click="fetchNotifications(page)">
            {{ page }}
          </button>
        </li>
        <li
          :class="['page-item', currentPage === totalPages ? 'disabled' : '']"
        >
          <button
            class="page-link"
            @click="fetchNotifications(currentPage + 1)"
          >
            下一頁
          </button>
        </li>
      </ul>
    </nav>

    <!-- 查看詳情彈窗 Component -->
    <NotificationDetailModal
      v-if="showDetailModal"
      :notification="selectedNotification"
      @close="showDetailModal = false"
    />

    <!-- 編輯彈窗 Component -->
    <NotificationEditModal
      v-if="showEditModal"
      :notification="selectedNotification"
      @close="showEditModal = false"
      @success="handleEditSuccess"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import httpClient from "@/service/httpClient";
import NotificationDetailModal from "./NotificationDetailModal.vue";
import NotificationEditModal from "./NotificationEditModal.vue";

// 查詢條件與分頁狀態
const searchParams = ref({
  title: "",
  notificationType: "",
  startDate: "",
  endDate: "",
});
const currentPage = ref(1);
const totalPages = ref(1);
const notificationLogList = ref([]);

// Modal 控制狀態
const showDetailModal = ref(false);
const showEditModal = ref(false);
const selectedNotification = ref(null);

onMounted(() => {
  fetchNotifications(1);
});

// 時間格式處理 helper
const formatToIsoDateTime = (dateTimeLocalStr) => {
  if (!dateTimeLocalStr) return null;
  return dateTimeLocalStr.length === 16
    ? `${dateTimeLocalStr}:00`
    : dateTimeLocalStr;
};

// 撈取資料 API
const fetchNotifications = async (page = 1) => {
  try {
    const params = {
      pageNumber: page,
      pageSize: 10,
    };

    if (searchParams.value.title?.trim()) {
      params.title = searchParams.value.title.trim();
    }
    if (searchParams.value.notificationType) {
      params.notificationType = searchParams.value.notificationType;
    }
    if (searchParams.value.startDate) {
      params.startCreatedAt = formatToIsoDateTime(searchParams.value.startDate);
    }
    if (searchParams.value.endDate) {
      params.endCreatedAt = formatToIsoDateTime(searchParams.value.endDate);
    }


    const res = await httpClient.get("/notification/notificationPage", {
      params,
    });
    notificationLogList.value = res.data.content || [];
    totalPages.value = res.data.totalPages || 1;
    currentPage.value = page;
  } catch (err) {
    console.error("查詢失敗", err);
  }
};

// 重設查詢
const handleReset = () => {
  searchParams.value = {
    title: "",
    notificationType: "",
    startDate: "",
    endDate: "",
  };
  fetchNotifications(1);
};

// 開啟查看 Modal
const openDetailModal = (item) => {
  selectedNotification.value = item;
  showDetailModal.value = true;
};

// 開啟編輯 Modal
const openEditModal = (item) => {
  selectedNotification.value = item;
  showEditModal.value = true;
};

// 編輯成功處置
const handleEditSuccess = () => {
  showEditModal.value = false;
  fetchNotifications(currentPage.value);
};

// 點擊「發送」推播按鈕
const handleSendNotice = async (item) => {
  if (!confirm(`確定要向全體會員推播「${item.title}」嗎？`)) return;

  try {
    // 改為呼叫發送通知 API
    await httpClient.post(`/notification/send/${item.notificationId}`);
    alert("發送推播成功！");
    fetchNotifications(currentPage.value);
  } catch (err) {
    console.error("發送失敗", err);
    alert("發送失敗，請稍後再試。");
  }
};

// 點擊「刪除」按鈕
const handleDelete = async (id) => {
  if (!confirm("確定要刪除這筆通知紀錄嗎？")) return;

  try {
    await httpClient.delete("/notification/notificationDelete", {
      params: { notificationId: id },
    });
    alert("刪除成功");
    fetchNotifications(currentPage.value);
  } catch (err) {
    console.error("刪除失敗", err);
    alert("刪除失敗");
  }
};

// 時間格式化
const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  return new Date(dateStr).toLocaleString("zh-TW", { hour12: false });
};
</script>
