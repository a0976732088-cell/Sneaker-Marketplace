<template>
  <nav
    v-if="totalPages > 0"
    class="auction-pagination"
    aria-label="競標商品分頁"
  >
    <p>
      第
      <strong>
        {{ currentPage + 1 }}
      </strong>
      /
      {{ totalPages }}
      頁，共
      {{ totalElements }}
      筆
    </p>

    <div class="pagination-actions">
      <button
        type="button"
        :disabled="loading || currentPage === 0"
        @click="emit('change', currentPage - 1)"
      >
        上一頁
      </button>

      <span aria-current="page">
        {{ currentPage + 1 }}
      </span>

      <button
        type="button"
        :disabled="loading || currentPage >= totalPages - 1"
        @click="emit('change', currentPage + 1)"
      >
        下一頁
      </button>
    </div>
  </nav>
</template>

<script setup>
defineProps({
  currentPage: { type: Number, required: true },
  totalPages: { type: Number, required: true },
  totalElements: { type: Number, required: true },
  loading: { type: Boolean, required: true },
});

const emit = defineEmits(["change"]);
</script>

<style scoped>
.auction-pagination {
  display: flex;

  align-items: center;

  justify-content: space-between;

  gap: 1rem;

  margin-top: 3rem;
  padding-top: 1.25rem;

  border-top: 1px solid #dee2e6;
}

.auction-pagination p {
  margin: 0;
  color: #495057;
  font-size: 0.95rem;
}

.pagination-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.pagination-actions button,
.pagination-actions span {
  min-width: 40px;
  min-height: 40px;

  display: inline-flex;

  align-items: center;
  justify-content: center;

  border: 1px solid #dee2e6;

  background: #fff;
  color: #212529;

  font-size: 0.95rem;
}

.pagination-actions button {
  padding-inline: 0.85rem;
}

.pagination-actions button:hover:not(:disabled) {
  border-color: #111;
}

.pagination-actions button:disabled {
  background: #f1f3f5;
  color: #495057;
  cursor: not-allowed;
}

.pagination-actions span {
  border-color: #111;
  background: #111;
  color: #fff;
  font-weight: 700;
}

@media (max-width: 767.98px) {
  .auction-pagination {
    align-items: stretch;
    flex-direction: column;
  }

  .pagination-actions {
    justify-content: space-between;
  }

  .pagination-actions button {
    flex: 1;
  }
}
</style>
