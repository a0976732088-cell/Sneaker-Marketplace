<script setup>
import NotificationLog
  from "@/components/notification/NotificationLog.vue";
</script>

<template>
  <section class="operation-page">
    <header class="log-page-header admin-page-heading">
      <div>
        <p class="eyebrow admin-page-eyebrow">SNEAKERMARKET ADMIN</p>
        <h1>發送紀錄</h1>
        <p class="admin-page-description">查詢通知發送結果與投遞狀態。</p>
      </div>
    </header>

    <NotificationLog />
  </section>
</template>

<style scoped>
.operation-page {
  width: 100%;
}

.log-page-header {
  margin-bottom: 20px;
}

.log-page-header h1 {
  margin: 0;
  font-size: 40px;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.log-page-header p {
  margin: 8px 0 0;
  color: #73766f;
}

.eyebrow {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
}
</style>
