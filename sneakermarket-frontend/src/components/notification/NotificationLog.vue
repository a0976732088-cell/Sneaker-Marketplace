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
    <div class="log-filter">
      <input
        v-model="searchParams.title"
        type="search"
        class="log-filter-control log-title-search"
        placeholder="搜尋通知標題"
        aria-label="搜尋通知標題"
        @keyup.enter="fetchLogs(1)"
      />

      <select
        v-model="searchParams.sendType"
        class="log-filter-control"
        aria-label="發送對象"
      >
        <option value="">全部發送對象</option>
        <option value="ALL_MEMBERS">全體會員</option>
        <option value="EVENT_FOLLOWERS">活動關注者</option>
      </select>

      <div class="log-filter-actions">
        <button
          type="button"
          class="log-filter-button reset"
          @click="resetSearch"
        >
          重設
        </button>

        <button
          type="button"
          class="log-filter-button search"
          @click="fetchLogs(1)"
        >
          搜尋
        </button>
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
                {{ sendTypeLabel(item.sendType) }}
              </span>

              <small class="secondary-text">
                {{ Number(item.totalRecipients || 0).toLocaleString("zh-TW") }}
                人
              </small>
            </td>

            <td>{{ formatDate(item.createdAt) }}</td>

            <td>
              <span class="status-badge" :class="statusClass(item.status)">
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
    <nav
      v-if="totalPages > 1"
      class="log-pagination"
      aria-label="發送紀錄分頁"
    >
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
    <section
      v-if="visibleEmailJobs.length > 0 || emailError"
      class="email-failure-section"
      aria-label="Email 失敗任務"
    >
      <details class="email-failure-details">
        <summary class="email-failure-summary">
          <span class="email-failure-icon">!</span>

          <span class="email-failure-title"> Email 失敗任務 </span>

          <span v-if="visibleEmailJobs.length > 0" class="email-failure-count">
            {{ visibleEmailJobs.length }}
          </span>

          <span class="email-failure-toggle"> 展開查看 </span>
        </summary>

        <div class="email-failure-content">
          <div class="email-failure-toolbar">
            <p>
              僅顯示目前等待處理的失敗寄送工作；略過提醒只會套用在此瀏覽器。
            </p>

            <button
              type="button"
              class="email-refresh-button"
              @click="fetchEmailJobs(emailPage)"
            >
              重新整理
            </button>
          </div>

          <p v-if="emailError" class="email-error-message">
            {{ emailError }}
          </p>

          <div
            v-if="visibleEmailJobs.length > 0"
            class="table-responsive email-job-table-wrapper"
          >
            <table class="table align-middle email-job-table">
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
                <tr v-for="job in visibleEmailJobs" :key="job.id">
                  <td>#{{ job.id }}</td>
                  <td>{{ job.recipientEmail }}</td>
                  <td>{{ job.retryCount }}</td>
                  <td class="email-error-cell">
                    {{ job.lastError || "未提供錯誤原因" }}
                  </td>
                  <td>
                    <div class="email-job-actions">
                      <button
                        type="button"
                        class="email-retry-button"
                        :disabled="retryingId === job.id"
                        @click="retryEmail(job)"
                      >
                        {{ retryingId === job.id ? "處理中…" : "手動重試" }}
                      </button>

                      <button
                        type="button"
                        class="email-dismiss-button"
                        title="僅在此瀏覽器隱藏，後端失敗紀錄仍會保留"
                        @click="dismissEmailJob(job)"
                      >
                        略過提醒
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-if="emailTotalPages > 1" class="email-job-pagination">
            <button
              type="button"
              :disabled="emailPage === 0"
              @click="fetchEmailJobs(emailPage - 1)"
            >
              上一頁
            </button>

            <span> 第 {{ emailPage + 1 }} / {{ emailTotalPages }} 頁 </span>

            <button
              type="button"
              :disabled="emailPage + 1 >= emailTotalPages"
              @click="fetchEmailJobs(emailPage + 1)"
            >
              下一頁
            </button>
          </div>
        </div>
      </details>
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
                    <dd>{{ sendTypeLabel(selectedLog.sendType) }}</dd>
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
                        Number(selectedLog.totalRecipients || 0).toLocaleString(
                          "zh-TW",
                        )
                      }}
                    </dd>
                  </div>

                  <div>
                    <dt>成功送達</dt>
                    <dd>
                      {{
                        Number(selectedLog.totalSent || 0).toLocaleString(
                          "zh-TW",
                        )
                      }}
                    </dd>
                  </div>

                  <div>
                    <dt>失敗</dt>
                    <dd class="failure-count">
                      {{
                        Number(selectedLog.failedCount || 0).toLocaleString(
                          "zh-TW",
                        )
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
  return Math.max(Math.round((100 - selectedDeliveryRate.value) * 10) / 10, 0);
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

const sendTypeLabel = (sendType) => {
  const labels = {
    ALL_MEMBERS: "全體會員",
    EVENT_FOLLOWERS: "活動關注者",
  };

  return labels[sendType] || sendType || "未指定";
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
const DISMISSED_EMAIL_JOBS_KEY =
  "sneakermarket.admin.dismissed-email-job-ids.v1";
const dismissedEmailJobIds = ref([]);

const visibleEmailJobs = computed(() => {
  const dismissedIds = new Set(dismissedEmailJobIds.value);

  return emailJobs.value.filter(
    (job) => !dismissedIds.has(String(job.id)),
  );
});

const loadDismissedEmailJobs = () => {
  try {
    const stored = JSON.parse(
      window.localStorage.getItem(DISMISSED_EMAIL_JOBS_KEY) || "[]",
    );

    dismissedEmailJobIds.value = Array.isArray(stored)
      ? stored.map(String)
      : [];
  } catch {
    dismissedEmailJobIds.value = [];
  }
};

const dismissEmailJob = (job) => {
  const jobId = String(job.id);

  if (!dismissedEmailJobIds.value.includes(jobId)) {
    dismissedEmailJobIds.value = [
      ...dismissedEmailJobIds.value,
      jobId,
    ];
  }

  try {
    window.localStorage.setItem(
      DISMISSED_EMAIL_JOBS_KEY,
      JSON.stringify(dismissedEmailJobIds.value),
    );
  } catch {
    // localStorage 不可用時仍保留本次瀏覽期間的隱藏狀態。
  }
};

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

const buildLogQueryParams = () => {
  const params = {};

  if (searchParams.value.title?.trim()) {
    params.title = searchParams.value.title.trim();
  }

  if (searchParams.value.sendType) {
    params.sendType = searchParams.value.sendType;
  }

  return params;
};

onMounted(() => {
  loadDismissedEmailJobs();
  fetchLogs(1);
  fetchEmailJobs(0);
});

// 🎯 2. 對接後台 Log 查詢 API (/api/notification/logs)
const fetchLogs = async (page = 1) => {
  try {
    const params = {
      ...buildLogQueryParams(),
      pageNumber: page,
      pageSize: 10,
    };

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

const escapeCsvValue = (value) => {
  let text = String(value ?? "");

  // 避免 Excel 將外部資料誤認成公式
  if (/^[=+\-@]/.test(text)) {
    text = `'${text}`;
  }

  return `"${text.replace(/"/g, '""')}"`;
};

const exportLogsCsv = async () => {
  const pageSize = 100;

  const firstResponse = await getNotificationLogs({
    ...buildLogQueryParams(),
    pageNumber: 1,
    pageSize,
  });

  const firstPage = firstResponse.data ?? {};
  const allLogs = [...(firstPage.content ?? [])];
  const exportTotalPages = firstPage.totalPages ?? 1;

  if (exportTotalPages > 1) {
    const remainingRequests = [];

    for (let page = 2; page <= exportTotalPages; page += 1) {
      remainingRequests.push(
        getNotificationLogs({
          ...buildLogQueryParams(),
          pageNumber: page,
          pageSize,
        }),
      );
    }

    const remainingResponses = await Promise.all(remainingRequests);

    remainingResponses.forEach((response) => {
      allLogs.push(...(response.data?.content ?? []));
    });
  }

  const headers = [
    "發送編號",
    "通知名稱",
    "發送對象",
    "關聯類型",
    "活動ID",
    "狀態",
    "目標人數",
    "成功送達",
    "失敗數量",
    "送達率",
    "建立時間",
    "開始時間",
    "完成時間",
    "錯誤訊息",
  ];

  const rows = allLogs.map((item) => [
    item.id,
    item.title,
    sendTypeLabel(item.sendType),
    item.targetType || "通用通知",
    item.eventId || "",
    statusLabel(item.status),
    Number(item.totalRecipients || 0),
    Number(item.totalSent || 0),
    Number(item.failedCount || 0),
    calculateDeliveryRate(item),
    formatDate(item.createdAt),
    formatDate(item.startedAt),
    formatDate(item.completedAt),
    item.errorMessage || "",
  ]);

  const csv = [
    headers.map(escapeCsvValue).join(","),
    ...rows.map((row) => row.map(escapeCsvValue).join(",")),
  ].join("\r\n");

  const blob = new Blob([`\uFEFF${csv}`], {
    type: "text/csv;charset=utf-8",
  });

  const downloadUrl = URL.createObjectURL(blob);
  const link = document.createElement("a");
  const today = new Date().toISOString().slice(0, 10);

  link.href = downloadUrl;
  link.download = `notification-logs-${today}.csv`;

  document.body.appendChild(link);
  link.click();
  link.remove();

  URL.revokeObjectURL(downloadUrl);

  return allLogs.length;
};

defineExpose({
  exportLogsCsv,
});
</script>

<style scoped>
.notification-log {
  width: 100%;
  min-width: 0;
}

.log-filter {
  display: grid;
  grid-template-columns:
    minmax(220px, 1fr)
    minmax(160px, 220px)
    auto;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px;
  border: 1px solid #dedfdb;
  border-radius: 5px;
  background: #ffffff;
}

.log-filter-control {
  width: 100%;
  min-width: 0;
  height: 38px;
  padding: 0 11px;
  border: 1px solid #dfe0dc;
  border-radius: 5px;
  color: #424540;
  background: #ffffff;
  font-size: 12px;
}

.log-filter-control:focus {
  border-color: #9a9d96;
  outline: 0;
  box-shadow: 0 0 0 2px rgb(17 17 17 / 6%);
}

.log-filter-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.log-filter-button {
  height: 38px;
  padding: 0 14px;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 650;
  white-space: nowrap;
  cursor: pointer;
}

.log-filter-button.reset {
  border: 1px solid #dfe0dc;
  color: #555852;
  background: #ffffff;
}

.log-filter-button.reset:hover {
  border-color: #bfc1bb;
  background: #f6f6f3;
}

.log-filter-button.search {
  border: 1px solid #222320;
  color: #ffffff;
  background: #222320;
}

.log-filter-button.search:hover {
  background: #111211;
}

.log-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.log-kpi-card {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  min-height: 76px;
  padding: 12px 14px;
  border: 1px solid #e0e1dd;
  border-radius: 5px;
  background: #ffffff;
}

.log-kpi-icon {
  display: inline-grid;
  flex: 0 0 32px;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 50%;
  font-size: 11px;
  font-weight: 800;
}

.log-kpi-card span:not(.log-kpi-icon) {
  display: block;
  margin-bottom: 3px;
  color: #6f706b;
  font-size: 11px;
}

.log-kpi-card strong {
  font-size: 20px;
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
  height: 660px;
  overflow-x: auto;
  overflow-y: hidden;
  border: 1px solid #deddd8;
  border-radius: 6px;
  background: #ffffff;
}

.log-table {
  width: 100%;
  min-width: 900px;
  table-layout: fixed;
}

.log-table th:nth-child(1) { width: 8%; }
.log-table th:nth-child(2) { width: 31%; }
.log-table th:nth-child(3) { width: 13%; }
.log-table th:nth-child(4) { width: 15%; }
.log-table th:nth-child(5) { width: 10%; }
.log-table th:nth-child(6) { width: 10%; }
.log-table th:nth-child(7) { width: 7%; }
.log-table th:nth-child(8) { width: 6%; }

.log-table thead th {
  padding: 10px;
  border-bottom: 1px solid #deddd8;
  color: #666660;
  background: #f7f7f5;
  font-size: 11px;
  white-space: nowrap;
}

.log-table tbody td {
  height: 58px;
  overflow: hidden;
  padding: 10px;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.log-table tbody tr {
  height: 58px;
}

.log-table tbody td strong,
.log-table .secondary-text {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.drawer-layer {
  position: fixed;
  z-index: 1050;
  inset: 0;
  background: rgb(17 17 17 / 5%);
}

.log-drawer {
  position: absolute;
  top: 0;
  right: 0;
  width: min(360px, 100%);
  height: 100%;
  overflow-y: auto;
  border-left: 1px solid #deddd8;
  background: #ffffff;
  box-shadow: -8px 0 24px rgb(17 17 17 / 8%);
}

.drawer-header {
  position: sticky;
  z-index: 2;
  top: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 17px 18px;
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
  padding: 18px;
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

.email-failure-section {
  margin-top: 16px;
}

.email-failure-details {
  overflow: hidden;
  border: 1px solid #ead8d5;
  border-radius: 5px;
  background: #ffffff;
}

.email-failure-summary {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 11px 13px;
  color: #704e4a;
  background: #f8efed;
  font-size: 12px;
  cursor: pointer;
  list-style: none;
}

.email-failure-summary::-webkit-details-marker {
  display: none;
}

.email-failure-icon {
  display: inline-grid;
  width: 22px;
  height: 22px;
  flex: 0 0 22px;
  place-items: center;
  border-radius: 50%;
  color: #ffffff;
  background: #b66f68;
  font-size: 11px;
  font-weight: 800;
}

.email-failure-title {
  font-weight: 700;
}

.email-failure-count {
  display: inline-grid;
  min-width: 20px;
  height: 18px;
  padding: 0 5px;
  place-items: center;
  border-radius: 9px;
  color: #ffffff;
  background: #b66f68;
  font-size: 10px;
}

.email-failure-toggle {
  margin-left: auto;
  color: #8c716d;
  font-size: 10px;
}

.email-failure-details[open]
  .email-failure-toggle::after {
  content: "（收合）";
}

.email-failure-content {
  padding: 12px;
  border-top: 1px solid #ead8d5;
}

.email-failure-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
}

.email-failure-toolbar p {
  margin: 0;
  color: #77736f;
  font-size: 11px;
}

.email-refresh-button,
.email-retry-button,
.email-dismiss-button,
.email-job-pagination button {
  height: 30px;
  padding: 0 10px;
  border: 1px solid #d9d8d3;
  border-radius: 4px;
  color: #4c4f49;
  background: #ffffff;
  font-size: 11px;
  font-weight: 650;
  cursor: pointer;
}

.email-refresh-button:hover,
.email-retry-button:hover:not(:disabled),
.email-dismiss-button:hover,
.email-job-pagination button:hover:not(:disabled) {
  background: #f3f3f0;
}

.email-retry-button:disabled,
.email-job-pagination button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.email-job-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.email-dismiss-button {
  color: #765b58;
  border-color: #dec9c6;
  background: #fff9f8;
}

.email-error-message {
  margin: 0 0 10px;
  color: #a85e57;
  font-size: 11px;
}

.email-job-table-wrapper {
  overflow-x: auto;
  border: 1px solid #e5e4e0;
  border-radius: 4px;
}

.email-job-table {
  min-width: 720px;
  margin-bottom: 0;
}

.email-job-table thead th {
  padding: 9px 10px;
  color: #6c7069;
  background: #f7f7f5;
  font-size: 10px;
  white-space: nowrap;
}

.email-job-table tbody td {
  padding: 9px 10px;
  border-bottom: 1px solid #ecece9;
  font-size: 10px;
}

.email-error-cell {
  max-width: 320px;
  overflow: hidden;
  color: #8d5c57;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.email-job-pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
  color: #747870;
  font-size: 10px;
}

.log-pagination {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}

.log-pagination .pagination {
  gap: 4px;
  margin: 0;
}

.log-pagination .page-link {
  min-width: 32px;
  border-color: #dedfdb;
  color: #565a53;
  background: #ffffff;
  font-size: 11px;
  text-align: center;
}

.log-pagination .page-item.active .page-link {
  border-color: #292b28;
  color: #ffffff;
  background: #292b28;
}

@media (max-width: 640px) {
  .email-failure-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .email-refresh-button {
    width: 100%;
  }
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

@media (max-width: 720px) {
  .log-table-wrapper {
    height: auto;
    min-height: 0;
    overflow-y: visible;
  }

  .log-filter {
    grid-template-columns: 1fr;
  }

  .log-filter-actions {
    justify-content: stretch;
  }

  .log-filter-button {
    flex: 1;
  }
}
</style>
