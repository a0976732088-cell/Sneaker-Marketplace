<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { deleteEvent, getEventPage } from "@/service/eventService";

import EventCreateForm from "@/components/events/EventCreateForm.vue";
import EventSearchFilter from "@/components/events/EventSearchFilter.vue";
import EventTable from "@/components/events/EventTable.vue";
import EventDetailModal from "@/components/events/EventDetailModal.vue";
import EventEditModal from "@/components/events/EventEditModal.vue";

const showCreateForm = ref(false);
const eventList = ref([]);
const currentPage = ref(1);
const totalPages = ref(1);
const loading = ref(false);
const activeStatus = ref("ALL");

const statusTabs = [
  { value: "ALL", label: "全部" },
  { value: "ACTIVE", label: "啟用中" },
  { value: "INACTIVE", label: "停用" },
  { value: "DRAFT", label: "草稿" },
  { value: "CANCELLED", label: "已取消" },
];

const searchParams = reactive({});

const selectedEvent = ref(null);
const showDetailModal = ref(false);
const showEditModal = ref(false);

const currentPageActiveCount = computed(() =>
  eventList.value.filter((event) => event.status === "ACTIVE").length,
);

function formatDateTimeParam(value) {
  if (!value) return undefined;
  return value.length === 16 ? `${value}:00` : value;
}

async function fetchEvents(page = 1) {
  if (page < 1 || (totalPages.value > 1 && page > totalPages.value)) {
    return;
  }

  loading.value = true;

  try {
    const params = {
      pageNumber: page,
    };

    if (searchParams.title?.trim()) {
      params.title = searchParams.title.trim();
    }

    if (searchParams.eventType) {
      params.eventType = searchParams.eventType;
    }

    if (searchParams.status) {
      params.status = searchParams.status;
    }

    if (searchParams.startDate) {
      params.startAt = formatDateTimeParam(searchParams.startDate);
    }

    if (searchParams.endDate) {
      params.endAt = formatDateTimeParam(searchParams.endDate);
    }

    const response = await getEventPage(params);
    const pageData = response.data ?? {};

    eventList.value = pageData.content ?? [];
    totalPages.value = Math.max(pageData.totalPages ?? 1, 1);
    currentPage.value = page;
  } catch (error) {
    console.error("查詢活動失敗：", error);
    alert(
      error.response?.data?.message ??
        error.response?.data?.detail ??
        "查詢活動失敗，請稍後再試。",
    );
  } finally {
    loading.value = false;
  }
}

function handleSearch(filters) {
  Object.keys(searchParams).forEach((key) => {
    delete searchParams[key];
  });

  Object.assign(searchParams, filters);
  activeStatus.value = filters.status || "ALL";
  fetchEvents(1);
}

function handleResetSearch() {
  Object.keys(searchParams).forEach((key) => {
    delete searchParams[key];
  });

  activeStatus.value = "ALL";
  fetchEvents(1);
}

function selectStatus(status) {
  activeStatus.value = status;

  if (status === "ALL") {
    delete searchParams.status;
  } else {
    searchParams.status = status;
  }

  fetchEvents(1);
}

function toggleCreateForm() {
  showCreateForm.value = !showCreateForm.value;
}

function openDetailModal(event) {
  selectedEvent.value = event;
  showDetailModal.value = true;
}

function openEditModal(event) {
  selectedEvent.value = event;
  showEditModal.value = true;
}

function closeDetailModal() {
  showDetailModal.value = false;
  selectedEvent.value = null;
}

function closeEditModal() {
  showEditModal.value = false;
  selectedEvent.value = null;
}

async function handleEditSuccess() {
  closeEditModal();
  await fetchEvents(currentPage.value);
}

async function handleDelete(eventId) {
  const confirmed = window.confirm(
    `確定要取消 ID：${eventId} 的活動嗎？\n取消後將通知目前關注此活動的會員。`,
  );

  if (!confirmed) return;

  try {
    await deleteEvent(eventId);
    alert("活動已取消，關注會員將收到通知。");

    const shouldGoToPreviousPage =
      eventList.value.length === 1 && currentPage.value > 1;

    await fetchEvents(
      shouldGoToPreviousPage
        ? currentPage.value - 1
        : currentPage.value,
    );
  } catch (error) {
    console.error("取消活動失敗：", error);
    alert(
      error.response?.data?.message ??
        error.response?.data?.detail ??
        "取消活動失敗，請稍後再試。",
    );
  }
}

onMounted(() => {
  fetchEvents(1);
});
</script>

<template>
  <div class="event-admin activity-management">
    <header class="admin-header">
      <div class="admin-header-top">
        <div>
          <span class="admin-eyebrow">SNEAKERMARKET ADMIN</span>

          <h1 class="admin-title">活動管理</h1>

          <p class="admin-description">
            建立、查詢、編輯與追蹤平台活動。
          </p>
        </div>

        <button
          type="button"
          class="create-button"
          @click="toggleCreateForm"
        >
          <span aria-hidden="true">
            {{ showCreateForm ? "×" : "+" }}
          </span>

          {{ showCreateForm ? "關閉新增" : "新增活動" }}
        </button>
      </div>

      <nav class="status-tabs" aria-label="活動狀態">
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
    </header>

    <section
      v-if="showCreateForm"
      class="create-section"
      aria-label="新增活動"
    >
      <EventCreateForm />
    </section>

    <section class="management-card">
      <div v-if="currentPageActiveCount > 0" class="operation-notice">
        <span class="notice-icon">▣</span>
        <strong>
          本頁共有 {{ currentPageActiveCount }} 場啟用中的活動。
        </strong>
      </div>

      <EventSearchFilter
        @search="handleSearch"
        @reset="handleResetSearch"
      />

      <div v-if="loading" class="loading-state" role="status">
        活動資料載入中……
      </div>

      <EventTable
        v-else
        :events="eventList"
        :current-page="currentPage"
        :total-pages="totalPages"
        @open-detail="openDetailModal"
        @open-edit="openEditModal"
        @delete="handleDelete"
        @change-page="fetchEvents"
      />
    </section>

    <EventDetailModal
      v-if="showDetailModal"
      :event="selectedEvent"
      @close="closeDetailModal"
    />

    <EventEditModal
      v-if="showEditModal"
      :event="selectedEvent"
      @close="closeEditModal"
      @success="handleEditSuccess"
    />
  </div>
</template>

<style scoped>
.activity-management {
  width: 100%;
  min-width: 0;
}

.create-button {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 38px;
  padding: 0 18px;
  border: 0;
  border-radius: 5px;
  color: #ffffff;
  background: #222320;
  font-size: 12px;
  font-weight: 650;
  cursor: pointer;
}

.create-button:hover {
  background: #2b2b2b;
  transform: translateY(-1px);
}

.create-button span {
  font-size: 18px;
  line-height: 1;
}

.create-section {
  margin-bottom: 24px;
}

.status-tabs {
  display: flex;
  gap: 28px;
  overflow-x: auto;
  margin: 0;
  padding: 0;
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

.status-tab.active {
  color: #111111;
}

.status-tab.active::after {
  background: #111111;
}

.management-card {
  min-width: 0;
  padding: 0;
  border: 0;
  background: #ffffff;
}

.operation-notice {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
  padding: 12px 14px;
  border: 1px solid #dbe6d8;
  border-radius: 6px;
  color: #52604e;
  background: #eff4ed;
  font-size: 13px;
}

.notice-icon {
  display: inline-grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 50%;
  background: #dce8d9;
}

.loading-state {
  padding: 48px 20px;
  color: #777777;
  text-align: center;
}

.create-section :deep(.card) {
  border: 1px solid #d8d7d2 !important;
  border-radius: 6px !important;
  background: #ffffff !important;
  box-shadow: none !important;
}

.activity-management :deep(.admin-header) {
  margin-bottom: 14px;
  padding-top: 0;
}

.activity-management :deep(.admin-title) {
  margin-bottom: 4px;
  font-size: 28px;
}

.activity-management :deep(.admin-description) {
  font-size: 12px;
}

@media (max-width: 768px) {
  .create-button {
    width: 100%;
  }

  .management-card {
    padding: 16px;
  }
}
</style>
