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
      <table class="table table-hover align-middle notification-table">
        <thead class="table-light">
          <tr>
            <th width="60">ID</th>
            <th>通知標題</th>
            <th width="150">類型</th>
            <th width="120">對象類型</th>
            <th width="180">建立時間</th>
            <th class="notification-action-heading">操作</th>
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
            <td class="notification-action-cell">
              <div class="notification-actions">
                <button
                  type="button"
                  class="notification-action action-view"
                  title="查看通知"
                  @click="openDetailModal(item)"
                >
                  查看
                </button>

                <button
                  type="button"
                  class="notification-action action-edit"
                  title="編輯通知"
                  @click="openEditModal(item)"
                >
                  編輯
                </button>

                <button
                  type="button"
                  class="notification-action action-all"
                  title="發送給全體會員"
                  :disabled="item.isSent || item.isQueued"
                  @click="handleSendNotice(item, 'ALL_MEMBERS')"
                >
                  發給全體
                </button>

                <button
                  type="button"
                  class="notification-action action-followers"
                  title="發送給活動關注者"
                  :disabled="
                    item.isSent ||
                    item.isQueued ||
                    item.targetType !== 'EVENT' ||
                    !item.targetId
                  "
                  @click="handleSendNotice(item, 'EVENT_FOLLOWERS')"
                >
                  發給關注者
                </button>

                <button
                  type="button"
                  class="notification-action action-delete"
                  title="刪除通知"
                  @click="handleDelete(item.notificationId)"
                >
                  刪除
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="notificationLogList.length === 0">
            <td colspan="6" class="text-center py-4 text-muted">
              目前查無通知資料
            </td>
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
import {
  deleteNotification,
  getNotificationPage,
  sendNotification,
} from "@/service/notificationService";
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

    const res = await getNotificationPage(params);

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

// 「發送」推播按鈕
const handleSendNotice = async (item, audienceType) => {
  if (item.isSent || item.isQueued) {
    alert("這則通知已經發送或正在等待發送");
    return;
  }

  const audienceLabel =
    audienceType === "ALL_MEMBERS" ? "全體會員" : "該活動的關注者";

  const confirmed = confirm(
    `確定要將「${item.title}」發送給${audienceLabel}嗎？`,
  );

  if (!confirmed) {
    return;
  }

  // 按下發送後先停用按鈕，避免連續加入兩次 Redis
  item.isQueued = true;

  try {
    const response = await sendNotification(item.notificationId, audienceType);

    if (response.data.mode === "ASYNC") {
      alert(`${response.data.message}\n任務編號：${response.data.jobId}`);

      // Redis 背景發送，稍後更新列表
      setTimeout(() => {
        fetchNotifications(currentPage.value);
      }, 2000);
    } else {
      alert(
        `${response.data.message}\n共發送給 ${response.data.totalSent} 位會員`,
      );

      // 同步發送已經完成
      item.isQueued = false;

      await fetchNotifications(currentPage.value);
    }
  } catch (error) {
    // 加入 Redis 失敗，重新開放按鈕
    item.isQueued = false;

    console.error("加入通知佇列失敗：", error);

    const message =
      error.response?.data?.detail ||
      error.response?.data?.message ||
      "加入通知佇列失敗，請稍後再試";

    alert(message);
  }
};

// 點擊「刪除」按鈕
const handleDelete = async (id) => {
  if (!confirm("確定要刪除這筆通知紀錄嗎？")) return;

  try {
    await deleteNotification(id);
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

<style scoped>
.table-responsive {
  max-width: 100%;
  overflow-x: auto;
}

.notification-action-heading,
.notification-action-cell {
  position: sticky;
  right: 0;
  width: 310px;
  min-width: 310px;
}

.notification-action-heading {
  z-index: 3;
  background: #eeece8;
}

.notification-action-cell {
  z-index: 2;
  background: #ffffff;
  box-shadow: -8px 0 12px rgba(15, 23, 42, 0.05);
}

.notification-actions {
  display: grid;
  grid-template-columns: repeat(3, max-content);
  justify-content: start;
  gap: 8px;
}
.notification-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 60px;
  height: 36px;
  padding: 0 14px;
  border: 0;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 650;
  line-height: 1;
  white-space: nowrap;
  cursor: pointer;
  transition:
    transform 140ms ease,
    filter 140ms ease;
}

.action-view,
.action-edit,
.action-delete {
  min-width: 60px;
}

.action-all {
  min-width: 88px;
}

.action-followers {
  min-width: 104px;
}

.notification-action:hover:not(:disabled) {
  transform: translateY(-1px);
  filter: brightness(0.95);
}

.notification-action:disabled {
  cursor: not-allowed;
  opacity: 0.42;
}

/* 查看：霧藍灰 */
.action-view {
  color: #526168;
  background: #dce3e6;
}

/* 編輯：中性水泥灰 */
.action-edit {
  color: #3f403d;
  background: #dfded9;
}

/* 發送全體：鼠尾草綠 */
.action-all {
  color: #52604e;
  background: #dce3d8;
}

/* 活動關注者：陶土色 */
.action-followers {
  color: #70594c;
  background: #e8dcd4;
}

/* 刪除：灰玫瑰 */
.action-delete {
  color: #744f4b;
  background: #eadbd9;
}

@media (max-width: 768px) {
  .notification-action-heading,
  .notification-action-cell {
    width: 440px;
    min-width: 440px;
  }
}
</style>
