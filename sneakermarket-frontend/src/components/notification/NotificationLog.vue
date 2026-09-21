<template>
  <div class="notification-log">
    <section class="log-kpi-grid" aria-label="發送紀錄摘要">
      <article class="log-kpi-card kpi-total">
        <span class="log-kpi-icon">信</span>
        <div>
          <span>總發送任務</span>
          <strong>{{ totalLogCount.toLocaleString("zh-TW") }}</strong>
        </div>
      </article>

      <article class="log-kpi-card kpi-success">
        <span class="log-kpi-icon">✓</span>
        <div>
          <span>本頁成功送達</span>
          <strong>{{ pageSentCount.toLocaleString("zh-TW") }}</strong>
        </div>
      </article>

      <article class="log-kpi-card kpi-failed">
        <span class="log-kpi-icon">!</span>
        <div>
          <span>本頁失敗</span>
          <strong>{{ pageFailedCount.toLocaleString("zh-TW") }}</strong>
        </div>
      </article>

      <article class="log-kpi-card kpi-rate">
        <span class="log-kpi-icon">%</span>
        <div>
          <span>本頁平均送達率</span>
          <strong>{{ pageDeliveryRate }}%</strong>
        </div>
      </article>
    </section>

    <!-- 搜尋條件區塊 -->
    <div class="card log-search-card mb-4">
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
<div class="table-responsive log-table-wrapper">
  <table class="table table-hover align-middle mb-0 log-table">
    <thead>
      <tr>
        <th>發送編號</th>
        <th>通知名稱</th>
        <th>發送對象</th>
        <th>發送時間</th>
        <th>狀態</th>
        <th>成功／失敗</th>
        <th>送達率</th>
        <th>詳情</th>
      </tr>
    </thead>

    <tbody>
      <tr
        v-for="item in notificationLogList"
        :key="item.id"
        class="log-row"
        @click="openLogDetail(item)"
      >
        <td>#{{ item.id }}</td>

        <td>
          <strong>{{ item.title }}</strong>
          <small class="secondary-text">
            {{ item.targetType || "通用通知" }}
          </small>
        </td>

        <td>
          <span class="audience-badge">
            {{ item.sendType || "未指定" }}
          </span>

          <small class="secondary-text">
            {{ Number(item.totalRecipients || 0).toLocaleString("zh-TW") }} 人
          </small>
        </td>

        <td>{{ formatDate(item.createdAt) }}</td>

        <td>
          <span
            class="status-badge"
            :class="statusClass(item.status)"
          >
            {{ statusLabel(item.status) }}
          </span>
        </td>

        <td>
          <span class="success-count">
            {{ Number(item.totalSent || 0).toLocaleString("zh-TW") }}
          </span>
          /
          <span class="failure-count">
            {{ Number(item.failedCount || 0).toLocaleString("zh-TW") }}
          </span>
        </td>

        <td>{{ calculateDeliveryRate(item) }}</td>

        <td>
          <button
            type="button"
            class="detail-button"
            @click.stop="openLogDetail(item)"
          >
            查看
          </button>
        </td>
      </tr>

      <tr v-if="notificationLogList.length === 0">
        <td colspan="8" class="text-center py-4 text-muted">
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
    <section class="card email-job-card mt-4">
      <div class="card-body">
        <div class="d-flex justify-content-between align-items-center mb-3">
          <h5 class="mb-0">Email 失敗任務</h5>
          <button
            class="btn btn-outline-secondary btn-sm"
            @click="fetchEmailJobs(emailPage)"
          >
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
                <td colspan="5" class="text-center text-muted">
                  目前沒有失敗任務
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div
          v-if="emailTotalPages > 1"
          class="d-flex gap-2 justify-content-end"
        >
          <button
            class="btn btn-outline-secondary btn-sm"
            :disabled="emailPage === 0"
            @click="fetchEmailJobs(emailPage - 1)"
          >
            上一頁
          </button>
          <span>第 {{ emailPage + 1 }} / {{ emailTotalPages }} 頁</span>
          <button
            class="btn btn-outline-secondary btn-sm"
            :disabled="emailPage + 1 >= emailTotalPages"
            @click="fetchEmailJobs(emailPage + 1)"
          >
            下一頁
          </button>
        </div>
      </div>
    </section>

    <Teleport to="body">
  <Transition name="drawer">
    <div
      v-if="selectedLog"
      class="drawer-layer"
      @click.self="closeLogDetail"
    >
      <aside
        class="log-drawer"
        role="dialog"
        aria-modal="true"
        aria-labelledby="log-drawer-title"
      >
        <header class="drawer-header">
          <h2 id="log-drawer-title">發送詳情</h2>

          <button
            type="button"
            class="drawer-close"
            aria-label="關閉詳情"
            @click="closeLogDetail"
          >
            ×
          </button>
        </header>

        <div class="drawer-content">
          <div class="readonly-notice">
            唯讀紀錄，此為歷史任務，無法編輯或重新發送。
          </div>

          <section class="drawer-section">
            <h3>{{ selectedLog.title }}</h3>

            <dl class="detail-list">
              <div>
                <dt>發送編號</dt>
                <dd>#{{ selectedLog.id }}</dd>
              </div>

              <div>
                <dt>任務狀態</dt>
                <dd>
                  <span
                    class="status-badge"
                    :class="statusClass(selectedLog.status)"
                  >
                    {{ statusLabel(selectedLog.status) }}
                  </span>
                </dd>
              </div>

              <div>
                <dt>發送對象</dt>
                <dd>{{ selectedLog.sendType || "未指定" }}</dd>
              </div>

              <div>
                <dt>關聯類型</dt>
                <dd>{{ selectedLog.targetType || "通用通知" }}</dd>
              </div>

              <div v-if="selectedLog.eventId">
                <dt>活動 ID</dt>
                <dd>{{ selectedLog.eventId }}</dd>
              </div>

              <div>
                <dt>建立時間</dt>
                <dd>{{ formatDate(selectedLog.createdAt) }}</dd>
              </div>

              <div>
                <dt>開始時間</dt>
                <dd>{{ formatDate(selectedLog.startedAt) }}</dd>
              </div>

              <div>
                <dt>完成時間</dt>
                <dd>{{ formatDate(selectedLog.completedAt) }}</dd>
              </div>

              <div>
                <dt>目標人數</dt>
                <dd>
                  {{
                    Number(
                      selectedLog.totalRecipients || 0,
                    ).toLocaleString("zh-TW")
                  }}
                </dd>
              </div>

              <div>
                <dt>成功送達</dt>
                <dd>
                  {{
                    Number(
                      selectedLog.totalSent || 0,
                    ).toLocaleString("zh-TW")
                  }}
                </dd>
              </div>

              <div>
                <dt>失敗</dt>
                <dd class="failure-count">
                  {{
                    Number(
                      selectedLog.failedCount || 0,
                    ).toLocaleString("zh-TW")
                  }}
                </dd>
              </div>
            </dl>
          </section>

          <section class="drawer-section">
            <div class="section-title-row">
              <h3>投遞狀態</h3>
              <strong>{{ selectedDeliveryRate }}%</strong>
            </div>

            <div class="delivery-track">
              <div
                class="delivery-success"
                :style="{ width: `${selectedDeliveryRate}%` }"
              ></div>

              <div
                class="delivery-failure"
                :style="{ width: `${selectedFailureRate}%` }"
              ></div>
            </div>

            <div class="delivery-legend">
              <span>
                <i class="legend-success"></i>
                成功 {{ selectedDeliveryRate }}%
              </span>

              <span>
                <i class="legend-failure"></i>
                失敗 {{ selectedFailureRate }}%
              </span>
            </div>
          </section>

          <section
            v-if="
              Number(selectedLog.failedCount || 0) > 0 ||
              selectedLog.errorMessage
            "
            class="drawer-section"
          >
            <h3>失敗原因</h3>

            <div class="error-box">
              {{
                selectedLog.errorMessage ||
                "部分通知未能成功送達，後端未提供個別失敗原因。"
              }}
            </div>
          </section>
        </div>
      </aside>
    </div>
  </Transition>
</Teleport>

  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import {
  getNotificationLogs,
  getEmailJobs,
  retryEmailJob,
} from "@/service/notificationService";

const currentPage = ref(1);
const totalPages = ref(1);
const totalLogCount = ref(0);
const notificationLogList = ref([]);
const selectedLog = ref(null);

const pageSentCount = computed(() =>
  notificationLogList.value.reduce(
    (total, item) => total + Number(item.totalSent || 0),
    0,
  ),
);

const pageFailedCount = computed(() =>
  notificationLogList.value.reduce(
    (total, item) => total + Number(item.failedCount || 0),
    0,
  ),
);

const pageRecipientCount = computed(() =>
  notificationLogList.value.reduce(
    (total, item) => total + Number(item.totalRecipients || 0),
    0,
  ),
);

const pageDeliveryRate = computed(() => {
  if (pageRecipientCount.value <= 0) return "0.0";

  return Math.min(
    (pageSentCount.value / pageRecipientCount.value) * 100,
    100,
  ).toFixed(1);
});

const selectedDeliveryRate = computed(() => {
  if (!selectedLog.value) return 0;

  const total = Number(selectedLog.value.totalRecipients || 0);
  const sent = Number(selectedLog.value.totalSent || 0);

  if (total <= 0) return 0;

  return Math.min(Math.round((sent / total) * 1000) / 10, 100);
});

const selectedFailureRate = computed(() => {
  return Math.max(
    Math.round((100 - selectedDeliveryRate.value) * 10) / 10,
    0,
  );
});

const openLogDetail = (item) => {
  selectedLog.value = item;
};

const closeLogDetail = () => {
  selectedLog.value = null;
};


const calculateDeliveryRate = (item) => {
  const total = Number(item.totalRecipients || 0);
  const sent = Number(item.totalSent || 0);

  if (total <= 0) return "0%";

  return `${Math.min(Math.round((sent / total) * 1000) / 10, 100)}%`;
};

const statusLabel = (status) => {
  const labels = {
    PENDING: "等待處理",
    PROCESSING: "發送中",
    COMPLETED: "已完成",
    FAILED: "失敗",
  };

  return labels[status] || status || "未知";
};

const statusClass = (status) => {
  return {
    PENDING: "status-pending",
    PROCESSING: "status-processing",
    COMPLETED: "status-completed",
    FAILED: "status-failed",
  }[status];
};

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
    totalLogCount.value = Number(res.data.totalElements || 0);
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
.notification-log {
  width: 100%;
  min-width: 0;
}

.log-search-card,
.email-job-card {
  border: 1px solid #deddd8 !important;
  border-radius: 6px !important;
  background: #ffffff;
  box-shadow: none !important;
}

.log-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.log-kpi-card {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  padding: 16px;
  border: 1px solid #deddd8;
  border-radius: 6px;
  background: #ffffff;
}

.log-kpi-icon {
  display: inline-grid;
  flex: 0 0 36px;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 800;
}

.log-kpi-card span:not(.log-kpi-icon) {
  display: block;
  margin-bottom: 3px;
  color: #6f706b;
  font-size: 11px;
}

.log-kpi-card strong {
  display: block;
  color: #181916;
  font-size: 22px;
  line-height: 1;
}

.kpi-total .log-kpi-icon {
  color: #526168;
  background: #e3eaf0;
}

.kpi-success .log-kpi-icon {
  color: #52604e;
  background: #dce8d9;
}

.kpi-failed .log-kpi-icon {
  color: #744f4b;
  background: #eadbd9;
}

.kpi-rate .log-kpi-icon {
  color: #526168;
  background: #dce8f2;
}

.table th {
  font-weight: 600;
  color: #64748b;
}

.log-table-wrapper {
  border: 1px solid #deddd8;
  border-radius: 6px;
  background: #ffffff;
}

.log-table thead th {
  padding: 14px 12px;
  border-bottom: 1px solid #deddd8;
  color: #666660;
  background: #f5f4f1;
  font-size: 12px;
  white-space: nowrap;
}

.log-table tbody td {
  padding: 14px 12px;
}

.log-row {
  cursor: pointer;
}

.log-row:hover td {
  background: #f7f7f5;
}

.secondary-text {
  display: block;
  margin-top: 4px;
  color: #85857f;
  font-size: 11px;
}

.audience-badge,
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 5px 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 650;
}

.audience-badge {
  color: #526168;
  background: #dce3e6;
}

.status-pending {
  color: #70594c;
  background: #e8dcd4;
}

.status-processing {
  color: #526168;
  background: #dce3e6;
}

.status-completed {
  color: #52604e;
  background: #dce3d8;
}

.status-failed {
  color: #744f4b;
  background: #eadbd9;
}

.success-count {
  color: #667963;
  font-weight: 700;
}

.failure-count {
  color: #a05d57;
  font-weight: 700;
}

.detail-button {
  padding: 0;
  border: 0;
  color: #2f6da5;
  background: transparent;
  font-size: 13px;
  font-weight: 650;
}

@media (max-width: 900px) {
  .log-kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .log-kpi-grid {
    grid-template-columns: 1fr;
  }
}

.drawer-layer {
  position: fixed;
  z-index: 1050;
  inset: 0;
  background: rgb(17 17 17 / 24%);
}

.log-drawer {
  position: absolute;
  top: 0;
  right: 0;
  width: min(430px, 100%);
  height: 100%;
  overflow-y: auto;
  border-left: 1px solid #deddd8;
  background: #ffffff;
  box-shadow: -16px 0 40px rgb(17 17 17 / 12%);
}

.drawer-header {
  position: sticky;
  z-index: 2;
  top: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 24px;
  border-bottom: 1px solid #deddd8;
  background: #ffffff;
}

.drawer-header h2 {
  margin: 0;
  font-size: 21px;
  font-weight: 750;
}

.drawer-close {
  width: 36px;
  height: 36px;
  border: 0;
  color: #111111;
  background: transparent;
  font-size: 28px;
  line-height: 1;
}

.drawer-content {
  padding: 24px;
}

.readonly-notice {
  margin-bottom: 24px;
  padding: 12px 14px;
  border-radius: 6px;
  color: #526168;
  background: #e8f0f7;
  font-size: 12px;
}

.drawer-section {
  padding: 22px 0;
  border-bottom: 1px solid #deddd8;
}

.drawer-section:first-of-type {
  padding-top: 0;
}

.drawer-section h3 {
  margin: 0 0 18px;
  font-size: 16px;
  font-weight: 750;
}

.detail-list {
  margin: 0;
}

.detail-list div {
  display: grid;
  grid-template-columns: 110px 1fr;
  gap: 16px;
  margin-bottom: 12px;
}

.detail-list dt {
  color: #777771;
  font-weight: 500;
}

.detail-list dd {
  margin: 0;
  color: #222222;
}

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-title-row h3 {
  margin-bottom: 0;
}

.delivery-track {
  display: flex;
  height: 12px;
  overflow: hidden;
  margin: 18px 0 12px;
  border-radius: 999px;
  background: #eeeeea;
}

.delivery-success {
  background: #86a281;
}

.delivery-failure {
  background: #c87c76;
}

.delivery-legend {
  display: flex;
  justify-content: space-between;
  color: #676762;
  font-size: 12px;
}

.delivery-legend i {
  display: inline-block;
  width: 9px;
  height: 9px;
  margin-right: 5px;
  border-radius: 50%;
}

.legend-success {
  background: #86a281;
}

.legend-failure {
  background: #c87c76;
}

.error-box {
  padding: 14px;
  border-radius: 6px;
  color: #744f4b;
  background: #f3e6e4;
  white-space: pre-wrap;
}

.drawer-enter-active,
.drawer-leave-active {
  transition: opacity 180ms ease;
}

.drawer-enter-active .log-drawer,
.drawer-leave-active .log-drawer {
  transition: transform 180ms ease;
}

.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
}

.drawer-enter-from .log-drawer,
.drawer-leave-to .log-drawer {
  transform: translateX(100%);
}
</style>
