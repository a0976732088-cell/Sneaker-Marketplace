<!-- components/calendar/CalendarListView.vue -->
<template>
  <div class="list-view-container">
    <!-- 🎯 Event Type 下拉篩選區塊 -->
    <div class="filter-dropdown-wrapper mb-4">
      <div class="dropdown">
        <button
          class="btn btn-outline-secondary dropdown-toggle rounded-pill bg-white shadow-sm border-0 px-3 py-2 text-dark font-medium"
          type="button"
          @click="toggleFilterDropdown"
        >
          Event Type
          <span v-if="selectedTypes.length" class="badge bg-primary rounded-pill ms-1">
            {{ selectedTypes.length }}
          </span>
        </button>

        <!-- 浮動篩選選單 -->
        <div
          v-if="isFilterOpen"
          class="dropdown-menu show p-3 shadow-lg border-0 rounded-4 mt-2"
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
              class="btn btn-primary btn-sm px-4 rounded-pill fw-bold"
              @click="applyTypeFilter"
            >
              套用篩選
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 列表卡片區 -->
    <div v-if="filteredMonthEvents.length === 0" class="no-events-large">
      <i class="bi bi-calendar-x display-6 d-block mb-2 text-muted"></i>
      本月尚無符合條件的預定活動
    </div>

    <div class="list-cards-wrapper">
      <div
        v-for="evt in filteredMonthEvents"
        :key="evt.id"
        class="modern-event-card cursor-pointer"
        @click="$emit('open-detail', evt)"
      >
        <!-- 左側日期徽章 -->
        <div class="card-date-badge">
          <span class="badge-month">{{ getEventMonthName(evt.date) }}</span>
          <span class="badge-day">{{ getEventDayNumber(evt.date) }}</span>
        </div>

        <!-- 卡片主要內容 -->
        <div class="card-body-content">
          <!-- 活動圖片 -->
          <img
            :src="`http://localhost:8080/api/calendar/event/${evt.id}/photo`"
            class="card-thumb"
            alt="Event Image"
            @error="(e) => (e.target.src = 'https://picsum.photos/100/100')"
          />

          <!-- 文字資訊 -->
          <div class="card-info">
            <div class="d-flex align-items-center gap-2 mb-1">
              <span class="type-badge" :class="getBadgeClass(evt.eventType)">
                {{ getEventTypeLabel(evt.eventType) }}
              </span>
              <span class="card-time-text">
                🕒 {{ evt.time }}
              </span>
            </div>
            <h3 class="card-title">{{ evt.title }}</h3>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from "vue";

const props = defineProps({
  events: { type: Array, default: () => [] },
  currentYear: { type: Number, required: true },
  currentMonth: { type: Number, required: true },
});

defineEmits(["open-detail"]);

const isFilterOpen = ref(false);
const selectedTypes = ref([]);
const tempSelectedTypes = ref([]);

const availableTypes = [
  { key: "RELEASE", label: "新品發售" },
  { key: "SALE", label: "優惠活動" },
  { key: "EVENT", label: "品牌活動" },
  { key: "NOTICE", label: "公告資訊" },
  { key: "OTHER", label: "其他" },
];

const monthNames = [
  "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
  "JUL", "AUG", "SEP", "OCT", "NOV", "DEC",
];

const getEventMonthName = (dateStr) => {
  if (!dateStr) return "";
  const m = parseInt(dateStr.split("-")[1], 10) - 1;
  return monthNames[m] || "";
};

const getEventDayNumber = (dateStr) => {
  if (!dateStr) return "";
  return parseInt(dateStr.split("-")[2], 10);
};

const getEventTypeLabel = (typeKey) => {
  const found = availableTypes.find((t) => t.key === typeKey);
  return found ? found.label : typeKey;
};

const getBadgeClass = (typeKey) => {
  switch (typeKey) {
    case "RELEASE": return "badge-release";
    case "SALE": return "badge-sale";
    case "EVENT": return "badge-event";
    case "NOTICE": return "badge-notice";
    default: return "badge-other";
  }
};

const toggleFilterDropdown = () => {
  isFilterOpen.value = !isFilterOpen.value;
  if (isFilterOpen.value) {
    tempSelectedTypes.value = [...selectedTypes.value];
  }
};

const toggleTypeSelection = (key) => {
  const index = tempSelectedTypes.value.indexOf(key);
  if (index > -1) {
    tempSelectedTypes.value.splice(index, 1);
  } else {
    tempSelectedTypes.value.push(key);
  }
};

const applyTypeFilter = () => {
  selectedTypes.value = [...tempSelectedTypes.value];
  isFilterOpen.value = false;
};

const filteredMonthEvents = computed(() => {
  const prefix = `${props.currentYear}-${String(props.currentMonth + 1).padStart(2, "0")}`;
  return props.events
    .filter((e) => {
      const matchMonth = e.date.startsWith(prefix);
      const matchType =
        selectedTypes.value.length === 0 ||
        selectedTypes.value.includes(e.eventType);
      return matchMonth && matchType;
    })
    .sort((a, b) => new Date(a.date) - new Date(b.date));
});
</script>

<style scoped>
.list-view-container {
  padding: 24px;
}

.filter-dropdown-wrapper {
  position: relative;
}

.type-pill-btn {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  color: #475569;
  border-radius: 20px;
  padding: 6px 14px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.type-pill-btn.active {
  background: #4f46e5;
  color: #ffffff;
  border-color: #4f46e5;
}

.list-cards-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 現代化卡片設計 */
.modern-event-card {
  display: flex;
  align-items: center;
  background: #ffffff;
  border: 1px solid #f1f5f9;
  border-radius: 16px;
  padding: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.modern-event-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
}

/* 左側日期區塊 */
.card-date-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 68px;
  height: 68px;
  background: #f8fafc;
  border-radius: 12px;
  flex-shrink: 0;
  margin-right: 16px;
}

.badge-month {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  letter-spacing: 0.5px;
}

.badge-day {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1;
}

/* 卡片內容 */
.card-body-content {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
}

.card-thumb {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  object-fit: cover;
  flex-shrink: 0;
}

.card-info {
  display: flex;
  flex-direction: column;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 2px 0 0;
}

.card-time-text {
  font-size: 12px;
  color: #64748b;
}

/* 輕量化標籤 (Badge Types) */
.type-badge {
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 700;
}

.badge-release { background: #eef2ff; color: #4f46e5; }
.badge-sale    { background: #fff7ed; color: #ea580c; }
.badge-event   { background: #f0fdf4; color: #16a34a; }
.badge-notice  { background: #fef2f2; color: #dc2626; }
.badge-other   { background: #f1f5f9; color: #475569; }

.cursor-pointer { cursor: pointer; }

.no-events-large {
  color: #94a3b8;
  font-size: 15px;
  text-align: center;
  padding: 40px 0;
}
</style>