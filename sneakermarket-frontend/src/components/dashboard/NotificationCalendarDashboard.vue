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
        v-if="hasUnacknowledgedFailedJobs"
        class="operation-alert"
      >
        <div>
          <span class="alert-icon">!</span>

          <strong>
            {{ formatNumber(dashboard.failedJobs) }}
            則通知發送失敗
          </strong>
        </div>

        <div class="alert-actions">
          <button
            type="button"
            class="alert-link"
            @click="goTo('NotificationLog')"
          >
            查看待處理 →
          </button>

          <button
            type="button"
            class="alert-acknowledge"
            title="僅在此瀏覽器隱藏目前提醒"
            @click="acknowledgeFailedJobs"
          >
            已了解
          </button>
        </div>
      </section>

      <!-- KPI -->
      <section class="metric-grid">
        <article class="metric-card metric-blue">
          <div class="metric-content">
            <span class="metric-icon">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M7 3v3M17 3v3M4 9h16M5 5h14a1 1 0 0 1 1 1v14H4V6a1 1 0 0 1 1-1Z" />
              </svg>
            </span>

            <div class="metric-copy">
              <div class="metric-heading">活動總數</div>
              <div class="metric-value-row">
                <strong class="metric-value">
                  {{ formatNumber(dashboard.totalEvents) }}
                </strong>
                <span
                  v-if="activityDelta !== null"
                  class="metric-delta"
                  :class="{ negative: activityDelta < 0 }"
                >
                  {{ formatDelta(activityDelta) }}
                </span>
              </div>
              <p>較前 7 天</p>
            </div>
          </div>

          <div class="metric-sparkline sparkline-blue">
            <svg viewBox="0 0 100 32" preserveAspectRatio="none" aria-hidden="true">
              <polygon :points="`0,32 ${activitySparkline} 100,32`" />
              <polyline :points="activitySparkline" />
            </svg>
          </div>
        </article>

        <article class="metric-card metric-green">
          <div class="metric-content">
            <span class="metric-icon">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="m8 5 11 7-11 7V5Z" />
              </svg>
            </span>

            <div class="metric-copy">
              <div class="metric-heading">即將開始</div>
              <div class="metric-value-row">
                <strong class="metric-value">
                  {{ formatNumber(dashboard.upcomingEvents) }}
                </strong>
              </div>
              <p>未來 7 天有效活動</p>
            </div>
          </div>

          <div class="metric-sparkline sparkline-green">
            <svg viewBox="0 0 100 32" preserveAspectRatio="none" aria-hidden="true">
              <polygon :points="`0,32 ${scheduleSparkline} 100,32`" />
              <polyline :points="scheduleSparkline" />
            </svg>
          </div>
        </article>

        <article class="metric-card metric-purple">
          <div class="metric-content">
            <span class="metric-icon">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M16 20v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 10a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM22 20v-2a4 4 0 0 0-3-3.87M16 2.13a4 4 0 0 1 0 7.75" />
              </svg>
            </span>

            <div class="metric-copy">
              <div class="metric-heading">會員關注</div>
              <div class="metric-value-row">
                <strong class="metric-value">
                  {{ formatNumber(dashboard.totalFollows) }}
                </strong>
                <span
                  v-if="followDelta !== null"
                  class="metric-delta"
                  :class="{ negative: followDelta < 0 }"
                >
                  {{ formatDelta(followDelta) }}
                </span>
              </div>
              <p>較前 7 天</p>
            </div>
          </div>

          <div class="metric-sparkline sparkline-purple">
            <svg viewBox="0 0 100 32" preserveAspectRatio="none" aria-hidden="true">
              <polygon :points="`0,32 ${followSparkline} 100,32`" />
              <polyline :points="followSparkline" />
            </svg>
          </div>
        </article>

        <article class="metric-card metric-red">
          <div class="metric-content">
            <span class="metric-icon">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="m22 2-7 20-4-9-9-4 20-7ZM11 13 22 2" />
              </svg>
            </span>

            <div class="metric-copy">
              <div class="metric-heading">通知成功率</div>
              <div class="metric-value-row">
                <strong class="metric-value metric-value-percent">
                  {{ formatPercent(dashboard.deliveryRate) }}
                </strong>
                <span
                  v-if="deliveryDelta !== null"
                  class="metric-delta"
                  :class="{ negative: deliveryDelta < 0 }"
                >
                  {{ formatDelta(deliveryDelta) }}
                </span>
              </div>
              <p>較前一週</p>
            </div>
          </div>

          <div class="metric-sparkline sparkline-red">
            <svg viewBox="0 0 100 32" preserveAspectRatio="none" aria-hidden="true">
              <polygon :points="`0,32 ${deliverySparkline} 100,32`" />
              <polyline :points="deliverySparkline" />
            </svg>
          </div>
        </article>
      </section>

      <!-- 中間圖表 -->
      <section class="dashboard-grid dashboard-grid-main">
        <!-- 活動趨勢 -->
        <article class="dashboard-panel trend-panel">
          <header class="panel-header">
            <h2>活動趨勢</h2>
          </header>

          <div class="chart-area chart-area-line">
            <canvas ref="activityTrendCanvas"></canvas>
          </div>
        </article>

        <!-- 活動狀態 -->
        <article class="dashboard-panel status-panel">
          <header class="panel-header">
            <h2>活動狀態</h2>
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

        </article>
      </section>

      <!-- 下方圖表 -->
      <section class="dashboard-grid dashboard-grid-bottom">
        <!-- 通知發送成效 -->
        <article class="dashboard-panel delivery-panel">
          <header class="panel-header">
            <h2>通知發送成效</h2>

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
            <h2>近 7 天排程</h2>

            <div class="schedule-key">
              <span>
                <i class="schedule-event-dot"></i>
                活動
              </span>
            </div>
          </header>

          <div v-if="dashboard.upcomingSchedule?.length" class="schedule-grid">
            <div class="schedule-corner"></div>

            <header
              v-for="day in dashboard.upcomingSchedule"
              :key="`header-${day.date}`"
              class="schedule-day-header"
            >
              <strong>{{ formatShortDate(day.date) }}</strong>
              <span>{{ formatWeekday(day.date) }}</span>
            </header>

            <div class="schedule-row-label">活動</div>

            <div
              v-for="day in dashboard.upcomingSchedule"
              :key="`events-${day.date}`"
              class="schedule-cell"
            >
              <button
                v-for="item in day.items"
                :key="`${item.type}-${item.id}`"
                type="button"
                class="schedule-item"
                :title="`${formatTime(item.scheduledAt)} ${item.title}`"
                @click="goTo('search')"
              >
                <span class="schedule-item-dot"></span>
              </button>

              <span v-if="!day.items?.length" class="schedule-empty">—</span>
            </div>
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
  watch,
} from "vue";
import Chart from "chart.js/auto";

import {
  getNotificationCalendarDashboard,
} from "@/service/notificationService";

const emit = defineEmits(["navigate"]);
const props = defineProps({
  rangeDays: {
    type: Number,
    default: 30,
  },
});

const loading = ref(false);
const hasLoaded = ref(false);
const errorMessage = ref("");
const FAILED_JOBS_ACK_KEY =
  "sneakermarket.admin.failed-jobs-acknowledged-count.v1";
const acknowledgedFailedJobs = ref(0);

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

function calculatePeriodDelta(values, periodLength = 7) {
  const normalized = values.map((value) => Number(value || 0));
  const current = normalized
    .slice(-periodLength)
    .reduce((sum, value) => sum + value, 0);
  const previous = normalized
    .slice(-(periodLength * 2), -periodLength)
    .reduce((sum, value) => sum + value, 0);

  if (previous <= 0) return null;

  return Math.round(((current - previous) / previous) * 1000) / 10;
}

function createSparklinePoints(values) {
  const normalized = values.map((value) => Number(value || 0));

  if (!normalized.length) return "0,27 100,27";

  const max = Math.max(...normalized);
  const min = Math.min(...normalized);
  const range = max - min || 1;
  const lastIndex = Math.max(normalized.length - 1, 1);

  return normalized
    .map((value, index) => {
      const x = (index / lastIndex) * 100;
      const y = 28 - ((value - min) / range) * 22;
      return `${x.toFixed(2)},${y.toFixed(2)}`;
    })
    .join(" ");
}

const displayedActivityTrend = computed(() =>
  (dashboard.value.activityTrend || []).slice(-props.rangeDays),
);

const displayedDeliveryTrend = computed(() => {
  const weekCount = Math.max(1, Math.ceil(props.rangeDays / 7));

  return (dashboard.value.notificationDeliveryTrend || []).slice(-weekCount);
});

const activityValues = computed(() =>
  displayedActivityTrend.value.map((item) => item.eventCount),
);

const followValues = computed(() =>
  displayedActivityTrend.value.map((item) => item.followCount),
);

const allActivityValues = computed(() =>
  (dashboard.value.activityTrend || []).map((item) => item.eventCount),
);

const allFollowValues = computed(() =>
  (dashboard.value.activityTrend || []).map((item) => item.followCount),
);

const scheduleValues = computed(() =>
  (dashboard.value.upcomingSchedule || []).map(
    (day) => day.items?.length || 0,
  ),
);

const deliveryValues = computed(() =>
  displayedDeliveryTrend.value.map((item) => {
    const completed = Number(item.completed || 0);
    const processing = Number(item.processing || 0);
    const failed = Number(item.failed || 0);
    const total = completed + processing + failed;

    return total > 0 ? (completed / total) * 100 : 0;
  }),
);

const allDeliveryValues = computed(() =>
  (dashboard.value.notificationDeliveryTrend || []).map((item) => {
    const completed = Number(item.completed || 0);
    const processing = Number(item.processing || 0);
    const failed = Number(item.failed || 0);
    const total = completed + processing + failed;

    return total > 0 ? (completed / total) * 100 : 0;
  }),
);

const activitySparkline = computed(() =>
  createSparklinePoints(activityValues.value),
);
const followSparkline = computed(() =>
  createSparklinePoints(followValues.value),
);
const scheduleSparkline = computed(() =>
  createSparklinePoints(scheduleValues.value),
);
const deliverySparkline = computed(() =>
  createSparklinePoints(deliveryValues.value),
);

const activityDelta = computed(() =>
  calculatePeriodDelta(allActivityValues.value),
);
const followDelta = computed(() =>
  calculatePeriodDelta(allFollowValues.value),
);
const deliveryDelta = computed(() => {
  const values = allDeliveryValues.value;

  if (values.length < 2 || values.at(-2) <= 0) return null;

  return Math.round((values.at(-1) - values.at(-2)) * 10) / 10;
});

function formatDelta(value) {
  const number = Number(value || 0);
  const arrow = number >= 0 ? "↑" : "↓";

  return `${arrow} ${Math.abs(number).toFixed(1)}%`;
}

const hasUnacknowledgedFailedJobs = computed(() => {
  return (
    Number(dashboard.value.failedJobs || 0) >
    acknowledgedFailedJobs.value
  );
});

function readAcknowledgedFailedJobs() {
  try {
    const stored = Number(
      window.localStorage.getItem(FAILED_JOBS_ACK_KEY) || 0,
    );

    acknowledgedFailedJobs.value = Number.isFinite(stored)
      ? Math.max(stored, 0)
      : 0;
  } catch {
    acknowledgedFailedJobs.value = 0;
  }
}

function storeAcknowledgedFailedJobs(value) {
  acknowledgedFailedJobs.value = Math.max(Number(value || 0), 0);

  try {
    window.localStorage.setItem(
      FAILED_JOBS_ACK_KEY,
      String(acknowledgedFailedJobs.value),
    );
  } catch {
    // localStorage 不可用時仍保留本次瀏覽期間的隱藏狀態。
  }
}

function acknowledgeFailedJobs() {
  storeAcknowledgedFailedJobs(dashboard.value.failedJobs);
}

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

  const trend = displayedActivityTrend.value;

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

  const trend = displayedDeliveryTrend.value;

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

    const failedJobs = Number(dashboard.value.failedJobs || 0);

    if (failedJobs < acknowledgedFailedJobs.value) {
      storeAcknowledgedFailedJobs(failedJobs);
    }

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
  readAcknowledgedFailedJobs();
  loadDashboard();
});

watch(
  () => props.rangeDays,
  () => {
    renderCharts();
  },
);

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

.alert-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.alert-acknowledge {
  height: 30px;
  padding: 0 12px;
  border: 1px solid #c99089;
  border-radius: 4px;
  color: #603f3b;
  background: #ffffff;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.alert-acknowledge:hover {
  background: #fff8f7;
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
  height: 28px;
  opacity: 0.72;
}

.metric-sparkline svg {
  display: block;
  width: 100%;
  height: 100%;
}

.metric-sparkline polyline {
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
  vector-effect: non-scaling-stroke;
}

.sparkline-blue polyline {
  stroke: #7ea2ca;
}

.sparkline-green polyline {
  stroke: #8aaa84;
}

.sparkline-purple polyline {
  stroke: #b0839f;
}

.sparkline-red polyline {
  stroke: #ce7771;
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
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 100%;
  min-height: 24px;
  padding: 4px;
  border: 0;
  border-radius: 4px;
  color: #36546f;
  background: #e6eef6;
  text-align: center;
}

.schedule-item-dot {
  width: 7px;
  height: 7px;
  margin-top: 0;
  border-radius: 50%;
  background: #668db8;
}

.schedule-item-time {
  font-size: 8px;
  font-weight: 700;
}

.schedule-item-title {
  display: none;
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

/* Reference dashboard visual system */
.dashboard-page {
  --dashboard-line: #dfe5ea;
  --dashboard-muted: #697586;
  --dashboard-paper: #ffffff;
  --dashboard-soft: #f7f9fa;
  --dashboard-black: #111513;
}

.operation-alert {
  min-height: 54px;
  margin-bottom: 18px;
  padding: 12px 18px;
  border-color: #edcfcc;
  border-radius: 8px;
  color: #28201f;
  background: linear-gradient(90deg, #fbefee 0%, #f9f1f0 100%);
  font-size: 14px;
}

.alert-icon {
  width: 26px;
  height: 26px;
  flex: 0 0 26px;
  background: #c45e57;
  font-size: 14px;
}

.alert-link,
.alert-acknowledge {
  font-size: 13px;
}

.metric-grid {
  gap: 12px;
  margin-bottom: 18px;
}

.metric-card {
  min-height: 178px;
  padding: 20px 14px 14px;
  border-color: #dfe5ea;
  border-radius: 8px;
  box-shadow: 0 2px 7px rgb(34 45 54 / 3%);
}

.metric-content {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.metric-icon {
  width: 50px;
  height: 50px;
  flex: 0 0 50px;
  font-size: initial;
}

.metric-icon svg {
  width: 25px;
  height: 25px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
}

.metric-copy {
  min-width: 0;
  padding-top: 3px;
}

.metric-heading {
  display: block;
  color: #242925;
  font-size: 16px;
  font-weight: 750;
}

.metric-value-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 5px;
}

.metric-value {
  display: block;
  margin: 0;
  color: #080b09;
  font-size: 38px;
  font-weight: 850;
  letter-spacing: -0.045em;
  line-height: 1;
}

.metric-value-percent {
  font-size: 30px;
}

.metric-delta {
  color: #4d8b50;
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
}

.metric-red .metric-delta {
  font-size: 11px;
}

.metric-red .metric-value-row {
  gap: 6px;
}

.metric-delta.negative {
  color: #c45852;
}

.metric-card p {
  margin: 6px 0 0;
  color: #7c8796;
  font-size: 11px;
}

.metric-blue .metric-icon {
  color: #1b2838;
  background: #e2ebf5;
}

.metric-green .metric-icon {
  color: #285f2d;
  background: #e3ede2;
}

.metric-purple .metric-icon {
  color: #622841;
  background: #efe3ea;
}

.metric-red .metric-icon {
  color: #a82f2d;
  background: #f7e2e1;
}

.metric-sparkline {
  right: 18px;
  bottom: 14px;
  left: 18px;
  height: 52px;
  opacity: 1;
}

.metric-sparkline polyline {
  fill: none;
  stroke-width: 2.2;
}

.metric-sparkline polygon {
  stroke: none;
}

.sparkline-blue polyline { stroke: #4f80b6; }
.sparkline-blue polygon { fill: rgb(79 128 182 / 10%); }
.sparkline-green polyline { stroke: #659568; }
.sparkline-green polygon { fill: rgb(101 149 104 / 10%); }
.sparkline-purple polyline { stroke: #aa5f88; }
.sparkline-purple polygon { fill: rgb(170 95 136 / 10%); }
.sparkline-red polyline { stroke: #c94f4c; }
.sparkline-red polygon { fill: rgb(201 79 76 / 10%); }

.dashboard-grid {
  gap: 18px;
  margin-bottom: 18px;
}

.dashboard-grid-main {
  grid-template-columns: minmax(0, 1.9fr) minmax(310px, 1.15fr);
}

.dashboard-grid-bottom {
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.08fr);
}

.dashboard-panel {
  padding: 20px;
  border-color: #dfe5ea;
  border-radius: 8px;
  box-shadow: 0 2px 7px rgb(34 45 54 / 3%);
}

.panel-header {
  align-items: center;
  margin-bottom: 16px;
}

.panel-header h2 {
  color: #151916;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.025em;
}

.panel-summary {
  padding: 6px 11px;
  color: #626b70;
  background: #f0f1ef;
  font-size: 11px;
}

.chart-area-line {
  height: 260px;
}

.chart-area-delivery {
  height: 205px;
}

.status-content {
  grid-template-columns: minmax(150px, 1.1fr) minmax(0, 0.9fr);
  gap: 12px;
  min-height: 260px;
}

.doughnut-wrapper {
  min-width: 0;
  height: 220px;
}

.doughnut-wrapper canvas {
  position: relative;
  z-index: 2;
}

.doughnut-center {
  z-index: 1;
}

.doughnut-center strong {
  font-size: 34px;
  font-weight: 850;
}

.doughnut-center span {
  font-size: 12px;
}

.status-legend li {
  grid-template-columns: 11px minmax(0, 1fr) auto;
  gap: 8px;
  margin-bottom: 17px;
  font-size: 13px;
}

.status-legend {
  min-width: 0;
  width: 100%;
}

.status-legend strong {
  min-width: 18px;
  text-align: right;
}

.legend-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.legend-dot {
  width: 11px;
  height: 11px;
}

.panel-link {
  margin-top: 13px;
  color: #245f91;
  font-size: 12px;
}

.schedule-key {
  font-size: 12px;
}

.schedule-grid {
  display: grid;
  grid-template-columns: 62px repeat(7, minmax(68px, 1fr));
  grid-template-rows: 66px 128px;
  min-height: 194px;
  overflow-x: auto;
  border: 1px solid var(--dashboard-line);
  border-radius: 5px;
}

.schedule-corner,
.schedule-day-header,
.schedule-row-label,
.schedule-cell {
  min-width: 0;
  border-right: 1px solid var(--dashboard-line);
  border-bottom: 1px solid var(--dashboard-line);
}

.schedule-day-header {
  display: grid;
  align-content: center;
  gap: 3px;
  padding: 8px 4px;
  text-align: center;
}

.schedule-day-header strong {
  font-size: 12px;
}

.schedule-day-header span {
  color: #677382;
  font-size: 11px;
}

.schedule-row-label {
  display: grid;
  place-items: center;
  color: #4e5a68;
  font-size: 12px;
  font-weight: 700;
}

.schedule-cell {
  display: flex;
  align-content: center;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 7px;
  padding: 10px 6px;
}

.schedule-grid > :nth-child(8n) {
  border-right: 0;
}

.schedule-grid > :nth-last-child(-n + 8) {
  border-bottom: 0;
}

.schedule-item {
  display: inline-grid;
  width: 22px;
  min-height: 22px;
  padding: 0;
  place-items: center;
  border: 0;
  border-radius: 50%;
  background: transparent;
}

.schedule-item:hover {
  background: #e9f0f7;
}

.schedule-item-dot {
  width: 13px;
  height: 13px;
  background: #5f8fbe;
  box-shadow: 0 0 0 3px rgb(95 143 190 / 8%);
}

.schedule-empty {
  color: #c4cad0;
  font-size: 12px;
}

.empty-state {
  min-height: 194px;
  border-radius: 5px;
}

@media (max-width: 1280px) {
  .metric-grid {
    gap: 12px;
  }

  .metric-card {
    padding-right: 14px;
    padding-left: 14px;
  }

  .metric-content {
    gap: 11px;
  }

  .metric-icon {
    width: 50px;
    height: 50px;
    flex-basis: 50px;
  }

  .metric-value {
    font-size: 32px;
  }

  .metric-value-percent {
    font-size: 28px;
  }
}

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
    align-items: stretch;
  }

  .alert-actions {
    justify-content: space-between;
    width: 100%;
  }

  .status-content {
    grid-template-columns: 1fr;
  }
}
</style>
