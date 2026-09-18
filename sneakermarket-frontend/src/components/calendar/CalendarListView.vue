<template>
  <section class="calendar-list">
    <EventTypeFilters v-model="selectedType" />

    <p v-if="visibleEvents.length === 0" class="empty-message">
      這個月份沒有符合條件的活動
    </p>

    <div v-else class="event-list">
      <CalendarEventCard
        v-for="event in pageEvents"
        :key="event.id"
        :event="event"
        :followed="followedIds.has(event.id)"
        :follow-loading="followLoadingIds.has(event.id)"
        @view="$emit('open-detail', $event)"
        @follow="$emit('toggle-follow', $event)"
      />
    </div>

    <nav v-if="pageCount > 1" class="pagination" aria-label="活動分頁">
      <button
        type="button"
        :disabled="page === 1"
        aria-label="上一頁"
        @click="page--"
      >
        ‹
      </button>

      <button
        v-for="number in pageCount"
        :key="number"
        type="button"
        :class="{ active: page === number }"
        :aria-label="`第 ${number} 頁`"
        :aria-current="page === number ? 'page' : undefined"
        @click="page = number"
      >
        {{ number }}
      </button>

      <button
        type="button"
        :disabled="page === pageCount"
        aria-label="下一頁"
        @click="page++"
      >
        ›
      </button>
    </nav>
  </section>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import EventTypeFilters from "./EventTypeFilters.vue";
import CalendarEventCard from "./CalendarEventCard.vue";

const props = defineProps({
  events: { type: Array, default: () => [] },
  currentYear: { type: Number, required: true },
  currentMonth: { type: Number, required: true },
  followedIds: { type: Set, default: () => new Set() },
  followLoadingIds: { type: Set, default: () => new Set() },
});

defineEmits(["open-detail", "toggle-follow"]);

const selectedType = ref(null);
const page = ref(1);
const pageSize = 3;

const visibleEvents = computed(() => {
  const monthKey = `${props.currentYear}-${String(props.currentMonth + 1).padStart(2, "0")}`;

  return props.events
    .filter(
      (event) =>
        event.date?.startsWith(`${monthKey}-`) &&
        (!selectedType.value || event.eventType === selectedType.value),
    )
    .sort((a, b) => (a.startAt || "").localeCompare(b.startAt || ""));
});

const pageCount = computed(() =>
  Math.ceil(visibleEvents.value.length / pageSize),
);

const pageEvents = computed(() => {
  const start = (page.value - 1) * pageSize;
  return visibleEvents.value.slice(start, start + pageSize);
});

watch(
  [
    selectedType,
    () => props.currentYear,
    () => props.currentMonth,
    () => props.events,
  ],
  () => {
    page.value = 1;
  },
);
</script>

<style scoped>
.calendar-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.event-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.empty-message {
  margin: 0;
  padding: 56px 20px;
  border: 1px dashed #d8dcde;
  border-radius: 14px;
  color: #697079;
  text-align: center;
}

.pagination {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.pagination button {
  min-width: 34px;
  height: 34px;
  border: 1px solid #d8dcde;
  border-radius: 8px;
  background: #fff;
  color: #24282b;
  cursor: pointer;
}

.pagination button.active {
  border-color: #e7664e;
  background: #e7664e;
  color: #fff;
}

.pagination button:disabled {
  cursor: default;
  opacity: 0.4;
}
</style>
