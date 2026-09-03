<template>
  <div class="container navbar-space">
    <!-- 切換 Tab 按鈕 -->
    <div class="tab-buttons mb-4">
      <button
        :class="[
          'btn',
          activeTab === 'create' ? 'btn-primary' : 'btn-outline-primary',
        ]"
        @click="activeTab = 'create'"
      >
        新增活動
      </button>
      <button
        :class="[
          'btn',
          activeTab === 'search' ? 'btn-primary' : 'btn-outline-primary',
          'ms-2',
        ]"
        @click="switchTab('search')"
      >
        查詢活動
      </button>
    </div>

    <!-- 1. 新增表單區塊 -->
    <EventCreateForm v-if="activeTab === 'create'" />

    <!-- 2. 查詢與列表區塊 -->
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

    <!-- 3. 詳細資料彈窗 -->
    <EventDetailModal
      v-if="showDetailModal"
      :event="selectedEvent"
      @close="showDetailModal = false"
    />

    <!-- 4. 編輯彈窗 -->
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

const activeTab = ref(localStorage.getItem("event_active_tab") || "search");

// 查詢與分頁狀態
const eventList = ref([]);
const currentPage = ref(1);
const totalPages = ref(1);
const searchParams = reactive({});

// Modal 控制狀態
const showDetailModal = ref(false);
const showEditModal = ref(false);
const selectedEvent = ref(null);

// 抓取分頁列表
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

// 搜尋與重設處理
const handleSearch = (filters) => {
  Object.assign(searchParams, filters);
  fetchEvents(1);
};

const handleResetSearch = () => {
  Object.keys(searchParams).forEach((key) => delete searchParams[key]);
  fetchEvents(1);
};

// 切換 Tab
const switchTab = (tab) => {
  activeTab.value = tab;
  if (tab === "search") fetchEvents(1);
};

// Modal 開啟
const openDetailModal = (event) => {
  selectedEvent.value = event;
  showDetailModal.value = true;
};

const openEditModal = (event) => {
  selectedEvent.value = event;
  showEditModal.value = true;
};

// 刪除處理
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

// 監聽 Tab 寫入 localStorage 且初始化載入
watch(
  activeTab,
  (newTab) => {
    localStorage.setItem("event_active_tab", newTab);
    if (newTab === "search") fetchEvents(1);
  },
  { immediate: true },
);
</script>
