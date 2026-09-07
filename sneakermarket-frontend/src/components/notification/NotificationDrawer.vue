<template>
  <div
    class="offcanvas offcanvas-end"
    :class="{ show: isOpen }"
    tabindex="-1"
    :style="{ visibility: isOpen ? 'visible' : 'hidden' }"
  >
    <div class="offcanvas-header border-bottom">
      <h5 class="offcanvas-title fw-bold">🔔 通知中心</h5>
      <button type="button" class="btn-close" @click="close"></button>
    </div>

    <div class="offcanvas-body p-0">
      <div v-if="notifications.length === 0" class="text-center text-muted py-5">
        目前沒有新通知
      </div>

      <div v-else class="list-group list-group-flush">
        <div
          v-for="item in notifications"
          :key="item.notificationId"
          class="list-group-item list-group-item-action p-3 cursor-pointer"
          :class="{ 'bg-light': !item.isRead }"
          @click="handleClick(item)"
        >
          <div class="d-flex w-100 justify-content-between mb-1">
            <strong class="text-dark">{{ item.title }}</strong>
            <small class="text-muted">{{ formatTime(item.createdAt) }}</small>
          </div>
          <p class="mb-1 text-secondary small">{{ item.content }}</p>
        </div>
      </div>
    </div>
  </div>

  <!-- 背景遮罩 -->
  <div v-if="isOpen" class="offcanvas-backdrop fade show" @click="close"></div>
</template>

<script setup>
import { useRouter } from "vue-router";

defineProps({
  isOpen: Boolean,
  notifications: {
    type: Array,
    default: () => [],
  },
});

const emit = defineEmits(["close", "mark-as-read"]);
const router = useRouter();

const close = () => emit("close");

const handleClick = (item) => {
  emit("mark-as-read", item.notificationId);
  close();

  if (item.targetUrl) {
    window.location.href = item.targetUrl;
  } else if (item.targetType === "EVENT") {
    router.push(`/calendar?eventId=${item.targetId}`);
  }
};

const formatTime = (timeStr) => {
  if (!timeStr) return "";
  return new Date(timeStr).toLocaleTimeString("zh-TW", {
    hour: "2-digit",
    minute: "2-digit",
  });
};
</script>