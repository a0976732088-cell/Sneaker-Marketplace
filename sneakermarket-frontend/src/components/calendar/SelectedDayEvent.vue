<template>
  <article class="day-event">
    <div class="event-image">
      <img
        v-if="event.photoUrl && !imageFailed"
        :src="event.photoUrl"
        :alt="event.title"
        @error="imageFailed = true"
      />
      <span v-else>EVENT</span>
    </div>

    <div class="event-info">
      <p class="event-time">{{ event.time || "全天" }}</p>
      <h4>{{ event.title }}</h4>

      <span
        class="event-type"
        :style="{ backgroundColor: event.color }"
      >
        {{ getEventType(event.eventType).label }}
      </span>

      <div class="event-actions">
        <button
          type="button"
          class="detail-button"
          @click="$emit('view', event)"
        >
          查看內容 →
        </button>
        <button
          type="button"
          class="follow-button"
          :disabled="followLoading"
          :aria-pressed="followed"
          @click="$emit('follow', event)"
        >
          {{ followLoading ? "處理中…" : followed ? "已關注" : "關注活動" }}
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
.day-event {
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr);
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #e8eaeb;
}

.day-event:last-child {
  border-bottom: 0;
}

.event-image {
  display: grid;
  min-height: 100px;
  place-items: center;
  overflow: hidden;
  border-radius: 9px;
  background: #e8e9e8;
  color: #656b6a;
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

.event-time {
  margin: 0 0 4px;
  color: #697079;
  font-size: 12px;
}

.event-info h4 {
  display: -webkit-box;
  overflow: hidden;
  margin: 0 0 7px;
  color: #202326;
  font-size: 15px;
  font-weight: 700;
  -webkit-box-orient: vertical;
  line-clamp: 2;
  -webkit-line-clamp: 2;
}

.event-type {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 999px;
  color: #24282b;
  font-size: 11px;
  font-weight: 600;
}

.event-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 10px;
}

.event-actions button {
  min-height: 30px;
  padding: 4px 10px;
  border-radius: 7px;
  font-size: 12px;
  font-weight: 600;
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
</style>