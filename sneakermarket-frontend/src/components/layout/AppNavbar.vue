<template>
  <nav
    class="navbar navbar-expand-lg navbar-light bg-white shadow-sm sticky-top"
  >
    <div class="container">
      <RouterLink class="navbar-brand brand" to="/">
        <img src="/images/brand/newLogo_circle.png" alt="" />
        <span>酷客 Kicks</span>
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
        <ul class="navbar-nav me-auto mb-2 mb-lg-0 main-links">
          <li class="nav-item">
            <RouterLink class="nav-link" to="/ads/storefront">首頁</RouterLink>
          </li>
          <li class="nav-item">
            <RouterLink class="nav-link" to="/products">一般商城</RouterLink>
          </li>
          <li class="nav-item">
            <RouterLink class="nav-link" to="/auctions">競標商城</RouterLink>
          </li>
          <li class="nav-item">
            <RouterLink class="nav-link" to="/calendar">活動</RouterLink>
          </li>
        </ul>

        <ul class="navbar-nav ms-auto align-items-lg-center right-links">
          <li v-if="currentMember" class="nav-item">
            <RouterLink
              class="nav-link auction-create-link"
              to="/auctions/create"
            >
              <svg
                class="auction-create-icon"
                viewBox="0 0 24 24"
                aria-hidden="true"
              >
                <path d="m14 13-7.5 7.5c-.83.83-2.17.83-3 0s-.83-2.17 0-3L11 10" />
                <path d="m16 16 6-6" />
                <path d="m8 8 6-6" />
                <path d="m9 7 8 8" />
                <path d="m21 11-8-8" />
              </svg>
              <span>建立競標</span>
            </RouterLink>
          </li>

          <li class="nav-item" ref="cartLinkWrapper">
            <RouterLink class="nav-link cart-link" to="/cart" aria-label="購物車">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M6 6h15l-1.5 9h-13z" />
                <path d="M6 6 5 3H2" />
                <circle cx="9" cy="20" r="1.5" />
                <circle cx="18" cy="20" r="1.5" />
              </svg>
              <span v-if="cartCount > 0" class="cart-count">{{ cartCount }}</span>
            </RouterLink>
          </li>

          <li class="nav-item notification-menu">
            <button
              type="button"
              class="nav-link notification-button"
              aria-label="通知"
              @click="toggleNotice"
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path
                  d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9ZM10 21h4"
                />
              </svg>
              <span v-if="unreadCount > 0" class="notification-count">
                {{ unreadCount }}
              </span>
            </button>

            <NotificationDrawer
              :is-open="isNoticeOpen"
              :notifications="notifications"
              :mark-all-loading="markAllLoading"
              :error="notificationError"
              @close="isNoticeOpen = false"
              @mark-as-read="handleMarkAsRead"
              @mark-all-as-read="handleMarkAllAsRead"
            />
          </li>

          <li v-if="!currentMember" class="nav-item">
            <RouterLink class="nav-link" to="/member">登入／註冊</RouterLink>
          </li>

          <li v-else class="nav-item dropdown">
            <button
              type="button"
              class="account-trigger dropdown-toggle"
              data-bs-toggle="dropdown"
              aria-expanded="false"
            >
              <span class="account-avatar" aria-hidden="true">
                <svg viewBox="0 0 24 24">
                  <circle cx="12" cy="8" r="3.5" />
                  <path d="M5 20c0-4 3-6 7-6s7 2 7 6" />
                </svg>
              </span>
              <span>您好，{{ currentMember.account }}</span>
            </button>

            <ul class="dropdown-menu dropdown-menu-end account-menu">
              <li>
                <RouterLink class="dropdown-item" to="/member">
                  會員中心
                </RouterLink>
              </li>

              <li>
                <RouterLink
                  class="dropdown-item"
                  to="/notifications/preferences"
                >
                  通知偏好
                </RouterLink>
              </li>

              <li v-if="isAdmin">
                <RouterLink class="dropdown-item" to="/admin">
                  管理後台
                </RouterLink>
              </li>
              <li><hr class="dropdown-divider" /></li>
              <li>
                <button
                  type="button"
                  class="dropdown-item"
                  @click="handleLogout"
                >
                  登出
                </button>
              </li>
            </ul>
          </li>
        </ul>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { ref, computed, onMounted, watch, onUnmounted , nextTick} from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  getMemberNotifications,
  markNotificationAsRead,
  markAllNotificationsAsRead,
} from "@/service/notificationService";
import NotificationDrawer from "@/components/notification/NotificationDrawer.vue";
import { cartCount, refreshCartCount, setCartIconPosition  } from "@/service/cartState";
/* ================================
   Session / 會員資料
================================ */

import { getCurrentMember, logout } from "@/service/authService";
import { createAuctionPrivateSocketClient } from "@/service/auctionPrivateSocketService";

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

  } catch (error) {
    // 沒登入時
    currentMember.value = null;

  }
}

/* =====================================================
   登出
===================================================== */

async function handleLogout() {
  try {
    // 呼叫 authService.js 的 logout service
    await logout();

    // Session 已失效，立即停止會員私人競標通知連線
    await disconnectAuctionPrivateSocket();

    // 清掉 Navbar 的會員狀態
    currentMember.value = null;

    // 登出後回首頁
    await router.push("/");
  } catch (error) {
    console.error("會員登出失敗：");
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

const isNoticeOpen = ref(false);

const cartLinkWrapper = ref(null);

function updateCartIconPosition() {
  if (!cartLinkWrapper.value) return;
  const rect = cartLinkWrapper.value.getBoundingClientRect();
  setCartIconPosition(rect.left, rect.bottom);
}

const notifications = ref([]);
const markAllLoading = ref(false);
const notificationError = ref("");

const unreadCount = computed(() => {
  return notifications.value.filter((n) => !n.isRead).length;
});

// 未登入時連通知抽屜都不能打開
const toggleNotice = () => {
  if (!currentMember.value) {
    alert("請先登入會員");
    return;
  }

  isNoticeOpen.value = !isNoticeOpen.value;

  if (isNoticeOpen.value) {
    fetchNotifications();
  }
};

const fetchNotifications = async () => {
  if (!currentMember.value) {
    notifications.value = [];
    return;
  }

  try {
    const res = await getMemberNotifications();
    // 加上 Array.isArray 檢查，防止被回傳的 404 HTML 弄壞陣列
    if (Array.isArray(res.data)) {
      notifications.value = res.data;
    } else {
      notifications.value = [];
    }
  } catch (err) {
    notifications.value = [];

    // Session 過期後停止輪詢，避免每分鐘重複發出 401。
    if (err.response?.status === 401) {
      currentMember.value = null;
      return;
    }

    console.error("取得通知失敗");
  }
};

let auctionPrivateSocket = null;

function ensureAuctionPrivateSocketConnected() {
  if (!auctionPrivateSocket) {
    auctionPrivateSocket = createAuctionPrivateSocketClient({
      // 初次連線或自動重連成功時，用 REST 補齊離線期間可能漏掉的通知
      onConnect: fetchNotifications,
      // WebSocket 只表示通知資料有更新；正式資料仍由既有通知 API 取得
      onMessage: fetchNotifications,
      onError: (error) => {
        console.error("競標私人通知連線失敗");
      },
    });
  }

  auctionPrivateSocket.connect();
}

async function disconnectAuctionPrivateSocket() {
  const socket = auctionPrivateSocket;
  auctionPrivateSocket = null;

  if (!socket) {
    return;
  }

  try {
    await socket.disconnect();
  } catch (error) {
    console.error("停止競標私人通知連線失敗");
  }
}

watch(currentMember, (member) => {
  if (member) {
    fetchNotifications();
    ensureAuctionPrivateSocketConnected();
    refreshCartCount();
  } else {
    void disconnectAuctionPrivateSocket();
    notifications.value = [];
    isNoticeOpen.value = false;
    cartCount.value = 0;
  }
  nextTick(() => updateCartIconPosition()); 
});

const handleMarkAsRead = async (id) => {
  try {
    // 呼叫後端更新資料庫 is_read = 1 與 read_at
    await markNotificationAsRead(id);

    // 更新前端狀態（紅點數量會自動減少）
    const target = notifications.value.find((n) => n.notificationId === id);

    if (target) {
      target.isRead = true;
    }
  } catch (err) {
    console.error("更新已讀狀態失敗");
  }
};

const handleMarkAllAsRead = async () => {
  if (markAllLoading.value || unreadCount.value === 0) return;

  markAllLoading.value = true;
  notificationError.value = "";

  try {
    await markAllNotificationsAsRead();

    notifications.value = notifications.value.map((item) => ({
      ...item,
      isRead: true,
    }));
  } catch (error) {
    notificationError.value = "標為全部已讀失敗，請再試一次。";
    console.error("標為全部已讀失敗", error);
  } finally {
    markAllLoading.value = false;
  }
};

let timer = null;
onMounted(() => {
  // Navbar 登入會員由 watch
  // immediate: true 負責讀取

  // 每 60 秒向後端拉取最新通知，作為私人 WebSocket 的低頻 fallback
  timer = setInterval(() => {
    fetchNotifications();
  }, 60000);

  // 當使用者切回這個頁面時，立即重新整理通知
  window.addEventListener("focus", fetchNotifications);

  updateCartIconPosition();                              
  window.addEventListener("resize", updateCartIconPosition); 
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
  window.removeEventListener("focus", fetchNotifications);
  window.removeEventListener("resize", updateCartIconPosition); 
  void disconnectAuctionPrivateSocket();
});
</script>

<style scoped>
.navbar {
  border-bottom: 1px solid #e8eaeb;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  color: #202326;
  font-size: 20px;
  font-weight: 800;
}

.brand img {
  width: 45px;
  height: 45px;
  border-radius: 50%;
  object-fit: cover;
}

.main-links {
  gap: 12px;
  margin-left: 28px;
}

.main-links .nav-link,
.right-links .nav-link {
  color: #4c5459;
  font-size: 14px;
  font-weight: 600;
}

.main-links .nav-link {
  border-bottom: 2px solid transparent;
}

.main-links .nav-link.router-link-active {
  border-bottom-color: #e7664e;
  color: #202326;
}

.auction-create-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.auction-create-icon {
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.notification-button {
  position: relative;
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border: 0;
  background: transparent;
}

.notification-button svg {
  width: 22px;
  height: 22px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
}

.notification-count {
  position: absolute;
  top: 1px;
  right: 0;
  display: grid;
  min-width: 17px;
  height: 17px;
  place-items: center;
  padding: 0 3px;
  border-radius: 999px;
  background: #e7664e;
  color: #fff;
  font-size: 10px;
}

.cart-link {
  position: relative;
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
}

.cart-link svg {
  width: 22px;
  height: 22px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
}

.cart-count {
  position: absolute;
  top: 1px;
  right: 0;
  display: grid;
  min-width: 17px;
  height: 17px;
  place-items: center;
  padding: 0 3px;
  border-radius: 999px;
  background: #e7664e;
  color: #fff;
  font-size: 10px;
}

.account-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 7px;
  border: 0;
  background: transparent;
  color: #202326;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.account-avatar {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border: 1px solid #d9dddf;
  border-radius: 50%;
  background: #f1f3f5;
  color: #495057;
  border: 1px solid #dee2e6;
}

.account-avatar svg {
  width: 21px;
  height: 21px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-width: 1.8;
}

.account-menu {
  min-width: 150px;
  padding: 8px;
  border: 1px solid #e8eaeb;
  border-radius: 10px;
  box-shadow: 0 10px 28px rgb(0 0 0 / 10%);
}

.account-menu .dropdown-item {
  padding: 8px 10px;
  border-radius: 6px;
  font-size: 13px;
}

.account-menu .dropdown-item:hover {
  background: #eef1f0;
}

.notification-menu {
  position: relative;
}

@media (max-width: 991px) {
  .main-links {
    gap: 0;
    margin-left: 0;
  }

  .main-links .nav-link.router-link-active {
    border-bottom-color: transparent;
    color: #e7664e;
  }
}
</style>
