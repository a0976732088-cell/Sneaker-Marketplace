<template>
  <div class="event-admin notification-center">
    <header class="admin-header">
      <div class="admin-header-top admin-page-heading">
        <div>
          <span class="admin-eyebrow admin-page-eyebrow">SNEAKERMARKET ADMIN</span>
          <h1 class="admin-title">通知中心</h1>
          <p class="admin-description admin-page-description">
            建立、搜尋、編輯、發送與管理會員通知。
          </p>
        </div>

        <button type="button" class="create-button" @click="toggleCreateForm">
          <span aria-hidden="true">
            {{ showCreateForm ? "×" : "+" }}
          </span>

          {{ showCreateForm ? "關閉新增" : "新增通知" }}
        </button>
      </div>
    </header>

    <section v-if="showCreateForm" class="create-section" aria-label="新增通知">
      <NotificationCreateForm @created="handleCreated" />
    </section>

    <section class="notification-workspace">
      <nav class="status-tabs" aria-label="通知狀態">
        <button
          v-for="tab in statusTabs"
          :key="tab.value"
          type="button"
          class="status-tab"
          :class="{ active: activeStatus === tab.value }"
          @click="selectStatus(tab.value)"
        >
          {{ tab.label }}
        </button>
      </nav>

      <section class="admin-content" aria-label="通知列表">
        <NotificationManager
          :key="notificationManagerKey"
          :sent-status="activeStatus"
        />
      </section>
    </section>
  </div>
</template>

<script setup>
import { ref } from "vue";

import NotificationCreateForm from "@/components/notification/NotificationCreateForm.vue";
import NotificationManager from "@/components/notification/NotificationManager.vue";

const showCreateForm = ref(false);
const notificationManagerKey = ref(0);

function toggleCreateForm() {
  showCreateForm.value = !showCreateForm.value;
}

function handleCreated() {
  showCreateForm.value = false;
  notificationManagerKey.value += 1;
}

const activeStatus = ref("ALL");

const statusTabs = [
  {
    value: "ALL",
    label: "全部",
  },
  {
    value: "UNSENT",
    label: "未發送",
  },
  {
    value: "SENT",
    label: "已發送",
  },
];

function selectStatus(status) {
  activeStatus.value = status;
}
</script>

<style scoped>
.notification-center {
  width: 100%;
  min-width: 0;
}

.create-button {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 46px;
  padding: 0 22px;
  border: 1px solid #111111;
  border-radius: 4px;
  color: #ffffff;
  background: #111111;
  font-size: 14px;
  font-weight: 650;
  cursor: pointer;
  transition:
    background-color 150ms ease,
    transform 150ms ease;
}

.create-button:hover {
  background: #2b2b2b;
  transform: translateY(-1px);
}

.create-button span {
  font-size: 22px;
  font-weight: 400;
  line-height: 1;
}

.create-section {
  margin-bottom: 24px;
}

.create-section :deep(.card),
.admin-content :deep(.card) {
  border: 1px solid #d8d7d2 !important;
  border-radius: 6px !important;
  background: #ffffff !important;
  box-shadow: none !important;
}

.create-section :deep(.form-control),
.create-section :deep(.form-select),
.admin-content :deep(.form-control),
.admin-content :deep(.form-select) {
  border-color: #d8d7d2;
  border-radius: 4px;
}

.create-section :deep(.form-control:focus),
.create-section :deep(.form-select:focus),
.admin-content :deep(.form-control:focus),
.admin-content :deep(.form-select:focus) {
  border-color: #111111;
  box-shadow: 0 0 0 2px rgb(17 17 17 / 8%);
}

.notification-workspace {
  min-width: 0;
}

.status-tabs {
  display: flex;
  gap: 28px;
  overflow-x: auto;
  margin-bottom: 16px;
  border-bottom: 1px solid #deddd8;
}

.status-tab {
  position: relative;
  flex: 0 0 auto;
  padding: 12px 2px 14px;
  border: 0;
  color: #777771;
  background: transparent;
  font-size: 13px;
  font-weight: 650;
  cursor: pointer;
}

.status-tab::after {
  position: absolute;
  right: 0;
  bottom: -1px;
  left: 0;
  height: 2px;
  background: transparent;
  content: "";
}

.status-tab:hover {
  color: #111111;
}

.status-tab.active {
  color: #111111;
}

.status-tab.active::after {
  background: #111111;
}

@media (max-width: 768px) {
  .create-button {
    width: 100%;
  }
}
</style>
