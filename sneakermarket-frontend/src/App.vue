<script setup>
import { computed } from "vue";

import { useRoute } from "vue-router";

import AppNavbar from "@/components/layout/AppNavbar.vue";

import AppFooter from "@/components/layout/AppFooter.vue";

import ChatbotWidget from "@/components/commerce/ChatbotWidget.vue";
const isCommercePage = computed(() => {
  return (
    route.path.startsWith("/products") ||
    route.path === "/cart" ||
    route.path === "/checkout" ||
    route.path.startsWith("/orders")
  );
});


const route = useRoute();

// =====================================================
// 判斷目前是不是 Admin
// =====================================================

const isAdminPage = computed(() => {
  return route.path.startsWith("/admin");
});
</script>

<template>
  <div class="app-shell">
    <!-- =====================================
         全站共用 Navbar
    ====================================== -->

    <AppNavbar />

    <!-- =====================================
         ADMIN

         AdminLayout 自己處理 container
         所以這裡不能再加 container
    ====================================== -->

    <main v-if="isAdminPage" class="admin-main">
      <RouterView />
    </main>

    <!-- =====================================
         一般前台

         沿用原本 Bootstrap container
    ====================================== -->

    <main v-else class="app-main">
      <div class="container py-4">
        <RouterView />
      </div>
    </main>

    <!-- =====================================
         Admin 不顯示 Footer
    ====================================== -->

    <AppFooter v-if="!isAdminPage" />

    <!-- 一般商城機器人 -->
    <ChatbotWidget v-if="isCommercePage" />
   
  </div>
</template>

<style>
html,
body,
#app {
  margin: 0;

  width: 100%;

  min-height: 100%;

  background: #ffffff;
}

* {
  box-sizing: border-box;
}

.app-shell {
  min-height: 100vh;

  display: flex;

  flex-direction: column;
}

/* =========================================
   一般前台
========================================= */

.app-main {
  flex: 1;
}

/* =========================================
   Admin

   千萬不要在這裡限制 max-width
========================================= */

.admin-main {
  flex: 1;

  width: 100%;

  padding: 0;

  margin: 0;

  background: #f7f7f7;
}
</style>
