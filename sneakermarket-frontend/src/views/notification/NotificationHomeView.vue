
<template>
  <main class="preference-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">NOTIFICATION SETTINGS</p>
        <h1>通知偏好</h1>
        <p class="page-description">
          選擇你想接收的通知類型與接收方式。
        </p>
      </div>

      <label class="master-switch">
        <span>
          <strong>接收通知</strong>
          <small>關閉後將暫停下方所有通知</small>
        </span>

        <input
          v-model="form.allEnabled"
          type="checkbox"
          :disabled="loading || saving"
        />

        <span class="switch-track" aria-hidden="true"></span>
      </label>
    </header>

    <div v-if="loading" class="status-card">
      正在讀取通知偏好……
    </div>

    <template v-else>
      <div
        v-if="successMessage"
        class="message success-message"
      >
        {{ successMessage }}
      </div>

      <div
        v-if="errorMessage"
        class="message error-message"
      >
        {{ errorMessage }}
      </div>

      <section
        class="preference-card"
        :class="{ disabled: !form.allEnabled }"
      >
        <div class="table-header">
          <span>通知類型</span>
          <span>站內通知</span>
          <span>Email</span>
        </div>

        <div
          v-for="row in rows"
          :key="row.key"
          class="preference-row"
        >
          <div class="preference-copy">
            <h2>{{ row.title }}</h2>
            <p>{{ row.description }}</p>
          </div>

          <label class="channel-option">
            <input
              v-model="form[row.inAppField]"
              type="checkbox"
              :disabled="!form.allEnabled || saving"
            />
            <span>站內通知</span>
          </label>

          <label class="channel-option">
            <input
              v-model="form[row.emailField]"
              type="checkbox"
              :disabled="!form.allEnabled || saving"
            />
            <span>Email</span>
          </label>
        </div>
      </section>

      <div class="page-actions">
        <button
          type="button"
          class="save-button"
          :disabled="saving"
          @click="savePreference"
        >
          {{ saving ? "儲存中……" : "儲存通知偏好" }}
        </button>
      </div>
    </template>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";

import {
  getMyNotificationPreference,
  updateMyNotificationPreference,
} from "@/service/notificationService";

const router = useRouter();

const loading = ref(true);
const saving = ref(false);

const errorMessage = ref("");
const successMessage = ref("");

const form = ref({
  allEnabled: true,

  eventInAppEnabled: true,
  eventEmailEnabled: true,

  systemInAppEnabled: true,
  systemEmailEnabled: true,

  releaseInAppEnabled: true,
  releaseEmailEnabled: true,
});

const rows = computed(() => [
  {
    key: "event",
    title: "活動通知",
    description: "活動提醒、延期、時間異動及取消通知。",
    inAppField: "eventInAppEnabled",
    emailField: "eventEmailEnabled",
  },
  {
    key: "system",
    title: "系統公告",
    description: "重要服務公告與會員相關系統訊息。",
    inAppField: "systemInAppEnabled",
    emailField: "systemEmailEnabled",
  },
  {
    key: "release",
    title: "新品通知",
    description: "關注品牌或鞋款類別的新品正式上架通知。",
    inAppField: "releaseInAppEnabled",
    emailField: "releaseEmailEnabled",
  },
]);

function applyPreference(data) {
  form.value = {
    allEnabled: data.allEnabled,

    eventInAppEnabled: data.eventInAppEnabled,
    eventEmailEnabled: data.eventEmailEnabled,

    systemInAppEnabled: data.systemInAppEnabled,
    systemEmailEnabled: data.systemEmailEnabled,

    releaseInAppEnabled: data.releaseInAppEnabled,
    releaseEmailEnabled: data.releaseEmailEnabled,
  };
}

function getApiErrorMessage(error, fallbackMessage) {
  const data = error.response?.data;

  if (typeof data === "string") {
    return data;
  }

  if (data?.message) {
    return data.message;
  }

  return fallbackMessage;
}

async function loadPreference() {
  loading.value = true;
  errorMessage.value = "";

  try {
    const response =
      await getMyNotificationPreference();

    applyPreference(response.data.data);
  } catch (error) {
    if (error.response?.status === 401) {
      await router.replace({
        name: "login",
        query: {
          redirect: "/notifications/preferences",
        },
      });

      return;
    }

    errorMessage.value = getApiErrorMessage(
      error,
      "讀取通知偏好失敗，請稍後再試。",
    );
  } finally {
    loading.value = false;
  }
}

async function savePreference() {
  if (saving.value) {
    return;
  }

  saving.value = true;
  errorMessage.value = "";
  successMessage.value = "";

  try {
    const response =
      await updateMyNotificationPreference({
        ...form.value,
      });

    applyPreference(response.data.data);

    successMessage.value = "通知偏好已儲存。";
  } catch (error) {
    errorMessage.value = getApiErrorMessage(
      error,
      "儲存通知偏好失敗，請稍後再試。",
    );
  } finally {
    saving.value = false;
  }
}

onMounted(loadPreference);
</script>

<style scoped>
.preference-page {
  width: min(1040px, calc(100% - 40px));
  min-height: 70vh;
  margin: 0 auto;
  padding: 56px 0 80px;
  color: #202326;
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 32px;
  padding-bottom: 28px;
  border-bottom: 1px solid #e4e7e8;
}

.eyebrow {
  margin: 0 0 10px;
  color: #a35444;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.16em;
}

.page-header h1 {
  margin: 0;
  font-size: clamp(32px, 5vw, 48px);
}

.page-description {
  margin: 12px 0 0;
  color: #687176;
}

.master-switch {
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
}

.master-switch > span:first-child {
  display: flex;
  flex-direction: column;
  text-align: right;
}

.master-switch small {
  margin-top: 4px;
  color: #7d8589;
}

.master-switch input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.switch-track {
  position: relative;
  width: 52px;
  height: 29px;
  flex: 0 0 auto;
  border-radius: 999px;
  background: #c8ced1;
  transition: 0.2s ease;
}

.switch-track::after {
  position: absolute;
  top: 4px;
  left: 4px;
  width: 21px;
  height: 21px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 2px 5px rgb(0 0 0 / 18%);
  content: "";
  transition: 0.2s ease;
}

.master-switch input:checked + .switch-track {
  background: #e7664e;
}

.master-switch input:checked + .switch-track::after {
  transform: translateX(23px);
}

.preference-card {
  margin-top: 30px;
  overflow: hidden;
  border: 1px solid #dfe3e5;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 12px 35px rgb(31 41 55 / 6%);
}

.preference-card.disabled {
  opacity: 0.62;
}

.table-header,
.preference-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 150px 150px;
  align-items: center;
}

.table-header {
  padding: 16px 24px;
  background: #f5f6f6;
  color: #6f777b;
  font-size: 13px;
  font-weight: 700;
}

.table-header span:not(:first-child) {
  text-align: center;
}

.preference-row {
  min-height: 116px;
  padding: 22px 24px;
  border-top: 1px solid #e8eaeb;
}

.preference-copy h2 {
  margin: 0;
  font-size: 19px;
}

.preference-copy p {
  margin: 8px 0 0;
  color: #737c80;
  font-size: 14px;
}

.channel-option {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  cursor: pointer;
}

.channel-option input {
  width: 18px;
  height: 18px;
  accent-color: #e7664e;
}

.channel-option input:disabled {
  cursor: not-allowed;
}

.page-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}

.save-button {
  min-width: 170px;
  padding: 12px 20px;
  border: 0;
  border-radius: 8px;
  background: #e7664e;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.save-button:disabled {
  cursor: wait;
  opacity: 0.65;
}

.status-card,
.message {
  margin-top: 24px;
  padding: 14px 16px;
  border-radius: 9px;
}

.status-card {
  background: #f3f5f5;
  color: #687176;
}

.success-message {
  background: #e9f7ed;
  color: #25733a;
}

.error-message {
  background: #fcebea;
  color: #a82c25;
}

@media (max-width: 700px) {
  .preference-page {
    width: min(100% - 28px, 1040px);
    padding-top: 32px;
  }

  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .master-switch {
    width: 100%;
    justify-content: space-between;
  }

  .master-switch > span:first-child {
    text-align: left;
  }

  .table-header {
    display: none;
  }

  .preference-row {
    grid-template-columns: 1fr 1fr;
    gap: 20px 10px;
  }

  .preference-copy {
    grid-column: 1 / -1;
  }
}
</style>