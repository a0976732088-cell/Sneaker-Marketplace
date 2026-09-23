<script setup>
import { onUnmounted } from "vue";
import { resolveAdvertisementImageUrl } from "@/service/advertisementService";
import { useAdvertisementTracking } from "@/composables/advertisement/useAdvertisementTracking";

defineProps({
  advertisement: {
    type: Object,
    required: true,
  },
  showAction: {
    type: Boolean,
    default: false,
  },
  actionLabel: {
    type: String,
    default: "立即報名",
  },
});

const emit = defineEmits(["close", "action"]);

const { vViewableAd, disposeAdvertisementTracking } =
  useAdvertisementTracking();

onUnmounted(() => {
  disposeAdvertisementTracking();
});
</script>

<template>
  <aside v-viewable-ad="advertisement" class="small-block-ad">
    <button
      type="button"
      class="small-close"
      aria-label="關閉廣告"
      @click="emit('close')"
    >
      ×
    </button>

    <img
      :src="resolveAdvertisementImageUrl(advertisement.imageUrl)"
      :alt="advertisement.adName"
    />

    <button
      v-if="showAction && advertisement.targetType === 'EVENT'"
      type="button"
      class="small-block-action"
      @click="emit('action')"
    >
      {{ actionLabel }}
      <span aria-hidden="true">→</span>
    </button>
  </aside>
</template>

<style scoped src="./AdvertisementSmallBlock.css"></style>
