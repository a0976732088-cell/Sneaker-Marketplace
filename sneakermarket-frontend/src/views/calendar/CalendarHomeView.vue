<template>
  <section class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h1 class="h3 mb-0">行事曆</h1>

      <span>
      <button type="button" class="btn btn-primary" @click="goToManage">
        Event-Manage
      </button>
      </span>
    </div>

    <!-- 主月曆卡片容器 -->
    <div class="calendar-widget">
      <!-- 頂部導覽與視圖切換列 -->
      <CalendarHeader
        :current-year="currentYear"
        :current-month="currentMonth"
        v-model:current-view="currentView"
        @change-month="changeMonth"
      />

      <!-- 1. 月曆檢視 (Month Grid) -->
      <CalendarMonthView
        v-if="currentView === 'month'"
        :events="events"
        :current-year="currentYear"
        :current-month="currentMonth"
        @open-detail="openDetailModal"
      />

      <!-- 2. 列表檢視 (List View) -->
      <CalendarListView
        v-else
        :events="events"
        :current-year="currentYear"
        :current-month="currentMonth"
        @open-detail="openDetailModal"
      />
    </div>

    <!-- 3. 詳細資料 Modal -->
    <EventDetailModal
      v-if="showDetailModal"
      :event="selectedModalEvent"
      :img-timestamp="modalImgTimestamp"
      @close="closeDetailModal"
    />
  </section>
</template>

<script setup>
import { ref, computed, watch } from "vue";
import { useRouter } from "vue-router";
import axios from "axios";

import CalendarHeader from "@/components/calendar/CalendarHeader.vue";
import CalendarMonthView from "@/components/calendar/CalendarMonthView.vue";
import CalendarListView from "@/components/calendar/CalendarListView.vue";
import EventDetailModal from "@/components/calendar/EventDetailModal.vue";

//router, goToManage (路由)
const router = useRouter();
// 點擊按鈕時，透過 Vue Router 跳轉至 /calendar/eventManage 後台管理頁。
const goToManage = () => {
  router.push("/calendar/eventManage");
};

// 紀錄目前切換在 month（月曆檢視）還是 list（列表檢視）。
const currentView = ref("month");
const currentDate = ref(new Date());

// 存放從後端抓回來並轉換完成的活動陣列，提供給子元件渲染。
const events = ref([]);
// API 請求中的狀態 flag（可用於畫面載入動畫）。
const loading = ref(false);

// 卡片與彩點背景顏色 Map (模仿圖片色彩風格)
const eventTypeColorMap = {
  RELEASE: "#6366f1", // 靛藍
  SALE: "#d97706", // 琥珀金
  EVENT: "#059669", // 翡翠綠
  NOTICE: "#dc2626", // 珊瑚紅
  OTHER: "#4b5563", // 沉穩灰
};

// 從 currentDate 算出的年份與月份（0~11），當 currentDate 改變時會自動更新。
const currentYear = computed(() => currentDate.value.getFullYear());
const currentMonth = computed(() => currentDate.value.getMonth());

// 點擊「上一月/下一月」時執行，依照傳入的 1 或 -1 加減月份。
const changeMonth = (delta) => {
  const d = new Date(currentDate.value);
  d.setMonth(d.getMonth() + delta);
  currentDate.value = d;
};

// 工具函式，將完整 ISO 時間字串轉換為 HH:mm 簡短格式（如 14:30）。
const formatTimeStr = (dateStr) => {
  if (!dateStr) return "";
  const d = new Date(dateStr);
  const hours = String(d.getHours()).padStart(2, "0");
  const minutes = String(d.getMinutes()).padStart(2, "0");
  return `${hours}:${minutes}`;
};

// 計算當月第一天與最後一天時間範圍（startAt ~ endAt）。
// 呼叫後端 API 取得當月活動列表。
// 使用 .map() 將後端資料欄位重新整理為前端卡片所需的格式（處理時間格式、色彩對照等）。
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
        params: {
          startAt,
          endAt,
          pageNumber: 1,
          pageSize: 100,
        },
      },
    );

    // 彈性相容各種 API 回應結構
    const rawData =
      response.data.content || response.data.data || response.data || [];

    events.value = rawData.map((item) => {
      // 修正：同時相容 "T" 與空白分割日期，確保取出的 datePart 為 "YYYY-MM-DD"
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
        color: eventTypeColorMap[item.eventType] || "#6366f1",
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

// Watch (放在所有被呼叫的 Function 下面)
watch([currentYear, currentMonth], fetchMonthEvents, { immediate: true });

// Modal 控制(活動詳情彈窗 (Modal) 控制)
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
</script>

<style scoped>
.calendar-widget {
  background: #f8fafc;
  width: 100%;
  max-width: 850px; /* 🎯 從原本滿版縮小至 850px 精緻寬度 */
  margin: 0 auto; /* 🎯 置中顯示 */
  border-radius: 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  font-family:
    system-ui,
    -apple-system,
    sans-serif;
}
</style>
