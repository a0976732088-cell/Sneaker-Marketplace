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

<template>
  <section class="operation-page">
    <header class="overview-header">
      <div>
        <p class="eyebrow">SNEAKERMARKET ADMIN</p>
        <h1>活動營運中心</h1>
        <p>掌握活動、會員關注與通知發送狀態。</p>
      </div>

      <button
        type="button"
        class="btn btn-dark"
        @click="$router.push({ name: 'admin-events' })"
      >
        ＋ 建立活動
      </button>
    </header>

    <NotificationCalendarDashboard @navigate="navigate" />
  </section>
</template>

<style scoped>
.operation-page {
  width: 100%;
}

.overview-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 28px;
}

.overview-header h1 {
  margin: 0;
  color: #161816;
  font-size: 40px;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.overview-header p {
  margin: 8px 0 0;
  color: #73766f;
}

.eyebrow {
  margin-bottom: 6px !important;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
}
</style>
