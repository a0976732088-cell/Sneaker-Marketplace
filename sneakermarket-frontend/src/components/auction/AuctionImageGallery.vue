<script setup>
import { computed, ref, watch } from "vue";

import { resolveAuctionImageUrl } from "@/utils/auctionImageUrl";

const props = defineProps({
  imageUrls: {
    type: Array,
    default: () => [],
  },
  productName: {
    type: String,
    default: "競標商品",
  },
});

const selectedIndex = ref(0);
const selectedImageFailed = ref(false);
const failedImageIndexes = ref(new Set());

const images = computed(() =>
  (Array.isArray(props.imageUrls) ? props.imageUrls : []).filter(Boolean),
);
const selectedImage = computed(() => images.value[selectedIndex.value] ?? null);

watch(
  () => props.imageUrls,
  () => {
    selectedIndex.value = 0;
    selectedImageFailed.value = false;
    failedImageIndexes.value = new Set();
  },
  { deep: true },
);

function selectImage(index) {
  selectedIndex.value = index;
  selectedImageFailed.value = failedImageIndexes.value.has(index);
}

function markImageFailed(index) {
  const failedIndexes = new Set(failedImageIndexes.value);
  failedIndexes.add(index);
  failedImageIndexes.value = failedIndexes;

  if (selectedIndex.value === index) {
    selectedImageFailed.value = true;
  }
}
</script>

<template>
  <div class="auction-gallery">
    <div class="gallery-main">
      <img
        v-if="selectedImage && !selectedImageFailed"
        :src="resolveAuctionImageUrl(selectedImage)"
        :alt="productName"
        @error="markImageFailed(selectedIndex)"
      />
      <div v-else class="gallery-placeholder" role="img" :aria-label="`${productName} 尚無圖片`">
        <span class="placeholder-mark">SM</span>
        <span>尚無商品圖片</span>
      </div>
    </div>

    <div v-if="images.length > 1" class="gallery-thumbnails" aria-label="商品圖片縮圖">
      <button
        v-for="(imageUrl, index) in images"
        :key="`${imageUrl}-${index}`"
        type="button"
        class="thumbnail-button"
        :class="{ active: selectedIndex === index }"
        :aria-label="`查看第 ${index + 1} 張商品圖片`"
        :aria-pressed="selectedIndex === index"
        @click="selectImage(index)"
      >
        <img
          v-if="!failedImageIndexes.has(index)"
          :src="resolveAuctionImageUrl(imageUrl)"
          :alt="`${productName} 縮圖 ${index + 1}`"
          @error="markImageFailed(index)"
        />
        <span v-else class="thumbnail-placeholder" aria-hidden="true">SM</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.auction-gallery {
  width: 100%;
}

.gallery-main {
  overflow: hidden;
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #f1f1f1;
}

.gallery-main img,
.gallery-placeholder {
  display: block;
  width: 100%;
  height: 100%;
}

.gallery-main img {
  object-fit: cover;
}

.gallery-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  color: #495057;
  font-size: 0.85rem;
}

.placeholder-mark {
  color: #5d6670;
  font-size: 2rem;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.gallery-thumbnails {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0.65rem;
  margin-top: 0.75rem;
}

.thumbnail-button {
  overflow: hidden;
  padding: 0;
  aspect-ratio: 1 / 1;
  border: 1px solid #dee2e6;
  background: #f8f9fa;
}

.thumbnail-button:hover,
.thumbnail-button.active {
  border-color: #111;
}

.thumbnail-button.active {
  box-shadow: inset 0 0 0 1px #111;
}

.thumbnail-button img,
.thumbnail-placeholder {
  display: block;
  width: 100%;
  height: 100%;
}

.thumbnail-button img {
  object-fit: cover;
}

.thumbnail-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #495057;
  font-size: 0.8rem;
  font-weight: 800;
  letter-spacing: 0.08em;
}
</style>
