<template>
  <nav class="navbar navbar-expand-lg navbar-dark bg-dark shadow-sm">
    <div class="container">
      <!-- Logo -->
      <RouterLink class="navbar-brand fw-bold" to="/">
        SneakerMarket
      </RouterLink>

      <!-- 手機版 Navbar 按鈕 -->
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
        <!-- ================================
             左側 Navbar
        ================================= -->
        <ul class="navbar-nav me-auto mb-2 mb-lg-0">
          <!-- 首頁 -->
          <li class="nav-item">
            <RouterLink class="nav-link" to="/"> 首頁 </RouterLink>
          </li>

          <!-- 一般商城 -->
          <li class="nav-item">
            <RouterLink class="nav-link" to="/products"> 一般商城 </RouterLink>
          </li>

          <!-- 競標商城 -->
          <li class="nav-item">
            <RouterLink class="nav-link" to="/auctions"> 競標商城 </RouterLink>
          </li>

          <!-- 行事曆 -->
          <li class="nav-item">
            <RouterLink class="nav-link" to="/calendar"> 行事曆 </RouterLink>
          </li>

          <!-- 通知彈窗 -->
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
        </ul>

        <!-- ================================
             右側 Navbar
        ================================= -->
        <ul class="navbar-nav ms-auto">
          <!-- 購物車 -->
          <li class="nav-item">
            <RouterLink class="nav-link" to="/cart"> 購物車 </RouterLink>
          </li>

          <!-- =================================
               尚未登入
          ================================== -->
          <template v-if="!currentMember">
            <li class="nav-item">
              <RouterLink class="nav-link" to="/member"> 登入/註冊 </RouterLink>
            </li>
          </template>

          <!-- =================================
               已登入
          ================================== -->
          <template v-else>
            <!-- 顯示會員帳號 -->
            <li class="nav-item">
              <span class="nav-link"> 您好，{{ currentMember.account }} </span>
            </li>

            <!-- 會員中心 -->
            <li class="nav-item">
              <RouterLink class="nav-link" to="/member"> 會員中心 </RouterLink>
            </li>

            <!-- =================================
                 管理後台
                 只有 ROLE_ADMIN 才顯示
            ================================== -->
            <li v-if="isAdmin" class="nav-item">
              <RouterLink class="nav-link" to="/admin"> 管理後台 </RouterLink>
            </li>

            <!-- 登出 -->
            <li class="nav-item">
              <button
                type="button"
                class="nav-link btn border-0"
                @click="handleLogout"
              >
                登出
              </button>
            </li>
          </template>

          <!--
            只有目前位於競標商城相關頁面
            才顯示建立競標
          -->
          <li v-if="isAuctionPage" class="nav-item">
            <RouterLink class="nav-link" to="/auctions/create">
              建立競標
            </RouterLink>
          </li>

          <!-- 系統測試 -->
          <li class="nav-item">
            <RouterLink class="nav-link" to="/system-check">
              系統測試
            </RouterLink>
          </li>
        </ul>
      </div>
    </div>
  </nav>

  <NotificationDrawer
    :is-open="isNoticeOpen"
    :notifications="notifications"
    @close="isNoticeOpen = false"
    @mark-as-read="handleMarkAsRead"
  />
</template>

<script setup>
import { ref, computed, onMounted, watch, onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import httpClient from "@/service/httpClient";// 🎯 Import 彈窗元件
import NotificationDrawer from "@/components/notification/NotificationDrawer.vue";

/* ================================
   Session / 會員資料
================================ */

import { getCurrentMember, logout } from "@/service/authService";

/* =====================================================
   Router
===================================================== */

const route = useRoute();
const router = useRouter();

/* =====================================================
   目前登入會員
===================================================== */

const currentMember = ref(null);

/* =====================================================
   判斷是否為 Admin
===================================================== */

const isAdmin = computed(() => {
  return currentMember.value?.role === "ROLE_ADMIN";
});

/* =====================================================
   取得目前 Session 登入會員
===================================================== */

async function loadCurrentMember() {
  try {
    const member = await getCurrentMember();

    currentMember.value = member;

    console.log("Navbar 目前登入會員：", currentMember.value);

    console.log("Navbar 目前角色：", currentMember.value?.role);

    console.log("是否為 Admin：", isAdmin.value);
  } catch (error) {
    // 沒登入時
    currentMember.value = null;

    console.log("Navbar：目前沒有登入會員");
  }
}

/* =====================================================
   登出
===================================================== */

async function handleLogout() {
  try {
    // 呼叫 authService.js 的 logout service
    await logout();

    // 清掉 Navbar 的會員狀態
    currentMember.value = null;

    console.log("會員登出成功");

    // 登出後回首頁
    await router.push("/");
  } catch (error) {
    console.error("會員登出失敗：", error);
  }
}

watch(
  () => route.fullPath,

  async () => {
    await loadCurrentMember();
  },

  {
    // 第一次開啟 Navbar
    // 也立即執行
    immediate: true,
  },
);

const isAuctionPage = computed(() => {
  return route.path.startsWith("/auctions");
});

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
    const res = await httpClient.get("/notification/noticeList");
    // 加上 Array.isArray 檢查，防止被回傳的 404 HTML 弄壞陣列
    if (Array.isArray(res.data)) {
      notifications.value = res.data;
    } else {
      notifications.value = [];
    }
  } catch (err) {
    console.error("取得通知失敗", err);
    notifications.value = [];
  }
};

const handleMarkAsRead = async (id) => {
  try {
    // 呼叫後端更新資料庫 is_read = 1 與 read_at
    await httpClient.put(`/notification/read/${id}`);
    
    // 更新前端狀態（紅點數量會自動減少）
    const target = notifications.value.find((n) => n.notificationId === id);

    if (target) {
      target.isRead = true;
    }
  } catch (err) {
    console.error("更新已讀狀態失敗", err);
  }
};

let timer = null;
onMounted(() => {
  // Navbar 登入會員由 watch
  // immediate: true 負責讀取

  // 這裡保留原本通知初始化
  fetchNotifications();
  // 🎯 每 5 秒自動向後端拉取最新通知
  timer = setInterval(() => {
    fetchNotifications();
  }, 15000);

  // 當使用者切回這個頁面時，立即重新整理通知
  window.addEventListener("focus", fetchNotifications);
});

onUnmounted(() => {
  // 元件銷毀時清除定時器
  if (timer) clearInterval(timer);
  window.removeEventListener("focus", fetchNotifications);
});
</script>
