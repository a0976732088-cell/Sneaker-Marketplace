<script setup>
import { ref } from "vue";

import NotificationManager from "@/components/notification/NotificationManager.vue";

import NotificationCreateForm from "@/components/notification/NotificationCreateForm.vue";

// =====================================================
// 新增通知表單
// =====================================================

const showCreateForm = ref(false);

// =====================================================
// 用 key 讓 NotificationManager
// 在新增成功後重新載入資料
// =====================================================

const notificationManagerKey = ref(0);

// =====================================================
// 開啟新增通知
// =====================================================

function openCreateForm() {
  showCreateForm.value = true;
}

// =====================================================
// 關閉新增通知
// =====================================================

function closeCreateForm() {
  showCreateForm.value = false;
}

// =====================================================
// 新增成功
//
// NotificationCreateForm 本身會 emit("created")
//
// 成功之後：
// 1. 關閉表單
// 2. 重建 NotificationManager
// 3. 重新抓通知列表
// =====================================================

function handleCreated() {
  showCreateForm.value = false;

  notificationManagerKey.value += 1;
}
</script>

<template>
  <div class="admin-notification-page">
    <!-- =========================================
         PAGE HEADER
    ========================================== -->

    <header class="page-header">
      <div class="header-text">
        <p class="eyebrow">NOTIFICATION MANAGEMENT</p>

        <h1>通知管理</h1>

        <p class="description">建立、搜尋、編輯、 發送與管理系統通知。</p>
      </div>

      <!-- =====================================
           新增通知
      ====================================== -->

      <button
        type="button"
        class="create-button"
        @click="showCreateForm ? closeCreateForm() : openCreateForm()"
      >
        {{ showCreateForm ? "關閉新增" : "+ 新增通知" }}
      </button>
    </header>

    <!-- =========================================
         CREATE FORM
    ========================================== -->

    <section v-if="showCreateForm" class="create-section">
      <NotificationCreateForm @created="handleCreated" />
    </section>

    <!-- =========================================
         NOTIFICATION MANAGER
    ========================================== -->

    <section class="manager-section">
      <NotificationManager :key="notificationManagerKey" />
    </section>
  </div>
</template>

<style scoped>
/* =========================================
   PAGE
========================================= */

.admin-notification-page {
  width: 100%;

  min-width: 0;
}

/* =========================================
   HEADER
========================================= */

.page-header {
  display: flex;

  justify-content: space-between;

  align-items: flex-start;

  gap: 30px;

  margin-bottom: 28px;
}

.header-text {
  min-width: 0;
}

.eyebrow {
  margin: 0 0 8px;

  color: #777777;

  font-size: 11px;

  font-weight: 700;

  letter-spacing: 0.16em;
}

.page-header h1 {
  margin: 0;

  color: #111111;

  font-size: 30px;

  font-weight: 500;
}

.description {
  margin: 10px 0 0;

  color: #666666;

  font-size: 14px;
}

/* =========================================
   BUTTON
========================================= */

.create-button {
  min-height: 40px;

  flex-shrink: 0;

  padding: 9px 16px;

  border: 1px solid #111111;

  border-radius: 4px;

  background: #111111;

  color: #ffffff;

  font-size: 13px;

  cursor: pointer;

  transition: background 0.15s ease;
}

.create-button:hover {
  background: #2b2b2b;
}

/* =========================================
   CREATE
========================================= */

.create-section {
  margin-bottom: 24px;
}

/* =========================================
   MANAGER
========================================= */

.manager-section {
  width: 100%;

  min-width: 0;
}

/* =========================================
   套用到既有 Notification 元件

   因為 NotificationCreateForm / Manager
   使用 Bootstrap card，
   我們稍微統一 Admin 的外觀。
========================================= */

.manager-section :deep(.card),
.create-section :deep(.card) {
  border: 1px solid #dddddd !important;

  border-radius: 6px !important;

  box-shadow: none !important;
}

/* =========================================
   Admin 內表單
========================================= */

.manager-section :deep(.form-control),
.manager-section :deep(.form-select),
.create-section :deep(.form-control),
.create-section :deep(.form-select) {
  border-color: #cccccc;
}

.manager-section :deep(.form-control:focus),
.manager-section :deep(.form-select:focus),
.create-section :deep(.form-control:focus),
.create-section :deep(.form-select:focus) {
  border-color: #555555;

  box-shadow: none;
}

/* =========================================
   RWD
========================================= */

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }

  .create-button {
    width: 100%;
  }
}
</style>
