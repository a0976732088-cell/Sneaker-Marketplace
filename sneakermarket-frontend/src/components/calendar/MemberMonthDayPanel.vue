<template>
  <aside class="month-day-panel">
    <header class="panel-heading">
      <h2>{{ dateLabel }} · 活動</h2>
      <span>共 {{ events.length }} 個活動</span>
    </header>

    <p v-if="events.length === 0" class="empty-message">
      這一天沒有已關注的活動
    </p>

    <div v-else class="event-list">
      <article v-for="event in events" :key="event.id" class="event-card">
        <div class="card-top">
          <span
            class="type-badge"
            :style="{ backgroundColor: event.color }"
          >
            {{ getEventType(event.eventType).label }}
          </span>
          <span class="event-time">{{ event.time || "全天" }}</span>
        </div>

        <div class="card-body">
          <div class="event-image">
            <img
              v-if="event.photoUrl && !failedImageIds.has(event.id)"
              :src="event.photoUrl"
              :alt="event.title"
              @error="markImageFailed(event.id)"
            />
            <span v-else>EVENT</span>
          </div>

          <div class="event-info">
            <h3>{{ event.title }}</h3>
            <p v-if="event.description" class="description">
              {{ event.description }}
            </p>

            <div class="event-actions">
              <button
                type="button"
                class="detail-button"
                @click="$emit('open-detail', event)"
              >
                查看內容
              </button>
              <button
                type="button"
                class="unfollow-button"
                :disabled="followLoadingIds.has(event.id)"
                @click="$emit('toggle-follow', event)"
              >
                {{
                  followLoadingIds.has(event.id)
                    ? "處理中…"
                    : "取消關注"
                }}
              </button>
            </div>
          </div>
        </div>
      </article>
    </div>
  </aside>
</template>

<script setup>
import { computed, ref } from "vue";
import { getEventType } from "@/constants/eventTypes";

const props = defineProps({
  selectedDate: { type: Date, required: true },
  events: { type: Array, default: () => [] },
  followLoadingIds: { type: Set, default: () => new Set() },
});

defineEmits(["open-detail", "toggle-follow"]);

const failedImageIds = ref(new Set());

const dateLabel = computed(
  () =>
    `${props.selectedDate.getMonth() + 1} 月 ` +
    `${props.selectedDate.getDate()} 日`,
);

function markImageFailed(eventId) {
  failedImageIds.value = new Set([...failedImageIds.value, eventId]);
}
</script>

<style scoped>
.month-day-panel {
  min-width: 0;
  padding: 16px;
  border: 1px solid #e5e8e9;
  border-radius: 12px;
  background: #fff;
}

.panel-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-heading h2 {
  margin: 0;
  color: #202326;
  font-size: 18px;
  font-weight: 700;
}

.panel-heading span,
.event-time {
  color: #697079;
  font-size: 12px;
}

.empty-message {
  padding: 32px 8px;
  color: #697079;
  text-align: center;
}

.event-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.event-card {
  padding: 10px;
  border: 1px solid #e5e8e9;
  border-radius: 10px;
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 9px;
}

.type-badge {
  padding: 3px 8px;
  border-radius: 999px;
  color: #24282b;
  font-size: 11px;
  font-weight: 600;
}

.card-body {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 12px;
}

.event-image {
  display: grid;
  min-height: 90px;
  place-items: center;
  overflow: hidden;
  border-radius: 8px;
  background: #eef0f1;
  color: #697079;
  font-size: 10px;
  letter-spacing: 0.12em;
}

.event-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.event-info {
  min-width: 0;
}

.event-info h3 {
  margin: 0 0 6px;
  color: #202326;
  font-size: 15px;
  font-weight: 700;
}

.description {
  display: -webkit-box;
  overflow: hidden;
  margin: 0;
  color: #697079;
  font-size: 12px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  line-clamp: 2;
  -webkit-line-clamp: 2;
}

.event-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.event-actions button {
  min-height: 30px;
  padding: 4px 10px;
  border-radius: 7px;
  font-size: 12px;
  cursor: pointer;
}

.detail-button {
  border: 1px solid #e7664e;
  background: #e7664e;
  color: #fff;
}

.unfollow-button {
  border: 1px solid #cbd0d3;
  background: #fff;
  color: #24282b;
}

.unfollow-button:disabled {
  cursor: wait;
  opacity: 0.6;
}
</style>