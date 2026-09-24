<template>
  <Teleport to="body">
    <div class="event-detail-backdrop" @click.self="emit('close')">
      <section class="event-detail-dialog" role="dialog" aria-modal="true" :aria-labelledby="titleId">
        <button type="button" class="event-detail-close" aria-label="關閉活動詳情" @click="emit('close')">×</button>
        <h2 class="event-detail-heading">活動詳情</h2>

        <div class="event-detail-layout">
          <div class="event-detail-image">
            <img v-if="photoUrl && !imageFailed" :src="photoUrl" :alt="event.title" @error="imageFailed = true" />
            <span v-else>EVENT</span>
          </div>

          <div class="event-detail-content">
            <span
              class="event-type-badge"
              :style="{ backgroundColor: eventTypeInfo.color }"
            >{{ eventTypeInfo.label }}</span>
            <h3 :id="titleId">{{ event.title }}</h3>
            <span class="event-status" :class="statusInfo.className"><i></i>{{ statusInfo.label }}</span>
            <div class="event-detail-separator"></div>

            <div class="event-detail-row">
              <span class="detail-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24"><path d="M7 3v3M17 3v3M4 9h16M5 5h14a1 1 0 0 1 1 1v14H4V6a1 1 0 0 1 1-1Z" /></svg>
              </span>
              <div><strong>活動時間</strong><p>{{ eventTime }}</p></div>
            </div>

            <div v-if="locationText" class="event-detail-row">
              <span class="detail-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24"><path d="M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Z" /><circle cx="12" cy="10" r="2.5" /></svg>
              </span>
              <div><strong>活動地點</strong><p>{{ locationText }}</p></div>
            </div>

            <div class="event-detail-separator"></div>
            <div class="event-detail-row description-row">
              <span class="detail-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24"><path d="M6 3h9l4 4v14H6V3Z" /><path d="M14 3v5h5M9 12h6M9 16h6" /></svg>
              </span>
              <div><strong>活動介紹</strong><p>{{ event.description || "目前沒有更多活動介紹。" }}</p></div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { getEventType } from "@/constants/eventTypes";

const props = defineProps({ event: { type: Object, required: true } });
const emit = defineEmits(["close"]);
const imageFailed = ref(false);
const timestamp = Date.now();
const eventId = computed(() => props.event.id || props.event.eventId);
const titleId = computed(() => `event-detail-title-${eventId.value || "modal"}`);

const photoUrl = computed(() => {
  if (props.event.photoUrl) return props.event.photoUrl;
  if (props.event.imageUrl) return props.event.imageUrl;
  if (!props.event.hasPhoto || !eventId.value) return "";
  return `http://localhost:8080/api/calendar/event/${eventId.value}/photo?t=${timestamp}`;
});

const eventTypeInfo = computed(() => getEventType(props.event.eventType));

function parseDate(value) {
  if (!value) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

function formatDateTime(value) {
  const date = parseDate(value);
  if (!date) return "未提供";
  return date.toLocaleString("zh-TW", { year: "numeric", month: "long", day: "numeric", weekday: "short", hour: "2-digit", minute: "2-digit", hour12: false });
}

function formatTime(value) {
  const date = parseDate(value);
  return date ? date.toLocaleTimeString("zh-TW", { hour: "2-digit", minute: "2-digit", hour12: false }) : "";
}

function isSameDay(start, end) {
  return start && end && start.getFullYear() === end.getFullYear() && start.getMonth() === end.getMonth() && start.getDate() === end.getDate();
}

const eventTime = computed(() => {
  const start = parseDate(props.event.startAt);
  const end = parseDate(props.event.endAt);
  if (start && end && isSameDay(start, end)) return `${formatDateTime(props.event.startAt)}－${formatTime(props.event.endAt)}`;
  if (start && end) return `${formatDateTime(props.event.startAt)} 至 ${formatDateTime(props.event.endAt)}`;
  return formatDateTime(props.event.startAt || props.event.endAt);
});

const locationText = computed(() => [props.event.venueName, props.event.address].filter(Boolean).join(" · "));

const statusInfo = computed(() => {
  const status = props.event.status;
  if (status === "CANCELLED") return { label: "已取消", className: "cancelled" };
  if (status === "DRAFT") return { label: "草稿", className: "draft" };
  if (status === "INACTIVE") return { label: "已停用", className: "inactive" };
  const now = new Date();
  const start = parseDate(props.event.startAt);
  const end = parseDate(props.event.endAt);
  if (start && now < start) return { label: "即將開始", className: "upcoming" };
  if (end && now > end) return { label: "已結束", className: "ended" };
  return { label: "進行中", className: "active" };
});

function onKeydown(event) { if (event.key === "Escape") emit("close"); }
watch(eventId, () => { imageFailed.value = false; });
onMounted(() => { window.addEventListener("keydown", onKeydown); document.body.classList.add("event-detail-open"); });
onUnmounted(() => { window.removeEventListener("keydown", onKeydown); document.body.classList.remove("event-detail-open"); });
</script>

<style scoped>
:global(body.event-detail-open) { overflow: hidden; }
.event-detail-backdrop { position: fixed; inset: 0; z-index: 1100; display: grid; overflow-y: auto; padding: 28px; place-items: center; background: rgb(19 21 20 / 64%); }
.event-detail-dialog { position: relative; width: min(100%, 1040px); padding: 28px; border: 1px solid rgb(255 255 255 / 70%); border-radius: 18px; background: #fff; box-shadow: 0 28px 80px rgb(0 0 0 / 24%); }
.event-detail-close { position: absolute; top: 18px; right: 18px; z-index: 2; display: grid; width: 42px; height: 42px; padding: 0 0 3px; place-items: center; border: 0; border-radius: 50%; color: #343733; background: #f1f2ef; font-size: 28px; font-weight: 300; line-height: 1; cursor: pointer; }
.event-detail-close:hover { background: #e7e8e4; }
.event-detail-heading { margin: 0 64px 22px 0; color: #1f211f; font-size: 24px; font-weight: 750; }
.event-detail-layout { display: grid; grid-template-columns: minmax(0, 1.18fr) minmax(320px, .92fr); gap: 32px; align-items: stretch; }
.event-detail-image { display: grid; min-height: 430px; overflow: hidden; place-items: center; border-radius: 16px; color: #7c8079; background: #eeefeb; box-shadow: 0 8px 24px rgb(29 31 28 / 10%); letter-spacing: .18em; }
.event-detail-image img { width: 100%; height: 100%; object-fit: cover; }
.event-detail-content { min-width: 0; padding: 4px 0; }
.event-type-badge { display: inline-flex; padding: 6px 12px; border-radius: 999px; color: #39403d; font-size: 11px; font-weight: 700; }
.event-detail-content h3 { margin: 14px 0 12px; color: #1d201d; font-size: clamp(25px, 2.4vw, 34px); font-weight: 720; line-height: 1.25; }
.event-status { display: inline-flex; align-items: center; gap: 7px; padding: 7px 13px; border-radius: 999px; font-size: 12px; font-weight: 700; }
.event-status i { width: 7px; height: 7px; border-radius: 50%; background: currentColor; }
.event-status.active, .event-status.upcoming { color: #56825b; background: #e0eee2; }
.event-status.ended, .event-status.draft, .event-status.inactive { color: #686b66; background: #ebebe8; }
.event-status.cancelled { color: #a35e57; background: #f1deda; }
.event-detail-separator { height: 1px; margin: 20px 0; background: #e6e7e3; }
.event-detail-row { display: grid; grid-template-columns: 30px minmax(0, 1fr); gap: 12px; margin-bottom: 18px; }
.detail-icon { display: grid; width: 28px; height: 28px; place-items: center; color: #70756f; }
.detail-icon svg { width: 24px; height: 24px; fill: none; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; }
.event-detail-row strong { display: block; margin-bottom: 3px; color: #747871; font-size: 12px; font-weight: 650; }
.event-detail-row p { margin: 0; color: #30332f; font-size: 14px; line-height: 1.65; white-space: pre-wrap; }
.description-row { margin-bottom: 0; }
@media (max-width: 820px) { .event-detail-backdrop { align-items: start; padding: 16px; } .event-detail-dialog { padding: 22px; } .event-detail-layout { grid-template-columns: 1fr; gap: 22px; } .event-detail-image { min-height: 0; aspect-ratio: 16 / 9; } }
@media (max-width: 520px) { .event-detail-backdrop { padding: 0; } .event-detail-dialog { min-height: 100vh; border-radius: 0; } .event-detail-heading { font-size: 21px; } }
</style>
