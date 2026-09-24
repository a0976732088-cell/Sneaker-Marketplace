<template>
  <aside class="member-calendar-panel">
    <header class="panel-heading">
      <h2>會員行事曆</h2>
      <p>記錄你的球鞋生活，不錯過任何一場相遇</p>
    </header>

    <div class="panel-columns">
      <div class="calendar-column">
        <CalendarHeader
          :current-year="currentYear"
          :current-month="currentMonth"
          @change-month="$emit('change-month', $event)"
        />
        <CalendarMonthView
          :events="events"
          :current-year="currentYear"
          :current-month="currentMonth"
          :selected-date="selectedDate"
          @select-date="$emit('select-date', $event)"
        />
      </div>

      <div class="day-column">
        <div class="day-heading">
          <h3>{{ selectedDateLabel }}</h3>
          <span>共 {{ selectedDayEvents.length }} 場活動</span>
        </div>

        <p v-if="selectedDayEvents.length === 0" class="empty-day">
          這一天尚無活動
        </p>

        <SelectedDayEvent
          v-for="event in selectedDayEvents"
          :key="event.id"
          :event="event"
          :followed="followedIds.has(event.id)"
          :follow-loading="followLoadingIds.has(event.id)"
          @view="$emit('open-detail', $event)"
          @follow="$emit('toggle-follow', $event)"
        />
      </div>
    </div>
  </aside>
</template>

<script setup>
import { computed } from "vue";
import CalendarHeader from "./CalendarHeader.vue";
import CalendarMonthView from "./CalendarMonthView.vue";
import SelectedDayEvent from "./SelectedDayEvent.vue";

const props = defineProps({
  events: { type: Array, default: () => [] },
  selectedDayEvents: { type: Array, default: () => [] },
  currentYear: { type: Number, required: true },
  currentMonth: { type: Number, required: true },
  selectedDate: { type: Date, required: true },
  followedIds: { type: Set, default: () => new Set() },
  followLoadingIds: { type: Set, default: () => new Set() },
});

defineEmits([
  "change-month",
  "select-date",
  "open-detail",
  "toggle-follow",
]);

const selectedDateLabel = computed(() =>
  props.selectedDate.toLocaleDateString("zh-TW", {
    year: "numeric",
    month: "long",
    day: "numeric",
    weekday: "short",
  }),
);
</script>

<style scoped>
.member-calendar-panel {
  min-width: 0;
  padding: 20px;
  border: 1px solid #e8eaeb;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 5px 22px rgb(0 0 0 / 4%);
}

.panel-heading {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.panel-heading h2 {
  margin: 0;
  color: #202326;
  font-size: 19px;
  font-weight: 800;
}

.panel-heading p {
  margin: 0;
  color: #697079;
  font-size: 12px;
}

.panel-columns {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 0.95fr);
  gap: 16px;
}

.day-column {
  min-width: 0;
}

.day-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 0;
  border-bottom: 1px solid #e8eaeb;
}

.day-heading h3 {
  margin: 0;
  color: #202326;
  font-size: 15px;
  font-weight: 700;
}

.day-heading span {
  flex-shrink: 0;
  color: #697079;
  font-size: 11px;
}

.empty-day {
  padding: 30px 8px;
  color: #697079;
  font-size: 13px;
  text-align: center;
}

.panel-quote {
  margin: 16px 0 0;
  padding: 18px 20px;
  border-radius: 12px;
  background: #eef0f1;
  color: #4c5459;
  font-size: 14px;
  letter-spacing: 0.06em;
}

@media (max-width: 1100px) {
  .panel-columns {
    grid-template-columns: 1fr;
  }
}
</style>