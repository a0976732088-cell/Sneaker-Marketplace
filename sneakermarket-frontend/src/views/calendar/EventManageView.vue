<template>
<div class="event-admin navbar-space">
      <!-- 同一排 Tab 按鈕列 -->
<header class="admin-header">
  <div class="admin-header-top">
    <div>
      <span class="admin-eyebrow">
        SNEAKERMARKET ADMIN
      </span>

      <h1 class="admin-title">
        活動與通知管理
      </h1>

      <p class="admin-description">
        管理平台活動、會員關注、系統通知與發送紀錄。
      </p>
    </div>

    <span class="admin-badge">
      CALENDAR · NOTIFICATION
    </span>
  </div>

  <nav class="admin-tabs">
    <button
      type="button"
      :class="[
        'admin-tab',
        { active: activeTab === 'dashboard' },
      ]"
      @click="switchTab('dashboard')"
    >
      後台首頁
    </button>

    <button
      type="button"
      :class="[
        'admin-tab',
        { active: activeTab === 'create' },
      ]"
      @click="switchTab('create')"
    >
      新增活動
    </button>

    <button
      type="button"
      :class="[
        'admin-tab',
        { active: activeTab === 'search' },
      ]"
      @click="switchTab('search')"
    >
      查詢活動
    </button>

    <button
      type="button"
      :class="[
        'admin-tab',
        { active: activeTab === 'createNotice' },
      ]"
      @click="switchTab('createNotice')"
    >
      新增通知
    </button>

    <button
      type="button"
      :class="[
        'admin-tab',
        { active: activeTab === 'NotificationManager' },
      ]"
      @click="switchTab('NotificationManager')"
    >
      查詢通知
    </button>

    <button
      type="button"
      :class="[
        'admin-tab',
        { active: activeTab === 'NotificationLog' },
      ]"
      @click="switchTab('NotificationLog')"
    >
      通知發送紀錄
    </button>
  </nav>
</header>

<main class="admin-content">

    <!-- 通知與行事曆後台首頁 -->
    <NotificationCalendarDashboard
      v-if="activeTab === 'dashboard'"
      @navigate="switchTab"
    />

    <!-- 1. 新增活動表單 -->
    <EventCreateForm v-if="activeTab === 'create'" />

    <!-- 2. 查詢與活動列表 -->
    <div v-if="activeTab === 'search'" class="card p-4">
      <h2>查詢活動</h2>
      <EventSearchFilter @search="handleSearch" @reset="handleResetSearch" />
      <EventTable
        :events="eventList"
        :current-page="currentPage"
        :total-pages="totalPages"
        @open-detail="openDetailModal"
        @open-edit="openEditModal"
        @delete="handleDelete"
        @change-page="fetchEvents"
      />
    </div>

    <!-- 3. 新增通知表單 -->
    <NotificationCreateForm
      v-if="activeTab === 'createNotice'"
      @created="activeTab = 'NotificationManager'"
    />

    <!-- 4. 通知管理與查詢區塊 -->
    <NotificationManager v-if="activeTab === 'NotificationManager'" />

    <!-- 5. 通知管理與查詢區塊 -->
    <NotificationLog v-if="activeTab === 'NotificationLog'" />

    <!-- 詳細資料彈窗 -->
    <EventDetailModal
      v-if="showDetailModal"
      :event="selectedEvent"
      @close="showDetailModal = false"
    />

    <!-- 編輯彈窗 -->
    <EventEditModal
      v-if="showEditModal"
      :event="selectedEvent"
      @close="showEditModal = false"
      @success="fetchEvents(currentPage)"
    />
    </main>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import { getEventPage, deleteEvent } from "@/api/events";

import EventCreateForm from "@/components/events/EventCreateForm.vue";
import EventSearchFilter from "@/components/events/EventSearchFilter.vue";
import EventTable from "@/components/events/EventTable.vue";
import EventDetailModal from "@/components/events/EventDetailModal.vue";
import EventEditModal from "@/components/events/EventEditModal.vue";
import NotificationManager from "@/components/notification/NotificationManager.vue";
import NotificationCreateForm from "@/components/notification/NotificationCreateForm.vue";
import NotificationLog from "@/components/notification/NotificationLog.vue";
import NotificationCalendarDashboard from "@/components/dashboard/NotificationCalendarDashboard.vue";

const activeTab = ref("dashboard");

const eventList = ref([]);
const currentPage = ref(1);
const totalPages = ref(1);
const searchParams = reactive({});

const showDetailModal = ref(false);
const showEditModal = ref(false);
const selectedEvent = ref(null);

const fetchEvents = async (page = 1) => {
  try {
    const params = { pageNumber: page };
    if (searchParams.title?.trim()) params.title = searchParams.title.trim();
    if (searchParams.eventType) params.eventType = searchParams.eventType;
    if (searchParams.status) params.status = searchParams.status;
    if (searchParams.startDate) {
      params.startAt =
        searchParams.startDate.length === 16
          ? `${searchParams.startDate}:00`
          : searchParams.startDate;
    }
    if (searchParams.endDate) {
      params.endAt =
        searchParams.endDate.length === 16
          ? `${searchParams.endDate}:00`
          : searchParams.endDate;
    }

    const response = await getEventPage(params);
    eventList.value = response.data.content || [];
    totalPages.value = response.data.totalPages || 1;
    currentPage.value = page;
  } catch (error) {
    console.error("查詢失敗:", error);
  }
};

const handleSearch = (filters) => {
  Object.assign(searchParams, filters);
  fetchEvents(1);
};

const handleResetSearch = () => {
  Object.keys(searchParams).forEach((key) => delete searchParams[key]);
  fetchEvents(1);
};

const switchTab = (tab) => {
  activeTab.value = tab;
  if (tab === "search") fetchEvents(1);
};

const openDetailModal = (event) => {
  selectedEvent.value = event;
  showDetailModal.value = true;
};

const openEditModal = (event) => {
  selectedEvent.value = event;
  showEditModal.value = true;
};

const handleDelete = async (eventId) => {
  const confirmed = confirm(
    `確定要取消 ID: ${eventId} 的活動嗎？\n取消後將通知目前的關注會員。`,
  );

  if (!confirmed) {
    return;
  }

  try {
    await deleteEvent(eventId);

    alert("活動已取消，關注會員將收到通知。");

    fetchEvents(currentPage.value);
  } catch (error) {
    console.error("取消活動失敗", error);
    alert("取消活動失敗！");
  }
};
</script>
