<!-- components/notification/NotificationManager.vue -->
<template>
  <div class="notification-manager">
    <!-- 頂部搜尋與篩選區域 -->
    <div class="notification-filter">
      <div>
        <input
          v-model="searchParams.title"
          type="text"
          class="form-control"
          placeholder="搜尋通知名稱"
        />
      </div>

      <div>
        <select v-model="searchParams.notificationType" class="form-select">
          <option value="">全部類型</option>
          <option value="EVENT_REMINDER">活動提醒 (EVENT_REMINDER)</option>
          <option value="SYSTEM_NOTICE">系統公告 (SYSTEM_NOTICE)</option>
          <option value="ORDER_UPDATE">訂單更新 (ORDER_UPDATE)</option>
          <option value="PROMOTION">促銷優惠 (PROMOTION)</option>
        </select>
      </div>

      <div>
        <input
          v-model="searchParams.startDate"
          type="datetime-local"
          class="form-control"
          placeholder="開始日期"
        />
      </div>

      <div>
        <input
          v-model="searchParams.endDate"
          type="datetime-local"
          class="form-control"
          placeholder="結束日期"
        />
      </div>

      <div class="filter-actions">
        <button
          type="button"
          class="filter-button reset-filter"
          @click="handleReset"
        >
          重設
        </button>
        <button
          type="button"
          class="filter-button search-filter"
          @click="fetchNotifications(1)"
        >
          查詢
        </button>
      </div>
    </div>

    <!-- 資料表格 -->
    <div class="table-responsive">
      <table class="table table-hover align-middle notification-table">
        <thead>
          <tr>
            <th>通知名稱</th>
            <th>類型</th>
            <th>發送對象</th>
            <th>建立／發送時間</th>
            <th>狀態</th>
            <th class="notification-action-heading">操作</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="(item, index) in notificationLogList"
            :key="item.notificationId"
          >
            <td class="notification-title-cell">
              <strong>{{ item.title }}</strong>

              <small> ID #{{ item.notificationId }} </small>
            </td>

            <td>
              <span class="type-badge">
                {{ notificationTypeLabel(item.notificationType) }}
              </span>
            </td>

            <td>
              <strong class="audience-label">
                {{ notificationAudience(item) }}
              </strong>

              <small v-if="item.targetId"> 關聯 ID：{{ item.targetId }} </small>
            </td>

            <td>
              <span>
                {{ formatDate(item.sentAt || item.createdAt) }}
              </span>

              <small>
                {{ item.sentAt ? "發送時間" : "建立時間" }}
              </small>
            </td>

            <td>
              <span
                class="notification-status"
                :class="notificationStatus(item).className"
              >
                <i></i>
                {{ notificationStatus(item).label }}
              </span>
            </td>

            <td
              class="notification-action-cell"
              :class="{
                'menu-open': openActionMenuId === item.notificationId,
              }"
            >
              <div class="notification-actions">
                <div class="notification-more-menu">
                  <button
                    type="button"
                    class="notification-more-action"
                    aria-label="更多通知操作"
                    :aria-expanded="openActionMenuId === item.notificationId"
                    :aria-controls="`notification-menu-${item.notificationId}`"
                    @click.stop="toggleActionMenu(item.notificationId)"
                  >
                    •••
                  </button>

                  <div
                    v-if="openActionMenuId === item.notificationId"
                    :id="`notification-menu-${item.notificationId}`"
                    class="notification-action-menu"
                    :class="{
                      'open-up': index >= notificationLogList.length - 2,
                    }"
                    role="menu"
                    @click.stop
                  >
                    <button
                      type="button"
                      class="notification-menu-item"
                      role="menuitem"
                      @click="handleOpenDetailFromMenu(item)"
                    >
                      查看詳情
                    </button>

                    <button
                      v-if="!item.isSent"
                      type="button"
                      class="notification-menu-item"
                      role="menuitem"
                      @click="handleOpenEditFromMenu(item)"
                    >
                      編輯通知
                    </button>

                    <button
                      v-if="!item.isSent"
                      type="button"
                      class="notification-menu-item"
                      role="menuitem"
                      :disabled="item.isQueued"
                      @click="handleSendFromMenu(item, 'ALL_MEMBERS')"
                    >
                      發給全體會員
                    </button>

                    <button
                      v-if="
                        !item.isSent &&
                        item.targetType === 'EVENT' &&
                        item.targetId
                      "
                      type="button"
                      class="notification-menu-item"
                      role="menuitem"
                      :disabled="item.isQueued"
                      @click="handleSendFromMenu(item, 'EVENT_FOLLOWERS')"
                    >
                      發給活動關注者
                    </button>

                    <button
                      v-if="!item.isSent"
                      type="button"
                      class="notification-menu-item danger"
                      role="menuitem"
                      @click="handleDeleteFromMenu(item.notificationId)"
                    >
                      刪除通知
                    </button>
                  </div>
                </div>
              </div>
            </td>
          </tr>

          <tr v-if="notificationLogList.length === 0">
            <td colspan="6" class="text-center py-5 text-muted">
              目前查無通知資料
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分頁導覽 -->
    <nav
      v-if="totalPages > 1"
      class="notification-pagination"
      aria-label="通知列表分頁"
    >
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
import { onBeforeUnmount, onMounted, ref, watch } from "vue";

import {
  deleteNotification,
  getNotificationPage,
  sendNotification,
} from "@/service/notificationService";

import NotificationDetailModal from "./NotificationDetailModal.vue";
import NotificationEditModal from "./NotificationEditModal.vue";

const props = defineProps({
  sentStatus: {
    type: String,
    default: "ALL",
    validator: (value) => ["ALL", "UNSENT", "SENT"].includes(value),
  },
});

const emit = defineEmits(["changed"]);

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

const openActionMenuId = ref(null);

const closeActionMenu = () => {
  openActionMenuId.value = null;
};

const toggleActionMenu = (notificationId) => {
  openActionMenuId.value =
    openActionMenuId.value === notificationId ? null : notificationId;
};

const handleOpenDetailFromMenu = (item) => {
  closeActionMenu();
  openDetailModal(item);
};

const handleOpenEditFromMenu = (item) => {
  closeActionMenu();
  openEditModal(item);
};

const handleSendFromMenu = (item, audienceType) => {
  closeActionMenu();
  handleSendNotice(item, audienceType);
};

const handleDeleteFromMenu = (notificationId) => {
  closeActionMenu();
  handleDelete(notificationId);
};

onMounted(() => {
  document.addEventListener("click", closeActionMenu);
});

onBeforeUnmount(() => {
  document.removeEventListener("click", closeActionMenu);
});

watch(
  () => props.sentStatus,
  () => {
    fetchNotifications(1);
  },
  {
    immediate: true,
  },
);
// 時間格式處理 helper
const formatToIsoDateTime = (dateTimeLocalStr) => {
  if (!dateTimeLocalStr) return null;
  return dateTimeLocalStr.length === 16
    ? `${dateTimeLocalStr}:00`
    : dateTimeLocalStr;
};

// 撈取資料 API
async function fetchNotifications(page = 1) {
  try {
    const params = {
      pageNumber: page,
      pageSize: 10,
    };

    if (props.sentStatus === "UNSENT") {
      params.isSent = false;
    }

    if (props.sentStatus === "SENT") {
      params.isSent = true;
    }

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
}

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
const handleEditSuccess = async () => {
  showEditModal.value = false;

  await fetchNotifications(currentPage.value);
  emit("changed");
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
      setTimeout(async () => {
        await fetchNotifications(currentPage.value);
        emit("changed");
      }, 2000);
    } else {
      alert(
        `${response.data.message}\n共發送給 ${response.data.totalSent} 位會員`,
      );

      // 同步發送已經完成
      item.isQueued = false;

      await fetchNotifications(currentPage.value);
      emit("changed");
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
    await fetchNotifications(currentPage.value);
    emit("changed");
  } catch (err) {
    console.error("刪除失敗", err);
    alert("刪除失敗");
  }
};

const notificationStatus = (item) => {
  if (item.isQueued) {
    return {
      label: "發送中",
      className: "status-processing",
    };
  }

  if (item.isSent) {
    return {
      label: "已發送",
      className: "status-sent",
    };
  }

  return {
    label: "未發送",
    className: "status-unsent",
  };
};

const notificationTypeLabel = (type) => {
  const labels = {
    EVENT_REMINDER: "活動提醒",
    SYSTEM_NOTICE: "系統公告",
    ORDER_UPDATE: "訂單更新",
    PROMOTION: "促銷優惠",
  };

  return labels[type] || type || "未分類";
};

const notificationAudience = (item) => {
  if (item.targetType === "EVENT" && item.targetId) {
    return "活動關注者";
  }

  return "全體會員";
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
  border: 1px solid #e3e4e0;
  border-radius: 5px;
  background: #ffffff;
}

/* 右側操作欄 */

.notification-action-heading,
.notification-action-cell {
  position: sticky;
  right: 0;
  width: 104px;
  min-width: 104px;
}

.notification-action-heading {
  z-index: 3;
  background: #f7f7f5 !important;
  text-align: center;
}

.notification-action-cell {
  z-index: 2;
  background: #ffffff !important;
  box-shadow: -7px 0 10px rgb(17 17 17 / 4%);
}

.notification-action-cell.menu-open {
  z-index: 10;
}

.notification-table tbody tr:hover .notification-action-cell {
  background: #fafaf8 !important;
}

.notification-actions {
  display: flex;
  align-items: center;
  justify-content: center;
}

.notification-more-action {
  height: 28px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 650;
  cursor: pointer;
}

.notification-more-menu {
  position: relative;
}

.notification-more-action {
  width: 30px;
  padding: 0;
  border: 1px solid #dedfdb;
  color: #5c6059;
  background: #ffffff;
  letter-spacing: 1px;
}

.notification-more-action:hover {
  border-color: #bfc1bb;
  background: #f3f3f0;
}

/* 操作下拉選單 */

.notification-action-menu {
  position: absolute;
  z-index: 20;
  top: 34px;
  right: 0;
  width: 154px;
  overflow: hidden;
  padding: 4px;
  border: 1px solid #dedfdb;
  border-radius: 5px;
  background: #ffffff;
  box-shadow: 0 10px 28px rgb(17 17 17 / 12%);
}

.notification-action-menu.open-up {
  top: auto;
  bottom: 34px;
}

.notification-menu-item {
  display: block;
  width: 100%;
  padding: 8px 10px;
  border: 0;
  border-radius: 3px;
  color: #464943;
  background: transparent;
  font-size: 11px;
  text-align: left;
  cursor: pointer;
}

.notification-menu-item:hover:not(:disabled) {
  background: #f2f2ef;
}

.notification-menu-item:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.notification-menu-item.danger {
  color: #9a554f;
}

.notification-menu-item.danger:hover {
  background: #f7eae8;
}
/* 通知列表基本版面 */

.notification-table {
  width: 100%;
  min-width: 880px;
  margin-bottom: 0;
}

.notification-table thead th {
  padding: 10px 11px;
  border-bottom: 1px solid #dedfdb;
  color: #6c7069;
  background: #f7f7f5;
  font-size: 11px;
  font-weight: 650;
  white-space: nowrap;
}

.notification-table tbody td {
  padding: 10px 11px;
  border-bottom: 1px solid #ecece9;
  color: #383b37;
  background: #ffffff;
  font-size: 11px;
  vertical-align: middle;
}

.notification-table tbody tr:last-child td {
  border-bottom: 0;
}

.notification-table tbody tr:hover td {
  background: #fafaf8;
}

.notification-table tbody tr:hover .notification-action-cell {
  background: #fafaf8 !important;
}

/* 通知名稱 */

.notification-title-cell {
  width: 27%;
}

.notification-title-cell strong {
  display: block;
  max-width: 260px;
  overflow: hidden;
  color: #222420;
  font-size: 12px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-title-cell small,
.notification-table td > small {
  display: block;
  margin-top: 3px;
  color: #949791;
  font-size: 10px;
}

/* 通知類型 */

.type-badge {
  display: inline-flex;
  align-items: center;
  padding: 5px 9px;
  border-radius: 999px;
  color: #67536a;
  background: #eee3ec;
  font-size: 10px;
  font-weight: 650;
  white-space: nowrap;
}

/* 發送對象 */

.audience-label {
  display: block;
  color: #444742;
  font-size: 11px;
  font-weight: 700;
}

/* 通知狀態 */

.notification-status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 10px;
  font-weight: 650;
  white-space: nowrap;
}

.notification-status i {
  width: 7px;
  height: 7px;
  flex: 0 0 7px;
  border-radius: 50%;
}

.status-unsent {
  color: #6d706b;
}

.status-unsent i {
  background: #a5a69f;
}

.status-processing {
  color: #526a82;
}

.status-processing i {
  background: #7095bf;
}

.status-sent {
  color: #586d54;
}

.status-sent i {
  background: #79a278;
}

.notification-manager {
  min-width: 0;
  padding: 0;
  border: 0;
  background: transparent;
}

.notification-filter {
  display: grid;
  grid-template-columns:
    minmax(0, 1.25fr)
    minmax(0, 0.95fr)
    minmax(0, 1.15fr)
    minmax(0, 1.15fr)
    108px;
  gap: 8px;
  margin-bottom: 12px;
}

.notification-filter > div {
  min-width: 0;
}

.notification-filter .form-control,
.notification-filter .form-select {
  height: 38px;
  min-height: 38px;
  border: 1px solid #dfe0dc;
  border-radius: 5px;
  font-size: 12px;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 108px;
}

.filter-button {
  flex: 1 1 0;
  height: 38px;
  min-width: 0;
  padding: 0 10px;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 650;
  white-space: nowrap;
}

.reset-filter {
  border: 1px solid #dfe0dc;
  color: #555852;
  background: #ffffff;
}

.search-filter {
  border: 1px solid #222320;
  color: #ffffff;
  background: #222320;
}

.notification-filter .form-control:focus,
.notification-filter .form-select:focus {
  border-color: #9a9d96;
  box-shadow: 0 0 0 2px rgb(17 17 17 / 6%);
}

.reset-filter:hover {
  border-color: #bfc1bb;
  background: #f6f6f3;
}

.search-filter:hover {
  background: #111211;
}

.notification-pagination {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}

.notification-pagination .pagination {
  gap: 4px;
  margin: 0;
}

.notification-pagination .page-link {
  min-width: 32px;
  border-color: #dedfdb;
  color: #565a53;
  background: #ffffff;
  font-size: 11px;
  text-align: center;
}

.notification-pagination .page-item.active .page-link {
  border-color: #292b28;
  color: #ffffff;
  background: #292b28;
}

@media (max-width: 1100px) {
  .notification-filter {
    grid-template-columns:
      minmax(0, 1.2fr)
      minmax(0, 0.8fr)
      minmax(0, 1fr)
      minmax(0, 1fr);
  }

  .filter-actions {
    grid-column: 1 / -1;
    justify-content: flex-end;
  }
}

@media (max-width: 760px) {
  .notification-filter {
    grid-template-columns: 1fr;
  }

  .filter-actions {
    grid-column: auto;
  }

  .filter-button {
    flex: 1;
  }

  .notification-table {
    min-width: 820px;
  }
}
</style>
