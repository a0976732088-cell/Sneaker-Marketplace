<template>
  <div class="month-view">
    <div class="weekdays">
      <span v-for="day in weekDays" :key="day">{{ day }}</span>
    </div>

    <div class="days">
      <span
        v-for="blank in firstDayOffset"
        :key="`blank-${blank}`"
        aria-hidden="true"
      ></span>

      <button
        v-for="day in daysInMonth"
        :key="day"
        type="button"
        class="day"
        :class="{
          selected: isSelected(day),
          today: isToday(day),
        }"
        :aria-label="`${currentYear} 年 ${currentMonth + 1} 月 ${day} 日，${getDayEvents(day).length} 個活動`"
        :aria-pressed="isSelected(day)"
        @click="$emit('select-date', new Date(currentYear, currentMonth, day))"
      >
        <span>{{ day }}</span>

        <span v-if="getDayEvents(day).length" class="event-dots">
          <i
            v-for="event in getDayEvents(day).slice(0, 3)"
            :key="event.id"
            :style="{ backgroundColor: event.color }"
          ></i>
        </span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { formatDateKey, isEventOnDate } from "@/utils/dateUtils";

const props = defineProps({
  events: { type: Array, default: () => [] },
  currentYear: { type: Number, required: true },
  currentMonth: { type: Number, required: true },
  selectedDate: { type: Date, default: null },
});

defineEmits(["select-date"]);

const weekDays = ["日", "一", "二", "三", "四", "五", "六"];

const firstDayOffset = computed(() =>
  new Date(props.currentYear, props.currentMonth, 1).getDay(),
);

const daysInMonth = computed(() =>
  new Date(props.currentYear, props.currentMonth + 1, 0).getDate(),
);

function getDayEvents(day) {
  const key = formatDateKey(props.currentYear, props.currentMonth, day);
  return props.events.filter((event) => isEventOnDate(event, key));
}

function isSelected(day) {
  return (
    props.selectedDate?.getFullYear() === props.currentYear &&
    props.selectedDate?.getMonth() === props.currentMonth &&
    props.selectedDate?.getDate() === day
  );
}

function isToday(day) {
  const today = new Date();
  return (
    today.getFullYear() === props.currentYear &&
    today.getMonth() === props.currentMonth &&
    today.getDate() === day
  );
}
</script>

<style scoped>
.month-view {
  padding: 14px;
  border: 1px solid #e5e8e9;
  border-radius: 0 0 12px 12px;
  background: #fff;
}

.weekdays,
.days {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  text-align: center;
}

.weekdays {
  margin-bottom: 8px;
  color: #737c82;
  font-size: 12px;
  font-weight: 600;
}

.weekdays span {
  padding: 8px 0;
}

.day {
  display: flex;
  min-width: 0;
  height: 48px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: #30373b;
  font-size: 13px;
  cursor: pointer;
}

.day:hover {
  background: #edf1ef;
}

.day.today {
  font-weight: 800;
}

.day.selected {
  background: #8499ae;
  color: #fff;
}

.event-dots {
  display: flex;
  justify-content: center;
  gap: 3px;
  height: 4px;
}

.event-dots i {
  width: 4px;
  height: 4px;
  border-radius: 50%;
}

.day:focus-visible {
  outline: 2px solid #202326;
  outline-offset: 2px;
}
</style>