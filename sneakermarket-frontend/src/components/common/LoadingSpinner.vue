<template>
  <div class="d-flex align-items-center gap-2" role="status" aria-live="polite">
    <div class="spinner-border spinner-border-sm" aria-hidden="true"></div>
    <span>{{ text }}</span>
  </div>
</template>

<script setup>
defineProps({
  text: {
    type: String,
    default: '讀取中...',
  },
})
</script>
