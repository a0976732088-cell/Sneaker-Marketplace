<template>
  <nav class="navbar navbar-expand-lg navbar-dark bg-dark shadow-sm">
    <div class="container">
      <RouterLink class="navbar-brand fw-bold" to="/">
        SneakerMarket
      </RouterLink>

      <button
        class="navbar-toggler"
        type="button"
        data-bs-toggle="collapse"
        data-bs-target="#mainNavbar"
        aria-controls="mainNavbar"
        aria-expanded="false"
        aria-label="切換導覽列"
      >
        <span class="navbar-toggler-icon"></span>
      </button>

      <div id="mainNavbar" class="collapse navbar-collapse">
        <ul class="navbar-nav me-auto mb-2 mb-lg-0">
          <li class="nav-item">
            <RouterLink class="nav-link" to="/"> 首頁 </RouterLink>
          </li>

          <li class="nav-item">
            <RouterLink class="nav-link" to="/products"> 一般商城 </RouterLink>
          </li>

          <li class="nav-item">
            <RouterLink class="nav-link" to="/auctions"> 競標商城 </RouterLink>
          </li>

          <li class="nav-item">
            <RouterLink class="nav-link" to="/calendar"> 行事曆 </RouterLink>
          </li>

          <!-- 🎯 點擊觸發彈窗開關 -->
          <li class="nav-item">
            <button
              type="button"
              class="nav-link btn border-0 position-relative"
              @click="toggleNotice"
            >
              通知
              <span
                v-if="unreadCount > 0"
                class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger"
              >
                {{ unreadCount }}
              </span>
            </button>
          </li>

          <li class="nav-item">
            <RouterLink class="nav-link" to="/notifications"> 通知 </RouterLink>
          </li>
        </ul>

        <ul class="navbar-nav ms-auto">
          <li class="nav-item">
            <RouterLink class="nav-link" to="/cart">購物車</RouterLink>
          </li>
          <li class="nav-item">
            <RouterLink class="nav-link" to="/member">會員中心</RouterLink>
          </li>

          <!--
            只有目前位於競標商城相關頁面時顯示。

            包含：
            /auctions
            /auctions/create
            /auctions/{id}
          -->
          <li v-if="isAuctionPage" class="nav-item">
            <RouterLink class="nav-link" to="/auctions/create">
              建立競標
            </RouterLink>
          </li>

          <li class="nav-item">
            <RouterLink class="nav-link" to="/system-check">
              系統測試
            </RouterLink>
          </li>
        </ul>
      </div>
    </div>
  </nav>

  <!-- 🎯 引入獨立的通知彈窗元件 -->
  <NotificationDrawer
    :is-open="isNoticeOpen"
    :notifications="notifications"
    @close="isNoticeOpen = false"
    @mark-as-read="handleMarkAsRead"
  />
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRoute } from "vue-router";
import axios from "axios";
// 🎯 Import 彈窗元件
import NotificationDrawer from "@/components/notification/NotificationDrawer.vue";

const isNoticeOpen = ref(false);
const notifications = ref([]);

const unreadCount = computed(() => {
  return notifications.value.filter((n) => !n.isRead).length;
});

const toggleNotice = () => {
  isNoticeOpen.value = !isNoticeOpen.value;
  if (isNoticeOpen.value) {
    fetchNotifications();
  }
};

const fetchNotifications = async () => {
  try {
    const res = await axios.get("/api/notifications/my");
    notifications.value = res.data || [];
  } catch (err) {
    console.error("取得通知失敗", err);
  }
};

const handleMarkAsRead = async (id) => {
  try {
    await axios.patch(`/api/notifications/${id}/read`);
    const target = notifications.value.find((n) => n.notificationId === id);
    if (target) target.isRead = true;
  } catch (err) {
    console.error("已讀更新失敗", err);
  }
};

onMounted(() => {
  fetchNotifications();
});

// 取得目前所在的 Router 資訊
const route = useRoute();

/**
 * 判斷目前是否位於競標商城範圍。
 *
 * 例如：
 * /auctions
 * /auctions/create
 * /auctions/15
 *
 * 都會回傳 true。
 */
const isAuctionPage = computed(() => {
  return route.path.startsWith("/auctions");
});
</script>
