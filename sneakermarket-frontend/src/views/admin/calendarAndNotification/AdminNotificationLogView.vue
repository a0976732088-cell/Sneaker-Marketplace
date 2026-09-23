<template>
  <section class="operation-page">
    <header class="log-page-header admin-page-heading">
      <div>
        <h1>發送紀錄</h1>
        <p class="admin-page-description">查詢通知發送結果與投遞狀態。</p>
      </div>

      <button
        type="button"
        class="export-button"
        :disabled="exporting"
        @click="handleExport"
      >
        {{ exporting ? "匯出中…" : "↓ 匯出紀錄" }}
      </button>
    </header>

    <NotificationLog ref="notificationLogRef" />
  </section>
</template>

<script setup>
import { ref } from "vue";

import NotificationLog from "@/components/notification/NotificationLog.vue";

const notificationLogRef = ref(null);
const exporting = ref(false);

const handleExport = async () => {
  if (
    exporting.value ||
    !notificationLogRef.value
  ) {
    return;
  }

  exporting.value = true;

  try {
    const count =
      await notificationLogRef.value.exportLogsCsv();

    if (count === 0) {
      alert("目前搜尋條件下沒有可匯出的紀錄。");
    }
  } catch (error) {
    console.error("匯出發送紀錄失敗：", error);
    alert("匯出失敗，請稍後再試。");
  } finally {
    exporting.value = false;
  }
};
</script>

<style scoped>
.export-button {
  cursor: pointer;
}

.export-button:hover:not(:disabled) {
  background: #f3f3f0;
}

.export-button:disabled {
  cursor: wait;
  opacity: 0.55;
}

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
