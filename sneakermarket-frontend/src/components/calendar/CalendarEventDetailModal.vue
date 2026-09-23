<template>
  <EventDetailModal :event="event" @close="emit('close')" />
</template>

<script setup>
import EventDetailModal from "@/components/events/EventDetailModal.vue";

defineProps({ event: { type: Object, required: true } });
const emit = defineEmits(["close"]);
</script>
