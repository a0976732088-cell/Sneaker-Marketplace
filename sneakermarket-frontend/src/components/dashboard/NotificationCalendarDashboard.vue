<template>
  <section class="dashboard-page">
    <div
      v-if="loading && !hasLoaded"
      class="dashboard-message"
      role="status"
    >
      Dashboard 資料載入中……
    </div>

    <div
      v-else-if="errorMessage"
      class="dashboard-message dashboard-error"
    >
      <span>{{ errorMessage }}</span>

      <button
        type="button"
        class="retry-button"
        @click="loadDashboard"
      >
        重新載入
      </button>
    </div>

    <template v-else>
      <!-- 異常提示 -->
      <section
        v-if="
          Number(dashboard.failedJobs || 0) > 0 ||
          Number(dashboard.cancelledEvents || 0) > 0
        "
        class="operation-alert"
      >
        <div>
          <span class="alert-icon">!</span>

          <strong>
            <template v-if="Number(dashboard.failedJobs || 0) > 0">
              {{ formatNumber(dashboard.failedJobs) }}
              則通知發送失敗
            </template>

            <template
              v-if="
                Number(dashboard.failedJobs || 0) > 0 &&
                Number(dashboard.cancelledEvents || 0) > 0
              "
            >
              ・
            </template>

            <template
              v-if="Number(dashboard.cancelledEvents || 0) > 0"
            >
              {{ formatNumber(dashboard.cancelledEvents) }}
              場活動已取消
            </template>
          </strong>
        </div>

        <button
          type="button"
          class="alert-link"
          @click="goTo('NotificationLog')"
        >
          查看待處理 →
        </button>
      </section>

      <!-- KPI -->
      <section class="metric-grid">
        <article class="metric-card metric-blue">
          <div class="metric-heading">
            <span class="metric-icon">曆</span>
            <span>活動總數</span>
          </div>

          <strong class="metric-value">
            {{ formatNumber(dashboard.totalEvents) }}
          </strong>

          <p>平台建立的所有活動</p>

          <div class="metric-sparkline sparkline-blue">
            <i></i>
            <i></i>
            <i></i>
            <i></i>
            <i></i>
            <i></i>
            <i></i>
            <i></i>
          </div>
        </article>

        <article class="metric-card metric-green">
          <div class="metric-heading">
            <span class="metric-icon">▶</span>
            <span>即將開始</span>
          </div>

          <strong class="metric-value">
            {{ formatNumber(dashboard.upcomingEvents) }}
          </strong>

          <p>未來即將開始的有效活動</p>

          <div class="metric-sparkline sparkline-green">
            <i></i>
            <i></i>
            <i></i>
            <i></i>
            <i></i>
            <i></i>
            <i></i>
            <i></i>
          </div>
        </article>

        <article class="metric-card metric-purple">
          <div class="metric-heading">
            <span class="metric-icon">關</span>
            <span>會員關注</span>
          </div>

          <strong class="metric-value">
            {{ formatNumber(dashboard.totalFollows) }}
          </strong>

          <p>目前累積的活動關注數</p>

          <div class="metric-sparkline sparkline-purple">
            <i></i>
            <i></i>
            <i></i>
            <i></i>
            <i></i>
            <i></i>
            <i></i>
            <i></i>
          </div>
        </article>

        <article class="metric-card metric-red">
          <div class="metric-heading">
            <span class="metric-icon">送</span>
            <span>通知成功率</span>
          </div>

          <strong class="metric-value">
            {{ formatPercent(dashboard.deliveryRate) }}
          </strong>

          <p>
            累積成功送達
            {{ formatNumber(dashboard.totalSent) }}
            人次
          </p>

          <div class="metric-sparkline sparkline-red">
            <i></i>
            <i></i>
            <i></i>
            <i></i>
            <i></i>
            <i></i>
            <i></i>
            <i></i>
          </div>
        </article>
      </section>

      <!-- 中間圖表 -->
      <section class="dashboard-grid dashboard-grid-main">
        <!-- 活動趨勢 -->
        <article class="dashboard-panel trend-panel">
          <header class="panel-header">
            <div>
              <h2>活動趨勢</h2>
              <p>最近 30 天活動建立數與會員關注數</p>
            </div>
          </header>

          <div class="chart-area chart-area-line">
            <canvas ref="activityTrendCanvas"></canvas>
          </div>
        </article>

        <!-- 活動狀態 -->
        <article class="dashboard-panel status-panel">
          <header class="panel-header">
            <div>
              <h2>活動狀態</h2>
              <p>目前各活動狀態分布</p>
            </div>
          </header>

          <div class="status-content">
            <div class="doughnut-wrapper">
              <canvas ref="eventStatusCanvas"></canvas>

              <div class="doughnut-center">
                <strong>
                  {{ formatNumber(dashboard.totalEvents) }}
                </strong>
                <span>場活動</span>
              </div>
            </div>

            <ul class="status-legend">
              <li
                v-for="item in eventStatusItems"
                :key="item.label"
              >
                <span
                  class="legend-dot"
                  :style="{ backgroundColor: item.color }"
                ></span>

                <span class="legend-label">
                  {{ item.label }}
                </span>

                <strong>
                  {{ formatNumber(item.value) }}
                </strong>
              </li>

              <li v-if="otherEventCount > 0">
                <span
                  class="legend-dot"
                  style="background-color: #deddd8"
                ></span>

                <span class="legend-label">其他</span>

                <strong>
                  {{ formatNumber(otherEventCount) }}
                </strong>
              </li>
            </ul>
          </div>

          <button
            type="button"
            class="panel-link"
            @click="goTo('search')"
          >
            查看全部活動 →
          </button>
        </article>
      </section>

      <!-- 下方圖表 -->
      <section class="dashboard-grid dashboard-grid-bottom">
        <!-- 通知發送成效 -->
        <article class="dashboard-panel delivery-panel">
          <header class="panel-header">
            <div>
              <h2>通知發送成效</h2>
              <p>最近四週成功、處理中與失敗人次</p>
            </div>

            <span class="panel-summary">
              共 {{ formatNumber(totalJobs) }} 筆任務
            </span>
          </header>

          <div class="chart-area chart-area-delivery">
            <canvas ref="deliveryTrendCanvas"></canvas>
          </div>

          <button
            type="button"
            class="panel-link"
            @click="goTo('NotificationLog')"
          >
            查看發送紀錄 →
          </button>
        </article>

        <!-- 近七天排程 -->
        <article class="dashboard-panel schedule-panel">
          <header class="panel-header">
            <div>
              <h2>近 7 天排程</h2>
              <p>即將開始的活動</p>
            </div>

            <div class="schedule-key">
              <span>
                <i class="schedule-event-dot"></i>
                活動
              </span>
            </div>
          </header>

          <div
            v-if="dashboard.upcomingSchedule?.length"
            class="schedule-grid"
          >
            <article
              v-for="day in dashboard.upcomingSchedule"
              :key="day.date"
              class="schedule-day"
            >
              <header>
                <strong>{{ formatShortDate(day.date) }}</strong>
                <span>{{ formatWeekday(day.date) }}</span>
              </header>

              <div class="schedule-items">
                <button
                  v-for="item in day.items"
                  :key="`${item.type}-${item.id}`"
                  type="button"
                  class="schedule-item schedule-event"
                  :title="
                    `${formatTime(item.scheduledAt)} ${item.title}`
                  "
                  @click="goTo('search')"
                >
                  <span class="schedule-item-dot"></span>

                  <span class="schedule-item-time">
                    {{ formatTime(item.scheduledAt) }}
                  </span>

                  <span class="schedule-item-title">
                    {{ item.title }}
                  </span>
                </button>

                <span
                  v-if="!day.items?.length"
                  class="schedule-empty"
                >
                  —
                </span>
              </div>
            </article>
          </div>

          <div v-else class="empty-state">
            近七天沒有活動排程。
          </div>
        </article>
      </section>
    </template>
  </section>
</template>

<script setup>
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
} from "vue";
import Chart from "chart.js/auto";

import {
  getNotificationCalendarDashboard,
} from "@/service/notificationService";

const emit = defineEmits(["navigate"]);

const loading = ref(false);
const hasLoaded = ref(false);
const errorMessage = ref("");

const activityTrendCanvas = ref(null);
const eventStatusCanvas = ref(null);
const deliveryTrendCanvas = ref(null);

let activityTrendChart = null;
let eventStatusChart = null;
let deliveryTrendChart = null;

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

  deliveryRate: 0,

  activeEvents: 0,
  endedEvents: 0,
  draftEvents: 0,

  activityTrend: [],
  notificationDeliveryTrend: [],
  upcomingSchedule: [],
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

  const known =
    Number(dashboard.value.upcomingEvents || 0) +
    Number(dashboard.value.activeEvents || 0) +
    Number(dashboard.value.endedEvents || 0) +
    Number(dashboard.value.draftEvents || 0) +
    Number(dashboard.value.cancelledEvents || 0);

  return Math.max(total - known, 0);
});

const eventStatusItems = computed(() => [
  {
    label: "即將開始",
    value: Number(dashboard.value.upcomingEvents || 0),
    color: "#86a281",
  },
  {
    label: "進行中",
    value: Number(dashboard.value.activeEvents || 0),
    color: "#6f93bd",
  },
  {
    label: "已結束",
    value: Number(dashboard.value.endedEvents || 0),
    color: "#af7f9b",
  },
  {
    label: "草稿",
    value: Number(dashboard.value.draftEvents || 0),
    color: "#bfc4c8",
  },
  {
    label: "已取消",
    value: Number(dashboard.value.cancelledEvents || 0),
    color: "#c87c76",
  },
]);

function formatNumber(value) {
  const number = Number(value ?? 0);

  return Number.isFinite(number)
    ? number.toLocaleString("zh-TW")
    : "0";
}

function formatPercent(value) {
  const number = Number(value ?? 0);

  return Number.isFinite(number)
    ? `${number.toFixed(1)}%`
    : "0.0%";
}

function formatShortDate(dateString) {
  if (!dateString) return "";

  const date = new Date(`${dateString}T00:00:00`);

  return new Intl.DateTimeFormat("zh-TW", {
    month: "numeric",
    day: "numeric",
  }).format(date);
}

function formatWeekday(dateString) {
  if (!dateString) return "";

  const date = new Date(`${dateString}T00:00:00`);

  return new Intl.DateTimeFormat("zh-TW", {
    weekday: "short",
  }).format(date);
}

function formatTime(dateTimeString) {
  if (!dateTimeString) return "";

  return new Intl.DateTimeFormat("zh-TW", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(new Date(dateTimeString));
}

function percentage(value, total) {
  const safeValue = Number(value || 0);
  const safeTotal = Number(total || 0);

  if (safeTotal <= 0) return "0%";

  return `${Math.min(
    Math.round((safeValue / safeTotal) * 100),
    100,
  )}%`;
}

function destroyCharts() {
  activityTrendChart?.destroy();
  eventStatusChart?.destroy();
  deliveryTrendChart?.destroy();

  activityTrendChart = null;
  eventStatusChart = null;
  deliveryTrendChart = null;
}

function renderActivityTrendChart() {
  if (!activityTrendCanvas.value) return;

  activityTrendChart?.destroy();

  const trend = dashboard.value.activityTrend || [];

  activityTrendChart = new Chart(
    activityTrendCanvas.value,
    {
      type: "line",

      data: {
        labels: trend.map((item) =>
          formatShortDate(item.date),
        ),

        datasets: [
          {
            label: "活動數",
            data: trend.map((item) =>
              Number(item.eventCount || 0),
            ),
            borderColor: "#5f88b8",
            backgroundColor: "rgb(95 136 184 / 12%)",
            pointBackgroundColor: "#5f88b8",
            pointRadius: 2.5,
            pointHoverRadius: 5,
            borderWidth: 2,
            tension: 0.35,
            fill: true,
          },
          {
            label: "會員關注",
            data: trend.map((item) =>
              Number(item.followCount || 0),
            ),
            borderColor: "#7c9b77",
            backgroundColor: "rgb(124 155 119 / 10%)",
            pointBackgroundColor: "#7c9b77",
            pointRadius: 2.5,
            pointHoverRadius: 5,
            borderWidth: 2,
            tension: 0.35,
            fill: true,
          },
        ],
      },

      options: {
        responsive: true,
        maintainAspectRatio: false,

        interaction: {
          mode: "index",
          intersect: false,
        },

        plugins: {
          legend: {
            position: "top",
            align: "end",

            labels: {
              usePointStyle: true,
              boxWidth: 8,
              boxHeight: 8,
              color: "#666660",
              font: {
                size: 11,
              },
            },
          },
        },

        scales: {
          x: {
            grid: {
              color: "#ecebe7",
            },

            ticks: {
              color: "#777771",
              maxTicksLimit: 10,
              font: {
                size: 10,
              },
            },
          },

          y: {
            beginAtZero: true,

            grid: {
              color: "#ecebe7",
            },

            ticks: {
              color: "#777771",
              precision: 0,
              font: {
                size: 10,
              },
            },
          },
        },
      },
    },
  );
}

function renderEventStatusChart() {
  if (!eventStatusCanvas.value) return;

  eventStatusChart?.destroy();

  const items = eventStatusItems.value;

  eventStatusChart = new Chart(
    eventStatusCanvas.value,
    {
      type: "doughnut",

      data: {
        labels: items.map((item) => item.label),

        datasets: [
          {
            data: items.map((item) => item.value),
            backgroundColor: items.map(
              (item) => item.color,
            ),
            borderWidth: 0,
            hoverOffset: 4,
          },
        ],
      },

      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: "66%",

        plugins: {
          legend: {
            display: false,
          },

          tooltip: {
            callbacks: {
              label(context) {
                return `${context.label}：${formatNumber(
                  context.raw,
                )}`;
              },
            },
          },
        },
      },
    },
  );
}

function renderDeliveryTrendChart() {
  if (!deliveryTrendCanvas.value) return;

  deliveryTrendChart?.destroy();

  const trend =
    dashboard.value.notificationDeliveryTrend || [];

  deliveryTrendChart = new Chart(
    deliveryTrendCanvas.value,
    {
      type: "bar",

      data: {
        labels: trend.map((item) => item.label),

        datasets: [
          {
            label: "完成",
            data: trend.map((item) =>
              Number(item.completed || 0),
            ),
            backgroundColor: "#86a281",
            borderRadius: 3,
            barThickness: 20,
          },
          {
            label: "處理中",
            data: trend.map((item) =>
              Number(item.processing || 0),
            ),
            backgroundColor: "#6f93bd",
            borderRadius: 3,
            barThickness: 20,
          },
          {
            label: "失敗",
            data: trend.map((item) =>
              Number(item.failed || 0),
            ),
            backgroundColor: "#c87c76",
            borderRadius: 3,
            barThickness: 20,
          },
        ],
      },

      options: {
        indexAxis: "y",
        responsive: true,
        maintainAspectRatio: false,

        plugins: {
          legend: {
            position: "top",
            align: "end",

            labels: {
              usePointStyle: true,
              boxWidth: 8,
              boxHeight: 8,
              color: "#666660",
              font: {
                size: 11,
              },
            },
          },
        },

        scales: {
          x: {
            stacked: true,
            beginAtZero: true,

            grid: {
              color: "#ecebe7",
            },

            ticks: {
              color: "#777771",
              precision: 0,
            },
          },

          y: {
            stacked: true,

            grid: {
              display: false,
            },

            ticks: {
              color: "#666660",
              font: {
                size: 11,
              },
            },
          },
        },
      },
    },
  );
}

async function renderCharts() {
  await nextTick();

  renderActivityTrendChart();
  renderEventStatusChart();
  renderDeliveryTrendChart();
}

async function loadDashboard() {
  loading.value = true;
  errorMessage.value = "";

  try {
    const response =
      await getNotificationCalendarDashboard();

    dashboard.value = {
      ...dashboard.value,
      ...(response.data || {}),
    };

    hasLoaded.value = true;

    await renderCharts();
  } catch (error) {
    console.error(
      "取得 Dashboard 資料失敗：",
      error,
    );

    errorMessage.value =
      error.response?.data?.message ||
      error.response?.data?.detail ||
      "無法取得 Dashboard 資料";
  } finally {
    loading.value = false;
  }
}

function goTo(target) {
  emit("navigate", target);
}

onMounted(() => {
  loadDashboard();
});

onBeforeUnmount(() => {
  destroyCharts();
});
</script>

<style scoped>
.dashboard-page {
  --dashboard-line: #deddd8;
  --dashboard-muted: #777771;
  --dashboard-paper: #ffffff;
  --dashboard-soft: #f7f7f5;
  --dashboard-black: #161816;

  width: 100%;
  min-width: 0;
  color: var(--dashboard-black);
}

.dashboard-message {
  padding: 24px;
  border: 1px solid var(--dashboard-line);
  border-radius: 6px;
  color: #666660;
  background: var(--dashboard-soft);
}

.dashboard-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: #744f4b;
  background: #f5e8e6;
}

.retry-button {
  padding: 8px 14px;
  border: 1px solid #744f4b;
  border-radius: 4px;
  color: #744f4b;
  background: transparent;
}

/* 異常提示 */

.operation-alert {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
  padding: 13px 16px;
  border: 1px solid #ead1ce;
  border-radius: 6px;
  color: #603f3b;
  background: #f8eae8;
  font-size: 13px;
}

.operation-alert > div {
  display: flex;
  align-items: center;
  gap: 10px;
}

.alert-icon {
  display: inline-grid;
  width: 22px;
  height: 22px;
  place-items: center;
  border-radius: 50%;
  color: #ffffff;
  background: #b95f58;
  font-weight: 800;
}

.alert-link {
  padding: 0;
  border: 0;
  color: #603f3b;
  background: transparent;
  font-weight: 700;
}

/* KPI */

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.metric-card {
  position: relative;
  min-width: 0;
  min-height: 108px;
  overflow: hidden;
  padding: 13px;
  border: 1px solid #e1e2de;
  border-radius: 5px;
  background: #ffffff;
}

.metric-heading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #50534e;
  font-size: 11px;
  font-weight: 700;
}

.metric-icon {
  display: inline-grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 50%;
  font-size: 11px;
}

.metric-blue .metric-icon {
  color: #476889;
  background: #e4edf6;
}

.metric-green .metric-icon {
  color: #587353;
  background: #e3ede1;
}

.metric-purple .metric-icon {
  color: #75566a;
  background: #eee2e9;
}

.metric-red .metric-icon {
  color: #9a4c47;
  background: #f5dedd;
}

.metric-value {
  display: block;
  margin: 6px 0 0 38px;
  font-size: 23px;
  font-weight: 800;
  letter-spacing: -0.04em;
  line-height: 1;
}

.metric-card p {
  margin: 4px 0 0 38px;
  color: #858881;
  font-size: 9px;
}

.metric-sparkline {
  position: absolute;
  right: 14px;
  bottom: 12px;
  left: 14px;
  display: flex;
  align-items: end;
  gap: 4px;
  height: 28px;
  opacity: 0.72;
}

.metric-sparkline i {
  flex: 1;
  border-radius: 2px 2px 0 0;
}

.metric-sparkline i:nth-child(1) {
  height: 32%;
}

.metric-sparkline i:nth-child(2) {
  height: 45%;
}

.metric-sparkline i:nth-child(3) {
  height: 28%;
}

.metric-sparkline i:nth-child(4) {
  height: 58%;
}

.metric-sparkline i:nth-child(5) {
  height: 42%;
}

.metric-sparkline i:nth-child(6) {
  height: 70%;
}

.metric-sparkline i:nth-child(7) {
  height: 54%;
}

.metric-sparkline i:nth-child(8) {
  height: 88%;
}

.sparkline-blue i {
  background: #7ea2ca;
}

.sparkline-green i {
  background: #8aaa84;
}

.sparkline-purple i {
  background: #b0839f;
}

.sparkline-red i {
  background: #ce7771;
}

/* 面板 */

.dashboard-grid {
  display: grid;
  gap: 10px;
  margin-bottom: 10px;
}

.dashboard-grid-main {
  grid-template-columns: minmax(0, 1.55fr) minmax(260px, 1fr);
}

.dashboard-grid-bottom {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.dashboard-panel {
  min-width: 0;
  padding: 13px;
  border: 1px solid #e1e2de;
  border-radius: 5px;
  background: #ffffff;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.panel-header h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 800;
}

.panel-header p {
  margin: 4px 0 0;
  color: var(--dashboard-muted);
  font-size: 11px;
}

.panel-summary {
  padding: 5px 9px;
  border-radius: 999px;
  color: #555550;
  background: #efefec;
  font-size: 11px;
  white-space: nowrap;
}

.chart-area {
  position: relative;
  width: 100%;
}

.chart-area-line {
  height: 210px;
}

.chart-area-delivery {
  height: 175px;
}

/* Doughnut */

.status-content {
  display: grid;
  grid-template-columns: minmax(150px, 1fr) minmax(120px, 0.8fr);
  align-items: center;
  gap: 16px;
}

.doughnut-wrapper {
  position: relative;
  height: 175px;
}

.doughnut-center {
  position: absolute;
  top: 50%;
  left: 50%;
  display: grid;
  transform: translate(-50%, -50%);
  text-align: center;
  pointer-events: none;
}

.doughnut-center strong {
  font-size: 25px;
  line-height: 1;
}

.doughnut-center span {
  margin-top: 5px;
  color: var(--dashboard-muted);
  font-size: 11px;
}

.status-legend {
  margin: 0;
  padding: 0;
  list-style: none;
}

.status-legend li {
  display: grid;
  grid-template-columns: 10px 1fr auto;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 12px;
}

.legend-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
}

.legend-label {
  color: #555550;
}

.panel-link {
  margin-top: 12px;
  padding: 0;
  border: 0;
  color: #2d6598;
  background: transparent;
  font-size: 12px;
  font-weight: 700;
}

/* 排程 */

.schedule-key {
  color: var(--dashboard-muted);
  font-size: 11px;
}

.schedule-key span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.schedule-event-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #668db8;
}

.schedule-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(70px, 1fr));
  min-height: 225px;
  overflow-x: auto;
  border-top: 1px solid var(--dashboard-line);
  border-left: 1px solid var(--dashboard-line);
}

.schedule-day {
  min-width: 76px;
  border-right: 1px solid var(--dashboard-line);
  border-bottom: 1px solid var(--dashboard-line);
}

.schedule-day > header {
  display: grid;
  gap: 2px;
  padding: 9px 6px;
  border-bottom: 1px solid var(--dashboard-line);
  text-align: center;
}

.schedule-day > header strong {
  font-size: 11px;
}

.schedule-day > header span {
  color: var(--dashboard-muted);
  font-size: 10px;
}

.schedule-items {
  display: grid;
  align-content: start;
  gap: 6px;
  min-height: 170px;
  padding: 9px 6px;
}

.schedule-item {
  display: grid;
  grid-template-columns: 7px 1fr;
  gap: 2px 5px;
  width: 100%;
  padding: 6px;
  border: 0;
  border-radius: 4px;
  color: #36546f;
  background: #e6eef6;
  text-align: left;
}

.schedule-item-dot {
  width: 7px;
  height: 7px;
  margin-top: 3px;
  border-radius: 50%;
  background: #668db8;
}

.schedule-item-time {
  font-size: 9px;
  font-weight: 700;
}

.schedule-item-title {
  grid-column: 2;
  overflow: hidden;
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-empty {
  color: #c1c1bc;
  text-align: center;
}

.empty-state {
  display: grid;
  min-height: 225px;
  place-items: center;
  border: 1px dashed var(--dashboard-line);
  color: var(--dashboard-muted);
  font-size: 12px;
}

/* RWD */

@media (max-width: 1100px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-grid-main,
  .dashboard-grid-bottom {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .operation-alert,
  .panel-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .status-content {
    grid-template-columns: 1fr;
  }

  .status-legend {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 8px 16px;
  }

  .status-legend li {
    margin: 0;
  }
}
</style>