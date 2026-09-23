<script setup>
import { computed } from "vue";

import { useRoute } from "vue-router";

import { FontAwesomeIcon } from "@fortawesome/vue-fontawesome";
import {
  faHouse,
  faUser,
  faUserPlus,
  faCube,
  faBullhorn,
  faCalendarDays,
  faChartLine,
  faBell,
  faEnvelope,
  faGavel,
  faPlus,
} from "@fortawesome/free-solid-svg-icons";

const route = useRoute();

// =====================================================
// MEMBER
// =====================================================

const isDashboardActive = computed(() => route.name === "admin");

const isMemberListActive = computed(() => {
  return route.name === "admin-members" && route.query.action !== "create-member";
});

const isCreateMemberActive = computed(() => {
  return route.name === "admin-members" && route.query.action === "create-member";
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

    <div class="menu-section home-section">

      <!-- 會員列表 -->

      <RouterLink
        :to="{
          name: 'admin',
        }"
        class="menu-item"
        :class="{
          active: isDashboardActive,
        }"
        active-class=""
        exact-active-class=""
      >
        <span class="menu-icon">
          <FontAwesomeIcon :icon="faHouse" />
        </span>

          <span> 後台首頁 </span>
      </RouterLink>
    </div>

    <nav class="sidebar-menu">
      <!-- =====================================
           MEMBER
      ====================================== -->

      <div class="menu-section">
        <p class="menu-section-title">MEMBER</p>

        <!-- 會員列表 -->

        <RouterLink
          :to="{
            name: 'admin-members',
          }"
          class="menu-item"
          :class="{
            active: isMemberListActive,
          }"
          active-class=""
          exact-active-class=""
        >
          <span class="menu-icon">
            <FontAwesomeIcon :icon="faUser" />
          </span>

          <span> 會員列表 </span>
        </RouterLink>

        <!-- 新增會員 -->

        <RouterLink
          :to="{
            name: 'admin-members',

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
          <span class="menu-icon">
            <FontAwesomeIcon :icon="faUserPlus" />
          </span>

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
          <span class="menu-icon">
            <FontAwesomeIcon :icon="faCube" />
          </span>

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
          <span class="menu-icon">
            <FontAwesomeIcon :icon="faPlus" />
          </span>

          <span> 新增商品 </span>
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
          <span class="menu-icon">
            <FontAwesomeIcon :icon="faGavel" />
          </span>

          <span> 競標管理 </span>
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
          <span class="menu-icon">
            <FontAwesomeIcon :icon="faBullhorn" />
          </span>

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
          <span class="menu-icon">
            <FontAwesomeIcon :icon="faChartLine" />
          </span>
          <span>活動總覽</span>
        </RouterLink>

        <RouterLink
          :to="{ name: 'admin-events' }"
          class="menu-item"
          :class="{ active: isActivityManagementActive }"
          active-class=""
          exact-active-class=""
        >
          <span class="menu-icon">
            <FontAwesomeIcon :icon="faCalendarDays" />
          </span>
          <span>活動管理</span>
        </RouterLink>

        <RouterLink
          :to="{ name: 'admin-notifications' }"
          class="menu-item"
          :class="{ active: isNotificationActive }"
          active-class=""
          exact-active-class=""
        >
          <span class="menu-icon">
            <FontAwesomeIcon :icon="faBell" />
          </span>
          <span>通知中心</span>
        </RouterLink>

        <RouterLink
          :to="{ name: 'admin-notification-logs' }"
          class="menu-item"
          :class="{ active: isNotificationLogActive }"
          active-class=""
          exact-active-class=""
        >
          <span class="menu-icon">
            <FontAwesomeIcon :icon="faEnvelope" />
          </span>
          <span>發送紀錄</span>
        </RouterLink>
      </div>
    </nav>
  </aside>
</template>

<style scoped>
.admin-sidebar {
  width: 228px;
  min-width: 228px;
  min-height: calc(100vh - 56px);
  background: #f0eee8;
  color: #202320;
  border-right: 1px solid #e3e3df;
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
  padding: 18px 14px 30px;
  overflow-y: auto;
  padding: 22px 12px 32px;
}

.menu-section {
  margin: 0;
  padding: 18px 0;
  border-top: 1px solid #deded9;
}

.home-section {
  padding-right: 14px;
  padding-left: 14px;
}

.menu-section-title {
  margin: 0 10px 10px;
  color: #72766f;
  font-size: 12px;
  font-weight: 650;
  letter-spacing: 0;
}

.menu-item {
  position: relative;
  display: flex;
  min-height: 48px;
  align-items: center;
  gap: 14px;
  margin: 2px 0;
  padding: 0 14px;
  border-radius: 6px;
  color: #252925;
  font-size: 14px;
  font-weight: 650;
  text-decoration: none;
  transition:
    color 160ms ease,
    background 160ms ease,
    transform 160ms ease;
}

.menu-item:hover {
  color: #111411;
  background: #ebebe7;
}

.menu-item.active {
  color: #ffffff;
  background: linear-gradient(135deg, #313331, #191a19);
  box-shadow: 0 6px 16px rgb(22 24 22 / 14%);
}

.menu-item.active::before {
  position: absolute;
  top: 5px;
  bottom: 5px;
  left: 0;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: #81a889;
  content: "";
}

.menu-item.active .menu-icon {
  color: #ffffff;
}

.menu-icon {
  display: inline-grid;
  width: 22px;
  height: 22px;
  flex: 0 0 22px;
  place-items: center;
  color: currentColor;
  font-size: 19px;
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