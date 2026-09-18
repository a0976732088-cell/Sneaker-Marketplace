<template>
  <div class="event-type-filters" role="group" aria-label="活動分類">
    <button
      v-for="type in EVENT_TYPES"
      :key="type.key"
      type="button"
      class="filter-chip"
      :class="{ active: modelValue === type.key }"
      :style="{ '--chip-color': type.color }"
      :aria-pressed="modelValue === type.key"
      @click="$emit('update:modelValue', modelValue === type.key ? null : type.key)"
    >
      {{ type.label }}
    </button>
  </div>
</template>

<script setup>
import { EVENT_TYPES } from "@/constants/eventTypes";

defineProps({
  modelValue: { type: String, default: null },
});

defineEmits(["update:modelValue"]);
</script>

<style scoped>
.event-type-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-chip {
  min-height: 38px;
  padding: 7px 18px;
  border: 1px solid transparent;
  border-radius: 999px;
  background: var(--chip-color);
  color: #24282b;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  opacity: 0.78;
  transition: opacity 0.2s, transform 0.2s, box-shadow 0.2s;
}

.filter-chip:hover,
.filter-chip.active {
  opacity: 1;
  transform: translateY(-1px);
}

.filter-chip.active {
  box-shadow: 0 0 0 2px #24282b;
}

.filter-chip:focus-visible {
  outline: 3px solid #24282b;
  outline-offset: 3px;
}
</style>