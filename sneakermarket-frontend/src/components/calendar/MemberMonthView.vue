<template>
  <section class="member-month-view">
    <header class="view-heading">
      <h1>行事曆</h1>
      <p>你已關注的活動，都記錄在這裡。</p>
    </header>

    <div class="month-layout">
      <MemberMonthGrid
        :events="events"
        :current-year="currentYear"
        :current-month="currentMonth"
        :selected-date="selectedDate"
        @change-month="$emit('change-month', $event)"
        @select-date="$emit('select-date', $event)"
        @go-today="$emit('go-today')"
      />

      <MemberMonthDayPanel
        :events="selectedDayEvents"
        :selected-date="selectedDate"
        :follow-loading-ids="followLoadingIds"
        @open-detail="$emit('open-detail', $event)"
        @toggle-follow="$emit('toggle-follow', $event)"
      />
    </div>
  </section>
</template>

<script setup>
import MemberMonthGrid from "./MemberMonthGrid.vue";
import MemberMonthDayPanel from "./MemberMonthDayPanel.vue";

defineProps({
  events: { type: Array, default: () => [] },
  selectedDayEvents: { type: Array, default: () => [] },
  currentYear: { type: Number, required: true },
  currentMonth: { type: Number, required: true },
  selectedDate: { type: Date, required: true },
  followLoadingIds: { type: Set, default: () => new Set() },
});

defineEmits([
  "change-month",
  "select-date",
  "go-today",
  "open-detail",
  "toggle-follow",
]);
</script>

<style scoped>
.member-month-view {
  min-width: 0;
}

.view-heading {
  margin-bottom: 18px;
}

.view-heading h1 {
  margin: 0;
  color: #202326;
  font-size: 32px;
  font-weight: 800;
}

.view-heading p {
  margin: 4px 0 0;
  color: #697079;
  font-size: 14px;
}

.month-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(0, 1fr);
  align-items: start;
  gap: 16px;
}

@media (max-width: 900px) {
  .month-layout {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>