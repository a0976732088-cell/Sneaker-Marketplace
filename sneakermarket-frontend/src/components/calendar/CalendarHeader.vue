<template>
  <div class="calendar-header">
    <button
      type="button"
      aria-label="上一個月"
      @click="$emit('change-month', -1)"
    >
      ‹
    </button>

    <h3 aria-live="polite">
      {{ currentYear }} 年 {{ currentMonth + 1 }} 月
    </h3>

    <button
      type="button"
      aria-label="下一個月"
      @click="$emit('change-month', 1)"
    >
      ›
    </button>
  </div>
</template>

<script setup>
defineProps({
  currentYear: { type: Number, required: true },
  currentMonth: { type: Number, required: true },
});

defineEmits(["change-month"]);
</script>

<style scoped>
.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid #e5e8e9;
  border-bottom: 0;
  border-radius: 12px 12px 0 0;
}

.calendar-header h3 {
  margin: 0;
  color: #202326;
  font-size: 17px;
  font-weight: 700;
}

.calendar-header button {
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #30373b;
  font-size: 24px;
  cursor: pointer;
}

.calendar-header button:hover {
  background: #eef1f0;
}
</style>