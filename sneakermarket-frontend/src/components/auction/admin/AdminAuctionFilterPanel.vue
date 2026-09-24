<template>
  <section class="card shadow-sm mb-4">
    <div class="card-body">
      <form class="row g-3 align-items-end" @submit.prevent="emit('submit')">
        <div class="col-12 col-xl-4">
          <label
            class="form-label filter-field-label"
            for="admin-auction-keyword"
            >商品名稱或型號</label
          >
          <input
            id="admin-auction-keyword"
            v-model.trim="filters.keyword"
            class="form-control"
            type="search"
          />
        </div>
        <div class="col-12 col-md-4 col-xl-2">
          <label
            class="form-label filter-field-label"
            for="admin-auction-status"
            >競標狀態</label
          >
          <select
            id="admin-auction-status"
            v-model="filters.status"
            class="form-select"
          >
            <option value="">全部</option>
            <option v-for="status in statuses" :key="status" :value="status">
              {{ formatStatus(status) }}
            </option>
          </select>
        </div>
        <div class="col-12 col-md-4 col-xl-2">
          <label
            class="form-label filter-field-label"
            for="admin-order-status"
            >訂單狀態</label
          >
          <select
            id="admin-order-status"
            v-model="filters.orderStatus"
            class="form-select"
          >
            <option value="">全部</option>
            <option
              v-for="status in orderStatuses"
              :key="status"
              :value="status"
            >
              {{ formatAuctionOrderStatus(status) }}
            </option>
          </select>
        </div>
        <div class="col-12 col-md-4 col-xl-2">
          <label
            class="form-label filter-field-label"
            for="admin-management-state"
            >管理狀態</label
          >
          <select
            id="admin-management-state"
            v-model="filters.managementState"
            class="form-select"
          >
            <option value="">全部</option>
            <option value="VISIBLE">公開中</option>
            <option value="REMOVED">已下架</option>
            <option value="DELETED">已刪除</option>
          </select>
        </div>
        <div class="col-12 col-xl-2 d-grid">
          <button
            class="btn btn-dark"
            type="submit"
            :disabled="loading || submitting"
          >
            {{ loading ? "查詢中…" : "查詢" }}
          </button>
        </div>
      </form>
    </div>
  </section>
</template>

<script setup>
import { formatAuctionOrderStatus } from "@/utils/auctionMemberStatus";

defineProps({
  filters: { type: Object, required: true },
  statuses: { type: Array, required: true },
  orderStatuses: { type: Array, required: true },
  formatStatus: { type: Function, required: true },
  loading: { type: Boolean, required: true },
  submitting: { type: Boolean, required: true },
});

const emit = defineEmits(["submit"]);
</script>

<style scoped>
.filter-field-label {
  white-space: nowrap;
  word-break: keep-all;
}
</style>
