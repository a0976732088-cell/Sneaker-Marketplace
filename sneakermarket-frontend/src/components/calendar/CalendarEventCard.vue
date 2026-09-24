<template>
  <article class="event-card">
    <div class="event-image">
      <img
        v-if="event.photoUrl && !imageFailed"
        :src="event.photoUrl"
        :alt="event.title"
        @error="imageFailed = true"
      />
      <span v-else>EVENT</span>
    </div>

    <div class="event-content">
      <div class="event-meta">
        <p class="event-date">{{ event.dateTimeRange }}</p>
        <span class="type-badge" :style="{ backgroundColor: event.color }">
          {{ getEventType(event.eventType).label }}
        </span>
      </div>

      <h3>{{ event.title }}</h3>
      <p v-if="event.description" class="event-description">
        {{ event.description }}
      </p>

      <div class="event-actions">
        <button
          type="button"
          class="detail-button"
          @click="$emit('view', event)"
        >
          查看內容 <span aria-hidden="true">→</span>
        </button>
        <button
          type="button"
          class="follow-button"
          :disabled="followLoading"
          :aria-pressed="followed"
          @click="$emit('follow', event)"
        >
          {{ followLoading ? "處理中…" : followed ? "已關注 ♡" : "關注活動 ♡" }}
        </button>
      </div>
    </div>
  </article>
</template>

<script setup>
import { ref } from "vue";
import { getEventType } from "@/constants/eventTypes";

defineProps({
  event: { type: Object, required: true },
  followed: { type: Boolean, default: false },
  followLoading: { type: Boolean, default: false },
});

defineEmits(["view", "follow"]);

const imageFailed = ref(false);
</script>

<style scoped>
.event-card {
  position: relative;
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 10px;
  min-height: 132px;
  padding: 8px;
  border: 1px solid #e8e8e8;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 5px 18px rgb(0 0 0 / 4%);
}

.event-image {
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 11px;
  background: #e8e9e8;
  color: #656b6a;
  font-size: 12px;
  letter-spacing: 0.2em;
}

.event-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.event-content {
  min-width: 0;
  padding: 6px 0;
}

.event-meta {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.event-date {
  margin: 0;
  color: #697079;
  font-size: 12px;
  line-height: 1.4;
  white-space: pre-wrap;
}

.event-content h3 {
  margin: 0 0 6px;
  color: #202326;
  font-size: 18px;
  font-weight: 700;
}

.event-description {
  overflow: hidden;
  margin: 0;
  color: #697079;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.event-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.event-actions button {
  min-height: 34px;
  padding: 5px 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
}

.detail-button {
  border: 1px solid #e7664e;
  background: #e7664e;
  color: #fff;
}

.follow-button {
  border: 1px solid #cbd0d3;
  background: #fff;
  color: #24282b;
}

.follow-button:disabled {
  cursor: wait;
  opacity: 0.6;
}

.type-badge {
  align-self: flex-end;
  flex-shrink: 0;
  padding: 4px 10px;
  border-radius: 999px;
  color: #24282b;
  font-size: 11px;
  font-weight: 600;
}

@media (max-width: 620px) {
  .event-card {
    grid-template-columns: 1fr;
  }

  .event-image {
    height: 180px;
  }

  .event-content {
    padding-right: 0;
  }
}
</style>
