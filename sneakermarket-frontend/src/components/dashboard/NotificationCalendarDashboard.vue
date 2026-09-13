<template>
  <section class="dashboard-page">
    <!-- 第一次載入 -->
    <div v-if="loading && !hasLoaded" class="dashboard-message">
      Dashboard 資料載入中...
    </div>

    <!-- 載入錯誤 -->
    <div v-else-if="errorMessage" class="dashboard-message dashboard-error">
      <span>{{ errorMessage }}</span>

      <button
        type="button"
        class="btn btn-sm btn-danger"
        @click="loadDashboard"
      >
        重新載入
      </button>
    </div>

    <template v-else>
      <!-- 四個核心數字 -->
      <section class="dashboard-section">
        <div class="section-heading">
          <div>
            <span class="section-eyebrow">OVERVIEW</span>
            <h3>核心數據</h3>
          </div>

          <span class="update-status"> 即時資料 </span>
        </div>

        <div class="metric-grid">
          <article class="metric-card metric-blue">
            <div class="metric-top">
              <span class="metric-icon">活</span>
              <span class="metric-label">活動總數</span>
            </div>

            <strong>
              {{ formatNumber(dashboard.totalEvents) }}
            </strong>

            <p>目前建立的所有活動</p>
          </article>

          <article class="metric-card metric-green">
            <div class="metric-top">
              <span class="metric-icon">即</span>
              <span class="metric-label">即將開始</span>
            </div>

            <strong>
              {{ formatNumber(dashboard.upcomingEvents) }}
            </strong>

            <p>尚未開始的有效活動</p>
          </article>

          <article class="metric-card metric-purple">
            <div class="metric-top">
              <span class="metric-icon">關</span>
              <span class="metric-label">會員關注</span>
            </div>

            <strong>
              {{ formatNumber(dashboard.totalFollows) }}
            </strong>

            <p>目前有效的活動關注</p>
          </article>

          <article class="metric-card metric-orange">
            <div class="metric-top">
              <span class="metric-icon">送</span>
              <span class="metric-label">發送總人數</span>
            </div>

            <strong>
              {{ formatNumber(dashboard.totalSent) }}
            </strong>

            <p>批次任務成功發送人數</p>
          </article>
        </div>
      </section>

      <!-- 詳細狀態 -->
      <section class="overview-grid">
        <!-- 活動概況 -->
        <article class="overview-panel">
          <div class="panel-heading">
            <div>
              <span class="section-eyebrow">CALENDAR</span>
              <h3>活動概況</h3>
            </div>

            <span class="panel-total">
              共 {{ formatNumber(dashboard.totalEvents) }} 筆
            </span>
          </div>

          <div class="progress-item">
            <div class="progress-label">
              <span>即將開始</span>
              <strong>
                {{ formatNumber(dashboard.upcomingEvents) }}
              </strong>
            </div>

            <div class="progress-track">
              <div
                class="progress-fill progress-green"
                :style="{
                  width: percentage(
                    dashboard.upcomingEvents,
                    dashboard.totalEvents,
                  ),
                }"
              ></div>
            </div>
          </div>

          <div class="progress-item">
            <div class="progress-label">
              <span>已取消活動</span>
              <strong>
                {{ formatNumber(dashboard.cancelledEvents) }}
              </strong>
            </div>

            <div class="progress-track">
              <div
                class="progress-fill progress-red"
                :style="{
                  width: percentage(
                    dashboard.cancelledEvents,
                    dashboard.totalEvents,
                  ),
                }"
              ></div>
            </div>
          </div>

          <div class="progress-item">
            <div class="progress-label">
              <span>其他活動狀態</span>
              <strong>
                {{ formatNumber(otherEventCount) }}
              </strong>
            </div>

            <div class="progress-track">
              <div
                class="progress-fill progress-blue"
                :style="{
                  width: percentage(otherEventCount, dashboard.totalEvents),
                }"
              ></div>
            </div>
          </div>

          <button type="button" class="panel-link" @click="goTo('search')">
            查看全部活動 →
          </button>
        </article>

        <!-- 通知任務 -->
        <article class="overview-panel">
          <div class="panel-heading">
            <div>
              <span class="section-eyebrow">NOTIFICATION</span>
              <h3>通知任務</h3>
            </div>

            <span class="panel-total">
              共 {{ formatNumber(totalJobs) }} 筆
            </span>
          </div>

          <div class="notification-summary">
            <div>
              <span>通知資料</span>
              <strong>
                {{ formatNumber(dashboard.totalNotifications) }}
              </strong>
            </div>

            <div>
              <span>完成任務</span>
              <strong>
                {{ formatNumber(dashboard.completedJobs) }}
              </strong>
            </div>
          </div>

          <div class="job-status-grid">
            <div class="job-status status-completed">
              <span class="status-dot"></span>

              <div>
                <strong>
                  {{ formatNumber(dashboard.completedJobs) }}
                </strong>
                <span>已完成</span>
              </div>
            </div>

            <div class="job-status status-pending">
              <span class="status-dot"></span>

              <div>
                <strong>
                  {{ formatNumber(dashboard.pendingJobs) }}
                </strong>
                <span>等待處理</span>
              </div>
            </div>

            <div class="job-status status-processing">
              <span class="status-dot"></span>

              <div>
                <strong>
                  {{ formatNumber(dashboard.processingJobs) }}
                </strong>
                <span>處理中</span>
              </div>
            </div>

            <div class="job-status status-failed">
              <span class="status-dot"></span>

              <div>
                <strong>
                  {{ formatNumber(dashboard.failedJobs) }}
                </strong>
                <span>失敗任務</span>
              </div>
            </div>
          </div>

          <button
            type="button"
            class="panel-link"
            @click="goTo('NotificationLog')"
          >
            查看通知發送紀錄 →
          </button>
        </article>
      </section>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import httpClient from "@/service/httpClient";

const emit = defineEmits(["navigate"]);

const loading = ref(false);
const hasLoaded = ref(false);
const errorMessage = ref("");

const dashboard = ref({
  totalEvents: 0,
  upcomingEvents: 0,
  cancelledEvents: 0,
  totalFollows: 0,

  totalNotifications: 0,
  totalSent: 0,
  pendingJobs: 0,
  processingJobs: 0,
  completedJobs: 0,
  failedJobs: 0,
});

const totalJobs = computed(() => {
  return (
    Number(dashboard.value.pendingJobs || 0) +
    Number(dashboard.value.processingJobs || 0) +
    Number(dashboard.value.completedJobs || 0) +
    Number(dashboard.value.failedJobs || 0)
  );
});

const otherEventCount = computed(() => {
  const total = Number(dashboard.value.totalEvents || 0);
  const upcoming = Number(dashboard.value.upcomingEvents || 0);
  const cancelled = Number(dashboard.value.cancelledEvents || 0);

  return Math.max(total - upcoming - cancelled, 0);
});

async function loadDashboard() {
  loading.value = true;
  errorMessage.value = "";

  try {
    const response = await httpClient.get("/dashboard/notification-calendar");

    dashboard.value = {
      ...dashboard.value,
      ...response.data,
    };

    hasLoaded.value = true;
  } catch (error) {
    console.error("取得 Dashboard 資料失敗：", error);

    errorMessage.value =
      error.response?.data?.message ||
      error.response?.data?.detail ||
      "無法取得 Dashboard 資料";
  } finally {
    loading.value = false;
  }
}

function formatNumber(value) {
  const number = Number(value ?? 0);

  return Number.isFinite(number) ? number.toLocaleString("zh-TW") : "0";
}

function percentage(value, total) {
  const safeValue = Number(value || 0);
  const safeTotal = Number(total || 0);

  if (safeTotal <= 0) {
    return "0%";
  }

  return `${Math.min(Math.round((safeValue / safeTotal) * 100), 100)}%`;
}

function goTo(tab) {
  emit("navigate", tab);
}

onMounted(() => {
  loadDashboard();
});
</script>

<style scoped>
.dashboard-page {
  min-height: 0;
  padding: 0;
  color: var(--admin-text);
  background: transparent;
}

.dashboard-section {
  margin-bottom: 1rem;
}

.section-heading,
.panel-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 1rem;
  margin-bottom: 0.9rem;
}

.section-eyebrow {
  display: block;
  margin-bottom: 0.25rem;
  color: var(--admin-muted);
  font-size: 0.67rem;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.section-heading h3,
.panel-heading h3 {
  margin: 0;
  color: var(--admin-black);
  font-size: 1.15rem;
  font-weight: 750;
}

.update-status,
.panel-total {
  padding: 0.38rem 0.7rem;
  border-radius: 999px;
  color: var(--admin-black);
  background: var(--admin-cement);
  font-size: 0.72rem;
  font-weight: 650;
}

/* 四個核心數據 */

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.8rem;
}

.metric-card {
  min-width: 0;
  min-height: 180px;
  padding: 1.25rem;
  border: 0;
  border-radius: 1rem;
  box-shadow: none;
  transition: transform 160ms ease;
}

.metric-card:hover {
  transform: translateY(-3px);
}

.metric-blue {
  color: #ffffff;
  background: var(--admin-black);
}

.metric-green {
  background: var(--admin-sage);
}

.metric-purple {
  background: var(--admin-mauve);
}

.metric-orange {
  background: var(--admin-clay);
}

.metric-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.7rem;
  margin-bottom: 2rem;
}

.metric-icon {
  display: grid;
  width: 2.2rem;
  height: 2.2rem;
  place-items: center;
  border-radius: 50%;
  font-size: 0.76rem;
  font-weight: 750;
}

.metric-blue .metric-icon {
  color: var(--admin-black);
  background: #ffffff;
}

.metric-green .metric-icon,
.metric-purple .metric-icon,
.metric-orange .metric-icon {
  color: var(--admin-black);
  background: rgb(255 255 255 / 45%);
}

.metric-label {
  color: #454640;
  font-size: 0.8rem;
  font-weight: 650;
}

.metric-blue .metric-label {
  color: #d8d8d3;
}

.metric-card strong {
  display: block;
  margin-bottom: 0.5rem;
  color: var(--admin-black);
  font-size: 2.5rem;
  font-weight: 750;
  letter-spacing: -0.05em;
  line-height: 1;
}

.metric-blue strong {
  color: #ffffff;
}

.metric-card p {
  margin: 0;
  color: #55564f;
  font-size: 0.75rem;
}

.metric-blue p {
  color: #aaaaa5;
}

/* 下方概況 */

.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
}

.overview-panel {
  min-width: 0;
  padding: 1.35rem;
  border: 0;
  border-radius: 1rem;
  background: var(--admin-cement);
}

/* 活動比例 */

.progress-item {
  margin-bottom: 1rem;
}

.progress-label {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.45rem;
  color: var(--admin-text-soft);
  font-size: 0.78rem;
}

.progress-track {
  height: 0.42rem;
  overflow: hidden;
  border-radius: 999px;
  background: #d1d0ca;
}

.progress-fill {
  height: 100%;
  border-radius: inherit;
}

.progress-green {
  background: var(--admin-sage-dark);
}

.progress-red {
  background: var(--admin-rose-dark);
}

.progress-blue {
  background: var(--admin-blue-gray-dark);
}

/* 通知摘要 */

.notification-summary {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.65rem;
  margin-bottom: 0.75rem;
}

.notification-summary div {
  padding: 0.9rem;
  border-radius: 0.8rem;
  background: var(--admin-white);
}

.notification-summary span,
.notification-summary strong {
  display: block;
}

.notification-summary span {
  margin-bottom: 0.25rem;
  color: var(--admin-muted);
  font-size: 0.72rem;
}

.notification-summary strong {
  color: var(--admin-black);
  font-size: 1.4rem;
  letter-spacing: -0.03em;
}

/* 任務狀態 */

.job-status-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.6rem;
}

.job-status {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.75rem;
  border-radius: 0.8rem;
  background: rgb(255 255 255 / 55%);
}

.status-dot {
  flex: 0 0 auto;
  width: 0.55rem;
  height: 0.55rem;
  border-radius: 50%;
}

.status-completed .status-dot {
  background: var(--admin-sage-dark);
}

.status-pending .status-dot {
  background: var(--admin-clay-dark);
}

.status-processing .status-dot {
  background: var(--admin-mauve-dark);
}

.status-failed .status-dot {
  background: var(--admin-rose-dark);
}

.job-status strong,
.job-status span {
  display: block;
}

.job-status strong {
  color: var(--admin-black);
  line-height: 1.1;
}

.job-status div span {
  margin-top: 0.12rem;
  color: var(--admin-muted);
  font-size: 0.7rem;
}

.panel-link {
  margin-top: 1rem;
  padding: 0;
  border: 0;
  color: var(--admin-black);
  background: transparent;
  font-size: 0.78rem;
  font-weight: 700;
}

.panel-link:hover {
  text-decoration: underline;
}

.dashboard-message {
  padding: 1rem;
  border-radius: 0.8rem;
  color: var(--admin-text-soft);
  background: var(--admin-cement);
}

.dashboard-error {
  color: #76524e;
  background: #eee3e1;
}

@media (max-width: 992px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 576px) {
  .metric-grid,
  .job-status-grid {
    grid-template-columns: 1fr;
  }

  .metric-card {
    min-height: 150px;
  }
}
</style>
