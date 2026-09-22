<template>
  <section class="operation-page">
    <header class="log-page-header">
      <div>
        <h1>發送紀錄</h1>
        <p>查詢通知發送結果與投遞狀態。</p>
      </div>

      <button type="button" class="export-button">↓ 匯出紀錄</button>
    </header>

    <NotificationLog />
  </section>
</template>

<script setup>
import NotificationLog from "@/components/notification/NotificationLog.vue";
</script>

<style scoped>
.operation-page {
  width: 100%;
}

.log-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 14px;
}

.log-page-header h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.log-page-header p {
  margin: 5px 0 0;
  color: #73766f;
  font-size: 12px;
}

.export-button {
  height: 36px;
  padding: 0 14px;
  border: 1px solid #373936;
  border-radius: 5px;
  color: #30322f;
  background: #ffffff;
  font-size: 12px;
  font-weight: 650;
}
</style>
