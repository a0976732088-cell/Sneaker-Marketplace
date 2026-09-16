<!-- components/notification/NotificationCreateForm.vue -->
<template>
  <div class="card p-4 shadow-sm border-0">
    <h2 class="fw-bold mb-4">新增通知</h2>
    <form @submit.prevent="submitForm">
      <div class="mb-3">
        <label class="form-label"
          >通知標題 <span class="text-danger">*</span></label
        >
        <input
          v-model.trim="form.title"
          type="text"
          class="form-control"
          placeholder="例如：【發售提醒】Air Jordan 1"
          required
        />
      </div>

      <div class="row">
        <div class="col-md-6 mb-3">
          <label for="notificationType" class="form-label"
            >通知類型 <span class="text-danger">*</span></label
          >
          <select
            v-model="form.notificationType"
            class="form-select"
            id="notificationType"
            required
          >
            <option value="" disabled>請選擇通知類型</option>
            <option value="EVENT_REMINDER">活動提醒 (EVENT_REMINDER)</option>
            <option value="SYSTEM_NOTICE">系統公告 (SYSTEM_NOTICE)</option>
            <option value="ORDER_UPDATE">訂單更新 (ORDER_UPDATE)</option>
            <option value="PROMOTION">促銷優惠 (PROMOTION)</option>
          </select>
        </div>

        <div class="col-md-6 mb-3">
          <label for="targetType" class="form-label"
            >對象類型 (Target Type)</label
          >
          <select v-model="form.targetType" class="form-select" id="targetType">
            <option value="">無關聯資料</option>
            <option value="SYSTEM">系統公告 (SYSTEM)</option>
            <option value="EVENT">活動 (EVENT)</option>
            <option value="ORDER">訂單 (ORDER)</option>
            <option value="AUCTION">競標 (AUCTION)</option>
            <option value="USER">使用者 (USER)</option>
          </select>
        </div>
      </div>

      <div class="row">
        <div class="col-md-6 mb-3">
          <label class="form-label">對象 ID (Target ID)</label>
          <input
            v-model.number="form.targetId"
            type="number"
            class="form-control"
            placeholder="例如：123"
          />
        </div>

        <div class="col-md-6 mb-3">
          <label class="form-label">跳轉網址 (Target URL)</label>
          <input
            v-model.trim="form.targetUrl"
            type="url"
            class="form-control"
            placeholder="https://example.com"
          />
        </div>
      </div>

      <div class="mb-3">
        <label class="form-label"
          >通知內容 <span class="text-danger">*</span></label
        >
        <textarea
          v-model.trim="form.content"
          class="form-control"
          rows="4"
          placeholder="請輸入詳細通知內文..."
          required
        ></textarea>
      </div>

      <div class="d-flex justify-content-end gap-2">
        <button
          type="button"
          class="btn btn-outline-secondary"
          @click="resetForm"
          :disabled="loading"
        >
          清除
        </button>
        <button type="submit" class="btn btn-primary px-4" :disabled="loading">
          <span
            v-if="loading"
            class="spinner-border spinner-border-sm me-2"
          ></span>
          {{ loading ? "新增中..." : "新增通知" }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import { createNotification } from "@/service/notificationService";

const emit = defineEmits(["created"]);
const loading = ref(false);

const initialForm = {
  title: "",
  notificationType: "",
  targetType: "",
  targetId: null,
  targetUrl: "",
  content: "",
};

const form = reactive({ ...initialForm });

const submitForm = async () => {
  loading.value = true;
  try {
    await createNotification(form);
    alert("新增通知成功！");
    resetForm();
    emit("created");
  } catch (error) {
    console.error("新增通知失敗：", error);
    alert("新增通知失敗，請確認輸入資料。");
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  Object.assign(form, initialForm);
};
</script>
