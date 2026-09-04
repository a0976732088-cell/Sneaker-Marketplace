<template>
  <div class="card shadow-sm border-0 rounded-3 p-4 bg-white">
    <!-- 標題 -->
    <h4 class="fw-bold mb-4">通知管理系統</h4>

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
          <option value="ORDER_CREATED">訂單通知 (ORDER_CREATED)</option>
          <option value="SYSTEM_ANNOUNCEMENT">系統公告 (SYSTEM_ANNOUNCEMENT)</option>
        </select>
      </div>

      <div class="col-md-3">
        <label class="form-label small text-muted">開始時間起</label>
        <input v-model="searchParams.startDate" type="datetime-local" class="form-control" />
      </div>

      <div class="col-md-3">
        <label class="form-label small text-muted">結束時間止</label>
        <input v-model="searchParams.endDate" type="datetime-local" class="form-control" />
      </div>

      <div class="col-12 text-end">
        <button class="btn btn-outline-secondary me-2" @click="handleReset">重設</button>
        <button class="btn btn-primary px-4" @click="fetchNotifications">查詢</button>
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
          <tr v-for="item in notificationList" :key="item.notificationId">
            <td>{{ item.notificationId }}</td>
            <td class="fw-bold text-dark">{{ item.title }}</td>
            <td>
              <span class="badge bg-info text-dark">{{ item.notificationType }}</span>
            </td>
            <td>
              <span class="badge bg-secondary">{{ item.targetType || '通用' }}</span>
            </td>
            <td>{{ formatDate(item.createdAt) }}</td>
            <td class="text-center">
              <div class="btn-group btn-group-sm">
                <button class="btn btn-outline-info" @click="openModal('VIEW', item)">查看</button>
                <button class="btn btn-outline-primary" @click="openModal('EDIT', item)">編輯</button>
                <button class="btn btn-outline-success" @click="handleSendNotice(item)">發送</button>
                <button class="btn btn-outline-danger" @click="handleDelete(item.notificationId)">刪除</button>
              </div>
            </td>
          </tr>
          <tr v-if="notificationList.length === 0">
            <td colspan="6" class="text-center py-4 text-muted">目前查無通知資料</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 查看 / 編輯 / 新增 彈窗 (Modal) -->
    <div ref="modalRef" class="modal fade" tabindex="-1">
      <div class="modal-dialog modal-lg">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title fw-bold">
              {{ modalMode === 'VIEW' ? '查看通知' : modalMode === 'EDIT' ? '編輯通知' : '新增通知' }}
            </h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body">
            <form>
              <div class="mb-3">
                <label class="form-label">通知標題</label>
                <input
                  v-model="formData.title"
                  :disabled="modalMode === 'VIEW'"
                  type="text"
                  class="form-control"
                />
              </div>

              <div class="row g-3 mb-3">
                <div class="col-md-6">
                  <label class="form-label">通知類型</label>
                  <input
                    v-model="formData.notificationType"
                    :disabled="modalMode === 'VIEW'"
                    type="text"
                    class="form-control"
                  />
                </div>
                <div class="col-md-6">
                  <label class="form-label">目標類型 (Target Type)</label>
                  <input
                    v-model="formData.targetType"
                    :disabled="modalMode === 'VIEW'"
                    type="text"
                    class="form-control"
                  />
                </div>
              </div>

              <div class="row g-3 mb-3">
                <div class="col-md-6">
                  <label class="form-label">目標 ID (Target ID)</label>
                  <input
                    v-model="formData.targetId"
                    :disabled="modalMode === 'VIEW'"
                    type="number"
                    class="form-control"
                  />
                </div>
                <div class="col-md-6">
                  <label class="form-label">跳轉 URL (Target URL)</label>
                  <input
                    v-model="formData.targetUrl"
                    :disabled="modalMode === 'VIEW'"
                    type="text"
                    class="form-control"
                  />
                </div>
              </div>

              <div class="mb-3">
                <label class="form-label">通知內文</label>
                <textarea
                  v-model="formData.content"
                  :disabled="modalMode === 'VIEW'"
                  rows="4"
                  class="form-control"
                ></textarea>
              </div>
            </form>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">關閉</button>
            <button
              v-if="modalMode !== 'VIEW'"
              type="button"
              class="btn btn-primary"
              @click="submitForm"
            >
              儲存變更
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { Modal } from 'bootstrap';

// 查詢條件
const searchParams = ref({
  title: '',
  notificationType: '',
  startDate: '',
  endDate: ''
});

// 資料清單
const notificationList = ref([]);

// Modal 相關控制
const modalRef = ref(null);
let modalInstance = null;
const modalMode = ref('VIEW'); // 'VIEW', 'EDIT', 'CREATE'

const formData = ref({
  notificationId: null,
  title: '',
  notificationType: '',
  content: '',
  targetType: '',
  targetId: null,
  targetUrl: ''
});

onMounted(() => {
  if (modalRef.value) {
    modalInstance = new Modal(modalRef.value);
  }
  fetchNotifications();
});

// 撈取資料 API
const fetchNotifications = async () => {
  try {
    const res = await axios.get('/api/notifications', { params: searchParams.value });
    notificationList.value = Array.isArray(res.data) ? res.data : (res.data.content || []);
  } catch (err) {
    console.error('查詢失敗', err);
  }
};

// 重設查詢
const handleReset = () => {
  searchParams.value = { title: '', notificationType: '', startDate: '', endDate: '' };
  fetchNotifications();
};

// 打開 Modal (查看/編輯)
const openModal = (mode, item) => {
  modalMode.value = mode;
  formData.value = { ...item };
  modalInstance.show();
};

// 儲存表單 (新增 / 編輯)
const submitForm = async () => {
  try {
    if (modalMode.value === 'EDIT') {
      await axios.put(`/api/notifications/${formData.value.notificationId}`, formData.value);
      alert('更新成功');
    }
    modalInstance.hide();
    fetchNotifications();
  } catch (err) {
    console.error('儲存失敗', err);
    alert('操作失敗');
  }
};

// 點擊「發送」按鈕
const handleSendNotice = async (item) => {
  if (!confirm(`確定要向全體會員推播「${item.title}」嗎？`)) return;

  try {
    await axios.post('/api/notification/createNotice', {
      notificationType: item.notificationType,
      title: item.title,
      content: item.content,
      targetType: item.targetType,
      targetId: item.targetId,
      targetUrl: item.targetUrl
    });
    alert('發送推播成功！');
  } catch (err) {
    console.error('發送失敗', err);
    alert('發送失敗');
  }
};

// 點擊「刪除」按鈕
const handleDelete = async (id) => {
  if (!confirm('確定要刪除這筆通知紀錄嗎？')) return;

  try {
    await axios.delete(`/api/notifications/${id}`);
    alert('刪除成功');
    fetchNotifications();
  } catch (err) {
    console.error('刪除失敗', err);
  }
};

// 時間格式化
const formatDate = (dateStr) => {
  if (!dateStr) return '-';
  return new Date(dateStr).toLocaleString();
};
</script>