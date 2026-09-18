<template>
  <Teleport to="body">
    <div
      class="detail-backdrop"
      role="presentation"
      @click.self="$emit('close')"
    >
      <section
        class="detail-dialog"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="`event-title-${event.id}`"
      >
        <button
          type="button"
          class="close-button"
          aria-label="關閉活動詳情"
          @click="$emit('close')"
        >
          ×
        </button>

        <div class="detail-image">
          <img
            v-if="photoUrl && !imageFailed"
            :src="photoUrl"
            :alt="event.title"
            @error="imageFailed = true"
          />
          <span v-else>EVENT</span>
        </div>

        <div class="detail-content">
          <span
            class="type-badge"
            :style="{ backgroundColor: event.color }"
          >
            {{ getEventType(event.eventType).label }}
          </span>

          <h2 :id="`event-title-${event.id}`">{{ event.title }}</h2>

          <dl>
            <div>
              <dt>開始時間</dt>
              <dd>{{ formatDateTime(event.startAt) }}</dd>
            </div>
            <div>
              <dt>結束時間</dt>
              <dd>{{ formatDateTime(event.endAt) }}</dd>
            </div>
          </dl>

          <p class="description">
            {{ event.description || "目前沒有更多活動介紹。" }}
          </p>

          <a
            v-if="event.targetUrl"
            class="event-link"
            :href="event.targetUrl"
            target="_blank"
            rel="noopener noreferrer"
          >
            前往活動頁面 →
          </a>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { getEventType } from "@/constants/eventTypes";

const props = defineProps({
  event: { type: Object, required: true },
});

const emit = defineEmits(["close"]);
const imageFailed = ref(false);

const photoUrl = computed(() => props.event.photoUrl || "");

function formatDateTime(value) {
  if (!value) return "未提供";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "未提供";

  return date.toLocaleString("zh-TW", {
    year: "numeric",
    month: "long",
    day: "numeric",
    weekday: "short",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
}

function onKeydown(event) {
  if (event.key === "Escape") emit("close");
}

onMounted(() => window.addEventListener("keydown", onKeydown));
onUnmounted(() => window.removeEventListener("keydown", onKeydown));
</script>

<style scoped>
.detail-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1100;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgb(15 18 20 / 62%);
}

.detail-dialog {
  position: relative;
  width: min(100%, 720px);
  max-height: min(90vh, 850px);
  overflow-y: auto;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 20px 70px rgb(0 0 0 / 24%);
}

.close-button {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 1;
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 50%;
  background: rgb(255 255 255 / 94%);
  color: #202326;
  font-size: 26px;
  cursor: pointer;
}

.detail-image {
  display: grid;
  height: 290px;
  place-items: center;
  overflow: hidden;
  background: #e8e9e8;
  color: #656b6a;
  letter-spacing: 0.2em;
}

.detail-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-content {
  padding: 28px 32px 32px;
}

.type-badge {
  display: inline-block;
  padding: 5px 12px;
  border-radius: 999px;
  color: #24282b;
  font-size: 12px;
  font-weight: 700;
}

h2 {
  margin: 14px 0 22px;
  color: #202326;
  font-size: 28px;
}

dl {
  display: grid;
  gap: 10px;
  margin: 0 0 22px;
}

dl div {
  display: flex;
  gap: 18px;
}

dt {
  min-width: 64px;
  color: #697079;
  font-weight: 500;
}

dd {
  margin: 0;
  color: #202326;
}

.description {
  color: #4c5459;
  line-height: 1.8;
  white-space: pre-wrap;
}

.event-link {
  display: inline-block;
  margin-top: 12px;
  padding: 10px 18px;
  border-radius: 8px;
  background: #e7664e;
  color: #fff;
  font-weight: 700;
  text-decoration: none;
}

@media (max-width: 620px) {
  .detail-image {
    height: 210px;
  }

  .detail-content {
    padding: 22px;
  }

  h2 {
    font-size: 22px;
  }
}
</style>