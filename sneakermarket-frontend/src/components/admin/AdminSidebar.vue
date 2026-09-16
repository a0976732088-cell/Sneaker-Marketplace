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

const isCalendarActive = computed(() => {
  return route.name === "admin-events";
});

// =====================================================
// NOTIFICATION
// =====================================================

const isNotificationActive = computed(() => {
  return route.name === "admin-notifications";
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
    <!-- =========================================
         MENU
    ========================================== -->

    <nav class="sidebar-menu">
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

        <!-- 行事曆管理 -->

        <RouterLink
          :to="{
            name: 'admin-events',
          }"
          class="menu-item"
          :class="{
            active: isCalendarActive,
          }"
          active-class=""
          exact-active-class=""
        >
          <span class="menu-icon"> ▣ </span>

          <span> 行事曆管理 </span>
        </RouterLink>

        <!-- 通知管理 -->

        <RouterLink
          :to="{
            name: 'admin-notifications',
          }"
          class="menu-item"
          :class="{
            active: isNotificationActive,
          }"
          active-class=""
          exact-active-class=""
        >
          <span class="menu-icon"> ◇ </span>

          <span> 通知管理 </span>
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
/* =========================================
   SIDEBAR
========================================= */

.admin-sidebar {
  width: 250px;

  min-width: 250px;

  min-height: calc(100vh - 56px);

  display: flex;

  flex-direction: column;

  background: #111111;

  color: #ffffff;

  border-right: 1px solid #2b2b2b;
}

/* =========================================
   MENU
========================================= */

.sidebar-menu {
  flex: 1;

  padding: 30px 14px;

  overflow-y: auto;
}

/* =========================================
   SECTION
========================================= */

.menu-section {
  margin-bottom: 28px;
}

.menu-section-title {
  margin: 0 10px 10px;

  color: #777777;

  font-size: 10px;

  font-weight: 700;

  letter-spacing: 0.16em;
}

/* =========================================
   MENU ITEM
========================================= */

.menu-item {
  position: relative;

  min-height: 42px;

  display: flex;

  align-items: center;

  gap: 12px;

  padding: 10px 12px;

  margin: 2px 0;

  color: #bdbdbd;

  text-decoration: none;

  font-size: 14px;

  border-radius: 4px;

  transition:
    background 0.15s ease,
    color 0.15s ease;
}

.menu-item:hover {
  background: #1f1f1f;

  color: #ffffff;
}

/* =========================================
   ACTIVE
========================================= */

.menu-item.active {
  background: #ffffff;

  color: #111111;
}

.menu-item.active .menu-icon {
  color: #111111;
}

/* =========================================
   ICON
========================================= */

.menu-icon {
  width: 20px;

  flex-shrink: 0;

  text-align: center;

  color: #aaaaaa;

  font-size: 14px;
}

/* =========================================
   SCROLLBAR
========================================= */

.sidebar-menu::-webkit-scrollbar {
  width: 4px;
}

.sidebar-menu::-webkit-scrollbar-thumb {
  background: #333333;
}
</style>
