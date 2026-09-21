<template>
  <section class="google-calendar-card">
    <div class="calendar-icon" aria-hidden="true">
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <rect
          x="3"
          y="5"
          width="18"
          height="16"
          rx="3"
        />
        <path d="M16 3v4M8 3v4M3 10h18" />
        <path d="m8 15 2.5 2.5L16 12" />
      </svg>
    </div>

    <div class="connection-content">
      <div class="connection-heading">
        <div>
          <span class="eyebrow">
            GOOGLE CALENDAR
          </span>

          <h2>同步我的活動行程</h2>
        </div>

        <span
          v-if="status === 'loading'"
          class="status-label"
        >
          確認中…
        </span>

        <RouterLink
          v-else-if="status === 'guest'"
          class="sync-toggle"
          :to="{
            path: '/login',
            query: {
              redirect: '/calendar',
            },
          }"
        >
          登入後同步
        </RouterLink>

        <span
          v-else-if="status === 'unavailable'"
          class="status-label unavailable"
        >
          未啟用
        </span>

        <button
          v-else
          type="button"
          class="sync-toggle"
          :class="{ connected }"
          :disabled="actionLoading"
          :title="
            connected
              ? '點擊停止同步'
              : '點擊開啟同步'
          "
          @click="toggleSync"
        >
          {{
            actionLoading
              ? "處理中…"
              : connected
                ? "已同步"
                : "開啟同步"
          }}
        </button>
      </div>

      <p class="description">
        開啟後，關注的活動會自動加入 Google Calendar；
        活動更新或取消時也會同步處理。
      </p>

      <p
        v-if="message"
        class="message"
        :class="messageType"
      >
        {{ message }}
      </p>
    </div>
  </section>
</template>

<script setup>
import {
  onMounted,
  ref,
} from "vue";

import {
  useRoute,
  useRouter,
} from "vue-router";

import {
  disconnectGoogleCalendar,
  getGoogleCalendarConnectUrl,
  getGoogleCalendarStatus,
} from "@/service/calendarService";

const route = useRoute();
const router = useRouter();

const status = ref("loading");
const connected = ref(false);
const actionLoading = ref(false);

const message = ref("");
const messageType = ref("success");

async function removeOAuthQuery() {
  if (!route.query.googleCalendar) {
    return;
  }

  const nextQuery = {
    ...route.query,
  };

  delete nextQuery.googleCalendar;

  await router.replace({
    path: route.path,
    query: nextQuery,
    hash: route.hash,
  });
}

async function handleOAuthResult() {
  const result =
    route.query.googleCalendar;

  if (result === "connected") {
    message.value =
      "Google Calendar 同步已開啟。";

    messageType.value =
      "success";
  }

  if (result === "failed") {
    message.value =
      "Google Calendar 授權失敗或已取消。";

    messageType.value =
      "error";
  }

  await removeOAuthQuery();
}

async function loadStatus() {
  status.value = "loading";

  try {
    const response =
      await getGoogleCalendarStatus();

    connected.value =
      Boolean(
        response.data?.data?.connected,
      );

    status.value = "ready";
  } catch (error) {
    connected.value = false;

    if (
      error.response?.status === 401
    ) {
      status.value = "guest";
      return;
    }

    status.value = "unavailable";

    message.value =
      "目前無法使用 Google Calendar 同步。";

    messageType.value =
      "error";

    console.error(
      "讀取 Google Calendar 狀態失敗：",
      error,
    );
  }
}

async function toggleSync() {
  if (!connected.value) {
    actionLoading.value = true;

    window.location.assign(
      getGoogleCalendarConnectUrl(),
    );

    return;
  }

  const confirmed =
    window.confirm(
      "確定要停止 Google Calendar 同步嗎？\n本站建立的 Google 行程也會一併移除。",
    );

  if (!confirmed) {
    return;
  }

  actionLoading.value = true;
  message.value = "";

  try {
    await disconnectGoogleCalendar();

    connected.value = false;

    message.value =
      "Google Calendar 同步已停止。";

    messageType.value =
      "success";
  } catch (error) {
    message.value =
      "停止 Google Calendar 同步失敗，請稍後再試。";

    messageType.value =
      "error";

    console.error(
      "停止 Google Calendar 同步失敗：",
      error,
    );
  } finally {
    actionLoading.value = false;
  }
}

onMounted(async () => {
  await handleOAuthResult();
  await loadStatus();
});
</script>

<style scoped>
.google-calendar-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 14px;
  padding: 18px;
  border: 1px solid #e3e6e8;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 24px rgb(31 41 55 / 6%);
}

.calendar-icon {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  border-radius: 13px;
  background:
    linear-gradient(
      135deg,
      #4285f4,
      #34a853
    );
  color: #fff;
  box-shadow: 0 8px 16px rgb(66 133 244 / 20%);
}

.calendar-icon svg {
  width: 27px;
  height: 27px;
}

.connection-content {
  min-width: 0;
}

.connection-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.eyebrow {
  color: #7b848a;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.connection-heading h2 {
  margin: 3px 0 0;
  color: #202326;
  font-size: 17px;
}

.description {
  margin: 9px 0 0;
  color: #687177;
  font-size: 12px;
  line-height: 1.7;
}

.sync-toggle,
.status-label {
  flex-shrink: 0;
  padding: 6px 11px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.sync-toggle {
  border: 1px solid #cdd4d8;
  background: #fff;
  color: #596166;
  text-decoration: none;
  cursor: pointer;
}

.sync-toggle:hover {
  border-color: #4285f4;
  color: #2767c5;
}

.sync-toggle.connected {
  border-color: #b9dec5;
  background: #e4f3e9;
  color: #24713d;
}

.sync-toggle.connected:hover {
  border-color: #e3b8b1;
  background: #faece9;
  color: #9a4338;
}

.sync-toggle:disabled {
  cursor: wait;
  opacity: 0.65;
}

.status-label {
  background: #eef1f0;
  color: #697079;
}

.status-label.unavailable {
  background: #f8e9e5;
  color: #8c3b32;
}

.message {
  margin: 11px 0 0;
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 12px;
}

.message.success {
  background: #e8f5ec;
  color: #276d3d;
}

.message.error {
  background: #f8e9e5;
  color: #8c3b32;
}

@media (max-width: 650px) {
  .connection-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .sync-toggle,
  .status-label {
    align-self: flex-start;
  }
}
</style>