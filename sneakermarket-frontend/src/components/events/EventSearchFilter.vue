<template>
  <div class="event-filter">
    <input
      v-model="searchForm.title"
      type="search"
      class="filter-control search-control"
      placeholder="搜尋活動名稱"
      @keyup.enter="handleSearch"
    />

    <select v-model="searchForm.status" class="filter-control">
      <option value="">全部狀態</option>
      <option value="ACTIVE">啟用</option>
      <option value="INACTIVE">停用</option>
      <option value="DRAFT">草稿</option>
      <option value="CANCELLED">已取消</option>
    </select>

    <select v-model="searchForm.eventType" class="filter-control">
      <option value="">全部活動類型</option>
      <option value="RELEASE">新品發售</option>
      <option value="SALE">優惠活動</option>
      <option value="EVENT">品牌活動</option>
      <option value="NOTICE">公告</option>
      <option value="OTHER">其他</option>
    </select>

    <div class="date-range">
      <input
        v-model="searchForm.startDate"
        type="datetime-local"
        class="date-input"
        aria-label="開始時間"
      />

      <span>～</span>

      <input
        v-model="searchForm.endDate"
        type="datetime-local"
        class="date-input"
        aria-label="結束時間"
      />
    </div>

    <button type="button" class="reset-button" @click="handleReset">
      重設
    </button>

    <button type="button" class="search-button" @click="handleSearch">
      查詢
    </button>
  </div>
</template>

<script setup>
import { reactive } from "vue";

const emit = defineEmits(["search", "reset"]);

const searchForm = reactive({
  title: "",
  eventType: "",
  status: "",
  startDate: "",
  endDate: "",
});

const handleSearch = () => {
  emit("search", { ...searchForm });
};

const handleReset = () => {
  searchForm.title = "";
  searchForm.eventType = "";
  searchForm.status = "";
  searchForm.startDate = "";
  searchForm.endDate = "";
  emit("reset");
};
</script>

<style scoped>
.event-filter {
  display: grid;
  grid-template-columns:
    minmax(170px, 1.3fr)
    minmax(110px, 0.7fr)
    minmax(130px, 0.8fr)
    minmax(280px, 1.5fr)
    auto
    auto;
  gap: 8px;
  margin-bottom: 12px;
}

.filter-control {
  min-width: 0;
  height: 38px;
  padding: 0 11px;
  border: 1px solid #dfe0dc;
  border-radius: 5px;
  color: #424540;
  background: #ffffff;
  font-size: 12px;
}

.search-control {
  padding-left: 14px;
}

.reset-button,
.search-button {
  height: 38px;
  padding: 0 14px;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 650;
}

.reset-button {
  border: 1px solid #dfe0dc;
  color: #555852;
  background: #ffffff;
}

.search-button {
  border: 1px solid #222320;
  color: #ffffff;
  background: #222320;
}

.date-range {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  height: 38px;
  padding: 0 9px;
  border: 1px solid #dfe0dc;
  border-radius: 5px;
  background: #ffffff;
}

.date-range span {
  flex: 0 0 auto;
  color: #969993;
  font-size: 11px;
}

.date-input {
  min-width: 0;
  width: 100%;
  border: 0;
  outline: 0;
  color: #424540;
  background: transparent;
  font-size: 11px;
}

@media (max-width: 1000px) {
  .event-filter {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
