<!-- 搜尋篩選區塊 -->
<template>
  <div class="card bg-light border-0 p-3 mb-4">
    <div class="row g-3">
      <div class="col-md-3">
        <label class="form-label fw-bold">活動標題</label>
        <input
          v-model="searchForm.title"
          type="text"
          class="form-control"
          placeholder="搜尋標題關鍵字..."
        />
      </div>

      <div class="col-md-3">
        <label class="form-label fw-bold">活動類型</label>
        <select v-model="searchForm.eventType" class="form-select">
          <option value="">全部類型</option>
          <option value="RELEASE">新品發售</option>
          <option value="SALE">優惠活動</option>
          <option value="EVENT">品牌活動</option>
          <option value="NOTICE">公告</option>
          <option value="OTHER">其他</option>
        </select>
      </div>

      <div class="col-md-3">
        <label class="form-label fw-bold">活動狀態</label>
        <select v-model="searchForm.status" class="form-select">
          <option value="">全部狀態</option>
          <option value="ACTIVE">啟用</option>
          <option value="INACTIVE">停用</option>
          <option value="DRAFT">草稿</option>
          <option value="CANCELLED">已取消</option>
        </select>
      </div>

      <div class="col-md-3">
        <label class="form-label fw-bold">開始時間起</label>
        <input
          v-model="searchForm.startDate"
          type="datetime-local"
          class="form-control"
        />
      </div>

      <div class="col-md-3">
        <label class="form-label fw-bold">結束時間止</label>
        <input
          v-model="searchForm.endDate"
          type="datetime-local"
          class="form-control"
        />
      </div>
    </div>

    <div class="d-flex justify-content-end gap-2 mt-3">
      <button
        type="button"
        class="btn btn-outline-secondary"
        @click="handleReset"
      >
        重設
      </button>
      <button type="button" class="btn btn-primary px-4" @click="handleSearch">
        <i class="bi bi-search"></i> 查詢
      </button>
    </div>
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
