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
      <div
        v-if="notifications.length === 0"
        class="text-center text-muted py-5"
      >
        目前沒有新通知
      </div>

      <div v-else class="list-group list-group-flush">
        <div
          v-for="item in notifications"
          :key="item.notificationId"
          class="list-group-item list-group-item-action p-3 cursor-pointer position-relative"
          :class="{ 'is-unread': !item.isRead, 'is-read': item.isRead }"
          @click="handleClick(item)"
        >
          <!-- 未讀時顯示小藍點 -->
          <span v-if="!item.isRead" class="unread-dot"></span>

          <div class="d-flex w-100 justify-content-between mb-1 ps-2">
            <strong
              :class="
                item.isRead ? 'text-secondary fw-normal' : 'text-dark fw-bold'
              "
            >
              {{ item.title }}
            </strong>
            <small class="text-muted">{{ formatTime(item.createdAt) }}</small>
          </div>
          <p class="mb-1 text-secondary small ps-2">{{ item.content }}</p>
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
    // 🎯 改為開新分頁前往外部或指定網址
    window.open(item.targetUrl, "_blank");
  } else if (item.targetType === "EVENT") {
    // 🎯 若為站內路由，取得完整 URL 後開新分頁
    const routeData = router.resolve(`/calendar?eventId=${item.targetId}`);
    window.open(routeData.href, "_blank");
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

<style scoped>
.cursor-pointer {
  cursor: pointer;
}

/* 🎯 未讀卡片：淡藍背景 */
.is-unread {
  background-color: #f0f7ff !important;
  transition: background-color 0.2s ease;
}

/* 🎯 已讀卡片：白色背景 + 透明度降低 */
.is-read {
  background-color: #ffffff !important;
  opacity: 0.7;
}

/* 🎯 未讀小藍點 */
.unread-dot {
  position: absolute;
  top: 18px;
  left: 8px;
  width: 8px;
  height: 8px;
  background-color: #0d6efd; /* Bootstrap primary 藍色 */
  border-radius: 50%;
}
</style>