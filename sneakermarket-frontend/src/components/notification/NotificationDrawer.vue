<template>
  <div
    v-if="isOpen"
    class="notification-popover"
    role="dialog"
    aria-label="通知"
  >
    <header class="popover-header">
      <h2>通知</h2>

      <div class="header-actions">
        <button
          type="button"
          class="mark-all-button"
          :disabled="unreadCount === 0 || markAllLoading"
          @click="$emit('mark-all-as-read')"
        >
          {{ markAllLoading ? "處理中…" : "標為全部已讀" }}
        </button>
        <button
          type="button"
          class="close-button"
          aria-label="關閉通知"
          @click="$emit('close')"
        >
          ×
        </button>
      </div>
    </header>

    <p v-if="error" class="error-message" role="alert">{{ error }}</p>

    <div class="notification-list">
      <p v-if="visibleNotifications.length === 0" class="empty-message">
        {{ showAll ? "目前沒有通知" : "目前沒有未讀通知" }}
      </p>

      <button
        v-for="item in visibleNotifications"
        :key="item.notificationId"
        type="button"
        class="notification-item"
        :class="{ read: item.isRead }"
        @click="handleClick(item)"
      >
        <span class="notification-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <rect x="3" y="5" width="18" height="16" rx="2" />
            <path d="M7 3v4M17 3v4M3 10h18" />
          </svg>
        </span>

        <span class="notification-text">
          <strong>
            <i v-if="!item.isRead" class="unread-dot"></i>
            {{ item.title }}
          </strong>
          <span>{{ item.content }}</span>
          <small>{{ formatTime(item.createdAt) }}</small>
        </span>
      </button>
    </div>

    <footer class="popover-footer">
      <button type="button" @click="showAll = !showAll">
        {{ showAll ? "返回未讀通知" : "查看全部通知" }}
      </button>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRouter } from "vue-router";

const props = defineProps({
  isOpen: { type: Boolean, default: false },
  notifications: { type: Array, default: () => [] },
  markAllLoading: { type: Boolean, default: false },
  error: { type: String, default: "" },
});

const emit = defineEmits(["close", "mark-as-read", "mark-all-as-read"]);
const router = useRouter();
const showAll = ref(false);

const unreadCount = computed(
  () => props.notifications.filter((item) => !item.isRead).length,
);

const visibleNotifications = computed(() =>
  showAll.value
    ? props.notifications
    : props.notifications.filter((item) => !item.isRead),
);

watch(
  () => props.isOpen,
  (open) => {
    if (open) showAll.value = false;
  },
);

function handleClick(item) {
  if (!item.isRead) {
    emit("mark-as-read", item.notificationId);
  }

  emit("close");

  if (item.targetUrl) {
    window.open(item.targetUrl, "_blank");
  } else if (item.targetType === "EVENT") {
    const route = router.resolve(`/calendar?eventId=${item.targetId}`);
    window.open(route.href, "_blank");
  }
}

function handleOutsideClick(event) {
  if (!props.isOpen) return;
  if (event.target.closest(".notification-menu")) return;
  emit("close");
}

onMounted(() => {
  document.addEventListener("pointerdown", handleOutsideClick);
});

onUnmounted(() => {
  document.removeEventListener("pointerdown", handleOutsideClick);
});

function formatTime(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";

  const minutes = Math.floor((Date.now() - date.getTime()) / 60000);
  if (minutes < 1) return "剛剛";
  if (minutes < 60) return `${minutes} 分鐘前`;
  if (minutes < 1440) return `${Math.floor(minutes / 60)} 小時前`;

  return date.toLocaleDateString("zh-TW");
}
</script>

<style scoped>
.notification-popover {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  z-index: 1050;
  width: min(420px, calc(100vw - 24px));
  overflow: hidden;
  border: 1px solid #e5e8e9;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 12px 32px rgb(0 0 0 / 14%);
}

.popover-header,
.header-actions {
  display: flex;
  align-items: center;
}

.popover-header {
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid #e5e8e9;
}

.popover-header h2 {
  margin: 0;
  font-size: 19px;
  font-weight: 800;
}

.header-actions {
  gap: 10px;
}

.mark-all-button,
.close-button,
.popover-footer button {
  border: 0;
  background: transparent;
  color: #d4863d;
  cursor: pointer;
}

.mark-all-button,
.popover-footer button {
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
}

.mark-all-button {
  font-size: 13px;
  font-weight: 700;
}

.mark-all-button:disabled {
  opacity: 0.45;
  cursor: default;
}

.close-button {
  color: #697079;
  font-size: 22px;
}

.notification-list {
  max-height: min(60vh, 420px);
  overflow-y: auto;
}

.notification-item {
  display: grid;
  width: 100%;
  grid-template-columns: 60px minmax(0, 1fr);
  gap: 12px;
  padding: 12px 16px;
  border: 0;
  border-bottom: 1px solid #e5e8e9;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.notification-item:hover {
  background: #f7f8f7;
}

.notification-item.read {
  opacity: 0.65;
}

.notification-icon {
  display: grid;
  width: 60px;
  height: 60px;
  place-items: center;
  border-radius: 9px;
  background: #eef1f0;
}

.notification-icon svg {
  width: 25px;
  height: 25px;
  fill: none;
  stroke: #4c5459;
  stroke-width: 1.8;
}

.notification-text {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
  color: #4c5459;
  font-size: 12px;
}

.notification-text strong {
  color: #202326;
  font-size: 14px;
  line-height: 1.4;
}

.notification-text small {
  color: #697079;
}

.unread-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 5px;
  border-radius: 50%;
  background: #8499ae;
}

.empty-message {
  margin: 0;
  padding: 35px 16px;
  color: #697079;
  text-align: center;
}

.error-message {
  margin: 8px 16px;
  color: #9a4038;
  font-size: 12px;
}

.popover-footer {
  padding: 12px;
  text-align: center;
}

.popover-footer button {
  padding: 7px 16px;
  border: 1px solid #d8dcde;
  border-radius: 8px;
  background: #fff;
  color: #4c5459;
  text-decoration: none;
  cursor: pointer;
}

.popover-footer button:hover {
  background: #eef1f0;
}
</style>