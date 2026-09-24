<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { RouterLink } from "vue-router";
import Chart from "chart.js/auto";
import { FontAwesomeIcon } from "@fortawesome/vue-fontawesome";
import {
  faArrowRight,
  faBell,
  faBullhorn,
  faCalendarDays,
  faChartLine,
  faCheck,
  faClock,
  faCube,
  faEye,
  faGavel,
  faMousePointer,
  faPaperPlane,
  faPlus,
  faTriangleExclamation,
  faUsers,
} from "@fortawesome/free-solid-svg-icons";

import { getAdvertisementDashboardSummary } from "@/service/advertisementService";
import { getAdminAuctions } from "@/service/auctionAdminService";
import { getAllMembers } from "@/service/memberService";
import { getNotificationCalendarDashboard } from "@/service/notificationService";
import { getAdminProducts } from "@/service/productAdminService";

const range = ref(30);
const loading = ref(true);
const unavailableSections = ref([]);
const trendCanvas = ref(null);
let trendChart = null;

const members = ref([]);
const products = ref([]);
const activeAuctionCount = ref(0);
const dashboard = ref({
  upcomingEvents: 0,
  completedJobs: 0,
  processingJobs: 0,
  failedJobs: 0,
  deliveryRate: 0,
  activityTrend: [],
  upcomingSchedule: [],
});
const advertisement = ref({
  activeAdvertisements: 0,
  views: 0,
  clicks: 0,
  ctr: 0,
});

const numberFormatter = new Intl.NumberFormat("zh-TW");
const dateFormatter = new Intl.DateTimeFormat("zh-TW", {
  month: "2-digit",
  day: "2-digit",
});
const timeFormatter = new Intl.DateTimeFormat("zh-TW", {
  hour: "2-digit",
  minute: "2-digit",
  hour12: false,
});

const lowStockProducts = computed(() =>
  [...products.value]
    .filter((product) => Number(product.totalStock) <= 5)
    .sort((left, right) => Number(left.totalStock) - Number(right.totalStock))
    .slice(0, 3),
);

const upcomingItems = computed(() =>
  (dashboard.value.upcomingSchedule || [])
    .flatMap((day) => day.items || [])
    .slice(0, 5),
);

const rangeLabel = computed(() => `近 ${range.value} 天`);

function unwrap(response) {
  return response?.data?.data ?? response?.data ?? response;
}

function formatNumber(value) {
  return numberFormatter.format(Number(value) || 0);
}

function formatPercent(value) {
  return `${Number(value || 0).toFixed(1)}%`;
}

function formatScheduleDate(value) {
  if (!value) return "--/--";
  return dateFormatter.format(new Date(value));
}

function formatScheduleTime(value) {
  if (!value) return "--:--";
  return timeFormatter.format(new Date(value));
}

function normalizeProducts(payload) {
  if (Array.isArray(payload)) return payload;
  return payload?.content || payload?.items || [];
}

function getAuctionTotal(payload) {
  return Number(payload?.totalElements ?? payload?.total ?? payload?.items?.length ?? 0);
}

function chartPoints() {
  return (dashboard.value.activityTrend || []).slice(-range.value);
}

function renderTrendChart() {
  if (!trendCanvas.value) return;
  trendChart?.destroy();

  const points = chartPoints();
  trendChart = new Chart(trendCanvas.value, {
    type: "line",
    data: {
      labels: points.map((point) => {
        const date = new Date(`${point.date}T00:00:00`);
        return `${date.getMonth() + 1}/${date.getDate()}`;
      }),
      datasets: [
        {
          label: "活動數",
          data: points.map((point) => point.eventCount || 0),
          borderColor: "#6f9875",
          backgroundColor: "rgba(111, 152, 117, 0.12)",
          fill: true,
          tension: 0.38,
          pointRadius: 2.5,
          pointBackgroundColor: "#6f9875",
        },
        {
          label: "會員關注",
          data: points.map((point) => point.followCount || 0),
          borderColor: "#4f83b9",
          backgroundColor: "rgba(79, 131, 185, 0.1)",
          fill: true,
          tension: 0.38,
          pointRadius: 2.5,
          pointBackgroundColor: "#4f83b9",
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { intersect: false, mode: "index" },
      plugins: {
        legend: {
          align: "end",
          labels: { usePointStyle: true, boxWidth: 8, color: "#5d625d" },
        },
      },
      scales: {
        x: { grid: { display: false }, ticks: { color: "#858a85", maxTicksLimit: 10 } },
        y: { beginAtZero: true, grid: { color: "#eceeeb" }, ticks: { precision: 0, color: "#858a85" } },
      },
    },
  });
}

async function loadDashboard() {
  loading.value = true;
  unavailableSections.value = [];

  const requests = [
    ["會員", getAllMembers()],
    ["商品", getAdminProducts()],
    ["競標", getAdminAuctions({ page: 0, size: 1, status: "ACTIVE" })],
    ["活動與通知", getNotificationCalendarDashboard()],
    ["廣告", getAdvertisementDashboardSummary()],
  ];

  const results = await Promise.allSettled(requests.map(([, request]) => request));

  results.forEach((result, index) => {
    const section = requests[index][0];
    if (result.status === "rejected") {
      unavailableSections.value.push(section);
      return;
    }

    const payload = unwrap(result.value);
    if (index === 0) members.value = Array.isArray(payload) ? payload : [];
    if (index === 1) products.value = normalizeProducts(payload);
    if (index === 2) activeAuctionCount.value = getAuctionTotal(payload);
    if (index === 3) dashboard.value = { ...dashboard.value, ...payload };
    if (index === 4) advertisement.value = { ...advertisement.value, ...payload };
  });

  loading.value = false;
  await nextTick();
  renderTrendChart();
}

watch(range, async () => {
  await nextTick();
  renderTrendChart();
});

onMounted(loadDashboard);
onBeforeUnmount(() => trendChart?.destroy());
</script>

<template>
  <div class="dashboard-page">
    <header class="dashboard-header">
      <div>
        <span class="eyebrow">酷客 KICKS ADMIN</span>
        <h1>營運總覽</h1>
        <p>掌握平台會員、商品、競標與活動狀態。</p>
      </div>

      <label class="range-select">
        <FontAwesomeIcon :icon="faCalendarDays" />
        <select v-model.number="range" aria-label="趨勢範圍">
          <option :value="7">近 7 天</option>
          <option :value="14">近 14 天</option>
          <option :value="30">近 30 天</option>
        </select>
      </label>
    </header>

    <div v-if="unavailableSections.length" class="partial-warning" role="status">
      <FontAwesomeIcon :icon="faTriangleExclamation" />
      {{ unavailableSections.join("、") }}資料暫時無法載入，其餘區塊仍可正常使用。
      <button type="button" @click="loadDashboard">重新載入</button>
    </div>

    <section class="kpi-grid" aria-label="平台摘要">
      <article class="kpi-card">
        <span class="kpi-icon green"><FontAwesomeIcon :icon="faUsers" /></span>
        <div><span>會員總數</span><strong>{{ loading ? "—" : formatNumber(members.length) }}</strong></div>
      </article>
      <article class="kpi-card">
        <span class="kpi-icon blue"><FontAwesomeIcon :icon="faCube" /></span>
        <div><span>商品總數</span><strong>{{ loading ? "—" : formatNumber(products.length) }}</strong></div>
      </article>
      <article class="kpi-card">
        <span class="kpi-icon mauve"><FontAwesomeIcon :icon="faGavel" /></span>
        <div><span>進行中競標</span><strong>{{ loading ? "—" : formatNumber(activeAuctionCount) }}</strong></div>
      </article>
      <article class="kpi-card">
        <span class="kpi-icon tan"><FontAwesomeIcon :icon="faCalendarDays" /></span>
        <div><span>即將開始活動</span><strong>{{ loading ? "—" : formatNumber(dashboard.upcomingEvents) }}</strong></div>
      </article>
    </section>

    <section class="dashboard-grid top-grid">
      <article class="panel trend-panel">
        <div class="panel-heading">
          <div><h2>活動與會員關注趨勢</h2><p>{{ rangeLabel }}平台動態</p></div>
          <RouterLink :to="{ name: 'admin-activity-overview' }">查看詳情 <FontAwesomeIcon :icon="faArrowRight" /></RouterLink>
        </div>
        <div class="chart-wrap"><canvas ref="trendCanvas"></canvas></div>
      </article>

      <article class="panel ad-panel">
        <div class="panel-heading">
          <div><h2>本月廣告成效</h2><p>曝光與點擊摘要</p></div>
          <RouterLink :to="{ name: 'admin-advertisements' }">查看詳情 <FontAwesomeIcon :icon="faArrowRight" /></RouterLink>
        </div>
        <div class="ad-stats">
          <div><FontAwesomeIcon :icon="faBullhorn" /><span>啟用中</span><strong>{{ formatNumber(advertisement.activeAdvertisements) }}</strong></div>
          <div><FontAwesomeIcon :icon="faEye" /><span>曝光</span><strong>{{ formatNumber(advertisement.views) }}</strong></div>
          <div><FontAwesomeIcon :icon="faMousePointer" /><span>點擊</span><strong>{{ formatNumber(advertisement.clicks) }}</strong></div>
          <div><FontAwesomeIcon :icon="faChartLine" /><span>CTR</span><strong>{{ Number(advertisement.ctr || 0).toFixed(2) }}%</strong></div>
        </div>
      </article>
    </section>

    <section class="dashboard-grid bottom-grid">
      <article class="panel schedule-panel">
        <div class="panel-heading"><div><h2>近 7 天活動排程</h2><p>即將開始的活動</p></div></div>
        <div v-if="upcomingItems.length" class="schedule-list">
          <div v-for="item in upcomingItems" :key="`${item.id}-${item.scheduledAt}`" class="schedule-row">
            <time><b>{{ formatScheduleDate(item.scheduledAt) }}</b>{{ formatScheduleTime(item.scheduledAt) }}</time>
            <span class="schedule-title">{{ item.title }}</span><span class="status-pill green-pill">活動</span>
          </div>
        </div>
        <p v-else class="empty-state">近 7 天目前沒有活動排程</p>
      </article>

      <div class="center-stack">
        <article class="panel notification-panel">
          <div class="panel-heading"><div><h2>通知發送成效</h2><p>目前任務狀態</p></div></div>
          <div class="notification-stats">
            <div><FontAwesomeIcon class="success" :icon="faCheck" /><span>已完成</span><strong>{{ formatNumber(dashboard.completedJobs) }}</strong></div>
            <div><FontAwesomeIcon class="processing" :icon="faClock" /><span>處理中</span><strong>{{ formatNumber(dashboard.processingJobs) }}</strong></div>
            <div><FontAwesomeIcon class="danger" :icon="faTriangleExclamation" /><span>失敗</span><strong>{{ formatNumber(dashboard.failedJobs) }}</strong></div>
            <div><FontAwesomeIcon class="success" :icon="faPaperPlane" /><span>送達率</span><strong>{{ formatPercent(dashboard.deliveryRate) }}</strong></div>
          </div>
        </article>

        <article class="panel stock-panel">
          <div class="panel-heading"><div><h2>低庫存商品</h2><p>庫存 5 件以下</p></div><RouterLink :to="{ name: 'admin-products', query: { maxStock: 5 } }">查看全部</RouterLink></div>
          <div v-if="lowStockProducts.length" class="stock-list">
            <div v-for="product in lowStockProducts" :key="product.productId">
              <span>{{ product.productName }}</span><b>庫存 {{ formatNumber(product.totalStock) }}</b>
            </div>
          </div>
          <p v-else class="empty-state compact">目前沒有低庫存商品</p>
        </article>
      </div>

      <article class="panel quick-panel">
        <div class="panel-heading"><div><h2>快速操作</h2><p>常用建立功能</p></div></div>
        <nav class="quick-links">
          <RouterLink class="quick green-bg" :to="{ name: 'admin-product-create' }"><FontAwesomeIcon :icon="faCube" /><span>新增商品</span><FontAwesomeIcon :icon="faPlus" /></RouterLink>
          <RouterLink class="quick blue-bg" :to="{ name: 'admin-events', query: { action: 'create' } }"><FontAwesomeIcon :icon="faCalendarDays" /><span>新增活動</span><FontAwesomeIcon :icon="faPlus" /></RouterLink>
          <RouterLink class="quick mauve-bg" :to="{ name: 'admin-notifications', query: { action: 'create' } }"><FontAwesomeIcon :icon="faBell" /><span>新增通知</span><FontAwesomeIcon :icon="faPlus" /></RouterLink>
          <RouterLink class="quick tan-bg" :to="{ name: 'admin-advertisements', query: { action: 'create' } }"><FontAwesomeIcon :icon="faBullhorn" /><span>新增廣告</span><FontAwesomeIcon :icon="faPlus" /></RouterLink>
        </nav>
      </article>
    </section>
  </div>
</template>

<style scoped>
.dashboard-page { color: #20231f; }
.dashboard-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 24px; margin-bottom: 24px; }
.eyebrow { color: #747970; font-size: 12px; font-weight: 700; letter-spacing: .18em; }
.dashboard-header h1 { margin: 5px 0 4px; font-size: clamp(30px, 3vw, 42px); font-weight: 750; letter-spacing: -.04em; }
.dashboard-header p, .panel-heading p { margin: 0; color: #858a84; font-size: 14px; }
.range-select { display: flex; height: 44px; align-items: center; gap: 8px; padding: 0 12px; border: 1px solid #dcded9; border-radius: 8px; background: #fff; }
.range-select select { border: 0; outline: 0; background: transparent; color: #252824; font-weight: 650; }
.partial-warning { display: flex; align-items: center; gap: 9px; margin-bottom: 18px; padding: 12px 14px; border: 1px solid #ead8b5; border-radius: 8px; background: #fff9ed; color: #725921; font-size: 13px; }
.partial-warning button { margin-left: auto; border: 0; background: transparent; color: inherit; font-weight: 700; }
.kpi-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; margin-bottom: 16px; }
.kpi-card { display: flex; min-height: 116px; align-items: center; gap: 18px; padding: 20px; border: 1px solid #dfe1dd; border-radius: 10px; background: #fff; }
.kpi-icon { display: grid; width: 58px; height: 58px; flex: 0 0 58px; place-items: center; border-radius: 50%; font-size: 24px; }
.green { background: #dce8dc; color: #477251; }.blue { background: #dce8f3; color: #34678f; }.mauve { background: #eadde5; color: #814b68; }.tan { background: #eee1d0; color: #855c25; }
.kpi-card div { display: grid; gap: 3px; }.kpi-card span { color: #555b54; font-size: 14px; }.kpi-card strong { font-size: 30px; line-height: 1; }
.dashboard-grid { display: grid; gap: 16px; margin-top: 16px; }.top-grid { grid-template-columns: minmax(0, 1.75fr) minmax(300px, 1fr); }.bottom-grid { grid-template-columns: 1.02fr 1.08fr .85fr; align-items: stretch; }
.panel { min-width: 0; border: 1px solid #dfe1dd; border-radius: 10px; background: #fff; padding: 18px; }
.panel-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 15px; }.panel-heading h2 { margin: 0 0 3px; font-size: 18px; font-weight: 750; }.panel-heading a { flex: 0 0 auto; color: #6f756f; font-size: 12px; text-decoration: none; }
.chart-wrap { height: 270px; }.ad-stats { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }.ad-stats > div { display: grid; min-height: 103px; place-items: center; padding: 12px; border-radius: 8px; background: #f7f8f6; }.ad-stats svg { color: #628b69; font-size: 21px; }.ad-stats span { color: #777d76; font-size: 12px; }.ad-stats strong { font-size: 21px; }
.schedule-list { display: grid; }.schedule-row { display: grid; grid-template-columns: 86px minmax(0, 1fr) auto; align-items: center; gap: 10px; padding: 11px 0; border-top: 1px solid #eceeea; }.schedule-row:first-child { border-top: 0; }.schedule-row time { display: flex; gap: 6px; color: #7a8079; font-size: 12px; }.schedule-title { overflow: hidden; font-size: 13px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }.status-pill { padding: 4px 9px; border-radius: 999px; font-size: 11px; }.green-pill { background: #e0ece1; color: #477251; }
.center-stack { display: grid; gap: 16px; }.notification-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 4px; }.notification-stats div { display: grid; place-items: center; gap: 2px; border-left: 1px solid #e7e9e5; }.notification-stats div:first-child { border-left: 0; }.notification-stats svg { font-size: 18px; }.notification-stats span { color: #737972; font-size: 11px; }.notification-stats strong { font-size: 20px; }.success { color: #56855f; }.processing { color: #4c82b6; }.danger { color: #c84f4f; }
.stock-list { display: grid; }.stock-list div { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 8px 0; border-top: 1px solid #eceeea; font-size: 13px; }.stock-list div:first-child { border-top: 0; }.stock-list span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.stock-list b { flex: 0 0 auto; padding: 3px 8px; border-radius: 999px; background: #f9e6e4; color: #b4534b; font-size: 11px; }
.quick-links { display: grid; gap: 10px; }.quick { display: grid; grid-template-columns: 23px 1fr auto; min-height: 48px; align-items: center; gap: 10px; padding: 0 13px; border-radius: 8px; color: #293029; font-size: 13px; font-weight: 700; text-decoration: none; }.green-bg { background: #dfe9df; }.blue-bg { background: #dce8f4; }.mauve-bg { background: #eadde5; }.tan-bg { background: #eee1d0; }
.empty-state { display: grid; min-height: 205px; place-items: center; margin: 0; color: #969b95; font-size: 13px; }.empty-state.compact { min-height: 72px; }
@media (max-width: 1200px) { .kpi-grid { grid-template-columns: repeat(2, 1fr); }.top-grid, .bottom-grid { grid-template-columns: 1fr; }.quick-links { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 680px) { .dashboard-header { flex-direction: column; }.kpi-grid { grid-template-columns: 1fr; }.quick-links { grid-template-columns: 1fr; }.notification-stats { grid-template-columns: repeat(2, 1fr); row-gap: 14px; }.notification-stats div:nth-child(3) { border-left: 0; } }
</style>
