<script setup>
import { onUnmounted } from "vue";
import { resolveAdvertisementImageUrl } from "@/service/advertisementService";
import { useAdvertisementTracking } from "@/composables/advertisement/useAdvertisementTracking";

defineProps({
  popup: {
    type: Object,
    required: true,
  },
});

const emit = defineEmits(["close", "click-ad"]);

const { vViewableAd, disposeAdvertisementTracking } =
  useAdvertisementTracking();

onUnmounted(() => {
  disposeAdvertisementTracking();
});
</script>

<template>
  <Transition
    name="popup-ios"
    appear
    :duration="{ enter: 520, leave: 260 }"
  >
    <div class="popup-background">
      <article v-viewable-ad="popup" class="popup-ad">
        <button
          type="button"
          class="popup-close"
          aria-label="關閉廣告"
          @click="emit('close')"
        >
          ×
        </button>

        <img
          :src="resolveAdvertisementImageUrl(popup.imageUrl)"
          :alt="popup.adName"
          @click="emit('click-ad')"
        />
      </article>
    </div>
  </Transition>
</template>

<style scoped src="./AdvertisementPopup.css"></style>
