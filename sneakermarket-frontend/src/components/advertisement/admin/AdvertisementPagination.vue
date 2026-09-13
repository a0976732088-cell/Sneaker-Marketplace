<script setup>
defineProps({
  currentPage: {
    type: Number,
    required: true,
  },
  totalPages: {
    type: Number,
    required: true,
  },
  totalElements: {
    type: Number,
    required: true,
  },
  pageNumbers: {
    type: Array,
    required: true,
  },
  firstItemNumber: {
    type: Number,
    required: true,
  },
  lastItemNumber: {
    type: Number,
    required: true,
  },
  loading: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(["change-page"]);

function requestPage(pageNumber) {
  emit("change-page", pageNumber);
}
</script>

<template>
  <footer class="panel-footer">
    <span>
      顯示 {{ firstItemNumber }}–{{ lastItemNumber }}，共 {{ totalElements }} 筆
    </span>

    <div>
      <button
        type="button"
        aria-label="上一頁"
        :disabled="currentPage === 0 || loading"
        @click="requestPage(currentPage - 1)"
      >
        ‹
      </button>

      <button
        v-for="pageNumber in pageNumbers"
        :key="pageNumber"
        type="button"
        :class="{ current: currentPage === pageNumber }"
        :disabled="loading"
        @click="requestPage(pageNumber)"
      >
        {{ pageNumber + 1 }}
      </button>

      <button
        type="button"
        aria-label="下一頁"
        :disabled="totalPages === 0 || currentPage >= totalPages - 1 || loading"
        @click="requestPage(currentPage + 1)"
      >
        ›
      </button>
    </div>
  </footer>
</template>

<style scoped>
.panel-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-top: 1px solid var(--line);
  color: #9099a7;
  font-size: 14px;
}

.panel-footer div {
  display: flex;
  gap: 4px;
}

.panel-footer button {
  min-width: 32px;
  height: 32px;
  padding: 0 8px;
  border: 1px solid var(--line);
  border-radius: 5px;
  color: #788394;
  background: #fff;
  font-size: 14px;
}

.panel-footer button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.panel-footer button.current {
  border-color: var(--coral);
  color: #fff;
  background: var(--coral);
}
</style>
