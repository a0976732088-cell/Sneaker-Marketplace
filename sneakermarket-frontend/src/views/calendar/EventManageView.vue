<template>
  <div class="container navbar-space">
    <!-- 同一排 Tab 按鈕列 -->
    <div class="tab-buttons mb-4">
      <button
        :class="['btn', activeTab === 'create' ? 'btn-primary' : 'btn-outline-primary']"
        @click="activeTab = 'create'"
      >
        新增活動
      </button>

      <button
        :class="['btn', activeTab === 'search' ? 'btn-primary' : 'btn-outline-primary', 'ms-2']"
        @click="switchTab('search')"
      >
        查詢活動
      </button>

      <button
        :class="['btn', activeTab === 'createNotice' ? 'btn-primary' : 'btn-outline-primary', 'ms-2']"
        @click="activeTab = 'createNotice'"
      >
        新增通知
      </button>

      <button
        :class="['btn', activeTab === 'NotificationManager' ? 'btn-primary' : 'btn-outline-primary', 'ms-2']"
        @click="activeTab = 'NotificationManager'"
      >
        查詢通知
      </button>
    </div>

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
  </div>
</template>

<script setup>
import { ref, reactive, watch } from "vue";
import { getEventPage, deleteEvent } from "@/api/events";

import EventCreateForm from "@/components/events/EventCreateForm.vue";
import EventSearchFilter from "@/components/events/EventSearchFilter.vue";
import EventTable from "@/components/events/EventTable.vue";
import EventDetailModal from "@/components/events/EventDetailModal.vue";
import EventEditModal from "@/components/events/EventEditModal.vue";
import NotificationManager from "@/components/notification/NotificationManager.vue";
import NotificationCreateForm from "@/components/notification/NotificationCreateForm.vue";

const activeTab = ref(localStorage.getItem("event_active_tab") || "search");

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
  if (!confirm(`確定要刪除 ID: ${eventId} 的活動嗎？`)) return;

  try {
    await deleteEvent(eventId);
    alert("刪除成功！");
    fetchEvents(currentPage.value);
  } catch (error) {
    console.error("刪除失敗", error);
    alert("刪除失敗！");
  }
};

watch(
  activeTab,
  (newTab) => {
    localStorage.setItem("event_active_tab", newTab);
    if (newTab === "search") fetchEvents(1);
  },
  { immediate: true }
);
</script>