<script setup>
import AdminSidebar from "@/components/admin/AdminSidebar.vue";
</script>

<template>
  <!-- =========================================
       整個 Admin 區域背景
  ========================================== -->
  <div class="admin-page">
    <!-- =========================================
         重要：
         使用跟 Navbar 一樣的 Bootstrap container

         所以：
         左側 Sidebar 會對齊 SneakerMarket
         右側內容會對齊 登出
    ========================================== -->
    <div class="container admin-layout">
      <!-- =====================================
           左側 Sidebar
      ====================================== -->
      <AdminSidebar />

      <!-- =====================================
           右側內容
      ====================================== -->
      <main class="admin-content">
        <div class="admin-content-inner">
          <RouterView />
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
/* =========================================
   整個後台背景
========================================= */

.admin-page {
  width: 100%;

  min-height: calc(100vh - 56px);

  background: #f7f7f7;
}

/* =========================================
   Admin 主框架

   Bootstrap .container
   會自動使用跟 Navbar 相同寬度

   所以不用自己寫 1200 / 1400 px
========================================= */

.admin-layout {
  display: flex;

  align-items: stretch;

  min-height: calc(100vh - 56px);

  padding-left: 0;

  padding-right: 0;

  background: #ffffff;
}

/* =========================================
   右側 Content
========================================= */

.admin-content {
  flex: 1;

  min-width: 0;

  background: #f7f7f7;
}

/* =========================================
   後台內容

   不要再 max-width: 1400px
   否則裡面又會自己置中一次
========================================= */

.admin-content-inner {
  width: 100%;

  padding: 38px 40px 70px;

  box-sizing: border-box;
}

/* =========================================
   大螢幕
========================================= */

@media (min-width: 1200px) {
  .admin-content-inner {
    padding-left: 48px;

    padding-right: 48px;
  }
}

/* =========================================
   Tablet
========================================= */

@media (max-width: 992px) {
  .admin-layout {
    max-width: 100%;
  }

  .admin-content-inner {
    padding: 28px 24px 60px;
  }
}

/* =========================================
   Mobile

   小螢幕先讓 Sidebar + Content
   保持可以水平顯示
========================================= */

@media (max-width: 768px) {
  .admin-content-inner {
    padding: 24px 18px 50px;
  }
}
</style>
