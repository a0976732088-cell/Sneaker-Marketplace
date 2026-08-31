<template>
  <div class="alert alert-danger" role="alert">
    {{ message }}
  </div>
</template>

<script setup>
defineProps({
  message: {
    type: String,
    required: true,
  },
})
</script>
