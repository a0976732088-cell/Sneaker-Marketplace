<template>
  <section class="operation-page">
<header class="overview-header">
  <div>
    <h1>活動營運中心</h1>
    <p>掌握活動、會員關注與通知發送狀態。</p>
  </div>

  <div class="header-actions">
    <select class="range-select" aria-label="統計區間">
      <option>近 30 天</option>
    </select>

    <button
      type="button"
      class="create-button"
      @click="$router.push({ name: 'admin-events' })"
    >
      <span>＋</span>
      建立
    </button>
  </div>
</header>

    <NotificationCalendarDashboard @navigate="navigate" />
  </section>
</template>

<script setup>
import { useRouter } from "vue-router";
import NotificationCalendarDashboard from "@/components/dashboard/NotificationCalendarDashboard.vue";

const router = useRouter();

const navigate = (target) => {
  const routes = {
    search: "admin-events",
    NotificationManager: "admin-notifications",
    NotificationLog: "admin-notification-logs",
  };

  if (routes[target]) {
    router.push({ name: routes[target] });
  }
};
</script>

<style scoped>
.operation-page {
  width: 100%;
}

.overview-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 14px;
}

.overview-header h1 {
  margin: 0;
  color: #171916;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.overview-header p {
  margin: 5px 0 0;
  color: #747871;
  font-size: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.range-select {
  min-width: 105px;
  height: 36px;
  padding: 0 12px;
  border: 1px solid #dedfdb;
  border-radius: 5px;
  color: #444742;
  background: #ffffff;
  font-size: 12px;
}

.create-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 16px;
  border: 0;
  border-radius: 5px;
  color: #ffffff;
  background: #232421;
  font-size: 12px;
  font-weight: 650;
}

.create-button:hover {
  background: #111211;
}

@media (max-width: 640px) {
  .overview-header {
    flex-direction: column;
  }
}
</style>
