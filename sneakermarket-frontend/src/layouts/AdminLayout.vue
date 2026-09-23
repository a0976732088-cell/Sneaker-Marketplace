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
.admin-page {
  width: 100%;
  min-height: calc(100vh - 56px);
  background: #f8f9fa;
}

.admin-layout {
  display: flex;
  align-items: stretch;
  min-height: calc(100vh - 56px);
  padding: 0;
  background: #f8f9fa;
}

.admin-content {
  flex: 1;
  min-width: 0;
}

.admin-content-inner {
  width: 100%;
  min-width: 0;
  padding: 32px 32px 64px;
}

@media (max-width: 992px) {
  .admin-layout {
    flex-direction: column;
    max-width: 100%;
  }

  .admin-content-inner {
    padding: 28px 24px 56px;
  }
}

@media (max-width: 576px) {
  .admin-content-inner {
    padding: 24px 16px 48px;
  }
}
</style>