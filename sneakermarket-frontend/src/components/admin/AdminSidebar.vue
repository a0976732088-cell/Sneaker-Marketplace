<script setup>
import { computed } from "vue";

import { useRoute } from "vue-router";

const route = useRoute();

// =====================================================
// MEMBER
// =====================================================

const isMemberListActive = computed(() => {
  return route.name === "admin" && route.query.action !== "create-member";
});

const isCreateMemberActive = computed(() => {
  return route.name === "admin" && route.query.action === "create-member";
});

// =====================================================
// COMMERCE
// =====================================================

const isProductListActive = computed(() => {
  return route.name === "admin-products" || route.name === "admin-product-edit";
});

const isProductCreateActive = computed(() => {
  return route.name === "admin-product-create";
});

// =====================================================
// ADVERTISEMENT
// =====================================================

const isAdvertisementActive = computed(() => {
  return route.name === "admin-advertisements";
});

// =====================================================
// CALENDAR
// =====================================================

const isActivityOverviewActive = computed(() => {
  return route.name === "admin-activity-overview";
});

const isCalendarActive = computed(() => {
  return route.name === "admin-events";
});

const isActivityManagementActive = computed(() => {
  return route.name === "admin-events";
});

// =====================================================
// NOTIFICATION
// =====================================================

const isNotificationActive = computed(() => {
  return route.name === "admin-notifications";
});

const isNotificationLogActive = computed(() => {
  return route.name === "admin-notification-logs";
});

// =====================================================
// AUCTION
// =====================================================

const isAuctionSection = computed(() => {
  return ["admin-auctions", "admin-auction-detail"].includes(route.name);
});
</script>

<template>
  <aside class="admin-sidebar">
    <div class="sidebar-heading">
      <small>SNEAKERMARKET</small>
      <strong>管理後台</strong>
    </div>
    <!-- =========================================
         MENU
    ========================================== -->

    <nav class="sidebar-menu" aria-label="管理功能">
      <!-- =====================================
           MEMBER
      ====================================== -->

      <div class="menu-section">
        <p class="menu-section-title">MEMBER</p>

        <!-- 會員列表 -->

        <RouterLink
          :to="{
            name: 'admin',
          }"
          class="menu-item"
          :class="{
            active: isMemberListActive,
          }"
          active-class=""
          exact-active-class=""
        >
          <span class="menu-icon"> ○ </span>

          <span> 會員列表 </span>
        </RouterLink>

        <!-- 新增會員 -->

        <RouterLink
          :to="{
            name: 'admin',

            query: {
              action: 'create-member',
            },
          }"
          class="menu-item"
          :class="{
            active: isCreateMemberActive,
          }"
          active-class=""
          exact-active-class=""
        >
          <span class="menu-icon"> ＋ </span>

          <span> 新增會員 </span>
        </RouterLink>
      </div>

      <!-- =====================================
           COMMERCE
      ====================================== -->

      <div class="menu-section">
        <p class="menu-section-title">COMMERCE</p>

        <!-- 商品管理 -->

        <RouterLink
          :to="{
            name: 'admin-products',
          }"
          class="menu-item"
          :class="{
            active: isProductListActive,
          }"
          active-class=""
          exact-active-class=""
        >
          <span class="menu-icon"> □ </span>

          <span> 商品管理 </span>
        </RouterLink>

        <!-- 新增商品 -->

        <RouterLink
          :to="{
            name: 'admin-product-create',
          }"
          class="menu-item"
          :class="{
            active: isProductCreateActive,
          }"
          active-class=""
          exact-active-class=""
        >
          <span class="menu-icon"> ＋ </span>

          <span> 新增商品 </span>
        </RouterLink>
      </div>

      <!-- =====================================
           ADVERTISEMENT
      ====================================== -->

      <div class="menu-section">
        <p class="menu-section-title">ADVERTISEMENT</p>

        <RouterLink
          :to="{
            name: 'admin-advertisements',
          }"
          class="menu-item"
          :class="{
            active: isAdvertisementActive,
          }"
          active-class=""
          exact-active-class=""
        >
          <span class="menu-icon"> ◇ </span>

          <span> 廣告管理 </span>
        </RouterLink>
      </div>

      <!-- =====================================
           CALENDAR / NOTIFICATION
      ====================================== -->

      <div class="menu-section">
        <p class="menu-section-title">CALENDAR</p>

        <RouterLink
          :to="{ name: 'admin-activity-overview' }"
          class="menu-item"
          :class="{ active: isActivityOverviewActive }"
          active-class=""
          exact-active-class=""
        >
          <span class="menu-icon"> ◫ </span>
          <span>活動總覽</span>
        </RouterLink>

        <RouterLink
          :to="{ name: 'admin-events' }"
          class="menu-item"
          :class="{ active: isActivityManagementActive }"
          active-class=""
          exact-active-class=""
        >
          <span class="menu-icon"> ▣ </span>
          <span>活動管理</span>
        </RouterLink>

        <RouterLink
          :to="{ name: 'admin-notifications' }"
          class="menu-item"
          :class="{ active: isNotificationActive }"
          active-class=""
          exact-active-class=""
        >
          <span class="menu-icon"> ◇ </span>
          <span>通知中心</span>
        </RouterLink>

        <RouterLink
          :to="{ name: 'admin-notification-logs' }"
          class="menu-item"
          :class="{ active: isNotificationLogActive }"
          active-class=""
          exact-active-class=""
        >
          <span class="menu-icon"> ✉ </span>
          <span>發送紀錄</span>
        </RouterLink>
      </div>

      <!-- =====================================
           AUCTION
      ====================================== -->

      <div class="menu-section">
        <p class="menu-section-title">AUCTION</p>

        <RouterLink
          :to="{
            name: 'admin-auctions',
          }"
          class="menu-item"
          :class="{
            active: isAuctionSection,
          }"
          active-class=""
          exact-active-class=""
        >
          <span class="menu-icon"> △ </span>

          <span> 競標管理 </span>
        </RouterLink>
      </div>
    </nav>
  </aside>
</template>

<style scoped>
.admin-sidebar {
  position: sticky;
  top: 56px;
  display: flex;
  flex: 0 0 224px;
  flex-direction: column;
  width: 224px;
  min-width: 224px;
  height: calc(100vh - 56px);
  color: #fff;
  background: #111;
}

.sidebar-heading {
  padding: 26px 22px 20px;
  border-bottom: 1px solid #303030;
}

.sidebar-heading small,
.sidebar-heading strong {
  display: block;
}

.sidebar-heading small {
  margin-bottom: 5px;
  color: #a4a4a4;
  font-size: 0.66rem;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.sidebar-heading strong {
  font-size: 1.18rem;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.sidebar-menu {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 22px 12px 32px;
}

.menu-section {
  margin-bottom: 22px;
}

.menu-section-title {
  margin: 0 12px 9px;
  color: #929292;
  font-size: 0.65rem;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 11px;
  min-height: 42px;
  margin: 2px 0;
  padding: 10px 12px;
  border-radius: 8px;
  color: #c8c8c8;
  font-size: 0.875rem;
  font-weight: 600;
  text-decoration: none;
  transition: background-color 150ms ease, color 150ms ease;
}

.menu-item:hover,
.menu-item:focus-visible {
  color: #fff;
  background: #272727;
}

.menu-item.active {
  color: #111;
  background: #fff;
}

.menu-item.active .menu-icon {
  color: #111;
}

.menu-icon {
  flex: 0 0 20px;
  width: 20px;
  color: #aaa;
  font-size: 0.9rem;
  text-align: center;
}

.sidebar-menu::-webkit-scrollbar {
  width: 4px;
  height: 4px;
}

.sidebar-menu::-webkit-scrollbar-thumb {
  border-radius: 4px;
  background: #555;
}

@media (max-width: 992px) {
  .admin-sidebar {
    position: static;
    flex: none;
    width: 100%;
    min-width: 0;
    height: auto;
  }

  .sidebar-heading {
    padding: 16px 24px 12px;
  }

  .sidebar-heading small {
    display: none;
  }

  .sidebar-heading strong {
    font-size: 1rem;
  }

  .sidebar-menu {
    display: flex;
    gap: 18px;
    overflow-x: auto;
    overflow-y: hidden;
    padding: 0 20px 12px;
  }

  .menu-section {
    display: flex;
    flex: 0 0 auto;
    gap: 4px;
    margin: 0;
  }

  .menu-section-title {
    display: none;
  }

  .menu-item {
    flex: 0 0 auto;
    min-height: 38px;
    margin: 0;
    padding: 8px 10px;
    white-space: nowrap;
  }
}
</style>