<template>
  <section class="member-month-grid">
    <header class="month-toolbar">
      <div class="month-heading">
        <button type="button" aria-label="上一個月" @click="$emit('change-month', -1)">
          ‹
        </button>
        <button type="button" aria-label="下一個月" @click="$emit('change-month', 1)">
          ›
        </button>
        <h2>{{ currentYear }} 年 {{ currentMonth + 1 }} 月</h2>
      </div>

      <button type="button" class="today-button" @click="$emit('go-today')">
        今天
      </button>
    </header>

    <div class="weekdays">
      <span v-for="day in weekDays" :key="day">{{ day }}</span>
    </div>

    <div class="days">
      <button
        v-for="cell in cells"
        :key="cell.key"
        type="button"
        class="day-cell"
        :class="{
          outside: !cell.inMonth,
          selected: isSelected(cell),
        }"
        :disabled="!cell.inMonth"
        :aria-label="`${cell.key}，${eventsFor(cell).length} 個已關注活動`"
        :aria-pressed="isSelected(cell)"
        @click="$emit('select-date', cell.date)"
      >
        <span class="day-number">{{ cell.date.getDate() }}</span>

        <span class="day-events">
          <span
            v-for="event in eventsFor(cell).slice(0, 2)"
            :key="event.id"
            class="day-event"
          >
            <i :style="{ backgroundColor: event.color }"></i>
            {{ getEventType(event.eventType).label }}
          </span>

          <span v-if="eventsFor(cell).length > 2" class="more-events">
            +{{ eventsFor(cell).length - 2 }} 個活動
          </span>
        </span>
      </button>
    </div>
  </section>
</template>

<script setup>
import { computed } from "vue";
import { getEventType } from "@/constants/eventTypes";
import { formatDateKey, isEventOnDate } from "@/utils/dateUtils";

const props = defineProps({
  events: { type: Array, default: () => [] },
  currentYear: { type: Number, required: true },
  currentMonth: { type: Number, required: true },
  selectedDate: { type: Date, required: true },
});

defineEmits(["change-month", "select-date", "go-today"]);

const weekDays = ["週日", "週一", "週二", "週三", "週四", "週五", "週六"];

const cells = computed(() => {
  const firstWeekday = new Date(
    props.currentYear,
    props.currentMonth,
    1,
  ).getDay();

  const daysInMonth = new Date(
    props.currentYear,
    props.currentMonth + 1,
    0,
  ).getDate();

  const cellCount = Math.ceil((firstWeekday + daysInMonth) / 7) * 7;

  return Array.from({ length: cellCount }, (_, index) => {
    const date = new Date(
      props.currentYear,
      props.currentMonth,
      1 - firstWeekday + index,
    );

    return {
      date,
      key: formatDateKey(
        date.getFullYear(),
        date.getMonth(),
        date.getDate(),
      ),
      inMonth: date.getMonth() === props.currentMonth,
    };
  });
});

function eventsFor(cell) {
  return props.events.filter((event) => isEventOnDate(event, cell.key));
}

function isSelected(cell) {
  return (
    cell.date.getFullYear() === props.selectedDate.getFullYear() &&
    cell.date.getMonth() === props.selectedDate.getMonth() &&
    cell.date.getDate() === props.selectedDate.getDate()
  );
}
</script>

<style scoped>
.member-month-grid {
  min-width: 0;
  overflow: hidden;
  border: 1px solid #e5e8e9;
  border-radius: 12px;
  background: #fff;
}

.month-toolbar,
.month-heading {
  display: flex;
  align-items: center;
}

.month-toolbar {
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #e5e8e9;
}

.month-heading {
  gap: 8px;
}

.month-heading h2 {
  margin: 0 0 0 4px;
  font-size: 18px;
  font-weight: 700;
}

.month-heading button,
.today-button {
  min-width: 30px;
  height: 30px;
  border: 0;
  border-radius: 8px;
  background: #eef1f0;
  color: #30373b;
  cursor: pointer;
}

.today-button {
  padding: 0 12px;
  background: #fff;
  border: 1px solid #d8dcde;
  font-size: 12px;
}

.weekdays,
.days {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
}

.weekdays span {
  padding: 9px 4px;
  border-right: 1px solid #e5e8e9;
  color: #697079;
  font-size: 12px;
  text-align: center;
}

.day-cell {
  display: flex;
  min-width: 0;
  min-height: 86px;
  flex-direction: column;
  align-items: flex-start;
  gap: 5px;
  padding: 8px;
  border: 0;
  border-top: 1px solid #e5e8e9;
  border-right: 1px solid #e5e8e9;
  background: #fff;
  color: #202326;
  text-align: left;
  cursor: pointer;
}

.day-cell:hover:not(:disabled) {
  background: #f7f8f7;
}

.day-cell.outside {
  color: #aab0b4;
  cursor: default;
}

.day-number {
  display: grid;
  width: 25px;
  height: 25px;
  place-items: center;
  border-radius: 50%;
  font-size: 12px;
}

.day-cell.selected .day-number {
  background: #e7664e;
  color: #fff;
}

.day-events {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.day-event,
.more-events {
  overflow: hidden;
  max-width: 100%;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.day-event i {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 3px;
  border-radius: 50%;
}

.more-events {
  color: #697079;
}
</style>