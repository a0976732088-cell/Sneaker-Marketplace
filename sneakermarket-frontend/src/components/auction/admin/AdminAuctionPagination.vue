<template>
  <div
    class="card-body d-flex flex-wrap justify-content-between align-items-center gap-3 border-top"
  >
    <span class="text-secondary"
      >共 {{ totalElements }} 筆，第 {{ page + 1 }} /
      {{ Math.max(totalPages, 1) }} 頁</span
    >
    <div class="btn-group" role="group" aria-label="競標管理分頁">
      <button
        class="btn btn-outline-secondary"
        type="button"
        :disabled="loading || submitting || page === 0"
        @click="emit('change', page - 1)"
      >
        上一頁
      </button>
      <button
        class="btn btn-outline-secondary"
        type="button"
        :disabled="loading || submitting || page + 1 >= totalPages"
        @click="emit('change', page + 1)"
      >
        下一頁
      </button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  page: { type: Number, required: true },
  totalPages: { type: Number, required: true },
  totalElements: { type: Number, required: true },
  loading: { type: Boolean, required: true },
  submitting: { type: Boolean, required: true },
});

const emit = defineEmits(["change"]);
</script>
