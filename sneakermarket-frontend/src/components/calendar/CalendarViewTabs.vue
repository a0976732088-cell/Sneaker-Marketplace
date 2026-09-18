<template>
  <div class="view-tabs" role="group" aria-label="活動檢視模式">
    <button
      v-for="tab in tabs"
      :key="tab.value"
      type="button"
      :class="{ active: modelValue === tab.value }"
      :aria-pressed="modelValue === tab.value"
      @click="$emit('update:modelValue', tab.value)"
    >
      {{ tab.label }}
    </button>
  </div>
</template>

<script setup>
defineProps({
  modelValue: { type: String, required: true },
});

defineEmits(["update:modelValue"]);

const tabs = [
  { value: "list", label: "活動列表" },
  { value: "map", label: "地圖瀏覽" },
  { value: "month", label: "日曆模式" },
];
</script>

<style scoped>
.view-tabs {
  display: flex;
  gap: 24px;
  overflow-x: auto;
  border-bottom: 1px solid #e4e6e7;
}

.view-tabs button {
  flex: 0 0 auto;
  padding: 14px 6px 12px;
  border: 0;
  border-bottom: 3px solid transparent;
  background: transparent;
  color: #6a7178;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.view-tabs button.active {
  border-bottom-color: #e7664e;
  color: #202326;
}

.view-tabs button:focus-visible {
  outline: 2px solid #202326;
  outline-offset: -2px;
}
</style>