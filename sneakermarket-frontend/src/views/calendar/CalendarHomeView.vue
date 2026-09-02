<template>
  <section class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h1 class="h3 mb-0">行事曆</h1>
      <button type="button" class="btn btn-primary" @click="goToManage">
        Event-Manage
      </button>
    </div>

    <!-- 主月曆卡片容器 -->
    <div class="calendar-widget">
      <!-- 頂部導覽與視圖切換列 -->
      <div class="calendar-header">
        <div class="nav-group">
          <button class="icon-btn" @click="changeMonth(-1)">&lt;</button>
          <h2>{{ currentYear }} 年 {{ currentMonth + 1 }} 月</h2>
          <button class="icon-btn" @click="changeMonth(1)">&gt;</button>
        </div>

        <div class="view-switch">
          <button
            :class="{ active: currentView === 'month' }"
            @click="currentView = 'month'"
          >
            月曆
          </button>
          <button
            :class="{ active: currentView === 'list' }"
            @click="currentView = 'list'"
          >
            列表
          </button>
        </div>
      </div>

      <!-- 1. 月曆檢視 (Month Grid) -->
      <div v-if="currentView === 'month'" class="month-view">
        <div class="weekdays">
          <div v-for="day in weekDaysList" :key="day">{{ day }}</div>
        </div>

        <div class="days">
          <div
            v-for="b in firstDayOffset"
            :key="'b-' + b"
            class="day empty"
          ></div>
          <div
            v-for="day in daysInMonth"
            :key="day"
            class="day"
            :class="{
              today: isToday(day),
              selected: isSelected(day),
              'has-event': getEventsForDay(day).length > 0,
            }"
            @click="selectDate(day)"
          >
            <span class="day-num">{{ day }}</span>
            <div class="event-dots" v-if="getEventsForDay(day).length">
              <span
                v-for="evt in getEventsForDay(day)"
                :key="evt.id"
                class="dot"
                :style="{ backgroundColor: evt.color }"
              ></span>
            </div>
          </div>
        </div>

        <!-- 點擊特定日期的當日預覽 -->
        <div class="day-events-preview" v-if="selectedDate">
          <h3>{{ selectedDateFormatted }} 的活動</h3>
          <div v-if="selectedDayEvents.length === 0" class="no-events">
            今日無排定活動
          </div>
          <div
            v-for="evt in selectedDayEvents"
            :key="evt.id"
            class="event-card cursor-pointer"
            :style="{ borderLeftColor: evt.color }"
            @click="openDetailModal(evt)"
          >
            <div class="event-time">{{ evt.time }}</div>
            <div class="event-title">{{ evt.title }}</div>
          </div>
        </div>
      </div>

      <!-- 2. 列表檢視 (List View) -->
      <div v-else class="list-view-container">
        <!-- 🎯 Event Type 下拉篩選區塊 -->
        <div class="filter-dropdown-wrapper mb-4">
          <div class="dropdown">
            <button
              class="btn btn-outline-secondary dropdown-toggle rounded-3 bg-white shadow-sm"
              type="button"
              @click="toggleFilterDropdown"
            >
              Event Type
              <span v-if="selectedTypes.length"
                >({{ selectedTypes.length }})</span
              >
            </button>

            <!-- 浮動篩選選單 -->
            <div
              v-if="isFilterOpen"
              class="dropdown-menu show p-3 shadow-lg border-0 rounded-4"
              style="width: 320px; z-index: 1000"
            >
              <div class="d-flex flex-wrap gap-2 mb-3">
                <button
                  v-for="type in availableTypes"
                  :key="type.key"
                  type="button"
                  class="type-pill-btn"
                  :class="{ active: tempSelectedTypes.includes(type.key) }"
                  @click="toggleTypeSelection(type.key)"
                >
                  {{ type.label }}
                </button>
              </div>

              <div class="d-flex justify-content-end">
                <button
                  type="button"
                  class="btn btn-dark btn-sm px-4 rounded-3"
                  @click="applyTypeFilter"
                >
                  Apply
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 列表卡片區 -->
        <div v-if="filteredMonthEvents.length === 0" class="no-events-large">
          本月尚無符合條件的預定活動
        </div>

        <div class="list-cards-wrapper">
          <div
            v-for="evt in filteredMonthEvents"
            :key="evt.id"
            class="museum-event-card cursor-pointer"
            :style="{ backgroundColor: evt.color }"
            @click="openDetailModal(evt)"
          >
            <!-- 左側日期白區 -->
            <div class="card-date-badge">
              <span class="badge-month">{{ getEventMonthName(evt.date) }}</span>
              <span class="badge-day">{{ getEventDayNumber(evt.date) }}</span>
            </div>

            <!-- 卡片內容區 -->
            <div class="card-content">
              <!-- 活動照片縮圖 -->
              <img
                :src="`http://localhost:8080/api/calendar/event/${evt.id}/photo`"
                class="card-thumb"
                alt="Event Image"
                @error="(e) => (e.target.src = 'https://picsum.photos/100/100')"
              />

              <!-- 文字資訊 -->
              <div class="card-info">
                <div class="card-category">{{ evt.eventType }}</div>
                <h3 class="card-title">{{ evt.title }}</h3>
                <div class="card-time-pill">🕒 {{ evt.time }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 3. 詳細資料 Modal -->
    <div
      v-if="showDetailModal"
      class="modal fade show d-block"
      tabindex="-1"
      style="background-color: rgba(0, 0, 0, 0.5); z-index: 1050"
      @click.self="closeDetailModal"
    >
      <div class="modal-dialog modal-dialog-centered modal-lg">
        <div
          class="modal-content shadow-lg border-0 rounded-4 position-relative"
        >
          <button
            type="button"
            class="btn-close position-absolute top-0 end-0 m-3"
            style="z-index: 1060; cursor: pointer"
            aria-label="Close"
            @click="closeDetailModal"
          ></button>

          <div class="modal-body text-center pt-4 px-4 pb-4">
            <p class="text-uppercase text-muted fw-bold tracking-wider mb-1">
              {{ selectedModalEvent?.eventType || "EVENT DETAILS" }}
            </p>
            <h2 class="fw-bold mb-4">{{ selectedModalEvent?.title }}</h2>

            <div
              class="event-image-container mb-4 overflow-hidden rounded-3 shadow-sm"
            >
              <img
                :src="`http://localhost:8080/api/calendar/event/${selectedModalEvent?.id}/photo?t=${modalImgTimestamp}`"
                class="img-fluid w-100 object-fit-cover"
                style="max-height: 350px"
                alt="Event Image"
                @error="(e) => (e.target.src = 'https://picsum.photos/800/400')"
              />
            </div>

            <div class="text-start bg-light p-3 rounded-3">

              <p class="mb-2">
                <strong>狀態：</strong> {{ selectedModalEvent?.status }}
              </p>
              <p class="mb-2">
                <strong>活動時間：</strong> {{ selectedModalEvent?.time }}
              </p>
              <p class="mb-2">
                <strong>描述：</strong>
                {{ selectedModalEvent?.description || "無詳細描述" }}
              </p>
              <p class="mb-2" v-if="selectedModalEvent?.targetUrl">
                <strong>相關連結：</strong>
                <a
                  :href="selectedModalEvent.targetUrl"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="text-primary text-decoration-underline ms-1"
                >點此前往       </a>
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { useRouter } from "vue-router";
import axios from "axios";

const router = useRouter();

const goToManage = () => {
  router.push("/calendar/eventManage");
};

const currentView = ref("month");
const weekDaysList = ["日", "一", "二", "三", "四", "五", "六"];

const currentDate = ref(new Date());
const selectedDate = ref(new Date());

const events = ref([]);
const loading = ref(false);

// 🎯 Event Type 相關變數與設定
const isFilterOpen = ref(false);
const selectedTypes = ref([]); // 最終套用的篩選條件
const tempSelectedTypes = ref([]); // 選單內暫存點選的條件

const availableTypes = [
  { key: "RELEASE", label: "新品發售" },
  { key: "SALE", label: "優惠活動" },
  { key: "EVENT", label: "品牌活動" },
  { key: "NOTICE", label: "公告資訊" },
  { key: "OTHER", label: "其他" },
];

// 卡片與彩點背景顏色 Map (模仿圖片色彩風格)
const eventTypeColorMap = {
  RELEASE: "#6366f1", // 靛藍
  SALE: "#d97706", // 琥珀金
  EVENT: "#059669", // 翡翠綠
  NOTICE: "#dc2626", // 珊瑚紅
  OTHER: "#4b5563", // 沉穩灰
};

const currentYear = computed(() => currentDate.value.getFullYear());
const currentMonth = computed(() => currentDate.value.getMonth());

const firstDayOffset = computed(() =>
  new Date(currentYear.value, currentMonth.value, 1).getDay(),
);
const daysInMonth = computed(() =>
  new Date(currentYear.value, currentMonth.value + 1, 0).getDate(),
);

// 格式化月份縮寫 (SEP, OCT...)
const monthNames = [
  "JAN",
  "FEB",
  "MAR",
  "APR",
  "MAY",
  "JUN",
  "JUL",
  "AUG",
  "SEP",
  "OCT",
  "NOV",
  "DEC",
];
const getEventMonthName = (dateStr) => {
  if (!dateStr) return "";
  const m = parseInt(dateStr.split("-")[1], 10) - 1;
  return monthNames[m] || "";
};

const formatDateKey = (year, month, day) => {
  const m = String(month + 1).padStart(2, "0");
  const d = String(day).padStart(2, "0");
  return `${year}-${m}-${d}`;
};

const formatTimeStr = (dateStr) => {
  if (!dateStr) return "";
  const d = new Date(dateStr);
  const hours = String(d.getHours()).padStart(2, "0");
  const minutes = String(d.getMinutes()).padStart(2, "0");
  return `${hours}:${minutes}`;
};

// 🎯 API 載入當月資料
const fetchMonthEvents = async () => {
  loading.value = true;
  const startAt = `${currentYear.value}-${String(currentMonth.value + 1).padStart(2, "0")}-01T00:00:00`;
  const lastDay = daysInMonth.value;
  const endAt = `${currentYear.value}-${String(currentMonth.value + 1).padStart(2, "0")}-${String(lastDay).padStart(2, "0")}T23:59:59`;

  try {
    const response = await axios.get("/api/calendar/eventPage", {
      params: { startAt, endAt, pageNumber: 1 },
    });

    const rawData = response.data.content || [];

    events.value = rawData.map((item) => {
      const datePart = item.startAt ? item.startAt.split("T")[0] : "";
      const startTime = formatTimeStr(item.startAt);
      const endTime = formatTimeStr(item.endAt);

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

// 切換選單顯示
const toggleFilterDropdown = () => {
  isFilterOpen.value = !isFilterOpen.value;
  if (isFilterOpen.value) {
    tempSelectedTypes.value = [...selectedTypes.value];
  }
};

// 點擊膠囊標籤
const toggleTypeSelection = (key) => {
  const index = tempSelectedTypes.value.indexOf(key);
  if (index > -1) {
    tempSelectedTypes.value.splice(index, 1);
  } else {
    tempSelectedTypes.value.push(key);
  }
};

// 點擊 Apply 執行篩選
const applyTypeFilter = () => {
  selectedTypes.value = [...tempSelectedTypes.value];
  isFilterOpen.value = false;
};

const changeMonth = (delta) => {
  const d = new Date(currentDate.value);
  d.setMonth(d.getMonth() + delta);
  currentDate.value = d;
};

const getEventsForDay = (day) => {
  const dateKey = formatDateKey(currentYear.value, currentMonth.value, day);
  return events.value.filter((e) => e.date === dateKey);
};

// 🎯 計算經過 Event Type 篩選後的當月活動列表
const filteredMonthEvents = computed(() => {
  const prefix = `${currentYear.value}-${String(currentMonth.value + 1).padStart(2, "0")}`;

  return events.value
    .filter((e) => {
      const matchMonth = e.date.startsWith(prefix);
      const matchType =
        selectedTypes.value.length === 0 ||
        selectedTypes.value.includes(e.eventType);
      return matchMonth && matchType;
    })
    .sort((a, b) => new Date(a.date) - new Date(b.date));
});

const isToday = (day) => {
  const t = new Date();
  return (
    day === t.getDate() &&
    currentMonth.value === t.getMonth() &&
    currentYear.value === t.getFullYear()
  );
};

const isSelected = (day) => {
  if (!selectedDate.value) return false;
  return (
    day === selectedDate.value.getDate() &&
    currentMonth.value === selectedDate.value.getMonth() &&
    currentYear.value === selectedDate.value.getFullYear()
  );
};

const selectDate = (day) => {
  selectedDate.value = new Date(currentYear.value, currentMonth.value, day);
};

const selectedDateFormatted = computed(() => {
  if (!selectedDate.value) return "";
  return `${selectedDate.value.getMonth() + 1} 月 ${selectedDate.value.getDate()} 日`;
});

const selectedDayEvents = computed(() => {
  if (!selectedDate.value) return [];
  return getEventsForDay(selectedDate.value.getDate());
});

const getEventDayNumber = (dateStr) => {
  if (!dateStr) return "";
  return parseInt(dateStr.split("-")[2], 10);
};

// Modal 控制
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

watch([currentYear, currentMonth], () => {
  fetchMonthEvents();
});

onMounted(() => {
  fetchMonthEvents();
});
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

/* 頂部控制欄 */
.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px; /* 🎯 高度變矮，更俐落 */
  background: #1e293b;
  color: #fff;
}

.nav-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-group h2 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.icon-btn {
  background: rgba(255, 255, 255, 0.15);
  border: none;
  color: #fff;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
}

.view-switch {
  background: rgba(255, 255, 255, 0.1);
  padding: 4px;
  border-radius: 12px;
  display: flex;
}

.view-switch button {
  background: transparent;
  border: none;
  color: #94a3b8;
  padding: 6px 16px;
  font-size: 14px;
  border-radius: 8px;
  cursor: pointer;
}

.view-switch button.active {
  background: #6366f1;
  color: #ffffff;
  font-weight: 600;
}

/* ---------------- 月曆視圖 ---------------- */
.weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  color: #64748b;
  font-size: 14px;
  font-weight: 600;
  padding: 12px 12px 4px;
}

.days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  padding: 4px 12px 12px;
  gap: 4px; /* 🎯 縮小格子之間的間隙 */
}

.day {
  height: 52px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  cursor: pointer;
  position: relative;
  font-size: 14px;
}

.day:hover:not(.empty) {
  background-color: #e2e8f0;
}

.day.today {
  color: #ee5b05;
  font-weight: bold;
}

.day.selected {
  background-color: #6366f1 !important;
  color: #fff !important;
}

.event-dots {
  display: flex;
  gap: 3px;
  margin-top: 2px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.day-events-preview {
  padding: 16px 24px 24px;
  border-top: 1px solid #e2e8f0;
  background: #ffffff;
}

.event-card {
  background: #f8fafc;
  padding: 10px 14px;
  border-radius: 8px;
  border-left: 30px solid #6366f1;
  margin-bottom: 8px;
}

/* ---------------- 🎯 列表檢視 (圖片樣式重構) ---------------- */
.list-view-container {
  padding: 24px;
}

/* Event Type 下拉懸浮窗 */
.filter-dropdown-wrapper {
  position: relative;
}

.type-pill-btn {
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  color: #475569;
  border-radius: 20px;
  padding: 6px 14px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.type-pill-btn.active {
  background: #1e293b;
  color: #ffffff;
  border-color: #1e293b;
}

/* 卡片排版 */
.list-cards-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.museum-event-card {
  display: flex;
  align-items: center;
  border-radius: 20px;
  overflow: hidden;
  color: #ffffff;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06);
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}

.museum-event-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}

/* 左側白色日期區塊 */
.card-date-badge {
  background: #ffffff;
  color: #1e293b;
  width: 90px;
  height: 110px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-top-left-radius: 20px;
  border-bottom-left-radius: 20px;
}

.badge-month {
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
  letter-spacing: 1px;
}

.badge-day {
  font-size: 32px;
  font-weight: 800;
  line-height: 1.1;
}

/* 右側活動內容區 */
.card-content {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 16px 24px;
  width: 100%;
}

/* 活動縮圖 */
.card-thumb {
  width: 70px;
  height: 70px;
  border-radius: 12px;
  object-fit: cover;
  flex-shrink: 0;
}

.card-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-category {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1.2px;
  text-transform: uppercase;
  opacity: 0.85;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
  color: #ffffff;
}

.card-time-pill {
  display: inline-block;
  background: rgba(0, 0, 0, 0.2);
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  width: fit-content;
  margin-top: 4px;
}

.cursor-pointer {
  cursor: pointer;
}

.no-events-large,
.no-events {
  color: #94a3b8;
  font-size: 16px;
  text-align: center;
  padding: 24px 0;
}
</style>
