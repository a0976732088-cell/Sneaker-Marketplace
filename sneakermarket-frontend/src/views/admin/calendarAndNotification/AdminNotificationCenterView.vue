<template>
  <div class="event-admin notification-center">
    <header class="admin-header">
      <div class="admin-header-top admin-page-heading">
        <div>
          <span class="admin-eyebrow">酷客 Kicks ADMIN</span>
          <h1 class="admin-title">通知中心</h1>
          <p class="admin-description admin-page-description">
            建立、搜尋、編輯、發送與管理會員通知。
          </p>
        </div>

        <button type="button" class="create-button" @click="toggleCreateForm">
          <span aria-hidden="true">
            {{ showCreateForm ? "×" : "+" }}
          </span>

          {{ showCreateForm ? "關閉新增" : "新增通知" }}
        </button>
      </div>
    </header>

    <section v-if="showCreateForm" class="create-section" aria-label="新增通知">
      <NotificationCreateForm @created="handleCreated" />
    </section>

    <aside class="notification-summary" aria-label="通知摘要">
      <details class="summary-card">
        <summary class="summary-card-header">
          <div>
            <h2>今日發送排程</h2>
            <p>依今日關聯活動整理</p>
          </div>
          <span class="summary-count">
            {{ dashboardSummary.pendingJobs }}
          </span>
        </summary>

        <div class="summary-card-content">
          <div v-if="todayScheduleItems.length" class="today-schedule-list">
            <div
              v-for="item in todayScheduleItems.slice(0, 4)"
              :key="`${item.type}-${item.id}`"
              class="today-schedule-item"
            >
              <span class="schedule-time">{{ formatScheduleTime(item.scheduledAt) }}</span>
              <div>
                <strong>{{ item.title }}</strong>
                <small>關聯活動</small>
              </div>
            </div>
          </div>

          <p v-else class="summary-empty">今日沒有關聯活動排程。</p>
        </div>
      </details>

      <details class="summary-card">
        <summary class="summary-card-header">
          <div>
            <h2>發送狀態</h2>
            <p>通知任務即時摘要</p>
          </div>
        </summary>

        <div class="summary-card-content">
          <div class="delivery-summary-list">
            <div
              v-for="item in deliverySummaryItems"
              :key="item.label"
              class="delivery-summary-item"
            >
              <div class="delivery-summary-label">
                <span><i :style="{ backgroundColor: item.color }"></i>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
              <div class="delivery-summary-track">
                <span
                  :style="{
                    width: item.percent,
                    backgroundColor: item.color,
                  }"
                ></span>
              </div>
            </div>
          </div>

          <p v-if="dashboardError" class="summary-error">
            {{ dashboardError }}
          </p>
        </div>
      </details>
    </aside>

    <div class="notification-layout">
      <section class="notification-workspace">
        <nav class="status-tabs" aria-label="通知狀態">
        <button
          v-for="tab in statusTabs"
          :key="tab.value"
          type="button"
          class="status-tab"
          :class="{ active: activeStatus === tab.value }"
          @click="selectStatus(tab.value)"
        >
          <span>{{ tab.label }}</span>
          <span class="tab-count">{{ tab.count }}</span>
        </button>
        </nav>

        <section class="admin-content" aria-label="通知列表">
          <NotificationManager
            :key="notificationManagerKey"
            :sent-status="activeStatus"
            @changed="refreshNotificationOverview"
          />
        </section>
      </section>

    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";

import {
  getNotificationCalendarDashboard,
  getNotificationPage,
} from "@/service/notificationService";
import NotificationCreateForm from "@/components/notification/NotificationCreateForm.vue";
import NotificationManager from "@/components/notification/NotificationManager.vue";

const showCreateForm = ref(false);
const notificationManagerKey = ref(0);
const dashboardError = ref("");

const dashboardSummary = reactive({
  pendingJobs: 0,
  processingJobs: 0,
  completedJobs: 0,
  failedJobs: 0,
  upcomingSchedule: [],
});

function toggleCreateForm() {
  showCreateForm.value = !showCreateForm.value;
}

async function handleCreated() {
  showCreateForm.value = false;
  notificationManagerKey.value += 1;
  activeStatus.value = "ALL";

  await refreshNotificationOverview();
}

const activeStatus = ref("ALL");

const statusCounts = reactive({
  ALL: 0,
  UNSENT: 0,
  SENT: 0,
});

const statusTabs = computed(() => [
  {
    value: "ALL",
    label: "全部",
    count: statusCounts.ALL,
  },
  {
    value: "UNSENT",
    label: "未發送",
    count: statusCounts.UNSENT,
  },
  {
    value: "SENT",
    label: "已發送",
    count: statusCounts.SENT,
  },
]);

async function fetchNotificationCounts() {
  try {
    const [allResponse, unsentResponse, sentResponse] = await Promise.all([
      getNotificationPage({
        pageNumber: 1,
        pageSize: 1,
      }),
      getNotificationPage({
        pageNumber: 1,
        pageSize: 1,
        isSent: false,
      }),
      getNotificationPage({
        pageNumber: 1,
        pageSize: 1,
        isSent: true,
      }),
    ]);

    statusCounts.ALL = allResponse.data?.totalElements ?? 0;

    statusCounts.UNSENT = unsentResponse.data?.totalElements ?? 0;

    statusCounts.SENT = sentResponse.data?.totalElements ?? 0;
  } catch (error) {
    console.error("取得通知狀態數量失敗：", error);
  }
}

async function fetchDashboardSummary() {
  dashboardError.value = "";

  try {
    const response = await getNotificationCalendarDashboard();
    Object.assign(dashboardSummary, response.data || {});
  } catch (error) {
    console.error("取得通知摘要失敗：", error);
    dashboardError.value = "目前無法讀取通知摘要。";
  }
}

async function refreshNotificationOverview() {
  await Promise.all([
    fetchNotificationCounts(),
    fetchDashboardSummary(),
  ]);
}

const todayKey = () => {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, "0");
  const day = String(today.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const todayScheduleItems = computed(() => {
  const today = dashboardSummary.upcomingSchedule?.find(
    (item) => item.date === todayKey(),
  );
  return today?.items || [];
});

const totalDeliveryJobs = computed(() =>
  [
    dashboardSummary.pendingJobs,
    dashboardSummary.processingJobs,
    dashboardSummary.completedJobs,
    dashboardSummary.failedJobs,
  ].reduce((total, value) => total + Number(value || 0), 0),
);

const deliverySummaryItems = computed(() => {
  const total = totalDeliveryJobs.value;
  const buildItem = (label, value, color) => ({
    label,
    value: Number(value || 0),
    color,
    percent:
      total > 0
        ? `${Math.round((Number(value || 0) / total) * 100)}%`
        : "0%",
  });

  return [
    buildItem("等待處理", dashboardSummary.pendingJobs, "#b69a72"),
    buildItem("發送中", dashboardSummary.processingJobs, "#7095bf"),
    buildItem("已完成", dashboardSummary.completedJobs, "#79a278"),
    buildItem("失敗", dashboardSummary.failedJobs, "#c97872"),
  ];
});

function formatScheduleTime(value) {
  if (!value) return "--:--";
  return new Intl.DateTimeFormat("zh-TW", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(new Date(value));
}

function selectStatus(status) {
  activeStatus.value = status;
}

onMounted(() => {
  refreshNotificationOverview();
});
</script>

<style scoped>
.notification-center {
  width: 100%;
  min-width: 0;
}

.create-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 38px;
  padding: 0 18px;
  border: 0;
  border-radius: 5px;
  color: #ffffff;
  background: #222320;
  font-size: 12px;
  font-weight: 650;
}

.create-button:hover {
  background: #2b2b2b;
  transform: translateY(-1px);
}

.create-button span {
  font-size: 22px;
  font-weight: 400;
  line-height: 1;
}

.create-section {
  margin-bottom: 24px;
}

.create-section :deep(.card),
.admin-content :deep(.card) {
  border: 1px solid #d8d7d2 !important;
  border-radius: 6px !important;
  background: #ffffff !important;
  box-shadow: none !important;
}

.create-section :deep(.form-control),
.create-section :deep(.form-select),
.admin-content :deep(.form-control),
.admin-content :deep(.form-select) {
  border-color: #d8d7d2;
  border-radius: 4px;
}

.create-section :deep(.form-control:focus),
.create-section :deep(.form-select:focus),
.admin-content :deep(.form-control:focus),
.admin-content :deep(.form-select:focus) {
  border-color: #111111;
  box-shadow: 0 0 0 2px rgb(17 17 17 / 8%);
}

.notification-workspace {
  min-width: 0;
}

.notification-layout {
  min-width: 0;
}

.notification-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.summary-card {
  padding: 0;
  border: 1px solid #dedfdb;
  border-radius: 5px;
  background: #ffffff;
}

.summary-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 13px;
  list-style: none;
  cursor: pointer;
}

.summary-card-header::-webkit-details-marker {
  display: none;
}

.summary-card-header::after {
  align-self: center;
  color: #777a74;
  content: "+";
  font-size: 16px;
  line-height: 1;
}

.summary-card[open] .summary-card-header::after {
  content: "−";
}

.summary-card-content {
  padding: 0 13px 13px;
}

.summary-card-header h2 {
  margin: 0;
  color: #282a27;
  font-size: 13px;
  font-weight: 750;
}

.summary-card-header p,
.summary-empty,
.summary-error {
  margin: 3px 0 0;
  color: #858881;
  font-size: 10px;
}

.summary-count {
  display: inline-grid;
  min-width: 24px;
  height: 22px;
  padding: 0 6px;
  place-items: center;
  border-radius: 11px;
  color: #526168;
  background: #e4ebf0;
  font-size: 10px;
  font-weight: 750;
}

.summary-card-header .summary-count {
  margin-left: auto;
}

.today-schedule-list,
.delivery-summary-list {
  display: grid;
  gap: 9px;
}

.today-schedule-item {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 8px;
  align-items: start;
  padding: 8px;
  border-radius: 4px;
  background: #f7f7f5;
}

.schedule-time {
  color: #5d7b99;
  font-size: 10px;
  font-weight: 750;
}

.today-schedule-item strong,
.today-schedule-item small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.today-schedule-item strong {
  color: #3d403b;
  font-size: 10px;
}

.today-schedule-item small {
  margin-top: 2px;
  color: #92958f;
  font-size: 9px;
}

.delivery-summary-item {
  display: grid;
  gap: 5px;
}

.delivery-summary-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #62665f;
  font-size: 10px;
}

.delivery-summary-label span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.delivery-summary-label i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.delivery-summary-track {
  height: 4px;
  overflow: hidden;
  border-radius: 2px;
  background: #ecece8;
}

.delivery-summary-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
}

.summary-error {
  color: #a25c55;
}

.status-tabs {
  display: flex;
  gap: 28px;
  margin-bottom: 16px;
  border-bottom: 1px solid #deddd8;
}

.status-tab {
  position: relative;
  flex: 0 0 auto;
  padding: 12px 2px 14px;
  border: 0;
  color: #777771;
  background: transparent;
  font-size: 13px;
  font-weight: 650;
  cursor: pointer;
}

.status-tab::after {
  position: absolute;
  right: 0;
  bottom: -1px;
  left: 0;
  height: 2px;
  background: transparent;
  content: "";
}

.status-tab:hover {
  color: #111111;
}

.status-tab.active {
  color: #111111;
}

.status-tab.active::after {
  background: #111111;
}

.notification-center :deep(.admin-header) {
  margin-bottom: 14px;
  padding-top: 0;
}

.notification-center :deep(.admin-title) {
  margin-bottom: 4px;
  font-size: 28px;
}

.notification-center :deep(.admin-description) {
  font-size: 12px;
}

.status-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tab-count {
  display: inline-grid;
  min-width: 20px;
  height: 18px;
  padding: 0 5px;
  place-items: center;
  border-radius: 9px;
  color: #777a74;
  background: #eeefec;
  font-size: 10px;
  line-height: 1;
}

.status-tab.active .tab-count {
  color: #ffffff;
  background: #292b28;
}

@media (max-width: 768px) {
  .notification-summary {
    grid-template-columns: 1fr;
  }

  .create-button {
    width: 100%;
  }
}
</style>
