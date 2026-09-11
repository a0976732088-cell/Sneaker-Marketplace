<!-- views/CalendarHomeView.vue -->
<template>
  <div class="calendar-dashboard-container">
    <!-- 頂部 Header & 導覽列 -->
    <header
      class="dashboard-header d-flex justify-content-between align-items-center mb-4"
    >
      <div>
        <h1 class="h3 fw-bold text-slate-800 mb-1">Morning, User! 👋</h1>
        <p class="text-muted small m-0">這是你本月的排程與活動總覽。</p>
      </div>
      <div class="d-flex align-items-center gap-3">
        <button
          type="button"
          class="btn btn-primary rounded-pill px-4 fw-semibold shadow-sm"
          @click="goToManage"
        >
          後臺管理
        </button>
      </div>
    </header>

    <!-- 左右雙欄卡片容器 (模仿參考圖的 Layout) -->
    <div class="dashboard-grid">
      <!-- 左側：月曆主視圖 (Calendar Main Grid) -->
      <main class="calendar-main-card">
        <CalendarHeader
          :current-year="currentYear"
          :current-month="currentMonth"
          v-model:current-view="currentView"
          @change-month="changeMonth"
        />

        <CalendarMonthView
          v-if="currentView === 'month'"
          :events="events"
          :current-year="currentYear"
          :current-month="currentMonth"
          :selected-date="selectedDate"
          @select-date="handleSelectDate"
          @open-detail="openDetailModal"
        />

        <CalendarListView
          v-else
          :events="events"
          :current-year="currentYear"
          :current-month="currentMonth"
          @open-detail="openDetailModal"
        />
      </main>

      <!-- 右側：當日行程預覽側邊欄 (Scheduled Sidebar) -->
      <aside class="scheduled-sidebar-card">
        <div class="sidebar-header">
          <div>
            <h2 class="h6 fw-bold text-slate-800 m-0">Scheduled</h2>
            <span class="text-muted small">{{ selectedDateFormatted }}</span>
          </div>
          <span class="badge bg-light text-primary rounded-pill fw-bold border">
            {{ selectedDayEvents.length }} 個活動
          </span>
        </div>

        <div v-if="selectedDayEvents.length === 0" class="empty-scheduled">
          <i class="bi bi-calendar-check text-slate-300 fs-1 mb-2"></i>
          <p class="text-muted small m-0">今日無排定活動</p>
        </div>

        <div v-else class="scheduled-list">
          <div
            v-for="evt in selectedDayEvents"
            :key="evt.id"
            class="scheduled-card"
            @click="openDetailModal(evt)"
          >
            <!-- 頂部彩色條 -->
            <div
              class="card-accent-bar"
              :style="{ backgroundColor: evt.color }"
            ></div>

            <div class="card-content">
              <div class="time-label">{{ evt.time || "全天" }}</div>
              <h3 class="event-title">{{ evt.title }}</h3>
              <p v-if="evt.description" class="event-desc text-truncate">
                {{ evt.description }}
              </p>

              <!-- 底部按鈕/狀態 (選配) -->
              <div class="card-footer-info">
                <a
                  v-if="evt.targetUrl"
                  :href="evt.targetUrl"
                  target="_blank"
                  class="link-btn"
                  @click.stop
                >
                  🔗 相關連結
                </a>

                <button
                  type="button"
                  class="follow-btn"
                  :class="{ followed: isFollowed(evt.id) }"
                  :disabled="followLoadingIds.has(evt.id)"
                  @click.stop="toggleFollow(evt)"
                >
                  {{ isFollowed(evt.id) ? "✓ 已關注" : "☆ 關注" }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <!-- 詳細資料 Modal -->
    <EventDetailModal
      v-if="showDetailModal"
      :event="selectedModalEvent"
      :img-timestamp="modalImgTimestamp"
      @close="closeDetailModal"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from "vue";
import { useRouter } from "vue-router";
import axios from "axios";

import CalendarHeader from "@/components/calendar/CalendarHeader.vue";
import CalendarMonthView from "@/components/calendar/CalendarMonthView.vue";
import CalendarListView from "@/components/calendar/CalendarListView.vue";
import EventDetailModal from "@/components/events/EventDetailModal.vue";
import {
  getMyEventFollows,
  followEvent,
  unfollowEvent,
} from "@/api/calendar";

const router = useRouter();
const goToManage = () => router.push("/calendar/eventManage");

const currentView = ref("month");
const currentDate = ref(new Date());
const selectedDate = ref(new Date()); // 目前點選的日期

const events = ref([]);
const loading = ref(false);

const eventTypeColorMap = {
  RELEASE: "#4f46e5", // Indigo 靛藍
  SALE: "#f59e0b", // Amber 琥珀橘
  EVENT: "#10b981", // Emerald 翡翠綠
  NOTICE: "#ef4444", // Rose 珊瑚紅
  OTHER: "#64748b", // Slate 沉穩灰
};

const currentYear = computed(() => currentDate.value.getFullYear());
const currentMonth = computed(() => currentDate.value.getMonth());

const changeMonth = (delta) => {
  const d = new Date(currentDate.value);
  d.setMonth(d.getMonth() + delta);
  currentDate.value = d;
};

const handleSelectDate = (date) => {
  selectedDate.value = date;
};

const selectedDateFormatted = computed(() => {
  if (!selectedDate.value) return "";
  const y = selectedDate.value.getFullYear();
  const m = selectedDate.value.getMonth() + 1;
  const d = selectedDate.value.getDate();
  return `${y} 年 ${m} 月 ${d} 日`;
});

const selectedDayEvents = computed(() => {
  if (!selectedDate.value) return [];
  const y = selectedDate.value.getFullYear();
  const m = String(selectedDate.value.getMonth() + 1).padStart(2, "0");
  const d = String(selectedDate.value.getDate()).padStart(2, "0");
  const dateStr = `${y}-${m}-${d}`;
  return events.value.filter((e) => e.date === dateStr);
});

const formatTimeStr = (dateStr) => {
  if (!dateStr) return "";
  const d = new Date(dateStr);
  const hours = String(d.getHours()).padStart(2, "0");
  const minutes = String(d.getMinutes()).padStart(2, "0");
  return `${hours}:${minutes}`;
};

const fetchMonthEvents = async () => {
  loading.value = true;
  const lastDay = new Date(
    currentYear.value,
    currentMonth.value + 1,
    0,
  ).getDate();
  const startAt = `${currentYear.value}-${String(currentMonth.value + 1).padStart(2, "0")}-01T00:00:00`;
  const endAt = `${currentYear.value}-${String(currentMonth.value + 1).padStart(2, "0")}-${String(lastDay).padStart(2, "0")}T23:59:59`;

  try {
    const response = await axios.get(
      "http://localhost:8080/api/calendar/eventPage",
      {
        params: { startAt, endAt, pageNumber: 1, pageSize: 100 },
      },
    );
    const rawData =
      response.data.content || response.data.data || response.data || [];

    events.value = rawData
      .filter((item) => item.status !== "INACTIVE" && item.status !== "DRAFT" && item.status !== "CANCELLED")
      .map((item) => {
        const rawStartAt = item.startAt || "";
        const datePart = rawStartAt
          ? rawStartAt.replace(" ", "T").split("T")[0]
          : "";
        const startTime = item?.startAt ? formatTimeStr(item.startAt) : "";
        const endTime = item?.endAt ? formatTimeStr(item.endAt) : "";

        return {
          id: item.eventId,
          date: datePart,
          time: endTime ? `${startTime} - ${endTime}` : startTime,
          title: item.title,
          description: item.description,
          color: eventTypeColorMap[item.eventType] || "#4f46e5",
          status: item.status,
          eventType: item.eventType || "OTHER",
          targetUrl: item.targetUrl,
        };
      });
  } catch (error) {
    console.error("抓取活動失敗:", error);
  } finally {
    loading.value = false;
  }
};

watch([currentYear, currentMonth], fetchMonthEvents, { immediate: true });

const showDetailModal = ref(false);
const selectedModalEvent = ref(null);
const modalImgTimestamp = ref(Date.now());

const openDetailModal = (evt) => {
  selectedModalEvent.value = evt;
  modalImgTimestamp.value = Date.now();
  showDetailModal.value = true;
};

const closeDetailModal = () => {
  showDetailModal.value = false;
  selectedModalEvent.value = null;
};

const followedEventIds = ref(new Set());
const followLoadingIds = ref(new Set());

const isFollowed = (eventId) => {
  return followedEventIds.value.has(eventId);
};

const loadMyFollows = async () => {
  try {
    const response = await getMyEventFollows();

    followedEventIds.value = new Set(
      response.data.map((follow) => follow.eventId)
    );
  } catch (error) {
    if (error.response?.status === 401) {
      followedEventIds.value = new Set();
      return;
    }

    console.error("讀取活動關注資料失敗", error);
  }
};

const toggleFollow = async (event) => {
  const loadingIds = new Set(followLoadingIds.value);
  loadingIds.add(event.id);
  followLoadingIds.value = loadingIds;

  try {
    const nextIds = new Set(followedEventIds.value);

    if (nextIds.has(event.id)) {
      await unfollowEvent(event.id);
      nextIds.delete(event.id);
    } else {
      await followEvent(event.id, 60);
      nextIds.add(event.id);
    }

    followedEventIds.value = nextIds;
  } catch (error) {
    if (error.response?.status === 401) {
      await router.push({
        path: "/login",
        query: { redirect: "/calendar" },
      });

      return;
    }

    console.error("更新關注狀態失敗", error);
  } finally {
    const loadingIds = new Set(followLoadingIds.value);
    loadingIds.delete(event.id);
    followLoadingIds.value = loadingIds;
  }

  onMounted(() => {
  loadMyFollows();
});
};
</script>

<style scoped>
.calendar-dashboard-container {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px;
  font-family:
    -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial,
    sans-serif;
}

.text-slate-800 {
  color: #1e293b;
}

/* 左右雙欄 Grid Layout */
.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 24px;
}

@media (max-width: 992px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

/* 左側 Main Card */
.calendar-main-card {
  background: #ffffff;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  overflow: hidden;
}

/* 右側 Sidebar Card */
.scheduled-sidebar-card {
  background: #ffffff;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
  margin-bottom: 16px;
}

.empty-scheduled {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 240px;
}

/* 行程卡片 (如同圖右側卡片) */
.scheduled-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.scheduled-card {
  position: relative;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
}

.scheduled-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.06);
  background: #ffffff;
}

.card-accent-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
}

.time-label {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  margin-bottom: 4px;
}

.event-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 4px 0;
}

.event-desc {
  font-size: 12px;
  color: #64748b;
  margin: 0;
}

.card-footer-info {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed #e2e8f0;
}

.link-btn {
  font-size: 12px;
  color: #4f46e5;
  text-decoration: none;
  font-weight: 500;
}
.link-btn:hover {
  text-decoration: underline;
}
</style>
