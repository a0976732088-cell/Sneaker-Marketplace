<!-- 列表視圖 (含 Event Type 下拉篩選) -->
 <template>
  <div class="list-view-container">
    <!-- 🎯 Event Type 下拉篩選區塊 -->
    <div class="filter-dropdown-wrapper mb-4">
      <div class="dropdown">
        <button
          class="btn btn-outline-secondary dropdown-toggle rounded-3 bg-white shadow-sm"
          type="button"
          @click="toggleFilterDropdown"
        >
          Event Type
          <span v-if="selectedTypes.length">({{ selectedTypes.length }})</span>
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
        @click="$emit('open-detail', evt)"
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
</template>

<script setup>
import { ref, computed } from "vue";

const props = defineProps({
  events: {
    type: Array,
    default: () => [],
  },
  currentYear: {
    type: Number,
    required: true,
  },
  currentMonth: {
    type: Number,
    required: true,
  },
});

defineEmits(["open-detail"]);

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

// 🎯 計算經過 Event Type 篩選後的當月活動列表
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
/* ---------------- 🎯 列表檢視 ---------------- */
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

.no-events-large {
  color: #94a3b8;
  font-size: 16px;
  text-align: center;
  padding: 24px 0;
}
</style>