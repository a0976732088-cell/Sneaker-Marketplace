<!-- components/calendar/CalendarMonthView.vue -->
<template>
  <div class="month-grid-wrapper">
    <!-- 1. 星期標頭（嚴格 7 欄） -->
    <div class="weekdays-grid">
      <div v-for="day in weekDaysList" :key="day" class="weekday-cell">
        {{ day }}
      </div>
    </div>

    <!-- 2. 日期網格（嚴格 7 欄） -->
    <div class="days-grid">
      <!-- 月初前面的空白格 -->
      <div
        v-for="b in firstDayOffset"
        :key="'blank-' + b"
        class="day-cell empty"
      ></div>

      <!-- 當月實際日期格 -->
      <div
        v-for="day in daysInMonth"
        :key="day"
        class="day-cell"
        :class="{
          today: isToday(day),
          selected: isSelected(day)
        }"
        @click="onDateClick(day)"
      >
        <div class="day-header">
          <span class="day-number">{{ day }}</span>
        </div>

        <!-- 格子內的活動標籤 -->
        <div class="cell-events">
          <div
            v-for="evt in getEventsForDay(day).slice(0, 2)"
            :key="evt.id"
            class="event-pill"
            :style="{
              backgroundColor: evt.color + '15',
              color: evt.color,
              borderColor: evt.color + '30'
            }"
            @click.stop="$emit('open-detail', evt)"
          >
            <span class="pill-dot" :style="{ backgroundColor: evt.color }"></span>
            <span class="pill-title">{{ evt.title }}</span>
          </div>

          <div v-if="getEventsForDay(day).length > 2" class="more-count">
            +{{ getEventsForDay(day).length - 2 }} 個活動
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  events: { type: Array, default: () => [] },
  currentYear: { type: Number, required: true },
  currentMonth: { type: Number, required: true },
  selectedDate: { type: Date, default: null },
});

const emit = defineEmits(["open-detail", "select-date"]);

const weekDaysList = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

const firstDayOffset = computed(() =>
  new Date(props.currentYear, props.currentMonth, 1).getDay()
);

const daysInMonth = computed(() =>
  new Date(props.currentYear, props.currentMonth + 1, 0).getDate()
);

const formatDateKey = (year, month, day) => {
  const m = String(month + 1).padStart(2, "0");
  const d = String(day).padStart(2, "0");
  return `${year}-${m}-${d}`;
};

const getEventsForDay = (day) => {
  const dateKey = formatDateKey(props.currentYear, props.currentMonth, day);
  return props.events.filter((e) => e.date === dateKey);
};

const isToday = (day) => {
  const t = new Date();
  return (
    day === t.getDate() &&
    props.currentMonth === t.getMonth() &&
    props.currentYear === t.getFullYear()
  );
};

const isSelected = (day) => {
  if (!props.selectedDate) return false;
  return (
    day === props.selectedDate.getDate() &&
    props.currentMonth === props.selectedDate.getMonth() &&
    props.currentYear === props.selectedDate.getFullYear()
  );
};

const onDateClick = (day) => {
  const d = new Date(props.currentYear, props.currentMonth, day);
  emit("select-date", d);
};
</script>

<style scoped>
.month-grid-wrapper {
  padding: 20px;
  width: 100%;
  box-sizing: border-box;
}

/* 🎯 關鍵修復 1：星期列強制均分 7 欄 */
.weekdays-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
  text-align: center;
  font-size: 13px;
  font-weight: 700;
  color: #94a3b8;
  margin-bottom: 12px;
}

.weekday-cell {
  padding: 4px 0;
}

/* 🎯 關鍵修復 2：日期列強制均分 7 欄，禁止橫向自動擴充 */
.days-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
  width: 100%;
}

/* 日期卡片 */
.day-cell {
  min-height: 96px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 12px;
  padding: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  /* 確保內容不撐開格子 */
  min-width: 0; 
}

.day-cell:hover:not(.empty) {
  background: #ffffff;
  border-color: #cbd5e1;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

.day-cell.today {
  border-color: #818cf8;
  background: #f5f3ff;
}

.day-cell.today .day-number {
  color: #4f46e5;
  font-weight: 800;
}

.day-cell.selected {
  outline: 2px solid #4f46e5;
  background: #ffffff;
}

.day-header {
  margin-bottom: 6px;
}

.day-number {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

/* 🎯 關鍵修復 3：膠囊標籤文字長度防爆 */
.cell-events {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
  overflow: hidden;
}

.event-pill {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 6px;
  border-radius: 6px;
  border: 1px solid transparent;
  font-size: 11px;
  font-weight: 600;
  width: 100%;
  box-sizing: border-box;
  overflow: hidden;
}

.pill-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  flex-shrink: 0;
}

.pill-title {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
}

.more-count {
  font-size: 10px;
  color: #94a3b8;
  font-weight: 600;
  margin-top: 2px;
}

/* 空白格保持高度但不顯示背景 */
.day-cell.empty {
  background: transparent;
  border: 1px solid transparent;
  cursor: default;
}
</style>